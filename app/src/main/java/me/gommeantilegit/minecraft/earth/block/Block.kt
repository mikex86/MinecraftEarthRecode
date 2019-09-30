package me.gommeantilegit.minecraft.earth.block

import android.graphics.BitmapFactory
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import me.gommeantilegit.minecraft.earth.MainActivity
import me.gommeantilegit.minecraft.earth.rendering.ISubmeshProcessor
import me.gommeantilegit.minecraft.earth.rendering.TextureResource

/**
 * @param unlocalizedName a key for block name translation
 * @param id a block unique identifier
 * @param invisible whether the block is invisible or not
 * @param textureResources the side parent textures. Length must be 6
 * 0 -> bottom
 * 1 -> top
 * 2 -> north
 * 3 -> south
 * 4 -> west
 * 5 -> east
 * @see me.gommeantilegit.minecraft.earth.utils.EnumFacing
 */
data class Block(val unlocalizedName: String, val id: Int, val invisible: Boolean, val textureResources: Array<Array<TextureResource>>, val metallic: Float, val roughness: Float, val reflectance: Float, val submeshProcessor: ISubmeshProcessor) {

    constructor(unlocalizedName: String, id: Int, textureResources: Array<Array<TextureResource>>, metallic: Float, roughness: Float, reflectance: Float, submeshProcessor: ISubmeshProcessor = ISubmeshProcessor { _, _, _, _, _, _ -> }) : this(unlocalizedName, id, false, textureResources, metallic, roughness, reflectance, submeshProcessor)

    constructor(unlocalizedName: String, id: Int, textureResource: String, transparent: Boolean, metallic: Float, roughness: Float, reflectance: Float, submeshProcessor: ISubmeshProcessor = ISubmeshProcessor { _, _, _, _, _, _ -> }) : this(unlocalizedName, id, false, arrayOf(arrayOf(TextureResource(textureResource, transparent)), arrayOf(TextureResource(textureResource, transparent)), arrayOf(TextureResource(textureResource, transparent)), arrayOf(TextureResource(textureResource, transparent)), arrayOf(TextureResource(textureResource, transparent)), arrayOf(TextureResource(textureResource, transparent))), metallic, roughness, reflectance, submeshProcessor)

    constructor(unlocalizedName: String, id: Int, invisible: Boolean) : this(unlocalizedName, id, invisible, emptyArray(), 0f, 0f, 0f, ISubmeshProcessor { _, _, _, _, _, _ -> }) {
        if (!invisible)
            error("Use different constructor for non invisible blocks.")
    }

    init {
        if ((textureResources.size != 6 && !invisible) || (textureResources.isNotEmpty() && invisible))
            error("Invalid size of texture resources array for block sides")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Block

        if (unlocalizedName != other.unlocalizedName) return false
        if (id != other.id) return false
        if (!textureResources.contentEquals(other.textureResources)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = unlocalizedName.hashCode()
        result = 31 * result + id
        result = 31 * result + textureResources.contentHashCode()
        return result
    }
}

object Blocks {
    val air = Block("air", 0, true)
    val stone = Block("stone", 1, "stone", false, 0f, 1f, 0f)
    val dirt = Block("dirt", 2, "dirt", false, 0f, 1f, 0f)
    val bedrock = Block("bedrock", 3, "bedrock", false, 0.25f, 1f, 0.15f)
    val grass = Block("grass", 4, arrayOf(arrayOf(TextureResource("dirt", false)), arrayOf(TextureResource("grass_block_top", false)), arrayOf(TextureResource("dirt", false), TextureResource("grass_block_side_overlay", true)), arrayOf(TextureResource("dirt", false), TextureResource("grass_block_side_overlay", true)), arrayOf(TextureResource("dirt", false), TextureResource("grass_block_side_overlay", true)), arrayOf(TextureResource("dirt", false), TextureResource("grass_block_side_overlay", true))), 0f, 1f, 0f, object : ISubmeshProcessor {

        private val colorMap = BitmapFactory.decodeStream(MainActivity.context.assets.open("textures/colormap/grass.png"))

        override fun onSubmeshCreation(x: Int, y: Int, z: Int, sideIndex: Int, materialIndex: Int, material: Material) {
            if (sideIndex != 0 && ((sideIndex == 1 && materialIndex == 0) || (materialIndex == 1 && sideIndex != 1))) {
                material.setFloat4("color", Color(getGrassColor(0.65f, 0.80f)))
            }
        }

        fun getGrassColor(temperature: Float, humidity: Float): Int {
            val value = humidity * temperature
            val x = ((1.0 - temperature) * 255).toInt()
            val y = ((1.0 - value) * 255).toInt()
            return colorMap.getPixel(x, y)
        }
    })
    val log = Block("log", 5, arrayOf(arrayOf(TextureResource("spruce_log_top", false)), arrayOf(TextureResource("spruce_log_top", false)), arrayOf(TextureResource("spruce_log", false)), arrayOf(TextureResource("spruce_log", false)), arrayOf(TextureResource("spruce_log", false)), arrayOf(TextureResource("spruce_log", false))), 0f, 1f, 0f)
    val leaves = Block("leaves", 6, arrayOf(arrayOf(TextureResource("spruce_leaves", false)), arrayOf(TextureResource("spruce_leaves", false)), arrayOf(TextureResource("spruce_leaves", false)), arrayOf(TextureResource("spruce_leaves", false)), arrayOf(TextureResource("spruce_leaves", false)), arrayOf(TextureResource("spruce_leaves", false))), 0f, 1f, 0f, ISubmeshProcessor { _, _, _, _, _, material ->
        //        private val colorMap = BitmapFactory.decodeStream(MainActivity.context.assets.open("textures/colormap/foliage.png"))

        material.setFloat4("color", Color(4764952))
    })
    val diamondOre = Block("diamond_ore", 7, "diamond_ore", false, 0f, 1f, 0f)

    val blocks = listOf(
            air, stone, dirt, bedrock, grass, log, leaves, diamondOre
    )
}