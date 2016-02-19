package com.ipt.ebsa.agnostic.client.aws.module;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Vpc;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.Retryable;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.client.logging.LogUtils;
import com.ipt.ebsa.agnostic.client.logging.LogUtils.LogAction;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.jcabi.aspects.Loggable;

/**
 * 
 *
 */
@Loggable(prepend = true)
public class AwsSecurityGroupModule extends AwsModule {

	private Logger logger = LogManager.getLogger(AwsSecurityGroupModule.class);

	public Collection<String> getSecurityGroups(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc, Vpc vpc) {
		Collection<String> securityGroups = new ArrayList<String>();
		for (SecurityGroup sg : getSecurityGroups("tag:Name", AwsNamingUtil.getVmcName(vmc), vpc)) {
			securityGroups.add(sg.getGroupId());
		}
		return securityGroups;
	}

	public Collection<String> getGatewaySecurityGroup(XMLEnvironmentType env, XMLGatewayType gateway, Vpc vpc) {
		Collection<String> securityGroups = new ArrayList<String>();
		for (SecurityGroup sg : getSecurityGroups("tag:Name", AwsNamingUtil.getGatewayName(gateway), vpc)) {
			securityGroups.add(sg.getGroupId());
		}
		return securityGroups;
	}

	protected Collection<String> getSecurityGroups(XMLEnvironmentType env, XMLGatewayType gatewayConfig, Vpc vpc) {
		Collection<String> securityGroups = new ArrayList<String>();
		for (SecurityGroup sg : getSecurityGroups("tag:Name", AwsNamingUtil.getGatewayName(gatewayConfig), vpc)) {
			securityGroups.add(sg.getGroupId());
		}
		return securityGroups;
	}

	protected void deleteSecurityGroup(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc, Vpc vpc) {
		Collection<String> groups = getSecurityGroups(env, vmc, vpc);
		if (groups.size() > 0) {
			final DeleteSecurityGroupRequest delete = new DeleteSecurityGroupRequest();
			delete.setGroupId((String) groups.toArray()[0]);
			AwsRetryManager.run(new Retryable<Void>() {
				@Override
				public Void run() {
					cv.getEC2Client().deleteSecurityGroup(delete);
					return null;
				}
			});

		}
	}

	public void deleteSecurityGroup(SecurityGroup sg) {
		final DeleteSecurityGroupRequest delete = new DeleteSecurityGroupRequest();
		delete.setGroupId(sg.getGroupId());
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				LogUtils.log(LogAction.DELETING, "Security Group", delete, "groupId");
				cv.getEC2Client().deleteSecurityGroup(delete);
				LogUtils.log(LogAction.DELETED, "Security Group", delete, "groupId");
				return null;
			}
		});

	}

	protected String createSecurityGroup(String groupName, String vpcId, String description, XMLVirtualMachineContainerType vmc) {
		String groupId = null;
		CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest();
		createSecurityGroupRequest.withGroupName(groupName).withDescription(description);
		createSecurityGroupRequest.setRequestCredentials(cv.getCredentials());
		createSecurityGroupRequest.setVpcId(vpcId);

		LogUtils.log(LogAction.CREATING, "Security Group", groupName);
		CreateSecurityGroupResult csgr = cv.getEC2Client().createSecurityGroup(createSecurityGroupRequest);
		groupId = csgr.getGroupId();
		createTags(groupName, groupId, vmc);
		LogUtils.log(LogAction.CREATED, "Security Group", groupName);

		// Register this security group with owner
		AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
		authorizeSecurityGroupIngressRequest.withGroupId(groupId);
		// LogUtils.log(LogAction.ATTACHING, "Owner to Security Group");
		// amazonEC2Client.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
		return groupId;
	}

	protected String createSecurityGroupGateway(String groupName, String vpcId) {
		String groupId = null;
		CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest();
		createSecurityGroupRequest.withGroupName(groupName).withDescription("Gateway SecurityGroup");
		createSecurityGroupRequest.setRequestCredentials(cv.getCredentials());
		createSecurityGroupRequest.setVpcId(vpcId);

		LogUtils.log(LogAction.CREATING, "Security Group", groupName);
		CreateSecurityGroupResult csgr = cv.getEC2Client().createSecurityGroup(createSecurityGroupRequest);
		groupId = csgr.getGroupId();
		createTags(groupName, groupId, null);
		LogUtils.log(LogAction.CREATED, "Security Group", groupId);

		setDefaultGatewayIngressPorts(groupId, groupName);

		// implicity there from aws config
		// addEgressPorts(amazonEC2Client, groupId, groupName, "0.0.0.0/0",
		// "-1", -1, -1);

		return groupId;
	}

	public void setDefaultGatewayIngressPorts(String groupId, String groupName) {
		addIngressPorts(groupId, groupName, "0.0.0.0/0", "tcp", 22, 22);
		addIngressPorts(groupId, groupName, "0.0.0.0/0", "tcp", 443, 443);
		addIngressPorts(groupId, groupName, "0.0.0.0/0", "tcp", 50, 50);
		addIngressPorts(groupId, groupName, "0.0.0.0/0", "tcp", 51, 51);
		addIngressPorts(groupId, groupName, "0.0.0.0/0", "udp", 500, 500);
		addIngressPorts(groupId, groupName, "0.0.0.0/0", "udp", 4500, 4500);
	}

	public void removeIngressPorts(String groupId, String groupName, String vpcId) {
		DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
		Collection<Filter> filters = new ArrayList<Filter>();
		filters.add(getVpcFilter(vpcId));
		request.setFilters(filters);
		ArrayList<String> ids = new ArrayList<String>();
		ids.add(groupId);
		request.setGroupIds(ids);
		DescribeSecurityGroupsResult result = cv.getEC2Client().describeSecurityGroups(request);
		for (IpPermission ingress : result.getSecurityGroups().get(0).getIpPermissions()) {
			for (String cidr : ingress.getIpRanges()) {
				final RevokeSecurityGroupIngressRequest requestRevoke = new RevokeSecurityGroupIngressRequest();
				requestRevoke.setCidrIp(cidr);
				requestRevoke.setFromPort(ingress.getFromPort());
				requestRevoke.setToPort(ingress.getToPort());
				requestRevoke.setIpProtocol(ingress.getIpProtocol());
				requestRevoke.setGroupId(groupId);
				requestRevoke.setGroupName(groupName);
				logger.info("Removing incoming port range " + ingress.getFromPort() + "-" + ingress.getToPort() + " for protocol"
						+ ingress.getIpProtocol() + " with ip range " + cidr + " for security group=" + groupName + " groupId=" + groupId);

				AwsRetryManager.run(new Retryable<Void>() {
					@Override
					public Void run() {
						cv.getEC2Client().revokeSecurityGroupIngress(requestRevoke);
						return null;
					}
				});

			}

		}
	}

	public void addIngressPorts(String groupId, String groupName, String ipRange, String protocol, Integer fromPort, Integer toPort) {
		Collection<IpPermission> ips = new ArrayList<IpPermission>();
		IpPermission ipssh = new IpPermission();
		ipssh.withIpRanges(ipRange).withIpProtocol(protocol).withFromPort(fromPort).withToPort(toPort);
		ips.add(ipssh);

		logger.info("Incoming port range " + fromPort + "-" + toPort + " for protocol" + protocol + " with ip range " + ipRange
				+ " for security group=" + groupName + " groupId=" + groupId);
		final AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
		authorizeSecurityGroupIngressRequest.withGroupId(groupId).withIpPermissions(ips);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
				return null;
			}
		});

	}

	public void addEgressPorts(String groupId, String groupName, String ipRange, String protocol, Integer fromPort, Integer toPort) {
		Collection<IpPermission> ips = new ArrayList<IpPermission>();
		IpPermission ipssh = new IpPermission();
		ipssh.withIpRanges(ipRange).withIpProtocol(protocol).withFromPort(fromPort).withToPort(toPort);
		ips.add(ipssh);

		logger.info("Outgoing port range " + fromPort + "-" + toPort + " for protocol" + protocol + " with ip range " + ipRange
				+ " for security group=" + groupName + " groupId=" + groupId);
		final AuthorizeSecurityGroupEgressRequest egress = new AuthorizeSecurityGroupEgressRequest();
		egress.withGroupId(groupId).withIpPermissions(ips);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().authorizeSecurityGroupEgress(egress);
				return null;
			}
		});

	}

	public Collection<String> getSecurityGroups(XMLEnvironmentType env, XMLNetworkType subnet, Vpc vpc) {
		Collection<String> securityGroups = new ArrayList<String>();
		for (SecurityGroup sg : getSecurityGroups("tag:Name", AwsNamingUtil.getNetworkName(subnet), vpc)) {
			securityGroups.add(sg.getGroupId());
		}
		return securityGroups;
	}

}
