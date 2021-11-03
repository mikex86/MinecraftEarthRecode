package me.gommeantilegit.minecraft.earth.utils

import kotlin.math.ceil
import kotlin.math.floor


fun floorCeil(value: Float): Int {
    return if (value > 0) ceil(value).toInt() else floor(value).toInt()
}

fun ceilDiv(a: Int, b: Int): Int {
    return (a / b) + if (a % b == 0) 0 else 1
}