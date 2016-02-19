package com.ipt.ebsa.agnostic.client.aws.module;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesRequest;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.DescribeVpcPeeringConnectionsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcPeeringConnectionsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.NetworkInterfaceStatus;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.SnapshotState;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.VolumeState;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ec2.model.VpcPeeringConnectionStateReason;
import com.ipt.ebsa.agnostic.client.aws.connection.AwsCloudValues;
import com.ipt.ebsa.agnostic.client.aws.extensions.IptInstanceStatus;
import com.ipt.ebsa.agnostic.client.aws.extensions.VpcPeeringConnectionStatus;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.Retryable;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.WaitCondition;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.logging.LogUtils;
import com.ipt.ebsa.agnostic.client.logging.LogUtils.LogAction;
import com.ipt.ebsa.agnostic.client.util.MandatoryCheck;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.jcabi.aspects.Loggable;

/**
 * Aws base class that is extended by all aws modules
 * 
 *
 */
@Loggable(prepend = true)
public abstract class AwsModule {

	private Logger logger = LogManager.getLogger(AwsModule.class);

	@Inject
	protected AwsCloudValues cv;

	private Vpc vpc;

	public Vpc getVpc(XMLEnvironmentType env) {
		MandatoryCheck.checkNotNull(MandatoryCheck.ENVIRONMENT, env);
		LogUtils.log(LogAction.GETTING, "Vpc", env, "name");
		if (vpc == null || !vpc.getCidrBlock().equals(env.getEnvironmentDefinition().get(0).getCidr())) {
			List<Vpc> vpcs = getAllVpc(env, true);
			if (vpcs.size() > 0) {
				vpc = vpcs.get(0);
			}
		}
		LogUtils.log(LogAction.GOT, "Vpc", vpc, "vpcId", "cidrBlock");
		return vpc;
	}

	public Vpc getVpc() {
		return vpc;
	}

	public Vpc getVpc(String vpcId) {
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, vpcId);
		LogUtils.log(LogAction.GETTING, "Vpc", vpcId);
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
		Vpc retVal = null;
		if (result.getVpcs().size() > 0) {
			retVal = result.getVpcs().get(0);
		}
		LogUtils.log(LogAction.GOT, "Vpc", retVal, "vpcId");
		return retVal;
	}

	public List<Vpc> getAllVpc(XMLEnvironmentType env, boolean exactMatch) {
		return getAllVpc(AwsNamingUtil.getEnvironmentName(env), exactMatch);
	}

	public Vpc getVpcByName(String vpcName) throws ToManyResultsException {
		List<Vpc> vpcs = getAllVpc(vpcName, true);
		Vpc retVal = null;
		if (vpcs.size() > 1) {
			throw new ToManyResultsException(vpcs);
		} else if (vpcs.size() == 1) {
			retVal = vpcs.get(0);
		}
		return retVal;

	}

	public List<Vpc> getAllVpc(String vpcName, boolean exactMatch) {
		MandatoryCheck.checkNotNull(MandatoryCheck.ENVIRONMENT, vpcName);

		logger.debug("looking up vpc " + vpcName);
		final DescribeVpcsRequest request = new DescribeVpcsRequest();
		Collection<Filter> filters = new ArrayList<Filter>(2);

		if (exactMatch) {
			filters.add(getNameFilter(vpcName));
			request.setFilters(filters);
		}

		DescribeVpcsResult result = AwsRetryManager.run(new Retryable<DescribeVpcsResult>() {
			@Override
			public DescribeVpcsResult run() {
				return cv.getEC2Client().describeVpcs(request);
			}
		});
		logger.debug("got getVpc results size of " + result.getVpcs().size());
		List<Vpc> allResults = result.getVpcs();
		List<Vpc> returnResults = new ArrayList<Vpc>();
		if (!exactMatch) {
			for (Vpc vpc : allResults) {
				for (Tag tag : vpc.getTags()) {
					if (tag.getKey().contains("Name") && tag.getValue().contains(vpcName)) {
						returnResults.add(vpc);
						break;
					}
				}
			}
		} else {
			returnResults = allResults;
		}
		return returnResults;
	}

	protected List<SecurityGroup> getSecurityGroups(String keyname, String value, Vpc vpc) {
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, vpc);
		MandatoryCheck.checkNotNull(MandatoryCheck.VMC, vpc);
		MandatoryCheck.checkNotNull(MandatoryCheck.TAG_KEY, keyname);
		MandatoryCheck.checkNotNull(MandatoryCheck.TAG_VALUE, value);

		final DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();

		Collection<Filter> filters = new ArrayList<Filter>(2);
		Filter vpcFilterType = new Filter();
		vpcFilterType = vpcFilterType.withName(keyname).withValues(value);
		filters.add(vpcFilterType);

		Filter vpcFilter = new Filter();
		vpcFilter = vpcFilter.withName("vpc-id").withValues(vpc.getVpcId());
		filters.add(vpcFilter);

		request.setFilters(filters);

		DescribeSecurityGroupsResult result = AwsRetryManager.run(new Retryable<DescribeSecurityGroupsResult>() {
			@Override
			public DescribeSecurityGroupsResult run() {
				return cv.getEC2Client().describeSecurityGroups(request);
			}
		});
		return result.getSecurityGroups();
	}

	protected List<Subnet> getSubnet(XMLEnvironmentType env, XMLNetworkType network, Vpc vpc) {
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, vpc);
		MandatoryCheck.checkNotNull(MandatoryCheck.SUBNET, network);
		MandatoryCheck.checkNotNull(MandatoryCheck.ENVIRONMENT, env);

		LogUtils.log(LogAction.GETTING, "Subnet", network, "name", "CIDR");
		final DescribeSubnetsRequest request = new DescribeSubnetsRequest();

		Collection<Filter> filters = new ArrayList<Filter>(2);
		Filter vpcFilterType = new Filter();
		vpcFilterType = vpcFilterType.withName("tag:Name").withValues(AwsNamingUtil.getNetworkName(network));
		filters.add(vpcFilterType);

		Filter vpcFilter = new Filter();
		vpcFilter = vpcFilter.withName("vpc-id").withValues(vpc.getVpcId());
		filters.add(vpcFilter);

		request.setFilters(filters);

		DescribeSubnetsResult result = AwsRetryManager.run(new Retryable<DescribeSubnetsResult>() {
			@Override
			public DescribeSubnetsResult run() {
				return cv.getEC2Client().describeSubnets(request);
			}
		});
		LogUtils.log(LogAction.GOT, "Subnet results " + result.getSubnets().size(), network, "name", "CIDR");
		return result.getSubnets();
	}

	protected List<Subnet> getSubnets(Vpc vpc) {
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, vpc);
		LogUtils.log(LogAction.GETTING, "Subnets for vpc", vpc, "vpcId");
		final DescribeSubnetsRequest request = new DescribeSubnetsRequest();
		Collection<Filter> filters = new ArrayList<Filter>(2);
		filters.add(getVpcFilter(vpc.getVpcId()));
		request.setFilters(filters);

		DescribeSubnetsResult result = AwsRetryManager.run(new Retryable<DescribeSubnetsResult>() {
			@Override
			public DescribeSubnetsResult run() {
				return cv.getEC2Client().describeSubnets(request);
			}
		});
		LogUtils.log(LogAction.GETTING, "Subnets for vpc results: " + result.getSubnets().size(), vpc, "vpcId");
		return result.getSubnets();
	}

	protected NetworkInterface getNetworkInterfaceByPrivateIpAddress(String privateIpAddress, Vpc vpc) {
		MandatoryCheck.checkNotNull(MandatoryCheck.IP_ADDRESS, privateIpAddress);
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, vpc);
		LogUtils.log(LogAction.GETTING, "NetworkInterface for ip " + privateIpAddress + " in vpc", vpc, "vpcId");

		final DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();

		Collection<Filter> filters = new ArrayList<Filter>(2);
		Filter vpcFilter = new Filter();
		vpcFilter = vpcFilter.withName("vpc-id").withValues(vpc.getVpcId());
		filters.add(vpcFilter);

		Filter ipFilter = new Filter();
		ipFilter = ipFilter.withName("addresses.private-ip-address").withValues(privateIpAddress);
		filters.add(ipFilter);

		request.setFilters(filters);

		DescribeNetworkInterfacesResult result = AwsRetryManager.run(new Retryable<DescribeNetworkInterfacesResult>() {
			@Override
			public DescribeNetworkInterfacesResult run() {
				return cv.getEC2Client().describeNetworkInterfaces(request);
			}
		});

		NetworkInterface retVal = null;
		if (result.getNetworkInterfaces().size() > 0) {
			retVal = result.getNetworkInterfaces().get(0);
		}
		LogUtils.log(LogAction.GOT, "NetworkInterface for ip " + privateIpAddress + " in vpc", retVal, "vpcId", "networkInterfaceId", "subnetId");
		return retVal;
	}

	protected NetworkInterface getNetworkInterfaceByElasticIpAddress(String elasticIpAddress, Vpc vpc) {
		MandatoryCheck.checkNotNull(MandatoryCheck.IP_ADDRESS, elasticIpAddress);
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, vpc);
		LogUtils.log(LogAction.GETTING, "NetworkInterface for ip " + elasticIpAddress + " in vpc", vpc, "vpcId", "CidrBlock");

		final DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();

		Collection<Filter> filters = new ArrayList<Filter>(2);
		// Filter vpcFilter = new Filter();
		// vpcFilter = vpcFilter.withName("vpc-id").withValues(vpc.getVpcId());
		// filters.add(vpcFilter);

		Filter ipFilter = new Filter();
		ipFilter = ipFilter.withName("association.public-ip").withValues(elasticIpAddress);
		filters.add(ipFilter);

		request.setFilters(filters);

		DescribeNetworkInterfacesResult result = AwsRetryManager.run(new Retryable<DescribeNetworkInterfacesResult>() {
			@Override
			public DescribeNetworkInterfacesResult run() {
				return cv.getEC2Client().describeNetworkInterfaces(request);
			}
		});

		NetworkInterface retVal = null;
		if (result.getNetworkInterfaces().size() > 0) {
			retVal = result.getNetworkInterfaces().get(0);
		}
		LogUtils.log(LogAction.GOT, "NetworkInterface for ip " + elasticIpAddress + " in vpc", retVal, "vpcId", "networkInterfaceId", "subnetId");
		return retVal;
	}

	
	
	
	public List<Instance> getInstanceProfileUsage(String iamInstanceProfileArn) {
		MandatoryCheck.checkNotNull(MandatoryCheck.INSTANCE_PROFILE_ARN, iamInstanceProfileArn);
		final DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.withFilters(new Filter().withName("iam-instance-profile.arn").withValues(iamInstanceProfileArn));

		DescribeInstancesResult result = AwsRetryManager.run(new Retryable<DescribeInstancesResult>() {
			@Override
			public DescribeInstancesResult run() {
				return cv.getEC2Client().describeInstances(request);
			}
		});

		if (!result.getReservations().isEmpty()) {
			Reservation instance = result.getReservations().get(0);
			return instance.getInstances();
		} else {
			return new ArrayList<Instance>();
		}
	}
	
	public Instance getInstance(String instanceId) {
		MandatoryCheck.checkNotNull(MandatoryCheck.VM_RESOURCE_ID, instanceId);
		final DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.withInstanceIds(instanceId);

		DescribeInstancesResult result = AwsRetryManager.run(new Retryable<DescribeInstancesResult>() {
			@Override
			public DescribeInstancesResult run() {
				return cv.getEC2Client().describeInstances(request);
			}
		});

		if (!result.getReservations().isEmpty()) {
			Reservation instance = result.getReservations().get(0);
			return instance.getInstances().get(0);
		} else {
			return null;
		}
	}

	public Instance getInstance(XMLEnvironmentType env, XMLVirtualMachineType virtualMachine, XMLVirtualMachineContainerType vmc, Vpc vpc)
			throws ToManyResultsException {
		MandatoryCheck.checkNotNull(MandatoryCheck.VM, virtualMachine);
		MandatoryCheck.checkNotNull(MandatoryCheck.ENVIRONMENT, env);
		MandatoryCheck.checkNotNull(MandatoryCheck.VMC, vmc);
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, vpc);
		final DescribeInstancesRequest request = new DescribeInstancesRequest();

		Collection<Filter> filters = new ArrayList<Filter>(2);
		filters.add(getNameFilter(AwsNamingUtil.getVmName(virtualMachine, vmc)));
		filters.add(getVpcFilter(vpc.getVpcId()));
		filters.add(getVmcFilter(AwsNamingUtil.getVmcName(vmc)));
		request.setFilters(filters);

		DescribeInstancesResult result = AwsRetryManager.run(new Retryable<DescribeInstancesResult>() {
			@Override
			public DescribeInstancesResult run() {
				return cv.getEC2Client().describeInstances(request);
			}
		});

		if (result.getReservations().size() > 1) {
			throw new ToManyResultsException(result.getReservations());
		}
		if (!result.getReservations().isEmpty()) {
			Reservation instance = result.getReservations().get(0);
			if (instance.getInstances().size() > 1) {
				throw new ToManyResultsException(instance.getInstances());
			}
			return instance.getInstances().get(0);
		} else {
			return null;
		}
	}

	public List<Instance> getAllVmcInstances(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc, Vpc vpc) {
		MandatoryCheck.checkNotNull(MandatoryCheck.ENVIRONMENT, env);
		MandatoryCheck.checkNotNull(MandatoryCheck.VMC, vmc);
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, vpc);
		final DescribeInstancesRequest request = new DescribeInstancesRequest();

		Collection<Filter> filters = new ArrayList<Filter>(2);
		filters.add(getVpcFilter(vpc.getVpcId()));
		filters.add(getVmcFilter(AwsNamingUtil.getVmcName(vmc)));
		// instance.group-name
		request.setFilters(filters);

		DescribeInstancesResult result = AwsRetryManager.run(new Retryable<DescribeInstancesResult>() {
			@Override
			public DescribeInstancesResult run() {
				return cv.getEC2Client().describeInstances(request);
			}
		});

		if (!result.getReservations().isEmpty()) {
			ArrayList<Instance> instances = new ArrayList<Instance>();
			for (Reservation instance : result.getReservations()) {
				instances.addAll(instance.getInstances());
			}
			return instances;
		} else {
			return new ArrayList<Instance>();
		}
	}

	protected Filter getNameFilter(String name) {
		MandatoryCheck.checkNotNull(MandatoryCheck.TAG_VALUE, name);
		return new Filter().withName("tag:Name").withValues(name);
	}

	protected Filter getVpcFilter(String vpcId) {
		MandatoryCheck.checkNotNull(MandatoryCheck.TAG_VALUE, vpcId);
		return new Filter().withName("vpc-id").withValues(vpcId);
	}

	protected Filter getVmcFilter(String vmcName) {
		MandatoryCheck.checkNotNull(MandatoryCheck.TAG_VALUE, vmcName);
		return new Filter().withName("tag:Environment").withValues(vmcName);
	}

	public List<Instance> getInstances(Vpc vpc) {
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, vpc);
		final DescribeInstancesRequest request = new DescribeInstancesRequest();
		Collection<Filter> filters = new ArrayList<Filter>(2);
		filters.add(getVpcFilter(vpc.getVpcId()));
		request.setFilters(filters);

		DescribeInstancesResult result = AwsRetryManager.run(new Retryable<DescribeInstancesResult>() {
			@Override
			public DescribeInstancesResult run() {
				return cv.getEC2Client().describeInstances(request);
			}
		});

		List<Instance> vpcInstances = new ArrayList<Instance>();

		if (!result.getReservations().isEmpty()) {
			for (Reservation res : result.getReservations()) {
				vpcInstances.addAll(res.getInstances());
			}
		}
		return vpcInstances;
	}

	public List<SecurityGroup> getVmcs(Vpc vpc) {
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC, vpc);
		final DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
		Collection<Filter> filters = new ArrayList<Filter>(2);
		filters.add(getVpcFilter(vpc.getVpcId()));
		request.setFilters(filters);

		DescribeSecurityGroupsResult result = AwsRetryManager.run(new Retryable<DescribeSecurityGroupsResult>() {
			@Override
			public DescribeSecurityGroupsResult run() {
				return cv.getEC2Client().describeSecurityGroups(request);
			}
		});
		return result.getSecurityGroups();
	}

	protected void createTag(String name, String resourceId) {
		MandatoryCheck.checkNotNull(MandatoryCheck.TAGGABLE_RESOURCE_NAME, name);
		MandatoryCheck.checkNotNull(MandatoryCheck.TAGGABLE_RESOURCE_ID, resourceId);
		ArrayList<Tag> tags = new ArrayList<Tag>();
		tags.add(new Tag("Name", name));
		createTags(tags, resourceId);
	}

	protected void createTags(String name, String resourceId, XMLVirtualMachineContainerType vmc, ArrayList<Tag> tags) {
		MandatoryCheck.checkNotNull(MandatoryCheck.TAGGABLE_RESOURCE_NAME, name);

		tags.add(new Tag("Name", name));

		if (vmc != null) {
			tags.add(new Tag("Environment", AwsNamingUtil.getVmcName(vmc)));
		}
		createTags(tags, resourceId);
	}

	protected void createTags(String name, String resourceId, XMLVirtualMachineContainerType vmc) {
		ArrayList<Tag> tags = new ArrayList<Tag>();
		createTags(name, resourceId, vmc, tags);
	}

	protected void createTags(final Collection<Tag> tags, String resourceId) {
		MandatoryCheck.checkNotNull(MandatoryCheck.TAG_COLLECTION, tags);
		MandatoryCheck.checkNotNull(MandatoryCheck.TAGGABLE_RESOURCE_ID, resourceId);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		Date dt = new Date(System.currentTimeMillis());
		String readableTime = sdf.format(dt);

		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				tags.add(new Tag("Owner", cv.getIamUsername()));
				return null;
			}
		});

		tags.add(new Tag("LaunchTime", readableTime));
		tags.add(new Tag("CostCode", "(None defined)"));

		final CreateTagsRequest request = new CreateTagsRequest();
		
		Collection<Tag> obfuscatedTags = new ArrayList<Tag>();
		Iterator<Tag> theseTags = tags.iterator();
		
		while(theseTags.hasNext()) {
			Tag thisTag = theseTags.next();
			Tag obfTag = new Tag(thisTag.getKey(), AwsNamingUtil.replaceHO_AND_IPT(thisTag.getValue()));
			obfuscatedTags.add(obfTag);
		}

		request.withResources(resourceId).withTags(obfuscatedTags);
		LogUtils.log(LogAction.CREATING, "tags", "resource=" + resourceId + ", tags=" + tags);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().createTags(request);
				return null;
			}
		});
		LogUtils.log(LogAction.CREATED, "tags", "resource=" + resourceId + ", tags=" + tags);
	}

	protected void deleteTags(Collection<Tag> tags, String resourceId) {
		MandatoryCheck.checkNotNull(MandatoryCheck.TAG_COLLECTION, tags);
		MandatoryCheck.checkNotNull(MandatoryCheck.TAGGABLE_RESOURCE_ID, resourceId);
		LogUtils.log(LogAction.DELETING, "tags", "resource=" + resourceId + ", tags=" + tags);
		final DeleteTagsRequest request = new DeleteTagsRequest();
		request.withResources(resourceId).withTags(tags);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().deleteTags(request);
				return null;
			}
		});
		LogUtils.log(LogAction.DELETED, "tags", "resource=" + resourceId + ", tags=" + tags);
	}

	public IptInstanceStatus getInstanceStatus(String instanceId) {
		logger.debug("Checking status for instance " + instanceId);
		MandatoryCheck.checkNotNull(MandatoryCheck.VM_RESOURCE_ID, instanceId);
		final DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withInstanceIds(instanceId);
		DescribeInstancesResult describeInstanceResult = AwsRetryManager.run(new Retryable<DescribeInstancesResult>() {
			@Override
			public DescribeInstancesResult run() {
				return cv.getEC2Client().describeInstances(describeInstanceRequest);
			}
		});
		InstanceState state = describeInstanceResult.getReservations().get(0).getInstances().get(0).getState();
		logger.debug("Checked status for instance " + instanceId + ", status is " + state.getName());
		return IptInstanceStatus.fromValue(state.getName());
	}

	public VpcPeeringConnectionStatus getPeeringStatus(String vpcPeeringConnectionId) {
		logger.debug("Checking status for VPC Peer Connection " + vpcPeeringConnectionId);
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC_PEER_ENVIRONMENT, vpcPeeringConnectionId);
		DescribeVpcPeeringConnectionsResult result = getPeeringConnection(vpcPeeringConnectionId);
		VpcPeeringConnectionStateReason state = result.getVpcPeeringConnections().get(0).getStatus();
		logger.debug("Checked status for vpc peering " + vpcPeeringConnectionId + ", status is " + state.getCode());
		return VpcPeeringConnectionStatus.fromValue(state.getCode());
	}

	public DescribeVpcPeeringConnectionsResult getPeeringConnection(String vpcPeeringConnectionId) {
		final DescribeVpcPeeringConnectionsRequest request = new DescribeVpcPeeringConnectionsRequest()
				.withVpcPeeringConnectionIds(vpcPeeringConnectionId);
		request.withVpcPeeringConnectionIds(vpcPeeringConnectionId);
		DescribeVpcPeeringConnectionsResult result = AwsRetryManager.run(new Retryable<DescribeVpcPeeringConnectionsResult>() {
			@Override
			public DescribeVpcPeeringConnectionsResult run() {
				return cv.getEC2Client().describeVpcPeeringConnections(request);
			}
		});
		return result;
	}

	public VolumeState getVolumeStatus(String volumeId) {
		MandatoryCheck.checkNotNull(MandatoryCheck.VOLUME_RESOURCE_ID, volumeId);
		logger.debug("Checking status for volume " + volumeId);
		final DescribeVolumesRequest describeVolumeRequest = new DescribeVolumesRequest().withVolumeIds(volumeId);
		DescribeVolumesResult describeVolumeResult = AwsRetryManager.run(new Retryable<DescribeVolumesResult>() {
			@Override
			public DescribeVolumesResult run() {
				return cv.getEC2Client().describeVolumes(describeVolumeRequest);
			}
		});
		String state = VolumeState.Error.name();
		if (describeVolumeResult.getVolumes().size() > 0) {
			state = describeVolumeResult.getVolumes().get(0).getState();
			logger.debug("Checked status for volume " + volumeId + ", status is " + state);
		} else {
			logger.debug("No volume found for id " + volumeId);
		}
		return VolumeState.fromValue(state);
	}

	public SnapshotState getSnapshotStatus(String snapshotId) {
		MandatoryCheck.checkNotNull(MandatoryCheck.SNAPSHOT_RESOURCE_ID, snapshotId);
		logger.debug("Checking status for snapshot " + snapshotId);
		final DescribeSnapshotsRequest request = new DescribeSnapshotsRequest().withSnapshotIds(snapshotId);
		DescribeSnapshotsResult result = AwsRetryManager.run(new Retryable<DescribeSnapshotsResult>() {
			@Override
			public DescribeSnapshotsResult run() {
				return cv.getEC2Client().describeSnapshots(request);
			}
		});
		String state = SnapshotState.Error.name();
		if (result.getSnapshots().size() > 0) {
			state = result.getSnapshots().get(0).getState();
			logger.debug("Checked status for snapshot " + snapshotId + ", status is " + state);
		} else {
			logger.debug("No snapshot found for id " + snapshotId);
		}
		return SnapshotState.fromValue(state);
	}

	public NetworkInterfaceStatus getNetworkInterfaceStatus(String networkInterfaceId) {
		MandatoryCheck.checkNotNull(MandatoryCheck.NIC_RESOURCE_ID, networkInterfaceId);
		logger.debug("Checking status for network interface " + networkInterfaceId);
		final DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest().withNetworkInterfaceIds(networkInterfaceId);
		DescribeNetworkInterfacesResult result = 
	    	AwsRetryManager.run(new Retryable<DescribeNetworkInterfacesResult>() {
	    		@Override
	    		public DescribeNetworkInterfacesResult run() {
	    			try {
	    			return cv.getEC2Client().describeNetworkInterfaces(request);
	    			} catch (AmazonServiceException e) {
						if(!e.getErrorCode().equals("InvalidNetworkInterfaceID.NotFound")) {
							throw e;
						}
					}
	    			return null;
	    		}
	    	});	
	    String state = NetworkInterfaceStatus.InUse.name();
	    if(result == null) {
	    	AmazonServiceException notFound = new AmazonServiceException("InvalidNetworkInterfaceID.NotFound");
	    	notFound.setErrorCode("InvalidNetworkInterfaceID.NotFound");
	    	throw notFound;
	    }
	    if(result.getNetworkInterfaces().size() > 0) {
	    	state = result.getNetworkInterfaces().get(0).getStatus();
	    	logger.debug("Checked status for network interface "+networkInterfaceId+", status is "+state);
	    } else {
	    	logger.debug("No network interface found for id "+networkInterfaceId);
	    }
	    return NetworkInterfaceStatus.fromValue(state);
	}

	public void waitForInstanceStatus(final String instanceId, final InstanceStateName desiredState, boolean extended)
			throws UnSafeOperationException {
		MandatoryCheck.checkNotNull(MandatoryCheck.VM_RESOURCE_ID, instanceId);
		MandatoryCheck.checkNotNull(MandatoryCheck.STATE, desiredState);

		final int desiredMinCode = IptInstanceStatus.fromValue(desiredState).getCode();

		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				int retVal = getInstanceStatus(instanceId).getCode();
				logger.debug("desiredMinCode=" + desiredMinCode + " retVal=" + retVal);
				return retVal >= desiredMinCode;
			}
		};

		AwsRetryManager.waitFor(waitCondition, "VM: " + instanceId + " did not reach state: " + desiredState, 5000, extended);
	}

	public void waitForPeeringStatus(final String vpcPeeringConnectionId, final VpcPeeringConnectionStatus state, boolean extended)
			throws UnSafeOperationException {
		MandatoryCheck.checkNotNull(MandatoryCheck.VPC_PEER_ENVIRONMENT, vpcPeeringConnectionId);
		MandatoryCheck.checkNotNull(MandatoryCheck.STATE, state);

		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				VpcPeeringConnectionStatus currentStatus = getPeeringStatus(vpcPeeringConnectionId);
				return currentStatus.getCode() >= state.getCode();
			}
		};

		AwsRetryManager.waitFor(waitCondition, "VM: " + vpcPeeringConnectionId + " did not reach state: " + state, 5000, extended);
	}

	public void waitForNicStatus(final String networkInterfaceId, final NetworkInterfaceStatus desiredState, boolean extended)
			throws UnSafeOperationException {
		MandatoryCheck.checkNotNull(MandatoryCheck.NIC_RESOURCE_ID, networkInterfaceId);
		MandatoryCheck.checkNotNull(MandatoryCheck.STATE, desiredState);

		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getNetworkInterfaceStatus(networkInterfaceId) == desiredState;
			}
		};

		AwsRetryManager.waitFor(waitCondition, "NIC: " + networkInterfaceId + " did not reach state: " + desiredState, 5000, extended);
	}

}
