package com.ipt.ebsa.manage.mco;

import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.log4j.Logger;

public final class MCOUtils {
	private static final Logger LOG = Logger.getLogger(MCOUtils.class);
	
	/**
	 * Parse the JSON response from an MCO operation
	 * @param json
	 * @return
	 */
	public static Map<String, String> parseJson(String json) {
		Map<String, String> resultMap = new TreeMap<>();
		
		if (json != null && !json.isEmpty()) {				
			LOG.debug("Parsing JSON of length " + json.length());
			try {			
				JsonReader jsonReader = Json.createReader(new StringReader(json));
				JsonArray array = jsonReader.readArray();
				LOG.debug("Array size: " + array.size());
				for (int i = 0; i < array.size(); i++) {
					JsonObject host = array.getJsonObject(i);
					
					String hostname = host.getString("sender");
					LOG.debug(String.format("Object %s host: %s",  i, hostname));
					
					JsonObject hostData = host.getJsonObject("data");
					String output = "";
					if (hostData != null && hostData.containsKey("out")) {
						output = hostData.getString("out");				
					}
					resultMap.put(hostname, output);
				}
				jsonReader.close();
			} catch (Exception e) {
				LOG.error("Failed to parse JSON response from MCO", e);
			}
			
			LOG.debug("Compiled report for " + resultMap.size() + " hosts");
		} else {
			LOG.error("No JSON returned from MCO");
		}
		
		return resultMap; 
	}
}
