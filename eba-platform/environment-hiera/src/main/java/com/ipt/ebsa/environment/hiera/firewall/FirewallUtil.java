package com.ipt.ebsa.environment.hiera.firewall;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class
 * @author James Shepherd
 */
public class FirewallUtil {
	private static final String DEFAULT_VYATTA_HOST_PATTERN = "^(m|a)fw.*";
	private static Pattern vyattaHostPattern = null;
	private static String vyattaHostRegexp = DEFAULT_VYATTA_HOST_PATTERN;
	private static final Pattern HOST_PATTERN = Pattern.compile("(?<prefix>[^\\d]+)(?<suffix>\\d+(?:\\s*/\\s*[\\d]+)*)");
	
	/**
	 * @param host something like host01/02
	 * @return Array of [host01,host02]
	 */
	public static List<String> parseHostname(String host) {
		ArrayList<String> output = new ArrayList<>();
		Matcher m = HOST_PATTERN.matcher(host);
		if (m.find()) {
			String prefix = m.group("prefix").trim();
			String suffixi = m.group("suffix");
			
			for (String suffix : suffixi.split("/")) {
				suffix = suffix.trim();
				output.add(prefix + suffix);
			}
		} else {
			throw new IllegalArgumentException(String.format("Failed to parse hostname [%s]", host));
		}
		return output;
	}

	public static synchronized boolean isHostnameVyatta(String hostname) {
		if (null == vyattaHostPattern) {
			vyattaHostPattern = Pattern.compile(getVyattaFirewallHostnamePattern());
		}
		
		return vyattaHostPattern.matcher(hostname).matches();
	}

	public static String getVyattaFirewallHostnamePattern() {
		return vyattaHostRegexp;
	}
	
	public static void setVyattaFirewallHostnamePattern(String pattern) {
		vyattaHostRegexp = pattern;
	}
}
