package com.ipt.ebsa.agnostic.client.exception;

/**
 * 
 *
 */
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class ResourceInUseException extends Exception {

	public ResourceInUseException(String message, Object conflictObject, Throwable error) {
		super(buildMessage(message,conflictObject),error);	
	}
	
	private static String buildMessage(String message, Object conflictObject) {
		StringBuilder sb = new StringBuilder();
		sb.append("Error Message : ");
		sb.append(message);
		sb.append("Object Values : ");
		sb.append(ReflectionToStringBuilder.toString(conflictObject));
		return sb.toString();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3798763203776313564L;

}
