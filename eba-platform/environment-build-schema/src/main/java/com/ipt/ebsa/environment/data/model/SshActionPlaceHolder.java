package com.ipt.ebsa.environment.data.model;

import java.util.Arrays;
import java.util.List;

import com.ipt.ebsa.environment.v1.build.XMLSSHCommandActionDefinitionType;

final class SshActionPlaceHolder extends ActionPlaceHolder {

	final XMLSSHCommandActionDefinitionType xmlData;

	public SshActionPlaceHolder(String id, EnvironmentData environmentData, XMLSSHCommandActionDefinitionType xmlData) {
		super(id, environmentData);
		this.xmlData = xmlData;
	}
	
	@Override
	public List<? extends Action> getActions() {
		return Arrays.asList(new SshAction[] { new SshAction(this) });
	}
}