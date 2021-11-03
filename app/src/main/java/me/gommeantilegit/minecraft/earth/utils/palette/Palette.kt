package me.gommeantilegit.minecraft.earth.utils.palette

import java.lang.IllegalStateException

class Palette<T>(private val possibleStates: List<T>) {

    fun getIndex(value: T): Int {
        val index = possibleStates.indexOf(value)
        if (index == -1) {
            throw IllegalStateException("Cannot retrieve palette index for value not contained in possible states list")
        }
        return index
    }

    fun fromIndex(index: Int): T {
        return possibleStates[index]
    }

}

class PaletteStorage<T>(private val palette: Palette<T>) {


}