package com.ipt.ebsa.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class IPUtils {
	private static final Pattern CIDR_PATTERN = Pattern.compile("(\\d{1,3})(\\.\\d{1,3})?(\\.\\d{1,3})?(\\.\\d{1,3})?/(?<mask>\\d{1,2})");
	private static final Pattern IPv4_PATTERN = Pattern.compile("(?<ip>(?<prefix>\\d{1,3}(?:\\.\\d{1,3}){2})\\.\\d{1,3})(?<csv>(?:\\s*,\\s*\\d{1,3})*)");
	
	/**
	 * @param cidr something like "10.43/24"
	 * @return "10.43.0.0/24"
	 */
	public static String toFullIPv4Cidr(String cidr) {
		Matcher m = getFullIPv4CidrMatcher(cidr);
		if (m.find()) {
			StringBuilder sb = new StringBuilder(m.group(1));
			for (int i = 2; i < 5; i++) {
				String bite = m.group(i);
				if (StringUtils.isBlank(bite)) {
					bite = ".0";
				}
				sb.append(bite);
			}
			
			String mask = m.group("mask");
			if (null != mask) {
				sb.append("/").append(mask);
			}
			
			return sb.toString();
		} else {
			throw new IllegalArgumentException(String.format("Couldn't convert [%s] to x.x.x.x/y notation", cidr));
		}
	}

	/**
	 * 
	 * @param cidr something like "10.43/24"
	 * @return is of that format
	 */
	public static boolean isIPv4Cidr(String cidr) {
		return getFullIPv4CidrMatcher(cidr).find();
	}
	
	private static Matcher getFullIPv4CidrMatcher(String cidr) {
		if (null != cidr) {
			return CIDR_PATTERN.matcher(cidr);
		} else {
			throw new RuntimeException("Failed to parse null IPv4 Address");
		}
	}
	
	/**
	 * @param ipAddress something like "1.2.3.4" or "1.2.3.4,5,6"
	 * @return ["1.2.3.4"] or ["1.2.3.4","1.2.3.5","1.2.3.6"]
	 */
	public static List<String> toIPv4Addresses(String ipAddress) {
		ArrayList<String> output = new ArrayList<>();
		Matcher m = getIPv4AddressMatcher(ipAddress);
		
		if (m.find()) {
			output.add(m.group("ip"));
			String csv = m.group("csv");
			if (null != csv) {
				String prefix = m.group("prefix");
				for (String bite : csv.split(",")) {
					bite = bite.trim();
					if (bite.length() > 0) {
						output.add(prefix + "." + bite);
					}
				}
			}
		} else {
			throw new IllegalArgumentException(String.format("Failed to parse to IPv4 addresses [%s]", ipAddress));
		}
		
		return output;
	}

	/**
	 * @param ipAddress something like "1.2.3.4" or "1.2.3.4,5,6"
	 * @return true if is of this format
	 */
	public static boolean isIPv4Addresses(String ipAddress) {
		return getIPv4AddressMatcher(ipAddress).find();
	}
	
	private static Matcher getIPv4AddressMatcher(String ipAddress) {
		if (null != ipAddress) {
			return IPv4_PATTERN.matcher(ipAddress);
		} else {
			throw new RuntimeException("cannot parse null ipAddress");
		}
	}
}
