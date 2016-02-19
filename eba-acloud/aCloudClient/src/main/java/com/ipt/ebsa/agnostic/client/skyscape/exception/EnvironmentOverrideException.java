package com.ipt.ebsa.agnostic.client.skyscape.exception;

/**
 * Thrown if an XPath substitution operation fails to complete successfully.
 *
 */
public class EnvironmentOverrideException extends Exception {
    
    
	private static final long serialVersionUID = 8895087872957055336L;

	public EnvironmentOverrideException() {
		super();
	}

	public EnvironmentOverrideException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public EnvironmentOverrideException(String message, Throwable cause) {
		super(message, cause);
	}

	public EnvironmentOverrideException(String message) {
		super(message);
	}

	public EnvironmentOverrideException(Throwable cause) {
		super(cause);
	}

	
}
