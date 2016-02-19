package com.ipt.ebsa.manage.git;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.git.GitManager;
import com.ipt.ebsa.manage.Configuration;

public class EMGitManager {

	private static final String COMMIT_PREFIX			= "EBSAD-00";
	private static final Logger	LOG						= LogManager.getLogger(EMGitManager.class);
	private String				branchName				= null;
	private File				checkoutDir				= null;
	private GitManager			gitManager;
	private String				jobId					= null;
	private String				relatedJiraIssue		= null;
	private Organisation		organisation			= null;

	public EMGitManager(String jobId, String relatedJiraIssue, Organisation organisation) {
		this.jobId = jobId;
		this.relatedJiraIssue = relatedJiraIssue;
		this.organisation = organisation;
		gitManager = getGitManagerInstance();
	}

	public static GitManager getGitManagerInstance() {
		GitManager gitManager = new GitManager();
		gitManager.setPassword(Configuration.getGITPassword());
		gitManager.setUsername(Configuration.getGITUsername());
		gitManager.setInteractivePasswordEnabled(Configuration.isAllowInteractivePasswordProvision());
		return gitManager;
	}
	
	public void checkoutHiera() throws Exception {
		checkoutDir = new File(Configuration.getLocalCheckoutDir(), "hiera-ext");
		
		LOG.debug("Checkout dir: " + checkoutDir);
		FileUtils.deleteDirectory(checkoutDir);
		
		String remoteRepoUrl = Configuration.getRemoteCheckoutRepoUrl(organisation);
		
		LOG.debug(String.format("Cloning repository from remote [%s] to [%s]", remoteRepoUrl, checkoutDir));
		gitManager.gitClone(remoteRepoUrl, checkoutDir, GitManager.MASTER_STARTING_POINT, false);
		
		branchName = gitManager.checkoutNewBranch(this.jobId, null);
		gitManager.commit(String.format("%s #Environment tool committing for JIRA ID %s to branch %s", COMMIT_PREFIX, relatedJiraIssue, getBranchName()));
		gitManager.push();
	}
	
	/**
	 * This adds, commits changes to the branch. Then it pulls master and merges in the
	 * branch, pushes master. This will now retry the pull, merge and push on failure.
	 * @param baseCommitMessage
	 * @throws IOException
	 * @throws Exception
	 */
	public void commitBranchMergeToMaster(String baseCommitMessage) {
		LOG.debug("Adding all files to branch " + getBranchName());
		gitManager.addAllFiles();
		
		String commitMessage = String.format("%s #Committing changed files for JIRA ID %s into branch %s for %s", COMMIT_PREFIX, relatedJiraIssue, getBranchName(), baseCommitMessage);
		LOG.info("Committing to branch with msg: " + commitMessage);
		gitManager.commit(commitMessage);
		
		LOG.debug("Pushing to branch: " + getBranchName());
		gitManager.push();
		
		String mergeMessage = String.format("%s #Merging changes for JIRA ID %s from branch '%s' into '%s' with message: '%s'", 
				COMMIT_PREFIX, relatedJiraIssue, getBranchName(), GitManager.MASTER_STARTING_POINT, baseCommitMessage);
		
		final int maxAttemptCount = Configuration.getGitPushRetryCount();
		for (int attempt = 1; attempt <= maxAttemptCount; attempt++) {
			try {
				LOG.info("Attempt: " + attempt);
				gitManager.checkoutBranch(GitManager.MASTER_STARTING_POINT);
				gitManager.pull();
				
				LOG.info(String.format("Beginning merge of '%s' into '%s' with msg '%s'", getBranchName(), GitManager.MASTER_STARTING_POINT, mergeMessage));
				boolean mergeSuccess = gitManager.merge(getBranchName());
				
				String mergeSuccessMessage = String.format("Merging was success was %s for %s", mergeSuccess, baseCommitMessage);
				LOG.debug(mergeSuccessMessage);
				if(!mergeSuccess) {
					throw new RuntimeException(String.format("The merge failed, fatal for %s", baseCommitMessage));
				}
				
				gitManager.push();
				
				String mergeCompleteMessage = String.format("Completed merge from %s into %s for %s", getBranchName(), GitManager.MASTER_STARTING_POINT, baseCommitMessage);
				LOG.info(mergeCompleteMessage);
				break;
			} catch (Exception e) {
				LOG.warn("Push to repo failed", e);
				if (attempt >= maxAttemptCount) {
					throw new RuntimeException("Failed to push repo", e);
				}
			}
		}
		
		gitManager.checkoutBranch(getBranchName());
	}
	
	/**
	 * Close the encapsulated git resources.
	 */
	public void close() {
		if (null != gitManager) {
			gitManager.close();
		}
	}
	
	public String getBranchName() {
		return branchName;
	}

	public File getCheckoutDir() {
		return checkoutDir;
	}

	public GitManager getGitManager() {
		return gitManager;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public static void checkoutRemoteCheckoutEnvironmentConfigurationFiles() {
		checkout(Configuration.getLocalEnvironmentConfigurationCheckoutDir(),
				Configuration.getRemoteCheckoutEnvironmentConfigurationRepoUrl(),
				GitManager.MASTER_STARTING_POINT);
	}

	public static String checkoutRemoteEnvironmentDeploymentDescriptorsFiles() {
		return checkout(Configuration.getLocalEnvironmentDeploymentDescriptorsCheckoutDir(),
				Configuration.getRemoteCheckoutEnvironmentDeploymentDescriptorsRepoUrl(),
				"ci");
	}
	
	public static String checkoutRemoteCompositeDeploymentDescriptorsFiles() {
		return checkout(Configuration.getLocalCompositeDeploymentDescriptorsCheckoutDir(),
				Configuration.getRemoteCheckoutCompositeDeploymentDescriptorsRepoUrl(),
				"ci");
	}

	public static void checkoutRemoteHieraFiles() {
		checkout(Configuration.getLocalHieraCheckoutDir(),
				Configuration.getRemoteCheckoutHieraRepoUrl(),
				GitManager.MASTER_STARTING_POINT);
	}
	
	/**
	 * Performs the checkout and returns the hash of the HEAD revision.
	 */
	private static String checkout(String localDir, String gitURL, String branch) {
		try {
			FileUtils.deleteDirectory(new File(localDir));
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to checkout branch [%s] from [%s] to [%s]", branch, gitURL, localDir), e);
		}
		GitManager gitMgr = new GitManager();
		gitMgr.gitClone(gitURL, localDir, branch, false);
		String hashForHead = gitMgr.getHashForHead();
		gitMgr.close();
		return hashForHead;
	}

}
