package com.ipt.ebsa.agnostic.client.exception;

import java.util.Collection;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.aws.module.AwsVmModule;

/**
 * 
 *
 */
public class ToManyResultsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7385117424240964272L;
	
	private Logger logger = LogManager.getLogger(AwsVmModule.class);

	public ToManyResultsException(Collection<?> results) {
		logger.error("To many results were detected, listing offending results:");
		for(Object result : results) {
			logger.error(ReflectionToStringBuilder.toString(result));
		}
		logger.error("Finished listing "+results.size()+" results");
	}

}
