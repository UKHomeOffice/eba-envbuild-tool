package com.ipt.ebsa.environment.metadata.generation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.config.ConfigurationFactory;

/**
 * Environment Build Metadata Import Program.
 * Imports data from the Visio extract XML into the ENVIRONMENT_BUILD database
 *
 */
public class EnvironmentBuildMetadataImportCLI {
	
	private static final Logger LOG = LogManager.getLogger(EnvironmentBuildMetadataImportCLI.class);

	private static final String HELP = "help";
	private static final String CMD_IMPORT = "import";
	private static final String ARG_INPUT_XML_FILENAME = "inputXMLFile";
	private static final String CONFIG = "config";
	private static final String COMMAND = "command";
	private static final String[] SUPPORTED_COMMANDS = new String[] {
		EnvironmentBuildMetadataImportCLI.CMD_IMPORT,
		EnvironmentBuildMetadataImportCLI.HELP,
	};
	
	/**
	 * Parses and checks command line options, kicks off the relevant command
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		/* now we can resume normal startup */
		Options options = createCommandLineOptions();

		CommandLineParser parser = new GnuParser();

		try {
			/* Parse the command line options */
			CommandLine line = parser.parse(options, args);

			if(!showHelp(options, line)) {
	
				/* Options are good, lets start */
				try {
					/* If there is a configuration file then manage how that gets used */
					manageConfigurationFile(line);
	
					/* This puts left over arguments that are added onto the command line into the Configuration */
					manageAdditionalArgs(line);
					
					/*
					 * Parse out the command line arguments and reconcile them with any that might be in the properties file
					 * (apply command line overrides)
					 */
					String command = manageArgument(line, COMMAND);
	
					/* Validate that basic mandatory properties have been set */
					String[] mandatoryProperties = new String[] { COMMAND };
					String[] mandatoryValues = new String[] { command };
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);
	
					/* Execute the relevant command */
					if (command.equals(HELP)) {
						displayHelp(options);
					}
					else if (command.equals(CMD_IMPORT)) {
						String inputFileName = manageArgument(line, ARG_INPUT_XML_FILENAME);
						mandatoryProperties = new String[] { ARG_INPUT_XML_FILENAME };
						mandatoryValues = new String[] { inputFileName };
						validateMandatoryArguments(mandatoryProperties, mandatoryValues);
	
						EnvironmentBuildMetadataImport importer = new EnvironmentBuildMetadataImport(inputFileName);
						importer.importEnvironmentMetadata();
						exit(0);
					}
					else {
						LOG.error("'" + command
										+ "' is either not supported or has not been implemented. Supported (but not neccessarily implemented) commands are: "
										+ Arrays.toString(getSupportedCommands()));
						exit(1);
					}
				} catch (Exception e) {
					LOG.error("Failed to execute command", e);
					exit(1);
				}
			}
		} catch (org.apache.commons.cli.ParseException e1) {
			LOG.error("Unable to parse arguments", e1);
			displayHelp(options);
			exit(1);
		}
	}

	private static void manageAdditionalArgs(CommandLine line) {
		String[] lefOverAgrs = line.getArgs();
		for (int i = 0; i < lefOverAgrs.length; i++) {
			String option = lefOverAgrs[i];
			if (option.indexOf("=") > 0) {
				String[] split = option.split("=");
				ConfigurationFactory.getProperties().setProperty(split[0], split[1]);
			} else {
				ConfigurationFactory.getProperties().setProperty(option, option);
			}

		}
	}

	/**
	 * Calls exit() on the ExitHandler
	 * 
	 * @param code
	 */
	private static void exit(int code) {
		exitHandler.exit(code);
	}

	/**
	 * Execute special logic related to the provision of a configuration file on the command line
	 * 
	 * @param line
	 * @throws FileNotFoundException
	 */
	private static void manageConfigurationFile(CommandLine line) throws FileNotFoundException {
		if (line.hasOption(CONFIG) && line.getOptionValue(CONFIG) != null) {
			File configFile = new File(line.getOptionValue(CONFIG));
			if (configFile.exists()) {
				ConfigurationFactory.setConfigFile(configFile);
			} else {
				throw new FileNotFoundException("Cannot find config file '" + configFile.getAbsolutePath() + "'");
			}
		}
	}

	/**
	 * If the command line argument is null then it returns the value from the configuration file
	 * otherwise it overwrites the value in the configuration properties (in memory but not in the source file)
	 * and returns that. This effectively allows the overriding of any properties in the properties file
	 * 
	 * @param line
	 * @param argumentName
	 * @return
	 */
	private static String manageArgument(CommandLine line, String argumentName) {
		String argumentValue = line.getOptionValue(argumentName);
		if (argumentValue != null) {
			ConfigurationFactory.getProperties().put(argumentName, argumentValue);
		}
		return (String) ConfigurationFactory.getProperties().get(argumentName);
	}

	/**
	 * Make sure that we have all of the mandatory parameters specified either on the command line or in the
	 * configuration file.
	 * 
	 * @param command
	 * @param filepath
	 * @param organisation
	 * @param vdc
	 * @param user
	 * @param password
	 */
	private static void validateMandatoryArguments(String[] mandatoryProperties, String[] mandatoryValues) {

		StringBuffer b = new StringBuffer();
		for (int i = 0; i < mandatoryValues.length; i++) {
			if (mandatoryValues[i] == null) {
				if (b.length() > 0) {
					b.append(", ");
				}
				b.append("'");
				b.append(mandatoryProperties[i]);
				b.append("'");
			}
		}
		if (b.length() > 0) {
			throw new RuntimeException("Values for mandatory properties " + b.toString()
					+ " are missing.  These must be provided in either the config file or on the command line");
		}

	}

	/**
	 * Show the help menu if necessary
	 * 
	 * @param options
	 * @param line
	 * @return true if we have displayed help
	 */
	private static boolean showHelp(Options options, CommandLine line) {
		if (line.hasOption(HELP)) {
			displayHelp(options);
			exit(0);
			return true;	
		}
		
		return false;
	}

	/**
	 * Returns a list of commands which are supported by this application
	 * 
	 * @return
	 */
	private static String[] getSupportedCommands() {
		return SUPPORTED_COMMANDS;
	}

	private static void displayHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(300);
		formatter.printHelp("java -jar environment-build.jar [options]", options);
		exit(-1);
	}

	/**
	 * This constructs the command line arguments model
	 */
	private static Options createCommandLineOptions() {
		// create Options object
		Options options = new Options();

		options.addOption(
				CONFIG,
				true,
				"The path to a configuration file containing program arguments that are also available on the command line (i.e. those that are in this list apart from this one)");
		options.addOption(COMMAND, true, "Command to be executed on the data in the config.  Supported commands: "
				+ Arrays.toString(getSupportedCommands()));
		options.addOption(HELP, false, "Display this menu");

		return options;
	}
	
	/**
	 * Defines what happens when the main method tries to exit the JVM
	 */
	public interface ExitHandler {
		void exit(int code);
	}
	
	// Default exit handler - exits the JVM with the given exit code
	private static ExitHandler exitHandler = new ExitHandler() {
		public void exit(int code) {
			System.out.flush();
			System.err.flush();
			System.exit(code);
		};
	};
	
	/**
	 * Set a different exit handler (e.g. for JUnit testing to avoid exiting the JVM so we can verify the outcome)
	 * @param newExitHandler
	 */
	public static void setExitHandler(ExitHandler newExitHandler) {
		if (newExitHandler == null) {
			throw new NullPointerException("newExitHandler cannot be null");
		} else {
			exitHandler = newExitHandler;
		}
	}
}
