package com.ipt.ebsa.agnostic.client.aws.extensions;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.jcabi.aspects.Loggable;

public class IptCredentialProvider implements AWSCredentialsProvider {

	AWSCredentials credentialslongTerm;
	String roleArnToSwitchTo;
	String roleSwitchSessionName;
	ClientConfiguration clientConfig;
	String enableSwitchRole;
	STSAssumeRoleSessionCredentialsProvider providerAssume;

	private static final Log LOG = LogFactory.getLog(IptCredentialProvider.class);

	/**
	 * The executor service used for refreshing the credentials in the
	 * background.
	 */
	private volatile ScheduledExecutorService executor;

	/** The expiration for the current instance profile credentials */
	protected volatile Date credentialsExpiration;

	/** The time of the last attempt to check for new credentials */
	protected volatile Date lastInstanceProfileCheck;

	public IptCredentialProvider(AWSCredentials credentials, String enableSwitchRole, String roleArnToSwitchTo, String roleSwitchSessionName,
			ClientConfiguration clientConfig, String autoRefresh) {
		credentialslongTerm = credentials;
		this.roleArnToSwitchTo = roleArnToSwitchTo;
		this.roleSwitchSessionName = roleSwitchSessionName;
		this.clientConfig = clientConfig;
		this.enableSwitchRole = enableSwitchRole;

		if (checkForTrue(enableSwitchRole)) {
			providerAssume = new STSAssumeRoleSessionCredentialsProvider(credentials, roleArnToSwitchTo, roleSwitchSessionName, clientConfig);
			refreshTimerThread(checkForTrue(autoRefresh), STSAssumeRoleSessionCredentialsProvider.DEFAULT_DURATION_SECONDS - 30);
		}
	}

	@Override
	public synchronized AWSCredentials getCredentials() {
		if (providerAssume != null) {
			return providerAssume.getCredentials();
		} else {
			return credentialslongTerm;
		}
	}

	@Override
	public void refresh() {
		providerAssume.refresh();
	}

	public static boolean checkForTrue(final String isTrue) {
		if (StringUtils.isBlank(isTrue)) {
			return false;
		}
		String check = isTrue.toLowerCase();
		if (check.equals("y") || check.equals("yes") || check.equals("true")) {
			return true;
		} else {
			return false;
		}
	}

	private void refreshTimerThread(boolean refreshCredentialsAsync, int asyncRefreshIntervalInSeconds) {
		if (refreshCredentialsAsync) {
			executor = Executors.newScheduledThreadPool(1);
			executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getCredentials();
					} catch (AmazonClientException ace) {
						handleError(ace);
					} catch (RuntimeException re) {
						handleError(re);
					} catch (Error e) {
						handleError(e);
					}
				}
			}, 0, asyncRefreshIntervalInSeconds, TimeUnit.SECONDS);
		}
	}

	private void handleError(Throwable t) {
		LOG.error(t.getMessage(), t);
	}

}
