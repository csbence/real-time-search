package edu.unh.cs.searkt.planner.realtime

import edu.unh.cs.searkt.MetronomeConfigurationException
import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.*
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.planner.*
import edu.unh.cs.searkt.planner.SafetyBackup.PARENT
import edu.unh.cs.searkt.planner.SafetyBackup.PREDECESSOR
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.util.AdvancedPriorityQueue
import edu.unh.cs.searkt.util.resize
import java.util.*
import kotlin.Long.Companion.MAX_VALUE
import kotlin.system.measureTimeMillis

/**
 * SimpleSafe as described in "Avoiding Dead Ends in Real-time Heuristic Search"
 *
 * Planning phase - performs a breadth-first search to depth k, noting any
 * safe states that are generated.
 *
 * The tree is then cleared and restarts search back at the initial state and
 * behaves like LSS-LRTA*, using its remaining expansion budget to perform
 * best-first search on f.
 *
 * After the learning phase marks the ancestors (via predecessors) of all generated
 * safe states (from both the breadth-first and best-first searches) as comfortable
 * and all top-level actions leading from the initial state to a comfortable state as
 * safe.
 *
 * If there are safe actions, it commits to one whose successors state has lowest f.
 *
 * If no actions are safe, it just commits to the best action.
 *
 * This look continues until the goal has been found.
 *
 */

class SimpleSafePlanner<StateType : State<StateType>>(val domain: Domain<StateType>, configuration: ExperimentConfiguration) : RealTimePlanner<StateType>() {
    private val safetyBackup = configuration.safetyBackup
            ?: throw MetronomeConfigurationException("Safety backup strategy is not specified.")
    private val targetSelection = configuration.targetSelection
            ?: throw MetronomeConfigurationException("Safety bstrategy is not specified.")
    private val depthBound: Int = configuration.lookaheadDepthLimit?.toInt()
            ?: throw MetronomeConfigurationException("Lookahead depth limit is not specified.")

    private val versionNumber = SimpleSafeVersion.TWO

    class Node<StateType : State<StateType>>(override val state: StateType,
                                             override var heuristic: Double,
                                             override var cost: Long,
                                             override var actionCost: Long,
                                             override var action: Action,
                                             var iteration: Long,
                                             parent: Node<StateType>? = null,
                                             override var safe: Boolean = false,
                                             override var depth: Int,
                                             override var unsafe: Boolean = false
    ) : Safe, SearchNode<StateType, Node<StateType>>, Depth {
        /** Item index in the open list */
        override var index: Int = -1

        override val open: Boolean
            get() = index >= 0

        /** Nodes that generated this Node as a successor in the current exploration phase */
        override var predecessors: MutableList<SearchEdge<Node<StateType>>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor */
        override var parent: Node<StateType> = parent ?: this

        override fun hashCode(): Int = state.hashCode()

        override fun equals(other: Any?): Boolean {
            if (other != null && other is Node<*>) {
                return state == other.state
            }
            return false
        }

        override fun toString(): String =
                "Node: [State: $state, h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open]"
    }

    private var iterationCounter = 0L

    private val safeNodes = ArrayList<Node<StateType>>()


    private val fValueComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1 // break ties on g
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val heuristicComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap<StateType, Node<StateType>>(100000000, 1.toFloat()).resize()

    private var openList = AdvancedPriorityQueue(100000000, fValueComparator)

    private var rootState: StateType? = null

    private var aStarPopCounter = 0
    private var dijkstraPopCounter = 0
    var aStarTimer = 0L
    var dijkstraTimer = 0L

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        // first search iteration check

        println("$iterationCounter :: $sourceState")

        if (rootState == null) {
            rootState = sourceState
        } else if (sourceState != rootState) {
        }

        if (domain.isGoal(sourceState)) {
            return emptyList()
        }

        // Every turn do k-breadth-first search to learn safe states
        // then A* until time expires


        if (versionNumber == SimpleSafeVersion.ONE) {
            resetSearchTree()
        }

        if (openList.isNotEmpty()) {
            dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }
        }

        breadthFirstSearch(sourceState, terminationChecker, depthBound)

        var plan: List<ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val targetNode = aStar(sourceState, terminationChecker)

            when (safetyBackup) {
                PARENT -> throw MetronomeException("Invalid configuration. SimpleSafe does not implement the BEST_SAFE strategy")
                PREDECESSOR -> predecessorSafetyPropagation(safeNodes)
            }

            val targetSafeNode = when (targetSelection) {
                SafeRealTimeSearchTargetSelection.SAFE_TO_BEST -> selectSafeToBest(openList)
                SafeRealTimeSearchTargetSelection.BEST_SAFE -> throw MetronomeException("Invalid configuration. SimpleSafe does not implement the BEST_SAFE strategy")
            }

            plan = extractPath(targetSafeNode ?: targetNode, sourceState)
            rootState = targetNode.state
        }

        return plan!!
    }

    /**
     * Runs local breadth-first search then clears the search tree
     * except for the safe nodes we learn about up to a depth k
     */
    private fun breadthFirstSearch(state: StateType, terminationChecker: TerminationChecker, depthBound: Int) {
        val breadthFirstFrontier = ArrayDeque<Node<StateType>>()
        val node = Node(state, nodes[state]?.heuristic
                ?: domain.heuristic(state), 0, 0, NoOperationAction, iterationCounter, null, false, 0)
        nodes[state] = node
        breadthFirstFrontier.add(node)

        var currentIteration = 0

        while (!terminationChecker.reachedTermination() && currentIteration < depthBound) {

            val topNode = breadthFirstFrontier.peek()
                    ?: throw GoalNotReachableException("Open list is empty during k-BFS")
            if (domain.isGoal(topNode.state)) return

            val foundSafeNode = expandFromNode(breadthFirstFrontier.pop()!!, breadthFirstFrontier)
            terminationChecker.notifyExpansion()
            currentIteration = breadthFirstFrontier.peek()?.depth
                    ?: throw GoalNotReachableException("Open list is empty during k-BFS")

            if (versionNumber == SimpleSafeVersion.TWO) {
                if (foundSafeNode) {
                    currentIteration = depthBound + 1
                }
            }
        }
        breadthFirstFrontier.peek() ?: throw GoalNotReachableException("Open list is empty during k-BFS")
    }

    /**
     * Expands a node and add it to closed list, similar to expandFromNode
     * but uses the queue implementation being passed in, for each successor
     * it will add it to the open list passed and store its g value as long as the
     * state has not been seen before, or is found with a lower g value.
     */
    private fun expandFromNode(sourceNode: Node<StateType>, openListQueue: Queue<Node<StateType>>): Boolean {
        expandedNodeCount++
        var foundSafeNode = false
        val currentGValue = sourceNode.cost
        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state

            val successorNode = getNode(sourceNode, successor)

            if (successorNode.heuristic == Double.POSITIVE_INFINITY
                    && successorNode.iteration != iterationCounter) {
                // Ignore this successor as it is a dead end
                continue
            }

            // update the node depth to be one mor than the parent
            successorNode.depth = sourceNode.depth + 1

            // do not need to worry about predecessors because we are dumping the nodes after
            // but care about safety still
            if (domain.isSafe(successorNode.state)) {
                safeNodes.add(successorNode)
                successorNode.safe = true
                foundSafeNode = true
            }

            if (successorNode.iteration != iterationCounter) {
                successorNode.apply {
                    iteration = iterationCounter
                    predecessors.clear()
                    cost = MAX_VALUE
                }
            }

            // skip if we circle back to the parent
            if (successorState == sourceNode.parent.state) {
                continue
            }

            val successorGValueFromCurrent = currentGValue + successor.actionCost
            // always add the successor doing a BFS
            successorNode.apply {
                cost = successorGValueFromCurrent.toLong()
                parent = sourceNode
                action = successor.action
                actionCost = successor.actionCost.toLong()
                depth = parent.depth + 1
            }

            // we always add the node doing a BFS
            openListQueue.add(successorNode)
        }

        sourceNode.heuristic = Double.POSITIVE_INFINITY

        return foundSafeNode
    }

    /**
     * Runs AStar until termination and returns the path to the head of openlist
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(state: StateType, terminationChecker: TerminationChecker): Node<StateType> {
        // build the fabulous (and glorious) A* tree / essential
        initializeAStar()

        val node = Node(state, nodes[state]?.heuristic
                ?: domain.heuristic(state), 0, 0, NoOperationAction, iterationCounter, null, false, 0)
        nodes[state] = node
        openList.add(node)

        while (!terminationChecker.reachedTermination()) {
            aStarPopCounter++

            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
            if (domain.isGoal(topNode.state)) return topNode

            expandFromNode(openList.pop()!!)
            terminationChecker.notifyExpansion()

        }
        return openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
    }

    private fun initializeAStar() {
        iterationCounter++
        openList.clear()
        safeNodes.clear()
        openList.reorder(fValueComparator)
    }

    private fun resetSearchTree() {
        openList.clear()
        nodes.clear()
        openList.reorder(fValueComparator)
    }

    /**
     * Expands a node and add it to the closed list. For each successor
     * it will add it to the open list and store it's g value, as long as the
     * state has not been seen before, or is found with a lower g value
     */
    private fun expandFromNode(sourceNode: Node<StateType>) {
        expandedNodeCount++

        val currentGValue = sourceNode.cost
        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state

            val successorNode = getNode(sourceNode, successor)
            successorNode.depth = sourceNode.depth + 1

            // safety check
            if (domain.isSafe(successorNode.state)) {
                safeNodes.add(successorNode)
                successorNode.safe = true
            }

            successorNode.predecessors.add(SearchEdge(node = sourceNode, action = successor.action, actionCost = successor.actionCost.toLong()))

            // out dated nodes are updated
            if (successorNode.iteration != iterationCounter) {
                successorNode.apply {
                    iteration = iterationCounter
                    predecessors.clear()
                    cost = MAX_VALUE
                }
            }

            // skip is we got back to the parent
            if (successorState == sourceNode.parent.state) {
                continue
            }

            // only generated states that are not yet visited or whose cost values are lower than this path
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorNode.cost > successorGValueFromCurrent) {
                successorNode.apply {
                    cost = successorGValueFromCurrent.toLong()
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost.toLong()
                    depth = parent.depth + 1
                }

                if (!successorNode.open) {
                    openList.add(successorNode)
                } else {
                    openList.update(successorNode)
                }
            } else {
            }
        }
    }

    /**
     * Get a node for the state if it exists, else create new node.
     *
     * @return node corresponding to the given state.
     */
    private fun getNode(parent: Node<StateType>, successor: SuccessorBundle<StateType>): Node<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        return if (tempSuccessorNode == null) {
            generatedNodeCount++

            val undiscoveredNode = Node(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    actionCost = successor.actionCost.toLong(),
                    action = successor.action,
                    parent = parent,
                    cost = MAX_VALUE,
                    iteration = iterationCounter,
                    depth = parent.depth + 1
            )

            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            parent.depth += 1
            tempSuccessorNode.depth = parent.depth + 1
            tempSuccessorNode
        }
    }

    /**
     * Performs Dikjatra until runs out of resources or done
     *
     * Updates the mode to SEARCH if done with DIJKSTRA
     *
     * Dijkstra updates repeatedly pop the state s according to their heuristic value, and then update
     * the cost values for all it's visited successors, based on the heuristic s.
     *
     * This increases the stored heuristic value, ensuring that A* won't go in circles, and in general generating
     * a better table of heuristics.
     */
    private fun dijkstra(terminationChecker: TerminationChecker) {
        iterationCounter++

        openList.reorder(heuristicComparator)

        while (!terminationChecker.reachedTermination() && openList.isNotEmpty()) {
            // check closed list
            val node = openList.pop() ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")
            node.iteration = iterationCounter

            val currentHeuristicValue = node.heuristic

            for ((predecessorNode, _, actionCost) in node.predecessors) {
                if (predecessorNode.iteration == iterationCounter && !predecessorNode.open) {
                    // the node was already learned and closed in current iteration
                    continue
                }

                val predecessorHeuristicValue = predecessorNode.heuristic

                if (!predecessorNode.open) {
                    // node is not open yet because it was not visited in the current planning iteration

                    predecessorNode.heuristic = currentHeuristicValue + actionCost

                    assert(predecessorNode.iteration == iterationCounter - 1)

                    predecessorNode.iteration = iterationCounter
                    openList.add(predecessorNode)
                } else if (predecessorHeuristicValue > currentHeuristicValue + actionCost) {
                    predecessorNode.heuristic = currentHeuristicValue + actionCost
                    openList.update(predecessorNode)
                }
            }
        }
    }
}

enum class SimpleSafeConfiguration {
    SAFETY_BACKUP, SAFETY, VERSION
}

enum class SimpleSafeSafety {
    ABSOLUTE, PREFERRED
}

enum class SimpleSafeVersion {
    ONE, TWO
}