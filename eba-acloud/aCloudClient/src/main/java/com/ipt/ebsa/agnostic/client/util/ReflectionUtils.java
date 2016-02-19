package com.ipt.ebsa.agnostic.client.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;

/**
 * 
 *
 */
public class ReflectionUtils {

	private static Logger logger = LogManager.getLogger(ReflectionUtils.class);
	
	public static Method locateActionMethod(String operationType,
			String componentType, Object target) {
		logger.debug("Started locateActionMethod");
		Method returnMethod = null;
		String methodName = operationType.toLowerCase() + componentType;
		Method[] methods = target.getClass().getMethods();
		boolean duplicate = false;
		logger.debug("Looking for method name ["+methodName+" on class "+target.getClass().getName());
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				if (duplicate == true) {
					throw new RuntimeException(
							"Duplicate method discovered for method name "
									+ methodName + "in class "
									+ target.getClass().getName()+"]");
				}
				logger.debug("Found method name ["+methodName+" on class "+target.getClass().getName()+ "with signature["+Arrays.toString(method.getParameterTypes())+"]");
				returnMethod = method;
				duplicate = true;
			}
		}
		if(!duplicate) {
			logger.error("Did not find method name ["+methodName+" on class "+target.getClass().getName()+"]");
			throw new RuntimeException(
					"Did not find method name "
							+ methodName + " in class "
							+ target.getClass().getName());
		}
		logger.debug("Finished locateActionMethod");
		return returnMethod;
	}

	public static void executeActionMethod(Method method, Object executingObject,
			HashMap<Class<?>, Object> objects) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		logger.debug("Started executeActionMethod");
		if (method != null) {
			Class<?>[] parameters = method.getParameterTypes();
			Object[] args = new Object[parameters.length];

			int index = 0;
			for (Class<?> arg : parameters) {
				args[index] = objects.get(arg);
				if(args[index] == null) {
					throw new IllegalArgumentException("Parameter for argument index "+index+" is null, this should be a non null instance of "+arg.getName() + " for method "+method.getName());
				}
				index++;
			}

			// executeNoParams
			logger.debug(String.format("Executing method %s on class %s with args %s",method.getName(), executingObject.getClass().getName(), ReflectionToStringBuilder.toString(args)));
			method.invoke(executingObject, args);
		}
		logger.debug("Finished executeActionMethod");
	}
	
	public static void executeCommandWithStrategy(Object command, Object componentName, Object target, HashMap<Class<?>, Object> objects) throws ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		logger.debug("Started executeCommandWithStrategy");
		if (command != null) {
			Method cloudManagerMethod = ReflectionUtils.locateActionMethod(command.toString(),
					componentName.toString(), target);
			if (cloudManagerMethod != null) {
				
				ReflectionUtils.executeActionMethod(cloudManagerMethod, target, objects);
			} else {
				warnNotApplicable(command, componentName.toString());
			}
		}
		logger.debug("Finished executeCommandWithStrategy");
	}
	
	private static void warnNotApplicable(Object command, String type) {
		logger.warn(String
				.format("Command %s is not supported for %s.  No action will be taken.",
						command.toString(), type));
	}
}
