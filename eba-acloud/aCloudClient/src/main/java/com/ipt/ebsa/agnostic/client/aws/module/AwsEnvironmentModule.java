package com.ipt.ebsa.agnostic.client.aws.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.AttachmentStatus;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.CreateVpcResult;
import com.amazonaws.services.ec2.model.DeleteVpcRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceAssociation;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.InternetGatewayAttachment;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.NetworkInterfaceAssociation;
import com.amazonaws.services.ec2.model.NetworkInterfaceStatus;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RouteTableAssociation;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ec2.model.VpcPeeringConnection;
import com.amazonaws.services.identitymanagement.model.Role;
import com.ipt.ebsa.agnostic.client.aws.exception.VpcUnavailableException;
import com.ipt.ebsa.agnostic.client.aws.extensions.VpcPeeringConnectionStatus;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.Retryable;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.logging.LogUtils;
import com.ipt.ebsa.agnostic.client.logging.LogUtils.LogAction;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler;
import com.ipt.ebsa.agnostic.client.util.MandatoryCheck;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.jcabi.aspects.Loggable;

/**
 * Module for controlling the top level environment types
 * 
 *
 */
@Loggable(prepend = true)
public class AwsEnvironmentModule extends AwsModule {

	private Logger logger = LogManager.getLogger(AwsEnvironmentModule.class);

	@Inject
	AwsVmModule vmModule;

	@Inject
	AwsVmContainerModule vmcModule;

	@Inject
	AwsNetworkModule netModule;

	@Inject
	AwsRoleModule roleModule;

	@Inject
	AwsGatewayModule gatewayModule;

	@Inject
	private StrategyHandler strategyHandler;

	public Vpc createVpc(XMLEnvironmentType env) throws InterruptedException {

		String vpcCidrBlock = env.getEnvironmentDefinition().get(0).getCidr();// "10.16.0.0/16";
		final CreateVpcRequest createVpcRequest = new CreateVpcRequest();
		createVpcRequest.setCidrBlock(vpcCidrBlock);
		LogUtils.log(LogAction.CREATING, "Vpc", env, "name");
		CreateVpcResult result = AwsRetryManager.run(new Retryable<CreateVpcResult>() {
			@Override
			public CreateVpcResult run() {
				return cv.getEC2Client().createVpc(createVpcRequest);
			}
		});

		LogUtils.log(LogAction.CREATED, "Vpc", "Vpc id=" + result.getVpc().getVpcId(), env, "name");

		createTag(AwsNamingUtil.getEnvironmentName(env), result.getVpc().getVpcId());

		return result.getVpc();
	}

	public void deleteVpc(final XMLEnvironmentType env, final String prefix) throws ToManyResultsException, InterruptedException,
			UnSafeOperationException {
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				try {
					deleteVpcs(env, true, prefix);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

	}

	private void deleteVpcs(XMLEnvironmentType env, boolean exactMatch, String prefix) throws ToManyResultsException, InterruptedException,
			UnSafeOperationException {
		MandatoryCheck.checkNotNull(MandatoryCheck.ENVIRONMENT, env);
		List<Vpc> deleteVpcs = getAllVpc(env, exactMatch);
		if (deleteVpcs != null) {
			logger.info(String.format("Found %s VPC's to delete", deleteVpcs.size()));
			for (Vpc deleteVpc : deleteVpcs) {
				logger.info(String.format("Deleting VPC %s %s", deleteVpc.getVpcId(), ReflectionToStringBuilder.toString(deleteVpc.getTags())));

				List<VpcPeeringConnection> acceptorPeers = netModule.getVpcPeerConnections(deleteVpc.getVpcId(), null);
				List<VpcPeeringConnection> requestorPeers = netModule.getVpcPeerConnections(null, deleteVpc.getVpcId());
				List<VpcPeeringConnection> peers = new ArrayList<VpcPeeringConnection>(acceptorPeers);
				peers.addAll(requestorPeers);

				for (VpcPeeringConnection peer : peers) {
					if(deleteVpc.getVpcId().equals(peer.getRequesterVpcInfo().getVpcId()) || deleteVpc.getVpcId().equals(peer.getAccepterVpcInfo().getVpcId())) {
						logger.debug(String.format("Deleting peer connection %s between requester vpcid %s and acceptor vpcid %s",peer.getVpcPeeringConnectionId(), peer.getRequesterVpcInfo().getVpcId(), peer.getAccepterVpcInfo().getVpcId())); 
						netModule.deleteVpcPeerConnection(peer.getVpcPeeringConnectionId());
						netModule.waitForPeeringStatus(peer.getVpcPeeringConnectionId(), VpcPeeringConnectionStatus.DELETED, false);
					} else {
						logger.warn("Caught vpc peer connection that should not be deleted "+peer.getVpcPeeringConnectionId());
					}
				}

				List<Instance> instances = getInstances(deleteVpc);
				
				if (instances != null) {
					List<InstanceNetworkInterface> nics = new ArrayList<InstanceNetworkInterface>();
					for (Instance vm : instances) {
						nics.addAll(vm.getNetworkInterfaces());
					}

					for (InstanceNetworkInterface nic : nics) {
						netModule.modifyNicDeleteOnTerminate(nic, true);
						InstanceNetworkInterfaceAssociation elasticAssociation = nic.getAssociation();
						if (elasticAssociation != null) {
							// Disassociate address
							// Release Elastic ip
							gatewayModule.disassociateAddress(elasticAssociation.getPublicIp());
							gatewayModule.releaseAddress(elasticAssociation.getPublicIp());
						}
					}

					for (Instance vm : instances) {
						vmModule.deleteVirtualMachine(vm);
						nics.addAll(vm.getNetworkInterfaces());
					}

					int counterLimit = 100;
					int deleteInstanceCounter = 0;
					while (getInstances(deleteVpc).size() != 0 && deleteInstanceCounter != counterLimit) {
						// Wait
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						deleteInstanceCounter++;
					}
				}

				List<NetworkInterface> vpcNics = netModule.getVpcNetworkInterfaces(deleteVpc);
				for (NetworkInterface nic : vpcNics) {
					NetworkInterfaceAssociation elasticAssociation = nic.getAssociation();
					if (elasticAssociation != null) {
						// Dissassociate address
						// Release Elastic ip
						gatewayModule.disassociateAddress(elasticAssociation.getPublicIp());
						gatewayModule.releaseAddress(elasticAssociation.getPublicIp());
					}

					try {
						waitForNicStatus(nic.getNetworkInterfaceId(), NetworkInterfaceStatus.Available, false);
						netModule.deleteNetworkInterface(nic.getNetworkInterfaceId());
					} catch (AmazonServiceException e) {
						if (!e.getErrorCode().equals("InvalidNetworkInterfaceID.NotFound")) {
							throw e;
						}

					}
				}

				List<Subnet> subnets = getSubnets(deleteVpc);
				if (subnets != null) {
					for (Subnet subnet : subnets) {
						List<RouteTable> tables = gatewayModule.getRouteTables(subnet.getSubnetId());
						for (RouteTable table : tables) {
							List<RouteTableAssociation> associations = table.getAssociations();
							for (RouteTableAssociation association : associations) {
								gatewayModule.disassociateRouteTable(association.getRouteTableAssociationId(), association.getRouteTableId(),
										association.getSubnetId());
								gatewayModule.deleteRouteTable(association.getRouteTableAssociationId(), association.getRouteTableId(),
										association.getSubnetId());
							}
						}

						int tryCount = 1;
						while (tryCount <= 10) {
							try {
								logger.debug("Deleteing subnets, loop=" + tryCount);
								netModule.deleteSubnet(subnet);
								tryCount = 11;
							} catch (AmazonServiceException e) {

							}
							tryCount++;
						}
					}
				}

				List<SecurityGroup> vmcs = getVmcs(deleteVpc);
				if (vmcs != null) {
					for (SecurityGroup vmc : vmcs) {
						if (!vmc.getGroupName().equals("default")) {
							vmcModule.deleteVmc(vmc);
						}
					}
				}

				List<InternetGateway> gateways = gatewayModule.getEdgeGateways(deleteVpc, exactMatch, prefix);
				if (gateways != null) {
					for (InternetGateway gateway : gateways) {
						for (InternetGatewayAttachment attachment : gateway.getAttachments()) {
							if (attachment.getVpcId().equals(deleteVpc.getVpcId())) {
								gatewayModule.detachInternetGatewayFromVpc(gateway, deleteVpc);
								gatewayModule.deleteInternetGatewayFromVpc(gateway);
							} else if (attachment.getState().equals(AttachmentStatus.Detached.toString())) {
								gatewayModule.deleteInternetGatewayFromVpc(gateway);
							}

						}
					}
				}

				final DeleteVpcRequest request = new DeleteVpcRequest();
				request.setVpcId(deleteVpc.getVpcId());

				AwsRetryManager.run(new Retryable<Void>() {
					@Override
					public Void run() {
						cv.getEC2Client().deleteVpc(request);
						return null;
					}
				});
			}
		}
	}

	private void deleteVpcs(String vpcId, String prefix) throws ToManyResultsException, InterruptedException, UnSafeOperationException {
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC_ID, vpcId);
		boolean exactMatch = true;
		final DescribeVpcsRequest request = new DescribeVpcsRequest();
		Collection<Filter> filters = new ArrayList<Filter>(2);
		filters.add(getVpcFilter(vpcId));
		request.setFilters(filters);
		DescribeVpcsResult result = AwsRetryManager.run(new Retryable<DescribeVpcsResult>() {
			@Override
			public DescribeVpcsResult run() {
				return cv.getEC2Client().describeVpcs(request);
			}
		});

		List<Vpc> deleteVpcs = result.getVpcs();
		if (deleteVpcs != null) {
			logger.info(String.format("Found %s VPC's to delete", deleteVpcs.size()));
			for (Vpc deleteVpc : deleteVpcs) {
				logger.info(String.format("Deleting VPC %s %s", deleteVpc.getVpcId(), ReflectionToStringBuilder.toString(deleteVpc.getTags())));

				List<VpcPeeringConnection> acceptorPeers = netModule.getVpcPeerConnections(deleteVpc.getVpcId(), null);
				List<VpcPeeringConnection> requestorPeers = netModule.getVpcPeerConnections(null, deleteVpc.getVpcId());
				List<VpcPeeringConnection> peers = new ArrayList<VpcPeeringConnection>(acceptorPeers);
				peers.addAll(requestorPeers);

				for (VpcPeeringConnection peer : peers) {
					if(deleteVpc.getVpcId().equals(peer.getRequesterVpcInfo().getVpcId()) || deleteVpc.getVpcId().equals(peer.getAccepterVpcInfo().getVpcId())) {
						logger.debug(String.format("Deleting peer connection %s between requester vpcid %s and acceptor vpcid %s",peer.getVpcPeeringConnectionId(), peer.getRequesterVpcInfo().getVpcId(), peer.getAccepterVpcInfo().getVpcId())); 
						netModule.deleteVpcPeerConnection(peer.getVpcPeeringConnectionId());
						netModule.waitForPeeringStatus(peer.getVpcPeeringConnectionId(), VpcPeeringConnectionStatus.DELETED, false);
					} else {
						logger.debug("Caught vpc peer connection that should not be deleted "+peer.getVpcPeeringConnectionId());
					}
				}

				List<Instance> instances = getInstances(deleteVpc);
				if (instances != null) {
					List<InstanceNetworkInterface> nics = new ArrayList<InstanceNetworkInterface>();
					for (Instance vm : instances) {
						nics.addAll(vm.getNetworkInterfaces());
					}

					for (InstanceNetworkInterface nic : nics) {
						netModule.modifyNicDeleteOnTerminate(nic, true);
						InstanceNetworkInterfaceAssociation elasticAssociation = nic.getAssociation();
						if (elasticAssociation != null) {
							// Disassociate address
							// Release Elastic ip
							gatewayModule.disassociateAddress(elasticAssociation.getPublicIp());
							gatewayModule.releaseAddress(elasticAssociation.getPublicIp());
						}
					}

					for (Instance vm : instances) {
						vmModule.deleteVirtualMachine(vm);
						nics.addAll(vm.getNetworkInterfaces());
					}

					int counterLimit = 100;
					int deleteInstanceCounter = 0;
					while (getInstances(deleteVpc).size() != 0 && deleteInstanceCounter != counterLimit) {
						// Wait
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						deleteInstanceCounter++;
					}
				}

				List<NetworkInterface> vpcNics = netModule.getVpcNetworkInterfaces(deleteVpc);
				for (NetworkInterface nic : vpcNics) {
					NetworkInterfaceAssociation elasticAssociation = nic.getAssociation();
					if (elasticAssociation != null) {
						// Dissassociate address
						// Release Elastic ip
						gatewayModule.disassociateAddress(elasticAssociation.getPublicIp());
						gatewayModule.releaseAddress(elasticAssociation.getPublicIp());
					}

					try {
						waitForNicStatus(nic.getNetworkInterfaceId(), NetworkInterfaceStatus.Available, false);
						netModule.deleteNetworkInterface(nic.getNetworkInterfaceId());
					} catch (AmazonServiceException e) {
						if (!e.getErrorCode().equals("InvalidNetworkInterfaceID.NotFound")) {
							throw e;
						}

					}
				}

				List<Subnet> subnets = getSubnets(deleteVpc);
				if (subnets != null) {
					for (Subnet subnet : subnets) {
						List<RouteTable> tables = gatewayModule.getRouteTables(subnet.getSubnetId());
						for (RouteTable table : tables) {
							List<RouteTableAssociation> associations = table.getAssociations();
							for (RouteTableAssociation association : associations) {
								gatewayModule.disassociateRouteTable(association.getRouteTableAssociationId(), association.getRouteTableId(),
										association.getSubnetId());
								gatewayModule.deleteRouteTable(association.getRouteTableAssociationId(), association.getRouteTableId(),
										association.getSubnetId());
							}
						}

						int tryCount = 1;
						while (tryCount <= 10) {
							try {
								logger.debug("Deleteing subnets, loop=" + tryCount);
								netModule.deleteSubnet(subnet);
								tryCount = 11;
							} catch (AmazonServiceException e) {

							}
							tryCount++;
						}
					}
				}

				List<SecurityGroup> vmcs = getVmcs(deleteVpc);
				if (vmcs != null) {
					for (SecurityGroup vmc : vmcs) {
						if (!vmc.getGroupName().equals("default")) {
							vmcModule.deleteVmc(vmc);
						}
					}
				}

				List<InternetGateway> gateways = gatewayModule.getEdgeGateways(deleteVpc, exactMatch, prefix);
				if (gateways != null) {
					for (InternetGateway gateway : gateways) {
						for (InternetGatewayAttachment attachment : gateway.getAttachments()) {
							if (attachment.getVpcId().equals(deleteVpc.getVpcId())) {
								gatewayModule.detachInternetGatewayFromVpc(gateway, deleteVpc);
								gatewayModule.deleteInternetGatewayFromVpc(gateway);
							} else if (attachment.getState().equals(AttachmentStatus.Detached.toString())) {
								gatewayModule.deleteInternetGatewayFromVpc(gateway);
							}

						}
					}
				}

				final DeleteVpcRequest request2 = new DeleteVpcRequest();
				request2.setVpcId(deleteVpc.getVpcId());

				AwsRetryManager.run(new Retryable<Void>() {
					@Override
					public Void run() {
						cv.getEC2Client().deleteVpc(request2);
						return null;
					}
				});

			}
		}
	}

	public void deleteAllVpc(String prefix) throws ToManyResultsException, InterruptedException, UnSafeOperationException {
		XMLEnvironmentType removeAllUnitVpc = new XMLEnvironmentType();
		removeAllUnitVpc.setName(prefix);
		deleteVpcs(removeAllUnitVpc, false, prefix);
	}

	public void confirmVpc(CmdStrategy strategy, XMLEnvironmentType env) throws InterruptedException, StrategyFailureException,
			InvalidStrategyException, ToManyResultsException, VpcUnavailableException {
		Vpc vpc = getVpc(env);
		strategyHandler.resolveConfirmStrategy(strategy, vpc, "Vpc", vpc != null ? vpc.getVpcId():"No VPC Found", " confirm");
	}

	public void deleteVpcById(String vpcId, String prefix) throws ToManyResultsException, InterruptedException, UnSafeOperationException {
		if (StringUtils.isNotBlank(vpcId)) {
			deleteVpcs(vpcId, prefix);
		}

	}
}
