package com.ipt.ebsa.agnostic.client.aws.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AssociateRouteTableRequest;
import com.amazonaws.services.ec2.model.AssociateRouteTableResult;
import com.amazonaws.services.ec2.model.AttachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.CreateInternetGatewayRequest;
import com.amazonaws.services.ec2.model.CreateInternetGatewayResult;
import com.amazonaws.services.ec2.model.CreateRouteRequest;
import com.amazonaws.services.ec2.model.CreateRouteResult;
import com.amazonaws.services.ec2.model.CreateRouteTableRequest;
import com.amazonaws.services.ec2.model.CreateRouteTableResult;
import com.amazonaws.services.ec2.model.DeleteInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DeleteRouteTableRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysResult;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.DetachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.DisassociateRouteTableRequest;
import com.amazonaws.services.ec2.model.DomainType;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.InternetGatewayAttachment;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.Route;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Vpc;
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
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.jcabi.aspects.Loggable;

/**
 * This modules controls the Internet gateway objects
 * 
 *
 */
@Loggable(prepend = true)
public class AwsGatewayModule extends AwsModule {

	private Logger logger = LogManager.getLogger(AwsGatewayModule.class);

	@Inject
	AwsSecurityGroupModule securityGroupModule;

	@Inject
	private StrategyHandler strategyHandler;

	public InternetGateway createGateway(XMLEnvironmentType env, XMLGatewayType gatewayConfig) throws InterruptedException {
		final CreateInternetGatewayRequest gateway = new CreateInternetGatewayRequest();

		CreateInternetGatewayResult result = AwsRetryManager.run(new Retryable<CreateInternetGatewayResult>() {
			@Override
			public CreateInternetGatewayResult run() {
				return cv.getEC2Client().createInternetGateway(gateway);
			}
		});
		String internetGatewayId = result.getInternetGateway().getInternetGatewayId();
		LogUtils.log(LogAction.CREATED, "Internet Gateway", "id=" + internetGatewayId);

		createTag(AwsNamingUtil.getGatewayName(gatewayConfig), internetGatewayId);
		Vpc environment = getVpc(env);

		LogUtils.log(LogAction.ATTACHING, "Internet Gateway", "to vpcId=" + environment.getVpcId(), env, "name");
		final AttachInternetGatewayRequest attachInternetGatewayRequest = new AttachInternetGatewayRequest();
		attachInternetGatewayRequest.setVpcId(environment.getVpcId());
		attachInternetGatewayRequest.setInternetGatewayId(internetGatewayId);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().attachInternetGateway(attachInternetGatewayRequest);
				return null;
			}
		});

		LogUtils.log(LogAction.ATTACHED, "Internet Gateway", "to vpcId=" + environment.getVpcId(), env, "name");

		String groupId = securityGroupModule.createSecurityGroupGateway(AwsNamingUtil.getGatewayName(gatewayConfig), environment.getVpcId());

		createTag(AwsNamingUtil.getGatewayName(gatewayConfig), groupId);
		return result.getInternetGateway();
	}

	public InternetGateway updateGateway(XMLEnvironmentType env, XMLGatewayType gatewayConfig) throws InterruptedException,
			ToManyResultsException {
		InternetGateway gateway = getEdgeGateway(env, gatewayConfig);
		String internetGatewayId = gateway.getInternetGatewayId();
		// LogUtils.log(LogAction.CREATED, "Internet Gateway", "id=" +
		// internetGatewayId);

		createTag(AwsNamingUtil.getGatewayName(gatewayConfig), internetGatewayId);
		Vpc environment = getVpc(env);

		boolean attached = false;
		for (InternetGatewayAttachment att : gateway.getAttachments()) {
			if (att.getVpcId().equals(environment.getVpcId())) {
				attached = true;
			}
		}

		if (!attached) {
			LogUtils.log(LogAction.ATTACHING, "Internet Gateway", "to vpcId=" + environment.getVpcId(), env, "name");
			final AttachInternetGatewayRequest attachInternetGatewayRequest = new AttachInternetGatewayRequest();
			attachInternetGatewayRequest.setVpcId(environment.getVpcId());
			attachInternetGatewayRequest.setInternetGatewayId(internetGatewayId);
			AwsRetryManager.run(new Retryable<Void>() {
				@Override
				public Void run() {
					cv.getEC2Client().attachInternetGateway(attachInternetGatewayRequest);
					return null;
				}
			});

			LogUtils.log(LogAction.ATTACHED, "Internet Gateway", "to vpcId=" + environment.getVpcId(), env, "name");
		}

		Collection<String> groups = securityGroupModule.getGatewaySecurityGroup(env, gatewayConfig, environment);
		for (String groupId : groups) {
			securityGroupModule.removeIngressPorts(groupId, AwsNamingUtil.getGatewayName(gatewayConfig), environment.getVpcId());
			securityGroupModule.setDefaultGatewayIngressPorts(groupId, AwsNamingUtil.getGatewayName(gatewayConfig));
		}

		return gateway;
	}

	public InternetGateway getEdgeGateway(XMLEnvironmentType env, XMLGatewayType gatewayConfig) throws ToManyResultsException {
		final DescribeInternetGatewaysRequest request = new DescribeInternetGatewaysRequest();

		Collection<Filter> filters = new ArrayList<Filter>(2);
		Filter vpcFilterType = new Filter();
		vpcFilterType = vpcFilterType.withName("tag:Name").withValues(AwsNamingUtil.getGatewayName(gatewayConfig));
		filters.add(vpcFilterType);

		request.setFilters(filters);

		DescribeInternetGatewaysResult result = AwsRetryManager.run(new Retryable<DescribeInternetGatewaysResult>() {
			@Override
			public DescribeInternetGatewaysResult run() {
				return cv.getEC2Client().describeInternetGateways(request);
			}
		});
		if (result.getInternetGateways().size() > 1) {
			throw new ToManyResultsException(result.getInternetGateways());
		}
		if (!result.getInternetGateways().isEmpty()) {
			InternetGateway instance = result.getInternetGateways().get(0);
			return instance;
		} else {
			return null;
		}
	}

	public void confirmEdgeGateway(CmdStrategy strategy, XMLEnvironmentType env, XMLGatewayType gatewayConfig) throws ToManyResultsException,
			StrategyFailureException, InvalidStrategyException {
		InternetGateway gateway = getEdgeGateway(env, gatewayConfig);
		strategyHandler.resolveConfirmStrategy(strategy, gateway, "Gateway", AwsNamingUtil.getGatewayName(gatewayConfig), " confirm");
	}

	public List<InternetGateway> getEdgeGateways(Vpc vpc, boolean exactMatch, String qualifier) throws ToManyResultsException {
		final DescribeInternetGatewaysRequest request = new DescribeInternetGatewaysRequest();

		DescribeInternetGatewaysResult result = AwsRetryManager.run(new Retryable<DescribeInternetGatewaysResult>() {
			@Override
			public DescribeInternetGatewaysResult run() {
				return cv.getEC2Client().describeInternetGateways(request);
			}
		});
		List<InternetGateway> results = new ArrayList<InternetGateway>();

		for (InternetGateway gateway : result.getInternetGateways()) {
			if (exactMatch) {
				for (InternetGatewayAttachment attachment : gateway.getAttachments()) {
					if (attachment.getVpcId().equals(vpc.getVpcId())) {
						results.add(gateway);
					} else {
						for (Tag tag : gateway.getTags()) {
							if (tag.getKey().equals("Name") && tag.getValue().equals(qualifier)) {
								results.add(gateway);
								break;
							}
						}
					}
				}
			} else {
				for (Tag tag : gateway.getTags()) {
					if (tag.getKey().contains("Name") && tag.getValue().contains(qualifier)) {
						results.add(gateway);
						break;
					}
				}
			}
		}

		return results;
	}

	public void detachInternetGatewayFromVpc(final InternetGateway internetGateway, Vpc vpc) {
		final DetachInternetGatewayRequest request = new DetachInternetGatewayRequest();
		request.setInternetGatewayId(internetGateway.getInternetGatewayId());
		request.setVpcId(vpc.getVpcId());

		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				try {
					cv.getEC2Client().detachInternetGateway(request);
					LogUtils.log(LogAction.DETACHED, "Internet Gateway", internetGateway, "internetGatewayId");
				} catch (AmazonServiceException e) {
					if (!e.getErrorCode().equals("Gateway.NotAttached")) {
						throw e;
					}
				}
				return null;
			}
		});

	}

	public void deleteInternetGatewayFromVpc(InternetGateway internetGateway) {
		final DeleteInternetGatewayRequest request = new DeleteInternetGatewayRequest();
		request.setInternetGatewayId(internetGateway.getInternetGatewayId());
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().deleteInternetGateway(request);
				return null;
			}
		});

		LogUtils.log(LogAction.DELETED, "Internet Gateway", internetGateway, "internetGatewayId");
	}

	public void deleteGateway(XMLEnvironmentType env, XMLGatewayType gatewayConfig) throws ToManyResultsException {
		InternetGateway gateway = getEdgeGateway(env, gatewayConfig);
		if (gateway != null) {
			detachInternetGatewayFromVpc(gateway, getVpc(env));
			deleteInternetGatewayFromVpc(gateway);
		}
	}

	public RouteTable updateRouteTable(XMLEnvironmentType env, XMLGatewayType gatewayConfig, InternetGateway internetGateway,
			String destinationCidrBlock) {
		RouteTable mainRouteTable = getMainRouteTable(env);
		// add internet gateway route to main route table
		final CreateRouteRequest createIGWRouteRequest = new CreateRouteRequest();
		createIGWRouteRequest.setDestinationCidrBlock(destinationCidrBlock);
		createIGWRouteRequest.setGatewayId(internetGateway.getInternetGatewayId());
		createIGWRouteRequest.setRouteTableId(mainRouteTable.getRouteTableId());
		boolean ignore = false;
		for (Route r : mainRouteTable.getRoutes()) {
			if (r.getDestinationCidrBlock().equals(destinationCidrBlock) && r.getGatewayId().equals(internetGateway.getInternetGatewayId())) {
				ignore = true;
			}
		}

		if (!ignore) {
			AwsRetryManager.run(new Retryable<Void>() {
				@Override
				public Void run() {
					cv.getEC2Client().createRoute(createIGWRouteRequest);
					return null;
				}
			});

		} else {
			LogUtils.log(LogAction.IGNORING, "Internet Gateway Route existed", internetGateway.getInternetGatewayId() + " to main route table: "
					+ mainRouteTable.getRouteTableId());
		}
		createTag(AwsNamingUtil.getEnvironmentName(env) + "_MainRouteTable", mainRouteTable.getRouteTableId());
		LogUtils.log(LogAction.ADDED, "Internet Gateway",
				internetGateway.getInternetGatewayId() + " to main route table: " + mainRouteTable.getRouteTableId());
		return mainRouteTable;
	}

	public void createTagsFromSubnet(Subnet containingSubnet, String resourceId) {
		Collection<Tag> tags = new ArrayList<Tag>();
		for (Tag t : containingSubnet.getTags()) {
			if (t.getKey().equals("Environment")) {
				tags.add(new Tag("Environment", t.getValue()));
			}

			if (t.getKey().equals("Name")) {
				tags.add(new Tag("Name", t.getValue() + "_RouteTable"));
			}
		}

		createTags(tags, resourceId);
	}

	public RouteTable updateRouteTable(RouteTable table, String vpcPeeringConnectionId, String destinationCidrBlock, Subnet containingSubnet) {
		MandatoryCheck.checkCidr(destinationCidrBlock);
		MandatoryCheck.checkNotNull(MandatoryCheck.ROUTE_TABLE, table);
		// add internet gateway route to main route table
		final CreateRouteRequest request = new CreateRouteRequest();
		request.setVpcPeeringConnectionId(vpcPeeringConnectionId);
		request.setRouteTableId(table.getRouteTableId());
		request.setDestinationCidrBlock(destinationCidrBlock);
		LogUtils.log(LogAction.CREATING, "Subnet Route", request, "vpcPeeringConnectionId", "routeTableId", "destinationCidrBlock");

		CreateRouteResult result = AwsRetryManager.run(new Retryable<CreateRouteResult>() {
			@Override
			public CreateRouteResult run() {
				try{
					return cv.getEC2Client().createRoute(request);
				} catch (AmazonServiceException e) {
					if(e.getErrorCode().equals("RouteAlreadyExists")) {
						logger.debug("Route already existed, ",e);
					} else {
						throw e;
					}
					return null;
				}
				
			}
		});
		createTagsFromSubnet(containingSubnet, table.getRouteTableId());
		LogUtils.log(LogAction.CREATED, "Subnet Route");
		return table;
	}

	public RouteTable getSubnetRouteTable(Subnet targetSubnet, Vpc targetVpc) {
		final DescribeRouteTablesRequest request = new DescribeRouteTablesRequest();

		Collection<Filter> filters = new ArrayList<Filter>(2);
		filters.add(getVpcFilter(targetVpc.getVpcId()));
		filters.add(new Filter().withName("association.subnet-id").withValues(targetSubnet.getSubnetId()));
		request.setFilters(filters);

		DescribeRouteTablesResult result = AwsRetryManager.run(new Retryable<DescribeRouteTablesResult>() {
			@Override
			public DescribeRouteTablesResult run() {
				return cv.getEC2Client().describeRouteTables(request);
			}
		});

		List<RouteTable> routeTables = result.getRouteTables();
		if (routeTables.size() > 0) {
			return routeTables.get(0);
		} else {
			return null;
		}
	}

	public RouteTable getMainRouteTable(XMLEnvironmentType env) {
		Vpc vpc = getVpc(env);
		final DescribeRouteTablesRequest request = new DescribeRouteTablesRequest();
		Collection<Filter> filters = new ArrayList<Filter>(2);

		Filter mainFilter = new Filter();
		mainFilter = mainFilter.withName("association.main").withValues("true");
		filters.add(mainFilter);

		Filter vpcFilter = new Filter();
		vpcFilter = vpcFilter.withName("vpc-id").withValues(vpc.getVpcId());
		filters.add(vpcFilter);

		request.setFilters(filters);

		DescribeRouteTablesResult result = AwsRetryManager.run(new Retryable<DescribeRouteTablesResult>() {
			@Override
			public DescribeRouteTablesResult run() {
				return cv.getEC2Client().describeRouteTables(request);
			}
		});
		List<RouteTable> routeTables = result.getRouteTables();
		return routeTables.get(0);
	}

	public List<RouteTable> getRouteTables(String subnetId) {
		final DescribeRouteTablesRequest request = new DescribeRouteTablesRequest();
		Collection<Filter> filters = new ArrayList<Filter>(2);
		filters.add(new Filter().withName("association.subnet-id").withValues(subnetId));
		request.setFilters(filters);

		DescribeRouteTablesResult result = AwsRetryManager.run(new Retryable<DescribeRouteTablesResult>() {
			@Override
			public DescribeRouteTablesResult run() {
				return cv.getEC2Client().describeRouteTables(request);
			}
		});
		return result.getRouteTables();
	}

	public RouteTable createRouteTable(String vpcId, Subnet subnet) {
		final CreateRouteTableRequest request = new CreateRouteTableRequest();
		request.setVpcId(vpcId);
		CreateRouteTableResult result = AwsRetryManager.run(new Retryable<CreateRouteTableResult>() {
			@Override
			public CreateRouteTableResult run() {
				return cv.getEC2Client().createRouteTable(request);
			}
		});
		createTagsFromSubnet(subnet, result.getRouteTable().getRouteTableId());
		LogUtils.log(LogAction.CREATED, "Private Routing Table", result.getRouteTable().getRouteTableId() + " for vpc " + vpcId);
		return result.getRouteTable();
	}

	public void disassociateRouteTable(String associationId, String routeTableId, String subnetId) {
		final DisassociateRouteTableRequest request = new DisassociateRouteTableRequest();
		request.setAssociationId(associationId);
		LogUtils.log(LogAction.DISASSOCIATING, "Private Routing Table", routeTableId + " for subnet " + subnetId + "with association Id"
				+ associationId);

		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().disassociateRouteTable(request);
				return null;
			}
		});
		LogUtils.log(LogAction.DISASSOCIATED, "Private Routing Table", routeTableId + " for subnet " + subnetId + "with association Id"
				+ associationId);
	}

	public void deleteRouteTable(String associationId, String routeTableId, String subnetId) {
		final DeleteRouteTableRequest request = new DeleteRouteTableRequest();
		request.setRouteTableId(routeTableId);
		LogUtils.log(LogAction.DELETING, "Private Routing Table", routeTableId + " for subnet " + subnetId + "with association Id" + associationId);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().deleteRouteTable(request);
				return null;
			}
		});

		LogUtils.log(LogAction.DELETED, "Private Routing Table", routeTableId + " for subnet " + subnetId + "with association Id" + associationId);
	}

	public String associateRouteTable(Subnet subnet, String routeTableId) {
		final AssociateRouteTableRequest request = new AssociateRouteTableRequest();
		request.setRouteTableId(routeTableId);
		request.setSubnetId(subnet.getSubnetId());
		AssociateRouteTableResult result = AwsRetryManager.run(new Retryable<AssociateRouteTableResult>() {
			@Override
			public AssociateRouteTableResult run() {
				return cv.getEC2Client().associateRouteTable(request);
			}
		});
		createTagsFromSubnet(subnet, routeTableId);
		LogUtils.log(LogAction.ASSOCIATED, "Private Routing Table", routeTableId + " for subnet " + subnet);
		return result.getAssociationId();
	}

	public AllocateAddressResult createElasticIPAddress() throws UnSafeOperationException {
		LogUtils.log(LogAction.CREATING, "elastic IP address for public access");
		final AllocateAddressRequest allocateRequest = new AllocateAddressRequest();
		allocateRequest.setDomain(DomainType.Vpc);

		AllocateAddressResult result = AwsRetryManager.run(new Retryable<AllocateAddressResult>() {
			@Override
			public AllocateAddressResult run() {
				return cv.getEC2Client().allocateAddress(allocateRequest);
			}
		});

		LogUtils.log(LogAction.CREATED, "ip address", result, "publicIp");
		return result;
	}

	public void associateElasticIPAddress(String instanceId, String allocationId, String elasticIpAddress, NetworkInterface gatewayNic)
			throws UnSafeOperationException {
		waitForInstanceStatus(instanceId, InstanceStateName.Running, false);
		LogUtils.log(LogAction.ASSOCIATING, "ip address with the running gateway instance", "instanceId=" + instanceId + "Ip address="
				+ elasticIpAddress);
		// Assign the IP to an EC2 instance
		final AssociateAddressRequest associateRequest = new AssociateAddressRequest();
		associateRequest.setAllocationId(allocationId);
		associateRequest.setNetworkInterfaceId(gatewayNic.getNetworkInterfaceId());
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().associateAddress(associateRequest);
				return null;
			}
		});

		LogUtils.log(LogAction.ASSOCIATED, "ip address with the running gateway instance", "instanceId=" + instanceId + "Ip address="
				+ elasticIpAddress);
	}

	public void disassociateAddress(String publicIp) {
		final DisassociateAddressRequest request = new DisassociateAddressRequest();
		request.setPublicIp(publicIp);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().disassociateAddress(request);
				return null;
			}
		});

		LogUtils.log(LogAction.DISASSOCIATED, "ip address", publicIp);
	}

	public void releaseAddress(String publicIp) {
		final DescribeAddressesRequest descRequest = new DescribeAddressesRequest();
		descRequest.withPublicIps(publicIp);

		DescribeAddressesResult result = AwsRetryManager.run(new Retryable<DescribeAddressesResult>() {
			@Override
			public DescribeAddressesResult run() {
				return cv.getEC2Client().describeAddresses(descRequest);
			}
		});
		List<Address> ips = result.getAddresses();
		for (Address ip : ips) {
			final ReleaseAddressRequest request = new ReleaseAddressRequest();
			request.setAllocationId(ip.getAllocationId());
			AwsRetryManager.run(new Retryable<Void>() {
				@Override
				public Void run() {
					cv.getEC2Client().releaseAddress(request);
					return null;
				}
			});

			LogUtils.log(LogAction.RELEASED, "ip address", publicIp);
		}
	}

	/**
	 * Only gets the first external IP it finds in the vpc by design. It assumes
	 * only 1 external ip per vpc
	 * 
	 * @param vpc
	 * @return
	 * @throws ToManyResultsException
	 */
	public String getGatewayElasticIpAddress(Vpc vpc) throws ToManyResultsException {
		if (vpc != null) {
			List<Address> addresses = getAllElasticIpAddress(vpc);
			// if(addresses != null && addresses.size() > 1) {
			// //Added as a stop gap due to lack of requirements for how to
			// handle multiple elastic ip addresses so we fail fast!
			// throw new ToManyResultsException(addresses);
			// }
			if(addresses != null) {
				for (Address addr : addresses) {
					return addr.getPublicIp();
				}
			}
		}
		logger.debug("No External Ip found");
		return StringUtils.EMPTY;
	}

	/**
	 * 
	 * @param vpc
	 * @return
	 * @throws ToManyResultsException
	 */
	public List<Address> getAllElasticIpAddress(Vpc vpc) {
		if (vpc != null) {
			final DescribeAddressesRequest descRequest = new DescribeAddressesRequest();
			// Collection<Filter> filters = new ArrayList<Filter>(2);
			// Filter vpcFilter = new Filter();
			// vpcFilter =
			// vpcFilter.withName("vpc-id").withValues(vpc.getVpcId());
			// filters.add(vpcFilter);
			// descRequest.setFilters(filters);
			DescribeAddressesResult result = AwsRetryManager.run(new Retryable<DescribeAddressesResult>() {
				@Override
				public DescribeAddressesResult run() {
					return cv.getEC2Client().describeAddresses(descRequest);
				}
			});
			if (result.getAddresses().size() > 0) {

				return result.getAddresses();
			}
		}
		logger.debug("No External Ips found");
		return null;
	}

	/**
	 * Only gets the first external IP it finds in the vpc by design. It assumes
	 * only 1 external ip per vpc
	 * 
	 * @param vpc
	 * @return
	 * @throws ToManyResultsException
	 */
	public List<Address> getUnallocatedGatewayElasticIpAddress(Vpc vpc) {
		List<Address> unallocatedAddr = new ArrayList<Address>();
		if (vpc != null) {
			List<Address> addresses = getAllElasticIpAddress(vpc);
			if (addresses == null || addresses.size() == 0) {
				logger.debug("No unallocated external ips found");
			} else {
				for (Address addr : addresses) {
					NetworkInterface nic = getNetworkInterfaceByElasticIpAddress(addr.getPublicIp(), vpc);

					if (nic == null) {
						unallocatedAddr.add(addr);
					}
				}
			}
		}

		return unallocatedAddr;
	}
}
