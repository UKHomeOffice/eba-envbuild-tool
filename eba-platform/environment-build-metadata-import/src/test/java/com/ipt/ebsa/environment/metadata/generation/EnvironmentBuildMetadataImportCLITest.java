package com.ipt.ebsa.environment.metadata.generation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ipt.ebsa.environment.metadata.generation.EnvironmentBuildMetadataImportCLI.ExitHandler;

/**
 * JUnit tests for EnvironmentBuildMetadataImportCLI
 * @author Mark Kendall
 *
 */
public class EnvironmentBuildMetadataImportCLITest extends DBTest {

	private static Integer exitCode = null;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		// Override exit handler so we can inspect the return code
		ExitHandler exitHandler = new EnvironmentBuildMetadataImportCLI.ExitHandler() {
			@Override
			public void exit(int code) {
				exitCode = code;
			}
		};
		EnvironmentBuildMetadataImportCLI.setExitHandler(exitHandler);
	}
	
	@Test
	public void testHelp() throws Exception {
		EnvironmentBuildMetadataImportCLI.main(new String[] { "-help" });
		assertNotNull("Exit code", exitCode);
		assertEquals("Exit code", 0, exitCode.intValue());
	}
	
	@Test
	public void testNoArgs() throws Exception {
		EnvironmentBuildMetadataImportCLI.main(new String[] {});
		assertNotNull("Exit code", exitCode);
		assertEquals("Exit code", 1, exitCode.intValue());
	}
	
	@Test 
	public void testMissingInputFile() throws Exception {
		EnvironmentBuildMetadataImportCLI.main(new String[] { "-command=import" });
		assertNotNull("Exit code", exitCode);
		assertEquals("Exit code", 1, exitCode.intValue());
	}
	
	@Test 
	public void testImportEnvironmentSkyscape() throws Exception {
		EnvironmentBuildMetadataImportCLI.main(new String[] { "-command=import", "inputXMLFile=src/test/resources/HO_IPT_NP_PRP1_agnostic_skyscape.xml" });
		//EnvironmentBuildMetadataImportCLI.main(new String[] { "-command=import", "inputXMLFile=C:/Users/IBM_ADMIN/Documents/IPT EBSA/Backup/EBSAD-20647/aCloudConfiguration/skyscape test data/exportEnvironment - agnostic VCloud Client Test Env 16889.xml" });
		assertNotNull("Exit code", exitCode);
		assertEquals("Exit code", 0, exitCode.intValue());
		exportToCSV();
		verifyCSV(CSV_EXPECTED_ENVIRONMENT_DIR_SKYSCAPE);
	}
	
	@Test 
	public void testImportEnvironmentAws() throws Exception {
		EnvironmentBuildMetadataImportCLI.main(new String[] { "-command=import", "inputXMLFile=src/test/resources/HO_IPT_NP_PRP1_agnostic_aws.xml" });
		assertNotNull("Exit code", exitCode);
		assertEquals("Exit code", 0, exitCode.intValue());
		exportToCSV();
		verifyCSV(CSV_EXPECTED_ENVIRONMENT_DIR_AWS);
	}
	
	@Test 
	public void testImportEnvironmentContainerDefinitionSkyscape() throws Exception {
		//EnvironmentBuildMetadataImportCLI.main(new String[] { "-command=import", "inputXMLFile=C:/Users/IBM_ADMIN/Documents/IPT EBSA/Backup/EBSAD-20086/Test data/EnvDef-AWS.xml" });
		//EnvironmentBuildMetadataImportCLI.main(new String[] { "-command=import", "inputXMLFile=C:/Users/IBM_ADMIN/Documents/IPT EBSA/Backup/EBSAD-20086/Test data/EnvConDef-AWS.xml" });
		//EnvironmentBuildMetadataImportCLI.main(new String[] { "-command=import", "inputXMLFile=C:/ipt/EBSAD-12598/ebsa-platform-components/Function/vcloud/aCloudClient/src/test/resources/aws/awsBridgeTestEnvironment.xml" });
		EnvironmentBuildMetadataImportCLI.main(new String[] { "-command=import", "inputXMLFile=src/test/resources/HO_IPT_NP_agnostic_skyscape.xml" });
		assertNotNull("Exit code", exitCode);
		assertEquals("Exit code", 0, exitCode.intValue());
		exportToCSV();
		verifyCSV(CSV_EXPECTED_ENVIRONMENT_CONTAINER_DEFINITION_DIR_SKYSCAPE);
	}
	
	@Test 
	public void testImportEnvironmentContainerDefinitionAws() throws Exception {
		EnvironmentBuildMetadataImportCLI.main(new String[] { "-command=import", "inputXMLFile=src/test/resources/HO_IPT_NP_agnostic_aws.xml" });
		//EnvironmentBuildMetadataImportCLI.main(new String[] { "-command=import", "inputXMLFile=C:/ipt/ebsa-platform-components/Function/acloud/aCloudClient/src/test/resources/aws/awsBridgeTestEnvironment.xml" });
		assertNotNull("Exit code", exitCode);
		assertEquals("Exit code", 0, exitCode.intValue());
		exportToCSV();
		verifyCSV(CSV_EXPECTED_ENVIRONMENT_CONTAINER_DEFINITION_DIR_AWS);
	}
	
}
