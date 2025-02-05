package edu.unh.cs.searkt.planner.suboptimal

import edu.unh.cs.searkt.environment.Action
import edu.unh.cs.searkt.environment.heavytiles.HeavyTilePuzzleIO
import edu.unh.cs.searkt.environment.pancake.PancakeIO
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.searkt.planner.Planners
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import kotlin.math.roundToInt
import kotlin.test.assertTrue

class WeightedAStarTest {
//    private val configuration = ExperimentConfiguration("SLIDING_TILE_PUZZLE_4", null, "WEIGHTED_A_STAR", TerminationType.EXPANSION, "input/tiles/korf/4/real/12", 1000000L, 1000L, 1000000L,
//            null, 3.0, LookaheadType.STATIC, CommitmentStrategy.SINGLE, null, null, null, null, null, null, null, null)

    private val configuration = ExperimentConfiguration(domainName = "SLIDING_TILE_PUZZLE_4", algorithmName = Planners.WEIGHTED_A_STAR,
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L,
            expansionLimit = 1000000000L, weight = 1.0)

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

//    @Test
//    fun testRunTime() {
//        var plan: List<*> = emptyList<Action>()
//        val tiles = "14 1 9 6 4 8 12 5 7 2 3 0 10 11 13 15"
//        val instance = createInstanceFromString(tiles)
//        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
//        val initialState = slidingTilePuzzle.initialState
//        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
//        val runTime = measureNanoTime {
//            plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(10L))
//        }
//        val result = executeConfiguration(configuration)
//        println("$runTime >= ${result.experimentRunTime}")
//        println("diff ${result.experimentRunTime - runTime}")
//        println(plan)
//        kotlin.test.assertTrue { plan.isNotEmpty() }
//         kotlin.test.assertTrue { plan.size == 12 }
//
//    }

//    @Test
//    fun testAStar1() {
//        val tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
//        val instance = createInstanceFromString(tiles)
//        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
//        val initialState = slidingTilePuzzle.initialState
//        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
//        kotlin.test.assertTrue { aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L)).isEmpty() }
//        println("timeTaken:${aStarAgent.executionNanoTime}")
//    }

//    @Test
//    fun testAStar2() {
//        val tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
//        val instance = createInstanceFromString(tiles)
//        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
//        val initialState = slidingTilePuzzle.initialState
//        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
//        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//        println(plan)
//        kotlin.test.assertTrue { plan.isNotEmpty() }
//        kotlin.test.assertTrue { plan.size == 3 }
//    }

//    @Test
//    fun testAStar3() {
//        val tiles = "4 1 2 3 8 5 6 7 12 9 10 11 13 14 15 0"
//        val instance = createInstanceFromString(tiles)
//        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
//        val initialState = slidingTilePuzzle.initialState
//        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
//        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//        println(plan)
//        kotlin.test.assertTrue { plan.isNotEmpty() }
//        kotlin.test.assertTrue { plan.size == 6 }
//    }

//    @Test
//    fun testAStar4() {
//        val tiles = "0 4 1 2 8 5 6 3 12 9 10 7 13 14 15 11"
//        val instance = createInstanceFromString(tiles)
//        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
//        val initialState = slidingTilePuzzle.initialState
//        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
//        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//        println(plan)
//        kotlin.test.assertTrue { plan.isNotEmpty() }
//        kotlin.test.assertTrue { plan.size == 12 }
//    }

//    @Test
//    fun testAStar5() {
//        val tiles = "4 1 2 3 8 0 10 6 12 5 9 7 13 14 15 11"
//        val instance = createInstanceFromString(tiles)
//        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
//        val initialState = slidingTilePuzzle.initialState
//        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
//        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//        println(plan)
//        println("timeTaken:${aStarAgent.executionNanoTime}")
//        kotlin.test.assertTrue { plan.isNotEmpty() }
//        kotlin.test.assertTrue { plan.size == 12 }
//    }

//    @Test
//    fun testAStar6() {
//        val weight = 1.0
//        configuration.weight = weight
//        val optimalSolutionLengths = intArrayOf(57, 55, 59, 56, 56, 52, 52, 50, 46, 59, 57, 45)
//        for (i in 12 until 13) {
//            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
//            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
//            val initialState = slidingTilePuzzle.initialState
//            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
//            val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//            var currentState = initialState
//            plan.forEach { action ->
//                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
//            }
//            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
//            assertEquals(optimalSolutionLengths[i - 1], plan.size, "instance $i")
//        }
//    }

    @Test
    fun testXDPSuboptimality() {
        val startingWeight = 0.9
        val stepSize = 0.1


        var times = 0
        var aStarExpansionSum = 0
        var xdpExpansionSum = 0
        var subPExpansionSum = 0

        for (w in 1..100) {
            val currentWeight = startingWeight + (stepSize * w)
            println("running sub-optimality validation on weight: $currentWeight")
            configuration.weight = currentWeight

            for (i in 0..10) {
                val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/pancake/$i.pqq")
                val pancakeProblem = PancakeIO.parseFromStream(stream, 1L)
                val initialState = pancakeProblem.initialState

//                try {
                var currentState = initialState
                // run A* then use solution with validation
                configuration.weight = 1.0
                configuration.algorithmName = Planners.WEIGHTED_A_STAR
                val aStarAgent = WeightedAStar(pancakeProblem.domain, configuration)
                val aStarPlan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(10000000))
                // A* plan check
                aStarPlan.forEach { action ->
                    currentState = pancakeProblem.domain.successors(currentState).first { it.action == action }.state
                }
                assertTrue { pancakeProblem.domain.heuristic(currentState) == 0.0 }
                println("\tA* expanded ${aStarAgent.expandedNodeCount}")

                currentState = initialState
                // then run WA*_XDP and check suboptimality
                configuration.weight = currentWeight
                configuration.algorithmName = Planners.WEIGHTED_A_STAR_XDP
                val xdpAgent = WeightedAStar(pancakeProblem.domain, configuration)
                val xdpPlan = xdpAgent.plan(initialState, StaticExpansionTerminationChecker(10000000))
                // XDP plan check
                xdpPlan.forEach { action ->
                    currentState = pancakeProblem.domain.successors(currentState).first { it.action == action }.state
                }
                assertTrue { pancakeProblem.domain.heuristic(currentState) == 0.0 }
                println("\tXDP expanded ${xdpAgent.expandedNodeCount}")

                currentState = initialState
                // then run SubPotential
                configuration.weight = currentWeight
                configuration.algorithmName = Planners.SUBPOTENTIAL
                val subPAgent = SubPotential(pancakeProblem.domain, configuration)
                val subPPlan = subPAgent.plan(initialState, StaticExpansionTerminationChecker(10000000))
                // SubP plan check
                subPPlan.forEach { action ->
                    currentState = pancakeProblem.domain.successors(currentState).first { it.action == action }.state
                }
                assertTrue { pancakeProblem.domain.heuristic(currentState) == 0.0 }
                println("\tSubP expanded ${subPAgent.expandedNodeCount}")


                assertTrue(subPPlan.size <= currentWeight * aStarPlan.size, message = "SubP not within suboptimality bound on instance $i with weight $currentWeight and A* solution ${aStarPlan.size}: " +
                        "${subPPlan.size} <= ${currentWeight * aStarPlan.size} is False")
                println("\t\t${subPPlan.size} <= ${currentWeight * aStarPlan.size}")

                assertTrue(xdpPlan.size <= currentWeight * aStarPlan.size, message = "XDP not within suboptimality bound on instance $i with weight $currentWeight and A* solution ${aStarPlan.size}: " +
                        "${xdpPlan.size} <= ${currentWeight * aStarPlan.size} is False")
                println("\t\t${xdpPlan.size} <= ${currentWeight * aStarPlan.size}")


                aStarExpansionSum += aStarAgent.expandedNodeCount
                xdpExpansionSum += xdpAgent.expandedNodeCount
                subPExpansionSum += subPAgent.expandedNodeCount
                times++
//                } catch (e: Exception) {
//                    System.err.println(e.message + " on instance $i with weight $currentWeight")
//                }
            }
        }
        println("A* average expansions: ${aStarExpansionSum / times}")
        println("XDP average expansions: ${xdpExpansionSum / times}")
        println("SubP average expansions: ${subPExpansionSum / times}")
    }

    @Test
    fun testAStar7() {
        val optimalSolutionLengths = intArrayOf(57, 55, 59, 56, 56, 52, 52, 50, 46, 59, 57, 45,
                46, 59, 62, 42, 66, 55, 46, 52, 54, 59, 49, 54, 52, 58, 53, 52, 54, 47, 50, 59, 60, 52, 55, 52, 58, 53, 49, 54, 54,
                42, 64, 50, 51, 49, 47, 49, 59, 53, 56, 56, 64, 56, 41, 55, 50, 51, 57, 66, 45, 57, 56, 51, 47, 61, 50, 51, 53, 52,
                44, 56, 49, 56, 48, 57, 54, 53, 42, 57, 53, 62, 49, 55, 44, 45, 52, 65, 54, 50, 57, 57, 46, 53, 50, 49, 44, 54, 57, 54)
        val startingWeight = 2.00
        val stepSize = 0.0
        for (w in 1..1) {
            val currentWeight = startingWeight + (stepSize * w)
            println("running sub-optimality validation on weight: $currentWeight")
            configuration.weight = currentWeight
            for (i in 1..1) {
                val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
                val slidingTilePuzzle = HeavyTilePuzzleIO.parseFromStream(stream, 1L)
                val initialState = slidingTilePuzzle.initialState
                val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
                val plan: List<Action>
                try {
                    plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(10000000))
                    val pathLength = plan.size.toLong()
                    var currentState = initialState
                    // valid plan check
                    plan.forEach { action ->
                        currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
                    }
                    assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
                    println(aStarAgent.expandedNodeCount)
                    println(plan.size)
                    // sub-optimality bound check
                    if (((optimalSolutionLengths[i - 1] * currentWeight).roundToInt()) < pathLength) {
                        System.err.println("weight: $currentWeight breaks sub-optimality bound on instance $i")
                        System.err.println("${(optimalSolutionLengths[i - 1] * currentWeight).roundToInt()} >= $pathLength")
                    }
                } catch (e: Exception) {
                    System.err.println(e.message + " on instance $i with weight $currentWeight")
                }

                // assertTrue { (optimalSolutionLengths[i - 1] * currentWeight).roundToInt() >= plan.size }
                // assertEquals((optimalSolutionLengths[i - 1 ] * 1.25).roundToInt(), plan.size, "instance $i")
            }
        }
    }

//    @Test
//    fun testAStar7() {
//        val weight = 1.0
//        val configuration = ExperimentConfiguration("SLIDING_TILE_PUZZLE_4", null, "WEIGHED_A_STAR", TerminationType.EXPANSION,
//            null, 1L, 1000L, 1000000L, null, weight, null, null, null, null,
//            null, null, null, null, null, null)
//
//        var experimentNumber = 0
//        val instanceNumbers = intArrayOf(42, 47, 55)
//        val optimalSolutionLengths = intArrayOf(42, 47, 41)
//        for (i in instanceNumbers) {
//            print("Executing $i...")
//            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
//            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
//            val initialState = slidingTilePuzzle.initialState
//            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
//            val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//            var currentState = initialState
//            plan.forEach { action ->
//                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
//            }
//            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
//            assertEquals(optimalSolutionLengths[experimentNumber], plan.size, "instance $i")
//            println("total time: ${aStarAgent.executionNanoTime}")
//            experimentNumber++
//        }
//    }

//    @Test
//    fun testAStarHardPuzzle() {
//        val weight = 1.5
//        configuration.weight = weight
//
//        val configuration = GeneralExperimentConfiguration(mutableMapOf(Configurations.WEIGHT.toString() to weight))
//
//        val instanceNumbers = intArrayOf(1, 3)
//        val optimalSolutionLengths = intArrayOf(57, 59)
//
//        for ((experimentNumber, i) in instanceNumbers.withIndex()) {
//            print("Executing $i...")
//            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
//            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
//            val initialState = slidingTilePuzzle.initialState
//
//            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
//            val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//            var currentState = initialState
//            plan.forEach { action ->
//                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
//            }
//            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
//            print("...plan size: ${plan.size}...")
//            assertTrue { optimalSolutionLengths[experimentNumber] * weight >= plan.size }
//            print("nodes expanded: ${aStarAgent.expandedNodeCount}...")
//            print("nodes generated: ${aStarAgent.generatedNodeCount}...")
//            println("total time: ${aStarAgent.executionNanoTime}")
//        }
//    }


//    @Test
//    fun testWeightedAStarGridWorld1() {
//        val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/vacuum/cups.vw")
//        val gridWorld = GridWorldIO.parseFromStream(stream, 1L)
//        val initialState = gridWorld.initialState
//        val aStarAgent = WeightedAStar(gridWorld.domain, configuration)
//        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//        var currentState = initialState
//        plan.forEach { action ->
//            currentState = gridWorld.domain.successors(currentState).first { it.action == action }.state
//        }
//        println(plan)
//        println(plan.size)
//    }
//
//
//    @Test
//    fun testWeightedAStarGridWorld2() {
//        val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/vacuum/maze.vw")
//        val gridWorld = GridWorldIO.parseFromStream(stream, 1L)
//        val initialState = gridWorld.initialState
//        val aStarAgent = WeightedAStar(gridWorld.domain, configuration)
//        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//        var currentState = initialState
//        plan.forEach { action ->
//            currentState = gridWorld.domain.successors(currentState).first { it.action == action }.state
//        }
//        println(plan)
//        println(plan.size)
//    }

}
