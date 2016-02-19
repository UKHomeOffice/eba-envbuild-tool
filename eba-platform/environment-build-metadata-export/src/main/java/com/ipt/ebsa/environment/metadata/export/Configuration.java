package com.ipt.ebsa.environment.metadata.export;

/**
 * Configuration constants for property names in config.properties
 *
 */
public class Configuration {
	// Path and filename for the exported Environment Container XML. Defaults to <environmentContainerName>-environmentContainer.xml in the metadata.export.dir. Directories will be created if necessary.
	protected static final String EXPORT_CONFIGURATION_XML_FILE = "metadata.export.environmentContainerXML.file";
	// Path and filename for the exported Environment XML. Defaults to <environmentName>-environment.xml in the metadata.export.dir. Directories will be created if necessary.
	protected static final String EXPORT_ENVIRONMENT_XML_FILE = "metadata.export.environmentXML.file";
	// Directory to put the export XML files in if using the default output file names (i.e. when EXPORT_CONFIGURATION_XML_FILE or EXPORT_ENVIRONMENT_XML_FILE are not specified). Directories will be created if necessary.
	protected static final String EXPORT_DIR = "metadata.export.dir";
	
	// Database properties 
	protected static final String DATABASE_JDBC_HBM2DDL_AUTO = "database.jdbc.hbm2ddl.auto";
	protected static final String DATABASE_JDBC_HIBERNATE_DIALECT = "database.jdbc.hibernateDialect";
	protected static final String DATABASE_JDBC_SCHEMA = "database.jdbc.schema";
	protected static final String DATABASE_JDBC_PASSWORD = "database.jdbc.password";
	protected static final String DATABASE_JDBC_USERNAME = "database.jdbc.username";
	protected static final String DATABASE_JDBC_URL = "database.jdbc.url";
	protected static final String DATABASE_JDBC_DRIVER = "database.jdbc.driver";
}
