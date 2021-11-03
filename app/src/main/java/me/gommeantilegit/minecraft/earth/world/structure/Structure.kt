package me.gommeantilegit.minecraft.earth.world.structure

import me.gommeantilegit.minecraft.earth.MainActivity.Companion.context
import me.gommeantilegit.minecraft.earth.block.Block
import me.gommeantilegit.minecraft.earth.block.Blocks
import me.gommeantilegit.minecraft.earth.world.Chunk

class Structure(val sizeX: Int, val sizeY: Int, val sizeZ: Int, private val blockArray: Array<Block>) {

    private fun get(x: Int, y: Int, z: Int): Block {
        return blockArray[x * sizeZ + y * sizeZ * sizeX + z]
    }

    fun placeIntoChunk(chunk: Chunk, startX: Int, startY: Int, startZ: Int) {
        if (
                (startX < 0 || startX + sizeX >= Chunk.chunkSize) ||
                (startY < 0 || startY + sizeY >= Chunk.chunkSize) ||
                (startZ < 0 || startZ + sizeZ >= Chunk.chunkSize)
        ) {
            error("Structure would exceed chunk borders at this position")
        }
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                for (z in 0 until sizeZ) {
                    val block = get(x, y, z)
                    if (block != Blocks.air)
                        chunk.setBlock(startX + x, startY + y, startZ + z, block)
                }
            }
        }
    }

    companion object {
        fun ofResource(resourcePath: String): Structure {
            val stream = context.resources.assets.open(resourcePath)
            val reader = stream.reader(Charsets.UTF_8)
            val lines = reader.readLines()
            reader.close()

            var sizeX = -1
            var sizeY = -1
            var sizeZ = -1

            var blocks: Array<MutableList<MutableList<Block>>> = arrayOf()

            val blockSynonymMap = HashMap<Char, Block>()

            var currentSectionName = ""
            var currentSectionArgs: List<String> = listOf()

            for (line in lines) {
                if (line.startsWith(".")) {
                    val sectionString = line.substringAfter(".")
                    val sectionStringSplit = sectionString.split(" ")
                    currentSectionName = sectionStringSplit[0]
                    currentSectionArgs = sectionStringSplit.subList(1, sectionStringSplit.size)
                } else {
                    if (line.startsWith("$")) {
                        val varDeclString = line.substringAfter("$")
                        val varDeclArgs = varDeclString.split("=")
                        val varName = varDeclArgs[0]
                        val varValue = varDeclArgs[1]
                        when (currentSectionName) {
                            "info" -> {
                                when (varName) {
                                    "sizeX" -> sizeX = varValue.toInt()
                                    "sizeY" -> {
                                        sizeY = varValue.toInt()
                                        blocks = Array(sizeY) { mutableListOf<MutableList<Block>>() }
                                    }
                                    "sizeZ" -> sizeZ = varValue.toInt()
                                }
                            }
                            "blocks" -> {
                                blockSynonymMap[varName[0]] = Blocks.byName(varValue)
                                        ?: error("Unknown block: $varValue")
                            }
                        }
                    } else if (line.startsWith("|")) {
                        when (currentSectionName) {
                            "layer" -> {
                                if (sizeX == -1 || sizeY == -1 || sizeZ == -1) {
                                    error("Size of structure must be defined before a block state layer is declared")
                                }
                                val yPos = currentSectionArgs[0].toInt()
                                val lineArgs = line.split("|")

                                // skip first arg because it is empty because the line starts with '|'
                                val blockLine = mutableListOf<Block>()
                                for (arg in lineArgs) {
                                    if (arg.isEmpty()) {
                                        continue
                                    }
                                    val c = arg[0]
                                    val block = if (c == ' ') Blocks.air else blockSynonymMap[c]
                                            ?: error("Unknown block synonym: $arg")
                                    blockLine.add(block)
                                }
                                blocks[yPos].add(blockLine)
                            }
                        }
                    }
                }
            }

            val blockArray = Array(sizeX * sizeY * sizeZ) { Blocks.air }
            for (x in 0 until sizeX) {
                for (y in 0 until sizeY) {
                    for (z in 0 until sizeZ) {
                        val block = blocks[y][z][x]
                        blockArray[x * sizeZ + y * sizeZ * sizeX + z] = block
                    }
                }
            }
            return Structure(sizeX, sizeY, sizeZ, blockArray)
        }
    }
}