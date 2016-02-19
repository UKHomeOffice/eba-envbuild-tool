package com.ipt.ebsa.environment.hiera.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.Before;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.environment.metadata.generation.EnvironmentBuildMetadataImport;

public class BaseTest {

	protected File workDir = new File("target/test/hiera");

	@Before
	public void baseSetup() throws Exception {
		resetConfig();
		FileUtils.copyFile(new File("target/h2db/environment-build-database.mv.db"), new File("target/h2db/environment-build-database-copy.mv.db"));
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_NP_II-0.1.xml").importEnvironmentMetadata();
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_NP_II_PJT3_DEV1.xml").importEnvironmentMetadata();
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_PR-0.1.xml").importEnvironmentMetadata();
		new EnvironmentBuildMetadataImport("src/test/resources/HO_IPT_PR_CTL1-0.26.xml").importEnvironmentMetadata();
		
		if (workDir.exists()) {
			FileUtils.deleteDirectory(workDir);
		}
		
		if (!workDir.mkdirs()) {
			throw new RuntimeException("Failed to create workDir");
		}
	}
	
	public static void resetConfig() throws Exception {
		try {
			Method clearConfig = ConfigurationFactory.class.getDeclaredMethod("reset");
			clearConfig.setAccessible(true);
			clearConfig.invoke(null, (Object[]) null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to reset properties", e);
		}
		
		ConfigurationFactory.setConfigFile(new File("src/test/resources/test.properties"));
	}

	/**
	 * Assert that there are no differences between two File's.
	 * For directories, works recursively. Stops at first difference.
	 * @param file
	 * @param targetEnvironmentHieraDir
	 * @throws IOException 
	 */
	protected void diff(File expected, File actual) throws IOException {
		if (expected.isFile()) {
			assertTrue(String.format("expected [%s] to be a file", actual), actual.isFile());
			assertEquals(String.format("contents of [%s]", actual), FileUtils.readFileToString(expected).replace("\r", ""), FileUtils.readFileToString(actual).replace("\r", ""));
			return;
		}
		
		if (expected.isDirectory()) {
			assertTrue(String.format("expected [%s] to be a directory", actual), actual.isDirectory());
			String[] expectedList = expected.list();
			String[] actualList = actual.list();
			Arrays.sort(expectedList);
			Arrays.sort(actualList);
			assertArrayEquals(String.format("Directory list at [%s]", expected), expectedList, actualList);
			for(String file : expectedList) {
				diff(new File(expected, file), new File(actual, file));
			}
			return;
		}
		
		throw new IllegalArgumentException(String.format("[%s] not a file or directory!", expected.getAbsolutePath()));
	}
}
