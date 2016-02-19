package com.ipt.ebsa.environment.build.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.git.GitManager;

public class TestHelper {
	/**
	 * @param sourceDir
	 * @return url of repo
	 * @throws IOException 
	 */
	public static String setupGitRepo(String sourceDir) throws IOException {
		File repoDir = mkTmpDir();
		File sourceDirFile = new File(sourceDir);
		GitManager gm = new GitManager();
		gm.gitInit(repoDir);
		FileUtils.copyDirectory(sourceDirFile, repoDir);
		gm.addAllFiles();
		gm.commit("init");
		String url = gm.getGitMetadataDir().toURI().toString();
		gm.close();
		return url;
	}

	public static String initEmptyRepo() {
		File repoDir = mkTmpDir();
		GitManager gm = new GitManager();
		gm.gitInit(repoDir);
		gm.addAllFiles();
		gm.commit("init");
		String url = gm.getGitMetadataDir().toURI().toString();
		gm.close();
		return url;
	}
	
	public static File mkTmpDir(String prefix, String suffix) {
		String uuid = UUID.randomUUID().toString();
		if (null != prefix) {
			uuid = uuid + prefix;
		}
		if (null != suffix) {
			uuid += suffix;
		}
		File tmpDir = new File("target/test-tmp/", uuid);
		tmpDir.mkdirs();
		return tmpDir;
	}
	
	public static File mkTmpDir() {
		return mkTmpDir(null, null);
	}
	
	public static void resetConfig() {
		try {
			Method clearConfig = ConfigurationFactory.class.getDeclaredMethod("reset");
			clearConfig.setAccessible(true);
			clearConfig.invoke(null, (Object[]) null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to reset properties", e);
		}
	}
	
	public static File updateRepoWorkingDir(String gitUrl) throws GitAPIException, CheckoutConflictException, IOException, URISyntaxException {
		Git git = Git.open(new File(new URI(gitUrl).getPath()));
		git.reset().setMode(ResetType.HARD).setRef("HEAD").call();
		File workDir = git.getRepository().getWorkTree();
		git.close();
		return workDir;
	}
}
