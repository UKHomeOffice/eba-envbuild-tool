package com.ipt.ebsa.environment.data.model;

import org.apache.commons.lang.StringUtils;


/**
 * Performs an SSH command as the action.
 *
 * @author David Manning
 */
public class SshAction extends Action {

    private final SshActionPlaceHolder placeHolder;

	public SshAction(SshActionPlaceHolder placeHolder) {
		super(placeHolder.getId());
		this.placeHolder = placeHolder;
	}

	public String getCommand() {
		return placeHolder.xmlData.getRemotecommand();
	}

	public String getJumpHosts() {
		return placeHolder.xmlData.getJumphosts();
	}

	public String getMachine() {
		return placeHolder.xmlData.getMachine();
	}

	public String getSshOptsFile() {
		return placeHolder.xmlData.getSshoptsfile();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SshAction)) {
			return false;
		}
		if (!StringUtils.equals(getId(), ((SshAction)obj).getId())) {
			return false;
		}
		if (!placeHolder.equals(((SshAction)obj).placeHolder)) {
			return false;
		}
		
		return true;
	}
}

