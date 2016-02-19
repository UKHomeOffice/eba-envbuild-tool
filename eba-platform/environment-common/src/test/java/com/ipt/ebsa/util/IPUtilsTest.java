package com.ipt.ebsa.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

public class IPUtilsTest {

	@Test
	public void testToFullCidr() {
		assertEquals("10.43.0.0/24", IPUtils.toFullIPv4Cidr("10.43/24"));
		assertEquals("10.43.0.0/24", IPUtils.toFullIPv4Cidr("10.43.0.0/24"));
		
		try {
			IPUtils.toFullIPv4Cidr("Walk without rhythm, and it won't attract the worm.");
			fail("not an ip address");
		} catch (Exception e) {
			assertEquals("Couldn't convert [Walk without rhythm, and it won't attract the worm.] to x.x.x.x/y notation", e.getMessage());
		}
		
		try {
			IPUtils.toFullIPv4Cidr("10.0X/24");
			fail("not an ip address");
		} catch (Exception e) {
			assertEquals("Couldn't convert [10.0X/24] to x.x.x.x/y notation", e.getMessage());
		}
	}
	
	@Test
	public void testToIPv4Address() {
		assertEquals(Arrays.asList(new String[]{"1.2.3.4"}), IPUtils.toIPv4Addresses("1.2.3.4"));
		assertEquals(Arrays.asList(new String[]{"1.2.3.4", "1.2.3.5", "1.2.3.6"}), IPUtils.toIPv4Addresses("1.2.3.4 ,5, 6 "));
		
		try {
			IPUtils.toIPv4Addresses("Don't be shocked, by the sound of my voice.");
			fail("not an ip address");
		} catch (Exception e) {
			assertEquals("Failed to parse to IPv4 addresses [Don't be shocked, by the sound of my voice.]", e.getMessage());
		}
	}
}
