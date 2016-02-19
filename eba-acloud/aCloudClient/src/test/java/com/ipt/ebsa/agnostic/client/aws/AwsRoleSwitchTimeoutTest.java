package com.ipt.ebsa.agnostic.client.aws;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;

public class AwsRoleSwitchTimeoutTest extends AwsBaseTest {

	private static Logger logger = LogManager.getLogger(AwsRoleSwitchTimeoutTest.class);
	
	@Test
	public void timeoutAssumedRole() throws Exception {
		AWSCredentials credentials = cv.getCredentials();
		DateTime start = new DateTime();

		do {
			DescribeAvailabilityZonesResult result = cv.getEC2Client().describeAvailabilityZones();
			Assert.assertTrue(result.getAvailabilityZones().size() == 3);
			Thread.sleep(5000);
			Assert.assertTrue(cv.getIAMClient().listRoles().getRoles().size() > 0);
			Thread.sleep(5000);
			logger.debug("Loop complete");
		} while (isTimedOut(start) || !credentialsChanged(credentials, cv.getCredentials()));

		AWSCredentials credFinish = cv.getCredentials();
		Assert.assertNotEquals(credentials.getAWSAccessKeyId(), credFinish.getAWSAccessKeyId());
		Assert.assertNotEquals(credentials.getAWSSecretKey(), credFinish.getAWSSecretKey());
	}
	
	public boolean isTimedOut(DateTime start) {
		Period timeout = new Period().withMinutes(61);
		return start.plus(timeout).isBeforeNow();
	}
	
	public boolean credentialsChanged(AWSCredentials credentials1, AWSCredentials credentials2) {
		if((!credentials1.getAWSAccessKeyId().equals(credentials2.getAWSAccessKeyId())) || (!credentials1.getAWSSecretKey().equals(credentials2.getAWSSecretKey()))) {
			logger.info("Credentials changed");
			return true;
		}
		return false;
	}
	
}
