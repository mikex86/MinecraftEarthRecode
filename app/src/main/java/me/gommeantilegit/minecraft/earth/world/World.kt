package me.gommeantilegit.minecraft.earth.world

import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.RenderableDefinition
import com.google.ar.sceneform.rendering.Vertex
import me.gommeantilegit.minecraft.earth.block.Block
import me.gommeantilegit.minecraft.earth.block.Blocks
import me.gommeantilegit.minecraft.earth.rendering.BlockModelBakery
import me.gommeantilegit.minecraft.earth.utils.BlockPos
import me.gommeantilegit.minecraft.earth.utils.EnumFacing
import java.lang.Math.floorDiv
import java.util.concurrent.CompletableFuture

data class BlockState(val block: Block)

/**
 * @property chunkX the x position of the chunk in unit "chunks"
 * @property chunkX the y position of the chunk in unit "chunks"
 * @property chunkX the z position of the chunk in unit "chunks"
 */
class ChunkPosition(val chunkX: Int, val chunkY: Int, val chunkZ: Int) {

    fun iterateRange(chunkDistance: Int): Iterable<ChunkPosition> {
        val positions = HashSet<ChunkPosition>((2 * chunkDistance) * (2 * chunkDistance) * (2 * chunkDistance))
        for (x in -chunkDistance..chunkDistance) {
//            for (y in -chunkDistance..chunkDistance) {
            val y = 0
            for (z in -chunkDistance..chunkDistance) {
                positions.add(ChunkPosition(chunkX + x, chunkY + y, chunkZ + z))
            }
//            }
        }
        return positions
    }

    val xPosition: Int
        get() {
            return chunkX * Chunk.chunkSize
        }

    val yPosition: Int
        get() {
            return chunkY * Chunk.chunkSize
        }

    val zPosition: Int
        get() {
            return chunkZ * Chunk.chunkSize
        }

    val asVector: Vector3
        get() {
            return Vector3(xPosition.toFloat(), yPosition.toFloat(), zPosition.toFloat())
        }

    companion object {
        /**
         * @return a the position for the chunk containing the specified block position
         */
        fun of(blockPos: BlockPos) = with(blockPos) {

            ChunkPosition(
                    floorDiv(x, Chunk.chunkSize),
                    floorDiv(y, Chunk.chunkSize),
                    floorDiv(z, Chunk.chunkSize)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is ChunkPosition) {
            return other.chunkX == chunkX && other.chunkY == chunkY && other.chunkZ == chunkZ
        }
        return false
    }

    override fun hashCode(): Int {
        var result = chunkX
        result = 31 * result + chunkY
        result = 31 * result + chunkZ
        return result
    }

    override fun toString(): String {
        return "ChunkPosition(chunkX=$chunkX, chunkY=$chunkY, chunkZ=$chunkZ)"
    }
}

open class World {

    companion object {
        const val worldScale = 1f
    }

    private val chunks = HashMap<ChunkPosition, Chunk>()

    private val blockChangeListeners = ArrayList<(BlockPos, BlockState) -> Unit>()

    fun addBlockChangeListener(listener: (BlockPos, BlockState) -> Unit) {
        blockChangeListeners.add(listener)
    }

    /**
     * @param x the x position of the block in unit "blocks"
     * @param y the y position of the block in unit "blocks"
     * @param z the z position of the block in unit "blocks"
     * @return the block at the specified world-space position
     */
    fun getBlockWithWorldSpaceCoords(x: Int, y: Int, z: Int) = getBlockWithWorldSpaceCoords(BlockPos(x, y, z))

    /**
     * @param pos the position of the block in unit "blocks"
     * @return the block at the specified position
     */
    fun getBlockWithWorldSpaceCoords(pos: BlockPos) = with(pos) {
        return@with chunks[ChunkPosition.of(this)]?.getBlockWithWorldSpaceCoords(x, y, z)
    }

    /**
     * @return whether a chunk at the specified position exists
     */
    fun hasChunkAt(chunkPosition: ChunkPosition): Boolean {
        return chunks.containsKey(chunkPosition)
    }

    /**
     * Adds the specified chunk to the world
     * @param chunk the chunk to add
     */
    fun addChunk(chunk: Chunk) {
        chunks[chunk.chunkPosition] = chunk
    }

    /**
     * Breaks the block at the specified world space position
     * @param pos the world space position of the block to break
     */
    fun breakBlock(pos: BlockPos) {
        setBlockWithWorldSpaceCoords(pos, Blocks.air)
    }

    /**
     * Sets the block at the specified world space position to the specified block state
     * @param pos the world space position of the block to set
     * @param blockState the block state to set the block to
     */
    fun setBlockWithWorldSpaceCoords(pos: BlockPos, blockState: BlockState) {
        chunks.computeIfAbsent(ChunkPosition.of(pos)) { Chunk(pos.chunkPos) }.setBlockWithWorldSpaceCoords(pos, blockState)
    }

    /**
     * Sets the block at the specified world space position to the default block state of the specified block
     * @param pos the world space position of the block to set
     * @param block the block to set the block to
     */
    fun setBlockWithWorldSpaceCoords(pos: BlockPos, block: Block) {
        val newState = BlockState(block)
        chunks.computeIfAbsent(ChunkPosition.of(pos)) { Chunk(pos.chunkPos) }.setBlockWithWorldSpaceCoords(pos, newState)
        for (listener in blockChangeListeners) {
            listener(pos, newState)
        }
    }

    /**
     * @param x the x position of the block in unit "blocks"
     * @param y the y position of the block in unit "blocks"
     * @param z the z position of the block in unit "blocks"
     * @param side the side of the block face
     * @return whether the block is exposed in the specified facing direction.
     * If eg. a block is surrounded by air on all sides, it is exposed on all sides.
     * If a block is surrounded by air on all sides except the top, it is exposed on all sides except the top.
     */
    fun isBlockExposedOnSide(x: Int, y: Int, z: Int, side: EnumFacing): Boolean {
        with(side.facingDirection) {
            with(getBlockWithWorldSpaceCoords(x + this.x, y + this.y, z + this.z) ?: return false) {
                return this.block.invisible
            }
        }
    }

    /**
     * @param chunkPos the position of the chunk
     * @return the chunk at the specified position
     */
    fun getChunkAt(chunkPos: ChunkPosition): Chunk? {
        return chunks[chunkPos]
    }

}

/**
 * Result for async chunk creation
 */
data class ChunkMeshBuildResult(val chunkPosition: ChunkPosition, val meshes: List<RenderableDefinition.Submesh>, val vertices: List<Vertex>)

open class Chunk(val chunkPosition: ChunkPosition) {

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
    fun getBlockWithLocalCoords(x: Int, y: Int, z: Int) = blockStates.getOrNull(getIndex(x, y, z))

    /**
     * @return the block state of the block with the given world space x, y, z, coordinate
     */
    fun getBlockWithWorldSpaceCoords(x: Int, y: Int, z: Int) = getBlockWithLocalCoords(x - chunkPosition.xPosition, y - chunkPosition.yPosition, z - chunkPosition.zPosition)

    /**
     * @return the index for a given block position in the block states array or -1 if the position is out of local chunk bounds
     */
    private fun getIndex(x: Int, y: Int, z: Int): Int {
        if (x >= chunkSize || y >= chunkSize || z >= chunkSize || x < 0 || y < 0 || z < 0)
            throw IndexOutOfBoundsException("Block position out of chunk bounds")
        return x * chunkSize * chunkSize + y * chunkSize + z
    }

    /**
     * Sets the block state at the given chunk local x, y, z, coordinate to the specified block state
     * @param x the x coordinate of the block locally to the chunk origin
     * @param y the y coordinate of the block locally to the chunk origin
     * @param z the z coordinate of the block locally to the chunk origin
     * @param blockState the block state to set the block to
     */
    fun setBlockWithLocalCoords(x: Int, y: Int, z: Int, blockState: BlockState) {
        blockStates[getIndex(x, y, z)] = blockState
    }

    /**
     * Sets the block state at the given world space x, y, z, coordinate to the default block state of the given block
     * @param x the x coordinate of the block globally
     * @param y the y coordinate of the block globally
     * @param z the z coordinate of the block globally
     * @param block the type of block that should be set at the given position
     */
    fun setBlockWithWorldSpaceCoords(x: Int, y: Int, z: Int, blockState: BlockState) {
        setBlockWithLocalCoords(x - chunkPosition.xPosition, y - chunkPosition.yPosition, z - chunkPosition.zPosition, blockState)
    }


    /**
     * Sets the block state at the given world space x, y, z, coordinate
     * @param blockPos the position of the block globally
     * @param blockState the block state to set the block to
     */
    fun setBlockWithWorldSpaceCoords(blockPos: BlockPos, blockState: BlockState) {
        setBlockWithWorldSpaceCoords(blockPos.x, blockPos.y, blockPos.z, blockState)
    }

    fun createRenderable(world: World): CompletableFuture<Pair<List<RenderableDefinition.Submesh>, List<Vertex>>> = CompletableFuture.completedFuture(let {
        val vertices = ArrayList<Vertex>()
        val meshes = ArrayList<RenderableDefinition.Submesh>()
        for (x in 0 until chunkSize) {
            for (y in 0 until chunkSize) {
                for (z in 0 until chunkSize) {
                    val state = getBlockWithLocalCoords(x, y, z)
                    meshes.addAll(BlockModelBakery.getModel(state?.block
                            ?: error("Could not retrieve chunk local block state for co-ordinates (x=$x, y=$y, z=$z)"))?.modelBuilder?.build(world, this, vertices, x, y, z)
                            ?: continue) // continue for invisible blocks as they do not have a model
                }
            }
        }
        Pair(meshes, vertices)
    })

    override fun hashCode(): Int {
        return blockStates.contentDeepHashCode()
    }
}