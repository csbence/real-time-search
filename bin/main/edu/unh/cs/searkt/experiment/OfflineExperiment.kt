package edu.unh.cs.searkt.experiment

import edu.unh.cs.searkt.environment.Action
import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.planner.classical.OfflinePlanner
import edu.unh.cs.searkt.util.convertNanoUpDouble
import java.util.concurrent.TimeUnit

/**
 * An experiments meant for classical search, such as depth first search.
 * An single run means requesting the planner to return a plan given an initial state.
 *
 * You can either run experiments on a specific state, or have them randomly
 * generated by the domain.
 *
 * NOTE: assumes the same domain is used to create both the planner as this class
 *
 * @param planner is the planner that is involved in the experiment
 * @param domain is the domain of the planner. Used for random state generation
 * @param initialState is the start state of the planner.
 */
class OfflineExperiment<StateType : State<StateType>>(val configuration: ExperimentConfiguration,
                                                      val planner: OfflinePlanner<StateType>,
                                                      val domain: Domain<StateType>,
                                                      val initialState: StateType,
                                                      val terminationChecker: TerminationChecker) : Experiment() {

    private var actions: List<Action> = emptyList()
    private val expansionLimit = configuration.expansionLimit

    override fun run(): ExperimentResult {
        // do experiment on state, either given or randomly created
        val state: StateType = initialState


        if (expansionLimit != null) terminationChecker.resetTo(expansionLimit)

        val experimentExecutionTime = measureThreadCpuNanoTime {
            actions = planner.plan(state, terminationChecker)
        }

//        val planningTime: Long = when (configuration.terminationType) {
//            TIME -> experimentExecutionTime
//            EXPANSION -> planner.expandedNodeCount.toLong()
//            else -> throw MetronomeException("Unknown termination type")
//        }

        // log results
        val pathLength = actions.size.toLong() - 1

        var currentState = initialState
        // validate path
        actions.forEach {
            currentState = domain.transition(currentState, it)
                    ?: return ExperimentResult(experimentConfiguration = configuration, errorMessage = "Invalid transition. From $currentState with $it")
        }

        if (!domain.isGoal(currentState)) {
            return ExperimentResult(experimentConfiguration = configuration, errorMessage =
            "Found plan does not lead to a goal instead leads to $currentState with heuristic value ${domain.heuristic(currentState)}")
        }

        val experimentResult = ExperimentResult(
                configuration = configuration,
                expandedNodes = planner.expandedNodeCount,
                generatedNodes = planner.generatedNodeCount,
                planningTime = experimentExecutionTime,
                iterationCount = 1,
                actionExecutionTime = pathLength * configuration.actionDuration,
                goalAchievementTime = 0,
                idlePlanningTime = 0,
                pathLength = pathLength,
                actions = actions.map(Action::toString),
                experimentRunTime = convertNanoUpDouble(experimentExecutionTime, TimeUnit.SECONDS)
        )

        experimentResult.reexpansions = planner.reexpansions

        domain.appendDomainSpecificResults(experimentResult)
        return experimentResult
    }
}