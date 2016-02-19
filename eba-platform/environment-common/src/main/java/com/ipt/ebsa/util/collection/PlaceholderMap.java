package com.ipt.ebsa.util.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a normal map, but put and putAll parse the given value/values for
 * ${parameter} and look for key "parameter" and replace ${parameter} with the value
 * for key ${parameter}. This is done recursively, an exception is thrown if there is
 * a loop in the recursion. You can escape $ with \$ (so in a java string "not a \\${placeholder}")
 * @author LONJS43
 *
 */
public class PlaceholderMap implements Map<String, String>, Cloneable {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(?<!\\\\)\\$\\{([^}]+)\\}");
	private static final Pattern ESCAPED_PLACEHOLDER_PATTERN = Pattern.compile("\\\\\\$");
	private static final String QUOTED_DOLLAR = Matcher.quoteReplacement("$");
	
	private TreeMap<String, String> myMap = new TreeMap<>();

	/**
	 * @return map with \$ replaced with $
	 */
	private Map<String, String> getUnescapedMap() {
		TreeMap<String, String> newMap = new TreeMap<>();
		
		for (Map.Entry<String, String> me : myMap.entrySet()){
			newMap.put(me.getKey(), unescape(me.getValue()));
		}
		
		return newMap;
	}

	/**
	 * Remove escapes \$ -> $
	 * @param value
	 * @return
	 */
	private String unescape(String value) {
		return ESCAPED_PLACEHOLDER_PATTERN.matcher(value).replaceAll(QUOTED_DOLLAR);
	}
	
	/**
	 * @return
	 * @see java.util.AbstractMap#isEmpty()
	 */
	public boolean isEmpty() {
		return myMap.isEmpty();
	}

	/**
	 * @return
	 * @see java.util.TreeMap#size()
	 */
	public int size() {
		return myMap.size();
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		return myMap.containsKey(key);
	}

	/**
	 * @param value
	 * @return
	 * @see java.util.TreeMap#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		return getUnescapedMap().containsValue(value);
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#get(java.lang.Object)
	 */
	public String get(Object key) {
		// could be optimised
		return getUnescapedMap().get(key);
	}

	/**
	 * Resolves placeholders, where placeholder keys could already be in this map, or
	 * be in the map we are putAll-ing.
	 * @param map
	 * @see java.util.TreeMap#putAll(java.util.Map)
	 */
	public void putAll(Map<? extends String, ? extends String> map) {
		myMap.putAll(map);
		
		// now need to resolve placeholders
		for (Map.Entry<? extends String, ? extends String> me : map.entrySet()) {
			put(me.getKey(), me.getValue());
		}
	}

	/**
	 * Resolves placeholders.
	 * @param key
	 * @param value
	 * @return
	 * @see java.util.TreeMap#put(java.lang.Object, java.lang.Object)
	 */
	public String put(String key, String value) {
		String derivedValue = resolvePlaceholders(key, value);
		return myMap.put(key, derivedValue);
	}

	/**
	 * @param template String with placeholders that are in this map as keys
	 * @return 
	 */
	public String resolvePlaceholders(String template) {
		return unescape(resolvePlaceholders(template, new ArrayList<String>()));
	}
	
	/**
	 * @throws RuntimeException if we hit a loop when deriving the value, or a key doesn't exist
	 * @param key key we are deriving for (used to check for loops)
	 * @param value may contain placeholders
	 * @return derivedValue
	 */
	private String resolvePlaceholders(String key, String value) {
		ArrayList<String> keysFollowed = new ArrayList<>();
		keysFollowed.add(key);
		return resolvePlaceholders(value, keysFollowed);
	}
		
	/**
	 * @throws RuntimeException if we hit a loop when deriving the value, or a key doesn't exist
	 * @param value may contain placeholders
	 * @param keysFollowed list of keys we have already tried to decode
	 * @return derivedValue
	 */
	private String resolvePlaceholders(String value, List<String> keysFollowed) {
		if (null == value) {
			return "";
		}
		
		Matcher m = PLACEHOLDER_PATTERN.matcher(value);
		
		while (m.find()) {
			// we still have placeholders
			String key = m.group(1);
			
			if (keysFollowed.contains(key)) {
				throw new RuntimeException("Recursive loop when looking up key: '" + key + "'");
			}
			
			String placeholderValue = myMap.get(key);
			
			if (null == placeholderValue) {
				throw new RuntimeException("Key not found when looking up placeholder: '" + key + "'");
			}
			
			if (PLACEHOLDER_PATTERN.matcher(placeholderValue).find()) {
				// we need to recurse to resolve placeholder value
				ArrayList<String> moreKeysFollowed = new ArrayList<>(keysFollowed);
				moreKeysFollowed.add(key);
				value = value.replace("${" + key + "}", resolvePlaceholders(placeholderValue, moreKeysFollowed));
			} else {
				// we have a leaf value, can continue with next placeholder
				value = value.replace("${" + key + "}", placeholderValue);
			}

			m = PLACEHOLDER_PATTERN.matcher(value);
		}
		
		return value;
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.TreeMap#remove(java.lang.Object)
	 */
	public String remove(Object key) {
		return myMap.remove(key);
	}

	/**
	 * 
	 * @see java.util.TreeMap#clear()
	 */
	public void clear() {
		myMap.clear();
	}

	/**
	 * @return
	 * @see java.util.TreeMap#keySet()
	 */
	public Set<String> keySet() {
		return myMap.keySet();
	}

	/**
	 * @return
	 * @see java.util.TreeMap#values()
	 */
	public Collection<String> values() {
		return getUnescapedMap().values();
	}

	/**
	 * @return
	 * @see java.util.TreeMap#entrySet()
	 */
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		return getUnescapedMap().entrySet();
	}
	
	/**
	 * @param o
	 * @return
	 * @see java.util.AbstractMap#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return getUnescapedMap().equals(o);
	}

	/**
	 * @return
	 * @see java.util.AbstractMap#hashCode()
	 */
	public int hashCode() {
		return getUnescapedMap().hashCode();
	}

	/**
	 * @return
	 * @see java.util.AbstractMap#toString()
	 */
	public String toString() {
		return getUnescapedMap().toString();
	}
	
	/**
	 * Deep-copy of this map, note that this map only contains
	 * String's as key and values, which are immutable, so we
	 * don't create new String objects.
	 */
	@Override
	public PlaceholderMap clone() {
		PlaceholderMap newMap = new PlaceholderMap();
		newMap.myMap.putAll(myMap);
		return newMap;
	}
}
