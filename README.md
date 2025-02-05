# Searkt - Heuristic Search Library

Searkt provides a unified API to compare offline, anytime, and real-time search algorithms over well defined domains. It comes with several popular hearistic search domains and algorithms, and allows users to provide their own.

## Configuration

Configuration in Metronome are specified using JSON configuration objects. These can be generated by Metronome, loaded from a file, or communicated by using Metronome Server API.

Usage example:
`./gradlew run --args='-internalConfiguration'`
The result json will be printed to the standard out upon completion.

## Search algorithms

* Depth First Search
* A*
* Anytime Repairing A*
* Real-time A*
* LSS-LRTA*
* Dynamic f-hat
* Safe Real-time Search

## Domains

#### Vacuum World

A 2D grid with with dirty and blocked cells. The agent aims to clean all dirty spots by vacuuming those cells.

State space: (`x`,`y`, {dirty locations})

Actions: up, down, left, right, vacuum 

#### Race Track [[1]](#ref-1)

A 2D grid of a track of any shape of cells (representing possible locations of the car) connecting a starting and finish line. The goal is for the car to reach the finish line from the start location.

Whenever the car hits a track boundary, it will be put back on a random position on the starting line with 0 velocity.

State space: (`x, y, x. [-1,0,1], y. [-1,0,1]`)

Actions: Acceleration in `x` and `y` (`x.` and `y.`) of `[-1,0,1]`

Dynamics:

    x(t+1) = x(t) + x.(t) + m(x,t)
    y(t+1) = y(t) + y.(t) + m(y,t)
    x.(t+1) = x.(t) + m(x,t)
    y.(t+1) = y.(t) + m(y,t)

The Race Track domain contains dead end states from which the agent cannot reach any of the goal states.

#### Acrobot  [[2]](#ref-2)

The Acrobot is a two link planar robot arm.  The system is underactuated and torque may be applied to the middle joint in order to move the two links.

State space: (\theta_1, \theta_2, \dot\theta_1, \dot\theta_2)

Actions: Torque applied to joint 2

#### Sliding Tile Puzzle

A domain in which numbered tiles and a blank space are shifted (swapping the blank with any of the four adjacent numbered tiles), with the goal of placing the numbered tiles in numerical order. Assumed to be square with `size` width and height.

State space: (`zeroIndex`, {tile locations})

Actions: up, down, left, right 

#### Traffic World

Traffic World is a dynamic world that presents exogenous dangers to the agent. It is an extension of the traffic domain used by [[3]](#ref-2): reminiscent of the video game Frogger, the agent must navigate a grid from the upper-left to the lower-right using four-way movement while avoiding moving obstacles. A cartoon sketch is shown at the bottom right of Figure~\ref{fig:benchmarks}.  (The white cells are bunkers, which are described below.)  Each obstacle moves either vertically or horizontally, one cell per timestep.  Obstacles bounce off the edges of the grid but pass through each other.  While these velocities are known to the agent, and hence the domain is deterministic, the system state space is large, involving both the location of the agent and the obstacles (or equivalently, the timestep).  In addition to the four moves, the agent can also execute a no-op action and remain stationary.  We extend the domain to include special bunker cells, off of which dynamic obstacles bounce, protecting the agent.  The cost-to-go heuristic *h* is the Manhattan distance, which is perfect in the absence of obstacles. 100 random instances of 50 by 50 cells are provided with the framework in which each cell has a 50% chance to be the starting position of an obstacle or a 10% chance of being a bunker.

## Configuration Format

### Type and unit information

* The default time unit for every value is nanoseconds (unless otherwise specified).
* Quantitative values (numbers) are represented as 64 bit values. (Long/Double)
* Categorical values and paths are represented as strings.

### Mandatory Configurations

* algorithm name (`"algorithmName" : String`)
    - Name of the algorithm.
* domain name (`"domainName" : String`)
    - Name of the domain.
* serialized domain (`"rawDomain" : String`)
* domain instance name (`"domainInstanceName" : String`)
    - Name assigned to particular domain configuration instance
* time limit (`"timeLimit" : Long`)
* action duration (`"actionDuration" : Long`)
    - Must be greater than zero
* termination type (`"terminationType" : { "TIME" | "EXPANSION" | "UNLIMITED"`)
    - The unlimited type is for debugging purposes only. The unlimited termination checker will never interrupt an iteration.

### Real-time search

* time bound type (`"lookaheadType" : { "STATIC" | "DYNAMIC" }`)
    - The original lookahead bound will be used for every iteration if static lookahead is selected.
    - The available duration for planning depends on the duration of the committed actions when dynamic lookahead is used. 
* commitment strategy (`"commitmentStrategy" : { "SINGLE" | "MULTIPLE" }`)

### Anytime search

* max count (`"anytimeMaxCount" : Long`)

### Algorithm

#### A*

No extra parameter required

#### Weighted A*

* weight (`"weight" : Double`)

#### ARA*

#### RTA*

* lookahead depth limit (`"lookaheadDepthLimit" : Long`)

#### LSS-LRTA*

#### Dynamic f-hat

## Contribution

### Naming Conventions

* Naming
    - `iAmGoodVariable`
    - `IAmGoodClass`
    - `iAmGoodFunction()`
    - No abbreviations
* Style
    - Braces open on same line
* Use Javadoc on functions and classes, but mind the verbosity
* Logging is done using the different levels:
    - error: Actual errors / wrong stuff
    - warn: Experiment level (i.e. start i'th iteration)
    - info: higher level planners 
    - debug: internal stuff in planners, such as tree building occurrences
    - trace: Used as little as possible, but keep if used during debugging

## References

<a name="ref-1"></a>[1] Barto, Andrew G., Steven J. Bradtke, and Satinder P. Singh. "Learning to act using real-time dynamic programming." Artificial Intelligence 72.1 (1995): 81-138.

<a name="ref-2"></a>[2] R.M. Murray and J. Hauser, “A Case Study in Approximate Linearization:
The Acrobot Example,” Proc. American Control Conference, 1990.

<a name="ref-3"></a>[3] Scott Kiesel, Ethan Burns, and Wheeler Ruml, Achieving Goals Quickly Using Real-time Search: Experimental Results in Video Games, "Journal of Artificial Intelligence Research", 54, pp. 123-158, 2015.
