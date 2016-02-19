package com.ipt.ebsa.environment.data.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.environment.v1.build.XMLParamType;
import com.ipt.ebsa.environment.v1.build.XMLUserParameterType;

public class BuildRef  {
	
	String displayName;
	String id;
	String buildName;
	String provider;
	final Map<String, String> uiParams = new HashMap<>();
	final Map<String, String> genParams = new HashMap<>();

	BuildRef(String id, String displayName, String buildName, XMLProviderType provider, List<XMLUserParameterType> userParams, List<XMLParamType> generalParams) {
		super();
		this.id = id;
		this.displayName = displayName;
		this.buildName = buildName;
		this.provider = provider == null ? "" : provider.toString();
		for (XMLUserParameterType t : userParams) {
			uiParams.put(t.getId(), t.getDisplayname());
		}
		for (XMLParamType t : generalParams) {
			genParams.put(t.getName(), t.getValue());
		}
	}

	public String getId() {
		return id;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public String getProvider() {
		return provider;
	}
	
	public Map<String, String> getUiParams() {
		return uiParams;
	}

	public Map<String, String> getGenParams() {
		return genParams;
	}
	

}