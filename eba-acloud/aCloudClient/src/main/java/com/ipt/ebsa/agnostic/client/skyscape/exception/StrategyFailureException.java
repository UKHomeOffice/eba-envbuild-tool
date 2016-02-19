package com.ipt.ebsa.agnostic.client.skyscape.exception;

/**
 * Thrown if a strategy is being used and something is found which means that the strategy cannot be carried out.
 *
 */
public class StrategyFailureException extends Exception {
    
    
	private static final long serialVersionUID = 8895087872957055336L;

	public StrategyFailureException() {
		super();
	}

	public StrategyFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public StrategyFailureException(String message, Throwable cause) {
		super(message, cause);
	}

	public StrategyFailureException(String message) {
		super(message);
	}

	public StrategyFailureException(Throwable cause) {
		super(cause);
	}

	
}
