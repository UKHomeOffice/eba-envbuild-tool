package com.ipt.ebsa.manage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.config.BuildProperties;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.database.manager.GlobalConfig;
import com.ipt.ebsa.database.manager.GlobalConfig.ConnectionD;
import com.ipt.ebsa.environment.build.entities.Environment;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.manage.deploy.ApplicationDeploymentManager;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeploymentManager;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.puppet.SshManager;
import com.ipt.ebsa.manage.util.Utils;
import com.ipt.ebsa.ssh.HostnameUsernamePort;
import com.ipt.ebsa.util.OrgEnvUtil;

public class EnvironmentManagementCLI {

	private static final String	CMD_LINK_TEST			= "linkTest";
	
	// This is the application deployment command (SS2)
	private static final String	CMD_DEPLOY				= "deploy";
	
	// This is the composite release deployment command (SS3)
	private static final String	CMD_DEPLOY_COMPOSITE 	= "deployComposite";
	
	private static final String	CMD_CHECKOUT			= "checkout";
	private static final String	CMD_CHECKOUT_COMPOSITE	= "checkoutComposite";
	private static final String	CMD_ENVIRONMENTS		= "listEnvironments";
	private static final String	HELP					= "help";
	private static final String	CONFIG					= "config";
	private static final String	DEPLOY_APP_VAR			= "applicationVersionId";
	private static final String DEPLOY_COMP_APP_VAR		= "releaseVersionId";
	private static final String DEPLOY_COMP_DD_VAR		= "deploymentDescriptorFile";
	
	// This is the arg for where to deploy an application release (SS2) - it is actually a zone but has been
	// misrepresented as an environment. This arg will be overloaded to be an actual environment name but 
	// we need to handle both cases.
	// Current SS2 passes this arg as a zone with value such as 'HO_IPT_NP_PRP2_DAZO' or 'IPT_ST_SIT1_COR1'
	// It will change to be an environment 'HO_IPT_NP_PRP2' or 'IPT_ST_SIT1'
	private static final String	DEPLOY_ZONE_ENV_VAR		= "environmentName";
	// If an actual, bonefide  environment is provided then expect also: -envOverload=true 
	private static final String DEPLOY_ZONE_ENV_VAR_OL	= "envOverload";
	
	// This will be the arg for deploying a composite release (SS3)
	// SS3 should pass this arg with value such as 'HO_IPT_NP_PRP2' or 'IPT_ST_SIT1'
	private static final String	DEPLOY_COMP_ENV_VAR		= "environmentName";
	
	private static final String		LINK_TEST_ORG 		= "linkTestOrg";
	private static final String		COMMAND				= "command";
	private static final String[]	SUPPORTED_COMMANDS	= new String[] { CMD_DEPLOY,
			CMD_DEPLOY_COMPOSITE, CMD_CHECKOUT, CMD_LINK_TEST, CMD_ENVIRONMENTS };
	
	/**
	 * Tech Debt! We should have a better way to acquire this property.
	 */
	private static final String SCHEME_NAME_ENVIRONMENT_VAR = "schemename";
	
	private static final Logger 	LOG 				= LogManager.getLogger(EnvironmentManagementCLI.class);
	/**
	 * Parses and checks command line options, kicks off the relevant command
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		LOG.info("environment-management version: " + BuildProperties.getMvnVersion("environment-management"));
		
		/* now we can resume normal startup */
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

				/* This puts left over arguments that are added onto the command line into the Configuration */
				manageAdditionalArgs(line);
				
				// Scheme name is an optional CL arg and in most cases will be null
				String schemeName = System.getenv(SCHEME_NAME_ENVIRONMENT_VAR);

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
				if (command.equals(CMD_DEPLOY)) {
					String applicationVersionId = manageArgument(line, DEPLOY_APP_VAR);
					String zoneOrEnvName = manageArgument(line, DEPLOY_ZONE_ENV_VAR);
					String envNameOverload = manageArgument(line, DEPLOY_ZONE_ENV_VAR_OL);
					boolean isEnv = envNameOverload != null;
					
					/* Validate that basic mandatory properties have been set */
					mandatoryProperties = new String[] { DEPLOY_APP_VAR, DEPLOY_ZONE_ENV_VAR };
					mandatoryValues = new String[] { applicationVersionId, zoneOrEnvName };
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);
					
					ApplicationDeploymentManager manager = null;
					try {
						manager = new ApplicationDeploymentManager(applicationVersionId, zoneOrEnvName, isEnv, schemeName);
						manager.execute();
					} finally {
						manager.cleanUp();
					}
				} else if (command.equals(CMD_DEPLOY_COMPOSITE)) {
					String releaseVersionId = manageArgument(line, DEPLOY_COMP_APP_VAR);
					String environmentName = manageArgument(line, DEPLOY_COMP_ENV_VAR);
					String deploymentDescriptorFile = manageArgument(line, DEPLOY_COMP_DD_VAR);
					
					/* Validate that basic mandatory properties have been set */
					mandatoryProperties = new String[] { DEPLOY_COMP_APP_VAR, DEPLOY_COMP_ENV_VAR, DEPLOY_COMP_DD_VAR };
					mandatoryValues = new String[] { releaseVersionId, environmentName, deploymentDescriptorFile };
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);
					
					CompositeReleaseDeploymentManager manager = null;
					try {
						manager = new CompositeReleaseDeploymentManager(releaseVersionId, environmentName, deploymentDescriptorFile);
						manager.execute();
					} finally {
						manager.cleanUp();
					}
				} else if (command.equals(CMD_CHECKOUT)) {
					String localCheckoutDirectory = manageArgument(line, Configuration.GIT_LOCAL_CHECKOUT_DIR);
					mandatoryProperties = new String[] { Configuration.GIT_LOCAL_CHECKOUT_DIR };
					mandatoryValues = new String[] { localCheckoutDirectory };
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);
					
					LOG.info("Checking out to local files.");

					/* Check out the deployment descriptor files */
					EMGitManager.checkoutRemoteEnvironmentDeploymentDescriptorsFiles();

					/* Check out the hiera files */
					EMGitManager.checkoutRemoteHieraFiles();
				} else if (command.equals(CMD_CHECKOUT_COMPOSITE)) {
					String localCheckoutDirectory = manageArgument(line, Configuration.GIT_LOCAL_CHECKOUT_DIR);
					mandatoryProperties = new String[] { Configuration.GIT_LOCAL_CHECKOUT_DIR };
					mandatoryValues = new String[] { localCheckoutDirectory };
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);
					
					LOG.info("Checking out to local files.");
					/* Check out the composite release (SSv3) deployment descriptor files */
					EMGitManager.checkoutRemoteCompositeDeploymentDescriptorsFiles();
				} else if (command.equals(CMD_LINK_TEST)) {
					String linkTestOrg = manageArgument(line, LINK_TEST_ORG);
					
					/* Validate that basic mandatory properties have been set */
					mandatoryProperties = new String[] { LINK_TEST_ORG };
					mandatoryValues = new String[] { linkTestOrg };
					validateMandatoryArguments(mandatoryProperties, mandatoryValues);
					
					int timeout = Configuration.getPuppetMasterTimeout();
					String commandString = "whoami";
					String username = Configuration.getPuppetMasterLoginUsername();
					
					Organisation organisation = ConfigurationFactory.getOrganisations().get(linkTestOrg);
					
					String host = Configuration.getPuppetMaster(organisation);
					List<HostnameUsernamePort> jumphosts = Utils.getJumphostsForPuppetMaster(organisation, username);
					int port = Configuration.getPuppetMasterPort();
					
					SshManager ssh = new SshManager();
					// Test connection
					// Override OpenSSH config logic has been removed
					// It is expected that the caller would provide a property to override the absolute path to the environment OpenSSH config file
					// e.g. puppet.openssl.config.filename=/opt/environment-management/config/ssh_config
					int exitCode = ssh.runSSHExec(timeout, commandString, username, host, port, jumphosts);
					LOG.info("EXIT_CODE=["+exitCode+"]");
										
					if (exitCode != 0) {
						LOG.error("The linktest failed for " + linkTestOrg);
						LOG.error("This failure may be because an OpenSSH config file could not be found - provide an absolute path in the config file, e.g. puppet.openssl.config.filename=/path/to/file/np-config.properties");
					}
					
					if (exitCode != 0) {
						exit(1);
					}

				} else if (command.equals(CMD_ENVIRONMENTS)) {
					// Get environments from config
					Set<String> environments = Configuration.getEnvironmentSet();
					
					if (environments.isEmpty()) {
						LOG.info("No environments found in config. Retrieving from the database");
						
						// Get environments from the database
						String driver = Configuration.getEnvJdbcDriver();
						String url = Configuration.getEnvJdbcUrl();
						String username = Configuration.getEnvJdbcUsername();
						String password = Configuration.getEnvJdbcPassword();
						String schema = Configuration.getEnvJdbcSchema();
						String autodll = Configuration.getEnvJdbcHbm2ddlAuto();
						String dialect = Configuration.getEnvJdbcDialect();
						
						GlobalConfig.getInstance().setSharedConnectionData(new ConnectionD(driver, url, username, password, schema, autodll, dialect));
						
						ReadManager envReader = new ReadManager();
						environments = new TreeSet<>();
						for (Environment environment : envReader.getEnvironments()) {
							String name = environment.getName();
							if (name != null) {
								name = name.trim();
								if (!name.isEmpty() && (OrgEnvUtil.isST(name) || OrgEnvUtil.isNP(name))) {
									environments.add(name);
								} else {
									LOG.warn(String.format("Skipping invalid environment name from database: %s", name));
								}
							}
						}
					} else {
						LOG.info(String.format("Found %d environments in config", environments.size()));
					}
					
					// Get the filename
					String file = Configuration.getEnvironmentSetFile();
					
					JsonArrayBuilder builder = Json.createArrayBuilder();
					for (String environment : environments) {
						builder.add(environment);
					}
					JsonWriter writer = null;
					try {
						writer = Json.createWriter(new FileWriter(new File(file)));
						writer.writeArray(builder.build());
						LOG.info(String.format("Written environments to file %s", file));
					} catch (Exception e) {
						LOG.error(e);
					} finally {
						if (writer != null) {
							try {
								writer.close();
							} catch (Exception e) {
								LOG.error(e);
							}
						}
					}
				} else {
					LOG.error("'" + command
									+ "' is either not supported or has not been implemented. Supported (but not neccessarily implemented) commands are: "
									+ Arrays.toString(getSupportedCommands()));
				}
			} catch (Exception e) {
				LOG.error("Failed to execute command", e);
				exit(1);
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
		return SUPPORTED_COMMANDS;
	}

	private static void displayHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(300);
		formatter.printHelp("java -jar environment-management.jar [options]", options);

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
		
		// Self service v2
		options.addOption(DEPLOY_APP_VAR, true, "The database ID of the ApplicationVersion which is being deployed");
		options.addOption(DEPLOY_ZONE_ENV_VAR, true,
				"The name of the zone/environment to which the ApplicationVersion needs to be deployed.");
		options.addOption(DEPLOY_ZONE_ENV_VAR_OL, true, String.format("Flag to indicate that '-%s' is actually passing an environment, not a zone.", DEPLOY_ZONE_ENV_VAR));
		
		options.addOption(LINK_TEST_ORG, true, "The org that should be link tested (e.g. st, np, pr).");
		
		options.addOption(HELP, true, "Display this menu");
		
		// Self service v3
		options.addOption(DEPLOY_COMP_APP_VAR, true, "The database ID of the ReleaseVersion which is being deployed.");
		options.addOption(DEPLOY_COMP_DD_VAR, true, "The composite release deployment descriptor file used for the deployment.");
		options.addOption(DEPLOY_COMP_ENV_VAR, true, "The name of the environment to which the ReleaseVersion needs to be deployed.");
		
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
