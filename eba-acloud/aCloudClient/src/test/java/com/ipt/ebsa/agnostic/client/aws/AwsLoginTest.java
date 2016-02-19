package com.ipt.ebsa.agnostic.client.aws;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.ipt.ebsa.agnostic.client.BaseTest;
import com.ipt.ebsa.agnostic.client.aws.connection.AwsCloudValues;
import com.ipt.ebsa.agnostic.client.aws.connection.AwsConnector;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.Retryable;
import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;
import com.jcabi.aspects.Loggable;

/**
 * Aws Login Test
 * 
 * Tests the config is good for loggin into the aws account that is configured.
 * 
 *
 */
@Loggable(prepend = true)
public class AwsLoginTest extends BaseTest {

	private static Logger logger = LogManager.getLogger(AwsLoginTest.class);

	public void setUpBeforeAwsLoginTestClass() throws InterruptedException {
		logger.debug("Setting up configuration file");
		if (System.getProperty("user.name").contains("jenkins")) {
			Properties overrides = new Properties();
			try {
				overrides.load(new FileInputStream(AwsBaseTest.jenkinsOverridesFile));
			} catch (Exception e) {
				logger.error("Unable to load overrides from file " + AwsBaseTest.jenkinsOverridesFile, e);
				fail("Unable to load Jenkins overrides");
			}
			for (Entry<Object, Object> override : overrides.entrySet()) {
				ConfigurationFactory.getProperties().setProperty((String)override.getKey(), (String)override.getValue());
			}
		} else {
			ConfigurationFactory.setConfigFile(new File(AwsBaseTest.configFile));
		}
	}
	
	/**
	 * Basic login test
	 * @throws InterruptedException 
	 */
	@Test
	public void login() throws InterruptedException {
		setUpBeforeAwsLoginTestClass();
		Weld weld = new Weld();
		WeldContainer container = weld.initialize();
		AwsCloudValues cloudValues = container.instance().select(AwsCloudValues.class).get();
		AmazonEC2Client client = cloudValues.getEC2Client();
		Assert.assertTrue(client instanceof AmazonEC2Client);
		Assert.assertNotNull(client);
		String response = client.getServiceName();
		Assert.assertNotNull(response);
	}
	
	/**
	 * Basic login test
	 */
	@Test
	public void loginIam() throws InterruptedException {
		setUpBeforeAwsLoginTestClass();
		Weld weld = new Weld();
		WeldContainer container = weld.initialize();
		final AwsCloudValues cloudValues = container.instance().select(AwsCloudValues.class).get();
		
		AmazonIdentityManagementClient client =
		    	AwsRetryManager.run(new Retryable<AmazonIdentityManagementClient>() {
		    		@Override
		    		public AmazonIdentityManagementClient run() {
		    			return cloudValues.getIAMClient();
		    		}
		    	});
		Assert.assertTrue(client instanceof AmazonIdentityManagementClient);
		Assert.assertNotNull(client);
		String response = cloudValues.getIamUsername();
		logger.debug("Iam username "+response);
		Assert.assertNotNull(response);
	}

	//@Test
	public void loginWithUserAndPassword() {
		Weld weld = new Weld();
		WeldContainer container = weld.initialize();
		ConfigurationFactory.getProperties().put("user", "dummy1");
		ConfigurationFactory.getProperties().put("password", "dummy2");
		AwsConnector connector = container.instance().select(AwsConnector.class).get();
		AmazonEC2Client client = connector.connectEc2();
		Assert.assertNotNull(client);
		Assert.assertTrue(client instanceof AmazonEC2Client);
		String response = client.getServiceName();
		Assert.assertNotNull(response);
		AWSCredentials credentials = connector.getCredentials();
		Assert.assertNotNull(credentials);
		Assert.assertEquals("dummy1", credentials.getAWSAccessKeyId());
		Assert.assertEquals("dummy2", credentials.getAWSSecretKey());
	}
	
	//@Test
	public void loginWithAssumedRole() throws InterruptedException {
		setUpBeforeAwsLoginTestClass();
		Weld weld = new Weld();
		WeldContainer container = weld.initialize();
		AwsConnector connector = container.instance().select(AwsConnector.class).get();
		AmazonEC2Client ec2 = connector.connectEc2();
		AWSCredentials credentials = connector.getCredentials();
		Assert.assertNotNull(credentials);
		//Check we have connected to the service
		Assert.assertNotNull(ec2);
		Assert.assertTrue(ec2 instanceof AmazonEC2Client);
		Assert.assertEquals("ec2",ec2.getServiceName());
		
		//Check Iam connection
		AmazonIdentityManagementClient iam = connector.connectIam();
		Assert.assertEquals("iam",iam.getServiceName());
		
		//Check the long term creds are different from the token creds
		AWSCredentials credPersistant = connector.getLongTermCredentials(null, null);
		Assert.assertNotEquals(credentials.getAWSAccessKeyId(), credPersistant.getAWSAccessKeyId());
		Assert.assertNotEquals(credentials.getAWSSecretKey(), credPersistant.getAWSSecretKey());
		
		//Refresh the tokens and check they are new
		connector.refreshProvider();
		AWSCredentials credRefresh = connector.getCredentials();
		Assert.assertNotEquals(credRefresh.getAWSAccessKeyId(), credentials.getAWSAccessKeyId());
		Assert.assertNotEquals(credRefresh.getAWSSecretKey(), credentials.getAWSSecretKey());
		
		ec2.shutdown();
		iam.shutdown();
		ec2 = connector.connectEc2();
		iam = connector.connectIam();
		
		AWSCredentials reloginCreds = connector.getCredentials();
		Assert.assertEquals(credRefresh.getAWSAccessKeyId(), reloginCreds.getAWSAccessKeyId());
		Assert.assertEquals(credRefresh.getAWSSecretKey(), reloginCreds.getAWSSecretKey());
		Assert.assertEquals("ec2",ec2.getServiceName());
		Assert.assertEquals("iam",iam.getServiceName());
	}

	@After
	public void tareDown() {
		ConfigurationFactory.resetProperties();
	}

}
