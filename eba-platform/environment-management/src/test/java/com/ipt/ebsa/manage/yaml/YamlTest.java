package com.ipt.ebsa.manage.yaml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.git.GitManager;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.Deployer;
import com.ipt.ebsa.manage.deploy.database.DBTest;
import com.ipt.ebsa.manage.git.EMGitManagerTest;
import com.ipt.ebsa.manage.test.TestHelper;

public class YamlTest extends DBTest {
	
	private GitManager localRepo;
	
	@Before
	public void setUp() {
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER);
	}
	
	@After
	public void tearDown() throws IOException {
		EMGitManagerTest.deleteLocalRepo(localRepo);
	}
	
	@Test
	public void testSimpleRemoveYaml() throws Exception {
		localRepo = TestHelper.setupGitRepo("src/test/resources/yaml-remove");
		ApplicationVersion appVersion = TestHelper.getApplicationVersion(getEntityManager());
		
		Deployer d = new Deployer();
		d.deploy(appVersion, "IPT_ST_CIT1_APP2", null);
		
		File changedFile = new File(localRepo.getWorkingDir(), "st/st-cit1-app2/dbs.yaml");
		String before = FileUtils.readFileToString(changedFile);
		assertTrue(before.contains(String.format("system::services:%n    my-service:%n        ensure: running%n    my-other-service:%n        ensure: running")));
		EMGitManagerTest.updateRepoWorkingDir(localRepo);
		String after = FileUtils.readFileToString(changedFile);
		assertFalse(after.contains("my-service"));
		assertFalse(after.contains("my-other-service"));
		assertFalse(after.contains("system::services"));
	}
}
