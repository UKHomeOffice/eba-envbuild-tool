package com.ipt.ebsa.environment.data.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.HashCodeBuilder;

class ActionCollectionPlaceHolder extends ActionPlaceHolder {
	
	final List<ActionPlaceHolder> actions = new ArrayList<>();
	
	public ActionCollectionPlaceHolder(String id, EnvironmentData environmentData) {
		super(id, environmentData);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ActionCollectionPlaceHolder) {
			if (!((ActionCollectionPlaceHolder) obj).actions.equals(actions)) return false;
			return super.equals(obj);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(actions).append(super.hashCode()).toHashCode();
	}


	@Override
	public List<Action> getActions() {
		ArrayList<Action> output = new ArrayList<>();
		for (ActionPlaceHolder ph : actions) {
			output.addAll(ph.getActions());
		}
		
		return output;
	}
}