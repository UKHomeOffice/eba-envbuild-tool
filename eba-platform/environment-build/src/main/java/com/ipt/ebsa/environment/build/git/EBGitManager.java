package com.ipt.ebsa.environment.build.git;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.build.Configuration;
import com.ipt.ebsa.git.GitManager;

/**
 * Wraps a regular GitManager to provide Environment-Build specific Git functionality.
 * 
 * @author James Shepherd
 */
public class EBGitManager {
	
	private static final Logger LOG = Logger.getLogger(EBGitManager.class);
	
	/**
	 * Checks out whole build plan repo then moves just the plans sub-directory to the desired location.
	 * @param destDir
	 */
	public void getEnvironmentBuildPlans(File destDir) {
		clearDestination(destDir);
		File tmpDir = new File(destDir, "XXtmpXX");
		if (!tmpDir.mkdirs()) {
			throw new RuntimeException("Failed to create temp dir for checking out build plans: " + tmpDir.getAbsolutePath());
		}
		
		String environmentBuildPlansDir = Configuration.getEnvironmentBuildPlansDir();
		File planDir = new File(tmpDir, environmentBuildPlansDir == null ? "" : environmentBuildPlansDir);
		try {
			GitManager gm = buildGitManager();
			gm.gitClone(Configuration.getEnvironmentBuildPlansGitURL(), tmpDir, GitManager.MASTER_STARTING_POINT, false);
			gm.close();
		
			if (!planDir.isDirectory()) {
				throw new RuntimeException("Failed to find plan dir in checkout: " + planDir.getAbsolutePath());
			}
		
			FileUtils.copyDirectory(planDir, destDir);
		} catch (IOException e) {
			throw new RuntimeException("Failed to move plan dir: " + planDir.getAbsolutePath(), e);
		} finally {
			try {
				FileUtils.deleteDirectory(tmpDir);
			} catch (IOException e) {
				// Well, can't say we didn't try
			}
		}
	}
	
	/**
	 * Checks out whole build plan repo then moves just the plans sub-directory to the desired location.
	 * @param destDir
	 */
	public void getEnvironmentStartupShutdown(File destDir, String domain) {
		File susdRoot = new File(destDir,domain);
		clearDestination(susdRoot);
		File tmpDir = new File(susdRoot, "XXTMP"+domain+"TMPXX");
		if (!tmpDir.mkdirs()) {
			throw new RuntimeException("Failed to create temp dir for checking out startup/shutdown files: " + tmpDir.getAbsolutePath());
		}
		
		//String environmentBuildPlansDir = Configuration.getEnvironmentStartupShutdownDir();
		//File planDir = new File(tmpDir, environmentBuildPlansDir == null ? "" : environmentBuildPlansDir);
		try {
			GitManager gm = buildGitManager();
			//ssh://git@root/project/domain.git
			StringBuilder repoUrl = new StringBuilder(Configuration.getEnvironmentStartupShutdownGitRoot());
			if(!repoUrl.toString().endsWith(".git/")) {
				if(!repoUrl.toString().endsWith(".git")) {
					if(!repoUrl.toString().endsWith("/")) {
						repoUrl.append("/");
					}
					repoUrl.append(domain);
					repoUrl.append(".git");
				}
			}
			
			gm.gitClone(repoUrl.toString(), tmpDir, GitManager.MASTER_STARTING_POINT, false);
			gm.close();
		
			if (!tmpDir.isDirectory() || tmpDir.list().length == 0) {
				throw new RuntimeException("Failed to find startup/shutdown dir in checkout: " + tmpDir.getAbsolutePath());
			}
		
			FileUtils.copyDirectory(tmpDir, susdRoot);
		} catch (IOException e) {
			throw new RuntimeException("Failed to move startup/shutdown dir: " + tmpDir.getAbsolutePath(), e);
		} finally {
			try {
				FileUtils.deleteDirectory(tmpDir);
			} catch (IOException e) {
				// Well, can't say we didn't try
			}
		}
	}

	private void clearDestination(File destDir) {
		if (destDir.exists()) {
			LOG.warn(String.format("Destination checkout directory [%s] already exists. Attempting to delete before cloning", destDir));
			try {
				FileUtils.cleanDirectory(destDir);
			} catch (IOException e1) {
				LOG.error("Unable to clear out existing checkout directory");
			}
		}
	}

	public static GitManager buildGitManager() {
		GitManager gm = new GitManager();
		if (Configuration.getSshIdentity() != null) {
			gm.setIdentityFile(Configuration.getSshIdentity().getAbsolutePath());
		}
		if (Configuration.getSshKnownhosts() != null) {
			gm.setKnownHosts(Configuration.getSshKnownhosts().getAbsolutePath());
		}
		return gm;
	}

	/**
	 * Checks out the guest customisation scripts repo to the desired location.
	 */
	public void getGuestCustomisationScripts(File destDir, String org) {
		clearDestination(destDir);
		LOG.info("Checking out guest customisation scripts to [" + destDir.getAbsolutePath() + "]");
		GitManager gm = buildGitManager();
		gm.gitClone(Configuration.getCustomisationScriptsGitUrl(org), destDir, GitManager.MASTER_STARTING_POINT, false);
		gm.close();
	}
}
