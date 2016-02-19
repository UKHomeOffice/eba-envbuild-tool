package com.ipt.ebsa.agnostic.client.skyscape.exception;

/**
 * Thrown if a dependency of an entity cannot be resolved.  E.g. Cannot find the VApp which is the target for a network
 *
 */
public class UnresolvedDependencyException extends Exception {
    
    
	private static final long serialVersionUID = 8895087872957055336L;

	public UnresolvedDependencyException() {
		super();
	}

	public UnresolvedDependencyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UnresolvedDependencyException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnresolvedDependencyException(String message) {
		super(message);
	}

	public UnresolvedDependencyException(Throwable cause) {
		super(cause);
	}

	
}
