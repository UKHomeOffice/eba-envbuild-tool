package com.ipt.ebsa.puppet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.puppet.PuppetError;

public class PuppetErrorTest {

	@Test
	public void testForLockedError() throws IOException {
		String logToTest = loadTestLog("locked.txt");
		PuppetError pe = PuppetError.getPuppetErrorForLog(logToTest);
		Assert.assertEquals(pe, PuppetError.LOCKED);
	}
	
	@Test
	public void testForUnknownError() throws IOException {
		String logToTest = loadTestLog("yum-nothing-to-do.txt");
		PuppetError pe = PuppetError.getPuppetErrorForLog(logToTest);
		Assert.assertEquals(pe, PuppetError.UNKNOWN);
	}
	
	@Test
	public void testForRpmFailFileExistsError() throws IOException {
		String logToTest = loadTestLog("rpm-fail-file-exists.txt");
		PuppetError pe = PuppetError.getPuppetErrorForLog(logToTest);
		Assert.assertEquals(pe, PuppetError.RPM_FAIL_FILE_EXISTS);
	}
	
	private String loadTestLog(String logFileName) throws IOException{
		String logsBaseFolder = "src/test/resources/puppet-logs/";
		byte[] fileContents = Files.readAllBytes(Paths.get(logsBaseFolder, logFileName));
		return new String(fileContents);
	}
}
