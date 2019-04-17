package edu.unh.cs.searkt.environment.acrobot

import edu.unh.cs.searkt.environment.Action

/**
 * An action in the Acrobot domain.  Valid actions are a positive torque, negative torque, or no torque applied to
 * joint 2.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 */
enum class AcrobotAction : Action {
    NONE(0.0), POSITIVE(1.0), NEGATIVE(-1.0);

    val torque: Double

    constructor(torque: Double) {
        this.torque = torque
    }
}