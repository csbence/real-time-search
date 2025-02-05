package edu.unh.cs.searkt.environment.gridworld

import edu.unh.cs.searkt.environment.location.Location
import edu.unh.cs.searkt.environment.vacuumworld.InvalidVacuumWorldException
import java.io.InputStream
import java.util.*

object GridMapIO {
    fun parseFromStream(map: InputStream, instances: InputStream, seed: Long?, actionDuration: Long): GridWorldInstance {
        if (seed == null) throw InvalidVacuumWorldException("Invalid configuration. No seed was provided.")
        if (seed < 0) throw InvalidVacuumWorldException("Invalid seed in configuration. Seed must be positive: $seed")

        val instanceScanner = Scanner(instances)
        instanceScanner.nextLine() // Skip header

        if (!instanceScanner.hasNext()) throw InvalidVacuumWorldException("Invalid instance file. No instance configurations has found")

        // Skip to the desired configuration
        (0 until seed).forEach { i ->
            instanceScanner.hasNext()
            if (!instanceScanner.hasNext()) throw InvalidVacuumWorldException("Invalid map seed ($seed). Only ${i + 1} instances found.")
        }

        val instanceLine = instanceScanner.nextLine()
        val instanceValues = instanceLine.trim().split("\\s+".toRegex())
        if (instanceValues.size != 9) throw InvalidVacuumWorldException("Unexpected number of values (${instanceValues.size}) found for instance $seed")

        val width = instanceValues[2].toInt()
        val height = instanceValues[3].toInt()

        val startX = instanceValues[4].toInt()
        val startY = instanceValues[5].toInt()

        val endX = instanceValues[6].toInt()
        val endY = instanceValues[7].toInt()

        val inputScanner = Scanner(map)

        try {
            val (movementTypeKey, _) = inputScanner.nextLine().trim().split("\\s+".toRegex()).map { it.trim() }
            val (heightKey, heightValue) = inputScanner.nextLine().trim().split("\\s+".toRegex()).map { it.trim() }
            val (widthKey, widthValue) = inputScanner.nextLine().trim().split("\\s+".toRegex()).map { it.trim() }
            val mapMarker = inputScanner.nextLine().trim()

            if (!movementTypeKey.equals("type", ignoreCase = true)) throw InvalidVacuumWorldException("Unexpected error when parsing movement type from map. Found ($movementTypeKey) instead of (type)")
            if (!heightKey.equals("height", ignoreCase = true)) throw InvalidVacuumWorldException("Unexpected error when parsing height from map. Found ($heightKey) instead of (height)")
            if (!widthKey.equals("width", ignoreCase = true)) throw InvalidVacuumWorldException("Unexpected error when parsing width from map. Found ($widthKey) instead of (width)")
            if (!mapMarker.equals("map", ignoreCase = true)) throw InvalidVacuumWorldException("Unexpected error when parsing map. Missing map marker.")

            val mapWidth = widthValue.toInt()
            val mapHeight = heightValue.toInt()

            if (width > mapWidth) throw InvalidVacuumWorldException("Instance width can't be larger that the map's actual width.")
            if (height > mapHeight) throw InvalidVacuumWorldException("Instance height can't be larger that the map's actual width.")

        } catch (e: NoSuchElementException) {
            throw InvalidVacuumWorldException("Missing map header.", e)
        } catch (e: NumberFormatException) {
            throw InvalidVacuumWorldException("Unexpected value. The map width and height must be integers.", e)
        }

        val blockedCells = arrayListOf<Location>()

        try {
            for (y in 0 until height) {
                val line = inputScanner.nextLine()

                for (x in 0 until width) {
                    when (line[x]) {
                        '#', '@', 'T' -> blockedCells.add(Location(x, y))
                    }
                }
            }
        } catch (e: NoSuchElementException) {
            throw InvalidVacuumWorldException("Map is not complete.", e)
        }

        val gridWorld = GridWorld(width, height, blockedCells.toHashSet(), Location(endX, endY), actionDuration)
        val startState = GridWorldState(Location(startX, startY))

        return GridWorldInstance(gridWorld, startState)
    }
}

