package com.ipt.ebsa.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for managing Hiera type data
 * @author scowx
 *
 */
public class EnvironmentUtil {
   /**
	 * Uses introspection to convert the value into a standardised type
	 * @param attributes
	 * @param name
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getListFromStringOrArray(Map<String, Object> attributes, String name) {
		Object o = attributes.get(name);
		if (o == null) {
			return null;
		}
		else if (o instanceof List){
			return (List<String>) o;
		}
		else if (o instanceof String){
			List<String> s = new ArrayList<String>();
			s.add((String)o);
			return s;
		}
		else {
			throw new IllegalArgumentException("Cannot convert '"+o+"' into an ArrayList<String>");
		}
	}

	/**
	 * Returns true or false or throws an illegal argument exception if neither is relevant.
	 * @param roleOrFQDN
	 * @return
	 */
	public static boolean isRole(String roleOrFQDN) {
		if (roleOrFQDN == null || roleOrFQDN.length() < 3) {
			throw new IllegalArgumentException("roleOrFQDN is null and it should not be.");
		}
		
		if(roleOrFQDN.length() == 3) {
			return true;
		} else {
			return false;
		} 
	}
}
