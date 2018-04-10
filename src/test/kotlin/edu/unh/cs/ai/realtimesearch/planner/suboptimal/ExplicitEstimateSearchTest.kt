package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.heavytiles.HeavyTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleTest
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import kotlin.math.roundToInt
import kotlin.test.assertTrue

class ExplicitEstimateSearchTest {
    private val configuration = ExperimentConfiguration("SLIDING_TILE_PUZZLE_4", null, "EES", TerminationType.EXPANSION,
            null, 1L, 1000L, Long.MAX_VALUE, null, 1.0, null, null, null, null,
            null, null, null, null, null, null, errorModel = "path")

    private fun createInstanceFromString(puzzle: String): InputStream {
        val temp = File.createTempFile("tile", ".puzzle")
        temp.deleteOnExit()
        val tileReader = Scanner(puzzle)

        val fileWriter = FileWriter(temp)
        fileWriter.write("4 4\nstarting:\n")
        while (tileReader.hasNext()) {
            val num = tileReader.nextInt().toString()
            fileWriter.write(num + "\n")
        }
        fileWriter.close()
        return temp.inputStream()
    }


    @Test
    fun testEES() {
        val tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent = ExplicitEstimateSearch(slidingTilePuzzle.domain, configuration)
        kotlin.test.assertTrue { eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000)).isEmpty() }
    }

    @Test
    fun testEES2() {
        val tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent = ExplicitEstimateSearch(slidingTilePuzzle.domain, configuration)
        val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 3 }
    }

    @Test
    fun testEES3() {
        val tiles = "4 1 2 3 8 5 6 7 12 9 10 11 13 14 15 0"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent= ExplicitEstimateSearch(slidingTilePuzzle.domain, configuration)
        val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 6 }
    }

    @Test
    fun testEES4() {
        val tiles = "0 4 1 2 8 5 6 3 12 9 10 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent = ExplicitEstimateSearch(slidingTilePuzzle.domain, configuration)
        val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
    }

    @Test
    fun testEES5() {
        val tiles = "4 1 2 3 8 0 10 6 12 5 9 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent = ExplicitEstimateSearch(slidingTilePuzzle.domain, configuration)
        val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
    }

    @Test
    fun testEESKorfProblem() {
        val tiles = "4 1 2 3 8 0 10 6 12 5 9 7 13 14 15 11"
        configuration.weight = 3.0
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent = ExplicitEstimationTildeSearch(slidingTilePuzzle.domain, configuration)
        val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size <= 3.0 * 12 }
        println("Plan size: ${plan.size}")
    }

    @Test
    fun testEESBounds() {
        val optimalSolutionLengths = intArrayOf(57, 55, 59, 56, 56, 52, 52, 50, 46, 59, 57, 45,
                46, 59, 62, 42, 66, 55, 46, 52, 54, 59, 49, 54, 52, 58, 53, 52, 54, 47, 50, 59, 60, 52, 55, 52, 58, 53, 49, 54, 54,
                42, 64, 50, 51, 49, 47, 49, 59, 53, 56, 56, 64, 56, 41, 55, 50, 51, 57, 66, 45, 57, 56, 51, 47, 61, 50, 51, 53, 52,
                44, 56, 49, 56, 48, 57, 54, 53, 42, 57, 53, 62, 49, 55, 44, 45, 52, 65, 54, 50, 57, 57, 46, 53, 50, 49, 44, 54, 57, 54)
        val startingWeight = 3.0
        val stepSize = 0.00
        for (w in 2..2) {
            val currentWeight = startingWeight + (stepSize * w)
            println("running sub-optimality validation on weight: $currentWeight")
            configuration.weight = currentWeight
            for (i in 1..100) {
                print(i.toString() + " ")
                System.out.flush()
                val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
                val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
                val initialState = slidingTilePuzzle.initialState
                val eetsAgent = ExplicitEstimateSearch(slidingTilePuzzle.domain, configuration)
                val plan: List<Action>
                try {
                    plan = eetsAgent.plan(initialState, StaticExpansionTerminationChecker(Long.MAX_VALUE))
                    val pathLength  = plan.size.toLong()
                    var currentState = initialState
                    // valid plan check
                    plan.forEach { action ->
                        currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
                    }
                    assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
                    // sub-optimality bound check
                    if (((optimalSolutionLengths[i - 1] * currentWeight).roundToInt()) < pathLength) {
                        System.err.println("weight: $currentWeight breaks sub-optimality bound on instance $i")
                        System.err.println("${(optimalSolutionLengths[i - 1] * currentWeight).roundToInt()} >= $pathLength")
                        System.err.println("# generated nodes: ${eetsAgent.generatedNodeCount} | # expanded nodes ${eetsAgent.expandedNodeCount}")
                    }
                } catch (e: Exception) {
                    System.err.println(e.message + " on instance $i with weight $currentWeight")
                    System.err.println(e.stackTrace.forEach(::println))
                }

                // assertTrue { (optimalSolutionLengths[i - 1] * currentWeight).roundToInt() >= plan.size }
                // assertEquals((optimalSolutionLengths[i - 1 ] * 1.25).roundToInt(), plan.size, "instance $i")
            }
        }
    }

}