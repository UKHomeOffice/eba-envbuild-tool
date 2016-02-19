package com.ipt.ebsa.environment.data.model;

import java.util.Arrays;
import java.util.List;

import com.ipt.ebsa.environment.v1.build.XMLInfrastructureProvisioningActionDefinitionType;

final class InfraActionPlaceHolder extends ActionPlaceHolder {

	final XMLInfrastructureProvisioningActionDefinitionType xmlData;

	public InfraActionPlaceHolder(String id, EnvironmentData environmentData, XMLInfrastructureProvisioningActionDefinitionType xmlData) {
		super(id, environmentData);
		this.xmlData = xmlData;
	}
	
	@Override
	public List<? extends Action> getActions() {
		return Arrays.asList(new InfraAction[] { new InfraAction(this) });
	}
}