package com.ipt.ebsa.environment.build;

import static com.ipt.ebsa.environment.build.Configuration.ENVIRONMENT_BUILD_PLANS_GIT_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.util.UriEncoder;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerBuild;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.environment.build.manager.UpdateManager;
import com.ipt.ebsa.environment.build.test.BaseTest;
import com.ipt.ebsa.environment.build.test.TestHelper;
import com.ipt.ebsa.environment.metadata.generation.EnvironmentBuildMetadataImport;

public class CliTest extends BaseTest {
	
	private TestExitHandler exitHandler;
	private File reportOutputFile;
	private File envDefnXmlFileSkyscape;
	private File envDefnXmlFileAws;
	private File combinedBuildPlanXmlPath;
	
	@Before
	public void setup() {
		exitHandler = new TestExitHandler();
		EnvironmentBuildCLI.setExitHandler(exitHandler);
		
		reportOutputFile = new File("target/tests/report.html");
		envDefnXmlFileSkyscape = new File("target/test/envDefn_skyscape.xml");
		envDefnXmlFileSkyscape.delete();
		envDefnXmlFileAws = new File("target/test/envDefn_aws.xml");
		envDefnXmlFileAws.delete();
		combinedBuildPlanXmlPath = new File("target/test/combinedPlan.xml");
		combinedBuildPlanXmlPath.delete();
		reportOutputFile.getParentFile().mkdirs();
		if (reportOutputFile.exists()) {
			reportOutputFile.delete();
		}
	}
	
	private File buildUserParams(String... params) throws IOException {
		if (params.length % 2 != 0) {
			throw new RuntimeException("param list is missing something, not even num of params");
		}
		
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < params.length; i +=2) {
			if (i > 0) {
				sb.append("&");
			}
			sb.append(EnvironmentBuildCLI.USER_PARAMETER_PREFIX)
			.append(params[i]).append("=")
			.append(UriEncoder.encode(params[i+1]));
		}
		
		File output = new File(getWorkDir(), "params.dat");
		FileUtils.write(output, sb.toString());
		
		return output;
	}
	
	@Test
	public void testHelp() {
		EnvironmentBuildCLI.main(new String[] { "-help" });
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 0, exitHandler.getExitCode().intValue());
	}
	
	@Test
	public void testNoArgs() {
		EnvironmentBuildCLI.main(new String[] {});
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 2, exitHandler.getExitCode().intValue());
	}
	
	@Test
	public void testPrepareSkyscape() throws Exception {
		setupSkyscapeTestData(true);
		testPrepareSkyscape(XMLProviderType.SKYSCAPE);
	}
	
	@Test
	public void testPrepareAws() throws Exception {
		setupAwsTestData(true);
		testPrepareAws(XMLProviderType.AWS);
	}
	
	
	private void testPrepareSkyscape(XMLProviderType provider) throws IOException {
		setupBuildPlanRepo("src/test/resources/TestLoadEnvironmentData_resources");
		EnvironmentBuildCLI.main(new String[]{
				"-workdir=" + getWorkDirPath(),
				"-config=src/test/resources/test.properties", 
				"-command=prepare",
				"-environment=HO_IPT_NP_PRP1_MABC",
				"-mode=b1", "-version=1.0", 
				"-provider=" + provider,
				"-reportpath=" + reportOutputFile.getAbsolutePath()});
		
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 0, exitHandler.getExitCode().intValue());
		
		String report = FileUtils.readFileToString(reportOutputFile);
		assertTrue(report.contains("build_3=Freddy"));
	}
	private void testPrepareAws(XMLProviderType provider) throws IOException {
		setupBuildPlanRepo("src/test/resources/TestLoadEnvironmentData_resources");
		
		EnvironmentBuildCLI.main(new String[]{
				"-workdir=" + getWorkDirPath(),
				"-config=src/test/resources/test.properties", 
				"-command=prepare",
				"-environment=HO_IPT_NPA_PRP1_MABC",
				"-mode=b2", "-version=1.0", 
				"-provider=" + provider,
				"-reportpath=" + reportOutputFile.getAbsolutePath()});
		
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 0, exitHandler.getExitCode().intValue());
		
		String report = FileUtils.readFileToString(reportOutputFile);
		assertTrue(report.contains("build_3=Freddy"));
	}
	
	@Test
	public void testPrepareUserParams() throws Exception {
		File userParamsFile = new File("target/tests/userparams.dat");
		FileUtils.write(userParamsFile, "userparam1=test1&userparam2=test2");
		
		setupBuildPlanRepo("src/test/resources/TestLoadEnvironmentData_resources_userparam");
		setupSkyscapeTestData(true);
		EnvironmentBuildCLI.main(new String[]{
				"-workdir=" + getWorkDirPath(),
				"-config=src/test/resources/test.properties",
				"-command=prepare",
				"-environment=HO_IPT_NP_PRP1_MABC",
				"-mode=b1",
				"-version=1.0",
				"-provider=" + XMLProviderType.SKYSCAPE,
				"-reportpath=" + reportOutputFile.getAbsolutePath(),
				"-additionalparamspath=" + userParamsFile.getAbsolutePath(),
				"-envdefnxmlpath=" + envDefnXmlFileSkyscape.getAbsolutePath() });
		
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 0, exitHandler.getExitCode().intValue());
		
		String report = FileUtils.readFileToString(reportOutputFile);
		assertTrue(report.contains("build_3=Freddy"));
		assertTrue(report.contains("Gloria test1 Estefan"));
	}
	
	@Test
	public void testPrepareUserParamsEmpty() throws Exception {
		File userParamsFile = new File("target/tests/userparams.dat");
		FileUtils.write(userParamsFile, "\n");
		
		setupBuildPlanRepo("src/test/resources/TestLoadEnvironmentData_resources");
		setupSkyscapeTestData(true);
		EnvironmentBuildCLI.main(new String[]{
				"-workdir=" + getWorkDirPath(),
				"-config=src/test/resources/test.properties",
				"-command=prepare",
				"-environment=HO_IPT_NP_PRP1_MABC",
				"-mode=b1",
				"-version=1.0",
				"-provider=" + XMLProviderType.SKYSCAPE,
				"-reportpath=" + reportOutputFile.getAbsolutePath(),
				"-additionalparamspath=" + userParamsFile.getAbsolutePath(), 
				"-envdefnxmlpath=" + envDefnXmlFileSkyscape.getAbsolutePath() });
		
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 0, exitHandler.getExitCode().intValue());
		
		String report = FileUtils.readFileToString(reportOutputFile);
		assertTrue(report.contains("build_3=Freddy"));
	}
	
	@Test
	public void testRunSkyscape() throws Exception {
		setupSkyscapeTestData(true);
		testRunSkyscape(XMLProviderType.SKYSCAPE, envDefnXmlFileSkyscape, new File("src/test/resources/HO_IPT_NP_PRP1_SKYSCAPE_DB.xml"));
	}
	
	@Test
	public void testRunAws() throws Exception {
		setupAwsTestData(true);
		testRunAws(XMLProviderType.AWS, envDefnXmlFileAws, new File("src/test/resources/HO_IPT_NP_PRP1_aCloud.xml"));
	}
	
	private void testRunSkyscape(XMLProviderType provider, File outputEnvDefnXmlFile, File expectedEnvDefnXmlFile) throws IOException, GitAPIException, URISyntaxException {
		setupCustomisationScriptsRepo();
		String templatesFirewallGitUrl = TestHelper.setupGitRepo("src/test/resources/templates_firewall");
		String templatesInternalGitUrl = TestHelper.setupGitRepo("src/test/resources/templates_internal");
		String hieraGitUrl = TestHelper.initEmptyRepo();
		String sheetGitUrl = TestHelper.setupGitRepo("src/test/resources/xl");
		
		ConfigurationFactory.getProperties().setProperty(Configuration.FIREWALL_HIERA_TEMPLATES_GIT_URL, templatesFirewallGitUrl);
		ConfigurationFactory.getProperties().setProperty(Configuration.FIREWALL_HIERA_TEMPLATES_DIR, "");
		ConfigurationFactory.getProperties().setProperty(Configuration.INTERNAL_HIERA_TEMPLATES_GIT_URL, templatesInternalGitUrl);
		ConfigurationFactory.getProperties().setProperty(Configuration.INTERNAL_HIERA_TEMPLATES_DIR, "");
		ConfigurationFactory.getProperties().setProperty(Configuration.SSH_ACTION_ENABLED, "false");
		ConfigurationFactory.getProperties().setProperty(Configuration.INFRA_ACTION_ENABLED, "false");
		
		setupBuildPlanRepo("src/test/resources/TestLoadEnvironmentData_resources_hiera");
		
		File paramsFile = buildUserParams(
			"firewallHieraGitUrl", hieraGitUrl,
			"firewallSheetGitUrl", sheetGitUrl,
			"firewallSheetDir", "IPTFirewallRulesNP.xlsx", 
			"internalHieraGitUrl", hieraGitUrl,
			"internalSheetGitUrl", sheetGitUrl,
			"internalSheetDir", "IPTRoutesNP.xls"
		);
		
		EnvironmentBuildCLI.main(new String[]{
			"-additionalparamspath=" + paramsFile.getAbsolutePath(),
			"-workdir=" + getWorkDirPath(),
			"-config=src/test/resources/test.properties",
			"-command=run",
			"-environment=HO_IPT_NP_PRP1_MABC",
			"-mode=b1",
			"-version=1.0",
			"-provider=" + provider,
			"-reportpath=" + reportOutputFile.getAbsolutePath(),
			"-envdefnxmlpath=" + outputEnvDefnXmlFile.getAbsolutePath()
		});
		
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 0, exitHandler.getExitCode().intValue());
		
		assertTrue(FileUtils.readFileToString(reportOutputFile).contains(hieraGitUrl));
		assertEquals(FileUtils.readFileToString(expectedEnvDefnXmlFile).replaceAll("\r\n","\n"), FileUtils.readFileToString(outputEnvDefnXmlFile).replaceAll("\r\n","\n"));
		//assertTrue("Customisation scripts directory created", new File(getWorkDir(), "cust_scripts").isDirectory());
		//assertTrue("Customisation scripts checked out from git", new File(new File(getWorkDir(), "cust_scripts"), "customisation.script.sh").exists());
		
		File workDir = TestHelper.updateRepoWorkingDir(hieraGitUrl);
		boolean found = false;
		for (File file : workDir.listFiles()) {
			if ("np".equals(file.getName())) {
				found = true;
				break;
			}
		}

		assertTrue("looking for checked in np dir", found);
	}
	
	private void testRunAws(XMLProviderType provider, File outputEnvDefnXmlFile, File expectedEnvDefnXmlFile) throws IOException, GitAPIException, URISyntaxException {
		setupCustomisationScriptsRepo();
		String templatesFirewallGitUrl = TestHelper.setupGitRepo("src/test/resources/templates_firewall");
		String templatesInternalGitUrl = TestHelper.setupGitRepo("src/test/resources/templates_internal");
		String hieraGitUrl = TestHelper.initEmptyRepo();
		String sheetGitUrl = TestHelper.setupGitRepo("src/test/resources/xl");
		
		ConfigurationFactory.getProperties().setProperty(Configuration.FIREWALL_HIERA_TEMPLATES_GIT_URL, templatesFirewallGitUrl);
		ConfigurationFactory.getProperties().setProperty(Configuration.FIREWALL_HIERA_TEMPLATES_DIR, "");
		ConfigurationFactory.getProperties().setProperty(Configuration.INTERNAL_HIERA_TEMPLATES_GIT_URL, templatesInternalGitUrl);
		ConfigurationFactory.getProperties().setProperty(Configuration.INTERNAL_HIERA_TEMPLATES_DIR, "");
		ConfigurationFactory.getProperties().setProperty(Configuration.SSH_ACTION_ENABLED, "false");
		ConfigurationFactory.getProperties().setProperty(Configuration.INFRA_ACTION_ENABLED, "false");
		
		setupBuildPlanRepo("src/test/resources/TestLoadEnvironmentData_resources_hiera");
		
		File paramsFile = buildUserParams(
			"firewallHieraGitUrl", hieraGitUrl,
			"firewallSheetGitUrl", sheetGitUrl,
			"firewallSheetDir", "IPTFirewallRulesNP.xlsx", 
			"internalHieraGitUrl", hieraGitUrl,
			"internalSheetGitUrl", sheetGitUrl,
			"internalSheetDir", "IPTRoutesNP.xls"
		);
		
		EnvironmentBuildCLI.main(new String[]{
			"-additionalparamspath=" + paramsFile.getAbsolutePath(),
			"-workdir=" + getWorkDirPath(),
			"-config=src/test/resources/test.properties",
			"-command=run",
			"-environment=HO_IPT_NPA_PRP1_MABC",
			"-mode=b2",
			"-version=1.0",
			"-provider=" + provider,
			"-reportpath=" + reportOutputFile.getAbsolutePath(),
			"-envdefnxmlpath=" + outputEnvDefnXmlFile.getAbsolutePath()
		});
		
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 0, exitHandler.getExitCode().intValue());
		
		assertTrue(FileUtils.readFileToString(reportOutputFile).contains(hieraGitUrl));
		assertEquals(FileUtils.readFileToString(expectedEnvDefnXmlFile).replaceAll("\r\n","\n"), FileUtils.readFileToString(outputEnvDefnXmlFile).replaceAll("\r\n","\n"));
		//assertTrue("Customisation scripts directory created", new File(getWorkDir(), "cust_scripts").isDirectory());
		//assertTrue("Customisation scripts checked out from git", new File(new File(getWorkDir(), "cust_scripts"), "customisation.script.sh").exists());
		
		File workDir = TestHelper.updateRepoWorkingDir(hieraGitUrl);
		boolean found = false;
		for (File file : workDir.listFiles()) {
			if ("np".equals(file.getName())) {
				found = true;
				break;
			}
		}

		assertTrue("looking for checked in np dir", found);
	}
	
	@Test
	public void testRunAndWriteCombinedPlanXMLSkyscape() throws Exception {
		setupSkyscapeTestData(true);
		testRunAndWriteCombinedPlanXMLSkyscape(XMLProviderType.SKYSCAPE, envDefnXmlFileSkyscape, new File("src/test/resources/HO_IPT_NP_PRP1_SKYSCAPE_DB.xml"));
	}
	
	@Test
	public void testRunAndWriteCombinedPlanXMLAws() throws Exception {
		setupAwsTestData(true);
		testRunAndWriteCombinedPlanXMLAws(XMLProviderType.AWS, envDefnXmlFileAws, new File("src/test/resources/HO_IPT_NP_PRP1_aCloud.xml"));
	}
	
	private void testRunAndWriteCombinedPlanXMLSkyscape(XMLProviderType provider, File outputEnvDefnXmlFile, File expectedEnvDefnXmlFile) throws IOException {
		setupCustomisationScriptsRepo();
		ConfigurationFactory.getProperties().setProperty(Configuration.SSH_ACTION_ENABLED, "false");
		ConfigurationFactory.getProperties().setProperty(Configuration.INFRA_ACTION_ENABLED, "false");
		setupBuildPlanRepo("src/test/resources/TestLoadEnvironmentData_resources");
		EnvironmentBuildCLI.main(new String[]{
				"-workdir=" + getWorkDirPath(), 
				"-config=src/test/resources/test.properties", 
				"-command=run",
				"-environment=HO_IPT_NP_PRP1_MABC", 
				"-mode=b1", "-version=1.0",
				"-provider=" + provider,
				"-reportpath=" + reportOutputFile.getAbsolutePath(), 
				"-envdefnxmlpath=" + outputEnvDefnXmlFile.getAbsolutePath(),
				"-combinedbuildplanxmlpath=" + combinedBuildPlanXmlPath.getAbsolutePath()});
		
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 0, exitHandler.getExitCode().intValue());
		
		String report = FileUtils.readFileToString(reportOutputFile);
		assertTrue(report.contains("build_3=Freddy"));
		
		assertEquals(FileUtils.readFileToString(expectedEnvDefnXmlFile).replaceAll("\r\n","\n"), FileUtils.readFileToString(outputEnvDefnXmlFile).replaceAll("\r\n","\n"));
		assertEquals(FileUtils.readFileToString(new File("src/test/resources/HO_IPT_NP_PRP1_combinedPlan.xml")).replaceAll("\r\n","\n"), FileUtils.readFileToString(combinedBuildPlanXmlPath).replaceAll("\r\n","\n"));
	}
	
	private void testRunAndWriteCombinedPlanXMLAws(XMLProviderType provider, File outputEnvDefnXmlFile, File expectedEnvDefnXmlFile) throws IOException {
		setupCustomisationScriptsRepo();
		ConfigurationFactory.getProperties().setProperty(Configuration.SSH_ACTION_ENABLED, "false");
		ConfigurationFactory.getProperties().setProperty(Configuration.INFRA_ACTION_ENABLED, "false");
		setupBuildPlanRepo("src/test/resources/TestLoadEnvironmentData_resources");
		EnvironmentBuildCLI.main(new String[]{
				"-workdir=" + getWorkDirPath(), 
				"-config=src/test/resources/test.properties", 
				"-command=run",
				"-environment=HO_IPT_NPA_PRP1_MABC", 
				"-mode=b2", "-version=1.0",
				"-provider=" + provider,
				"-reportpath=" + reportOutputFile.getAbsolutePath(), 
				"-envdefnxmlpath=" + outputEnvDefnXmlFile.getAbsolutePath(),
				"-combinedbuildplanxmlpath=" + combinedBuildPlanXmlPath.getAbsolutePath()});
		
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 0, exitHandler.getExitCode().intValue());
		
		String report = FileUtils.readFileToString(reportOutputFile);
		assertTrue(report.contains("build_3=Freddy"));
		
		assertEquals(FileUtils.readFileToString(expectedEnvDefnXmlFile).replaceAll("\r\n","\n"), FileUtils.readFileToString(outputEnvDefnXmlFile).replaceAll("\r\n","\n"));
		assertEquals(FileUtils.readFileToString(new File("src/test/resources/HO_IPT_NP_PRP1_combinedPlan.xml")).replaceAll("\r\n","\n"), FileUtils.readFileToString(combinedBuildPlanXmlPath).replaceAll("\r\n","\n"));
	}
	
	
	@Test
	public void testCheckout() throws IOException {
		setupBuildPlanRepo();
		
		String jsonOutputPath = new File(getWorkDirPath(), "envdata.json").getAbsolutePath();
		EnvironmentBuildCLI.main(new String[] {
				"-workdir=" + getWorkDirPath(), 
				"-config=src/test/resources/test.properties", 
				"-command=checkoutplans",
				"-builddatapath=" + jsonOutputPath,
				"-provider=" + XMLProviderType.SKYSCAPE});
		
		assertTrue("File should have been checked out and moved here", new File(new File(getWorkDirPath(), "plans"), "Sequences.xml").isFile());
		assertTrue("JSON output should have been created here", new File(jsonOutputPath).isFile());
		FileUtils.deleteDirectory(getWorkDir());
	}
	
	@Test
	public void testGenerateDefinitionFileSkyscape() throws Exception {
		setupSkyscapeTestData(true);
		EnvironmentBuildCLI.main(new String[] {
				"-config=src/test/resources/test.properties", 
				"-command=gendef", 
				"-environment=HO_IPT_NP_PRP1_MABC", 
				"-version=1.0", 
				"-provider=" + XMLProviderType.SKYSCAPE, 
				"-envdefnxmlpath=" + envDefnXmlFileSkyscape.getAbsolutePath()});
		
		assertEquals(FileUtils.readFileToString(new File("src/test/resources/envDefn_skyscape.xml")).replaceAll("\r\n","\n"), FileUtils.readFileToString(envDefnXmlFileSkyscape).replaceAll("\r\n","\n"));
	}
	
	@Test
	public void testGenerateDefinitionFileAws() throws Exception {
		setupAwsTestData(true);
		EnvironmentBuildCLI.main(new String[] {
				"-config=src/test/resources/test.properties",
				"-command=gendef", 
				"-environment=HO_IPT_NPA_PRP1_MABC",
				"-version=1.0", 
				"-provider=" + XMLProviderType.AWS,
				"-envdefnxmlpath=" + envDefnXmlFileAws.getAbsolutePath()});
		
		assertEquals(FileUtils.readFileToString(new File("src/test/resources/envDefn_aws.xml")).replaceAll("\r\n","\n"), FileUtils.readFileToString(envDefnXmlFileAws).replaceAll("\r\n","\n"));
	}
	
	@Test
	public void testStartupShutdown() throws Exception {
		//File userParamsFile = new File("target/tests/userparams.dat");
		//FileUtils.write(userParamsFile, "\n");
		setupEnvironmentStartupShutdownPlanRepo("src/test/resources/susd-git-1");
		ConfigurationFactory.getProperties().setProperty(Configuration.SSH_ACTION_ENABLED, "false");
		ConfigurationFactory.getProperties().setProperty(Configuration.INFRA_ACTION_ENABLED, "false");
		setupSkyscapeTestData(true);
		EnvironmentBuildCLI.main(new String[]{
				"-workdir=" + getWorkDirPath(),
				"-config=src/test/resources/test.properties",
				"-command=startup",
				"-domain=np_prp1_mabc",
				"-environment=HO_IPT_NP_PRP1_MABC",
				"-vpc=HO_IPT_NP_PRP1_MABC",
				"-reportpath="+reportOutputFile.getAbsolutePath()});
		
		assertNotNull("Exit code", exitHandler.getExitCode());
		assertEquals("Exit code", 0, exitHandler.getExitCode().intValue());

		String report = FileUtils.readFileToString(reportOutputFile);

		assertTrue(report.contains("susd_build"));
		assertTrue(report.contains("container_name=np"));
		assertTrue(report.contains("domain=np-prp1-mabc.ipt.ho.local"));
		assertTrue(report.contains("environment=HO_IPT_NP_PRP1"));
		assertTrue(report.contains("expected_hosts=2"));
		assertTrue(report.contains("jumphosts=test_np_jumphosts"));
		assertTrue(report.contains("mtzo_env=test_np_mtzo_env"));
		assertTrue(report.contains("puppet_master=test_np_puppet_master"));
		assertTrue(report.contains("tooling_domain=test_np_tooling_domain"));
		assertTrue(report.contains("vapp_name=HO_IPT_NP_PRP1_MABC"));
			
				
	}
	
	private void setupBuildPlanRepo() throws IOException {
		setupBuildPlanRepo("src/test/resources/management-git-1");
	}
	
	private void setupEnvironmentStartupShutdownPlanRepo(String repo) throws IOException {
		String gitURL = TestHelper.setupGitRepo(repo);
		ConfigurationFactory.getProperties().setProperty(Configuration.ENVIRONMENT_BUILD_STARTUP_SHUTDOWN_GIT_ROOT, gitURL);
	}
	
	private void setupBuildPlanRepo(String repo) throws IOException {
		String gitURL = TestHelper.setupGitRepo(repo);
		ConfigurationFactory.getProperties().setProperty(ENVIRONMENT_BUILD_PLANS_GIT_URL, gitURL);
	}
	
	private void setupCustomisationScriptsRepo() throws IOException {
		String gitURL = TestHelper.setupGitRepo("src/test/resources/customisation.scripts");
		ConfigurationFactory.getProperties().setProperty("np." + Configuration.OrgProperty.CUSTOMISATION_SCRIPTS_GIT_URL.getKeySuffix(), gitURL);
	}
	
	/**
	 * Writes a successful deployment record to the database for the given Environment Container Definition
	 * @param environmentContainerDefinition
	 * @throws Exception
	 */
	private void recordSuccessfulDeployment(EnvironmentContainerDefinition environmentContainerDefinition) throws Exception {
		EnvironmentContainerBuild envConBuild = new EnvironmentContainerBuild();
		envConBuild.setDateStarted(new Date());
		envConBuild.setDateCompleted(new Date());
		envConBuild.setSucceeded(true);
		envConBuild.setJenkinsBuildId("test");
		envConBuild.setJenkinsBuildNumber(1);
		envConBuild.setJenkinsJobName("test");
		envConBuild.setEnvironmentContainerDefinition(environmentContainerDefinition);
		new UpdateManager().saveEnvironmentContainerBuild(envConBuild);
	}
	
	/**
	 * Import a Skyscape Environment and EnvironmentContainerDefinition.
	 * Record the EnvironmentContainerDefinition as having been successfully deployed if recordEnvironmentContainerDefinitionDeployed == true
	 * @param recordEnvironmentContainerDefinitionDeployed
	 * @throws Exception
	 */
	private void setupSkyscapeTestData(boolean recordEnvironmentContainerDefinitionDeployed) throws Exception {
		// Import the Skyscape Environment Container Definition
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_NP_skyscape.xml").importEnvironmentMetadata();
		// Import the Skyscape Environment Definition
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_NP_PRP1_skyscape.xml").importEnvironmentMetadata();
		// Load the Skyscape Environment Container Definition
		EnvironmentContainerDefinition ecd = new ReadManager().getEnvironmentContainerDefinition("np", "1.0", XMLProviderType.SKYSCAPE.toString());
		assertNotNull(ecd);
		if (recordEnvironmentContainerDefinitionDeployed) {
			// Pretend it's been successfully deployed
			recordSuccessfulDeployment(ecd);
		}
	}
	
	/**
	 * Import a AWS Environment and EnvironmentContainerDefinition.
	 * Record the EnvironmentContainerDefinition as having been successfully deployed if recordEnvironmentContainerDefinitionDeployed == true
	 * @param recordEnvironmentContainerDefinitionDeployed
	 * @throws Exception
	 */
	private void setupAwsTestData(boolean recordEnvironmentContainerDefinitionDeployed) throws Exception {
		// Import the AWS Environment Definition
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_NP_PRP1_aws.xml").importEnvironmentMetadata();
		// Import the AWS Environment Container Definition
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_NP_aws.xml").importEnvironmentMetadata();
		// Load the AWS Environment Container Definition
		EnvironmentContainerDefinition ecd = new ReadManager().getEnvironmentContainerDefinition("npa", "1.0", XMLProviderType.AWS.toString());
		assertNotNull(ecd);
		if (recordEnvironmentContainerDefinitionDeployed) {
			// Pretend it's been successfully deployed
			recordSuccessfulDeployment(ecd);
		}
	}
	
	public class TestExitHandler implements EnvironmentBuildCLI.ExitHandler {

		private Integer code;
		
		@Override
		public void exit(int code) {
			this.code = code; 
		}
		
		public Integer getExitCode() {
			return code;
		}
	}
}
