package com.ipt.ebsa.environment.build;

import static com.ipt.ebsa.environment.build.Configuration.ENVIRONMENT_BUILD_PLANS_GIT_URL;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.environment.build.git.EBGitManager;
import com.ipt.ebsa.environment.build.test.BaseTest;
import com.ipt.ebsa.environment.build.test.TestHelper;

public class EBGitManagerTest extends BaseTest {

	/**
	 * Checks that a git repo can be cloned to a specific location, with existing content in that location removed beforehand.
	 */
	@Test
	public void testCheckoutPlans() throws IOException {
		setupPlanRepo();
		// Create an output directory with some stuff already in it
		File outputDir = createPrePopulatedDir();
		
		EBGitManager gm = new EBGitManager();
		gm.getEnvironmentBuildPlans(outputDir);
		
		assertTrue("File should have been checked out and moved here", new File(outputDir, "Sequences.xml").isFile());
		assertTrue("Directory should have been checked out and moved here", new File(outputDir, "subDir").isDirectory());
		assertPreExistingContentDeleted(outputDir);
	}

	/**
	 * Tests checkout of the guest customisation scripts to the work directory.
	 */
	@Test
	public void testCheckoutGuestCustomisationScripts() throws IOException {
		setupCustScriptsRepo();
		// Create an output directory with some stuff already in it
		File outputDir = createPrePopulatedDir();
		EBGitManager gm = new EBGitManager();
		gm.getGuestCustomisationScripts(outputDir, "np");
		
		assertTrue("File should have been checked out to here", new File(outputDir, "customisation.script.sh").isFile());
		assertTrue("Directory should have been checked out to here", new File(outputDir, "subDir").isDirectory());		
		assertTrue("File should have been checked out to here", new File(new File(outputDir, "subDir"), "another.sh").isFile());
		assertPreExistingContentDeleted(outputDir);
	}

	private File createPrePopulatedDir() throws IOException {
		File outputDir = TestHelper.mkTmpDir();
		assertTrue("Unable to create dummy file", new File(outputDir, "non.git.file.1").createNewFile());
		File dummyDir = new File(outputDir, "non.git.dir.1");
		assertTrue("Unable to create dummy directory", dummyDir.mkdir());
		assertTrue("Unable to create dummy file in dummy directory", new File(dummyDir, "non.git.file.2").createNewFile());
		return outputDir;
	}

	private void assertPreExistingContentDeleted(File outputDir) {
		assertFalse("Existing file 1 should have been removed", new File(outputDir, "non.git.file.1").isFile());
		assertFalse("Existing directory should have been removed", new File(outputDir, "non.git.dir.1").isDirectory());
	}

	private void setupPlanRepo() throws IOException {
		String gitURL = TestHelper.setupGitRepo("src/test/resources/management-git-1");
		ConfigurationFactory.getProperties().setProperty(ENVIRONMENT_BUILD_PLANS_GIT_URL, gitURL);
	}

	private void setupCustScriptsRepo() throws IOException {
		String gitURL = TestHelper.setupGitRepo("src/test/resources/customisation.scripts");
		ConfigurationFactory.getProperties().setProperty("np." + Configuration.OrgProperty.CUSTOMISATION_SCRIPTS_GIT_URL.getKeySuffix(), gitURL);
	}
}
