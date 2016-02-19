package com.ipt.ebsa.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Load config and make it available to other components.
 * This configuration factory will serve properties sourced from many locations.
 * 1) A file called config.properties is loaded from the classpath.  This contains sensible defaults.
 * 2) If the "configFile" attribute is set before the getProperties method is called for the first time then 
 *    the properties from this configFile will be included in the properties.  These will take precedence over any 
 *    similar properties which were defined in the default config.properties
 * 3) Finally, any of the properties can be overridden at any time by any component that has access to the configuration factory
 * 
 * @author Stephen Cowx
 * 
 */
public class ConfigurationFactory {

	private static Logger LOG = LogManager.getLogger(ConfigurationFactory.class);
	public static final String USER_HOME = "user.home";
	public static final String PROPS_PATH = "config.properties";
	public static final String ORGANISATIONS = "config.organisations";

	private volatile static Properties properties;
	private static File configFile = null;
	
	/**
	 * @author James Shepherd
	 */
	public enum CommonOrgProperty {
		ENVIRONMENT_DEFINITION_PREFIX("environment.prefix"),
		ENVIRONMENT_DEFINITION_SUFFIX("environment.suffix");
		
		private String keySuffix;
		
		CommonOrgProperty(String keySuffix) {
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
	
	/**
	 * Returns a Map of Organisations keyed on the organisation name
	 * @return
	 */
	public static Map<String, Organisation> getOrganisations() {
		Map<String, Organisation> organisations = new TreeMap<String, Organisation>();
		String organisationProperty = ConfigurationFactory.getConfiguration(ORGANISATIONS);
		if (StringUtils.isNotBlank(organisationProperty)) {
			String[] orgs = organisationProperty.split(",");
			for (int i = 0; i < orgs.length; i++) {
				String orgPrefix = orgs[i];
				organisations.put(orgPrefix, new Organisation(orgPrefix));	
			}
		}
		return organisations;
	}

	/**
	 * IPT_, HO_IPT_, etc.
	 */
	public static String getEnvironmentDefinitionPrefix(Organisation organisation) {
		return ConfigurationFactory.getConfiguration(CommonOrgProperty.ENVIRONMENT_DEFINITION_PREFIX.getKey(organisation));
	}
	
	public synchronized static Properties getProperties() {

		if (properties == null) {
			properties = new Properties();
			try {
				LOG.debug("readProperties start");

				if (ConfigurationFactory.configFile != null){
					properties.load(new FileInputStream(ConfigurationFactory.configFile));
				}
				
				// Load a default properties file from the class path and apply them into the user provided properties
				// if the user provided properries already contains an equal key then don't apply the default
				Properties defaultProperties = new Properties();
				defaultProperties.load(ConfigurationFactory.class.getClassLoader().getResourceAsStream(PROPS_PATH));
				Set<Object> keys = defaultProperties.keySet();
				for (Object key : keys) {
					if (properties.get(key) == null) {
						properties.put(key, defaultProperties.get(key));
					}
				}
				
				LOG.debug("readProperties end");
			} catch (IOException ex) {
				LOG.error("IOException", ex);
				throw new RuntimeException(ex);
			}

		}

		return properties;
	}
	
	public static void setConfigFile(File configFile) {
		ConfigurationFactory.configFile = configFile;
	}

	public static File getConfigFile() {
		return ConfigurationFactory.configFile;
	}
	
	/**
	 * Returns a String value for a key. Key can be fully.qualified.className.memberVariableName, className.memberVariableName or just memberVariableName 
	 * @param p
	 * @return
	 */
	public static String getConfiguration(String configKey) {

		Properties config = getProperties();
		
		String value = config.getProperty(configKey);
		if (value == null || value.trim().length() <1 ) {
			value = null;
		}
		
		if (configKey.toLowerCase().contains("pass") ||  configKey.toLowerCase().contains("pwd") ||  configKey.toLowerCase().contains("user") ||  configKey.toLowerCase().contains("usr")) {
			if(StringUtils.isNotBlank(value)) {
				if (configKey.equals("true")) {
					new Exception().printStackTrace();
				}
				LOG.debug("Config key='" + configKey + "' value='"+ value.replaceAll(".", "*")+"'"  );
			} else {
				LOG.debug("Config key='" + configKey + "' value='UNDEFINED"  );
			}
		}
		else {
			LOG.debug("Config key='" + configKey + "' value='" + value + "'");
		}

		
		return value;
	}

	/**
	 * Returns a Double value for a key. 
	 * @param p
	 * @return
	 */
	public static Double getConfigurationDouble(String p) {

		String val = getConfiguration(p);
		if (val == null || val.trim().length() <1 ) {
			return null;
		}
		else {
			return Double.parseDouble(val);
		}

	}
	
	/**
	 * Returns a Integer value for a key. 
	 * @param p
	 * @return
	 */
	public static Integer getConfigurationInteger(String p) {

		String val = getConfiguration(p);
		if (val == null || val.trim().length() <1 ) {
			return null;
		}
		else {
		   return Integer.parseInt(val);
		}
	}
	
	/**
	 * @param configKey
	 * @param defaultValue
	 * @return the value corresponding to configKey, or the defaultValue if there is not found
	 */
	public static String getString(String configKey, String defaultValue) {
		String value = ConfigurationFactory.getConfiguration(configKey);
		if (StringUtils.isNotBlank(value)) {
			return value;
		} else {
			return defaultValue;
		}
	}
	
	/**
	 * 
	 * @param configKey
	 * @param defaultValue
	 * @return the value corresponding to configKey, or the defaultValue if there is not found
	 */
	public static int getInt(String configKey, int defaultValue) {
		Integer v = ConfigurationFactory.getConfigurationInteger(configKey);
		
		if (null == v) {
			return defaultValue;
		}
		
		return v;
	}
	
	/**
	 * Returns a boolean, defaults to false
	 * @param key
	 * @return
	 */
	public static boolean getBoolean(String key) {
		String d = ConfigurationFactory.getConfiguration(key);
		if (StringUtils.isNotBlank(d) && d.trim().toLowerCase().equals("true")) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns a boolean, defaults to true
	 * @param key
	 * @return
	 */
	public static boolean getBooleanDefaultTrue(String key) {
		String d = ConfigurationFactory.getConfiguration(key);
		if (StringUtils.isNotBlank(d) && d.trim().toLowerCase().equals("false")) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * This replaces ~ at the beginning of the string with the home dir of the current user
	 * @param key
	 * @return File object with path the value of specified key, null of key not found
	 */
	public static File getFile(String key) {
		String path = ConfigurationFactory.getConfiguration(key);
		
		if (StringUtils.isNotBlank(path)) {
			return new File(path.replaceAll("^~", Matcher.quoteReplacement(System.getProperty(USER_HOME))));
		}
		
		return null;
	}
	
	/**
	 * Force a lazy reload of the properties.
	 * For unit test use only (hence private, invoked by reflection).
	 */
	@SuppressWarnings("unused")
	private static synchronized void reset() {
		properties = null;
	}
}