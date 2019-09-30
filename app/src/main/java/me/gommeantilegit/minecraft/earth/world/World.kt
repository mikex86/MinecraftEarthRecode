package me.gommeantilegit.minecraft.earth.world

import android.os.Handler
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.RenderableDefinition
import com.google.ar.sceneform.rendering.Vertex
import me.gommeantilegit.minecraft.earth.block.Block
import me.gommeantilegit.minecraft.earth.block.Blocks
import me.gommeantilegit.minecraft.earth.rendering.BlockModelBakery
import me.gommeantilegit.minecraft.earth.utils.BlockPos
import me.gommeantilegit.minecraft.earth.utils.EnumFacing
import java.util.concurrent.CompletableFuture

data class BlockState(val block: Block)

open class World {

    companion object {
        const val worldScale = 1f
    }

    private val chunks = HashMap<ChunkPosition, Chunk>()

    /**
     * @property xChunk the x position of the chunk in unit "chunks"
     * @property xChunk the y position of the chunk in unit "chunks"
     * @property xChunk the z position of the chunk in unit "chunks"
     */
    data class ChunkPosition(val xChunk: Int, val yCoord: Int, val zChunk: Int) {
        companion object {
            /**
             * @return a the position for the chunk containing the specified block position
             */
            fun of(blockPos: BlockPos) = with(blockPos) {
                ChunkPosition(x / 16, y / 16, z / 16)
            }
        }
    }

    fun getBlock(x: Int, y: Int, z: Int) = getBlock(BlockPos(x, y, z))

    fun getBlock(blockPos: BlockPos) = with(blockPos) {
        chunks.computeIfAbsent(ChunkPosition.of(this)) { Chunk() }.getBlock(x, y, z)
    }

    /**
     * Response from async chunk creation
     */
    data class ChunkResponse(val vertices: List<Vertex>, val meshes: List<RenderableDefinition.Submesh>, val anchorNode: AnchorNode)

    open class Chunk {

        companion object {
            const val chunkSize = 16
        }

        private val blockStates = Array(chunkSize * chunkSize * chunkSize) { BlockState(Blocks.air) }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Chunk

            if (!blockStates.contentDeepEquals(other.blockStates)) return false

            return true
        }

        /**
         * @return the block state of the block with the given chunk local x, y, z, coordinate
         */
        fun getBlock(x: Int, y: Int, z: Int) = blockStates.getOrNull(getIndex(x, y, z))

        /**
         * @return the index for a given block position in the block states array or -1 if the position is out of local chunk bounds
         */
        private fun getIndex(x: Int, y: Int, z: Int): Int {
            if (x >= chunkSize || y >= chunkSize || z >= chunkSize || x < 0 || y < 0 || z < 0)
                return -1
            return x * chunkSize * chunkSize + y * chunkSize + z
        }

        fun setBlock(x: Int, y: Int, z: Int, blockState: BlockState) {
            blockStates[getIndex(x, y, z)] = blockState
        }

        fun createRenderable(handler: Handler, anchorNode: AnchorNode) {
            CompletableFuture.supplyAsync<Pair<List<RenderableDefinition.Submesh>, List<Vertex>>> {
                val vertices = ArrayList<Vertex>()
                val meshes = ArrayList<RenderableDefinition.Submesh>()
                for (x in 0 until 16) {
                    for (y in 0 until 16) {
                        for (z in 0 until 16) {
                            val state = getBlock(x, y, z)
                            meshes.addAll(BlockModelBakery.getModel(state?.block
                                    ?: error("Could not retrieve chunk local block state for co-ordinates (x=$x, y=$y, z=$z)"))?.modelBuilder?.build(this, vertices, x, y, z)
                                    ?: continue) // continue for invisible blocks as they do not have a model
                        }
                    }
                }
                Pair(meshes, vertices)
            }.thenAccept { pair ->
                val message = handler.obtainMessage()
                message.obj = ChunkResponse(pair.second, pair.first, anchorNode)
                handler.sendMessage(message)
            }
        }

        fun canSee(x: Int, y: Int, z: Int, side: Int): Boolean {
            val facing = EnumFacing.ofIndex(side) ?: error("Invalid enum facing: $side")
            with(facing.facingDirection) {
                with(getBlock(x + this.x, y + this.y, z + this.z) ?: return false) {
                    return this.block.invisible
                }
            }
        }

        override fun hashCode(): Int {
            return blockStates.contentDeepHashCode()
        }
    }
}