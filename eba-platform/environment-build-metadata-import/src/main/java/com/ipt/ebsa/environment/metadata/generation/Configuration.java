package com.ipt.ebsa.environment.metadata.generation;

/**
 * Configuration constants for property names in config.properties
 *
 */
public class Configuration {
	// Path and filename for the exported Configuration XML. Defaults to <environmentName>-configuration.xml in the metadata.export.dir. Directories will be created if necessary.
	public static final String EXPORT_CONFIGURATION_XML_FILE = "metadata.export.configurationXML.file";
	// Path and filename for the exported Environment XML. Defaults to <environmentName>-environment.xml in the metadata.export.dir. Directories will be created if necessary.
	public static final String EXPORT_ENVIRONMENT_XML_FILE = "metadata.export.environmentXML.file";
	// Directory to put the export XML files in if using the default output file names (i.e. when EXPORT_CONFIGURATION_XML_FILE or EXPORT_ENVIRONMENT_XML_FILE are not specified). Directories will be created if necessary.
	public static final String EXPORT_DIR = "metadata.export.dir";
	
	// Database properties 
	public static final String DATABASE_JDBC_HBM2DDL_AUTO = "database.jdbc.hbm2ddl.auto";
	public static final String DATABASE_JDBC_HIBERNATE_DIALECT = "database.jdbc.hibernateDialect";
	public static final String DATABASE_JDBC_SCHEMA = "database.jdbc.schema";
	public static final String DATABASE_JDBC_PASSWORD = "database.jdbc.password";
	public static final String DATABASE_JDBC_USERNAME = "database.jdbc.username";
	public static final String DATABASE_JDBC_URL = "database.jdbc.url";
	public static final String DATABASE_JDBC_DRIVER = "database.jdbc.driver";
}
