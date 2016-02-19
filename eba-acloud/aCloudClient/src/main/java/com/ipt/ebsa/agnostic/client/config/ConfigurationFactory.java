package com.ipt.ebsa.agnostic.client.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

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
 *
 */
public class ConfigurationFactory {

	private static Logger logger = LogManager.getLogger(ConfigurationFactory.class);
	private volatile static Properties properties;
	public static final String propsPath = "config.properties";
	private static File configFile = null; 

	public synchronized static Properties getProperties() {

		if (properties == null) {
			try {
				logger.debug("Loading default properties from " + propsPath);
				// These defaults are only used as a fallback when a particular entry cannot be found
				// in the regular properties file
				Properties defaultProperties = new Properties();
				defaultProperties.load(ConfigurationFactory.class.getClassLoader().getResourceAsStream(propsPath));
				properties = new Properties(defaultProperties);
				
				if (ConfigurationFactory.configFile != null){
					logger.debug("Loading properties from " + ConfigurationFactory.configFile);
					properties.load(new FileInputStream(ConfigurationFactory.configFile));
				} else {
					logger.warn("Properties file not found, using properties from classpath (may be incomplete)");
				}
				
				logger.debug("readProperties end");
			} catch (IOException ex) {
				logger.error("IOException", ex);
				throw new RuntimeException(ex);
			}

		}

		return properties;
	}
	
	public static void setConfigFile(File configFile) {
		ConfigurationFactory.configFile = configFile;
	}
	
	public static String getConfigFile() {
		if(configFile != null) {
			return configFile.getAbsolutePath();
		} else {
			return StringUtils.EMPTY;
		}
	}

	/**
	 * Returns a String value for a key. Key can be fully.qualified.className.memberVariableName, className.memberVariableName or just memberVariableName 
	 * @param p
	 * @return
	 */
	public @Produces
	@Config
	String getConfiguration(InjectionPoint p) {

		String configKey = p.getMember().getDeclaringClass().getName() + "." + p.getMember().getName();
		Properties config = getProperties();
		if (config.getProperty(configKey) == null) {
			configKey = p.getMember().getDeclaringClass().getSimpleName() + "." + p.getMember().getName();
			if (config.getProperty(configKey) == null)
				configKey = p.getMember().getName();
		}
		
		String value = config.getProperty(configKey);
		if (value == null || value.trim().length() <1 ) {
			value = null;
		}
		
		if ((configKey.toLowerCase().contains("pass") ||  configKey.toLowerCase().contains("pwd") 
				||  configKey.toLowerCase().contains("user") ||  configKey.toLowerCase().contains("usr")
				||  configKey.toLowerCase().contains("keystore")) && value != null) {
			logger.debug("Config key='" + configKey + "' value='"+ value.replaceAll(".", "*")+"'"  );
		}
		else {
			logger.debug("Config key='" + configKey + "' value='" + value + "'");
		}

		
		return value;
	}

	/**
	 * Returns a Double value for a key. Key can be fully.qualified.className.memberVariableName, className.memberVariableName or just memberVariableName
	 * @param p
	 * @return
	 */
	public @Produces
	@Config
	Double getConfigurationDouble(InjectionPoint p) {

		String val = getConfiguration(p);
		if (val == null || val.trim().length() <1 ) {
			return null;
		}
		else {
			return Double.parseDouble(val);
		}

	}
	
	/**
	 * Returns a Double value for a key. Key can be fully.qualified.className.memberVariableName, className.memberVariableName or just memberVariableName
	 * @param p
	 * @return
	 */
	public @Produces
	@Config
	Integer getConfigurationInteger(InjectionPoint p) {

		String val = getConfiguration(p);
		if (val == null || val.trim().length() <1 ) {
			return null;
		}
		else {
		   return Integer.parseInt(val);
		}
	}
	
	/**
	 * Method to force a re-read of the properties from the config file. Added to allow 
	 * loading of different properties files sequentially if required. Currently not required
	 * in production use case.
	 * 
	 * @TODO - look at whether the setConfig() method should nullify the properties variable, 
	 * otherwise the new config will never be loaded?
	 */
	public static void resetProperties()
	{
		properties = null;
	}
}