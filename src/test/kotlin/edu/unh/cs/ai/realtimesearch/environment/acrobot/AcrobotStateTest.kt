package edu.unh.cs.ai.realtimesearch.environment.acrobot

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcrobotStateTest {

    @Test
    fun testEnergy() {
        // Starting state; no energy has been injected into the system
        val state = AcrobotState(0.0, 0.0, 0.0, 0.0)

        assertTrue { doubleNearEquals(state.kineticEnergy, 0.0) }
        assertTrue { doubleNearEquals(state.potentialEnergy, 0.0) }
        assertTrue { doubleNearEquals(state.totalEnergy, 0.0) }
    }

    @Test
    fun testAcceleration() {
        val state = initialAcrobotState

        val (accelerationNone1, accelerationNone2) = state.calculateLinkAccelerations(AcrobotAction.NONE)
        assertTrue { doubleNearEquals(accelerationNone1, 0.0) }
        assertTrue { doubleNearEquals(accelerationNone2, 0.0) }

        // Can't guarantee
//        val (accelerationPositive1, accelerationPositive2) = state.calculateLinkAccelerations(AcrobotAction.POSITIVE)
//        assertTrue { accelerationPositive1 > 0.0 }
//        assertTrue { accelerationPositive2 > 0.0 }
//
//        val (accelerationNegative1, accelerationNegative2) = state.calculateLinkAccelerations(AcrobotAction.NEGATIVE)
//        assertTrue { accelerationNegative1 < 0.0 }
//        assertTrue { accelerationNegative2 < 0.0 }
    }

    @Test
    fun testBounds() {
        val lowerBound = AcrobotState(0.0, Math.PI / 2, -1.0, 1.0)
        val upperBound = lowerBound + AcrobotState(Math.PI, Math.PI, Math.PI, Math.PI)
        val state1 = AcrobotState(0.1, 0.1, 0.1, 0.1)
        val state2 = lowerBound + state1
        val state3 = upperBound - state1

        assertTrue { state2.inBounds(lowerBound, upperBound) }
        assertTrue { state3.inBounds(lowerBound, upperBound) }
        assertFalse { state1.inBounds(lowerBound, upperBound) }
    }
}