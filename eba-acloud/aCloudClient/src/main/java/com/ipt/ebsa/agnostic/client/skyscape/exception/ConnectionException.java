package com.ipt.ebsa.agnostic.client.skyscape.exception;

/**
 * Thrown if a log-in or -out operation fails to complete successfully.
 *
 */
public class ConnectionException extends Exception {

	private static final long serialVersionUID = -2076292595032607262L;

	public ConnectionException() {
		super();
	}

	public ConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ConnectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionException(String message) {
		super(message);
	}

	public ConnectionException(Throwable cause) {
		super(cause);
	}
}
