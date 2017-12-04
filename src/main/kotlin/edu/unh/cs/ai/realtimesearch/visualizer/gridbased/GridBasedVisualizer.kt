/*
package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.visualizer.BaseVisualizer
import javafx.stage.Screen
import java.util.*

*/
/**
 * Base visualizer for grid-based domains.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 * @since April 8, 2016
 *//*

abstract class GridBasedVisualizer : BaseVisualizer() {
    // Options
    protected val gridOptions = Options()
    protected val trackerOption = Option("t", "tracker", true, "show tracker around agent")
    protected val displayPathOption = Option("p", "path", false, "display line for agent's path")

    // Option fields
    protected var showTracker: Boolean = false
    protected var trackerSize: Double = 10.0
    protected var displayLine: Boolean = false

    // State fields
    protected var actionList: MutableList<String> = arrayListOf()
    protected var mapInfo: MapInfo = MapInfo.ZERO

    // Graphical fields
    protected var grid: GridCanvasPane = GridCanvasPane.ZERO
    protected var agentView: AgentView = AgentView.ZERO
    private val primaryScreenBounds = Screen.getPrimary().visualBounds
    protected val windowWidth = primaryScreenBounds.width - 100
    protected val windowHeight = primaryScreenBounds.height - 100
    protected var tileWidth = 0.0
    protected var tileHeight = 0.0
    protected var tileSize = 0.0
    open protected var robotScale = 2.0

    protected var initialAgentXLocation = 0.0
    protected var initialAgentYLocation = 0.0

    init {
        trackerOption.setOptionalArg(true)
        gridOptions.addOption(trackerOption)
        gridOptions.addOption(displayPathOption)
    }

    override fun getOptions(): Options {
        return gridOptions
    }

    override fun processOptions(cmd: CommandLine) {
        showTracker = cmd.hasOption(trackerOption.opt)
        trackerSize = cmd.getOptionValue(trackerOption.opt, trackerSize.toString()).toDouble()
        displayLine = cmd.hasOption(displayPathOption.opt)
    }

    data class GridDimensions(val rowCount: Int, val columnCount: Int)

    */
/**
     * Parse the map header and return the row and column counts.  Domains which have a different header than the
     * standard grid world header should override and do what they need with the extra values and then return the
     * row and column counts here.  Implementations must not read more than the header from the scanner.
     *
     * @param inputScanner scanner pointing to header of raw domain string
     * @return the row and column counts given in the header
     *//*

    open protected fun parseMapHeader(inputScanner: Scanner): GridDimensions {
        val columnCount = inputScanner.nextLine().toInt()
        val rowCount = inputScanner.nextLine().toInt()
        return GridDimensions(rowCount, columnCount)
    }

    */
/**
     * Parse map; fill {@link MapInfo}
     *//*

    open protected fun parseMap(rawDomain: String): MapInfo {
        val inputScanner = Scanner(rawDomain.byteInputStream())
        val (rowCount, columnCount) = parseMapHeader(inputScanner)
        val mapInfo = MapInfo(rowCount, columnCount)
        for (y in 0..rowCount - 1) {
            val line = inputScanner.nextLine()
            for (x in 0..columnCount - 1) {
                when (line[x]) {
                    '#' -> {
                        mapInfo.blockedCells.add(Location(x, y))
                    }
                    '_' -> {
                    }
                    '*' -> {
                        mapInfo.goalCells.add(Location(x, y))
                    }
                    '@' -> {
                        mapInfo.startCells.add(Location(x, y))
                    }
                    else -> {
                        throw IllegalArgumentException("Invalid character ${line[x]} found in map")
                    }
                }
            }
        }
        return mapInfo
    }

    */
/**
     * Parse the experiment result for actions.  If the domain includes actions which cannot be directly translated
     * from the results as strings then the implementing visualizer should override this method.
     * {@link GridBasedVisualizer} will call this method after calling {@link BaseVisualizer#processCommandLine).
     *//*

    open protected fun parseActions(): MutableList<String> {
        */
/* Get action list from Application *//*

        val actionList: MutableList<String> = arrayListOf()
        for (action in experimentResult.actions) {
            actionList.add(action)
        }
        return actionList
    }

    */
/**
     * Performs parsing of results and graphical setup.  After this method is called, all {@link GridBasedVisualizer}
     * fields will be properly initialized.
     *//*

    protected fun visualizerSetup() {
        actionList = parseActions()

        // Parse map
        mapInfo = parseMap(rawDomain)
        if (mapInfo.startCells.size != 1) {
            throw IllegalArgumentException("${mapInfo.startCells.size} start cells found in map; required 1")
        }

        // Calculate tile sizes
        tileWidth = windowWidth / mapInfo.columnCount
        tileHeight = windowHeight / mapInfo.rowCount
        tileSize = Math.min(tileWidth, tileHeight)
        while (((tileSize * mapInfo.columnCount) > windowWidth) || ((tileSize * mapInfo.rowCount) > windowHeight)) {
            tileSize /= 1.05
        }

        // Calculate robot parameters
        val agentWidth = tileSize / robotScale
        val agentStartX = mapInfo.startCells.first().x
        val agentStartY = mapInfo.startCells.first().y
        initialAgentXLocation = agentStartX * tileSize + (tileSize / 2.0)
        initialAgentYLocation = agentStartY * tileSize + (tileSize / 2.0)
        val actualRobotXLocation = initialAgentXLocation - agentWidth / 2.0
        val actualRobotYLocation = initialAgentYLocation - agentWidth / 2.0

        // Agent setup
        agentView = AgentView(agentWidth, trackerSize)
        agentView.trackingEnabled = showTracker
        agentView.toFront()
        agentView.setLocation(actualRobotXLocation, actualRobotYLocation)

        // Grid setup
        grid = GridCanvasPane(mapInfo, tileSize)
        grid.children.add(agentView.agent)
        grid.children.add(agentView.tracker)
    }
}*/
