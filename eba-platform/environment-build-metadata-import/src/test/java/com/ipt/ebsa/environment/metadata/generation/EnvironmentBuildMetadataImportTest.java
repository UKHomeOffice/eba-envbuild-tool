package com.ipt.ebsa.environment.metadata.generation;

import java.io.FileNotFoundException;

import org.junit.Test;

/**
 * JUnit tests for EnvironmentBuildMetadataImport
 *
 */
public class EnvironmentBuildMetadataImportTest extends DBTest {
	
	@Test
	public void testImportEnvironmentSkyscape() throws Exception {
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_NP_PRP1_agnostic_skyscape.xml").importEnvironmentMetadata();
		exportToCSV();
		verifyCSV(CSV_EXPECTED_ENVIRONMENT_DIR_SKYSCAPE);
	}
	
	@Test
	public void testImportEnvironmentAws() throws Exception {
		// Import the Environment
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_NP_PRP1_agnostic_aws.xml").importEnvironmentMetadata();
		exportToCSV();
		verifyCSV(CSV_EXPECTED_ENVIRONMENT_DIR_AWS);
	}
	
	@Test
	public void testImportEnvironmentContainerDefinitionSkyscape() throws Exception {
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_NP_agnostic_skyscape.xml").importEnvironmentMetadata();
		exportToCSV();
		verifyCSV(CSV_EXPECTED_ENVIRONMENT_CONTAINER_DEFINITION_DIR_SKYSCAPE);
	}
	
	@Test
	public void testImportEnvironmentContainerDefinitionAws() throws Exception {
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_NP_agnostic_aws.xml").importEnvironmentMetadata();
		exportToCSV();
		verifyCSV(CSV_EXPECTED_ENVIRONMENT_CONTAINER_DEFINITION_DIR_AWS);
	}
	
	@Test(expected=FileNotFoundException.class)
	public void testImportMissingFile() throws Exception {
		new EnvironmentBuildMetadataImport("missingImportFile.xml").importEnvironmentMetadata();
	}

	@Test(expected=RuntimeException.class)
	public void testImportInvalidFile() throws Exception {
		new EnvironmentBuildMetadataImport("src/test/resources/config.properties").importEnvironmentMetadata();
	}
}
