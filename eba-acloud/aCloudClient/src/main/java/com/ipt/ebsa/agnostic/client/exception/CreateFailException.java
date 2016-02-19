package com.ipt.ebsa.agnostic.client.exception;

/**
 * 
 *
 */
public class CreateFailException extends FatalException {

	boolean exists;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4781583903212349484L;
	
	public CreateFailException(String message) {
		super(message);
	}

	public CreateFailException(String message, boolean exists) {
		super(message);
		this.exists = exists;
	}

	public boolean isExists() {
		return exists;
	}

	public void setExists(boolean exists) {
		this.exists = exists;
	}

}
