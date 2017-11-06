package edu.unh.cs.ai.realtimesearch.visualizer

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentResultFromJson
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import groovyjarjarcommonscli.*
import javafx.application.Application
import kotlin.system.exitProcess


/**
 * Base application for visualizers.  Handles command line parsing and provides framework for easily adding custom
 * options per visualizer implementation.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 */
abstract class BaseVisualizer : Application() {
    protected lateinit var experimentResult: ExperimentResult
    protected var rawDomain: String = ""

    /**
     * Process commandline arguments.  Converts retrieved experiment result into a {@link ExperimentResult} and
     * retrieves the raw domain from the configuration.  Then calls {@link #processOptions} for custom options.
     */
    protected fun processCommandLine(args: Array<String>) {
        val options = getOptions()

        val helpOption = Option("h", "help", false, "Print help and exit")

        options.addOption(helpOption)

        /* parse command line arguments */
        val cmd = GnuParser().parse(options, args)

        /* print help if help option was specified*/
        val formatter = HelpFormatter()
        if (cmd.hasOption("h")) {
            formatter.printHelp("real-time-search", options)
            exitProcess(1)
        }

        if (cmd.args.size < 1) {
            throw IllegalArgumentException("Error: Must pass results to visualizer")
        }

        try {
            experimentResult = experimentResultFromJson(cmd.args.first())
        } catch (e: JsonParseException) {
            throw InvalidResultException("Failed to parse result", e)
        } catch (e: JsonMappingException) {
            throw InvalidResultException("Failed to parse result", e)
        }

        if (experimentResult.configuration["rawDomain"] == null)
            throw InvalidResultException("Visualizer must have raw domain in result")

        rawDomain = experimentResult.configuration[Configurations.RAW_DOMAIN.toString()] as String

        processOptions(cmd)
    }

    /**
     * Return the application's options.  Should not include help option which is provided by the base.
     */
    protected abstract fun getOptions(): Options

    /**
     * Process any additional options provided by the visualizer.
     */
    protected abstract fun processOptions(cmd: CommandLine)
}