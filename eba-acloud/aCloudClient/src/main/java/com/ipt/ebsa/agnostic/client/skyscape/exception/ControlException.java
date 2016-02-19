package com.ipt.ebsa.agnostic.client.skyscape.exception;

/**
 * Thrown if a command operation, such as power on, restart, fails to complete successfully.
 *
 */
public class ControlException extends Exception {
    
    
	private static final long serialVersionUID = 8895087872957055336L;

	public ControlException() {
		super();
	}

	public ControlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ControlException(String message, Throwable cause) {
		super(message, cause);
	}

	public ControlException(String message) {
		super(message);
	}

	public ControlException(Throwable cause) {
		super(cause);
	}

	
}
