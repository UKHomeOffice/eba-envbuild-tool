package com.ipt.ebsa.agnostic.client.skyscape.exception;

public class VAppEntityBusyControlException extends ControlException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2384674160544104972L;

	public VAppEntityBusyControlException() {
		super();
	}

	public VAppEntityBusyControlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public VAppEntityBusyControlException(String message, Throwable cause) {
		super(message, cause);
	}

	public VAppEntityBusyControlException(String message) {
		super(message);
	}

	public VAppEntityBusyControlException(Throwable cause) {
		super(cause);
	}
	
}
