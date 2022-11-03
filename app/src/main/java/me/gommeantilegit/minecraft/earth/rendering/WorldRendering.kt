package me.gommeantilegit.minecraft.earth.rendering

import android.content.Context
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import me.gommeantilegit.minecraft.earth.block.Block
import me.gommeantilegit.minecraft.earth.block.Blocks
import me.gommeantilegit.minecraft.earth.utils.EnumFacing
import me.gommeantilegit.minecraft.earth.world.Chunk
import me.gommeantilegit.minecraft.earth.world.World
import java.util.concurrent.CompletableFuture

class TextureResource(val textureResource: String, val transparent: Boolean)

class TextureData(val texture: CompletableFuture<Texture>, val transparent: Boolean)

/**
 * An object that generates meshes for a given block type at a specified position.
 */
interface Mesher {
    /**
     * Builds the mesh for the given chunk
     * @param world the world the chunk is in. Used for context beyond the specified chunk.
     * @param chunk the chunk to build the mesh for
     * @param chunkVertices the list of vertices to add to (the mesh output)
     * @param x the chunk local x block position where to build the mesh
     * @param y the chunk local y block position where to build the mesh
     * @param z the chunk local z block position where to build the mesh
     */
    fun build(world: World, chunk: Chunk, chunkVertices: MutableList<Vertex>, x: Int, y: Int, z: Int): List<RenderableDefinition.Submesh>
}

/**
 * @param textureFutures the list of texture futures parent to a given side (Array = overlays per side):
 * 0 -> bottom
 * 1 -> top
 * 2 -> north
 * 3 -> south
 * 4 -> west
 * 5 -> east
 * @param context android activity context
 * @see EnumFacing
 *
 */
class StupidMesher(textureFutures: List<List<TextureData>>, context: Context, metallic: Float, roughness: Float, reflectance: Float, private val processor: ISubmeshProcessor) : Mesher {

    private val materials = Array<Array<Material?>>(6) { i -> Array(textureFutures[i].size) { null } }

    companion object {

        private lateinit var transparentMaterial: CompletableFuture<Material>
        private lateinit var opaqueMaterial: CompletableFuture<Material>
        private var initialized = false

        fun init(context: Context) {
            if (initialized) return
            transparentMaterial = Material.builder().setSource { context.assets.open("materials/mat_block_transparent.matc") }.build()
            opaqueMaterial = Material.builder().setSource { context.assets.open("materials/mat_block_opaque.matc") }.build()
            initialized = true
        }

    }

    init {
        init(context)
        transparentMaterial.thenAccept { transparentMat ->
            opaqueMaterial.thenAccept { opaqueMat ->
                for ((i, futureArray) in textureFutures.withIndex()) {
                    for ((j, future) in futureArray.withIndex()) {
                        future.texture.thenAccept { texture ->
                            val blockMat = (if (future.transparent) transparentMat else opaqueMat).makeCopy()
                            blockMat.setTexture("texture", texture)
                            blockMat.setFloat("metallic", metallic)
                            blockMat.setFloat("roughness", roughness)
                            blockMat.setFloat("reflectance", reflectance)
                            blockMat.setFloat4("color", Color(1f, 1f, 1f, 1f))
                            materials[i][j] = blockMat
                        }
                    }
                }
            }
        }
    }

    override fun build(world: World, chunk: Chunk, chunkVertices: MutableList<Vertex>, x: Int, y: Int, z: Int): List<RenderableDefinition.Submesh> {

        val size = Vector3(1f, 1f, 1f).scaled(0.5f).scaled(World.worldScale)

        val center = Vector3(x.toFloat() * World.worldScale + size.x, y.toFloat() * World.worldScale + size.y, z.toFloat() * World.worldScale + size.z)

        val p0 = Vector3.add(center, Vector3(-size.x, -size.y, size.z))
        val p1 = Vector3.add(center, Vector3(size.x, -size.y, size.z))
        val p2 = Vector3.add(center, Vector3(size.x, -size.y, -size.z))
        val p3 = Vector3.add(center, Vector3(-size.x, -size.y, -size.z))
        val p4 = Vector3.add(center, Vector3(-size.x, size.y, size.z))
        val p5 = Vector3.add(center, Vector3(size.x, size.y, size.z))
        val p6 = Vector3.add(center, Vector3(size.x, size.y, -size.z))
        val p7 = Vector3.add(center, Vector3(-size.x, size.y, -size.z))

        val up = Vector3.up()
        val down = Vector3.down()
        val front = Vector3.forward()
        val back = Vector3.back()
        val left = Vector3.left()
        val right = Vector3.right()

        val uv00 = Vertex.UvCoordinate(0.0f, 0.0f)
        val uv10 = Vertex.UvCoordinate(1.0f, 0.0f)
        val uv01 = Vertex.UvCoordinate(0.0f, 1.0f)
        val uv11 = Vertex.UvCoordinate(1.0f, 1.0f)

        val meshes = ArrayList<RenderableDefinition.Submesh>()

        val vertices = listOf(
                Vertex.builder().setPosition(p0).setNormal(down).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p1).setNormal(down).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p2).setNormal(down).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p3).setNormal(down).setUvCoordinate(uv00).build(),

                Vertex.builder().setPosition(p7).setNormal(up).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p6).setNormal(up).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p5).setNormal(up).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p4).setNormal(up).setUvCoordinate(uv00).build(),

                Vertex.builder().setPosition(p6).setNormal(back).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p7).setNormal(back).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p3).setNormal(back).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p2).setNormal(back).setUvCoordinate(uv00).build(),

                Vertex.builder().setPosition(p4).setNormal(front).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p5).setNormal(front).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p1).setNormal(front).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p0).setNormal(front).setUvCoordinate(uv00).build(),

                Vertex.builder().setPosition(p7).setNormal(left).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p4).setNormal(left).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p0).setNormal(left).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p3).setNormal(left).setUvCoordinate(uv00).build(),

                Vertex.builder().setPosition(p5).setNormal(right).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p6).setNormal(right).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p2).setNormal(right).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p1).setNormal(right).setUvCoordinate(uv00).build()
        )

        for (side in 0..5) {

            val facing = EnumFacing.ofIndex(side) ?: error("Invalid enum facing: $side")

            if (!world.isBlockExposedOnSide(chunk.chunkPosition.xPosition + x, chunk.chunkPosition.yPosition + y, chunk.chunkPosition.zPosition + z, facing))
                continue

            val indices = ArrayList<Int>(6)

            val sideIndices =
                    arrayOf(
                            3 + 4 * side,
                            1 + 4 * side,
                            0 + 4 * side,
                            3 + 4 * side,
                            2 + 4 * side,
                            1 + 4 * side
                    )

            for (vertexIndex in sideIndices) {
                val vertex = vertices[vertexIndex]
                var finalIndex = chunkVertices.indexOf(vertex)
                if (finalIndex == -1) {
                    chunkVertices.add(vertex)
                    finalIndex = chunkVertices.lastIndex
                }
                indices.add(finalIndex)
            }

            for ((materialIndex, material) in materials[side].withIndex()) {
                material ?: error("Material $materialIndex for side $side not yet loaded!")
                this.processor.onSubmeshCreation(x, y, z, side, materialIndex, material)
                meshes.add(RenderableDefinition.Submesh.builder()
                        .setMaterial(material)
                        .setTriangleIndices(indices)
                        .build())
            }
        }

        return meshes
    }

}

class BlockModel(private val context: Context, metallic: Float, roughness: Float, reflectance: Float, private val textureFutures: List<List<TextureData>>, processor: ISubmeshProcessor, val modelBuilder: Mesher = StupidMesher(textureFutures, context, metallic, roughness, reflectance, processor))

object BlockModelBakery {
    private val modelMap = HashMap<Block, BlockModel>(Blocks.blocks.size)

    fun init(context: Context) {
        for (block in Blocks.blocks) {
            if (block.invisible)
                continue
            val textures = Array<ArrayList<TextureData>>(6) { ArrayList() }
            for ((i, resourceArray) in block.textureResources.withIndex()) {
                for (resource in resourceArray) {
                    textures[i].add(
                            TextureData(
                                    Texture.builder().setSource {
                                        return@setSource context.resources.assets.open("textures/blocks/${resource.textureResource}.png")
                                    }
                                            .setUsage(Texture.Usage.DATA)
                                            .setSampler(Texture.Sampler.builder()
                                                    .setMagFilter(Texture.Sampler.MagFilter.NEAREST)
                                                    .setMinFilter(Texture.Sampler.MinFilter.LINEAR_MIPMAP_LINEAR)
                                                    .build())
                                            .build()
                                            .exceptionally { it.printStackTrace(); null },
                                    resource.transparent
                            )
                    )
                }
            }
            modelMap[block] = BlockModel(
                    context,
                    block.metallic,
                    block.roughness,
                    block.reflectance,
                    textures.toList(),
                    block.submeshProcessor
            )
        }
    }

    fun getModel(block: Block) = modelMap[block]
}