package com.ipt.ebsa.environment.data.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * An action to be taken in order to achieve the outcome of a {@link Step}.
 *
 * @author David Manning
 */
public abstract class Action extends ParameterisedNode {

	public Action(String id) {
		super(id);
	}
	
	/**
	 * This implementation returns {@link Collections#emptyMap()}.
	 */
	@Override
	public Map<String, String> getParameters() {
		return Collections.emptyMap();
	}
	
	/**
	 * This implementation returns {@link Collections#emptyList()}.
	 */
	@Override
	public List<ParameterisedNode> getChildren() {
		return Collections.emptyList();
	}
}
