package com.ipt.ebsa;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import com.ipt.ebsa.agnostic.client.cli.AgnosticCliController;
import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;

public class AgnosticClientCLI {

	public static final String HELP = "help";
	public static final String CONFIG = "config";
	public static final String PASSWORD = "password";
	public static final String USER = "user";
	public static final String VDC = "vdc";
	public static final String ORGANISATION = "organisation";
	public static final String DEFINITION_FILEPATH = "definition";
	public static final String NETWORK_LAYOUT_FILEPATH = "networkLayout";
	public static final String ENVIRONMENT_FILEPATHS = "environments";
	public static final String EXECUTIONPLAN = "executionplan";
	public static final String COMMAND = "command";
	public static final String URL = "url";
	public static final String INFODUMPFILE = "infodumpfullfilepath";
	public static final String[] supportedCommands = new String[] { "sandpit", "execute", "infodumptxt", "infodumpcsv" };

	/**
	 * Parses and checks command line options, initialises Weld, starts the
	 * controller, kicks off the relevant command
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		Options options = createCommandLineOptions();

		CommandLineParser parser = new GnuParser();

		Weld weld = null;
		try {
			/* Parse the command line options */
			CommandLine line = parser.parse(options, args);
			showHelp(options, line);

			/* Options are good, lets start */
			try {
				/*
				 * If there is a configuration file then manage how that gets
				 * used
				 */
				manageConfigurationFile(line);

				/*
				 * Parse out the command line arguments and reconcile them with
				 * any that might be in the properties file (apply command line
				 * overrides)
				 */
				String command = manageArgument(line, COMMAND);
				String filepath = manageArgument(line, DEFINITION_FILEPATH);
				String networkPath = manageArgument(line, NETWORK_LAYOUT_FILEPATH);
				String environmentPath = manageArgument(line, ENVIRONMENT_FILEPATHS);
				String instruction = manageArgument(line, EXECUTIONPLAN);
				String organisation = manageArgument(line, ORGANISATION);
				String vdc = manageArgument(line, VDC);
				String user = manageArgument(line, USER);
				String password = manageArgument(line, PASSWORD);
				String url = manageArgument(line, URL);

				String[] mandatoryProperties;
				String[] mandatoryValues;

				/* Validate that basic mandatory properties have been set */
				mandatoryProperties = new String[] { COMMAND };
				mandatoryValues = new String[] { command };

				validateMandatoryArguments(mandatoryProperties, mandatoryValues);

				/* Instantiate Weld */
				weld = new Weld();
				WeldContainer container = weld.initialize();

				AgnosticCliController controller = container.instance().select(AgnosticCliController.class).get();

				/* Execute the relevant command */
				if (command.equals("sandpit")) {
					controller.sandpit();
				} else if (command.equals("execute")) {
					try {
					/* Validate further mandatory properties have been set */
						mandatoryProperties = new String[] { EXECUTIONPLAN, DEFINITION_FILEPATH };
						mandatoryValues = new String[] { instruction, filepath };
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);
					} catch (RuntimeException re) {
						//failed validation of the definition use multi part files
						/* Validate further mandatory properties have been set */
						mandatoryProperties = new String[] { EXECUTIONPLAN, NETWORK_LAYOUT_FILEPATH, ENVIRONMENT_FILEPATHS };
						mandatoryValues = new String[] { instruction, networkPath, environmentPath };
						validateMandatoryArguments(mandatoryProperties, mandatoryValues);
					}
					
					
					controller.execute();
				} else {
					System.err.println("'" + command
							+ "' is either not supported or has not been implemented. Supported (but not neccessarily implemented) commands are: "
							+ Arrays.toString(getSupportedCommands()));
				}

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}

		} catch (org.apache.commons.cli.ParseException e1) {
			System.err.println("Unable to parse arguments");
			displayHelp(options);
			System.exit(1);
		} finally {
			if (weld != null) {
				weld.shutdown();
			}
		}
	}

	/**
	 * Execute special logic related to the provision of a configuration file on
	 * the command line
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
	 * If the command line argument is null then it returns the value from the
	 * configuration file otherwise it overwrites the value in the configuration
	 * properties (in memory but no tin the source file) and returns that. This
	 * effectively allows the overriding of any properties in the properties
	 * file
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
	 * Make sure that we have all of the mandatory parameters specified either
	 * on the command line or in the configuration file.
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
	 */
	private static void showHelp(Options options, CommandLine line) {
		if (line.hasOption(HELP)) {
			displayHelp(options);
		}
	}

	/**
	 * Returns a list of commands which are supported by this application
	 * 
	 * @return
	 */
	private static String[] getSupportedCommands() {
		return supportedCommands;
	}

	private static void displayHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(300);
		formatter.printHelp("java -jar aCloudClient-X.X.X.jar [options]", options);

		System.exit(-1);
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
		options.addOption(COMMAND, true,
				"Command to be executed on the data in the config.  Supported commands: " + Arrays.toString(getSupportedCommands()));
		options.addOption(DEFINITION_FILEPATH, true, "The path to a configuration file containing the full environment and network definition. Use either this option is you have a full file, or the options "+NETWORK_LAYOUT_FILEPATH+" and "+ENVIRONMENT_FILEPATHS+" together if you have seperated network layout and environment files.");
		options.addOption(EXECUTIONPLAN, true, "The path to a command file containing the execution plan");
		options.addOption(URL, true, "The Skyscape url against which the actions will be executed.");
		options.addOption(ORGANISATION, true, "The organisation to which you will be connecting (id rather than name)");
		options.addOption(VDC, true, "The VDC to which you will be connecting");
		options.addOption(USER, true, "User");
		options.addOption(PASSWORD, true, "Password");
		options.addOption(HELP, true, "Display this menu");
		options.addOption(ENVIRONMENT_FILEPATHS, true, "Enevironment file paths comma seperated. Used with "+NETWORK_LAYOUT_FILEPATH+" option");
		options.addOption(NETWORK_LAYOUT_FILEPATH, true, "The organisation network layout xml for the environments specified Used with"+ENVIRONMENT_FILEPATHS+" option");

		return options;
	}

}
