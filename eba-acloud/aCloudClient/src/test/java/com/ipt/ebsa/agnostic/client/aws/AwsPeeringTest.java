package com.ipt.ebsa.agnostic.client.aws;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ec2.model.VpcPeeringConnection;
import com.ipt.ebsa.agnostic.client.aws.exception.PeeringChoreographyException;
import com.ipt.ebsa.agnostic.client.aws.extensions.VpcPeeringConnectionStatus;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;

public class AwsPeeringTest extends AwsBaseTest {
	
	@Test
	public void testPeerCreation() throws InterruptedException, PeeringChoreographyException, ToManyResultsException, UnSafeOperationException {
		//XMLEnvironmentType localVpc = getBaseTestEnvironment(false, testPrefixIdent);
		networkModule.createOrganisationSubnet(environment, orgNetwork1);
		
		orgNetwork1.setPeerEnvironmentName(AwsNamingUtil.getEnvironmentName(environmentAdditionalVpc));
		orgNetwork1.setPeerNetworkName(AwsNamingUtil.getOrganisationalNetworkName(orgNetwork2));
		
		orgNetwork2.setPeerEnvironmentName(AwsNamingUtil.getEnvironmentName(environment));
		orgNetwork2.setPeerNetworkName(AwsNamingUtil.getOrganisationalNetworkName(orgNetwork1));
		
		orgNetwork4.setPeerEnvironmentName(AwsNamingUtil.getEnvironmentName(environment));
		orgNetwork4.setPeerNetworkName(AwsNamingUtil.getOrganisationalNetworkName(orgNetwork1));
		
		XMLEnvironmentType remoteVpcConfig = getBaseTestEnvironment2(true, testPrefixIdentAdditionalVpc);
		Vpc remoteVpc = envModule.createVpc(remoteVpcConfig);
		testAdditionalVpcId = remoteVpc.getVpcId();
		networkModule.createOrganisationSubnet(environmentAdditionalVpc, orgNetwork2);
		networkModule.createOrganisationSubnet(environmentAdditionalVpc, orgNetwork4);
		List<VpcPeeringConnection> peers = networkModule.getVpcPeerConnections(testVpcId, remoteVpc.getVpcId());
		Assert.assertEquals(1, peers.size());
		for(VpcPeeringConnection peer : peers) {
			Assert.assertTrue(networkModule.deleteVpcPeerConnection(peer.getVpcPeeringConnectionId()));
			networkModule.waitForPeeringStatus(peer.getVpcPeeringConnectionId(), VpcPeeringConnectionStatus.DELETED, false);
		}
		List<VpcPeeringConnection> peersConfirm = networkModule.getVpcPeerConnections(testVpcId, remoteVpc.getVpcId());
		Assert.assertEquals(1, peersConfirm.size());
		for(VpcPeeringConnection peer : peersConfirm) {
			Assert.assertEquals(VpcPeeringConnectionStatus.DELETED, VpcPeeringConnectionStatus.fromValue(peer.getStatus().getCode()));
		}
	}

}
