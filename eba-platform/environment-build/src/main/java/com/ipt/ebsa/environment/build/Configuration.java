package com.ipt.ebsa.environment.build;

import java.io.File;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.database.manager.ConnectionData;
import com.ipt.ebsa.database.manager.GlobalConfig;

/**
 * Very basic properties backed configuration
 */
public class Configuration {
	
	private static final Logger LOG = Logger.getLogger(Configuration.class);
	
	// key names
	public static final String SSH_KNOWNHOSTS_FILE = "ssh.knownhosts.file";
	public static final String SSH_IDENTITY_FILE = "ssh.identity.file";
	public static final String SSH_CONFIG_FILE = "ssh.config.file";
	public static final String SSH_TIMEOUT = "ssh.timeout.millis";
	public static final String SSH_ACTION_ENABLED = "ssh.action.enabled";
	public static final String INFRA_ACTION_ENABLED = "infra.action.enabled";
	public static final String ENVIRONMENT_BUILD_PLANS_GIT_URL = "build.plans.git.url";
	public static final String ENVIRONMENT_BUILD_PLANS_DIR = "build.plans.git.subdir";
	public static final String ENVIRONMENT_BUILD_STARTUP_SHUTDOWN_GIT_ROOT = "startup.shutdown.git.root";
	public static final String ENVIRONMENT_BUILD_STARTUP_SHUTDOWN_DIR = "startup.shutdown.git.subdir";
	public static final String DB_AUTODDL = "database.jdbc.hbm2ddl.auto";
	public static final String DB_USERNAME = "database.jdbc.username";
	public static final String DB_PASSWORD = "database.jdbc.password";
	public static final String DB_URL = "database.jdbc.url";
	public static final String DB_DIALECT = "database.jdbc.hibernateDialect";
	public static final String DB_DRIVERCLASS = "database.jdbc.driver";
	public static final String DB_SCHEMA = "database.jdbc.schema";
	public static final String ROLE_BASED_FILTERING_ENABLED = "role.based.filtering.enabled";
	public static final String FIREWALL_HIERA_TEMPLATES_GIT_URL = "templates.firewall.hiera.git.url";
	public static final String INTERNAL_HIERA_TEMPLATES_GIT_URL = "templates.internal.hiera.git.url";
	public static final String FIREWALL_HIERA_TEMPLATES_DIR = "templates.firewall.hiera.git.subdir";
	public static final String INTERNAL_HIERA_TEMPLATES_DIR = "templates.internal.hiera.git.subdir";
	public static final String IS_FIREWALL_HIERA_COMMIT_ENABLED = "firewall.hiera.commit.enabled";
	public static final String IS_INTERNAL_HIERA_COMMIT_ENABLED = "internal.hiera.commit.enabled";
	// The subdirectory of workdir into which the customisation scripts will be checked out
	public static final String CUSTOMISATION_SCRIPTS_CHECKOUT_DIR = "customisation.scripts.checkout.dir";
	public static final String VYATTA_FORWARD_FIREWALL_RULE_GENERATION_ENABLED = "vyatta.forward.firewall.rule.generation.enabled";
	public static final String VYATTA_HOSTAME_REGEXP = "vyatta.hostname.regexp";

	/**
	 * @author James Shepherd
	 */
	public enum OrgProperty {
		CUSTOMISATION_SCRIPTS_PATH("customisation.scripts.path"),
		CUSTOMISATION_SCRIPTS_GIT_URL("customisation.scripts.git.url");
		
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
	
	public static File getSshKnownhosts() {
		return ConfigurationFactory.getFile(SSH_KNOWNHOSTS_FILE);
	}
	
	public static File getSshIdentity() {
		return ConfigurationFactory.getFile(SSH_IDENTITY_FILE);
	}
	
	public static File getSshConfig() {
		return ConfigurationFactory.getFile(SSH_CONFIG_FILE);
	}

	public static int getSshTimeout() {
		return ConfigurationFactory.getConfigurationInteger(SSH_TIMEOUT);
	}

	public static boolean isSshActionEnabled() {
		return ConfigurationFactory.getBooleanDefaultTrue(SSH_ACTION_ENABLED);
	}
	
	public static boolean isInfraActionEnabled() {
		return ConfigurationFactory.getBooleanDefaultTrue(INFRA_ACTION_ENABLED);
	}

	public static String getEnvironmentBuildPlansGitURL() {
		return ConfigurationFactory.getConfiguration(ENVIRONMENT_BUILD_PLANS_GIT_URL);
	}
	
	public static String getEnvironmentBuildPlansDir() {
		return ConfigurationFactory.getConfiguration(ENVIRONMENT_BUILD_PLANS_DIR);
	}
	
	public static String getEnvironmentStartupShutdownGitRoot() {
		return ConfigurationFactory.getConfiguration(ENVIRONMENT_BUILD_STARTUP_SHUTDOWN_GIT_ROOT);
	}
	
	public static String getEnvironmentStartupShutdownDir() {
		return ConfigurationFactory.getConfiguration(ENVIRONMENT_BUILD_STARTUP_SHUTDOWN_DIR);
	}
	
	public static String getInternalHieraTemplatesGitUrl() {
		return ConfigurationFactory.getConfiguration(INTERNAL_HIERA_TEMPLATES_GIT_URL);
	}
	
	public static String getFirewallHieraTemplatesGitUrl() {
		return ConfigurationFactory.getConfiguration(FIREWALL_HIERA_TEMPLATES_GIT_URL);
	}
	
	public static String getInternalHieraTempatesDir() {
		return ConfigurationFactory.getConfiguration(INTERNAL_HIERA_TEMPLATES_DIR);
	}
	
	public static String getFirewallHieraTempatesDir() {
		return ConfigurationFactory.getConfiguration(FIREWALL_HIERA_TEMPLATES_DIR);
	}

	public static ConnectionData getConnectionData() {
		return new ConnectionData() {

			@Override
			public String getAutodll() {
				return ConfigurationFactory.getConfiguration(DB_AUTODDL);
			}

			@Override
			public String getDialect() {
				return ConfigurationFactory.getConfiguration(DB_DIALECT);
			}

			@Override
			public String getDriverClass() {
				return ConfigurationFactory.getConfiguration(DB_DRIVERCLASS);
			}

			@Override
			public String getPassword() {
				return ConfigurationFactory.getConfiguration(DB_PASSWORD);
			}

			@Override
			public String getSchema() {
				return ConfigurationFactory.getConfiguration(DB_SCHEMA);
			}

			@Override
			public String getUrl() {
				return ConfigurationFactory.getConfiguration(DB_URL);
			}

			@Override
			public String getUsername() {
				return ConfigurationFactory.getConfiguration(DB_USERNAME);
			}
		};
	}
	
	public static void configureConnectionData() {
		GlobalConfig.getInstance().setSharedConnectionData(getConnectionData());
	}

	public static Properties getProviderProperties(Organisation organisation, String provider) {
		Properties p = new Properties();
		String prefix = organisation.getShortName() + "." + provider.toLowerCase() + ".";
		int prefixLen = prefix.length();
		
		for (Entry<Object, Object> entry : ConfigurationFactory.getProperties().entrySet()) {
			String key = (String) entry.getKey();
			if (key.startsWith(prefix)) {
				String newKey = key.substring(prefixLen);
				LOG.debug(String.format("Adding key [%s] to properties as key [%s]", key, newKey));
				p.setProperty(newKey, (String) entry.getValue());
			}
		}
		
		return p;
	}
	
	public static boolean isRoleBasedFilteringEnabled() {
		 return ConfigurationFactory.getBooleanDefaultTrue(ROLE_BASED_FILTERING_ENABLED);
	}

	public static boolean isCommitEnabledFirewallHiera() {
		return ConfigurationFactory.getBooleanDefaultTrue(IS_FIREWALL_HIERA_COMMIT_ENABLED);
	}
	
	public static boolean isCommitEnabledInternalHiera() {
		return ConfigurationFactory.getBooleanDefaultTrue(IS_INTERNAL_HIERA_COMMIT_ENABLED);
	}
	
	/**
	 * @return The customisation script repo URL for the given org
	 */
	public static String getCustomisationScriptsGitUrl(String org) {
		Organisation organisation = ConfigurationFactory.getOrganisations().get(org.toLowerCase());
		if (organisation == null) {
			throw new IllegalStateException("Unknown org [" + org + "]");
		}
		return ConfigurationFactory.getConfiguration(OrgProperty.CUSTOMISATION_SCRIPTS_GIT_URL.getKey(organisation));
	}
	
	/**
	 * @return The customisation script path for the given org.
	 */
	public static String getCustomisationScriptPath(String org) {
		Organisation organisation = ConfigurationFactory.getOrganisations().get(org.toLowerCase());
		if (organisation == null) {
			throw new IllegalStateException("Unknown org [" + org + "]");
		}
		return ConfigurationFactory.getConfiguration(OrgProperty.CUSTOMISATION_SCRIPTS_PATH.getKey(organisation));
	}
	
	/**
	 * @return The sub-dir of the working directory that the customisation will be checked out to.
	 */
	public static String getCustomisationScriptsCheckoutDir() {
		return ConfigurationFactory.getConfiguration(CUSTOMISATION_SCRIPTS_CHECKOUT_DIR);
	}
	
	
	public static boolean isVyattaForwardRuleGenerationEnabled() {
		return ConfigurationFactory.getBooleanDefaultTrue(VYATTA_FORWARD_FIREWALL_RULE_GENERATION_ENABLED);
	}
	
	public static String getVyattaHostnameRegexp() {
		return ConfigurationFactory.getConfiguration(VYATTA_HOSTAME_REGEXP);
	}
}
