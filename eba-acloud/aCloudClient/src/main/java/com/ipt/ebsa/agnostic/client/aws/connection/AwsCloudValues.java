package com.ipt.ebsa.agnostic.client.aws.connection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.Retryable;
import com.jcabi.aspects.Loggable;

/**
 * Container class used to hold some shared values for convenience
 * 
 *
 */
@Singleton
public class AwsCloudValues {

	private Logger logger = LogManager.getLogger(AwsCloudValues.class);
	private AmazonEC2Client ec2client;
	private AmazonIdentityManagementClient identityManagementClient;
	private AWSCredentials credentials;

	@Inject
	private AwsConnector connector;

	/**
	 * Initialise and/or return a CloudValues object
	 * 
	 * @return
	 */
	private void initEc2Client() {
		if (ec2client == null || tokenSessionExpiry()) {
			synchronized (connector) {
				AwsRetryManager.run(new Retryable<Void>() {
					@Override
					public Void run() {
						setCredentials(connector.getCredentials());
						setEc2Client(connector.connectEc2());
						return null;
					}
				});
			}
		}
	}

	/**
	 * Initialise and/or return a CloudValues object
	 * 
	 * @return
	 */
	private void initIAMClient() {
		if (identityManagementClient == null || tokenSessionExpiry()) {
			synchronized (connector) {
				AwsRetryManager.run(new Retryable<Void>() {
					@Override
					public Void run() {
						setCredentials(connector.getCredentials());
						setIamClient(connector.connectIam());
						return null;
					}
				});

			}
		}
	}

	private synchronized boolean tokenSessionExpiry() {
		if (credentials == null
				|| (credentials != null && (!credentials.getAWSAccessKeyId().equals(connector.getCredentials().getAWSAccessKeyId()) || !credentials
						.getAWSSecretKey().equals(connector.getCredentials().getAWSSecretKey())))) {

			if (identityManagementClient != null) {
				connector.logout(identityManagementClient);
				identityManagementClient = null;
			}

			if (ec2client != null) {
				connector.logout(ec2client);
				ec2client = null;
			}
			logger.debug("Session Token Expired");
			return true;
		} else {
			return false;
		}
	}

	public synchronized AmazonEC2Client getEC2Client() {
		initEc2Client();
		return ec2client;
	}

	public synchronized AmazonIdentityManagementClient getIAMClient() {
		initIAMClient();
		return identityManagementClient;
	}

	public synchronized AWSCredentials getCredentials() {
		if (credentials == null || tokenSessionExpiry()) {
			connector.getCredentials();
		}
		return credentials;
	}

	private void setEc2Client(AmazonEC2Client client) {
		this.ec2client = client;
	}

	private void setIamClient(AmazonIdentityManagementClient client) {
		this.identityManagementClient = client;

	}

	private void setCredentials(AWSCredentials credential) {
		this.credentials = credential;
	}

	public String getIamUsername() {
		if (StringUtils.isNotBlank(connector.getIamUsername())) {
			return connector.getIamUsername();
		} else {
			return getIAMClient().getUser().getUser().getUserName();
		}

	}

	public String getAccount() {
		return connector.getAccount();
	}

}
