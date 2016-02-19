package com.ipt.ebsa.manage.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class VersionCompareTest {
	
	private static final int FIRST_ARG_IS_LOWEST = -1;
	private static final int FIRST_ARG_IS_HIGHEST = 1;
	private static final int BOTH_ARGS_ARE_EQUAL = 0;
	
	@Test
	public void testNumeric() {
		VersionCompare compare = new VersionCompare();
		assertEquals(FIRST_ARG_IS_LOWEST, compare.compare("1.0.0", "2.0.0"));
		assertEquals(BOTH_ARGS_ARE_EQUAL, compare.compare("1.0.0", "1.0.0"));
		assertEquals(FIRST_ARG_IS_HIGHEST, compare.compare("2.0.0", "1.0.0"));
		
		assertEquals(FIRST_ARG_IS_LOWEST, compare.compare("1.0.0", "1.10.0"));
		assertEquals(BOTH_ARGS_ARE_EQUAL, compare.compare("1.10.0", "1.10.0"));
		assertEquals(FIRST_ARG_IS_HIGHEST, compare.compare("1.10.0", "1.0.0"));
		
		assertEquals(FIRST_ARG_IS_LOWEST, compare.compare("1.0.0", "1.0.10"));
		assertEquals(BOTH_ARGS_ARE_EQUAL, compare.compare("1.0.10", "1.0.10"));
		assertEquals(FIRST_ARG_IS_HIGHEST, compare.compare("1.0.10", "1.0.0"));
	}
	
	@Test
	public void testNonNumeric() {
		VersionCompare compare = new VersionCompare();
		assertEquals(FIRST_ARG_IS_HIGHEST, compare.compare("abcdef", "1.0.0"));	
		assertEquals(BOTH_ARGS_ARE_EQUAL, compare.compare("abcdef", "abcdef"));
		assertEquals(FIRST_ARG_IS_HIGHEST, compare.compare("ghijkl", "abcdef"));
		assertEquals(FIRST_ARG_IS_LOWEST, compare.compare("abcdef", "ghijkl"));
	}
	
}
