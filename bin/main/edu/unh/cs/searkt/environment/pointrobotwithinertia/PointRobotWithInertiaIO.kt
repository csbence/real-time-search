package edu.unh.cs.searkt.environment.pointrobotwithinertia

import edu.unh.cs.searkt.environment.pointrobot.PointRobotIO
import java.io.InputStream
import java.util.*

object PointRobotWithInertiaIO {

    fun parseFromStream(input: InputStream, numAction: Int,
                        actionFraction: Double, stateFraction: Double, actionDuration: Long): PointRobotWithInertiaInstance {
        val inputScanner = Scanner(input)

        val header = PointRobotIO.parseHeader(inputScanner)
        val mapInfo = PointRobotIO.parseMap(inputScanner, header)

        val startLocation = mapInfo.startCells.first()
        val endLocation = mapInfo.endCells.first()

        val pointRobotWithInertia = PointRobotWithInertia(header.columnCount, header.rowCount,
                mapInfo.blockedCells.toHashSet(), endLocation, header.goalRadius, numAction, actionFraction, stateFraction, actionDuration)
        val startState = PointRobotWithInertiaState(startLocation.x, startLocation.y, 0.0, 0.0, actionFraction)
        return PointRobotWithInertiaInstance(pointRobotWithInertia, startState)
    }
}

data class PointRobotWithInertiaInstance(val domain: PointRobotWithInertia, val initialState: PointRobotWithInertiaState)

class InvalidPointRobotWithInertiaException(message: String, e: Exception? = null) : RuntimeException(message, e)