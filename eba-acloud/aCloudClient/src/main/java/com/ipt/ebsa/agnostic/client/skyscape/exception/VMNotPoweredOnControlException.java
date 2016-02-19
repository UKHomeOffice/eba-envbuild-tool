package com.ipt.ebsa.agnostic.client.skyscape.exception;

public class VMNotPoweredOnControlException extends ControlException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4010005508089497228L;

	public VMNotPoweredOnControlException() {
		super();
	}

	public VMNotPoweredOnControlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public VMNotPoweredOnControlException(String message, Throwable cause) {
		super(message, cause);
	}

	public VMNotPoweredOnControlException(String message) {
		super(message);
	}

	public VMNotPoweredOnControlException(Throwable cause) {
		super(cause);
	}
	
}
