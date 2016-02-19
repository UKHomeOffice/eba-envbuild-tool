/**
 * 
 */
package com.ipt.ebsa.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.TreeMap;

import org.junit.Test;

import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;

/**
 * @author LONJS43
 *
 */
public class YamlRemoveTest {
	
	private static final String APP = "APP";
	private static final String ZONE = "IPT_ST_SIT1_COR1";
	
	@Test
	public void testYamlUpdate() {
		YamlInjector injector = new YamlInjector();
		String path = "path/to/yaml";
		TreeMap<String, Object> yaml = new TreeMap<>();
		TreeMap<String, Object> moreYaml = new TreeMap<>();
		TreeMap<String, Object> evenMoreYaml = new TreeMap<>();
		evenMoreYaml.put("yaml", "egg");
		moreYaml.put("to", evenMoreYaml);
		yaml.put("path", moreYaml);
		HieraEnvironmentUpdate yu = injector.remove(path, yaml, APP, ZONE);
		assertEquals("checking remove path", "path", yu.getPathElementsRemoved());
		assertEquals("check existing value", "{to={yaml=egg}}", yu.getExistingValue().toString());
		assertEquals("check existing path", "path", yu.getExistingPath());
		assertNull("check required path", yu.getRequestedPath());
		assertNull("check required value", yu.getRequestedValue());
	}
}
