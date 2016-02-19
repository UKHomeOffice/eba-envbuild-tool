package com.ipt.ebsa.yaml;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import com.ipt.ebsa.hiera.HieraSearch;

public class YamlSearchTest {

	private Map<String, Object> yamlObj;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		try {
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
			new HieraSearch().search(yamlObj, null);
			Assert.fail();
		} catch (Exception e) {
		}
		try {
			Assert.assertNotNull(new HieraSearch().search(yamlObj, "system::augeas/grub_passwd/changes").getComponentState());
			Assert.assertNotNull(new HieraSearch().search(yamlObj, "system::packages/mlocate/tag").getComponentState());
			Assert.assertNotNull(new HieraSearch().search(yamlObj, "lvm::volume_groups/rootvg/logical_volumes/root_vol/options").getComponentState());
			Assert.assertNull(new HieraSearch().search(yamlObj, "system::packages/check/tag/test/hooloo").getComponentState());
			Assert.assertNotNull(new HieraSearch().search(yamlObj, "system::packages/gnutls").getComponentState());
			                
		} catch (Exception e) {
           e.printStackTrace();
		}
	}

}
