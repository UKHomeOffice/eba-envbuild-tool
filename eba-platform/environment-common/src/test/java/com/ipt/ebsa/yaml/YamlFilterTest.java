package com.ipt.ebsa.yaml;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

public class YamlFilterTest {
	
	@Test
	public void testFilter() throws IOException {
		assertEquals(FileUtils.readFileToString(new File("src/test/resources/filter-out.yaml")).replace("\r", ""),
			YamlUtil.filterByPaths(FileUtils.readFileToString(new File("src/test/resources/filter-in.yaml")),
			Sets.newSet(
				"path1/path11",
				"Longer\\/path"
			)).replace("\r", ""));
	}
	
	@Test
	public void testFilterAll() throws IOException {
		assertEquals("",YamlUtil.filterByPaths("test: value",
			Sets.newSet(
				"path1/path11",
				"Longer\\/path"
			)));
	}
	
	@Test
	public void testFilterEmpty() throws IOException {
		assertEquals("",YamlUtil.filterByPaths("",
			Sets.newSet(
				"path1/path11",
				"Longer\\/path"
			)));
	}
}
