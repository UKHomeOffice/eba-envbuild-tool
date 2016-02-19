package com.ipt.ebsa.environment.build.git;

import java.io.File;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.ipt.ebsa.git.GitManager;

/**
 * @author James Shepherd
 */
public class GitMultiplexer {
	private static final Logger LOG = Logger.getLogger(GitMultiplexer.class);
	
	private TreeMap<String, GitManager> gitManagers = new TreeMap<>();
	
	/**
	 * Checkout if hasn't been checked out before. Callers MUST use the
	 * returned File location as the working directory.
	 * @param gitRepoUrl
	 * @param branch
	 * @param suggestedWorkingDir
	 * @return the actual working dir that was used
	 */
	public File checkout(String gitRepoUrl, String branch, File suggestedWorkingDir) {
		GitManager git = gitManagers.get(gitRepoUrl);
		
		if (null == git) {
			git = new GitManager();
			git.gitClone(gitRepoUrl, suggestedWorkingDir, branch, false);
			gitManagers.put(gitRepoUrl, git);
		}
		
		return git.getWorkingDir();
	}
	
	public void addCommitPush(String gitRepoUrl, String commitMessage) {
		GitManager git = gitManagers.get(gitRepoUrl);
		
		if (null == git) {
			throw new RuntimeException(String.format("Failed to commit as no checkout [%s]", gitRepoUrl));
		}
		
		git.addAllFiles();
		git.commit(commitMessage);
		git.push();
	}
	
	public void close(String gitRepoUrl) {
		try {
			GitManager git = gitManagers.get(gitRepoUrl);
			
			if (null == git) {
				throw new RuntimeException(String.format("Failed to close as no checkout from [%s]", gitRepoUrl));
			}
			
			git.close();
		} catch (Exception e) {
			LOG.warn(String.format("Failed to close git manager at [%s]", gitRepoUrl), e);
		}
	}
}
