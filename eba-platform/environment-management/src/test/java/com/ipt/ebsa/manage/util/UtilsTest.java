package com.ipt.ebsa.manage.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import com.ipt.ebsa.yaml.YamlUtil;

public class UtilsTest {
	
	@Test
	public void getRoleOrFQDNParamStringFromYamlTest() {
		assertEquals("soatzm01", YamlUtil.getRoleOrHostFromYaml("soatzm01.st-dev1-ebs2.ipt.local.yaml"));
		assertEquals("soatzm01", YamlUtil.getRoleOrHostFromYaml("soatzm01"));
		assertEquals("cdp", YamlUtil.getRoleOrHostFromYaml("cdp.yaml"));
		assertEquals("cdp", YamlUtil.getRoleOrHostFromYaml("cdp"));
	}
}
