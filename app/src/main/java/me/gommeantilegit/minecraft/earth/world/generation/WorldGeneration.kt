package me.gommeantilegit.minecraft.earth.world.generation

import me.gommeantilegit.minecraft.earth.block.Blocks
import me.gommeantilegit.minecraft.earth.world.BlockState
import me.gommeantilegit.minecraft.earth.world.Chunk

interface IWorldGenerator {

    fun generate(chunk: Chunk)

}

class SuperFlatChunkGenerator : IWorldGenerator {

    override fun generate(chunk: Chunk) {
        for (x in 0 until Chunk.chunkSize) {
            for (z in 0 until Chunk.chunkSize) {
                chunk.setBlock(x, 0, z, BlockState(Blocks.grass))
            }
        }
    }

}