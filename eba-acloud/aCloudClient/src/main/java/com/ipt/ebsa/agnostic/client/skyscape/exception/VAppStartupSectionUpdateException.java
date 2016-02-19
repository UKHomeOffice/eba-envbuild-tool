package com.ipt.ebsa.agnostic.client.skyscape.exception;

/**
 * Thrown if an update to a vApp StartupSection operation fails to complete successfully.
 *
 */
public class VAppStartupSectionUpdateException extends Exception {
    
    
	private static final long serialVersionUID = 8895087872957055336L;

	public VAppStartupSectionUpdateException() {
		super();
	}

	public VAppStartupSectionUpdateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public VAppStartupSectionUpdateException(String message, Throwable cause) {
		super(message, cause);
	}

	public VAppStartupSectionUpdateException(String message) {
		super(message);
	}

	public VAppStartupSectionUpdateException(Throwable cause) {
		super(cause);
	}

	
}
