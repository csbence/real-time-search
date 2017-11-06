package edu.unh.cs.ai.realtimesearch.environment.location

import java.lang.Math.abs

/**
 * This represents a grid location, defined by its x and y coordinate.
 *
 * @param x: x index of the location
 * @param y: y index of the location
 */
data class Location(val x: Int, val y: Int) {
    operator fun plus(rhs: Location): Location = Location(x + rhs.x, y + rhs.y)

    operator fun minus(rhs: Location): Location = Location(x - rhs.x, y - rhs.y)

    fun manhattanDistance(other: Location): Int = abs(x - other.x) + abs(y - other.y)

    override fun equals(other: Any?): Boolean = other is Location && other.x == x && other.y == y

    override fun hashCode(): Int = x xor Integer.rotateLeft(y, 16)

    /**
     * Check if location is inside the boundaries.
     * The lower bound is inclusive the upper bound is exclusive.
     */
    fun inBounds(upperBound: Int, lowerBound: Int = 0): Boolean =
            x >= lowerBound && y >= lowerBound && x < upperBound && y < upperBound

    fun toDoubleLocation(): DoubleLocation = DoubleLocation(x.toDouble(), y.toDouble())
}