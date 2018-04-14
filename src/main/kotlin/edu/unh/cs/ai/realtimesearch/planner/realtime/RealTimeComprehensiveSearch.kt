package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import org.slf4j.LoggerFactory
import java.lang.Double.min
import java.lang.Long.max
import java.util.*
import kotlin.math.ceil
import kotlin.system.measureTimeMillis

/**
 * Examines the full state space as time allows, propagating back learned heuristic values to all predecessors,
 * generating them if necessary. Maintains the frontier of to-be-examined nodes between iterations
 * Action Plan is single-action only picking the node with the best h-value of immediate neighbors
 * @author Kevin C. Gall
 * @date 3/30/18
 */
class RealTimeComprehensiveSearch<StateType: State<StateType>>(
        val domain: Domain<StateType>,
        val configuration : ExperimentConfiguration) : RealTimePlanner<StateType>(){
    //Configuration parameters
    private val expansionRatio : Double = configuration.backlogRatio ?: 1.0
    //Logger and timekeeping
    private val logger = LoggerFactory.getLogger(RealTimeComprehensiveSearch::class.java)
    var explorationTimer = 0L


    class Node<StateType: State<StateType>> (val state: StateType, var heuristic: Double) : Indexable {
        override var index: Int = -1

        //add to ancestors
        val ancestors = HashMap<StateType, DiEdge<StateType>>()
        //will add to successors when node is expanded
        val successors = HashMap<StateType, DiEdge<StateType>>()

        var onGoalPath = false
        var onFrontier = false
        var expanded = false
        var visitCount = 0

        override fun hashCode(): Int = state.hashCode()
        override fun equals(other: Any?): Boolean {
            return when {
                other == null -> false
                other !is Node<*> -> false
                other.state == state -> true
                else -> false
            }
        }

        companion object EdgeFactory {
            fun <StateType : State<StateType>> createEdge(source : Node<StateType>, destination : Node<StateType>, actionCost : Long, action : Action) {
                val edge = DiEdge(source, destination, actionCost, action)

                source.successors[destination.state] = edge
                destination.ancestors[source.state] = edge
            }
        }
    }

    //Directed edge class - describes relationship from node to node
    data class DiEdge<StateType : State<StateType>> (val source : Node<StateType>, val destination : Node<StateType>,
                                                     val actionCost : Long, val action: Action) {
        fun getCost() : Double {
            return destination.heuristic + actionCost
        }
    }

    //initialization and persistent state
    private var lastExpansionCount : Long = 0
    private var iterationCount = 0
    private var foundGoal = false

    private var currentAgentState : StateType? = null

    //Closed List (also includes nodes on the frontier)
    private val closed = HashMap<StateType, Node<StateType>>(100000000, 1.toFloat())

    //Frontier (open list) and its comparator
    private val frontierComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }
    private val frontier = AdvancedPriorityQueue(1000000, frontierComparator)

    //Backlog Queue
    //Sorted on fuzzy-f: h value plus estimate of G value from current state
    private val backlogComparator = Comparator<Node<StateType>> { lhs, rhs ->
        //using heuristic for "fuzzy g value"
        //Our nodes aren't tracking distance from agent state as actions are committed, so must use estimate
        val lhsFuzzyG = domain.heuristic(currentAgentState!!, lhs.state)
        val rhsFuzzyG = domain.heuristic(currentAgentState!!, rhs.state)
        val lhsFuzzyF = lhs.heuristic + lhsFuzzyG
        val rhsFuzzyF = rhs.heuristic + rhsFuzzyG

        //break ties on lower estimate of G -> closer to agent!
        when {
            lhsFuzzyF < rhsFuzzyF -> -1
            lhsFuzzyF > rhsFuzzyF -> 1
            lhsFuzzyG < rhsFuzzyG -> -1
            rhsFuzzyG > lhsFuzzyG -> 1
            lhs.onGoalPath -> -1
            rhs.onGoalPath -> 1
            else -> 0
        }
    }
    //Will need to be reordered before every learning phase
    private val backlogQueue = AdvancedPriorityQueue(1000000, backlogComparator)
    private val goalPathQueue = AdvancedPriorityQueue(1000000, backlogComparator)

    override fun init() {
        explorationTimer = 0
        foundGoal = false
        iterationCount = 0
        lastExpansionCount = 0
        frontier.clear()
        closed.clear()
        backlogQueue.clear()
    }

    /**
     * Core Function of planner. Splits into 3 phases:
     * <ul>
     *     <li>
     *         Learning (Backward propagation)<br/>
     *         Bounded by previous iteration's expansions * config ratio
     *     </li>
     *     <li>
     *         Exploration (Expansion)<br/>
     *         Bounded by termination checker
     *     </li>
     *     <li>
     *         Movement (Action Commitment)<br/>
     *         Bounded by configuration setting
     *     </li>
     * </ul>
     */
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        logger.debug("""
            |Selecting action with source state:
            |$sourceState
            """.trimMargin())
        if (iterationCount == 0) {
            frontier.add(Node(sourceState, domain.heuristic(sourceState)))
            val firstNode = frontier.peek()
            firstNode!!.onFrontier = true
            closed[sourceState] = frontier.peek()!!
        }

        currentAgentState = sourceState
        val thisNode = closed[sourceState]
        if (thisNode !== null) {
            thisNode.visitCount++
        } else {
            throw GoalNotReachableException("Current state unexamined. The planner is confused!")
        }

        explorationTimer += measureTimeMillis { exploreStateSpace(terminationChecker, thisNode) }

        iterationCount++

        logger.debug("""
            |*****Status after Iteration $iterationCount*****
            |Timers:
            |   Exploration - $explorationTimer
            |
            |Frontier Size: ${frontier.size}
            |Backlog Queue Size: ${backlogQueue.size}
            """.trimMargin())
        return moveAgent(sourceState)
    }

    //Learning / Exploration facets, interleaved
    private fun exploreStateSpace(terminationChecker: TerminationChecker, sourceNode : Node<StateType>) {
        logger.debug("Exploration")

        backlogQueue.reorder(backlogComparator)

        var nextNode : Node<StateType>? = if (!sourceNode.expanded) {
            if (sourceNode.onFrontier) {
                frontier.remove(sourceNode)
                sourceNode.index = -1
            }
            sourceNode
        } else frontier.pop()

        val limit : Int = ceil(expansionRatio).toInt()
        term@ while (!terminationChecker.reachedTermination()) {
            if (nextNode === null && !foundGoal) {
                throw GoalNotReachableException("No reachable path to goal")
            } else if (nextNode !== null) {
                val expandedGoal = expandFrontierNode(nextNode)
                foundGoal = foundGoal || expandedGoal

                terminationChecker.notifyExpansion()
                nextNode.expanded = true
                nextNode.onFrontier = false
            } else if (backlogQueue.isEmpty()) {
                break
            }

            for (i in 1..limit) {
                if (terminationChecker.reachedTermination()) break

                if (backlogQueue.isEmpty()) {
                    logger.debug("Reached the end of the backlog queue")
                    break
                }

                val learnNode = backlogQueue.pop()!!

                var expanded = false
                if (domain.isGoal(learnNode.state)
                        || updateNodeHeuristic(learnNode)) {
                    expanded = addAncestorsToBacklog(learnNode, !foundGoal)
                }

                if (expanded) {
                    terminationChecker.notifyExpansion()
                    expandedNodeCount++
                }
            }

            nextNode = if (!terminationChecker.reachedTermination()) frontier.pop() else null
        }
    }

    private fun updateNodeHeuristic(node : Node<StateType>) : Boolean {
        var bestH = Double.POSITIVE_INFINITY
        var bestNode : Node<StateType>? = null
        for (successor in node.successors) {
            val successorEdge = successor.value
            val successorNode = successorEdge.destination

            //checking successor node h + the cost to get to that successor
            val tempBestH = bestH
            bestH = min(bestH, successorNode.heuristic + successorEdge.actionCost)

            if (tempBestH != bestH) bestNode = successorNode
        }

        //if  bestH is greater than h, this means it's more accurate
        if (node.heuristic < bestH && bestNode != null) {
            node.heuristic = bestH

            //add ancestors to backlog for examination
            return true
        }

        return false
    }

    private fun addAncestorsToBacklog(node : Node<StateType>, noExpansion : Boolean = false) : Boolean {
        val ancestorStates = domain.predecessors(node.state)

        var expandedNewNodes = false

        ancestorStates.forEach {
            var ancestorNode : Node<StateType>?
            if (!closed.containsKey(it.state)) {
                if (noExpansion) {
                    return@forEach
                }

                expandedNewNodes = true

                ancestorNode = Node(it.state, domain.heuristic(it.state))
                closed[it.state] = ancestorNode

                if (!foundGoal) {
                    frontier.add(ancestorNode)
                    ancestorNode.onFrontier = true
                }
                generatedNodeCount++
            } else {
                ancestorNode = closed[it.state]
            }

            ancestorNode ?: throw NullPointerException("Closed list has State Key which points to Null")

            //only adding edge if ancestor has not been registered in the ancestor map yet
            if (!node.ancestors.containsKey(ancestorNode.state)) {
                Node.createEdge(ancestorNode, node, it.actionCost, it.action)
            }

            if (node.onGoalPath) ancestorNode.onGoalPath = true

            if (!ancestorNode.onFrontier && !backlogQueue.contains(ancestorNode)) {
                backlogQueue.add(ancestorNode)
            }
        }

        return expandedNewNodes
    }

    private fun expandFrontierNode(frontierNode : Node<StateType>) : Boolean {
        val isGoal = domain.isGoal(frontierNode.state)
        if (isGoal) {
            frontierNode.onGoalPath = true
            backlogQueue.add(frontierNode)

            //eliminate frontier. We've reached a new phase of the algorithm
            frontier.clear()
        } else {
            val successors = domain.successors(frontierNode.state)

            var bestNode : Node<StateType> = frontierNode
            var bestH : Double = Double.POSITIVE_INFINITY
            successors.forEach {
                val successorNode: Node<StateType>?
                if (!closed.containsKey(it.state)) {
                    successorNode = Node(it.state, domain.heuristic(it.state))
                    closed[it.state] = successorNode

                    if (!foundGoal) {
                        frontier.add(successorNode)
                        successorNode.onFrontier = true
                    }
                    generatedNodeCount++
                } else {
                    successorNode = closed[it.state]
                }

                //null check
                successorNode ?: throw NullPointerException("Closed list has State Key which points to Null")

                if (!frontierNode.successors.containsKey(successorNode.state)) {
                    Node.createEdge(frontierNode, successorNode, it.actionCost, it.action)
                }

                val tempH = successorNode.heuristic + it.actionCost
                if (tempH < bestH) {
                    bestH = tempH
                    bestNode = successorNode
                }
            }
            if (bestH > frontierNode.heuristic) {
                frontierNode.heuristic = bestH

                if (bestNode.onGoalPath) {
                    frontierNode.onGoalPath = true
                }
                addAncestorsToBacklog(frontierNode)
            }
        }

        expandedNodeCount++
        return isGoal
    }

    //Agent moves to the next state with the lowest cost (action cost + h), breaking ties on h
    private fun moveAgent(sourceState : StateType) : List<ActionBundle> {
        logger.debug("Movement Phase")

        //only commit 1 at a time
        val actionList = ArrayList<ActionBundle>()

        var currentNode = closed[sourceState] ?:
        throw NullPointerException("Closed list has State Key which points to Null")

        //hard coding limit of 1 for now. May change later, so keeping it in loop
        val actionPlanLimit = 1
        for (i in 1..actionPlanLimit) {
            if (domain.isGoal(currentNode.state)) break

            //break when currentNode is on the frontier - we haven't examined past it yet!
            if (currentNode.successors.size == 0) break

            val pathEdge= currentNode.successors.values.reduce { minSoFar, next ->
                val lhsCost = minSoFar.getCost()
                val rhsCost = next.getCost()

                when {
                    minSoFar.destination.onGoalPath && !next.destination.onGoalPath -> minSoFar
                    next.destination.onGoalPath && !minSoFar.destination.onGoalPath -> next
                    lhsCost < rhsCost -> minSoFar
                    lhsCost > rhsCost -> next
                    minSoFar.destination.heuristic < next.destination.heuristic -> minSoFar
                    minSoFar.destination.heuristic > next.destination.heuristic -> next
                    minSoFar.destination.onFrontier -> next
                    else -> minSoFar
                }
            }

            actionList.add(ActionBundle(pathEdge.action, pathEdge.actionCost))

            //update the heuristic of the node we just left right now. This is so we don't get caught in local minima.
            //Additionally, prevent expansion when adding ancestors so we don't go over our expansion limit
            if (updateNodeHeuristic(currentNode)) addAncestorsToBacklog(currentNode, true)

            currentNode = pathEdge.destination
        }

        if (actionList.size == 0) {
            throw GoalNotReachableException("Agent has reached a dead end")
        }

        return actionList
    }

    enum class ComprehensiveConfigurations(val configurationName: String) {
        BACKLOG_RATIO ("backlogRatio");

        override fun toString() = configurationName
    }
}