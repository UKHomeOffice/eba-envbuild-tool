package com.ipt.ebsa.environment.build;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.config.BuildProperties;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.environment.build.diff.BuildDiffer;
import com.ipt.ebsa.environment.build.git.EBGitManager;
import com.ipt.ebsa.environment.build.json.JsonFileWriter;
import com.ipt.ebsa.environment.build.manage.BuildManager;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.environment.data.factory.XMLHelper;
import com.ipt.ebsa.environment.metadata.export.agnostic.EnvironmentBuildMetadataAgnosticExport;
import com.ipt.ebsa.environment.metadata.export.vCloud.EnvironmentBuildMetadataVCloudExport;
import com.ipt.ebsa.skyscape.config.v2.XMLOrganisationType;

public class EnvironmentBuildCLI {

	public static final String USER_PARAMETER_PREFIX = "up_";
	
	private static final String CONFIG = "config";
	private static final String COMMAND = "command";
	private static final String BUILD_PLAN_DIR = "buildplandir";
	private static final String REF_PLAN_DIR = "refplandir";
	private static final String BUILD_DATA_PATH = "builddatapath";
	private static final String REPORT_PATH = "reportpath";
	private static final String ENVIRONMENT = "environment";
	private static final String VPC = "vpc";
	private static final String DOMAIN = "domain";
	private static final String CONTAINER = "container";
	private static final String MODE = "mode";
	private static final String VERSION = "version";
	private static final String PROVIDER = "provider";
	private static final String ADDITIONAL_PARAMS_PATH = "additionalparamspath";
	private static final String ENV_DEFINITION_XML_PATH = "envdefnxmlpath";
	private static final String COMBINED_BUILD_PLAN_XML_PATH = "combinedbuildplanxmlpath";
	private static final String WORK_DIR = "workdir";
	
	private static final String CMD_HELP = "help";
	private static final String CMD_RUN = "run";
	private static final String CMD_STARTUP = "startup";
	private static final String CMD_SHUTDOWN = "shutdown";
	private static final String CMD_PREPARE = "prepare";
	private static final String CMD_CHECKOUT_PLANS = "checkoutplans";
	private static final String CMD_DIFF = "diff";
	private static final String CMD_GENERATE_DEFINITION_FILE = "gendef";
	private static final String[] SUPPORTED_COMMANDS = new String[] {
		CMD_HELP,
		CMD_RUN,
		CMD_PREPARE,
		CMD_CHECKOUT_PLANS,
		CMD_DIFF,
		CMD_GENERATE_DEFINITION_FILE
	};
	
	private static final Logger LOG = Logger.getLogger(EnvironmentBuildCLI.class.getName());
	
	/**
	 * Parses and checks command line options, kicks off the relevant command
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LOG.info("environment-build version: " + BuildProperties.getMvnVersion("environment-build") + " args: ");
		for (String s : args) {
			LOG.info(s);
		}
		
		/* now we can resume normal startup */
		Options options = createCommandLineOptions();

		CommandLineParser parser = new GnuParser();

		try {
			/* Parse the command line options */
			CommandLine line = parser.parse(options, args);

			if(!showHelp(options, line)) {
	
				/* Options are good, lets start */
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
				if (command.equals(CMD_HELP)) {
					displayHelp(options);
				}
				else if (command.equals(CMD_CHECKOUT_PLANS)) {
					String workDir = manageArgument(line, WORK_DIR);
					String buildDataPath = manageArgument(line, BUILD_DATA_PATH);
					String provider = manageArgument(line, PROVIDER);
					mandatoryProperties = new String[] {WORK_DIR, BUILD_DATA_PATH, PROVIDER};
					mandatoryValues = new String[] {workDir, buildDataPath, provider};
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);
					
					Configuration.configureConnectionData();
					
					File planCheckoutPath = checkoutBuildPlans(workDir);
					LOG.info("Writing environment and container build data JSON to [" + buildDataPath + "]");
					JsonFileWriter jsonFileWriter = new JsonFileWriter(new ReadManager());
					jsonFileWriter.writeToFile(planCheckoutPath.getAbsolutePath(), buildDataPath, provider);
					exit(0);
					return; // needed for tests
				} else if (command.equals(CMD_STARTUP) || command.equals(CMD_SHUTDOWN)) {
					String workDir = manageArgument(line, WORK_DIR);
					String domain = manageArgument(line, DOMAIN);
					String environment = manageArgument(line, ENVIRONMENT);
					String vpc = manageArgument(line, VPC);
					String reportPath = manageArgument(line, REPORT_PATH);
					
					mandatoryProperties = new String[] {WORK_DIR, DOMAIN, ENVIRONMENT, VPC};
					mandatoryValues = new String[] {workDir, domain, environment, vpc};
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);

					File buildPlanPath = checkoutStartupShutdown(workDir, domain);
					File susdYamlFile = new File(buildPlanPath,"envcontrol/"+domain+".yaml");

					BuildManager buildManager = null;
					try {
						if (!buildPlanPath.isDirectory()) {
							throw new RuntimeException("Build Plan Directory not found: " + buildPlanPath.getAbsolutePath());
						}
						
						if(!susdYamlFile.exists()) {
							throw new RuntimeException("Startup / Shutdown Yaml not found: " + susdYamlFile.getAbsolutePath());
						}
						
						Configuration.configureConnectionData();
						boolean startAction = false;
						if(command.equals(CMD_STARTUP)) {
							startAction = true;
						}
						buildManager = BuildManager.getStartupShutdownInstance(buildPlanPath,susdYamlFile, environment, vpc, startAction, reportPath);
						buildManager.execute();
						
						buildManager.cleanUp();
						exit(0);
						return; // needed for tests
					} finally {
						if (null != buildManager) {
							buildManager.cleanUp();
						}
					}
				} else if (command.equals(CMD_PREPARE) || command.equals(CMD_RUN)) {
					String workDir = manageArgument(line, WORK_DIR);
					String reportPath = manageArgument(line, REPORT_PATH);
					String environment = manageArgument(line, ENVIRONMENT);
					String container = manageArgument(line, CONTAINER);
					String mode = manageArgument(line, MODE);
					String version = manageArgument(line, VERSION);
					String provider = manageArgument(line, PROVIDER);
					String additionalParamsPath = manageArgument(line, ADDITIONAL_PARAMS_PATH);
					String envDefnXmlPath = manageArgument(line, ENV_DEFINITION_XML_PATH); // Optional/legacy
					String combinedBuildPlanXmlPath = manageArgument(line, COMBINED_BUILD_PLAN_XML_PATH); // Optional/legacy
					
					mandatoryProperties = new String[] {WORK_DIR, REPORT_PATH, MODE, VERSION, PROVIDER};
					mandatoryValues = new String[] {workDir, reportPath, mode, version, provider};
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);

					File buildPlanPath = checkoutBuildPlans(workDir);

					BuildManager buildManager = null;
					try {
						if (!buildPlanPath.isDirectory()) {
							throw new RuntimeException("Build Plan Directory not found: " + buildPlanPath.getAbsolutePath());
						}
						
						File reportHtmlFile = new File(reportPath);
						PrintWriter reportPrintWriter = new PrintWriter(new FileWriter(reportHtmlFile));
						
						LOG.info(String.format("Report will be written to [%s]", reportHtmlFile));
						
						String rawAdditionalParams = null;
						if (additionalParamsPath != null) {
							File additionParamsFile = new File(additionalParamsPath);
							rawAdditionalParams = FileUtils.readFileToString(additionParamsFile);
						}
						
						Configuration.configureConnectionData();
						buildManager = BuildManager.getDefaultInstance(buildPlanPath);
						buildManager.setReportPrintWriter(reportPrintWriter);
						
						if (!StringUtils.isBlank(environment)) {
							buildManager.prepareForEnvironment(workDir, environment, version, mode, provider, getAdditionalParams(rawAdditionalParams), envDefnXmlPath);
						} else {
							buildManager.prepareForContainer(workDir, container, version, mode, provider, getAdditionalParams(rawAdditionalParams), envDefnXmlPath);
						}
						
						if (command.equals(CMD_RUN)) {
							// Check out the guest customisation scripts (which are only needed for the actual build)
							EBGitManager gm = new EBGitManager();
							String org;
							if (!StringUtils.isBlank(environment)) {
								try {
									org = new ReadManager().getEnvironmentContainerForEnvironmentName(environment, provider).getName();
								} catch (Exception e) {
									throw new RuntimeException("Failed to get environment container name", e);
								}
							} else {
								org = container;
							}
							String gitUrl = Configuration.getCustomisationScriptsGitUrl(org);
							String scriptPath = Configuration.getCustomisationScriptPath(org);
							if (gitUrl == null && scriptPath == null) {
								throw new IllegalStateException("No customisation script directory git URL or local path specified.");
							} else if (gitUrl != null) {
								// We can check out the git url and pass the checkout path through to the vcloud client later
								gm.getGuestCustomisationScripts(new File(workDir, Configuration.getCustomisationScriptsCheckoutDir()), org);
							} // If a path's been supplied, that will be passed on to the vcloud client later.
							
							buildManager.execute();
						}
						if (StringUtils.isNotBlank(combinedBuildPlanXmlPath) && buildManager.getEnvironmentData() != null) {
							LOG.info("Writing combined plan XML to [" + combinedBuildPlanXmlPath + "]");
							FileUtils.write(new File(combinedBuildPlanXmlPath), buildManager.getEnvironmentData().getAllXmlAsXml(), "utf-8");
						}
						
						buildManager.cleanUp();
						exit(0);
						return; // needed for tests
					} finally {
						if (null != buildManager) {
							buildManager.cleanUp();
						}
					}
				} else if (command.equals(CMD_DIFF)) { 
					String buildPlanPath = manageArgument(line, BUILD_PLAN_DIR);
					String referencePath = manageArgument(line, REF_PLAN_DIR);
					String environment = manageArgument(line, ENVIRONMENT);
					String provider = manageArgument(line, PROVIDER);
					String buildRef = manageArgument(line, MODE);
					
					mandatoryProperties = new String[] {BUILD_PLAN_DIR, REF_PLAN_DIR, ENVIRONMENT, PROVIDER, MODE};
					mandatoryValues = new String[] {buildPlanPath, referencePath, environment, provider, buildRef};
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);
										
					BuildDiffer differ = new BuildDiffer();
					LOG.info(String.format("Calculating differences for environment [%s] using build ref [%s]", environment, buildRef));
					differ.calculateDifferences(new File(referencePath), new File(buildPlanPath), environment, provider, buildRef);
					
					if (differ.hasChanges()) {
						LOG.info("Changes detected, failing build.");
						exit(1);
					} else {
						LOG.info("No changes detected");
						exit(0);
					}
				} else if (command.equals(CMD_GENERATE_DEFINITION_FILE)) {
					String environment = manageArgument(line, ENVIRONMENT);
					String container = manageArgument(line, CONTAINER);
					String version = manageArgument(line, VERSION);
					String provider = manageArgument(line, PROVIDER);
					String envDefnXmlPath = manageArgument(line, ENV_DEFINITION_XML_PATH);
					
					mandatoryProperties = new String[] {VERSION, ENV_DEFINITION_XML_PATH, PROVIDER};
					mandatoryValues = new String[] {version, envDefnXmlPath, provider};
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);
					
					saveAndLog(environment, container, version, provider, envDefnXmlPath);
					exit(0);
				} else {
					LOG.severe("'" + command
									+ "' is either not supported or has not been implemented. Supported (but not neccessarily implemented) commands are: "
									+ Arrays.toString(getSupportedCommands()));
				}
				exit(1);
				return; // needed for tests
			}
		} catch (org.apache.commons.cli.ParseException e1) {
			LOG.log(Level.SEVERE, "Unable to parse arguments", e1);
			displayHelp(options);
			exit(1);
			return; // needed for tests
		} catch (Exception e) {
			// needed for tests
			e.printStackTrace();
			exit(2);
			return; // needed for tests
		}
	}

	private static File checkoutBuildPlans(String workDir) {
		EBGitManager gm = new EBGitManager();
		// The directory the plans are checked out to will be named the same as the directory that
		// holds the plans in git
		File planCheckoutPath;
		if (StringUtils.isNotBlank(Configuration.getEnvironmentBuildPlansDir())) {
			planCheckoutPath = new File(workDir, Configuration.getEnvironmentBuildPlansDir());
		} else {
			planCheckoutPath = new File(workDir);
		}
		gm.getEnvironmentBuildPlans(planCheckoutPath);
		return planCheckoutPath;
	}
	
	private static File checkoutStartupShutdown(String workDir, String domain) {
		EBGitManager gm = new EBGitManager();
		// The directory the plans are checked out to will be named the same as the directory that
		// holds the plans in git
		File planCheckoutPath;
		if (StringUtils.isNotBlank(Configuration.getEnvironmentStartupShutdownDir())) {
			planCheckoutPath = new File(workDir, Configuration.getEnvironmentStartupShutdownDir());
		} else {
			planCheckoutPath = new File(workDir);
		}
		gm.getEnvironmentStartupShutdown(planCheckoutPath, domain);
		return new File(planCheckoutPath,domain);
	}
	
	private static void saveAndLog(String environment, String organisation, String version, String provider, String envDefnPath) {
		FileUtils.deleteQuietly(new File(envDefnPath));
		
		try {
			LOG.info("Beginning extract of environment metadata from the database: " + environment == null ? organisation: environment + " for provider: " + provider);
			String xml;
			if (XMLProviderType.SKYSCAPE.toString().equals(provider)) {
				EnvironmentBuildMetadataVCloudExport exporter = new EnvironmentBuildMetadataVCloudExport();
				XMLOrganisationType envDefnMarshalled;
				if (environment == null) {
					envDefnMarshalled = exporter.extractConfiguration(organisation, version);
				} else {
					envDefnMarshalled = exporter.extractEnvironment(environment, version);
				}
				xml = new XMLHelper().marshallConfigurationXML(envDefnMarshalled);
			} else {
				EnvironmentBuildMetadataAgnosticExport exporter = new EnvironmentBuildMetadataAgnosticExport();
				XMLGeographicContainerType envDefnMarshalled;
				if (environment == null) {
					envDefnMarshalled = exporter.extractEnvironmentContainer(organisation, version, provider);
				} else {
					envDefnMarshalled = exporter.extractEnvironment(environment, version, provider);
				}
				xml = new XMLHelper().marshallConfigurationXML(envDefnMarshalled);
			}
			FileUtils.write(new File(envDefnPath), xml);
			LOG.log(Level.INFO, String.format("Full definition file for environment/organsation [%s] at version [%s] provider [%s] is:%n%s", environment == null ? organisation: environment, version, provider, xml));
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Unable to write environment definition file to path " + envDefnPath, e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, String.format("Enable to extract environment definition for environment/organsation [%s] at version [%s] provider [%s]", environment == null ? organisation: environment, version, provider), e);
			throw new RuntimeException(e);
		}
	}
	
	private static Map<String, String> getAdditionalParams(String rawParams) {
		Map<String,String> map = new HashMap<String, String>();
		
		if (StringUtils.isBlank(rawParams)) {
			return map;
		}
		
		for(String pair : rawParams.split("&")) {
			int index = pair.indexOf("=");
			
			if (index < 0) {
				throw new RuntimeException(String.format("Failed to parse user parameters from: [%s]", rawParams));
			}
			
			try {
				map.put(URLDecoder.decode(pair.substring(0, index), "UTF-8").replace(USER_PARAMETER_PREFIX, ""), URLDecoder.decode(pair.substring(index + 1), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Not going to happen
			}
		}
		
		return map;
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
	 */
	private static void validateMandatoryArguments(String[] mandatoryProperties, String[] mandatoryValues) {

		StringBuffer b = new StringBuffer();
		for (int i = 0; i < mandatoryProperties.length; i++) {
			if (mandatoryValues.length <= i || mandatoryValues[i] == null) {
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
		if (line.hasOption(CMD_HELP)) {
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
		
		options.addOption(CMD_HELP, false, "Display this menu");
		
		options.addOption(
				BUILD_PLAN_DIR,
				true,
				"The path to a Environment Build plan XML directory containing the environment build xml files");
		
		options.addOption(
				REPORT_PATH,
				true,
				"The path that the build report html file will be writen to");
		
		options.addOption(
				ENVIRONMENT,
				true,
				"The environment to be built");
		
		options.addOption(
				CONTAINER,
				true,
				"The environment container to be built");

		options.addOption(
				MODE,
				true,
				"What to do to the environment");
		options.addOption(
				DOMAIN,
				true,
				"The domain of the vpc");
		options.addOption(
				VERSION,
				true,
				"Version of the environment or organisation");
		options.addOption(
				VPC,
				true,
				"Vpc in the environment");
		options.addOption(
				PROVIDER,
				true,
				"The cloud provider the environment or container is to be built in");
		options.addOption(
				ADDITIONAL_PARAMS_PATH,
				true,
				"File containing addition context parameters provided by the user through Jenkins");
		options.addOption(
				ENV_DEFINITION_XML_PATH,
				true,
				"Output file path that will contain the environment XML if one is generated by this build");
		options.addOption(
				BUILD_DATA_PATH,
				true,
				"Output file path that will contain the build plan in JSON format for the benefit of the UI.");
		options.addOption(
				COMBINED_BUILD_PLAN_XML_PATH,
				true,
				"Output file path that will contain the result of combining all the relevent components of the plan files into one XML file.");
		options.addOption(
				REF_PLAN_DIR,
				true,
				"Location of the reference build plan for comparison.");
		options.addOption(
				WORK_DIR,
				true,
				"Directory to be used for this program's work (e.g. checkout of hiera data). It is intended that this directory " +
				"is safe to be deleted by calling code after the CLI program has terminated.");

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
