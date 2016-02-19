package com.ipt.ebsa.manage;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Test;

import com.ipt.ebsa.config.ConfigurationFactory;

import static org.junit.Assert.*;
import static com.ipt.ebsa.manage.Configuration.*;
import static org.apache.log4j.Level.*;

public class ConfigurationTest {
	
	@Test
	public void getOpenSSHLoggingLevelsTest() {
		assertNull(getOpenSSHLoggingLevels());
		
		ConfigurationFactory.getProperties().setProperty(OPEN_SSL_LOGGING_LEVELS, "");
		assertNull(getOpenSSHLoggingLevels());
		
		// Test trimming, skipping duplicates and invalid levels added as DEBUG
		ConfigurationFactory.getProperties().setProperty(OPEN_SSL_LOGGING_LEVELS, "  WARN  ,INFO,   ,, WARN ,  BLAH ");
		assertEquals(new LinkedHashSet<Level>(Arrays.asList(WARN, INFO, DEBUG)), getOpenSSHLoggingLevels());
	}
	
	@After
	public void afterEveryTest() {
		ConfigurationFactory.getProperties().remove(OPEN_SSL_LOGGING_LEVELS);
	}

}
