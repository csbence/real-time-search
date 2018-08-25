package edu.unh.cs.ai.realtimesearch.environment.heavyvacuumworld

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * A state in the vacuumworld is simply the current location of the agent,
 * and the location of all dirty cells. The blocked cells are global.
 */
data class HeavyVacuumWorldState(val agentLocation: Location, val dirtyCells: List<Location>,
                                 val heuristic: Double) : State<HeavyVacuumWorldState> {
    private val hashCode: Int = calculateHashCode()

    private fun calculateHashCode(): Int {
        var hashCode: Int = dirtyCells.size
        return agentLocation.hashCode() xor hashCode
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other !is HeavyVacuumWorldState -> false
            else -> agentLocation == other.agentLocation && this.dirtyCells == other.dirtyCells
        }
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(agentLocation, dirtyCells)

    override fun toString(): String {
        return "Agent location: " + agentLocation.toString() + ", dirty: " + dirtyCells.toString()
    }

}

