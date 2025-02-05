package edu.unh.cs.searkt.environment.heavytiles

import edu.unh.cs.searkt.environment.location.Location
import java.io.InputStream
import java.util.*

object HeavyTilePuzzleIO {

    fun parseFromStream(input: InputStream, actionDuration: Long): SlidingTilePuzzleInstance {
        val inputScanner = Scanner(input)
        inputScanner.useDelimiter("\n")

        val dimension: Int

        try {
            val dimensions = inputScanner.nextLine().trim().split(" ").map { it.toInt() }

            if (dimensions.size != 2) {
                throw InvalidSlidingTilePuzzleException("Only two dimensional puzzles are supported.")
            }

            if (dimensions[0] != dimensions[1]) {
                throw InvalidSlidingTilePuzzleException("Only square puzzles are supported.")
            }

            dimension = dimensions[0]
            if (dimension != 4) {
                throw RuntimeException("The dimensions of the sliding tile puzzle must be 4 by 4.")
            }

        } catch (e: NoSuchElementException) {
            throw InvalidSlidingTilePuzzleException("SlidingTilePuzzle's first line is missing. The first line should contain the dimensions", e)
        } catch (e: NumberFormatException) {
            throw InvalidSlidingTilePuzzleException("SlidingTilePuzzle's dimensions must be numbers.", e)
        }

        val slidingTilePuzzle = HeavyTilePuzzle(dimension, actionDuration)
        val tiles = ByteArray(16, { 0.toByte() })
        val slidingTilePuzzleState = HeavyTilePuzzle4State(0, tiles, 0.0, 0.0,
                slidingTilePuzzle.calculateHashCode(tiles))

        try {
            val tileList = inputScanner.asSequence().drop(1).take(dimension * dimension).map { it.toInt().toByte() }.toList()
            var zeroLocation: Location? = null
            tileList.forEachIndexed { i, value ->
                val y = i / dimension
                val x = i % dimension

                slidingTilePuzzleState[slidingTilePuzzleState.getIndex(x, y)] = value
                if (value == 0.toByte()) {
                    zeroLocation = Location(x, y)
                }
            }

            val zeroIndex = slidingTilePuzzleState.getIndex(zeroLocation!!.x, zeroLocation!!.y)
            val heuristic = slidingTilePuzzle.initialHeuristic(slidingTilePuzzleState)
            val distance = slidingTilePuzzle.initialDistance(slidingTilePuzzleState)
            val hashCode = slidingTilePuzzle.calculateHashCode(slidingTilePuzzleState.tiles)

            return SlidingTilePuzzleInstance(slidingTilePuzzle, HeavyTilePuzzle4State(zeroIndex,
                    slidingTilePuzzleState.tiles, heuristic, distance, hashCode))
        } catch (e: NumberFormatException) {
            throw InvalidSlidingTilePuzzleException("Tile must be a number.", e)
        }
    }

}

class InvalidSlidingTilePuzzleException(message: String, e: Exception? = null) : RuntimeException(message, e)
data class SlidingTilePuzzleInstance(val domain: HeavyTilePuzzle, val initialState: HeavyTilePuzzle4State)