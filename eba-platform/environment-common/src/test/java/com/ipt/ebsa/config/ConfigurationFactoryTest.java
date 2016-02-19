package com.ipt.ebsa.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Test;

public class ConfigurationFactoryTest {

	@Test
	public void testHomeDir() {
		String key1 = "key1";
		String value1 = "/test/path";
		String key2 = "key2";
		String value2 = "~/test/path";
		String value2expanded = System.getProperty("user.home") + "/test/path";
		
		ConfigurationFactory.getProperties().setProperty(key1, value1);
		ConfigurationFactory.getProperties().setProperty(key2, value2);
		
		assertNull(ConfigurationFactory.getFile("notexisting"));
		assertEquals(new File(value1).toURI(), ConfigurationFactory.getFile(key1).toURI());
		assertEquals(new File(value2expanded).toURI(), ConfigurationFactory.getFile(key2).toURI());
	}
}
