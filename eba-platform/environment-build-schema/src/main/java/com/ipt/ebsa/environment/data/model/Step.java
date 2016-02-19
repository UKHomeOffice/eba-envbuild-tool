package com.ipt.ebsa.environment.data.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a step in a sequence.
 *
 * @author David Manning
 */
public class Step extends ParameterisedNode {

	private StepPlaceHolder placeHolder;
	
	private EnvironmentDataImpl data;
	
	Step(StepPlaceHolder placeHolder, EnvironmentDataImpl data) {
		super(placeHolder.getId());
		this.placeHolder = placeHolder;
		this.data = data;
	}
	
	@Override
	public Map<String, String> getParameters() {
		return placeHolder.getParameters();
	}
	
	public String getActionId() {
		return placeHolder.actionId;
	}

	/**
	 * This is going to be a list containing a single {@link Action}.
	 * 
	 * @see com.ipt.ebsa.environment.data.model.ParameterisedNode#getChildren()
	 */
	@Override
	public List<ParameterisedNode> getChildren() {
		List<ParameterisedNode> actions = new ArrayList<ParameterisedNode>();
		ActionCollectionPlaceHolder topLevel = (ActionCollectionPlaceHolder) data.getActionPlaceHolders().get(placeHolder.actionId);
		if (topLevel == null) {
			throw new IllegalStateException("Action with id [" + placeHolder.actionId + "] not defined.");
		}
		for (ActionPlaceHolder ph : topLevel.actions) {
			actions.addAll(ph.getActions());
		}
		return actions;
	}
}
