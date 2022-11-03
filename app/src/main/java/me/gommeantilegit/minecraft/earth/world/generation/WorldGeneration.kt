package me.gommeantilegit.minecraft.earth.world.generation

import me.gommeantilegit.minecraft.earth.block.Blocks
import me.gommeantilegit.minecraft.earth.world.BlockState
import me.gommeantilegit.minecraft.earth.world.Chunk
import me.gommeantilegit.minecraft.earth.world.structure.Structure
import kotlin.random.Random

private val random = Random(System.currentTimeMillis())

interface IWorldGenerator {

    fun generate(chunk: Chunk)

}

open class GeneratorSequence(private val generators: List<IWorldGenerator>) : IWorldGenerator {

    override fun generate(chunk: Chunk) {
        generators.forEach { generator -> generator.generate(chunk) }
    }

}

class DefaultWorldGenerator : GeneratorSequence(listOf(
        // surface
        SurfaceGenerator(),

        // decorators
        TreeGenerator()
))

class SurfaceGenerator : IWorldGenerator {

    override fun generate(chunk: Chunk) {
        for (x in 0 until Chunk.chunkSize) {
            for (z in 0 until Chunk.chunkSize) {
                chunk.setBlockWithLocalCoords(x, 0, z, BlockState(Blocks.grass))
            }
        }
    }

}

class TreeGenerator : IWorldGenerator {

    private val treeStructure = Structure.ofResource("structures/trees/tree1.bs")

    override fun generate(chunk: Chunk) {
        val xTreePos = 1 + random.nextInt(Chunk.chunkSize - treeStructure.sizeX - 1)
        val zTreePos = 1 + random.nextInt(Chunk.chunkSize - treeStructure.sizeZ - 1)
        val yTreePos = 1
        treeStructure.placeIntoChunk(chunk, xTreePos, yTreePos, zTreePos)
    }

}