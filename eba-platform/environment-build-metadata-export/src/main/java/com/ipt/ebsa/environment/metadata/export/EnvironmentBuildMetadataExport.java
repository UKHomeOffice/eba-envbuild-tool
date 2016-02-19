package com.ipt.ebsa.environment.metadata.export;

import java.io.File;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.database.manager.ConnectionData;
import com.ipt.ebsa.database.manager.GlobalConfig;
import com.ipt.ebsa.environment.build.manager.ReadManager;

/**
 * Superclass for exporting data from the ENVIRONMENT_BUILD database 
 *
 */
public abstract class EnvironmentBuildMetadataExport {
	
	/** Logger */
	private static final Logger LOG = LogManager.getLogger(EnvironmentBuildMetadataExport.class);
	
	/** The 2 types of XML document that can be exported */
	public enum ExportType {
		environment, environmentContainer
	}
	
	/** Read Manager instance used to read from the ENVIRONMENT_BUILD database */
	protected ReadManager readManager = new ReadManager();
	
	/**
	 * Constructor 
	 */
	public EnvironmentBuildMetadataExport() {
		LOG.debug("Setting database connection details from configuration");
		GlobalConfig.getInstance().setSharedConnectionData(new ConnectionData() {
			@Override
			public String getUsername() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_USERNAME);
			}
			@Override
			public String getUrl() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_URL);
			}
			@Override
			public String getSchema() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_SCHEMA);
			}
			@Override
			public String getPassword() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_PASSWORD);
			}
			@Override
			public String getDriverClass() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_DRIVER);
			}
			@Override
			public String getDialect() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_HIBERNATE_DIALECT);
			}
			@Override
			public String getAutodll() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_HBM2DDL_AUTO);
			}
		});
	}

	/**
	 * Returns the File used for writing the export XML
	 * @param name the name of the Environment / Environment Container
	 * @param exportType
	 * @return
	 */
	protected File getExportFile(String name, ExportType exportType) {
		File exportFile = null;
		String exportFilePath = ConfigurationFactory.getString("metadata.export." + exportType + "XML.file", null);
		if (exportFilePath != null) {
			exportFile = new File(exportFilePath);
		} else {
			File exportDir = null;
			String dir = ConfigurationFactory.getString(Configuration.EXPORT_DIR, null);
			if (dir != null) {
				exportDir = new File(dir);
			}
			exportFile = new File(exportDir, name + "-" + exportType + ".xml");
		}
		if (exportFile.getParentFile() != null && !exportFile.getParentFile().exists() && !exportFile.getParentFile().mkdirs()) {
			throw new RuntimeException("Failed to create directories in path to export file: " + exportFile.getAbsolutePath());
		}
		return exportFile; 
	}
	
	/**
	 * Marshals the jaxbElement to the given writer
	 * @param jaxbElement
	 * @param writer
	 */
	protected <T> void writeTo(JAXBElement<T> jaxbElement, Writer writer) {
		try {
			JAXBContext context = JAXBContext.newInstance(jaxbElement.getDeclaredType());
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(jaxbElement, writer);
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to marshal XML to writer ", e);
		}
	}
}
