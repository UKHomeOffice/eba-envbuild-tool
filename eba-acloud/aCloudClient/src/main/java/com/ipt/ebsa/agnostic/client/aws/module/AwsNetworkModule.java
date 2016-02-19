package com.ipt.ebsa.agnostic.client.aws.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.AcceptVpcPeeringConnectionRequest;
import com.amazonaws.services.ec2.model.AcceptVpcPeeringConnectionResult;
import com.amazonaws.services.ec2.model.AssignPrivateIpAddressesRequest;
import com.amazonaws.services.ec2.model.AttachNetworkInterfaceRequest;
import com.amazonaws.services.ec2.model.CreateNetworkInterfaceRequest;
import com.amazonaws.services.ec2.model.CreateNetworkInterfaceResult;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.CreateSubnetResult;
import com.amazonaws.services.ec2.model.CreateVpcPeeringConnectionRequest;
import com.amazonaws.services.ec2.model.CreateVpcPeeringConnectionResult;
import com.amazonaws.services.ec2.model.DeleteNetworkInterfaceRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DeleteVpcPeeringConnectionRequest;
import com.amazonaws.services.ec2.model.DeleteVpcPeeringConnectionResult;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfaceAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfaceAttributeResult;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesRequest;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcPeeringConnectionsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcPeeringConnectionsResult;
import com.amazonaws.services.ec2.model.DetachNetworkInterfaceRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.ModifyNetworkInterfaceAttributeRequest;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.NetworkInterfaceAttachment;
import com.amazonaws.services.ec2.model.NetworkInterfaceAttachmentChanges;
import com.amazonaws.services.ec2.model.NetworkInterfaceStatus;
import com.amazonaws.services.ec2.model.PrivateIpAddressSpecification;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.UnassignPrivateIpAddressesRequest;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ec2.model.VpcPeeringConnection;
import com.ipt.ebsa.agnostic.client.aws.exception.PeeringChoreographyException;
import com.ipt.ebsa.agnostic.client.aws.exception.SubnetUnavailableException;
import com.ipt.ebsa.agnostic.client.aws.exception.VpcUnavailableException;
import com.ipt.ebsa.agnostic.client.aws.extensions.IptNetworkInterface;
import com.ipt.ebsa.agnostic.client.aws.extensions.VpcPeeringConnectionStatus;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.Retryable;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.client.exception.ResourceInUseException;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.logging.LogUtils;
import com.ipt.ebsa.agnostic.client.logging.LogUtils.LogAction;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler;
import com.ipt.ebsa.agnostic.client.util.MandatoryCheck;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLApplicationNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.jcabi.aspects.Loggable;

/**
 * Module for managing networks in aws
 * 
 *
 */
@Loggable(prepend = true)
public class AwsNetworkModule extends AwsModule {

	private Logger logger = LogManager.getLogger(AwsNetworkModule.class);

	@Inject
	AwsSecurityGroupModule securityGroupModule;

	@Inject
	AwsGatewayModule gatewayModule;

	@Inject
	StrategyHandler strategyHandler;

	public void createOrganisationSubnet(XMLEnvironmentType env, XMLOrganisationalNetworkType orgNetworkConfig) throws InterruptedException,
			PeeringChoreographyException, ToManyResultsException, UnSafeOperationException {
		LogUtils.log(LogAction.CREATING, "Organisation Subnet", orgNetworkConfig, "name");
		Vpc environmentVpc = getVpc(env);
		String availabilityZone = null;
		if (orgNetworkConfig.getDataCenterId() != null) {
			availabilityZone = orgNetworkConfig.getDataCenterId().getName();
		}
		Subnet created = createSubnet(availabilityZone, orgNetworkConfig.getCIDR(), environmentVpc.getVpcId(), orgNetworkConfig, null);
		String groupId = securityGroupModule.createSecurityGroup(AwsNamingUtil.getNetworkName(orgNetworkConfig), environmentVpc.getVpcId(),
				"Organisational network " + AwsNamingUtil.getNetworkName(orgNetworkConfig) + " security group", null);
		securityGroupModule.addIngressPorts(groupId, AwsNamingUtil.getNetworkName(orgNetworkConfig), "0.0.0.0/0", "-1", -1, -1);
		//securityGroupModule.addIngressPorts(groupId,AwsNamingUtil.getNetworkName(orgNetworkConfig),orgNetworkConfig.getCIDR(), "-1", -1,-1);
		//securityGroupModule.addEgressPorts(groupId,AwsNamingUtil.getNetworkName(orgNetworkConfig),orgNetworkConfig.getCIDR(), "-1", -1,-1);
		ArrayList<Tag> tags = new ArrayList<Tag>();
		if (StringUtils.isNotBlank(orgNetworkConfig.getPeerEnvironmentName()) && StringUtils.isNotBlank(orgNetworkConfig.getPeerNetworkName())) {
			StringBuilder peerConnections = new StringBuilder();
			int index = 0;
			Vpc remoteVpc = getVpcByName(orgNetworkConfig.getPeerEnvironmentName());
			if (remoteVpc != null) {
				securityGroupModule.addIngressPorts(groupId, AwsNamingUtil.getNetworkName(orgNetworkConfig), remoteVpc.getCidrBlock(), "-1", -1, -1);
			}
			tags.add(new Tag("peer", orgNetworkConfig.getPeerEnvironmentName() + ":" + orgNetworkConfig.getPeerNetworkName()));
		}

		createTags(tags, groupId);
	}

	public Subnet getSubnet(String vpcId, XMLNetworkType networkConfig) throws InterruptedException {
		return getSubnet(vpcId, AwsNamingUtil.getNetworkName(networkConfig));
	}

	public Subnet getSubnet(String vpcId, String networkName) throws InterruptedException {
		LogUtils.log(LogAction.GETTING, "Subnet", networkName);
		DescribeSubnetsRequest subnet = new DescribeSubnetsRequest();
		Collection<Filter> filters = new ArrayList<Filter>(2);
		filters.add(getNameFilter(networkName));
		filters.add(getVpcFilter(vpcId));
		subnet.setFilters(filters);
		DescribeSubnetsResult results = cv.getEC2Client().describeSubnets(subnet);
		Subnet snet = null;
		if (results.getSubnets().size() > 0) {
			snet = results.getSubnets().get(0);
		}
		return snet;
	}

	public Subnet getSubnet(XMLEnvironmentType env, XMLNetworkType network) {
		List<Subnet> subnets = getSubnet(env, network, getVpc(env));
		if (subnets.size() > 0) {
			return subnets.get(0);
		} else {
			return null;
		}
	}

	public void confirmSubnet(CmdStrategy strategy, XMLEnvironmentType env, XMLNetworkType networkConfig) throws InterruptedException,
			StrategyFailureException, InvalidStrategyException, VpcUnavailableException {
		Vpc vpc = getVpc(env);
		if (vpc == null) {
			throw new VpcUnavailableException(String.format("Vpc %s was not found when it was looked up.", AwsNamingUtil.getEnvironmentName(env)));
		}
		Boolean exists = getSubnet(env, networkConfig, vpc).size() > 0;
		strategyHandler.resolveConfirmStrategy(strategy, exists, "Network", AwsNamingUtil.getNetworkName(networkConfig), " confirm");
	}

	public void createApplicationSubnet(XMLEnvironmentType env, XMLApplicationNetworkType appNetworkType, XMLVirtualMachineContainerType vmc)
			throws InterruptedException, PeeringChoreographyException, ToManyResultsException, UnSafeOperationException {
		Vpc environmentVpc = getVpc(env);

		String availabilityZone = null;
		if (appNetworkType.getDataCenterId() != null) {
			availabilityZone = appNetworkType.getDataCenterId().getName();
		}

		createSubnet(availabilityZone, appNetworkType.getCIDR(), environmentVpc.getVpcId(), appNetworkType, vmc);
		if (securityGroupModule.getSecurityGroups(env, appNetworkType, environmentVpc).size() == 0) {
			String groupId = securityGroupModule.createSecurityGroup(AwsNamingUtil.getNetworkName(appNetworkType), environmentVpc.getVpcId(),
					"Application network " + AwsNamingUtil.getNetworkName(appNetworkType) + " security group", vmc);
			securityGroupModule.addIngressPorts(groupId, AwsNamingUtil.getNetworkName(appNetworkType), "0.0.0.0/0", "-1", -1, -1);
			// securityGroupModule.addIngressPorts(cv.getEC2Client(), groupId,
			// AwsNamingUtil.getNetworkName(appNetworkType),
			// appNetworkType.getCIDR(), "-1", -1,
			// -1);
			// securityGroupModule.addIngressPorts(cv.getClient(), groupId,
			// AwsNamingUtil.getNetworkName(appNetworkType),
			// appNetworkType.getCIDR(), "tcp", 0,
			// 65535);
			// securityGroupModule.addIngressPorts(cv.getClient(), groupId,
			// AwsNamingUtil.getNetworkName(appNetworkType),
			// appNetworkType.getCIDR(), "icmp", 8,
			// -1);
			createTags(AwsNamingUtil.getNetworkName(appNetworkType), groupId, vmc);
		}
	}

	private void performPeerChoreography(String peerEnvironmentNameIn, String peerNetworkNameIn, String localVpcId, Subnet localSubnet,
			XMLNetworkType localNetwork) throws PeeringChoreographyException, ToManyResultsException, InterruptedException, UnSafeOperationException {
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC_PEER_ENVIRONMENT, peerEnvironmentNameIn);
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC_PEER_NETWORK, peerNetworkNameIn);
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, localVpcId);
		MandatoryCheck.checkNotNull(MandatoryCheck.SUBNET, localSubnet);
		MandatoryCheck.checkNotNull(MandatoryCheck.SUBNET, localNetwork);
		
		String peerEnvironmentName = AwsNamingUtil.getPeerEnvironmentName(peerEnvironmentNameIn);
		String peerNetworkName = AwsNamingUtil.getPeerNetworkName(peerNetworkNameIn);
		// Get other vpc
		List<Vpc> vpcs = getAllVpc(peerEnvironmentName, true);
		if (vpcs.size() > 1) {
			throw new ToManyResultsException(vpcs);
		} else if (vpcs.size() == 0) {
			LogUtils.log(LogAction.PEERING_FAILED, "Remote VPC unavailable", peerEnvironmentName);
			return;
		}
		Vpc peerToVpc = vpcs.get(0);
		Vpc localVpc = getVpc(localVpcId);

		// Get subnets
		Subnet remoteSubnet = getSubnet(peerToVpc.getVpcId(), peerNetworkName);

		if (remoteSubnet == null) {
			LogUtils.log(LogAction.PEERING_FAILED, "VpcPeeringConnection state", VpcPeeringConnectionStatus.REMOTE_SUBNET_UNAVAILABLE, "value");
			throw new PeeringChoreographyException(peerEnvironmentName, peerNetworkName, VpcPeeringConnectionStatus.REMOTE_SUBNET_UNAVAILABLE);
		}

		// Create peer request
		VpcPeeringConnection peerConnection = createPeeringRequest(localVpcId, peerToVpc.getVpcId(), peerEnvironmentName, peerNetworkName,
				localNetwork);

		// Create subnet routing tables
		RouteTable remoteTable = gatewayModule.getSubnetRouteTable(remoteSubnet, peerToVpc);
		if(remoteTable == null) {
			remoteTable = gatewayModule.createRouteTable(peerToVpc.getVpcId(), remoteSubnet);
			gatewayModule.updateRouteTable(remoteTable, peerConnection.getVpcPeeringConnectionId(), localVpc.getCidrBlock(), remoteSubnet);
			gatewayModule.associateRouteTable(remoteSubnet, remoteTable.getRouteTableId());
		} else {
			try {
				gatewayModule.updateRouteTable(remoteTable, peerConnection.getVpcPeeringConnectionId(), localVpc.getCidrBlock(), remoteSubnet);
			} catch(Exception e) {
				logger.error("Got exception while trying to update the root table",e);
			}
		}
		
		RouteTable localTable = gatewayModule.getSubnetRouteTable(localSubnet, localVpc);
		if(localTable == null) {
			localTable = gatewayModule.createRouteTable(localVpcId, localSubnet);
			gatewayModule.updateRouteTable(localTable, peerConnection.getVpcPeeringConnectionId(), peerToVpc.getCidrBlock(), localSubnet);
			gatewayModule.associateRouteTable(localSubnet, localTable.getRouteTableId());
		}else {
			try {
				gatewayModule.updateRouteTable(localTable, peerConnection.getVpcPeeringConnectionId(), peerToVpc.getCidrBlock(), localSubnet);
			} catch(Exception e) {
				logger.error("Got exception while trying to update the root table",e);
			}
		}

		// Accept peer request
		List<VpcPeeringConnection> peers = getPeeringConnection(peerConnection.getVpcPeeringConnectionId()).getVpcPeeringConnections();
		for (VpcPeeringConnection peer : peers) {
			String statusCode = peer.getStatus().getCode();
			LogUtils.log(LogAction.PEERING, "VpcPeeringConnection state", peer.getStatus(), "processing");
			switch (VpcPeeringConnectionStatus.fromValue(statusCode)) {
			case INITIALISING:
				// Need to wait for pending
			case PENDING_ACCEPTANCE:
				waitForPeeringStatus(peer.getVpcPeeringConnectionId(), VpcPeeringConnectionStatus.PENDING_ACCEPTANCE, false);
				// need to accept connection
				acceptPeeringRequest(peer.getVpcPeeringConnectionId());
			case PROVISIONING:
				// Setting up, check to ensure peering was successful
			case ACTIVE:
				waitForPeeringStatus(peer.getVpcPeeringConnectionId(), VpcPeeringConnectionStatus.ACTIVE, false);
				// setup, nothing to do
				break;
			case DELETED:
				// state failed
			case EXPIRED:
				// state failed
			case FAILED:
				// state failed
			case REJECTED:
				// state failed
			default:
				LogUtils.log(LogAction.PEERING, "VpcPeeringConnection state failure", peer.getStatus(), "failed");
				throw new PeeringChoreographyException(peerEnvironmentName, peerNetworkName, VpcPeeringConnectionStatus.fromValue(peer.getStatus()
						.getCode()));
			}
			VpcPeeringConnectionStatus resultStatus = getPeeringStatus(peer.getVpcPeeringConnectionId());
			if (resultStatus.getCode() >= VpcPeeringConnectionStatus.FAILED.getCode()) {
				throw new PeeringChoreographyException(peerEnvironmentName, peerNetworkName, resultStatus);
			}
		}

	}

	public VpcPeeringConnection createPeeringRequest(String localVpcId, String peerVpcId, String peerEnvironmentName, String peerNetworkName,
			XMLNetworkType localNetwork) {
		
		final CreateVpcPeeringConnectionRequest request = new CreateVpcPeeringConnectionRequest();
		request.setPeerVpcId(peerVpcId);
		request.setVpcId(localVpcId);
		CreateVpcPeeringConnectionResult result = AwsRetryManager.run(new Retryable<CreateVpcPeeringConnectionResult>() {
			@Override
			public CreateVpcPeeringConnectionResult run() {
				return cv.getEC2Client().createVpcPeeringConnection(request);
			}
		});
		Collection<Tag> tags = new ArrayList<Tag>();
		tags.add(new Tag("Name", AwsNamingUtil.getNetworkName(localNetwork) + ":to:" + peerEnvironmentName));
		tags.add(new Tag("localSubnet", AwsNamingUtil.getNetworkName(localNetwork)));
		tags.add(new Tag("remoteSubnet", peerNetworkName));
		createTags(tags, result.getVpcPeeringConnection().getVpcPeeringConnectionId());
		return result.getVpcPeeringConnection();
	}

	public List<VpcPeeringConnection> getVpcPeerConnections(String accepterVpcId, String requesterVpcId) {
		DescribeVpcPeeringConnectionsRequest request = new DescribeVpcPeeringConnectionsRequest();
		Collection<Filter> filters = new ArrayList<Filter>();
		if (StringUtils.isNotBlank(accepterVpcId)) {
			filters.add(new Filter().withName("accepter-vpc-info.vpc-id").withValues(accepterVpcId));
		}
		if (StringUtils.isNotBlank(requesterVpcId)) {
			filters.add(new Filter().withName("requester-vpc-info.vpc-id").withValues(requesterVpcId));
		}
		request.setFilters(filters);
		DescribeVpcPeeringConnectionsResult result = cv.getEC2Client().describeVpcPeeringConnections(request);
		return result.getVpcPeeringConnections();
	}

	public Boolean deleteVpcPeerConnection(String vpcPeeringConnectionId) {
		final DeleteVpcPeeringConnectionRequest request = new DeleteVpcPeeringConnectionRequest();
		request.setVpcPeeringConnectionId(vpcPeeringConnectionId);

		DeleteVpcPeeringConnectionResult result = AwsRetryManager.run(new Retryable<DeleteVpcPeeringConnectionResult>() {
			@Override
			public DeleteVpcPeeringConnectionResult run() {
				return cv.getEC2Client().deleteVpcPeeringConnection(request);
			}
		});
		return result.getReturn();
	}

	public VpcPeeringConnection acceptPeeringRequest(String vpcPeeringConnectionId) {
		final AcceptVpcPeeringConnectionRequest request = new AcceptVpcPeeringConnectionRequest();
		request.setVpcPeeringConnectionId(vpcPeeringConnectionId);

		AcceptVpcPeeringConnectionResult result = AwsRetryManager.run(new Retryable<AcceptVpcPeeringConnectionResult>() {
			@Override
			public AcceptVpcPeeringConnectionResult run() {
				return cv.getEC2Client().acceptVpcPeeringConnection(request);
			}
		});
		return result.getVpcPeeringConnection();
	}

	private Subnet createSubnet(String availabilityZone, String cidrBlock, String vpcId, XMLNetworkType type, XMLVirtualMachineContainerType vmc)
			throws InterruptedException, PeeringChoreographyException, ToManyResultsException, UnSafeOperationException {
		final CreateSubnetRequest createSubnetRequest = new CreateSubnetRequest();
		if (availabilityZone != null) {
			createSubnetRequest.setAvailabilityZone(availabilityZone);
		}
		createSubnetRequest.setCidrBlock(cidrBlock);
		createSubnetRequest.setVpcId(vpcId);
		logger.debug(ReflectionToStringBuilder.toString(createSubnetRequest));
		LogUtils.log(LogAction.CREATING, "Subnet", createSubnetRequest, "vpcId", "cidrBlock", "availabilityZone");

		CreateSubnetResult result = AwsRetryManager.run(new Retryable<CreateSubnetResult>() {
			@Override
			public CreateSubnetResult run() {
				return cv.getEC2Client().createSubnet(createSubnetRequest);
			}
		});
		createTags(AwsNamingUtil.getNetworkName(type), result.getSubnet().getSubnetId(), vmc);
		LogUtils.log(LogAction.CREATED, "Subnet", result.getSubnet(), "subnetId", "vpcId", "cidrBlock");
		ArrayList<Tag> tags = new ArrayList<Tag>();

		if (type instanceof XMLOrganisationalNetworkType) {

			XMLOrganisationalNetworkType orgNetworkConfig = (XMLOrganisationalNetworkType) type;
			if (StringUtils.isNoneBlank(orgNetworkConfig.getPeerEnvironmentName()) && StringUtils.isNoneBlank(orgNetworkConfig.getPeerNetworkName())) {
				StringBuilder peerConnections = new StringBuilder();
				peerConnections.append(orgNetworkConfig.getPeerEnvironmentName());
				peerConnections.append(":");
				peerConnections.append(orgNetworkConfig.getPeerNetworkName());
				performPeerChoreography(orgNetworkConfig.getPeerEnvironmentName(), orgNetworkConfig.getPeerNetworkName(), vpcId, result.getSubnet(),
						type);

				tags.add(new Tag("peer", peerConnections.toString()));
			}
		}
		createTags(AwsNamingUtil.getNetworkName(type), result.getSubnet().getSubnetId(), vmc, tags);

		return result.getSubnet();
	}

	public IptNetworkInterface createNic(XMLNICType nic, XMLVirtualMachineContainerType vmc, XMLEnvironmentType env,
			XMLVirtualMachineType virtualMachine) throws ResourceInUseException, InterruptedException, UnSafeOperationException,
			SubnetUnavailableException {
		Vpc vpc = getVpc(env);
		List<PrivateIpAddressSpecification> ipSpec = createPrivateIpAddressSpecification(nic.getInterface(), virtualMachine, vpc);
		
		Collection<String> securityGroupIds = new ArrayList<String>();
		Collection<String> securityGroupIdsVapp = securityGroupModule.getSecurityGroups(env, vmc, vpc);
		logger.debug("Adding " + securityGroupIdsVapp.size() + " security groups from vapp");
		securityGroupIds.addAll(securityGroupIdsVapp);
		Collection<String> securityGroupIdsSubnet = null;
		Collection<String> securityGroupIdsGateway = null;

		XMLNetworkType subnet = null;

		if (nic.getNetworkID() != null) {
			subnet = (XMLNetworkType) nic.getNetworkID();
			securityGroupIdsSubnet = securityGroupModule.getSecurityGroups(env, subnet, vpc);
			logger.debug("Adding " + securityGroupIdsSubnet.size() + " security groups from subnet");
			securityGroupIds.addAll(securityGroupIdsSubnet);
		}

		if (subnet instanceof XMLOrganisationalNetworkType) {
			Object gateway = ((XMLOrganisationalNetworkType) subnet).getGatewayId();
			if (gateway != null) {
				XMLGatewayType gatewayConfig = ((XMLGatewayType) gateway);
				securityGroupIdsGateway = securityGroupModule.getSecurityGroups(env, gatewayConfig, vpc);
				logger.debug("Adding " + securityGroupIdsGateway.size() + " security groups from gateway");
				securityGroupIds.addAll(securityGroupIdsGateway);
			}
		}

		List<Subnet> subnets = getSubnet(env, subnet, vpc);
		if (subnets.size() == 0) {
			throw new SubnetUnavailableException("Could not locate subnet while creating nic ", subnet, nic);
		}

		Subnet locatedSubnet = subnets.get(0);
		LogUtils.log(LogAction.CREATING, "nic on subnet", subnet, "name", "CIDR");
		IptNetworkInterface nInterface = createNIC(securityGroupIds, ipSpec, locatedSubnet.getSubnetId(), nic, vpc, vmc, virtualMachine);
		nInterface.setDeviceIndex(nic.getIndexNumber().intValue());
		waitForNicStatus(nInterface.getNetworkInterfaceId(), NetworkInterfaceStatus.Available, false);
		modifyNicSourceDestinationCheck(nInterface.getNetworkInterfaceId(), false);
		return nInterface;
	}

	public void attachNicToInstance(IptNetworkInterface createdNic, Instance vm) {
		final AttachNetworkInterfaceRequest attach = new AttachNetworkInterfaceRequest();
		attach.setDeviceIndex(createdNic.getDeviceIndex());
		attach.setNetworkInterfaceId(createdNic.getNetworkInterfaceId());
		attach.setInstanceId(vm.getInstanceId());
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().attachNetworkInterface(attach);
				return null;
			}
		});

	}

	public void deleteSecondaryIp(String networkInterfaceId, Collection<String> privateIpAddresses) {
		final UnassignPrivateIpAddressesRequest request = new UnassignPrivateIpAddressesRequest();
		request.setNetworkInterfaceId(networkInterfaceId);
		request.setPrivateIpAddresses(privateIpAddresses);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().unassignPrivateIpAddresses(request);
				return null;
			}
		});

	}

	public void addSeccondaryIp(String networkInterfaceId, Collection<String> privateIpAddresses) {
		final AssignPrivateIpAddressesRequest request = new AssignPrivateIpAddressesRequest();
		request.setNetworkInterfaceId(networkInterfaceId);
		request.setPrivateIpAddresses(privateIpAddresses);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().assignPrivateIpAddresses(request);
				return null;
			}
		});

	}

	public List<PrivateIpAddressSpecification> createPrivateIpAddressSpecification(List<XMLInterfaceType> list, XMLVirtualMachineType virtualMachine, Vpc vpc) {
		int ipIndex = 0;
		for (XMLInterfaceType ip : list) {
			LogUtils.log(LogAction.CREATING, "IP specification item index " + ipIndex, ip, "name", "staticIpAddress", "interfaceNumber", "VRRP");
			ipIndex++;
		}
		List<PrivateIpAddressSpecification> ips = new ArrayList<PrivateIpAddressSpecification>();

		int index = 1;
		for (XMLInterfaceType iface : list) {
			PrivateIpAddressSpecification ipSpec = new PrivateIpAddressSpecification();
			if (index == 1) {
				ipSpec.setPrimary(true);
			} else {
				ipSpec.setPrimary(false);
			}
			index++;
			ipSpec.setPrivateIpAddress(iface.getStaticIpAddress());

			if (iface.isIsVip()) {
				NetworkInterface nic = getNetworkInterfaceByPrivateIpAddress(iface.getStaticIpAddress(), vpc);
				if (nic == null) {
					logger.debug(String.format("Adding vip ip %s for vm %s",iface.getStaticIpAddress(),virtualMachine.getComputerName()));
					ips.add(ipSpec);
				}
			} else {
				ips.add(ipSpec);
			}
		}
		return ips;
	}

	public IptNetworkInterface createNIC(Collection<String> groupIds, List<PrivateIpAddressSpecification> privateIpAddresses, String subnetId,
			XMLNICType nic2, Vpc vpc, XMLVirtualMachineContainerType vapp, XMLVirtualMachineType vm) throws ResourceInUseException, InterruptedException,
			UnSafeOperationException {
		final CreateNetworkInterfaceRequest createNetworkInterfaceRequest = new CreateNetworkInterfaceRequest();
		String name = AwsNamingUtil.getNicName(nic2, vapp);
		StringBuilder sb = new StringBuilder();
		Set<String> vips = new HashSet<String>();

		int index = 1;
		for (XMLInterfaceType iface : nic2.getInterface()) {
			sb.append("SI:");
			sb.append(AwsNamingUtil.getInterfaceName(iface));
			if (nic2.getInterface().size() < index) {
				sb.append(",");
			}
			index++;

			if (iface.isIsVip()) {
				vips.add(AwsNamingUtil.getVipNameFromVm(vm, vapp));
			}
		}
		//Trim the description to a max of 255 characters (Amazon exception workaround)
		
		String description = sb.toString();
		
		if (description.length() >= 254)
		{
			description = description.substring(0, 253);
		}
		
		createNetworkInterfaceRequest.setDescription(description);
		createNetworkInterfaceRequest.setGroups(groupIds);
		createNetworkInterfaceRequest.setPrivateIpAddresses(privateIpAddresses);
		createNetworkInterfaceRequest.setRequestCredentials(cv.getCredentials());
		createNetworkInterfaceRequest.setSubnetId(subnetId);

		logger.debug(ReflectionToStringBuilder.toString(createNetworkInterfaceRequest));
		NetworkInterface newNic = null;
		try {
			CreateNetworkInterfaceResult result = AwsRetryManager.run(new Retryable<CreateNetworkInterfaceResult>() {
				@Override
				public CreateNetworkInterfaceResult run() {
					CreateNetworkInterfaceResult result = null;
					return cv.getEC2Client().createNetworkInterface(createNetworkInterfaceRequest);
				}
			});
			newNic = result.getNetworkInterface();
		} catch (com.amazonaws.AmazonServiceException e) {
			logger.error("error creating nic ", e);
			if (e.getErrorCode().equals("InvalidIPAddress.InUse") && e.getMessage().startsWith("The specified address is already in use")) {
				NetworkInterface nic = getNetworkInterfaceByPrivateIpAddress(privateIpAddresses.get(0).getPrivateIpAddress(), vpc);
				NetworkInterfaceStatus status = NetworkInterfaceStatus.fromValue(nic.getStatus());
				switch (status) {
				case Available:
					newNic = nic;
					break;
				case Detaching:
					waitForNicStatus(nic.getNetworkInterfaceId(), NetworkInterfaceStatus.Available, false);
					newNic = nic;
					break;
				case Attaching:
				case InUse:
					throw new ResourceInUseException(String.format("Network Interface %s with ip %s is %s by instance %s",
							nic.getNetworkInterfaceId(), privateIpAddresses.get(0), status, nic.getAttachment().getInstanceId()), nic, e);
				}

			} else {
				logger.error(e);
				throw e;
			}

		}

		createTags(name, newNic.getNetworkInterfaceId(), vapp);

		IptNetworkInterface nic = new IptNetworkInterface(newNic);
		nic.setDeviceIndex(nic2.getIndexNumber().intValue());
		nic.setPrimary(nic2.isPrimary());
		nic.setVips(vips);
		String nicGroups = "";
		for (GroupIdentifier gi : nic.getGroups()) {
			nicGroups = nicGroups + ";" + gi.getGroupName();
		}
		LogUtils.log(LogAction.CREATED, "NIC", "in security group " + nicGroups, nic, "networkInterfaceId", "subnetId", "privateIpAddress");
		return nic;
	}

	public void detachNetworkInterface(String attachmentId) {
		final DetachNetworkInterfaceRequest detach = new DetachNetworkInterfaceRequest();
		detach.setAttachmentId(attachmentId);
		LogUtils.log(LogAction.DETACHING, "Network Interface", attachmentId);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().detachNetworkInterface(detach);
				return null;
			}
		});

		LogUtils.log(LogAction.DETACHED, "Network Interface", attachmentId);
	}

	public void deleteNetworkInterface(String networkInterfaceId) {
		final DeleteNetworkInterfaceRequest delete = new DeleteNetworkInterfaceRequest();
		delete.setNetworkInterfaceId(networkInterfaceId);
		LogUtils.log(LogAction.DELETING, "Network Interface", networkInterfaceId);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				try {
					cv.getEC2Client().deleteNetworkInterface(delete);
				} catch (AmazonServiceException e) {
					if (!e.getErrorCode().equals("InvalidNetworkInterfaceID.NotFound")) {
						throw e;
					}

				}
				return null;
			}
		});

		LogUtils.log(LogAction.DELETED, "Network Interface", networkInterfaceId);
	}

	public void deleteSubnet(Subnet subnet) {
		final DeleteSubnetRequest delete = new DeleteSubnetRequest();
		delete.setSubnetId(subnet.getSubnetId());
		LogUtils.log(LogAction.DELETING, "Subnet", subnet, "subnetId");
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().deleteSubnet(delete);
				return null;
			}
		});

		LogUtils.log(LogAction.DELETED, "Subnet", subnet, "subnetId");
	}

	/**
	 * Deletes a subnet and its associated security groups
	 * 
	 * @param env
	 * @param networkConfig
	 * @throws VpcUnavailableException
	 * @throws ToManyResultsException
	 */
	public void deleteSubnet(XMLEnvironmentType env, XMLNetworkType networkConfig) throws VpcUnavailableException, ToManyResultsException {
		Vpc vpc = getVpc(env);
		
		if (vpc == null) {
			throw new VpcUnavailableException(String.format("Vpc %s was not found when it was looked up.", AwsNamingUtil.getEnvironmentName(env)));
		}
		List<Subnet> subnets = getSubnet(env, networkConfig, vpc);
		if (subnets.size() > 1) {
			throw new ToManyResultsException(subnets);
		} 
		
		List<SecurityGroup> subnetSecurityGroups = securityGroupModule.getSecurityGroups("tag:Name", AwsNamingUtil.getNetworkName(networkConfig), vpc);
		for(SecurityGroup sg:subnetSecurityGroups) {
			securityGroupModule.deleteSecurityGroup(sg);
		}
		for(Subnet subnet :subnets) {
			deleteSubnet(subnet);
		}
	}

	public void modifyNicDeleteOnTerminate(InstanceNetworkInterface nic, boolean deleteOnTerminate) {
		final ModifyNetworkInterfaceAttributeRequest request = new ModifyNetworkInterfaceAttributeRequest();
		request.setNetworkInterfaceId(nic.getNetworkInterfaceId());
		NetworkInterfaceAttachmentChanges attachment = new NetworkInterfaceAttachmentChanges();
		attachment.setAttachmentId(nic.getAttachment().getAttachmentId());
		attachment.setDeleteOnTermination(deleteOnTerminate);
		request.setAttachment(attachment);
		logger.debug("Making NIC " + nic.getNetworkInterfaceId() + " delete on terminate " + deleteOnTerminate);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().modifyNetworkInterfaceAttribute(request);
				return null;
			}
		});

	}

	public void modifyNicSourceDestinationCheck(String networkInterfaceId, boolean enabled) {
		final ModifyNetworkInterfaceAttributeRequest request = new ModifyNetworkInterfaceAttributeRequest();
		request.setNetworkInterfaceId(networkInterfaceId);
		request.setSourceDestCheck(enabled);
		logger.debug("Making NIC " + networkInterfaceId + " source/destination check enabled " + enabled);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().modifyNetworkInterfaceAttribute(request);
				return null;
			}
		});

	}

	public List<NetworkInterface> getVpcNetworkInterfaces(Vpc vpc) {
		final DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
		Collection<Filter> filters = new ArrayList<Filter>();
		filters.add(getVpcFilter(vpc.getVpcId()));
		request.setFilters(filters);
		LogUtils.log(LogAction.GETTING, "Network Interfaces", "vpcId=" + vpc.getVpcId());

		DescribeNetworkInterfacesResult result = AwsRetryManager.run(new Retryable<DescribeNetworkInterfacesResult>() {
			@Override
			public DescribeNetworkInterfacesResult run() {
				return cv.getEC2Client().describeNetworkInterfaces(request);
			}
		});

		return result.getNetworkInterfaces();
	}

	public NetworkInterfaceAttachment getNetworkInterfaceAttribute(String networkInterfaceId) {
		final DescribeNetworkInterfaceAttributeRequest request = new DescribeNetworkInterfaceAttributeRequest();
		request.setNetworkInterfaceId(networkInterfaceId);
		LogUtils.log(LogAction.GETTING, "Network Interface Attributes", networkInterfaceId);
		DescribeNetworkInterfaceAttributeResult result = AwsRetryManager.run(new Retryable<DescribeNetworkInterfaceAttributeResult>() {
			@Override
			public DescribeNetworkInterfaceAttributeResult run() {
				return cv.getEC2Client().describeNetworkInterfaceAttribute(request);
			}
		});

		return result.getAttachment();
	}
}
