/**
 * 
 */
package com.ipt.ebsa.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * @author James Shepherd
 *
 */
public class GitTest {
	
	private static final Logger	LOG = LogManager.getLogger(GitTest.class);

	private GitManager localRepo;
	private GitManager clonedRepo;
	
	@Before
	public void setUp() throws FileNotFoundException, IOException {
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
	
	@Test
	public void testGitInit() throws NoWorkTreeException, IOException {
		localRepo.gitInit(null);
		Assert.assertNotNull("Local repo", localRepo.getGitMetadataDir());
	}

	
	@Test
	public void testCloneLocalRepo() throws NoWorkTreeException, IOException, GitAPIException {
		localRepo.gitInit(null);
		Assert.assertNotNull("Local repo", localRepo.getGitMetadataDir());
		clonedRepo.gitClone(localRepo.getGitMetadataDir().toURI().toString(), org.eclipse.jgit.util.FileUtils.createTempDir("git-", "-repo", null), "master", false);
		Assert.assertNotNull("Cloned repo", clonedRepo.getGitMetadataDir());
	}
	
	@Test
	public void testPush() throws Exception {
		localRepo.gitInit(null);
		localRepo.commit("init");
		clonedRepo.gitClone(localRepo.getGitMetadataDir().toURI().toString(), org.eclipse.jgit.util.FileUtils.createTempDir("git-", "-repo", null), "master", false);
		
		File testFile = commitRandomFile(clonedRepo, "master");
		LOG.info("Commited clonedRepo file");
		
		LOG.info("Trying to push master on clone to original");
		clonedRepo.push();
		updateRepoWorkingDir(localRepo);
		assertTrue("pushed file in repo", new File(localRepo.getWorkingDir(), testFile.getName()).isFile());
	}
	
	@Test
	public void testPushConflict() throws Exception {
		try {
			localRepo.gitInit(null);
			localRepo.commit("init");
			clonedRepo.gitClone(localRepo.getGitMetadataDir().toURI().toString(), org.eclipse.jgit.util.FileUtils.createTempDir("git-", "-repo", null), "master", false);
			
			commitRandomFile(localRepo, "master");
			LOG.info("Committed originalRepo file");
			
			commitRandomFile(clonedRepo, "master");
			LOG.info("Commited clonedRepo file");
			
			LOG.info("Trying to push master on clone to original, should fail");
			clonedRepo.push();
			fail();
		} catch (RuntimeException e) {
			assertEquals("Filed to push", e.getMessage());
		}
	}
	
	@Test
	public void testPullPush() throws Exception {
		localRepo.gitInit(null);
		localRepo.commit("init");
		clonedRepo.gitClone(localRepo.getGitMetadataDir().toURI().toString(), org.eclipse.jgit.util.FileUtils.createTempDir("git-", "-repo", null), "master", false);
		
		File originalsFile = commitRandomFile(localRepo, "master");
		LOG.info("Committed originalRepo file");
		
		File clonesFile = commitRandomFile(clonedRepo, "master");
		LOG.info("Commited clonedRepo file");
		
		LOG.info("Pulling from original to clone");
		clonedRepo.pull();
		
		File checkOriginalsFile = new File(clonedRepo.getWorkingDir(), originalsFile.getName());
		Assert.assertTrue("Has file from localRepo been pulled to clone repo?", checkOriginalsFile.isFile());
		
		LOG.info("Pushing to master");
		clonedRepo.push();

		// yes strange huh, have to do a reset on the localRepo to
		// get the changes that were pushed to it to appear in the
		// working dir.
		updateRepoWorkingDir(localRepo);
		
		File checkClonesFile = new File(localRepo.getWorkingDir(), clonesFile.getName());
		Assert.assertTrue("Has localRepo got the clonedRepo's file?", checkClonesFile.isFile());
	}

	public static void updateRepoWorkingDir(GitManager repo) throws GitAPIException, CheckoutConflictException {
		repo.getGit().reset().setMode(ResetType.HARD).setRef("HEAD").call();
	}

	private File commitRandomFile(GitManager gitRepo, String branch) throws IOException, NoFilepatternException, GitAPIException {
		gitRepo.checkoutBranch(branch);
		
		File workDir = gitRepo.getWorkingDir();
		
		File readmeFile = File.createTempFile("README-", ".txt", workDir);
		
		String basename = readmeFile.getName();
		
		gitRepo.addFileByPattern(basename);
		
		LOG.debug("Added file to index: " + basename);
		
		gitRepo.commit("Commiting file " + readmeFile.getName());
		
		return readmeFile;
	}
}
