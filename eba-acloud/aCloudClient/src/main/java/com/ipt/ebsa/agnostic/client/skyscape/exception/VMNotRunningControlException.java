package com.ipt.ebsa.agnostic.client.skyscape.exception;

public class VMNotRunningControlException extends ControlException {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3289916904425443238L;

	public VMNotRunningControlException() {
		super();
	}

	public VMNotRunningControlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public VMNotRunningControlException(String message, Throwable cause) {
		super(message, cause);
	}

	public VMNotRunningControlException(String message) {
		super(message);
	}

	public VMNotRunningControlException(Throwable cause) {
		super(cause);
	}
	
}
