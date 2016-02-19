package com.ipt.ebsa.environment.build.execute;

import java.util.ArrayList;
import java.util.List;

import com.ipt.ebsa.environment.data.model.ParameterisedNode;

public class BuildNode {

	private ParameterisedNode node;
	private BuildContext buildContext;
	private List<BuildNode> children = new ArrayList<>();
	private BuildNode parent;
	
	public ParameterisedNode getNode() {
		return node;
	}

	public void setNode(ParameterisedNode node) {
		this.node = node;
	}
	
	public String getId() {
		return node.getId();
	}
	
	public BuildContext getBuildContext() {
		return buildContext;
	}

	public void setBuildContext(BuildContext buildContext) {
		this.buildContext = buildContext;
	}
	
	public List<BuildNode> getChildren() {
		return children;
	}

	public void setChildren(List<BuildNode> children) {
		this.children = children;
	}

	public void setParent(BuildNode parent) {
		this.parent = parent;
	}

	public BuildNode getParent() {
		return parent;
	}
}
