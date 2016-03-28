package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.visualizer.BaseVisualizer
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.stage.Stage
import javafx.util.Duration
import java.util.*

/**
 * Created by Stephen on 2/29/16.
 */
class PointVisualizer : BaseVisualizer() {
    override fun getOptions(): Options = Options()

    override fun processOptions(cmd: CommandLine) {}

    override fun start(primaryStage: Stage) {
//        processCommandLine(parameters.raw.toTypedArray())

        val DISPLAY_LINE = true

        val parameters = getParameters()
        val raw = parameters.raw
        if(raw.isEmpty()){
            println("Cannot visualize without a domain!")
            //exitProcess(1);
        }

        val rawDomain = raw.first()//experimentResult!!.experimentConfiguration["rawDomain"] as String

        /* Get action list from Application */
        val actionList: MutableList<String> = arrayListOf()
        for (i in 1..raw.size - 1){//experimentResult!!.actions) {
            var xStart = raw.get(i).indexOf('(') + 1
            var xEnd = raw.get(i).indexOf(',')
            var yStart = xEnd + 2
            var yEnd = raw.get(i).indexOf(')')

            val x = raw.get(i).substring(xStart, xEnd)
            val y = raw.get(i).substring(yStart, yEnd)
            actionList.add(x)
            actionList.add(y)
        }

        primaryStage.title = "RTS Visualizer"
        val inputScanner = Scanner(rawDomain.byteInputStream())

        val rowCount: Int
        val columnCount: Int
        val startX: Double
        val startY: Double
        val goalX: Double
        val goalY: Double
        val goalRadius: Double

        columnCount = inputScanner.nextLine().toInt()
        rowCount = inputScanner.nextLine().toInt()
        startX = inputScanner.nextLine().toDouble()
        startY = inputScanner.nextLine().toDouble()
        goalX = inputScanner.nextLine().toDouble()
        goalY = inputScanner.nextLine().toDouble()
        goalRadius = inputScanner.nextLine().toDouble()


        val root = Pane()

        /* Graphical parameters */
        val WIDTH = 1400.0
        val HEIGHT = 800.0
        val TILE_WIDTH: Double = (WIDTH / columnCount)
        val TILE_HEIGHT: Double = (HEIGHT / rowCount)
        var TILE_SIZE = Math.min(TILE_WIDTH, TILE_HEIGHT)

        while(((TILE_SIZE * columnCount) > WIDTH) || ((TILE_SIZE * rowCount) > HEIGHT)){
           TILE_SIZE /= 1.05
        }

        /* The robot */
        val robotWidth = TILE_SIZE / 4.0
        val robot = Rectangle(robotWidth, robotWidth)
        robot.fill = Color.ORANGE
        root.children.add(robot)

        /* the dirty cell */
        val dirtyCell = Circle(goalX * TILE_SIZE, goalY * TILE_SIZE, TILE_SIZE / 4.0)
        dirtyCell.fill = Color.BLUE
        root.children.add(dirtyCell)

        /* the goal radius */
        val goalCircle = Circle(goalX * TILE_SIZE, goalY * TILE_SIZE, goalRadius * TILE_SIZE)
        goalCircle.stroke = Color.BLUE
        goalCircle.fill = Color.WHITE
        goalCircle.opacity = 0.5
        root.children.add(goalCircle)


        for (y in 0..rowCount - 1) {
            val line = inputScanner.nextLine()
            for (x in 0..columnCount - 1) {
                when (line[x]) {
                    '#' -> {
                        val blocked = Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE)
                        blocked.fill = Color.BLACK
                        blocked.stroke = Color.BLACK
                        root.children.add(blocked)
                    }
                    '_' -> {
                        val free = Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE)
                        free.fill = Color.LIGHTSLATEGRAY
                        free.stroke = Color.WHITE
                        free.opacity = 0.5
                        root.children.add(free)
                    }
                }
            }
        }

        primaryStage.scene = Scene(root, TILE_SIZE * columnCount, TILE_SIZE * rowCount)
        //primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()


        /* Create the path that the robot will travel */
        robot.toFront()
        val path = Path()
        val xLoc = startX * TILE_SIZE
        val yLoc = startY * TILE_SIZE
        robot.x = xLoc
        robot.y = yLoc
        robot.translateX = xLoc
        robot.translateY = yLoc
        path.elements.add(MoveTo(xLoc, yLoc))
        path.stroke = Color.ORANGE

        /* Display the path */
        if (DISPLAY_LINE)
            root.children.add(path)

        val sq = SequentialTransition()
        var count = 0
        while (count != actionList.size) {
            val x = actionList.get(count)
            val y = actionList.get(count + 1)
            var pt = animate(root, x, y, DISPLAY_LINE, robot, TILE_SIZE)
            sq.children.add(pt)
            count+=2
        }
        sq.setCycleCount(Timeline.INDEFINITE);
        sq.play()
    }

    private fun animate(root: Pane, x: String, y: String, dispLine: Boolean, robot: Rectangle, width: Double): PathTransition {
        val path = Path()

        val xDot = x.toDouble() * width
        val yDot = y.toDouble() * width

        path.elements.add(MoveTo(robot.translateX, robot.translateY))
        path.elements.add(LineTo(robot.translateX + xDot, robot.translateY + yDot))
        robot.translateX += xDot
        robot.translateY += yDot

        if(dispLine){
            path.stroke = Color.RED
            root.children.add(path)
            val action = Circle(robot.translateX, robot.translateY, width / 10.0)
            root.children.add(action)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.setDuration(Duration.millis(2000.0))
        pathTransition.setPath(path)
        pathTransition.setNode(robot)
        pathTransition.setInterpolator(Interpolator.LINEAR);
        return pathTransition
    }
}