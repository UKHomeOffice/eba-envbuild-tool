package com.ipt.ebsa.agnostic.client.aws.connection;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.ipt.ebsa.agnostic.client.aws.extensions.IptCredentialProvider;
import com.ipt.ebsa.agnostic.client.config.Config;
import com.jcabi.aspects.Loggable;

/**
 * This class handles connection logic
 * 
 *
 */
public class AwsConnector {

	private Logger logger = LogManager.getLogger(AwsConnector.class);

	@Inject
	@Config
	private String url;

	@Inject
	@Config
	private String user;

	@Inject
	@Config
	private String organisation;

	@Inject
	@Config
	private String password;

	@Inject
	@Config
	private String proxyHost;

	@Inject
	@Config
	private Integer proxyPort;

	@Inject
	@Config
	private String proxyScheme;

	@Inject
	@Config
	private String region = "eu-west-1";

	@Inject
	@Config
	private String useRoleSwitch;

	@Inject
	@Config
	private String roleArnToSwitchTo;

	@Inject
	@Config
	private String roleSwitchSessionName;

	@Inject
	@Config
	private String autoRefreshCredentials;

	@Inject
	@Config
	private String iamusername;

	@Inject
	@Config
	private String account = "";

	@Inject
	@Config
	private String accountRoleSwitched = "";

	private AWSCredentialsProvider provider;

	/**
	 * Sets up the credential provider for use
	 */
	public synchronized void initCredentialsProvider() {
		if (provider == null) {
			provider = new IptCredentialProvider(getLongTermCredentials(null, null), useRoleSwitch, roleArnToSwitchTo, roleSwitchSessionName,
					getClientConfig(), autoRefreshCredentials);
		}
	}

	/**
	 * Makes a connection to the cloud and returns a client.
	 * 
	 * @param config
	 * @return
	 */
	public AmazonEC2Client connectEc2() {
		AmazonEC2Client awsEC2Client = loginEc2();
		return awsEC2Client;
	}

	/**
	 * Makes a connection to the cloud and returns a client.
	 * 
	 * @param config
	 * @return
	 */
	public AmazonIdentityManagementClient connectIam() {
		AmazonIdentityManagementClient awsIamClient = null;
		synchronized (this) {
			awsIamClient = loginIam();
		}
		return awsIamClient;
	}

	/**
	 * Supplies the current credentials to use, could be a session credential or
	 * the long term. This depends on the client configuration and the use of
	 * role switching
	 * 
	 * @return current session credentials
	 */
	public synchronized AWSCredentials getCredentials() {
		if (provider == null) {
			initCredentialsProvider();
		}
		return provider.getCredentials();
	}

	/**
	 * Gets the user credentials the persist between sessions
	 * 
	 * @param access_key_id
	 * @param secret_access_key
	 * @return
	 */
	public AWSCredentials getLongTermCredentials(String access_key_id, String secret_access_key) {
		AWSCredentials credentials = null;
		if (StringUtils.isNotBlank(access_key_id) && StringUtils.isNotBlank(secret_access_key)) {
			logger.info("Using recieved credentials");
			credentials = new BasicAWSCredentials(access_key_id, secret_access_key);
		} else if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
			logger.info("Using config user/password credentials");
			credentials = new BasicAWSCredentials(user, getPassword(password));
		} else {
			// Use local credentials file as-is
			try {
				logger.info(" 'default' .aws credentials profile in user directory");
				credentials = new ProfileCredentialsProvider("default").getCredentials();
			} catch (Exception e) {
				throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
						+ "Please make sure that your credentials file is at the correct "
						+ "location (~/.aws/credentials), and is in valid format.", e);
			}
		}
		return credentials;
	}

	/**
	 * Log in to the cloud and return a client. The client will be attached to a
	 * shutdownHook so it will shut itself down
	 * 
	 * @return
	 */
	@Loggable(prepend = true, skipArgs = true)
	private AmazonEC2Client loginEc2() {
		initCredentialsProvider();
		// Setup proxy if provided
		ClientConfiguration clientConfig = getClientConfig();
		// Create the AmazonEC2Client object so we can call various APIs.
		final AmazonEC2Client ec2 = new AmazonEC2Client(provider, clientConfig);
		Region uuWest1 = Region.getRegion(Regions.fromName(region));
		ec2.setRegion(uuWest1);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					if (ec2 != null) {
						logger.debug("Closing AWS Ec2 connection");
						ec2.shutdown();
						logger.debug("AWS Ec2 connection closed");
					}
				} catch (Throwable e) {
					logger.error("Unable to close connection to AWS Ec2.", e);
				}
			}
		});

		return ec2;
	}

	/**
	 * Log in to the cloud and return a client. The client will be attached to a
	 * shutdownHook so it will shut itself down
	 * 
	 * @return
	 */
	@Loggable(prepend = true, skipArgs = true)
	private AmazonIdentityManagementClient loginIam() {
		initCredentialsProvider();
		// Setup proxy if provided
		ClientConfiguration clientConfig = getClientConfig();
		// Create the AmazonEC2Client object so we can call various APIs.
		final AmazonIdentityManagementClient iam = new AmazonIdentityManagementClient(provider, clientConfig);
		Region uuWest1 = Region.getRegion(Regions.fromName(region));
		iam.setRegion(uuWest1);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					if (iam != null) {
						logger.debug("Closing AWS Iam connection");
						iam.shutdown();
						logger.debug("AWS Iam connection closed");
					}
				} catch (Throwable e) {
					logger.error("Unable to close connection to AWS Iam.", e);
				}
			}
		});

		return iam;
	}

	/**
	 * Setup Amazon Client configuration (e.g. proxy details)
	 * 
	 * @return clientConfig
	 */
	private ClientConfiguration getClientConfig() {
		ClientConfiguration clientConfig = new ClientConfiguration();
		if (StringUtils.isNotBlank(proxyHost)) {
			clientConfig.setProxyHost(proxyHost);
			logger.info("Using proxy host: " + proxyHost);
			if (proxyPort != null) {
				clientConfig.setProxyPort(proxyPort);
				logger.info("Using proxy port: " + proxyPort);
			}
		}

		return clientConfig;
	}

	/**
	 * Cleans up the Ec2 connection by shutting it down gracefully
	 * 
	 * @param awsClient
	 */
	@Loggable
	public void logout(AmazonEC2Client awsClient) {
		try {
			if (awsClient != null) {
				logger.debug("Closing AWS Ec2 connection");
				awsClient.shutdown();
				logger.debug("AWS connection closed");
			}
		} catch (Throwable e) {
			logger.error("Unable to close connection to AWS.", e);
		}
	}

	/**
	 * Cleans up the IAM connection by shutting it down gracefully
	 * 
	 * @param awsClient
	 */
	@Loggable
	public void logout(AmazonIdentityManagementClient awsClient) {
		try {
			if (awsClient != null) {
				logger.debug("Closing AWS IAM connection");
				awsClient.shutdown();
				logger.debug("AWS connection closed");
			}
		} catch (Throwable e) {
			logger.error("Unable to close connection to AWS.", e);
		}
	}

	/**
	 * TODO: Override the password with that from your own Password Manager here
	 * 
	 * @param password
	 * @return
	 */
	private String getPassword(String password) {
		return password;
	}

	/**
	 * Returns the username
	 * 
	 * @return
	 */
	public String getUsername() {
		return user;
	}

	public void refreshProvider() {
		provider.refresh();
	}

	public String getIamUsername() {
		if (IptCredentialProvider.checkForTrue(useRoleSwitch) && StringUtils.isBlank(iamusername)) {
			AmazonIdentityManagementClient iam = new AmazonIdentityManagementClient(getLongTermCredentials(null, null), getClientConfig());
			try {
				iamusername = iam.getUser().getUser().getUserName();
			} catch (AmazonServiceException e) {
				if (e.getErrorCode().compareTo("AccessDenied") == 0) {
					String arn = null;
					String msg = e.getMessage();
					int arnIdx = msg.indexOf("arn:aws");
					if (arnIdx != -1) {
						int arnSpace = msg.indexOf(" ", arnIdx);
						arn = msg.substring(arnIdx, arnSpace);
						String[] username = arn.split("/");
						if (username.length > 0) {
							iamusername = username[username.length - 1];
						} else {
							iamusername = arn;
						}
					}
				}

			} finally {
				logout(iam);
			}

		}
		return iamusername;
	}

	public String getAccount() {
		if (IptCredentialProvider.checkForTrue(useRoleSwitch)) {
			if (StringUtils.isBlank(accountRoleSwitched)) {
				String[] tokens = roleArnToSwitchTo.split(":");
				String ac = "";
				if (tokens.length > 0) {
					ac = tokens[4];
				}
				accountRoleSwitched = ac;
			}
			return accountRoleSwitched;
		} else {
			return account;
		}
	}

	public String getRegion() {
		return region;
	}

}
