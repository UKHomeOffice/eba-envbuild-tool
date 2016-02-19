package com.ipt.ebsa.environment.metadata.export.vCloud;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.environment.metadata.export.ExportTest;
import com.ipt.ebsa.environment.metadata.generation.EnvironmentBuildMetadataImport;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualApplicationType;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualMachineType;

/**
 * Unit tests for the EnvironmentBuildMetadataExport
 *
 */
public class EnvironmentBuildMetadataVCloudExportTest extends ExportTest {
	
	/** Name of schema used to validate output XML */
	private static final String SCHEMA_NAME = "vCloudConfig-2.0.xsd";
	
	@Test
	public void testExtractConfigurationNoData() throws Exception {
		String environmentContainerName = "not found";
		String version = "1.0";
		try {
			new EnvironmentBuildMetadataVCloudExport().extractConfiguration(environmentContainerName, version);;
		} catch (RuntimeException e) {
			Assert.assertEquals("No Environment Container Definition found for environment container name: " + environmentContainerName + ", environment container definition version: " + version, e.getMessage());
		}
	}
	
	@Test
	public void testExportEnvironmentNoData() throws Exception {
		String environmentName = "not found";
		String version = "";
		try {
			new EnvironmentBuildMetadataVCloudExport().exportEnvironment(environmentName, version);;
		} catch (RuntimeException e) {
			Assert.assertEquals("No Physical Environment Definition found for environmentName: " + environmentName + ", version: " + version, e.getMessage());
		}
	}
	
	@Test
	public void testExportConfiguration() throws Exception {
		Assert.assertTrue("Export file absent " + EXPORT_ENVIRONMENT_CONTAINER_FILE.getAbsolutePath(), !EXPORT_ENVIRONMENT_CONTAINER_FILE.exists() || EXPORT_ENVIRONMENT_CONTAINER_FILE.delete());
		// Import some data into the database first so it can then be exported
		File input = new File("src/test/resources/HO_IPT_NP_PRP1_OrgNetworks.xml");
		//File input = new File("src/test/resources/ VCloud Client Test Env 16889_OrgNetworks.xml");
		ImportVCloudXML.importConfiguration(input);
		// Export the Configuration details
		new EnvironmentBuildMetadataVCloudExport().exportConfiguration("np", "1.0");
		// Verify the export
		Assert.assertTrue("Export file created " + EXPORT_ENVIRONMENT_CONTAINER_FILE, EXPORT_ENVIRONMENT_CONTAINER_FILE.exists());
		assertXMLValid(EXPORT_ENVIRONMENT_CONTAINER_FILE, SCHEMA_NAME);
		assertXMLEqual(input, EXPORT_ENVIRONMENT_CONTAINER_FILE);
	}
	
	@Test
	public void testExportEnvironmentBeforeEnvironmentContainerDefinitionDeployed() throws Exception {
		Assert.assertTrue("Export file absent " + EXPORT_ENVIRONMENT_FILE.getAbsolutePath(), !EXPORT_ENVIRONMENT_FILE.exists() || EXPORT_ENVIRONMENT_FILE.delete());
		// Import some environment data into the database first so it can then be exported
		File input = new File("src/test/resources/HO_IPT_NP_PRP1_vCloud.xml");
		ImportVCloudXML.importEnvironment(input);
		// Attempt to export the Environment details before an EnvironmentContainerDefinition has been deployed
		try {
			new EnvironmentBuildMetadataVCloudExport().exportEnvironment("HO_IPT_NP_PRP1", "1.0");
			Assert.fail("Expected a RuntimeException due to no successfully deployed Environment Container Definition");
		} catch (RuntimeException e) {
			Assert.assertTrue(e.getMessage().contains("No successfully deployed Environment Container Definition"));
		}
	}
	
	@Test
	public void testExportEnvironment() throws Exception {
		Assert.assertTrue("Export file absent " + EXPORT_ENVIRONMENT_FILE.getAbsolutePath(), !EXPORT_ENVIRONMENT_FILE.exists() || EXPORT_ENVIRONMENT_FILE.delete());
		// Import some environment data into the database first so it can then be exported
		File input = new File("src/test/resources/HO_IPT_NP_PRP1_vCloud.xml");
		//File input = new File("C:/ipt/ebsa-platform-components/Function/vcloud/vCloudClient/src/test/resources/createTests/vapp_config_EBSAD_16889_delete_vapp_ST1_test.xml");
		ImportVCloudXML.importEnvironment(input);
		// Import an Environment Container Definition
		new EnvironmentBuildMetadataImport(new File("src/test/resources/HO_IPT_NP_agnostic_skyscape.xml").getAbsolutePath()).importEnvironmentMetadata();
		// Load the imported Environment Container Definition
		EnvironmentContainerDefinition ecd = new ReadManager().getEnvironmentContainerDefinition("np", "1.0", XMLProviderType.SKYSCAPE.toString());
		Assert.assertNotNull(ecd);
		// Pretend it's been successfully deployed
		recordSuccessfulDeployment(ecd);
		// Export the Environment details
		new EnvironmentBuildMetadataVCloudExport().exportEnvironment("HO_IPT_NP_PRP1", "1.0");
		//new EnvironmentBuildMetadataVCloudExport().exportEnvironment("VCloud Client Test Env", "1.0");
		// Verify the export
		Assert.assertTrue("Export file created " + EXPORT_ENVIRONMENT_FILE, EXPORT_ENVIRONMENT_FILE.exists());
		assertXMLValid(EXPORT_ENVIRONMENT_FILE, SCHEMA_NAME);
		assertXMLEqual(input, EXPORT_ENVIRONMENT_FILE);
	}
	
	@Test
	public void testExportVMsWithDomain() {
		VirtualMachineContainer dbVmc = new VirtualMachineContainer();
		String domain = "test.domain";
		dbVmc.setDomain(domain);
		VirtualMachine dbVm = new VirtualMachine();
		String vmName = "vm1";
		dbVm.setVmName(vmName);
		String computerName = "computer1";
		dbVm.setComputerName(computerName);
		dbVm.setCpuCount(1);
		dbVmc.addVirtualmachine(dbVm);
		XMLVirtualApplicationType xmlVApp = new XMLVirtualApplicationType();
		new EnvironmentBuildMetadataVCloudExport().exportVirtualMachines(dbVmc, xmlVApp);
		Assert.assertEquals(1, xmlVApp.getVirtualMachine().size());
		XMLVirtualMachineType xmlVm = xmlVApp.getVirtualMachine().get(0);
		Assert.assertEquals(vmName + "." + domain, xmlVm.getVMName());
		Assert.assertEquals(computerName + "." + domain, xmlVm.getComputerName());
	}
}
