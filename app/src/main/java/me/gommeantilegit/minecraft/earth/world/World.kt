package me.gommeantilegit.minecraft.earth.world

import android.os.Handler
import android.os.Looper
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.RenderableDefinition
import com.google.ar.sceneform.rendering.Vertex
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import me.gommeantilegit.minecraft.earth.block.Block
import me.gommeantilegit.minecraft.earth.block.Blocks
import me.gommeantilegit.minecraft.earth.rendering.BlockModelBakery
import me.gommeantilegit.minecraft.earth.utils.BlockPos
import me.gommeantilegit.minecraft.earth.utils.EnumFacing
import me.gommeantilegit.minecraft.earth.world.generation.IWorldGenerator
import java.lang.Math.floorDiv
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class BlockState(val block: Block)

class WorldDisplayer(private val renderDistance: Int, private val arFragment: ArFragment, private val arSession: Session, private val worldPlacePoint: HitResult, private val world: World, private val worldGenerator: IWorldGenerator) {

    private val originAnchor = AnchorNode(worldPlacePoint.trackable.createAnchor(worldPlacePoint.hitPose)).apply {
        setParent(arFragment.arSceneView.scene)
    }

    private var lastChunkPos: ChunkPosition? = null
    private var lastBlockPos: BlockPos? = null

    /**
     * Handler on UI thread
     */
    private val handler = Handler(Looper.getMainLooper()) { message ->
        val obj = message.obj
        if (obj is ChunkResponse) {
            val node = Node().apply {
                setParent(originAnchor)
                val vec = obj.chunkPosition.asVector
                vec.y -= 1
                localPosition = vec
            }

            val definition = RenderableDefinition.builder()
                    .setVertices(obj.vertices)
                    .setSubmeshes(obj.meshes)
                    .build()

            ModelRenderable.builder()
                    .setSource(definition)
                    .build()
                    .thenAccept { renderable ->
                        node.renderable = renderable
                    }
        }
        true
    }

    fun onViewerMoved(viewerPosition: Vector3) {
        val currentBlockPos = BlockPos.of(viewerPosition)
        val currentChunkPos = currentBlockPos.chunkPos
        if (currentChunkPos != lastChunkPos) {
            onChunkChanged(currentChunkPos)
        }
        lastChunkPos = currentChunkPos
        lastBlockPos = currentBlockPos
    }

    private fun onChunkChanged(newChunkPosition: ChunkPosition) {
        val pos = newChunkPosition
//        for (pos in newChunkPosition.iterateRange(renderDistance)) {
        if (!world.hasChunkAt(pos)) {
            val chunk = Chunk(pos)
            worldGenerator.generate(chunk)
            world.addChunk(chunk)
            displayChunk(chunk)
        }
//        }
    }

    private fun displayChunk(chunk: Chunk) {
        chunk.createRenderable().thenAccept { pair ->
            val message = handler.obtainMessage()
            message.obj = ChunkResponse(chunk.chunkPosition, pair.second, pair.first)
            handler.sendMessage(message)
        }
    }

    fun getViewerPosition(localPosition: Vector3): Vector3 {
        val originTranslation = originAnchor.anchor!!.pose.translation
        return Vector3(localPosition.x - originTranslation[0], localPosition.y - originTranslation[1], localPosition.z - originTranslation[2])
    }
}

/**
 * @property chunkX the x position of the chunk in unit "chunks"
 * @property chunkX the y position of the chunk in unit "chunks"
 * @property chunkX the z position of the chunk in unit "chunks"
 */
data class ChunkPosition(val chunkX: Int, val chunkY: Int, val chunkZ: Int) {

    fun iterateRange(chunkDistance: Int): List<ChunkPosition> {
        val positions = ArrayList<ChunkPosition>((2 * chunkDistance) * (2 * chunkDistance) * (2 * chunkDistance))
        for (x in -chunkDistance..chunkDistance) {
            for (y in -chunkDistance..chunkDistance) {
                for (z in -chunkDistance..chunkDistance) {
                    positions.add(ChunkPosition(chunkX + x, chunkY + y, chunkZ + z))
                }
            }
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

}

open class World {

    companion object {
        const val worldScale = 1f
    }

    private val chunks = HashMap<ChunkPosition, Chunk>()

    fun getBlock(x: Int, y: Int, z: Int) = getBlock(BlockPos(x, y, z))

    fun getBlock(blockPos: BlockPos) = with(blockPos) {
        chunks.computeIfAbsent(ChunkPosition.of(this)) { Chunk(blockPos.chunkPos) }.getBlock(x, y, z)
    }

    fun hasChunkAt(chunkPosition: ChunkPosition): Boolean {
        return chunks.containsKey(chunkPosition)
    }

    fun addChunk(chunk: Chunk) {
        chunks[chunk.chunkPosition] = chunk
    }

}

/**
 * Response from async chunk creation
 */
data class ChunkResponse(val chunkPosition: ChunkPosition, val vertices: List<Vertex>, val meshes: List<RenderableDefinition.Submesh>)

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

    fun createRenderable() = CompletableFuture.supplyAsync<Pair<List<RenderableDefinition.Submesh>, List<Vertex>>> {
        val vertices = ArrayList<Vertex>()
        val meshes = ArrayList<RenderableDefinition.Submesh>()
        for (x in 0 until chunkSize) {
            for (y in 0 until chunkSize) {
                for (z in 0 until chunkSize) {
                    val state = getBlock(x, y, z)
                    meshes.addAll(BlockModelBakery.getModel(state?.block
                            ?: error("Could not retrieve chunk local block state for co-ordinates (x=$x, y=$y, z=$z)"))?.modelBuilder?.build(this, vertices, x, y, z)
                            ?: continue) // continue for invisible blocks as they do not have a model
                }
            }
        }
        Pair(meshes, vertices)
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