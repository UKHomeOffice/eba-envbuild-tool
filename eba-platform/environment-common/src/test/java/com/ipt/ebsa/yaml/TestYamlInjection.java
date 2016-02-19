package com.ipt.ebsa.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import com.ipt.ebsa.hiera.NodeMissingBehaviour;
import com.ipt.ebsa.yaml.YamlInjector;

public class TestYamlInjection {

	private static final String APP = "APP";
	private static final String ZONE = "IPT_ST_SIT1_COR1";
	
	private YamlInjector injector;
	private Map<String, Object> yamlObj;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		try {
			injector = new YamlInjector();
			Yaml yaml = new Yaml();
			yamlObj = (Map<String, Object>) yaml.load(new FileReader(new File("src/test/resources/common.yaml")));
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void testReplace() throws Exception {
		try {
			injector.inject(yamlObj, "system::packages/mlocate/tag", "newValue", NodeMissingBehaviour.FAIL, APP, ZONE);
			setUp();
			injector.inject(yamlObj, "test/mlocate/tag", "newValue", NodeMissingBehaviour.INSERT_ALL, APP, ZONE);
			setUp();
			injector.inject(yamlObj, "system::packages/check/tag", "newValue", NodeMissingBehaviour.INSERT_KEY_AND_VALUE_AND_PARENT_MAP_ONLY, APP, ZONE);
			setUp();
			try {
				injector.inject(yamlObj, "system::packages/check/tag/test", "newValue", NodeMissingBehaviour.INSERT_KEY_AND_VALUE_AND_PARENT_MAP_ONLY, APP, ZONE);
				Assert.fail("Expected not to be abe to insert an ancestor of the final parent");
			} catch (Exception e) {
			}
			setUp();
			try {
				injector.inject(yamlObj, "system::packages/check/tag", "newValue", NodeMissingBehaviour.INSERT_KEY_AND_VALUE_ONLY, APP, ZONE);
				Assert.fail("Expected not to be abe to insert an ancestor of the final parent");
			} catch (Exception e) {
			}
			setUp();
			try {
				injector.inject(yamlObj, "system::packages/check/tag/test", "newValue", NodeMissingBehaviour.INSERT_KEY_AND_VALUE_ONLY, APP, ZONE);
				Assert.fail("Expected not to be abe to insert an ancestor of the final parent");
			} catch (Exception e) {
			}
			setUp();
			try {
				injector.inject(yamlObj, "system::packages/check/tag/test/hooloo", "newValue", NodeMissingBehaviour.INSERT_KEY_AND_VALUE_ONLY, APP, ZONE);
				Assert.fail("Expected not to be abe to insert an ancestor of the final parent");
			} catch (Exception e) {
			}
			
			
		} catch (Exception e) {
           e.printStackTrace();
		}
	}

	@Test
	public void testExceptions() throws Exception {
		try {
			injector.inject(null, "system::packages/mlocate/tag", "newValue", NodeMissingBehaviour.FAIL, APP, ZONE);
			Assert.fail("Expected an error message");
		} catch (Exception e) {
		}
		try {
			injector.inject(yamlObj, null, "newValue", NodeMissingBehaviour.FAIL, APP, ZONE);
			Assert.fail("Expected an error message");
		} catch (Exception e) {
		}
		try {
			injector.inject(yamlObj, "", "newValue", NodeMissingBehaviour.FAIL, APP, ZONE);
			Assert.fail("Expected an error message");
		} catch (Exception e) {
		}
		try {
			injector.inject(yamlObj, "system::packages/mlocate/tag", "newValue", null, APP, ZONE);
			Assert.fail("Expected an error message");
		} catch (Exception e) {
		}
		try {
			injector.inject(yamlObj, "j", "newValue", NodeMissingBehaviour.FAIL, APP, ZONE);
			Assert.fail("Expected an error message");
		} catch (Exception e) {
		}
		try {
			injector.inject(yamlObj, "system", "newValue", NodeMissingBehaviour.FAIL, APP, ZONE);
			Assert.fail("Expected an error message");
		} catch (Exception e) {
		}
//		try {
//			injector.inject(yamlObj, "system::packages/mlocate", "newValue", NodeMissingBehaviour.FAIL);
//			Assert.fail("Expected an error message");
//		} catch (Exception e) {
//		}
		try {
			injector.inject(yamlObj, "system::packages/mlocate/tag/step", "newValue", NodeMissingBehaviour.FAIL, APP, ZONE);
			Assert.fail("Expected an error message");
		} catch (Exception e) {
		}
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testYamlRemoveLeaf() {
		assertEquals("Expected remove path", "system::services/sshd/enable", injector.remove(yamlObj, "system::services/sshd/enable"));
		assertNull("leaf not removed", ((Map<String, Object>)((Map<String, Object>) yamlObj.get("system::services")).get("sshd")).get("enable"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testYamlRemoveAll() {
		assertEquals("Expected remove path", "system::network::interfaces/eth0/ipaddress", injector.remove(yamlObj, "system::network::interfaces/eth0/ipaddress"));
		assertNotNull("check sibling still there", ((Map<String, Object>)((Map<String, Object>) yamlObj.get("system::network::interfaces")).get("eth0")).get("netmask"));
		assertNull("check removed", ((Map<String, Object>)((Map<String, Object>) yamlObj.get("system::network::interfaces")).get("eth0")).get("ipaddress"));
		
		assertEquals("Expected remove path", "system::network::interfaces", injector.remove(yamlObj, "system::network::interfaces/eth0/netmask"));
		assertNull("Check all removed", yamlObj.get("system::network::interfaces"));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testYamlRemoveHavingDecendents() {
		assertEquals("Expected remove path", "sudo::configs/sudoers", injector.remove(yamlObj, "sudo::configs/sudoers"));
		assertNotNull("sibling still there", ((Map<String, Object>)((Map<String, Object>) yamlObj.get("sudo::configs")).get("jenkins")));
		assertNull("checking removed node", ((Map<String, Object>)((Map<String, Object>) yamlObj.get("sudo::configs")).get("sudoers")));
	}
}
