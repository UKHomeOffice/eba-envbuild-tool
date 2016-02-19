package com.ipt.ebsa.agnostic.client.logging;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;

/**
 * Logging Utilities for providing a consistent log format
 *
 */
public class LogUtils {
	
	/** Logger */
	private static final Logger logger = Logger.getLogger(LogUtils.class);
	
	/** Actions to be logged */
	public enum LogAction {
		ADDING, ADDED, ASSOCIATING, ASSOCIATED, ATTACHING, ATTACHED, CREATING, CREATED, DELETING, DELETED, DETACHING, DETACHED, DISASSOCIATING, DISASSOCIATED, GETTING, GOT, IGNORING, REBOOTING, REBOOTED, RELEASING, RELEASED, STARTING, STARTED, STOPPING, STOPPED, TERMINATING, TERMINATED, UPDATING, UPDATED, PEERING, PEERING_FAILED, PEERED
	}
	
	/**
	 * Logs the action against the target together with the listed fieldNames from object
	 * @param action
	 * @param target
	 * @param object
	 * @param fieldNames
	 */
	public static void log(LogAction action, String target, Object object, String... fieldNames) {
		doLog(action, message(action, target, object, fieldNames));
	}
	
	/**
	 * Logs the action against the target together with the message and the listed fieldNames from object
	 * @param action
	 * @param target
	 * @param message
	 * @param object
	 * @param fieldNames
	 */
	public static void log(LogAction action, String target, String message, Object object, String... fieldNames) {
		String text = message(action, target, object, fieldNames);
		doLog(action, text + " " + message);
	}
	
	/**
	 * Logs the action against the target together with the message
	 * @param action
	 * @param target
	 * @param message
	 */
	public static void log(LogAction action, String target, String message) {
		String text = message(action, target, null, "");
		doLog(action, text + " " + message);
	}
	
	/**
	 * Logs the action against the target
	 * @param action
	 * @param target
	 */
	public static void log(LogAction action, String target) {
		doLog(action, message(action, target, null, ""));
	}
	
	/**
	 * Returns a message consisting of the action performed against the target together with a list of the fieldNames and values from object 
	 * @param action
	 * @param target
	 * @param object
	 * @param fieldNames
	 * @return
	 */
	private static String message(LogAction action, String target, Object object, String... fieldNames) {
		StringBuilder message = new StringBuilder();
		message.append(action);
		message.append(" ");
		message.append(target);
		boolean foundFields = false;
		if (object != null) {
			for (String fieldName : fieldNames) {
				try {
					String value = BeanUtils.getProperty(object, fieldName);
					message.append(" ");
					message.append(fieldName);
					message.append("=");
					message.append(value);
					message.append(",");
					foundFields = true;
				} catch (Exception e) {
					logger.warn("Error getting property: " + fieldName + " from: " + object + " - " + e.getMessage(), e);
				}
			}
			if (foundFields) {
				message.setLength(message.length() - 1);
			}
		}
		return message.toString();
	}
	
	/**
	 * Logs "ING" actions (e.g. creatING") at debug level and "ed" actions (e.g. "creatED") at info level
	 * @param action
	 * @param message
	 */
	private static void doLog(LogAction action, String message) {
		if (action.toString().endsWith("FAILED")) {
			logger.error(message);
		} else if (action.toString().endsWith("ED")) {
			logger.info(message);
		} else {
			logger.debug(message);
		}
	}
}
