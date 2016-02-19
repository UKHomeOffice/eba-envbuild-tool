package com.ipt.ebsa.agnostic.client.util;

public class StringTools {
	
	/**
	 * Returns the last String from a '.' delimited string.
	 * 
	 * @param tokenString
	 * @return
	 */
	public static String lastDotValue(String tokenString) {
		String[] strings = tokenString.split("\\.");
		return strings[strings.length -1];
	}
	
}
