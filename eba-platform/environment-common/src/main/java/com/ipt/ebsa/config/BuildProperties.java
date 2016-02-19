package com.ipt.ebsa.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class BuildProperties {

	public static final String VERSION_PROPERTIES_SUFFIX = "-version.properties";
	public static final String APP_HOME_PROPERTY = "app.home";
	public static final String MVN_VERSION = "MVN_VERSION";
	public static final String UNKNOWN = "UNKNOWN";
	
	private static Logger LOG = Logger.getLogger(BuildProperties.class);
	/**
	 * Uses the package
	 * @param libraryName {libraryName}-version.properties file created by maven in the root of the appassembler
	 * directory to fetch the version.
	 * @return version, or {@link #UNKNOWN}
	 */
	public static String getMvnVersion(String libraryName) {
		Properties props = getBuildProperties(libraryName);
		
		if (null != props) {
			String version = props.getProperty(MVN_VERSION);
			
			if (null != version) {
				LOG.debug(String.format("Found library [%s] version [%s]", libraryName, version));
				return version;
			}
		}
		
		LOG.debug("Failed to find version for: " + libraryName);
		
		return UNKNOWN;
	}
	
	/**
	 * 
	 * @param libraryName
	 * @return properties, or null if not found/error
	 */
	private static Properties getBuildProperties(String libraryName) {
		String baseDir = System.getProperty(APP_HOME_PROPERTY);
		
		if (null != baseDir) {
			File versionProperties = new File(baseDir, libraryName + VERSION_PROPERTIES_SUFFIX);
			
			if (versionProperties.exists()) {
				Properties props = new Properties();
				try {
					props.load(new FileInputStream(versionProperties));
					return props;
				} catch (FileNotFoundException e) {
					LOG.warn("Failed to load: " + versionProperties.getAbsolutePath());
				} catch (IOException e) {
					LOG.warn("Failed to load: " + versionProperties.getAbsolutePath());
				}
			} else {
				LOG.warn("File not found: " + versionProperties.getAbsolutePath());
			}
		} else {
			LOG.warn("Couldn't get app.home system property");
		}
		
		return null;
	}
}
