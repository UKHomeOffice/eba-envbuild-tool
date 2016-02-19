package com.ipt.ebsa.util;

public class StringUtils {

	/**
	 * @param value
	 * @return null if this is an empty string, otherwise trims the string
	 */
	public static String nullIfEmptyOrTrimString(String value) {
		return value != null && value.trim().length() > 0 ? value.trim() : null;
	}
}
