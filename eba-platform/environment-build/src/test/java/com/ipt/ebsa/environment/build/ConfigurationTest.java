package com.ipt.ebsa.environment.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Properties;

import org.junit.Test;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.environment.build.test.BaseTest;

public class ConfigurationTest extends BaseTest {

	@Test
	public void testProviderPropertiesSkyscape() {
		Properties p = Configuration.getProviderProperties(ConfigurationFactory.getOrganisations().get("np"), XMLProviderType.SKYSCAPE.toString());
		
		assertEquals("Fat", p.getProperty("username"));
		assertEquals("Sam's", p.getProperty("password"));
		assertEquals("GrandSlam", p.getProperty("url.full"));
	}
	
	@Test
	public void testProviderPropertiesAwsNP() {
		Properties p = Configuration.getProviderProperties(ConfigurationFactory.getOrganisations().get("np"), XMLProviderType.AWS.toString());
		
		assertEquals("Windy", p.getProperty("username"));
		assertEquals("R@in", p.getProperty("password"));
		assertEquals("Sunny", p.getProperty("url.full"));
		assertEquals("testRegion", p.getProperty("region"));
	}
	
	@Test
	public void testProviderPropertiesAwsST() {
		Properties p = Configuration.getProviderProperties(ConfigurationFactory.getOrganisations().get("st"), XMLProviderType.AWS.toString());
		
		assertEquals("Cloudy", p.getProperty("username"));
		assertNull(p.getProperty("password"));
		assertNull(p.getProperty("url.full"));
		assertEquals("testRegion", p.getProperty("region"));
	}
}
