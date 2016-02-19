package com.ipt.ebsa.environment.data.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.collect.ImmutableMap;

public abstract class ParameterizedPlaceHolder {
	
	private final String id;

	final Map<String, String> parameters = new HashMap<String, String>();

	protected EnvironmentData environmentData;
	
	public ParameterizedPlaceHolder(String id, EnvironmentData environmentData) {
		super();
		this.id = id;
		this.environmentData = environmentData;
	}

	public Map<String, String> getParameters() {
		return ImmutableMap.copyOf(parameters);
	}

	public String getId() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ParameterizedPlaceHolder) {
			if (!StringUtils.equals(((ParameterizedPlaceHolder) obj).getId(), id)) return false;
			if (!((ParameterizedPlaceHolder) obj).getParameters().equals(getParameters())) return false;
			return true;
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).append(parameters).toHashCode();
	}
}