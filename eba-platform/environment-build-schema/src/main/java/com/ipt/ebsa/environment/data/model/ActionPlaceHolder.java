package com.ipt.ebsa.environment.data.model;

import java.util.List;

abstract class ActionPlaceHolder extends ParameterizedPlaceHolder {
			
	public ActionPlaceHolder(String id, EnvironmentData environmentData) {
		super(id, environmentData);
	}
	
	public abstract List<? extends Action> getActions();
}