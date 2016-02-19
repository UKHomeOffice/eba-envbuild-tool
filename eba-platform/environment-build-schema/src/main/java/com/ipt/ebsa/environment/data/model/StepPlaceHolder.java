package com.ipt.ebsa.environment.data.model;


final class StepPlaceHolder extends ParameterizedPlaceHolder {
	
	String actionId;

	public StepPlaceHolder(EnvironmentData environmentData, String actionId) {
		super(null, environmentData); // Steps don't need an id
		this.actionId = actionId;
	}
}