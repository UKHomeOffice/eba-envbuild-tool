package com.ipt.ebsa.environment.data.model;

import java.util.Arrays;
import java.util.List;

import com.ipt.ebsa.environment.v1.build.XMLInternalHieraActionDefinitionType;

final class InternalHieraActionPlaceHolder extends ActionPlaceHolder {

	final XMLInternalHieraActionDefinitionType xmlData;

	public InternalHieraActionPlaceHolder(String id, EnvironmentData environmentData, XMLInternalHieraActionDefinitionType xmlData) {
		super(id, environmentData);
		this.xmlData = xmlData;
	}
	
	@Override
	public List<? extends Action> getActions() {
		return Arrays.asList(new InternalHieraAction[] { new InternalHieraAction(this) });
	}
}