package com.ipt.ebsa.environment.data.model;


import org.apache.commons.lang.StringUtils;

import com.ipt.ebsa.environment.v1.build.XMLInfrastructureProvisioningActionDefinitionType;

/**
 * Runs an instruction file to configure infrastructure as the action.
 *
 * @author David Manning
 */
public class InfraAction extends Action {

	private InfraActionPlaceHolder placeHolder;

	public InfraAction(InfraActionPlaceHolder placeHolder) {
		super(placeHolder.getId());
		this.placeHolder = placeHolder;
	}

	public XMLInfrastructureProvisioningActionDefinitionType getDefinition() {
		return placeHolder.xmlData;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InfraAction)) {
			return false;
		}
		if (!StringUtils.equals(getId(), ((InfraAction)obj).getId())) {
			return false;
		}
		if (!placeHolder.equals(((InfraAction)obj).placeHolder)) {
			return false;
		}
		
		return true;
	}
}
