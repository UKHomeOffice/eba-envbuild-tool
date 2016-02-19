/**
 * 
 */
package com.ipt.ebsa.manage.deploy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.git.GitManager;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.database.DBTest;
import com.ipt.ebsa.manage.git.EMGitManagerTest;
import com.ipt.ebsa.manage.test.TestHelper;

/**
 * @author James Shepherd
 *
 */
public class DeployTest extends DBTest {
	
	private static Logger log = LogManager.getLogger(DeployTest.class);

	private GitManager localRepo;
	
	@Before
	public void setUp() {
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER);
	}
	
	@After
	public void tearDown() throws IOException {
		EMGitManagerTest.deleteLocalRepo(localRepo);
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_CONFIG_CREATE_HIERA);
	}
	
	@Test
	public void testSingleHieraRepoUpdate() throws Exception {
		String baseFolder = "src/test/resources/deploy-test/hiera";
		String changingFile = "st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml";
		String newVersion = "1.0.135-1";
		deploy(baseFolder, changingFile, newVersion);
		
		//TODO: Check the report 
	}
	
	@Test
	public void testSingleHieraRepoUpdateNoYaml() throws Exception {
		String baseFolder = "src/test/resources/deploy-test-no-yaml/hiera";
		String changingFile = "st/st-cit1-app2/ext/soatzm01.st-cit1-app2.ipt.local.yaml";
		String newVersion = "1.0.135-1";
		deploy(baseFolder, changingFile, newVersion);
		
		//TODO: Check the report 
	}
	
	@Test
	public void testExtHieraRepoUpdate() throws Exception {
		String baseFolder = "src/test/resources/deploy-test/hiera-ext";
		String changingFile = "st/st-cit1-app2/ext/soatzm01.st-cit1-app2.ipt.local.yaml";
		String newVersion = "1.0.135-1";
		deploy(baseFolder, changingFile, newVersion);
		
		//TODO: Check the report 
	}
	
	@Test
	public void testInsertYamlBlock() throws Exception {
		String baseFolder = "src/test/resources/deploy-test/EBSAD-14629-insert-yaml-block";
		String changingFile = "st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml";
		String newVersion = "1.0.135-1";
		String after = deploy(baseFolder, changingFile, newVersion);
		
		log.info("-----YAML-----");
		log.info(after);
		log.info("--------------");
		
		assertTrue(after.contains("change: xyz"));
		assertTrue(!after.contains("change: 123"));
		assertTrue(after.contains("shouldRemain: true"));
		assertTrue(after.contains("- someSubChange"));
		assertTrue(after.contains("whichEquals: 42"));
		assertTrue(after.contains("/usr/local/pgsql"));
		
		//TODO: Check the report 
	}
	
	@Test
	public void testInsertYamlTypes() throws Exception {
		String baseFolder = "src/test/resources/deploy-test/EBSAD-15239-test-yaml-types-single-line";
		String changingFile = "st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml";
		String newVersion = "1.0.135-1";
		String after = deploy(baseFolder, changingFile, newVersion);
		
		log.info("-----YAML-----");
		log.info(after);
		log.info("--------------");
		
		assertTrue(after.contains("someText: xyz"));
		//assertTrue(after.contains("someTextWithQuote: xyz"));
		assertTrue(after.contains("aBoolean: false"));
		assertTrue(after.contains("aBooleanAsText: 'false'"));
		assertTrue(after.contains("aNumber: 5"));
		assertTrue(after.contains("aNumberAsText: '5'"));
		assertTrue(after.contains("aNumberAsTextAndQuotes: '''5'''"));
		
		//TODO: Check the report 
	}

	private String deploy(String baseFolder, String changingFile, String newVersion) throws IOException, FileNotFoundException, NoFilepatternException, GitAPIException, Exception,
			CheckoutConflictException {
		
		localRepo = TestHelper.setupGitRepo(baseFolder);
		
		ApplicationVersion appVersion = TestHelper.getApplicationVersion(getEntityManager());
		
		Deployer d = new Deployer();
		d.deploy(appVersion, "IPT_ST_CIT1_APP2", null);
		
		File changedFile = new File(localRepo.getWorkingDir(), changingFile);
		String before = "";
		try {
				before = FileUtils.readFileToString(changedFile);
		} catch (FileNotFoundException fnfe){
			//Squelch. Perfectly possible for the file not to have existed at the start.
		}

		assertFalse(before.contains(newVersion));
		EMGitManagerTest.updateRepoWorkingDir(localRepo);
		String after = FileUtils.readFileToString(changedFile);
		assertTrue(after.contains(newVersion));
		return after;
	}
}
