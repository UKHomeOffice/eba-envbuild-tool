package com.ipt.ebsa.manage.transitions;

public class TransitionFailureException extends Exception {

	private static final long serialVersionUID = 1L;

	public TransitionFailureException() {
		super();
	}

	public TransitionFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TransitionFailureException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransitionFailureException(String message) {
		super(message);
	}

	public TransitionFailureException(Throwable cause) {
		super(cause);
	}

}
