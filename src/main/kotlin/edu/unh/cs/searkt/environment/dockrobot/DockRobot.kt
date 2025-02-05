package edu.unh.cs.searkt.environment.dockrobot

import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.SuccessorBundle
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs

typealias Successor = SuccessorBundle<DockRobotState>

@Serializable
data class DockRobotSiteEdge(val siteId: SiteId, val edgeCost: Double)

class DockRobot(
        val maxPileCount: Int,
        val maxPileHeight: Int,
        val siteAdjacencyList: List<List<DockRobotSiteEdge>>,
        @Transient
        val goalContainerSites: List<Int>,
        @Transient
        val initialState: DockRobotState) : Domain<DockRobotState> {

    private val fastGoalContainerSites = IntArray(goalContainerSites.size) { goalContainerSites[it] }

    private val loadCost = 1.0
    private val unloadCost = 1.0
    private val levelCost = 0.05
    private val minMoveCost = 1.0

    /**
     * Generate successors from the following actions:
     *
     * - Moving the robot to any directly reachable site.
     * - Loading the cranes
     * - Unloading the cranes
     * - Loading the robot
     * - Unloading the robot
     */
    override fun successors(state: DockRobotState): List<SuccessorBundle<DockRobotState>> {
        return generateMoveSuccessors(state) + generateCraneSuccessors(state)
    }

    private fun generateMoveSuccessors(state: DockRobotState): List<Successor> {
        val robotSiteId = state.robotSiteId
        val successors = mutableListOf<Successor>()

        val reachableSites = siteAdjacencyList[robotSiteId]
        reachableSites.forEach { (targetSiteId, cost) ->
            // Only consider successors with "non-infinite" costs
            if (targetSiteId != robotSiteId) {
                val updatedContainerSites = state.containerSites.copyOf()

                // Update the container location list if the robot is moving a container between sites
                if (state.cargo != -1) {
                    updatedContainerSites[state.cargo] = robotSiteId
                }

                val newState = state.copy(robotSiteId = targetSiteId, containerSites = updatedContainerSites)
                successors.add(Successor(newState, DockRobotMoveAction(targetSiteId), actionCost = cost))
            }
        }

        return successors
    }

    private fun generateCraneSuccessors(state: DockRobotState): List<Successor> = if (state.hasCargo()) {
        generateUnLoadRobotSuccessors(state)
    } else {
        generateLoadRobotSuccessors(state)
    }

    private fun generateUnLoadRobotSuccessors(state: DockRobotState): MutableList<Successor> {
        val robotSiteId = state.robotSiteId
        val currentSite = state.sites[robotSiteId]
        val successors = mutableListOf<Successor>()

        currentSite
                ?.filter { it.size < maxPileHeight }
                ?.forEachIndexed { targetPileId, pile ->
                    val updatedContainerSites = state.containerSites.copyOf()
                    updatedContainerSites[state.cargo] = state.robotSiteId

                    val newPile = ArrayDeque(pile)
                    newPile.push(state.cargo)

                    val newPiles = ArrayList(currentSite)
                    newPiles[targetPileId] = newPile

                    newPiles.sortWith(DockRobotState.pileComparator)

                    val updatedSites = HashMap(state.sites)
                    updatedSites[robotSiteId] = newPiles

                    val newState = state.copy(cargo = -1, sites = updatedSites, containerSites = updatedContainerSites)
                    successors.add(Successor(newState, DockRobotUnLoadAction(targetPileId), unloadCost))
                }

        // Create a new pile with the container
        if (currentSite == null || currentSite.size < maxPileCount) {
            // The site does not exists yet

            // Create the new pile
            val newPile = ArrayDeque<Container>()
            newPile.push(state.cargo)


            // Create a copy of the old piles and add the new pile
            val newPiles = if (currentSite == null) {
                ArrayList()
            } else {
                ArrayList(currentSite)
            }

            newPiles.add(newPile)

            // Sorting helps duplicate detection
            newPiles.sortWith(DockRobotState.pileComparator)

            // deepcopy the sites for the hash map
            val copyHashMap = HashMap<SiteId, Piles>()
            state.sites.forEach { (siteId, site) ->
                val sitePileCopy = ArrayList<Pile>(site.size)
                site.forEachIndexed { pileIndex, pile ->
                    val pileCopy = ArrayDeque<Int>()
                    pile.forEach { pileCopy.add(it) }
                    sitePileCopy.add(pileCopy)
                }
                copyHashMap[siteId] = sitePileCopy
            }
            val updatedSites = HashMap(copyHashMap)
            updatedSites[robotSiteId] = newPiles

            val updatedContainerSites = state.containerSites.copyOf()
            updatedContainerSites[state.cargo] = state.robotSiteId

            val newState = state.copy(cargo = -1, sites = updatedSites, containerSites = updatedContainerSites)
            successors.add(Successor(newState, DockRobotUnLoadAction(newPiles.size - 1), unloadCost))
        }

        return successors
    }

    private fun generateLoadRobotSuccessors(state: DockRobotState): MutableList<Successor> {
        val robotSiteId = state.robotSiteId
        val currentSite = state.sites[robotSiteId]
        val successors = mutableListOf<Successor>()

        currentSite?.forEachIndexed { sourcePileId, pile ->
            val newPile = ArrayDeque(pile)
            val containerId = newPile.pop()

            val newPiles = ArrayList(currentSite)

            val updatedSites = HashMap(state.sites)

            if (newPile.isEmpty()) {
                newPiles.removeAt(sourcePileId)

                if (newPiles.isEmpty()) {
                    // The site is now empty as the last container from the last pile was removed
                    updatedSites.remove(state.robotSiteId)
                } else {
                    // The modified pile is empty
                    updatedSites[robotSiteId] = newPiles
                }
            } else {
                // The modified pile is not empty update the copy of the site
                newPiles[sourcePileId] = newPile
                newPiles.sortWith(DockRobotState.pileComparator)

                updatedSites[robotSiteId] = newPiles
            }

            val updatedContainerSites = state.containerSites.copyOf()
            updatedContainerSites[containerId] = state.robotSiteId

            val newState = state.copy(cargo = containerId, sites = updatedSites)
            successors.add(Successor(newState, DockRobotLoadAction(sourcePileId), loadCost))
        }

        return successors
    }

    override fun heuristic(state: DockRobotState): Double {
        if (state.heuristic >= 0) return state.heuristic

        // Determine which containers need to be moved
        val misplacedContainers = state.containerSites.indices
                .map { state.containerSites[it] != fastGoalContainerSites[it] }

        // Cost of performing move actions to the right site
        val moveActionsRequired = state.containerSites.indices
                .map { abs(goalContainerSites[it] - state.containerSites[it]) }.sum()

        // The minimum number of load actions
        val minimumNumberLoadActions = state.containerSites.indices
                .map { if (state.cargo != it) 1 else 0 }.sum()

        // Cost of removing the containers from the piles if they are at not at the right site
        val unstackCost = state.sites.map { (_, site) ->
            site.sumByDouble { pile ->
                val height = pile.indexOfFirst { misplacedContainers[it] }
                val depth = if (height >= 0) pile.size - 1 - height else 0
                val unstackCost = depth * levelCost

                unstackCost
            }
        }.sum()

        return unstackCost + moveActionsRequired + minimumNumberLoadActions
    }

    override fun distance(state: DockRobotState): Double {
        throw UnsupportedOperationException("not implemented")
    }

    override fun isGoal(state: DockRobotState): Boolean {
        return state.containerSites contentEquals fastGoalContainerSites
    }
}

