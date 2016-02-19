/**
 * 
 */
package com.ipt.ebsa.manage.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.buildtools.release.entities.Application;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.git.GitManager;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.ApplicationDeployment;
import com.ipt.ebsa.manage.test.TestHelper;


/**
 * @author James Shepherd
 *
 */
public class EMGitManagerTest {
	
	private static final Logger	LOG = LogManager.getLogger(EMGitManagerTest.class);

	private GitManager localRepo;
	private GitManager clonedRepo;
	
	@Before
	public void setUp() throws FileNotFoundException, IOException {
		TestHelper.setupTestConfig();
		localRepo = new GitManager();
		clonedRepo = new GitManager();
	}
	
	@After
	public void tearDown() throws IOException {
		deleteLocalRepo(localRepo);
		deleteLocalRepo(clonedRepo);
	}
	
	public static void deleteLocalRepo(GitManager localRepo) {
		if (null != localRepo && null != localRepo.getWorkingDir()) {
			localRepo.close();
			try {
				FileUtils.deleteDirectory(localRepo.getWorkingDir());
			} catch (NoWorkTreeException e) {
				LOG.warn("Failed to detete test git working dir", e);
			} catch (IOException e) {
				LOG.warn("Failed to detete test git working dir", e);
			}
		}
	}
	
	/**
	 * This test creates a conflict in git that git cannot resolve - it is a creation of the same file in both branches.
	 * @throws Exception
	 */
	@Test
	public void testMergingInHieraConflict() throws Exception {
		localRepo = createGitRepo(new File("src/test/resources/git-hiera/st"));
		String repoURL = "file://" + localRepo.getGitMetadataDir();
		LOG.info("URL: " + repoURL);
		File checkoutDir = org.eclipse.jgit.util.FileUtils.createTempDir("git-", "-repo", null);
		LOG.info("Checkout dir: " + checkoutDir);
		
		LOG.info("Setting up config");
		ConfigurationFactory.getProperties().put(Configuration.GIT_REMOTE_CHECKOUT_REPO_URL, repoURL);
		ConfigurationFactory.getProperties().put(Configuration.GIT_LOCAL_CHECKOUT_DIR, checkoutDir.getAbsolutePath());
		ConfigurationFactory.getProperties().put("st.hiera.ext.repo.enabled", "false");
		
		ApplicationVersion av = new ApplicationVersion();
		av.setApplication(new Application());
		av.setRelatedJiraIssue("EBSA-11007");
		ApplicationDeployment deployment = new ApplicationDeployment(av);
		
		LOG.info("Set up GITManager");
		EMGitManager git = new EMGitManager(deployment.getId(), deployment.getApplicationVersion().getRelatedJiraIssue(), deployment.getOrganisation());
		git.checkoutHiera();
		clonedRepo = git.getGitManager();
		
		LOG.info("Making a conflicting change");
		String testFile = "st/st-cit1-app2/azf.yaml";
		FileUtils.write(new File(localRepo.getWorkingDir(), testFile), "One-Eyed-Willy", false);
		localRepo.addAllFiles();
		localRepo.commit("Chunk");
		
		LOG.info("Editing workspace");
		String quote = "Truffle shuffle.";
		FileUtils.write(new File(git.getCheckoutDir(), testFile), quote, false);
		
		LOG.info("Attempting push");
		try {
			git.commitBranchMergeToMaster("test");
			fail();
		} catch (RuntimeException e) {
			assertEquals("merge should fail", "Failed to push repo", e.getMessage());
		}
	}
	
	/**
	 * This tests that changes in both branches are kept when those changes are non-conflicting.
	 * @throws Exception
	 */
	@Test
	public void testMergingInNonConflict() throws Exception {
		localRepo = createGitRepo(new File("src/test/resources/git-hiera/st"));
		String repoURL = "file://" + localRepo.getGitMetadataDir();
		LOG.info("URL: " + repoURL);
		File checkoutDir = org.eclipse.jgit.util.FileUtils.createTempDir("git-", "-repo", null);
		LOG.info("Checkout dir: " + checkoutDir);
		
		LOG.info("Setting up config");
		ConfigurationFactory.getProperties().put(Configuration.GIT_REMOTE_CHECKOUT_REPO_URL, repoURL);
		ConfigurationFactory.getProperties().put(Configuration.GIT_LOCAL_CHECKOUT_DIR, checkoutDir.getAbsolutePath());
		ConfigurationFactory.getProperties().put("st.hiera.ext.repo.enabled", "false");
		
		ApplicationVersion av = new ApplicationVersion();
		av.setApplication(new Application());
		av.setRelatedJiraIssue("EBSA-11007");
		ApplicationDeployment deployment = new ApplicationDeployment(av);
		
		LOG.info("Set up GITManager");
		EMGitManager git = new EMGitManager(deployment.getId(), deployment.getApplicationVersion().getRelatedJiraIssue(), deployment.getOrganisation());
		git.checkoutHiera();
		clonedRepo = git.getGitManager();
		
		LOG.info("Making a non-conflicting change");
		String testFile1 = "st/st-cit1-app2/jas.yaml";
		String contents1 = "One-Eyed-Willy";
		File localTestFile1 = new File(localRepo.getWorkingDir(), testFile1);
		FileUtils.write(localTestFile1, contents1, false);
		localRepo.addAllFiles();
		localRepo.commit("Chunk");
		
		LOG.info("Editing workspace");
		String testFile2 = "st/st-cit1-app2/maj.yaml";
		String contents2 = "Truffle shuffle.";
		FileUtils.write(new File(git.getCheckoutDir(), testFile2), contents2, false);
		git.commitBranchMergeToMaster("test");

		LOG.info("Checking that master branch has been updated");
		updateRepoWorkingDir(localRepo);
		
		String foundcontents1 = FileUtils.readFileToString(localTestFile1);
		assertEquals("Checking has remained change on master", contents1, foundcontents1);
		
		String foundcontents2 = FileUtils.readFileToString(new File(localRepo.getWorkingDir(), testFile2));
		assertEquals("Checking has remained change on branch", contents2, foundcontents2);
	}

	/**
	 * This is a happy-path test that the merge works and the branch stays created on the server.
	 * @throws Exception
	 */
	@Test
	public void testMerging() throws Exception {
		localRepo = createGitRepo(new File("src/test/resources/git-hiera/st"));
		String repoURL = "file://" + localRepo.getGitMetadataDir();
		LOG.info("URL: " + repoURL);
		File checkoutDir = org.eclipse.jgit.util.FileUtils.createTempDir("git-", "-repo", null);
		LOG.info("Checkout dir: " + checkoutDir);
		
		LOG.info("Setting up config");
		ConfigurationFactory.getProperties().put(Configuration.GIT_REMOTE_CHECKOUT_REPO_URL, repoURL);
		ConfigurationFactory.getProperties().put(Configuration.GIT_LOCAL_CHECKOUT_DIR, checkoutDir.getAbsolutePath());
		
		ApplicationVersion av = new ApplicationVersion();
		av.setApplication(new Application());
		av.setRelatedJiraIssue("EBSA-11007");
		ApplicationDeployment deployment = new ApplicationDeployment(av);
		
		LOG.info("Set up GITManager");
		EMGitManager git = new EMGitManager(deployment.getId(), deployment.getApplicationVersion().getRelatedJiraIssue(), deployment.getOrganisation());
		git.checkoutHiera();
		clonedRepo = git.getGitManager();
		
		LOG.info("Editing workspace");
		String testFile = "st/st-cit1-app2/azf.yaml";
		String quote = "Truffle shuffle.";
		FileUtils.write(new File(git.getCheckoutDir(), testFile), quote, false);
		
		LOG.info("Attempting push");
		git.commitBranchMergeToMaster("test");
		
		LOG.info("Checking that master branch has been updated");
		updateRepoWorkingDir(localRepo);
		File localTestFile = new File(localRepo.getWorkingDir(), testFile);
		String contents = FileUtils.readFileToString(localTestFile);
		
		assertEquals("Checking has pushed change", quote, contents);
		
		LOG.info("Checking that branch exists on remote");
		List<Ref> branchList = localRepo.getGit().branchList().call();
		boolean found = false;
		for (Ref ref : branchList) {
			if (ref.getName().equals(git.getBranchName())) {
				found = true;
				break;
			}
		}
		
		assertTrue("Branch has been pushed", found);
	}
	
	public static GitManager createGitRepo(File sourceDir) throws IOException, NoFilepatternException, GitAPIException {
		GitManager git = new GitManager();
		git.gitInit(null);
		LOG.info("Create git repo at " + git.getWorkingDir());
		
		File dest = new File(git.getWorkingDir(), sourceDir.getName());
		LOG.info("copying test hiera files to workdir " + dest.getAbsolutePath());
		FileUtils.copyDirectory(sourceDir, dest );
		
		LOG.info("add files");
		git.addFileByPattern(".");
		
		LOG.info("commit files");
		git.commit("init");
		
		return git;
	}
	
	public static void updateRepoWorkingDir(GitManager repo) throws GitAPIException, CheckoutConflictException {
		repo.getGit().reset().setMode(ResetType.HARD).setRef("HEAD").call();
	}
}
