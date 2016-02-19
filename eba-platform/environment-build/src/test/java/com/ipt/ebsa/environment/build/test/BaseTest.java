package com.ipt.ebsa.environment.build.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.environment.build.Configuration;
import com.ipt.ebsa.environment.metadata.generation.EnvironmentBuildMetadataImport;

public class BaseTest {

	private File workDir = new File("target/test/workDir");
	
	@Before
	public void resetConfig() throws Exception {
		TestHelper.resetConfig();
		FileUtils.deleteDirectory(workDir);
		if (!workDir.mkdirs()) {
			throw new RuntimeException("Failed to create workDir");
		}
		FileUtils.copyFile(new File("target/h2db/environment-build-database.mv.db"), new File("target/h2db/environment-build-database-copy.mv.db"));
		ConfigurationFactory.setConfigFile(new File("src/test/resources/test.properties"));
		Configuration.configureConnectionData();
	}
	
	protected File getWorkDir() {
		return workDir;
	}
	
	protected String getWorkDirPath() {
		return getWorkDir().getAbsolutePath();
	}
}
