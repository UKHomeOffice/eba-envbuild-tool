package com.ipt.ebsa.agnostic.client.skyscape.exception;

public class VMNotSuspendedControlException extends ControlException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8962949154088465167L;

	public VMNotSuspendedControlException() {
		super();
	}

	public VMNotSuspendedControlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public VMNotSuspendedControlException(String message, Throwable cause) {
		super(message, cause);
	}

	public VMNotSuspendedControlException(String message) {
		super(message);
	}

	public VMNotSuspendedControlException(Throwable cause) {
		super(cause);
	}
	
}
