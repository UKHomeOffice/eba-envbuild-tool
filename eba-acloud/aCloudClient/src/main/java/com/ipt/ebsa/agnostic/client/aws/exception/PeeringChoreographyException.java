package com.ipt.ebsa.agnostic.client.aws.exception;

import com.ipt.ebsa.agnostic.client.aws.extensions.VpcPeeringConnectionStatus;

/**
 * 
 *
 */
public class PeeringChoreographyException extends Exception {

	public PeeringChoreographyException(String environment, String network, VpcPeeringConnectionStatus resultStatus) {
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 560396967724066795L;

}
