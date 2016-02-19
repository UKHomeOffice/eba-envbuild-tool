package com.ipt.ebsa.agnostic.client.exception;

import org.apache.commons.lang.StringUtils;

/**
 * 
 *
 */
public class RetryableException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3599104367568690515L;
	
	private int statusCode;
	private String requestId = StringUtils.EMPTY;
	
	public RetryableException(String message) {
		super(message);
	}
	
	public RetryableException(String message, int statusCode, String requestId) {
		super(message);
		this.statusCode = statusCode;
		this.requestId = requestId;
	}
	
	public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }

	public int getStatusCode() {
		return statusCode;
	}
	
	public String getRequestId() {
		return requestId;
	}
	
}
