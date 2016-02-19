package com.ipt.ebsa.agnostic.client.skyscape.connection;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.logging.Level;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.FakeSSLSocketFactory;
import com.ipt.ebsa.agnostic.client.config.Config;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * This class handles connection logic
 *
 */
public class SkyscapeConnector {

	private Logger logger = LogManager.getLogger(SkyscapeConnector.class);
	
	@Inject @Config
	private String url;
	
	@Inject @Config
	private String user;
	
	@Inject @Config
	private String organisation;
	
	@Inject @Config
	private String password;
	
	@Inject @Config
	private String proxyHost;
	
	@Inject @Config
	private Integer proxyPort;
	
	@Inject @Config
	private String proxyScheme;
	
	/**
	 * Makes a connection to the cloud and returns a client.
	 * @param config
	 * @return
	 * @throws VCloudException
	 * @throws ConnectionException 
	 */
	public VcloudClient connect() throws VCloudException, ConnectionException {
		VcloudClient vCloudClient = login(url, user + "@" + organisation, password);
		return vCloudClient;
	}
	
	/**
	 * Log in to the cloud and return a client.
	 * The client will be attached to a shutdownHook so it will shut itself down
	 * @param vCloudURL
	 * @param username
	 * @param password
	 * @return
	 * @throws VCloudException
	 */
	private VcloudClient login(String vCloudURL, String username, String password) throws VCloudException, ConnectionException {		
		logger.debug("login start");

		//TODO: Override the "password" with that from your own Password Manager here
		
		VcloudClient.setLogLevel(Level.OFF);
				
		final VcloudClient vCloudClient = new VcloudClient(vCloudURL, Version.V5_5);
		try {
			if (proxyHost != null || proxyPort != null || proxyScheme != null) {
				logger.info(String.format("Found some proxy parameters proxyHost:'%s' proxyPort:'%s' proxyScheme:'%s'",proxyHost,proxyPort,proxyScheme));
				boolean proxyUsed = false;
				if (proxyHost != null && proxyHost.trim().length() > 0) {
					if (proxyPort != null) {
						if ( proxyScheme != null && proxyScheme.trim().length() > 0) {
					       vCloudClient.setProxy(proxyHost, proxyPort, proxyScheme);
					       proxyUsed = true;
						}
					}
				}
				if (!proxyUsed) {
					logger.info("Not all proxy parameters provided were able to be used.  WILL NOT USE ANY OF THE PROXY SETTINGS.");
				}
			}
			
			
			vCloudClient.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
			vCloudClient.login(username, password);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				
				public void run() {
					try {
						if (vCloudClient != null ) {
							logger.debug("Closing VCloud connection");
							vCloudClient.logout();
							logger.debug("VCloud connection closed");
						}
					} catch (Throwable e) {
						logger.error("Unable to close connection to VCloud.",e);
					}
				}
				
			});
			
		} catch (KeyManagementException e) {
    		logger.error("KeyManagementException", e);
    		handleConnectionError(e);
		} catch (UnrecoverableKeyException e) {
			logger.error("UnrecoverableKeyException", e);
			handleConnectionError(e);
		} catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException", e);
			handleConnectionError(e);
		} catch (KeyStoreException e) {
			logger.error("KeyStoreException", e);
			handleConnectionError(e);
		} catch (Exception e){
			handleConnectionError(e);
		}
		logger.debug("login end");
		return vCloudClient;
	}


	public void logout(VcloudClient vCloudClient) {
		try {
			if (vCloudClient != null ) {
				logger.debug("Closing VCloud connection");
				vCloudClient.logout();
				logger.debug("VCloud connection closed");
			}
		} catch (Throwable e) {
			logger.error("Unable to close connection to VCloud.",e);
		}
	}
	
	
	/**
	 * 
	 * Method to log and re-throw Exception caught during a log-in or -out operation as a new ConnectionException.
	 * 
	 * @param oe - Original Exception
	 * @throws ConnectionException
	 */
	private void handleConnectionError(Exception oe) throws ConnectionException
	{
		logger.error("Exception thrown during a connection operation. Rethrowing as a ConnectException to be handled by the calling process.", oe);
		throw new ConnectionException("Exception thrown during a connection operation.", oe);
	}
	
}
