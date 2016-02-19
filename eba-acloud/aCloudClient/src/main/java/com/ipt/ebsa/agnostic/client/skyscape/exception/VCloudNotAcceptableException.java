package com.ipt.ebsa.agnostic.client.skyscape.exception;

import com.vmware.vcloud.sdk.VCloudException;

public class VCloudNotAcceptableException extends Exception {

	public VCloudNotAcceptableException(VCloudException ve) {
		super(ve.getMessage(), ve);
		this.setStackTrace(ve.getStackTrace());
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3339124178306758139L;

}
