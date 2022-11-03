package me.gommeantilegit.minecraft.earth.world

import android.view.MotionEvent
import com.google.ar.core.HitResult
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.RenderableDefinition
import com.google.ar.sceneform.ux.ArFragment
import me.gommeantilegit.minecraft.earth.block.Blocks
import me.gommeantilegit.minecraft.earth.utils.BlockPos
import me.gommeantilegit.minecraft.earth.world.generation.IWorldGenerator
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap

class WorldDisplayer(private val renderDistance: Int, private val arFragment: ArFragment, private val arSession: Session, private val worldPlacePoint: HitResult, private val world: World, private val worldGenerator: IWorldGenerator) {

    private val originAnchor = AnchorNode(worldPlacePoint.createAnchor()).apply {
        setParent(arFragment.arSceneView.scene)
    }

    private var lastChunkPos: ChunkPosition? = null
    private var lastBlockPos: BlockPos? = null
    private val chunkToNodeMap = HashMap<ChunkPosition, Node>()

    init {
        world.addBlockChangeListener { blockPos, _ ->
            rebuildFor(blockPos)
        }
    }

    private fun rebuildFor(blockPos: BlockPos) {
        val chunkPos = ChunkPosition.of(blockPos)
        val chunk = world.getChunkAt(chunkPos)

        // update this chunk
        chunk?.let { updateChunkMesh(it) }
    }


    private val chunkRenderablesToFinalize = LinkedBlockingQueue<ChunkMeshBuildResult>(100)


    private var nMeshesFinalized = 0

    fun onFrame() {
        while (chunkRenderablesToFinalize.isNotEmpty()) {
            val chunkMeshBuildResult = chunkRenderablesToFinalize.poll()

            val vertices = chunkMeshBuildResult.vertices
            val meshes = chunkMeshBuildResult.meshes
            val chunkPosition = chunkMeshBuildResult.chunkPosition
            val definition = RenderableDefinition.builder()
                    .setVertices(vertices)
                    .setSubmeshes(meshes)
                    .build()

            ModelRenderable.builder()
                    .setSource(definition)
                    .build()
                    .get()
                    .let { renderable ->
                        chunkToNodeMap.remove(chunkPosition)?.let { oldChunkNode ->
                            originAnchor.removeChild(oldChunkNode)
                            oldChunkNode.renderable = null
                        }
                        val chunkNode = Node().apply {
                            val vec = chunkPosition.asVector
                            vec.y -= 1
                            localPosition = vec
                        }
                        chunkNode.renderable = renderable
                        originAnchor.addChild(chunkNode)
                        chunkNode.setOnTapListener { hitTestResult, motionEvent ->
                            onClick(hitTestResult, motionEvent)
                        }
                        chunkToNodeMap[chunkPosition] = chunkNode
                        nMeshesFinalized++
                        println("Finalized $nMeshesFinalized meshes")
                    }
        }
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
        for (pos in newChunkPosition.iterateRange(renderDistance)) {
            if (!world.hasChunkAt(pos)) {
                val chunk = Chunk(pos)
                worldGenerator.generate(chunk)
                world.addChunk(chunk)
                updateChunkMesh(chunk)
            }
        }
    }

    private fun updateChunkMesh(chunk: Chunk) {
        chunk.createRenderable(world).thenAcceptAsync { (meshes, vertices) ->
            if (meshes.isEmpty() || vertices.isEmpty()) {
                return@thenAcceptAsync
            }
            chunkRenderablesToFinalize.add(ChunkMeshBuildResult(chunk.chunkPosition, meshes, vertices))
        }
    }

    fun getWorldSpaceCoords(localPosition: Vector3): Vector3 {
        val originTranslation = originAnchor.anchor!!.pose.translation
        return Vector3(localPosition.x - originTranslation[0], localPosition.y - originTranslation[1], localPosition.z - originTranslation[2])
    }

    fun onClick(hitTestResult: HitTestResult, motionEvent: MotionEvent) {
//        val arSpaceCoords = hitTestResult.localPosition
//        val worldSpaceCoords = getWorldSpaceCoords(arSpaceCoords)
//        val blockPos = BlockPos.of(worldSpaceCoords)
        world.setBlockWithWorldSpaceCoords(BlockPos(0, 0, 0), Blocks.blocks.random())
    }
}