package com.ipt.ebsa.manage.th;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EnvironmentManagementTestHarnessTest {
	
	// init in setup
	private File executable;
	
	@Before
	public void setUp() {
		File[] files = getOnlyOneEnvironmentMgmtTool();
		
		String osName = System.getProperty("os.name").toLowerCase();
		
		executable = osName.contains("win") ? new File(files[0], "bin/app.bat") : new File(files[0], "bin/app.sh");
		Assert.assertTrue(executable.exists());
		
		
	}
	
	/**
	 * Finds where Maven has unpacked the environment-management code to and then executes it with some parameters.
	 */
	@Test
	public void testST() {
		runScenario(1);
	}

	/**
	 * See EBSAD-10634: We had an issue where we hardcoded enivronment names. This checks that the NP environments work.
	 */
	@Test
	public void testNP() {
		runScenario(2);
	}
	
	/**
	 * We've had issues before where we found multiple instances of the enivronment-management tool in the target
	 * (of different versions). This therefore checks there is only one, and gets it.
	 * @return a directory of files which contain different OS methods of running environment management.
	 */
	private File[] getOnlyOneEnvironmentMgmtTool() {
		File target = new File("target");
		File[] files = target.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isDirectory() && name.startsWith("environment-management");
			}
		});
		Assert.assertEquals("Expected exactly one environment-management tool in the target folder. Run mvn package/install first?",
				1, files.length);
		return files;
	}

	/**
	 * Runs a specific folder scenario. 
	 * @param scenario the folder id
	 */
	private void runScenario(int scenario) {
		String[] commands = getCommands(scenario);
		
		System.out.println(commands);
		try {
			int exitCode = EnvironmentManagementTestHarness.runEnvironmentManagementTestHarness(commands);
			
			Assert.assertEquals("Exit code should be 0", 0, exitCode);
			Assert.assertTrue(new File("target/scenario" + scenario + "/report.html").exists());
		} catch (Exception e) {
			fail("Unexpected exception occurred: " + e);
		}
	}
	
	private String[] getCommands(int scenario) {
		return new String[] {
				"-command=\"run\"",
				"-scenarioConfig=\"src/test/resources/scenario" + scenario + "/scenario.properties\"",
				"-executable=\""+executable.getAbsolutePath()+"\"",
				"-config=src/test/resources/config.properties"
		};
	}
	
	
}
