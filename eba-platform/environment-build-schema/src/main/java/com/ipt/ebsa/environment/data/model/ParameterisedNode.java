package com.ipt.ebsa.environment.data.model;

import java.util.List;
import java.util.Map;

public abstract class ParameterisedNode {
	
	private String id;
	
	public ParameterisedNode(String id) {
		super();
		this.id = id;
	}
	
	public abstract Map<String, String> getParameters();
	
	public abstract List<ParameterisedNode> getChildren();

	public String getId() {
		return id;
	}
}
