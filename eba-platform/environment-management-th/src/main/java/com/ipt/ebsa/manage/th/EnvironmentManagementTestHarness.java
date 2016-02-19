package com.ipt.ebsa.manage.th;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.manage.th.data.Data;
import com.ipt.ebsa.manage.th.database.DBUtil;

public class EnvironmentManagementTestHarness {

	public static final String CONFIG_PARAM_ENVIRONMENT = "environment";
	public static final String CONFIG_PARAM_NEW_VERSION = ".newVersion";
	public static final String CONFIG_PARAM_NEW_PACKAGE_NAMES = "packageNames";
	private static final String CMD_TEST_DD = "run";
	private static final String HELP = "help";
	private static final String PATH_TO_ENVIRONMENT_MGMT = "executable";
	private static final String CONFIG = "config";
	private static final String COMPONENT_VERSION_CONFIG = "scenarioConfig";
	private static final String COMMAND = "command";
	private static final String[] supportedCommands = new String[] { EnvironmentManagementTestHarness.CMD_TEST_DD};
    private static Properties mainProperties = new Properties();
	/**
	 * Kicks off the runEnvironmentTestHarness code below, and returns the status code.
	 * 
	 * This is easier to unit test like this.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		int exitCode = runEnvironmentManagementTestHarness(args);
		
		System.exit(exitCode);
	}


	/**
	 * Helper function which returns exit codes, making unit testing a little easier
	 * @param args
	 * @return
	 */
	public static int runEnvironmentManagementTestHarness(String[] args) {
		System.out.println(Arrays.toString(args));
		Options options = createCommandLineOptions();

		CommandLineParser parser = new GnuParser();
		
		try {
			/* Parse the command line options */
			CommandLine line = parser.parse(options, args);
			
			showHelp(options, line);

			/* Options are good, lets start */
			try {
				/* If there is a configuration file then manage how that gets used */
				manageConfigurationFile(line);
						
				/* Parse out the command line arguments and reconcile them with any that might be in the properties file (apply command line overrides) */
				String command = manageArgument(line,COMMAND);
				String scenarioPropertiesConfig = manageArgument(line,COMPONENT_VERSION_CONFIG);
				String config = manageArgument(line,EnvironmentManagementTestHarness.CONFIG);
				String executable = manageArgument(line,EnvironmentManagementTestHarness.PATH_TO_ENVIRONMENT_MGMT);
				
				/* Validate that basic mandatory properties have been set */
				String[] mandatoryProperties = new String[]{COMMAND, CONFIG, PATH_TO_ENVIRONMENT_MGMT, COMPONENT_VERSION_CONFIG};
				String[] mandatoryValues = new String[]{command, config, executable, scenarioPropertiesConfig};
				validateMandatoryArguments(mandatoryProperties, mandatoryValues);
				
				/* Load the additional properties for the component versions into the config 
				 * and store them in a common generated file*/
				Properties p = loadProperties(scenarioPropertiesConfig);
				mainProperties.putAll(p);
				
				File scenarioPropertiesFolder = new File(scenarioPropertiesConfig).getParentFile();
				File generatedMergedpropertiesFile = new File(scenarioPropertiesFolder, "generated.tmp.properties");
				mainProperties.store(new FileOutputStream(generatedMergedpropertiesFile), "This is an generated properties file.  Any changes to this file will be overwritten. Rather edit the source properties file '"+config+"' and '"+scenarioPropertiesConfig+"'");
				
				/* Execute the relevant command */
				if (command.equals(CMD_TEST_DD)) {
					
					/* Validate that basic mandatory properties have been set */
					String newReleaseDetails = manageArgument(line,EnvironmentManagementTestHarness.CONFIG_PARAM_NEW_PACKAGE_NAMES);
					validateMandatoryArguments(new String[]{CONFIG_PARAM_NEW_PACKAGE_NAMES}, new String[]{newReleaseDetails});
					
					DatabaseManager manager = new DatabaseManager();
					ApplicationVersion appVersion = null;
					try {
						/* Create a database */
						manager.initialise(mainProperties);
						DBUtil.clearDownDB(manager.getEntityManager());
						Data data = new Data(manager.getEntityManager());
						appVersion = data.setUpDataForTest(mainProperties);
					}
					finally {
						manager.finalise();
					}	
					/* Set up the executor */
					EnvironmentManagementExecutor executor = new EnvironmentManagementExecutor();
					String environment = mainProperties.getProperty(EnvironmentManagementTestHarness.CONFIG_PARAM_ENVIRONMENT);
					List<String> values = executor.getStandardExecutionCommand(executable, generatedMergedpropertiesFile.getPath(), 
							appVersion.getId().toString(), environment);

					//now run the program, and return the exit code from that.
					Map<String, String> map = new HashMap<String, String>();
					String[] executorArguments = values.toArray(new String[values.size()]);
					String workFolder = ".";
					
					return executor.runProgram(map, executorArguments, workFolder);
				} else {
					System.err.println("'"+command+"' is either not supported or has not been implemented. Supported (but not neccessarily implemented) commands are: " + Arrays.toString(getSupportedCommands()));
					
					return 1; //failure, exit with status 1
				}
			} catch (Exception e) {
				System.err.println("Exception occurred: " + e);
				e.printStackTrace();
				
				return 1; //failure, exit with status 1
			}

		} catch (org.apache.commons.cli.ParseException e1) {
			System.err.println("Unable to parse arguments");
			displayHelp(options);
			
			return 1; //failure, exit with status 1
		}
	}


	private static Properties loadProperties(String componentVersionsConfig) throws FileNotFoundException, IOException {
		Properties p = new Properties();
		FileReader reader = null;
		try {
			reader = new FileReader(componentVersionsConfig);
			p.load(reader);
		}
		finally {
			if (reader != null) {
				reader.close();
			}
		}
		return p;
	}
	

    /**
     * Execute special logic related to the provision of a configuration file on the command line 
     * @param line
     * @throws IOException 
     */
	private static void manageConfigurationFile(CommandLine line) throws IOException {
		if (line.hasOption(CONFIG) && line.getOptionValue(CONFIG) != null) {
		   File configFile = new File(line.getOptionValue(CONFIG));
		   if (configFile.exists()){
			   mainProperties = loadProperties(configFile.getAbsolutePath());
		   }
		   else {
			   throw new FileNotFoundException("Cannot find config file '"+configFile.getAbsolutePath()+"'");
		   }
		}
	}

	/**
	 * If the command line argument is null then it returns the value from the configuration file
	 * otherwise it overwrites the value in the configuration properties (in memory but not in the source file)
	 * and returns that.  This effectively allows the overriding of any properties in the properties file
	 * @param line
	 * @param argumentName
	 * @return
	 */
	private static String manageArgument(CommandLine line, String argumentName) {
		String argumentValue = line.getOptionValue(argumentName);
		if (argumentValue != null) {
		   mainProperties.put(argumentName, argumentValue);
		}
		return (String) mainProperties.get(argumentName);
	}


	/**
	 *  Make sure that we have all of the mandatory parameters specified either on the command line or in the 
	 *  configuration file.
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
		   throw new RuntimeException("Values for mandatory properties " + b.toString() + " are missing.  These must be provided in either the config file or on the command line");
		}
		
	}
	
	/**
	 *Show the help menu if necessary
	 * @param options
	 * @param line
	 */
	private static void showHelp(Options options, CommandLine line) {
		if (line.hasOption(HELP) ) {
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
		formatter.printHelp("java -jar environment-management-th.jar [options]", options);

		System.exit(-1);
	}

	/**
	 * This constructs the command line arguments model
	 */
	private static Options createCommandLineOptions() {
		// create Options object
		Options options = new Options();

		options.addOption(CONFIG, true, "The path to a configuration file containing program arguments that are also available on the command line (i.e. those that are in this list apart from this one)");
		options.addOption(COMMAND, true, "Command to be executed on the data in the config.  Supported commands: " + Arrays.toString(getSupportedCommands()));
		options.addOption(PATH_TO_ENVIRONMENT_MGMT, true, "Location of the evironment managent application executable that you wish to run.");
		options.addOption(CONFIG_PARAM_NEW_PACKAGE_NAMES, true, "A CSV list of package names for components which are to be included in an ApplicationVersion.  Each package name also needs an <packagename>.newVersion entry.");
		options.addOption(COMPONENT_VERSION_CONFIG, true, "The path to a configuration file containing program arguments that are scenario specific.");
		options.addOption(HELP, true, "Display this menu");

		return options;
	}
}
