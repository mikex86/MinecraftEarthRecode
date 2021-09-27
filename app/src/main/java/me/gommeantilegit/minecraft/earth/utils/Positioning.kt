package me.gommeantilegit.minecraft.earth.utils

import com.google.ar.sceneform.math.Vector3
import me.gommeantilegit.minecraft.earth.world.ChunkPosition
import kotlin.math.floor

data class Vec3i(val x: Int, val y: Int, val z: Int) {
    operator fun plus(vec: Vec3i) = Vec3i(x + vec.x, y + vec.y, vec.z + z)
}

enum class EnumFacing(val index: Int, val facingName: String, val axisDirection: AxisDirection, val axis: Axis, val facingDirection: Vec3i) {
    DOWN(0, "down", AxisDirection.NEGATIVE, Axis.Y, Vec3i(0, -1, 0)),
    UP(1, "up", AxisDirection.POSITIVE, Axis.Y, Vec3i(0, 1, 0)),

    NORTH(2, "north", AxisDirection.NEGATIVE, Axis.Z, Vec3i(0, 0, -1)),
    SOUTH(3, "south", AxisDirection.POSITIVE, Axis.Z, Vec3i(0, 0, 1)),

    WEST(4, "west", AxisDirection.NEGATIVE, Axis.X, Vec3i(-1, 0, 0)),
    EAST(5, "east", AxisDirection.POSITIVE, Axis.X, Vec3i(1, 0, 0));

    companion object {
        fun ofIndex(index: Int): EnumFacing? {
            return values().getOrNull(index)
        }
    }

    enum class AxisDirection(val dir: Int) {
        POSITIVE(1), NEGATIVE(-1)
    }

    enum class Axis(val axisName: String, val plane: Plane) {
        X("x", Plane.HORIZONTAL),
        Y("y", Plane.VERTICAL),
        Z("z", Plane.HORIZONTAL);
    }

    enum class Plane {
        HORIZONTAL,
        VERTICAL
    }
}

data class BlockPos(val x: Int, val y: Int, val z: Int) {

    val chunkPos: ChunkPosition
        get() {
            return ChunkPosition.of(this)
        }

    constructor(vec3i: Vec3i) : this(vec3i.x, vec3i.y, vec3i.z)

    companion object {
        private fun floorCast(value: Float): Int {
            return if (value > 0) value.toInt() else floor(value).toInt()
        }
        fun of(vec: Vector3): BlockPos {
            return BlockPos(floorCast(vec.x), floorCast(vec.y), floorCast(vec.z))
        }
    }

}