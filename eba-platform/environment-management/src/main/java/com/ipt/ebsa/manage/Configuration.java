package com.ipt.ebsa.manage;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;

/**
 * Very basic properties backed configuration
 */
public class Configuration {

	// default values
	public static final int		DEFAULT_TIMEOUT													= 30000;
	public static final int		DEFAULT_DO_PUPPET_TIMEOUT										= 3000000;
	public static final int		DEFAULT_SSH_PORT												= 22;
	public static final int		DEFAULT_YUM_UPDATE_POLL_SECS									= 20 * 60;
	public static final int		DEFAULT_YUM_UPDATE_SSH_TIMEOUT_MILLIS							= 2 * 60 * 1000;
	public static final int		DEFAULT_YUM_UPDATE_BETWEEN_POLL_SECS							= 5;
	public static final String	DEFAULT_CREATEREPO_Q_TOOL_DIR									= "/opt/createrepo-q-tool";
	public static final String	DEFAULT_PUPPET_PERFORM_RUN										= "run";
	public static final String	DEFAULT_PUPPET_DUMMY_RUN										= "check";
	public static final String	DEFAULT_IDENTITY_FILE_NAME										= "id_rsa";
	public static final String	DEFAULT_OPENSSL_FOLDER_NAME										= ".ssh";
	public static final boolean DEFAULT_DEPLOYMENT_CONFIG_CREATE_HIERA							= false;
	public static final String	NAME_OF_HIERA_FOLDER											= "Hiera";
	public static final String	NAME_OF_ENVIRONMENT_CONFIGURATION_FOLDER						= "EnvironmentConfiguration";
	public static final String	ENVIRONMENT_DEPLOYMENT_DESCRIPTORS								= "EnvironmentDeploymentDescriptors";
	public static final String	COMPOSITE_DEPLOYMENT_DESCRIPTORS								= "CompositeDeploymentDescriptors";
	public static final String	KNOWN_HOSTS														= "known_hosts";
	public static final String	OPEN_SSL_DEFAULT_CONFIG_FILENAME								= "config";
	public static final int		DEFAULT_GIT_RETRY_COUNT											= 5;
	public static final int		DEFAULT_PUPPET_RETRY_COUNT										= 4;
	public static final int		DEFAULT_PUPPET_RETRY_DELAY_SECONDS								= 60;
	public static final int		DEFAULT_JIT_YUM_ERROR_COUNT										= 10;
	public static final boolean DEFAULT_ENABLE_ALTERNATIVE_ENVIRONMENT_STATE					= false;
	public static final String 	DEFAULT_PRIMARY_ENVIRONMENT_STATE								= "com.ipt.ebsa.manage.hiera.HieraEnvironmentStateManager";
	public static final String 	DEFAULT_ALTERNATIVE_ENVIRONMENT_STATE							= "com.ipt.ebsa.manage.mco.MCOEnvironmentStateManager";
	public static final int		DEFAULT_ENVIRONMENT_STATE_MANAGER_MCO_TIMEOUT					= 60;
	public static final String	DEFAULT_DEPLOYMENT_ENVIRONMENTS_FILE							= "Environments.json";
	public static final int		DEFAULT_DEPLOYMENT_MAX_WAIT_SECS								= 60*60;
	// key names
	public static final String	FILE_SEPARATOR													= "file.separator";
	public static final String	GIT_LOCAL_CHECKOUT_DIR											= "git.local.checkout.dir";
	public static final String	GIT_REMOTE_CHECKOUT_ENVIRONMENT_DEPLOYMENT_DESCRIPTORS_REPO_URL	= "git.remote.checkout.environment.deployment.descriptors.repo.url";
	public static final String	GIT_REMOTE_CHECKOUT_COMPOSITE_DEPLOYMENT_DESCRIPTORS_REPO_URL	= "git.remote.checkout.composite.deployment.descriptors.repo.url";
	public static final String	GIT_REMOTE_CHECKOUT_ENVIRONMENT_CONFIGURATION_REPO_URL			= "git.remote.checkout.environment.configuration.repo.url";
	// This one is used by the plugin to find environments
	public static final String	GIT_REMOTE_CHECKOUT_HIERA_REPO_URL								= "git.remote.checkout.hiera.repo.url";
	// This one is used to find and commit hiera for a deployment
	public static final String	GIT_REMOTE_CHECKOUT_REPO_URL									= "git.remote.checkout.repo.url";
	public static final String	GIT_USERNAME													= "git.username";
	public static final String	GIT_PASSWORD													= "git.password";
	public static final String	GIT_CREDENTIALSINTERACTIVE										= "git.allow.interactive.passsword";
	public static final String	DEPLOYMENT_CONFIG_PREPARE_ONLY									= "deployment.config.prepareOnly";
	public static final String	DATABASE_JDBC_HBM2DDL_AUTO										= "database.jdbc.hbm2ddl.auto";
	public static final String	DATABASE_JDBC_HIBERNATE_DIALECT									= "database.jdbc.hibernateDialect";
	public static final String	DATABASE_JDBC_SCHEMA											= "database.jdbc.schema";
	public static final String	DATABASE_JDBC_PASSWORD											= "database.jdbc.password";
	public static final String	DATABASE_JDBC_USERNAME											= "database.jdbc.username";
	public static final String	DATABASE_JDBC_URL												= "database.jdbc.url";
	public static final String	DATABASE_JDBC_DRIVER											= "database.jdbc.driver";
	public static final String	ENV_DATABASE_JDBC_HBM2DDL_AUTO									= "env.database.jdbc.hbm2ddl.auto";
	public static final String	ENV_DATABASE_JDBC_HIBERNATE_DIALECT								= "env.database.jdbc.hibernateDialect";
	public static final String	ENV_DATABASE_JDBC_SCHEMA										= "env.database.jdbc.schema";
	public static final String	ENV_DATABASE_JDBC_PASSWORD										= "env.database.jdbc.password";
	public static final String	ENV_DATABASE_JDBC_USERNAME										= "env.database.jdbc.username";
	public static final String	ENV_DATABASE_JDBC_URL											= "env.database.jdbc.url";
	public static final String	ENV_DATABASE_JDBC_DRIVER										= "env.database.jdbc.driver";
	public static final String	DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER				= "deployment.config.preparationSummaryReportFolder";
	public static final String	DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FILENAME			= "deployment.config.preparationSummaryReportFilename";
	public static final String	DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_WRAP_IN_TEMPLATE   = "deployment.config.preparationSummaryReportWrapInTemplate";
	public static final String	DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_AUTO_REFRESH_ENABLED      = "deployment.config.preparationSummaryReportAutoRefreshEnabled";
	public static final String	DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_USE_UNIQUE_NAME	= "deployment.config.preparationSummaryReport.useUniqueName";
	public static final String	DEPLOYMENT_CONFIG_WORK_FOLDER									= "deployment.config.workFolder";
	public static final String	DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR							= "deployment.config.deploymentDescriptor";
	public static final String	DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX						= "deployment.config.hieraOrganisationPrefix";
	public static final String	DEPLOYMENT_CONFIG_HIERA_FOLDER									= "deployment.config.hieraFolder";
	public static final String	DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA							= "deployment.config.dontcheckoutddfolder";
	public static final String	DEPLOYMENT_CONFIG_USE_EXISTING_COMPOSITE_DD_DATA				= "deployment.config.dontcheckoutcompositeddfolder";
	public static final String	DEPLOYMENT_CONFIG_YUM_TEST_FOLDER								= "deployment.config.yumTestFolder";
	public static final String	DEPLOYMENT_ENVIRONMENTS											= "deployment.environments";
	public static final String	DEPLOYMENT_ENVIRONMENTS_FILE									= "deployment.environments.file";
	public static final String	DEPLOYMENT_MAX_WAIT_SECS										= "deployment.wait.maxsecs";
	public static final String	PUPPET_MASTER_UPDATE_TIMEOUT									= "puppet.master.update.timeout";
	public static final String	PUPPET_MASTER_UPDATE_LOGIN_USERNAME								= "puppet.master.update.login.username";
	public static final String	PUPPET_MASTER_UPDATE_LOGIN_PORT									= "puppet.master.update.login.port";
	public static final String	PUPPET_OPENSSL_CONFIG_FILENAME									= "puppet.openssl.config.filename";
	public static final String	PUPPET_OPENSSL_CONFIG_DIRECTORY									= "puppet.openssl.config.directory";
	public static final String	PUPPET_OPENSSL_DIRECTORY_NAME									= "puppet.openssl.directory.name";
	public static final String	PUPPET_OPENSSL_IDENTITY_FILENAME								= "puppet.openssl.identity.filename";
	public static final String	PUPPET_OPENSSL_KNOWN_HOSTS_FILENAME								= "puppet.openssl.known.hosts.filename";
	public static final String	PUPPET_DO_PUPPET_RUN_COMMAND									= "puppet.do.puppet.run.command";
	public static final String	PUPPET_DO_PUPPET_RUN_TIMEOUT									= "puppet.do.puppet.run.timeout";
	public static final String	PUPPET_MASTER_PERFORM_RUN										= "puppet.master.perform.run";
	public static final String	MCOLLECTIVE_COMMAND_MASK										= "mcollective.command.mask";
	public static final String	USER_NAME														= "user.name";
	public static final String	OPEN_SSL_CONFIG_OVERIDE_TEXT 									= "open.ssl.config.overide.text";
	public static final String	OPEN_SSL_USE_PTY												= "open.ssl.use.pty";
	public static final String	OPEN_SSL_LOGGING_LEVELS											= "open.ssl.logging.levels";
	public static final String	USER_HOME														= "user.home";
	public static final String	YUM_REPO_UPDATE_POLL_TIMEOUT_SECONDS							= "yum.repo.update.poll.timout.secs";
	public static final String	YUM_REPO_UPDATE_SSH_TIMEOUT_MILLIS								= "yum.repo.update.ssh.timout.millis";
	public static final String	CREATEREPO_Q_TOOL_DIR											= "createrepo.q.tool.dir";
	public static final String	YUM_REPO_UPDATE_BETWEEN_POLL_SECONDS							= "yum.repo.update.between.poll.secs";
	public static final String	GIT_RETRY_COUNT													= "git.retry.count";
	public static final String	ENABLE_MCO														= "deployment.config.mco.enabled";
	public static final String	PUPPET_RETRY_COUNT												= "puppet.retry.count";
	public static final String	PUPPET_RETRY_DELAY_SECONDS										= "puppet.retry.delay.secs";
	public static final String	JIT_YUM_ERROR_COUNT												= "yum.jit.error.count";
	public static final String 	DEPLOYMENT_CONFIG_CREATE_HIERA									= "deployment.config.createHiera";
	public static final String	DEPLOYMENT_CONFIG_ENABLE_ALTERNATIVE_ENVIRONMENT_STATE			= "deployment.config.alternativeEnvironmentState.enabled";
	public static final String	ALTERNATIVE_ENVIRONMENT_STATE									= "deployment.config.alternativeEnvironmentState.class";
	public static final String	PRIMARY_ENVIRONMENT_STATE										= "deployment.config.primaryEnvironmentState.class";
	public static final String	ENVIRONMENT_STATE_MANAGER_MCO_TIMEOUT							= "environmentStateManager.mco.timeout";
	public static final String	STASH_SS2_DD_URL_TEMPLATE										= "stash.ss2.dd.url.template";
	public static final String	STASH_SS3_DD_URL_TEMPLATE										= "stash.ss3.dd.url.template";
	
	/**
	 * @author James Shepherd
	 */
	public enum OrgProperty {
		HIERA_EXT_ENABLED("hiera.ext.repo.enabled"),
		HIERA_EXT_REPO("hiera.ext.repo"),
		PUPPET_MASTER_UPDATE_COMMAND("puppet.master.update.command"),
		PUPPET_MASTER_HOST("puppet.master.host"),
		YUM_REPO_JUMPHOSTS("yum.repo.jumphosts"),
		YUM_REPO_HOST("yum.repo.host"),
		YUM_REPO_USERNAME("yum.repo.username"),
		YUM_REPO_DIR("yum.repo.dir"),
		YUM_REPO_UPDATE_ENABLED("yum.repo.update.enabled"),
		YUM_REPO_UPDATE_PORT("yum.repo.port"),
		PUPPET_MASTER_JUMPHOSTS("puppet.master.jumphosts"),
		ENVIRONMENT_DEFINITION_PREFIX("environment.prefix"), 
		RPM_FAIL_FILE_REPORT_ENABLED("rpm.failfile.report.enabled");
		
		private String keySuffix;
		
		OrgProperty(String keySuffix) {
			this.keySuffix = keySuffix;
		}
		
		public String getKeySuffix() {
			return keySuffix;
		}
		
		/**
		 * @param organisation org
		 * @return The key name in the properties file
		 */
		public String getKey(Organisation organisation) {
			return organisation.getShortName() + "." + getKeySuffix();
		}
	}
	
	public static boolean getIsMcoEnabled() {
		return getBooleanDefaultTrue(ENABLE_MCO);
	}
	
	public static String getHieraFolder() {
		return ConfigurationFactory.getConfiguration(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER);
	}

	public static String getHieraOrganisationPrefix() {
		return ConfigurationFactory.getConfiguration(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX);
	}
	
	public static String getYumTestFolder() {
		return ConfigurationFactory.getConfiguration(Configuration.DEPLOYMENT_CONFIG_YUM_TEST_FOLDER);
	}

	public static String getDeploymentDescriptorFile() {
		return ConfigurationFactory.getConfiguration(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR);
	}

	public static String getWorkFolder() {
		return ConfigurationFactory.getConfiguration(Configuration.DEPLOYMENT_CONFIG_WORK_FOLDER);
	}

	public static String getPreparationSummaryReportFolder() {
		return ConfigurationFactory.getConfiguration(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER);
	}

	public static String getPreparationSummaryReportFileName() {
		return ConfigurationFactory
				.getConfiguration(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FILENAME);
	}

	public static String getJdbcDriver() {
		return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_DRIVER);
	}

	public static String getJdbcUrl() {
		return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_URL);
	}

	public static String getJdbcUsername() {
		return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_USERNAME);
	}

	public static String getJdbcPassword() {
		return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_PASSWORD);
	}

	public static String getJdbcSchema() {
		return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_SCHEMA);
	}

	public static String getJdbcDialect() {
		return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_HIBERNATE_DIALECT);
	}

	public static String getJdbcHbm2ddlAuto() {
		return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_HBM2DDL_AUTO);
	}

	public static boolean isUseUniqueName() {
		return getBoolean(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_USE_UNIQUE_NAME);
	}	

	public static boolean isWrapReportInTemplate() {
		return getBoolean(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_WRAP_IN_TEMPLATE);
	}
	
	public static boolean isAutoRefreshEnabled() {
		return getBoolean(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_AUTO_REFRESH_ENABLED);
	}
	
	public static boolean isPrepareOnly() {
		return getBoolean(Configuration.DEPLOYMENT_CONFIG_PREPARE_ONLY);
	}
	
	/**
	 * Environment Management should always be checking these out, not relying on something else to have done so.
	 * This is intended for testing only. 
	 */
	public static boolean isDontCheckoutDeploymentDescriptorsFolder() {
		return getBoolean(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA);
	}
	
	public static boolean isDontCheckoutCompositeDeploymentDescriptorsFolder() {
		return getBoolean(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_COMPOSITE_DD_DATA);
	}

	public static String getGITUsername() {
		return ConfigurationFactory.getConfiguration(Configuration.GIT_USERNAME);
	}
	
	public static String getGITPassword() {
		return ConfigurationFactory.getConfiguration(Configuration.GIT_PASSWORD);
	}
	
	public static boolean isAllowInteractivePasswordProvision() {
		return getBoolean(Configuration.GIT_CREDENTIALSINTERACTIVE);
	}
	
	public static String getRemoteCheckoutRepoUrl(Organisation organisation) {
		if (!isHieraExtReposEnabled(organisation)) {
			return ConfigurationFactory.getConfiguration(Configuration.GIT_REMOTE_CHECKOUT_REPO_URL);
		}

		return ConfigurationFactory.getConfiguration(OrgProperty.HIERA_EXT_REPO.getKey(organisation));
	}

	public static String getRemoteCheckoutHieraRepoUrl() {
		return ConfigurationFactory.getConfiguration(Configuration.GIT_REMOTE_CHECKOUT_HIERA_REPO_URL);
	}

	public static String getRemoteCheckoutEnvironmentConfigurationRepoUrl() {
		return ConfigurationFactory
				.getConfiguration(Configuration.GIT_REMOTE_CHECKOUT_ENVIRONMENT_CONFIGURATION_REPO_URL);
	}

	public static String getRemoteCheckoutEnvironmentDeploymentDescriptorsRepoUrl() {
		return ConfigurationFactory
				.getConfiguration(Configuration.GIT_REMOTE_CHECKOUT_ENVIRONMENT_DEPLOYMENT_DESCRIPTORS_REPO_URL);
	}
	
	public static String getRemoteCheckoutCompositeDeploymentDescriptorsRepoUrl() {
		return ConfigurationFactory
				.getConfiguration(Configuration.GIT_REMOTE_CHECKOUT_COMPOSITE_DEPLOYMENT_DESCRIPTORS_REPO_URL);
	}

	public static String getLocalCheckoutDir() {
		return ConfigurationFactory.getConfiguration(Configuration.GIT_LOCAL_CHECKOUT_DIR);
	}

	public static String getLocalHieraCheckoutDir() {
		return getLocalCheckoutDir() + System.getProperty(FILE_SEPARATOR) + NAME_OF_HIERA_FOLDER;
	}

	public static String getLocalEnvironmentConfigurationCheckoutDir() {
		return getLocalCheckoutDir() + System.getProperty(FILE_SEPARATOR) + NAME_OF_ENVIRONMENT_CONFIGURATION_FOLDER;
	}

	public static String getLocalEnvironmentDeploymentDescriptorsCheckoutDir() {
		return getLocalCheckoutDir() + System.getProperty(FILE_SEPARATOR) + ENVIRONMENT_DEPLOYMENT_DESCRIPTORS;
	}
	
	public static String getLocalCompositeDeploymentDescriptorsCheckoutDir() {
		return getLocalCheckoutDir() + System.getProperty(FILE_SEPARATOR) + COMPOSITE_DEPLOYMENT_DESCRIPTORS;
	}

	public static String getMCollectiveCommandMask() {
		String command = ConfigurationFactory.getConfiguration(Configuration.MCOLLECTIVE_COMMAND_MASK);
		if (StringUtils.isNotBlank(command)) {
			return command;
		}
		else {
			return "sudo -u peadmin /opt/puppet/bin/mco rpc -t $timeout $cmd $environment $filter";
		}
	}

	public static String getPuppetDoPuppetCommand() {
		String command = ConfigurationFactory.getConfiguration(Configuration.PUPPET_DO_PUPPET_RUN_COMMAND);
		if (StringUtils.isNotBlank(command)) {
			return command;
		} else {
			return "sudo -u peadmin /opt/puppet/bin/mco rpc -t $timeout gonzo $cmd $environment $filter";
		}
	}

	public static int getPuppetDoPuppetTimeout() {
		String timeout = ConfigurationFactory.getConfiguration(Configuration.PUPPET_DO_PUPPET_RUN_TIMEOUT);
		if (StringUtils.isNotBlank(timeout)) {
			return Integer.parseInt(timeout);
		} else {
			return DEFAULT_DO_PUPPET_TIMEOUT;
		}
	}
	
	public static int getPuppetRetryCount() {
		String timeout = ConfigurationFactory.getConfiguration(Configuration.PUPPET_RETRY_COUNT);
		if (StringUtils.isNotBlank(timeout)) {
			return Integer.parseInt(timeout);
		} else {
			return DEFAULT_PUPPET_RETRY_COUNT;
		}
	}
	
	public static int getPuppetRetryDelaySeconds() {
		String timeout = ConfigurationFactory.getConfiguration(Configuration.PUPPET_RETRY_DELAY_SECONDS);
		if (StringUtils.isNotBlank(timeout)) {
			return Integer.parseInt(timeout);
		} else {
			return DEFAULT_PUPPET_RETRY_DELAY_SECONDS;
		}
	}

	public static String getPuppetMasterUpdateCommand(Organisation org) {
		return ConfigurationFactory.getConfiguration(OrgProperty.PUPPET_MASTER_UPDATE_COMMAND.getKey(org));
	}

	public static int getPuppetMasterTimeout() {
		String timeout = ConfigurationFactory.getConfiguration(Configuration.PUPPET_MASTER_UPDATE_TIMEOUT);
		if (StringUtils.isNotBlank(timeout)) {
			return Integer.parseInt(timeout);
		} else {
			return DEFAULT_TIMEOUT;
		}
	}

	public static int getPuppetMasterPort() {
		String port = ConfigurationFactory.getConfiguration(Configuration.PUPPET_MASTER_UPDATE_LOGIN_PORT);
		if (StringUtils.isNotBlank(port)) {
			return Integer.parseInt(port);
		} else {
			return DEFAULT_SSH_PORT;
		}
	}

	public static String getPuppetMasterLoginUsername() {
		String username = ConfigurationFactory.getConfiguration(Configuration.PUPPET_MASTER_UPDATE_LOGIN_USERNAME);
		if (StringUtils.isNotBlank(username)) {
			return username;
		} else {

			return System.getProperty(USER_NAME);
		}
	}

	public static File getOpenSSHIdentityFile() {
		String identity = ConfigurationFactory.getConfiguration(Configuration.PUPPET_OPENSSL_IDENTITY_FILENAME);
		if (StringUtils.isNotBlank(identity)) {
			return new File(identity);
		} else {
			return new File(getOpenSSHDirectory() + System.getProperty(FILE_SEPARATOR) + getOpenSSHIdentityFilename());
		}
	}

	public static String getOpenSSHIdentityFilename() {
		String identity = ConfigurationFactory.getConfiguration(Configuration.PUPPET_OPENSSL_IDENTITY_FILENAME);
		if (StringUtils.isNotBlank(identity)) {
			return identity;
		} else {
			return DEFAULT_IDENTITY_FILE_NAME;
		}
	}

	public static File getOpenSSHKnownHosts() {
		String knownHosts = ConfigurationFactory.getConfiguration(Configuration.PUPPET_OPENSSL_KNOWN_HOSTS_FILENAME);
		if (StringUtils.isNotBlank(knownHosts)) {
			return new File(knownHosts);
		} else {
			return new File(getOpenSSHDirectory() + System.getProperty(FILE_SEPARATOR) + KNOWN_HOSTS);
		}
	}

	public static File getOpenSSHConfig() {
		String opensslDir = ConfigurationFactory.getConfiguration(Configuration.PUPPET_OPENSSL_CONFIG_FILENAME);
		if (StringUtils.isNotBlank(opensslDir)) {
			return new File(opensslDir);
		} else {
			return new File(getOpenSSHDirectory() + System.getProperty(FILE_SEPARATOR)
					+ OPEN_SSL_DEFAULT_CONFIG_FILENAME);
		}
	}

	public static File getOpenSSHDirectory() {
		String opensslDir = ConfigurationFactory.getConfiguration(Configuration.PUPPET_OPENSSL_CONFIG_DIRECTORY);
		if (StringUtils.isNotBlank(opensslDir)) {
			return new File(opensslDir);
		} else {
			return new File(System.getProperty(USER_HOME) + System.getProperty(FILE_SEPARATOR) + getOpenSSHFolderName());
		}
	}

	public static String getOpenSSHFolderName() {
		String opensslFolderName = ConfigurationFactory.getConfiguration(Configuration.PUPPET_OPENSSL_DIRECTORY_NAME);
		if (StringUtils.isNotBlank(opensslFolderName)) {
			return opensslFolderName;
		} else {
			return DEFAULT_OPENSSL_FOLDER_NAME;
		}
	}

	public static String getPuppetMaster(Organisation organisation){
		return ConfigurationFactory.getConfiguration(OrgProperty.PUPPET_MASTER_HOST.getKey(organisation));
	}
	
	public static String getYumRepoJumpHosts(Organisation organisation) {
		return ConfigurationFactory.getConfiguration(OrgProperty.YUM_REPO_JUMPHOSTS.getKey(organisation));
	}

	public static String getYumRepoHost(Organisation organisation) {
		return ConfigurationFactory.getConfiguration(OrgProperty.YUM_REPO_HOST.getKey(organisation));
	}
	
	public static String getYumRepoUsername(Organisation organisation) {
		return ConfigurationFactory.getConfiguration(OrgProperty.YUM_REPO_USERNAME.getKey(organisation));
	}

	public static String getYumRepoDir(Organisation organisation) {
		return ConfigurationFactory.getConfiguration(OrgProperty.YUM_REPO_DIR.getKey(organisation));
	}

	public static boolean getYumRepoUpdateEnabled(Organisation organisation) {
		return getBooleanDefaultTrue(OrgProperty.YUM_REPO_UPDATE_ENABLED.getKey(organisation));
	}
	
	public static boolean getRPMFailFileReportEnabled(Organisation organisation) {
		return getBooleanDefaultTrue(OrgProperty.RPM_FAIL_FILE_REPORT_ENABLED.getKey(organisation));
	}
	
	public static boolean getHieraShouldBeCreated() {
		return getBooleanDefault(Configuration.DEPLOYMENT_CONFIG_CREATE_HIERA, Configuration.DEFAULT_DEPLOYMENT_CONFIG_CREATE_HIERA);
	}

	public static int getYumRepoUpdatePort(Organisation organisation) {
		return ConfigurationFactory.getConfigurationInteger(OrgProperty.YUM_REPO_UPDATE_PORT.getKey(organisation));
	}
	
	public static int getYumRepoUpdatePollTimeoutSecs() {
		return getConfigValue(Configuration.YUM_REPO_UPDATE_POLL_TIMEOUT_SECONDS, DEFAULT_YUM_UPDATE_POLL_SECS);
	}
	
	public static int getYumRepoUpdateSshTimeoutMillis() {
		return getConfigValue(Configuration.YUM_REPO_UPDATE_SSH_TIMEOUT_MILLIS, DEFAULT_YUM_UPDATE_SSH_TIMEOUT_MILLIS);
	}
	
	public static int getYumRepoBetweenPollSecs() {
		return getConfigValue(Configuration.YUM_REPO_UPDATE_BETWEEN_POLL_SECONDS, DEFAULT_YUM_UPDATE_BETWEEN_POLL_SECS);
	}
	
	public static int getJitYumErrorCount() {
		return getConfigValue(Configuration.JIT_YUM_ERROR_COUNT, DEFAULT_JIT_YUM_ERROR_COUNT);
	}
	
	public static String getCreaterepoQToolDir() {
		return getConfigValue(Configuration.CREATEREPO_Q_TOOL_DIR, Configuration.DEFAULT_CREATEREPO_Q_TOOL_DIR);
	}

	public static String getPuppetMasterJumphosts(Organisation organisation) {
		return ConfigurationFactory.getConfiguration(OrgProperty.PUPPET_MASTER_JUMPHOSTS.getKey(organisation));
	}
	
	private static String getConfigValue(String configKey, String defaultValue) {
		String value = ConfigurationFactory.getConfiguration(configKey);
		if (StringUtils.isNotBlank(value)) {
			return value;
		} else {
			return defaultValue;
		}
	}
	
	private static int getConfigValue(String configKey, int defaultValue) {
		Integer v = ConfigurationFactory.getConfigurationInteger(configKey);
		
		if (null == v) {
			return defaultValue;
		}
		
		return v;
	}
	
	public static String getPuppetRunCommand() {
		if (getBoolean(Configuration.PUPPET_MASTER_PERFORM_RUN)) {
			return Configuration.DEFAULT_PUPPET_PERFORM_RUN;
		} else {
			return Configuration.DEFAULT_PUPPET_DUMMY_RUN;
		}
	}
	
	public static String getOpenSSLConfigOverideText() {
		String overide = ConfigurationFactory.getConfiguration(Configuration.OPEN_SSL_CONFIG_OVERIDE_TEXT);
		if (StringUtils.isNotBlank(overide)) {
			return overide;
		} else {
			return StringUtils.EMPTY;
		}
	}

	/**
	 * Whether or not JSch will allocate a Pseudo-Terminal, defaults to true
	 * @return
	 * @see <a href="http://tools.ietf.org/html/rfc4254#section-6.2">RFC4254 6.2. Requesting a Pseudo-Terminal</a>
	 */
	public static boolean getOpenSSHUsePty() {
		return getBooleanDefaultTrue(Configuration.OPEN_SSL_USE_PTY);
	}
	
	public static Set<Level> getOpenSSHLoggingLevels() {
		String levelStrings = getConfigValue(OPEN_SSL_LOGGING_LEVELS, null);
		if (levelStrings == null) {
			return null;
		}
		
		Set<Level> levels = new LinkedHashSet<>();
		for (String levelString : levelStrings.trim().split("\\s*,\\s*")) {
			if (!levelString.isEmpty()) {
				levels.add(Level.toLevel(levelString));
			}
		}
		return levels;
	}

	public static boolean isHieraExtReposEnabled(Organisation organisation) {
		return null != organisation && getBoolean(OrgProperty.HIERA_EXT_ENABLED.getKey(organisation));
	}
	
	public static int getGitPushRetryCount() {
		return getConfigValue(GIT_RETRY_COUNT, DEFAULT_GIT_RETRY_COUNT);
	}
	
	public static boolean getEnableAlternativeEnvironmentState() {
		return getBooleanDefault(Configuration.DEPLOYMENT_CONFIG_ENABLE_ALTERNATIVE_ENVIRONMENT_STATE, Configuration.DEFAULT_ENABLE_ALTERNATIVE_ENVIRONMENT_STATE);
	}
	
	public static String getAlternativeEnvironmentStateClass() {
		return getConfigValue(ALTERNATIVE_ENVIRONMENT_STATE, DEFAULT_ALTERNATIVE_ENVIRONMENT_STATE);
	}
	
	public static String getPrimaryEnvironmentStateClass() {
		return getConfigValue(PRIMARY_ENVIRONMENT_STATE, DEFAULT_PRIMARY_ENVIRONMENT_STATE);
	}
	
	public static int getEnvironmentStateMCOTimeout() {
		return getConfigValue(ENVIRONMENT_STATE_MANAGER_MCO_TIMEOUT, DEFAULT_ENVIRONMENT_STATE_MANAGER_MCO_TIMEOUT);
	}
	
	/**
	 * Returns a boolean, defaults to false
	 * @param key
	 * @return
	 */
	private static boolean getBoolean(String key) {
		return getBooleanDefault(key, false);
	}
	
	/**
	 * Returns a boolean, defaults to true
	 * @param key
	 * @return
	 */
	private static boolean getBooleanDefaultTrue(String key) {
		return getBooleanDefault(key, true);
	}
	
	private static boolean getBooleanDefault(String key, boolean defaultValue) {
		String d = ConfigurationFactory.getConfiguration(key);
		if (StringUtils.isNotBlank(d)) {
			if (d.trim().toLowerCase().equals("false")) {
				return false;
			} else if (d.trim().toLowerCase().equals("true")) {
				return true;
			}
		}
		
		return defaultValue;
	}
	
	/**
	 * Gets the configured set of environments. Environments are expected to be
	 * of the form 'IPT_ST_ENV' or 'HO_IPT_NP_ENV' are trimmed of whitespace. 
	 * @return set of environments or an empty set if none are configured
	 */
	public static Set<String> getEnvironmentSet() {
		Set<String> environmentSet = new LinkedHashSet<>();
		String environments = getConfigValue(DEPLOYMENT_ENVIRONMENTS, "");
		for (String environment : environments.trim().split("\\s*,\\s*")) {
			if (!environment.isEmpty()) {
				environmentSet.add(environment);
			}
		}
		return environmentSet;
	}
	
	public static String getEnvironmentSetFile() {
		return getConfigValue(DEPLOYMENT_ENVIRONMENTS_FILE, DEFAULT_DEPLOYMENT_ENVIRONMENTS_FILE);
	}

	public static int getDeploymentMaxWaitSecs() {
		return getConfigValue(DEPLOYMENT_MAX_WAIT_SECS, DEFAULT_DEPLOYMENT_MAX_WAIT_SECS);
	}

	public static String getEnvJdbcDriver() {
		return ConfigurationFactory.getConfiguration(Configuration.ENV_DATABASE_JDBC_DRIVER);
	}

	public static String getEnvJdbcUrl() {
		return ConfigurationFactory.getConfiguration(Configuration.ENV_DATABASE_JDBC_URL);
	}

	public static String getEnvJdbcUsername() {
		return ConfigurationFactory.getConfiguration(Configuration.ENV_DATABASE_JDBC_USERNAME);
	}

	public static String getEnvJdbcPassword() {
		return ConfigurationFactory.getConfiguration(Configuration.ENV_DATABASE_JDBC_PASSWORD);
	}

	public static String getEnvJdbcSchema() {
		return ConfigurationFactory.getConfiguration(Configuration.ENV_DATABASE_JDBC_SCHEMA);
	}

	public static String getEnvJdbcDialect() {
		return ConfigurationFactory.getConfiguration(Configuration.ENV_DATABASE_JDBC_HIBERNATE_DIALECT);
	}

	public static String getEnvJdbcHbm2ddlAuto() {
		return ConfigurationFactory.getConfiguration(Configuration.ENV_DATABASE_JDBC_HBM2DDL_AUTO);
	}

	public static String getSS2StashUrlTemplate() {
		return ConfigurationFactory.getConfiguration(STASH_SS2_DD_URL_TEMPLATE);
	}
	
	public static String getSS3StashUrlTemplate() {
		return ConfigurationFactory.getConfiguration(STASH_SS3_DD_URL_TEMPLATE);
	}
}
