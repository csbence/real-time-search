package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.State
import java.util.*

/**
 * State of a sliding tile puzzle.
 *
 *  width (x)
 * -------
 * |0|1|2|
 * |3|4|5| height(y)
 * |6|7|8|
 * -------
 *
 * (0, 0) == 0
 * (1, 0) == 1
 * (0, 1) == 3
 *
 */
data class SlidingTilePuzzleDynamicState(val zeroX: Int, val zeroY: Int, val tiles: SlidingTilePuzzleDynamicState.Tiles, val heuristic: Double) : State<SlidingTilePuzzleDynamicState> {
    private val hashCode: Int = calculateHashCode()

    private fun calculateHashCode(): Int {
        var hashCode: Int = tiles.hashCode()
        return hashCode xor zeroX xor zeroY
    }

    override fun copy(): SlidingTilePuzzleDynamicState {
        return SlidingTilePuzzleDynamicState(zeroX, zeroY, tiles.copy(), heuristic)
    }

    class Tiles(val dimension: Int, tiles: ByteArray? = null) {
        val tiles: ByteArray

        init {
            this.tiles = tiles ?: ByteArray(dimension * dimension)
        }

        fun copy(): Tiles {
            return Tiles(dimension, tiles.clone())
        }

        override fun hashCode(): Int {
            var hashCode = 0

            tiles.forEach {
                hashCode = Integer.rotateLeft(hashCode, 1) xor it.toInt()
            }

            return hashCode xor dimension
        }

        override fun equals(other: Any?): Boolean {
            return when {
                other == null -> false
                other === this -> true
                other !is Tiles -> false
                else -> dimension == other.dimension && Arrays.equals(this.tiles, other.tiles)
            }
        }

        fun getIndex(x: Int, y: Int): Int {
            return dimension * y + x
        }

        operator fun get(index: Int): Byte {
            return tiles[index]
        }

        operator fun set(index: Int, value: Byte) {
            tiles[index] = value
        }

        fun get(x: Int, y: Int): Byte {
            return tiles[y * dimension + x]
        }

        fun set(x: Int, y: Int, value: Byte) {
            tiles[y * dimension + x] = value
        }

        //        operator fun get(location: Location): Byte {
        //            return tiles[location.y * dimension + location.x]
        //        }
        //
        //        operator fun set(location: Location, value: Byte) {
        //            tiles[location.y * dimension + location.x] = value
        //        }

        override fun toString(): String {
            return "Tiles(dimension = $dimension)"
        }
    }

    override fun hashCode(): Int {
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other !is SlidingTilePuzzleDynamicState -> false
            else -> zeroX == other.zeroX && zeroY == other.zeroY && tiles == other.tiles
        }
    }
}

fun tiles(size: Int, init: SlidingTilePuzzleDynamicState.Tiles.() -> Unit): SlidingTilePuzzleDynamicState.Tiles {
    val internalTiles = ByteArray(size * size)
    internalTiles.forEachIndexed { i, _ -> internalTiles[i] = -1 }
    val tiles = SlidingTilePuzzleDynamicState.Tiles(size, internalTiles)

    tiles.init()
    return tiles
}

fun SlidingTilePuzzleDynamicState.Tiles.row(vararg args: Int) {
    val minusOne: Byte = -1
    val index = tiles.indexOfFirst { it == minusOne }
    val row = args.map(Int::toByte).toByteArray()

    System.arraycopy(row, 0, this.tiles, index, this.dimension)
}




