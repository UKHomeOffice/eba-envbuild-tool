package com.ipt.ebsa.environment.data.model;

import java.util.List;

import com.ipt.ebsa.environment.v1.build.XMLActionCallType;

final class CallActionPlaceHolder extends ActionPlaceHolder {

	final XMLActionCallType xmlData;

	public CallActionPlaceHolder(String id, EnvironmentData environmentData, XMLActionCallType xmlData) {
		super(id, environmentData);
		this.xmlData = xmlData;
	}
	
	@Override
	public List<? extends Action> getActions() {
		return ((EnvironmentDataImpl) environmentData).getActionPlaceHolders().get(xmlData.getAction()).getActions();
	}
}