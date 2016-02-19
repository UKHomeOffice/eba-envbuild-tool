package com.ipt.ebsa.util.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import com.ipt.ebsa.util.collection.PlaceholderMap;

public class PlaceholderMapTest {

	@Test
	public void testPutNoPlaceholders() {
		PlaceholderMap testMap = new PlaceholderMap();
		testMap.put("key1","value1");
		assertEquals("{key1=value1}", testMap.toString());
		testMap.put("key2","value2");
		assertEquals("{key1=value1, key2=value2}", testMap.toString());
		testMap.put("key1","value3");
		assertEquals("{key1=value3, key2=value2}", testMap.toString());
		testMap.remove("key2");
		assertEquals("{key1=value3}", testMap.toString());
		testMap.put("key2", "{not-a-placeholder}");
		assertEquals("{key1=value3, key2={not-a-placeholder}}", testMap.toString());
		testMap.put("key3", "\\${not-a-placeholder}");
		assertEquals("{key1=value3, key2={not-a-placeholder}, key3=${not-a-placeholder}}", testMap.toString());
	}
	
	@Test
	public void testPutAllNoPlaceholders() {
		PlaceholderMap testMap = new PlaceholderMap();
		TreeMap<String, String> valueMap = new TreeMap<>();
		valueMap.put("key1", "value1");
		valueMap.put("key2", "value2 not a \\${placeholder2}");
		valueMap.put("key3", "not a {placeholder}");
		testMap.putAll(valueMap);
		assertEquals(testMap.toString(), "{key1=value1, key2=value2 not a ${placeholder2}, key3=not a {placeholder}}");
	}
	
	@Test
	public void testPutWithPlaceholderError() {
		PlaceholderMap testMap = new PlaceholderMap();
		try {
			testMap.put("key1", "not found ${placeholder}");
			fail();
		} catch (RuntimeException e) {
			assertEquals("Key not found when looking up placeholder: 'placeholder'", e.getMessage());
		}
	}
	
	@Test
	public void testPutWithPlaceholders() {
		PlaceholderMap testMap = new PlaceholderMap();
		testMap.put("key1", "value1");
		testMap.put("key2", "value2");
		assertEquals("{key1=value1, key2=value2}", testMap.toString());
		testMap.put("key3", "replace ${key1} after");
		assertEquals("{key1=value1, key2=value2, key3=replace value1 after}", testMap.toString());
		testMap.put("key4", "several ${key1} \\${between} ${key1}");
		assertEquals("{key1=value1, key2=value2, key3=replace value1 after, key4=several value1 ${between} value1}", testMap.toString());
		testMap.put("key5", "different ${key1} and ${key4}");
		assertEquals("{key1=value1, key2=value2, key3=replace value1 after, key4=several value1 ${between} value1, key5=different value1 and several value1 ${between} value1}", testMap.toString());
		
		// check other ways of getting out values;
		assertTrue(testMap.containsValue("different value1 and several value1 ${between} value1"));
		assertEquals("different value1 and several value1 ${between} value1", testMap.get("key5"));
		assertEquals("[value1, value2, replace value1 after, several value1 ${between} value1, different value1 and several value1 ${between} value1]", testMap.values().toString());
		assertEquals("[key1=value1, key2=value2, key3=replace value1 after, key4=several value1 ${between} value1, key5=different value1 and several value1 ${between} value1]", testMap.entrySet().toString());
		
		// test clone
		assertEquals(testMap.toString(), testMap.clone().toString());
		
		// test resolve a template
		
		assertEquals("hello value1 and value2 and not a $key or a ${key}", testMap.resolvePlaceholders("hello ${key1} and ${key2} and not a \\$key or a \\${key}"));
	}
	
	@Test
	public void testPutAllPlaceholders() {
		PlaceholderMap testMap = new PlaceholderMap();
		testMap.put("key1", "value1");
		testMap.put("key2", "value2");
		TreeMap<String, String> newMap = new TreeMap<>();
		newMap.put("key2", "value2.1");
		newMap.put("key3", "value3 ${key1}");
		newMap.put("key4", "${key1} and ${key2} with ${key3}");
		testMap.putAll(newMap);
		assertEquals("{key1=value1, key2=value2.1, key3=value3 value1, key4=value1 and value2.1 with value3 value1}", testMap.toString());
	}
	
	@Test
	public void testPutRecursiveFail() {
		PlaceholderMap testMap = new PlaceholderMap();
		try {
			testMap.put("key1", "${key1}");
		} catch (RuntimeException e) {
			assertEquals("Recursive loop when looking up key: 'key1'", e.getMessage());
		}
	}
	
	@Test
	public void testPutAllRecursiveFail() {
		PlaceholderMap testMap = new PlaceholderMap();
		testMap.put("key1", "value1");
		testMap.put("key2", "value2");
		TreeMap<String, String> newMap = new TreeMap<>();
		newMap.put("key2", "value2.1");
		newMap.put("key3", "value3 ${key4}");
		newMap.put("key4", "this ${key1} and ${key2} with ${key3}");
		try {
			testMap.putAll(newMap);
			fail();
		} catch (RuntimeException e) {
			assertEquals("Recursive loop when looking up key: 'key3'", e.getMessage());
		}
	}
	
	@Test
	public void testResolutionOfPlaceHoldersWithinPlaceHolders() {
		PlaceholderMap testMap = new PlaceholderMap();
		Map<String, String> paramsMap = new TreeMap<>();
		paramsMap.put("a", "${b} a-content");
		paramsMap.put("b", "${c} b-content");
		paramsMap.put("c", "c-content");
		testMap.putAll(paramsMap);
		
		assertEquals("c-content b-content a-content", testMap.get("a"));
	}
}
