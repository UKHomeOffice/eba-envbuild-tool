package com.ipt.ebsa.environment.data.model;

import java.util.Arrays;
import java.util.List;

import com.ipt.ebsa.environment.v1.build.XMLFirewallHieraActionDefinitionType;

final class FirewallHieraActionPlaceHolder extends ActionPlaceHolder {

	final XMLFirewallHieraActionDefinitionType xmlData;

	public FirewallHieraActionPlaceHolder(String id, EnvironmentData environmentData, XMLFirewallHieraActionDefinitionType xmlData) {
		super(id, environmentData);
		this.xmlData = xmlData;
	}
	
	@Override
	public List<? extends Action> getActions() {
		return Arrays.asList(new FirewallHieraAction[] { new FirewallHieraAction(this) });
	}
}