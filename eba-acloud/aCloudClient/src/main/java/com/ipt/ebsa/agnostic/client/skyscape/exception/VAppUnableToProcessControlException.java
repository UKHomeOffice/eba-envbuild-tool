package com.ipt.ebsa.agnostic.client.skyscape.exception;

public class VAppUnableToProcessControlException extends ControlException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3571848870651317780L;

	public VAppUnableToProcessControlException() {
		super();
	}

	public VAppUnableToProcessControlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public VAppUnableToProcessControlException(String message, Throwable cause) {
		super(message, cause);
	}

	public VAppUnableToProcessControlException(String message) {
		super(message);
	}

	public VAppUnableToProcessControlException(Throwable cause) {
		super(cause);
	}
	
}
