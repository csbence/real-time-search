package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.lazyData
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.TIME
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

/**
 * An RTS experiment repeatedly queries the planner
 * for an action by some constraint (allowed time for example).
 * After each selected action, the experiment then applies this action
 * to its environment.
 *
 * The states are given by the environment, the domain. When creating the domain
 * it might be possible to determine what the initial state is.
 *
 * NOTE: assumes the same domain is used to create both the planner as the domain
 *
 * @param planner is a RTS planner that will supply the actions
 * @param domain is the environment
 * @param terminationChecker controls the constraint put upon the planner
 */
class RealTimeExperiment<StateType : State<StateType>>(val configuration: GeneralExperimentConfiguration,
                                                       val planner: RealTimePlanner<StateType>,
                                                       val domain: Domain<StateType>,
                                                       val initialState: StateType,
                                                       val terminationChecker: TerminationChecker) : Experiment() {

    private val logger = LoggerFactory.getLogger(RealTimeExperiment::class.java)
    private val commitmentStrategy by lazyData<String>(configuration, COMMITMENT_STRATEGY.toString())
    private val actionDuration by lazyData<Long>(configuration, ACTION_DURATION.toString())
    private val expansionLimit by lazyData<Long>(configuration, EXPANSION_LIMIT.toString())
    private val stepLimit by lazyData<Long>(configuration, STEP_LIMIT.toString())

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<Action> = arrayListOf()
        logger.debug { "Starting experiment from state $initialState" }
        var currentState: StateType = initialState

        var totalPlanningNanoTime = 0L
        var iterationCount = 0L
        val singleStepLookahead = CommitmentStrategy.valueOf(commitmentStrategy) == CommitmentStrategy.SINGLE

        var timeBound = actionDuration
        var actionList: List<RealTimePlanner.ActionBundle> = listOf()

        while (!domain.isGoal(currentState)) {
            val iterationNanoTime = measureThreadCpuNanoTime {
                terminationChecker.resetTo(timeBound)

                iterationCount++
                actionList = planner.selectAction(currentState, terminationChecker)

                if (actionList.size > 1 && singleStepLookahead) {
                    actionList = listOf(actionList.first()) // Trim the action list to one item
                }
            }

            timeBound = 0
            actionList.forEach {
                currentState = domain.transition(currentState, it.action) ?: return ExperimentResult(experimentConfiguration = configuration.valueStore, errorMessage = "Invalid transition. From $currentState with ${it.action}")// Move the planner
                actions.add(it.action) // Save the action
                timeBound += it.duration // Add up the action durations to calculate the time bound for the next iteration
            }

            logger.debug { "Agent return actions: |${actionList.size}| to state $currentState" }
//            println(domain.print(currentState))
//            Thread.sleep(1000)
            validateIteration(actionList, iterationNanoTime)

            totalPlanningNanoTime += iterationNanoTime

            if (expansionLimit <= planner.expandedNodeCount) {
                return ExperimentResult(experimentConfiguration = configuration.valueStore, errorMessage = "The planner exceeded the expansion limit: $expansionLimit")
            }

            if (stepLimit <= actions.size) {
                return ExperimentResult(experimentConfiguration = configuration.valueStore, errorMessage = "The planner exceeded the step limit: $stepLimit")
            }
        }

        val pathLength: Long = actions.size.toLong()
        val totalExecutionNanoDuration = pathLength * actionDuration
        val goalAchievementTime = when (TerminationType.valueOf(configuration.terminationType)) {
            TIME -> actionDuration + totalExecutionNanoDuration // We only plan during execution plus the first iteration
            EXPANSION -> actionDuration + pathLength * actionDuration
            else -> throw MetronomeException("Unsupported termination checker")
        }

        logger.info {
            "Path length: [$pathLength] After ${planner.expandedNodeCount} expanded " +
                    "and ${planner.generatedNodeCount} generated nodes in $totalPlanningNanoTime ns. " +
                    "(${planner.expandedNodeCount / convertNanoUpDouble(totalPlanningNanoTime, TimeUnit.SECONDS)} expanded nodes per sec)"
        }

        val experimentResult = ExperimentResult(
                configuration = configuration.valueStore,
                expandedNodes = planner.expandedNodeCount,
                generatedNodes = planner.generatedNodeCount,
                planningTime = totalPlanningNanoTime,
                iterationCount = iterationCount,
                actionExecutionTime = totalExecutionNanoDuration,
                goalAchievementTime = goalAchievementTime,
                idlePlanningTime = actionDuration,
                pathLength = pathLength,
                actions = actions.map(Action::toString))

        domain.appendDomainSpecificResults(experimentResult)
        return experimentResult
    }

    private fun validateIteration(actionList: List<RealTimePlanner.ActionBundle>, iterationNanoTime: Long) {
        if (actionList.isEmpty()) {
            val extras = if (planner is LssLrtaStarPlanner) {
                "A*: ${planner.aStarTimer} Learning: ${planner.dijkstraTimer}"
            } else {
                ""
            }

            throw RuntimeException("Select action did not return actions in the given bound. The planner is confused. $extras")
        }

        // Check if the algorithm satisfies the real-time bound
        if (terminationChecker !is TimeTerminationChecker) return
        if (terminationChecker.timeLimit < iterationNanoTime) {
            val extras = if (planner is LssLrtaStarPlanner) {
                "A*: ${planner.aStarTimer} Learning: ${planner.dijkstraTimer}"
            } else {
                ""
            }

            throw RuntimeException("Real-time bound is violated. Time bound: ${terminationChecker.timeLimit} but execution took $iterationNanoTime. $extras")
        } else {
            logger.info { "Time bound: ${terminationChecker.timeLimit} execution took $iterationNanoTime." }
        }
    }
}

