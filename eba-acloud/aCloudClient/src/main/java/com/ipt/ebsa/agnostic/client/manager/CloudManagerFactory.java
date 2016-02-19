package com.ipt.ebsa.agnostic.client.manager;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.aws.manager.AwsCloudManager;
import com.ipt.ebsa.agnostic.client.skyscape.manager.SkyscapeCloudManager;

/**
 * 
 *
 */	
public class CloudManagerFactory {
	
	@Inject
	AwsCloudManager awsManager;
	
	@Inject
	SkyscapeCloudManager skyscapeManager;

	private Logger logger = LogManager.getLogger(CloudManagerFactory.class);

	public ICloudManager getCloudManager(CloudManagerEnum providor) {
		switch (providor) {
		case SKYSCAPE:
			logger.debug("Creating an instance of the SkyscapeCloudManager");
			return skyscapeManager;

		case AWS:
			logger.debug("Creating an instance of the AwsCloudManager");
			return awsManager;

		case MICROSOFTAZURE:
			
		case GOOGLECOMPUTE:
		case IBMSOFTLAYER:
		default:
			logger.error("Not implemented!, Cannot create an instance of "
					+ providor.name());
			throw new UnsupportedOperationException(providor.name());
		}
	}

}
