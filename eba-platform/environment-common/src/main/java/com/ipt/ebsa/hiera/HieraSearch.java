package com.ipt.ebsa.hiera;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.ipt.ebsa.environment.StateSearchResult;

public class HieraSearch {

	private static final String SEP_REGEX = "\\/";
	
	/**
	 * Assumes the YAML Object is a m
	 * @param yaml
	 * @param path
	 * @param newValue
	 * @param behaviour
	 * @throws Exception 
	 */
	public StateSearchResult search(Map<String,Object> machineState, String keyPath) {
		StateSearchResult searchResult = new HieraStateSearchResult();
		if (machineState == null) {
			//throw new IllegalArgumentException("Object cannot be null");
			return searchResult;
		}
		if (StringUtils.isBlank(keyPath)) {
			throw new IllegalArgumentException("Path to object to set in environmentDescription cannot be empty blank or null");
		}
		
		String[] keys = keyPath.split(SEP_REGEX);		
		findNode(machineState, keyPath, keys, 0, searchResult);
		return searchResult;
		
	}

	/**
	 * Recursive method to do a search
	 * @param yaml
	 * @param keys
	 * @param i
	 * @param newValue
	 * @param behaviour
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	private void findNode(Map<String, Object> yaml, String keyPath, String[] keys, int i, StateSearchResult searchResult) 
	{
		Object o = yaml.get(keys[i]);
		if (o != null) {
			boolean isNotLastKeyInPath = i < keys.length-1;
			if (o instanceof Map ) {
				if ( isNotLastKeyInPath) {
					//dig down to find the next item
					findNode((Map<String, Object>) o, keyPath, keys, i+1, searchResult);
			    }
				else {
					searchResult.setComponentState((Map<String, Object>)o);
				}
			}
			else {
				if ( !isNotLastKeyInPath) {
					searchResult.setComponentState(yaml);
				}
			}
		}
	}
}
