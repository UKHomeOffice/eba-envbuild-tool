package com.ipt.ebsa.agnostic.client.skyscape.exception;

public class VAppNotRunningControlException extends ControlException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2824726467455840638L;

	public VAppNotRunningControlException() {
		super();
	}

	public VAppNotRunningControlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public VAppNotRunningControlException(String message, Throwable cause) {
		super(message, cause);
	}

	public VAppNotRunningControlException(String message) {
		super(message);
	}

	public VAppNotRunningControlException(Throwable cause) {
		super(cause);
	}
	
}
