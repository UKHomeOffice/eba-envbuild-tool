package com.ipt.ebsa.agnostic.client.aws;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ipt.ebsa.agnostic.client.aws.module.AwsEnvironmentModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsRoleModule;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.jcabi.aspects.Loggable;

/**
 * 
 *
 */
@Loggable(prepend=true)
public class AwsCleanupTest extends AwsBaseTest {

	private static Logger logger = LogManager.getLogger(AwsCleanupTest.class);
	
	@Inject
	AwsRoleModule roleModule;
	
	@BeforeClass
	public static void setUpBeforeAwsBaseTestClass() throws InterruptedException {
		logger.info("Stopped base setup to allow cleanup of VPC's");
		resetBaseTestConfig(testPrefixIdent,testPrefixIdentAdditionalVpc);
	}
	
	@Test
	public void cleanup() throws ToManyResultsException, InterruptedException, UnSafeOperationException {
		if(envModule == null) {
			container = weld.initialize();
			envModule = container.instance().select(AwsEnvironmentModule.class).get();
		}
		envModule.deleteAllVpc("UNITTEST");
		
//		Role r = roleModule.getRole("001_UNITTEST_ROLE");
//		if (r != null) {
//			roleModule.deleteRolePolicyForHa("001_UNITTEST_ROLE");
//		}
		//String vpcId = "vpc-a931adcc";
		//envModule.deleteVpcById(vpcId,"");
	}

}
