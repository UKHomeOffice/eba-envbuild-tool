package com.ipt.ebsa.agnostic.client.aws.exception;

/**
 * 
 *
 */
public class VpcUnavailableException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4273476721801354428L;

	public VpcUnavailableException() {
	}

	public VpcUnavailableException(String message) {
		super(message);
	}

	public VpcUnavailableException(Throwable cause) {
		super(cause);
	}

	public VpcUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public VpcUnavailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
