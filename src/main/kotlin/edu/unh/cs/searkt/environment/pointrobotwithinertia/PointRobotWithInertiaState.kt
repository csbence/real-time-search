package edu.unh.cs.searkt.environment.pointrobotwithinertia

import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.util.roundToNearestDecimal

/**
 *
 */
data class PointRobotWithInertiaState(val x: Double, val y: Double, val xdot: Double, val ydot: Double, val stateFraction: Double) : State<PointRobotWithInertiaState> {


    override fun equals(other: Any?): Boolean {
        //        println("" + other + " " + this)
        val fractions = stateFraction // number of values between whole numbers i.e. How many actions should there be in the range [0,1)?

        return when {
            other !is PointRobotWithInertiaState -> false
            roundToNearestDecimal(other.x, fractions) == roundToNearestDecimal(x, fractions) && roundToNearestDecimal(other.y, fractions) == roundToNearestDecimal(y, fractions) && other.xdot == xdot && other.ydot == ydot -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return x.toInt() xor Integer.reverse(y.toInt())
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(x, y, xdot, ydot)
}

