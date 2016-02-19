package com.ipt.ebsa.agnostic.client.aws;

/**
 * 
 *
 */
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
   AwsLoginTest.class,
   AwsNetworkTest.class,
   AwsRolePolicyTest.class,
   AwsVirtualMachineContainerTest.class,
   AwsPeeringTest.class,
   AwsVirtualMachineTest.class,
   AwsCleanupTest.class
})
public class AwsTestSuite {

	public AwsTestSuite() {
	}

}
