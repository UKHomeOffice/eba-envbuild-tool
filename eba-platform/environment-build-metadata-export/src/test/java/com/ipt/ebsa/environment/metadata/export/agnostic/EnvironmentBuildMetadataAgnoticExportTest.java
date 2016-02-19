package com.ipt.ebsa.environment.metadata.export.agnostic;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.environment.metadata.export.ExportTest;
import com.ipt.ebsa.environment.metadata.generation.EnvironmentBuildMetadataImport;

/**
 * Unit tests for EnvironmentBuildMetadataAgnoticExport 
 * @author Mark Kendall
 *
 */
public class EnvironmentBuildMetadataAgnoticExportTest extends ExportTest {
	
	/** Name of schema used to validate output XML */
	private static final String SCHEMA_NAME = "AgnosticCloudConfig-1.0.xsd";
	
	@Test
	public void testExtractEnvironmentNoData() throws Exception {
		String environmentName = "not found";
		String version = "1.0";
		try {
			new EnvironmentBuildMetadataAgnosticExport().extractEnvironment(environmentName, version, XMLProviderType.SKYSCAPE.toString());
		} catch (RuntimeException e) {
			Assert.assertEquals("No Physical Environment Definition found for environmentName: " + environmentName + ", version: " + version + ", provider: SKYSCAPE", e.getMessage());
		}
	}
	
	@Test
	public void testExtractEnvironmentSkyscape() throws Exception {
		// Import some data into the database first so it can then be exported...
		// Import the Environment Container Definition (Org networks) first
		new EnvironmentBuildMetadataImport(new File("src/test/resources/HO_IPT_NP_agnostic_skyscape.xml").getAbsolutePath()).importEnvironmentMetadata();
		// Import the Environment Definition
		new EnvironmentBuildMetadataImport(new File("src/test/resources/HO_IPT_NP_PRP1_agnostic_skyscape.xml").getAbsolutePath()).importEnvironmentMetadata();
		// Load the imported Environment Container Definition
		EnvironmentContainerDefinition ecd = new ReadManager().getEnvironmentContainerDefinition("np", "1.0", XMLProviderType.SKYSCAPE.toString());
		Assert.assertNotNull(ecd);
		// Pretend it's been successfully deployed
		recordSuccessfulDeployment(ecd);
		// Export the Environment Definition
		new EnvironmentBuildMetadataAgnosticExport().exportEnvironment("HO_IPT_NP_PRP1_MABC", "1.0", XMLProviderType.SKYSCAPE.toString());
		//new EnvironmentBuildMetadataAgnosticExport().exportEnvironment("VCloud Client Test Env", "1.0", XMLProviderType.SKYSCAPE.toString());
		// Verify the export
		Assert.assertTrue("Export file created " + EXPORT_ENVIRONMENT_FILE, EXPORT_ENVIRONMENT_FILE.exists());
		assertXMLValid(EXPORT_ENVIRONMENT_FILE, SCHEMA_NAME);
		File expected = new File("src/test/resources/expected/HO_IPT_NP_PRP1_agnostic_export_skyscape.xml");
		assertXMLEqual(expected, EXPORT_ENVIRONMENT_FILE);
	}
	
	@Test
	public void testExtractEnvironmentBeforeEnvironmentContainerDefinitionDeployedSkyscape() throws Exception {
		// Import some data into the database first so it can then be exported...
		// Import the Environment Definition
		new EnvironmentBuildMetadataImport(new File("src/test/resources/HO_IPT_NP_PRP1_agnostic_skyscape.xml").getAbsolutePath()).importEnvironmentMetadata();
		// Attempt to export the Environment details before an EnvironmentContainerDefinition has been deployed
		try {
			new EnvironmentBuildMetadataAgnosticExport().exportEnvironment("HO_IPT_NP_PRP1_MABC", "1.0", XMLProviderType.SKYSCAPE.toString());
			Assert.fail("Expected a RuntimeException due to no successfully deployed Environment Container Definition");
		} catch (RuntimeException e) {
			Assert.assertTrue(e.getMessage().contains("No successfully deployed Environment Container Definition"));
		}
	}
	
	@Test
	public void testExtractEnvironmentBeforeEnvironmentContainerDefinitionDeployedAws() throws Exception {
		// Import some data into the database first so it can then be exported...
		// Import the Environment Definition
		new EnvironmentBuildMetadataImport(new File("src/test/resources/HO_IPT_NP_PRP1_agnostic_aws.xml").getAbsolutePath()).importEnvironmentMetadata();
		// Attempt to export the Environment details before an EnvironmentContainerDefinition has been deployed
		try {
			new EnvironmentBuildMetadataAgnosticExport().exportEnvironment("HO_IPT_NP_PRP1_MABC", "1.0", XMLProviderType.AWS.toString());
			Assert.fail("Expected a RuntimeException due to no successfully deployed Environment Container Definition");
		} catch (RuntimeException e) {
			Assert.assertTrue(e.getMessage().contains("No successfully deployed Environment Container Definition"));
		}
	}
	
	@Test
	public void testExtractEnvironmentAws() throws Exception {
		// Import some data into the database first so it can then be exported...
		// Import the Environment Definition
		new EnvironmentBuildMetadataImport(new File("src/test/resources/HO_IPT_NP_PRP1_agnostic_aws.xml").getAbsolutePath()).importEnvironmentMetadata();
		// Import an Environment Container Definition
		new EnvironmentBuildMetadataImport(new File("src/test/resources/HO_IPT_NP_agnostic_aws.xml").getAbsolutePath()).importEnvironmentMetadata();
		// Load the imported Environment Container Definition
		EnvironmentContainerDefinition ecd = new ReadManager().getEnvironmentContainerDefinition("np", "1.0", XMLProviderType.AWS.toString());
		Assert.assertNotNull(ecd);
		// Pretend it's been successfully deployed
		recordSuccessfulDeployment(ecd);
		// Export the Environment Definition
		new EnvironmentBuildMetadataAgnosticExport().exportEnvironment("HO_IPT_NP_PRP1_MABC", "1.0", XMLProviderType.AWS.toString());
		// Verify the export
		Assert.assertTrue("Export file created " + EXPORT_ENVIRONMENT_FILE, EXPORT_ENVIRONMENT_FILE.exists());
		assertXMLValid(EXPORT_ENVIRONMENT_FILE, SCHEMA_NAME);
		File expected = new File("src/test/resources/expected/HO_IPT_NP_PRP1_agnostic_export_aws.xml");
		assertXMLEqual(expected, EXPORT_ENVIRONMENT_FILE);
	}

	@Test
	public void testExtractEnvironmentContainerNoData() throws Exception {
		String environmentContainerName = "not found";
		String version = "1.0";
		try {
			new EnvironmentBuildMetadataAgnosticExport().extractEnvironmentContainer(environmentContainerName, version, XMLProviderType.SKYSCAPE.toString());
		} catch (RuntimeException e) {
			Assert.assertEquals("No Environment Container Definition found for environment container name: " + environmentContainerName + ", environment container definition version: " + version + ", provider: SKYSCAPE", e.getMessage());
		}
	}
	
	@Test
	public void testExtractEnvironmentContainerSkyscape() throws Exception {
		// Import some data into the database first so it can then be exported...
		// Import the Environment Container Definition (Org networks)
		new EnvironmentBuildMetadataImport(new File("src/test/resources/HO_IPT_NP_agnostic_skyscape.xml").getAbsolutePath()).importEnvironmentMetadata();
		// Export the Environment Container 
		new EnvironmentBuildMetadataAgnosticExport().exportEnvironmentContainer("np", "1.0", XMLProviderType.SKYSCAPE.toString());
		// Verify the export
		Assert.assertTrue("Export file created " + EXPORT_ENVIRONMENT_CONTAINER_FILE, EXPORT_ENVIRONMENT_CONTAINER_FILE.exists());
		assertXMLValid(EXPORT_ENVIRONMENT_CONTAINER_FILE, SCHEMA_NAME);
		File expected = new File("src/test/resources/expected/HO_IPT_NP_agnostic_export_skyscape.xml");
		assertXMLEqual(expected, EXPORT_ENVIRONMENT_CONTAINER_FILE);
	}
	
	@Test
	public void testExtractEnvironmentContainerAws() throws Exception {
		// Import some data into the database first so it can then be exported...
		// Import the Environment Container Definition (Org networks)
		new EnvironmentBuildMetadataImport(new File("src/test/resources/HO_IPT_NP_agnostic_aws.xml").getAbsolutePath()).importEnvironmentMetadata();
		// Export the Environment Container 
		new EnvironmentBuildMetadataAgnosticExport().exportEnvironmentContainer("np", "1.0", XMLProviderType.AWS.toString());
		// Verify the export
		Assert.assertTrue("Export file created " + EXPORT_ENVIRONMENT_CONTAINER_FILE, EXPORT_ENVIRONMENT_CONTAINER_FILE.exists());
		assertXMLValid(EXPORT_ENVIRONMENT_CONTAINER_FILE, SCHEMA_NAME);
		File expected = new File("src/test/resources/expected/HO_IPT_NP_agnostic_export_aws.xml");
		assertXMLEqual(expected, EXPORT_ENVIRONMENT_CONTAINER_FILE);
	}
}
