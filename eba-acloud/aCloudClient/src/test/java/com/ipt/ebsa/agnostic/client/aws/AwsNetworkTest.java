package com.ipt.ebsa.agnostic.client.aws;

import java.util.Collection;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.ec2.model.Subnet;
import com.ipt.ebsa.agnostic.client.aws.exception.PeeringChoreographyException;
import com.ipt.ebsa.agnostic.client.aws.exception.VpcUnavailableException;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;

public class AwsNetworkTest extends AwsBaseTest {

	private static Logger logger = LogManager.getLogger(AwsNetworkTest.class);

	@BeforeClass
	public static void setUpAwsNetworkTestClass() throws InterruptedException {
		resetBaseTestConfig(testPrefixIdent, testPrefixIdentAdditionalVpc);
		vmcModule.createVirtualMachineContainer(environment, vmc);
	}

	@AfterClass
	public static void tearDownAfterAwsVirtualMachineTestClass() throws ToManyResultsException, InterruptedException, UnSafeOperationException {
		envModule.deleteVpc(environment, "UNITTEST");
	}
	
	@Test
	public void testCreateDeleteAppSubnet() throws VpcUnavailableException, ToManyResultsException, InterruptedException, PeeringChoreographyException, UnSafeOperationException {
		networkModule.createApplicationSubnet(environment, appNet1, vmc);
		Subnet appSubnetCreated = networkModule.getSubnet(testVpcId, appNet1);
		Assert.assertTrue(appSubnetCreated != null);
		Collection<String> appNetSecuityGroupsCreated = securityGroupModule.getSecurityGroups(environment, appNet1, baseVpc);
		Assert.assertTrue(appNetSecuityGroupsCreated.size() == 1);
		networkModule.deleteSubnet(environment, appNet1);
		
		Subnet appSubnetDelete = networkModule.getSubnet(testVpcId, appNet1);
		Assert.assertTrue(appSubnetDelete == null);
		Collection<String> appNetSecuityGroupsDeleted = securityGroupModule.getSecurityGroups(environment, appNet1, baseVpc);
		Assert.assertTrue(appNetSecuityGroupsDeleted.size() == 0);
	}
	
	@Test
	public void testCreateDeleteOrgSubnet() throws VpcUnavailableException, ToManyResultsException, InterruptedException, PeeringChoreographyException, UnSafeOperationException {
		networkModule.createOrganisationSubnet(environment, orgNetwork1);
		Subnet orgSubnetCreated = networkModule.getSubnet(testVpcId, orgNetwork1);
		Assert.assertTrue(orgSubnetCreated != null);
		Collection<String> orgNetSecuityGroupsCreated = securityGroupModule.getSecurityGroups(environment, orgNetwork1, baseVpc);
		Assert.assertTrue(orgNetSecuityGroupsCreated.size() == 1);
		networkModule.deleteSubnet(environment, orgNetwork1);
		
		Subnet orgSubnetDelete = networkModule.getSubnet(testVpcId, orgNetwork1);
		Assert.assertTrue(orgSubnetDelete == null);
		Collection<String> orgNetSecuityGroupsDeleted = securityGroupModule.getSecurityGroups(environment, orgNetwork1, baseVpc);
		Assert.assertTrue(orgNetSecuityGroupsDeleted.size() == 0);
	}
}
