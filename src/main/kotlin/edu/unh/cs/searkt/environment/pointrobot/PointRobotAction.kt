package edu.unh.cs.searkt.environment.pointrobot

import edu.unh.cs.searkt.environment.Action

/**
 * This is an action in the point robot domain
 *
 * @param index: the type of action, each return different relative locations
 */
data class PointRobotAction(val xdot: Double, val ydot: Double) : Action {

    override fun toString(): String {
        return "($xdot, $ydot)"
    }
}