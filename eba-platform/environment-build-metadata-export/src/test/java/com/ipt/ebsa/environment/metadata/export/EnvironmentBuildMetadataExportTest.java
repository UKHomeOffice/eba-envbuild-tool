package com.ipt.ebsa.environment.metadata.export;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.environment.metadata.export.EnvironmentBuildMetadataExport.ExportType;
import com.ipt.ebsa.environment.metadata.export.vCloud.EnvironmentBuildMetadataVCloudExport;

public class EnvironmentBuildMetadataExportTest {
	
	@Test
	public void testGetExportFileWithExportDirOnly() {
		doTestGetExportFileWithExportDirOnly("environment-test", "unit-test" + File.separator, ExportType.environmentContainer);
		doTestGetExportFileWithExportDirOnly("evnTest", "unit-test2" + File.separator, ExportType.environment);
	}

	@Test
	public void testGetExportFileWithSpecificEnvironmentFilename() {
		String exportFilename = "unit" + File.separator + "test" + File.separator + "env.txt";
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_DIR, "");
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_ENVIRONMENT_XML_FILE, exportFilename);
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_CONFIGURATION_XML_FILE, "");
		File exportFile = new EnvironmentBuildMetadataVCloudExport().getExportFile("test", ExportType.environment);
		Assert.assertNotNull(exportFile);
		exportFile.delete();
		exportFile.getParentFile().delete();
		exportFile.getParentFile().getParentFile().delete();
		Assert.assertEquals(exportFilename, exportFile.getPath());
		// Check the EXPORT_DIR is ignored
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_DIR, File.separator + "export-dir" + File.separator);
		exportFile = new EnvironmentBuildMetadataVCloudExport().getExportFile("test", ExportType.environment);
		Assert.assertNotNull(exportFile);
		exportFile.delete();
		exportFile.getParentFile().delete();
		exportFile.getParentFile().getParentFile().delete();
		Assert.assertEquals(exportFilename, exportFile.getPath());
	}
	
	@Test
	public void testGetExportFileWithSpecificConfigurationFilename() {
		String exportFilename = "unit" + File.separator + "test" + File.separator + "conf.txt";
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_DIR, "");
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_ENVIRONMENT_XML_FILE, "");
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_CONFIGURATION_XML_FILE, exportFilename);
		File exportFile = new EnvironmentBuildMetadataVCloudExport().getExportFile("test", ExportType.environmentContainer);
		Assert.assertNotNull(exportFile);
		exportFile.delete();
		exportFile.getParentFile().delete();
		exportFile.getParentFile().getParentFile().delete();
		Assert.assertEquals(exportFilename, exportFile.getPath());
		// Check the EXPORT_DIR is ignored
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_DIR, File.separator + "export-dir" + File.separator);
		exportFile = new EnvironmentBuildMetadataVCloudExport().getExportFile("test", ExportType.environmentContainer);
		Assert.assertNotNull(exportFile);
		exportFile.delete();
		exportFile.getParentFile().delete();
		exportFile.getParentFile().getParentFile().delete();
		Assert.assertEquals(exportFilename, exportFile.getPath());
	}
	
	private void doTestGetExportFileWithExportDirOnly(String environmentName, String exportDir, ExportType exportType) {
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_DIR, exportDir);
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_ENVIRONMENT_XML_FILE, "");
		ConfigurationFactory.getProperties().setProperty(Configuration.EXPORT_CONFIGURATION_XML_FILE, "");
		File exportFile = new EnvironmentBuildMetadataVCloudExport().getExportFile(environmentName, exportType);
		Assert.assertNotNull(exportFile);
		exportFile.delete();
		exportFile.getParentFile().delete();
		Assert.assertEquals(exportDir + environmentName + "-" + exportType + ".xml", exportFile.getPath());
	}

}
