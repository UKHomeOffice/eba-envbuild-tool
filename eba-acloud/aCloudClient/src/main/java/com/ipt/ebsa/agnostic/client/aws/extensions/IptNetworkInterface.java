package com.ipt.ebsa.agnostic.client.aws.extensions;

/**
 * 
 *
 */
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;

public class IptNetworkInterface extends NetworkInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4922913472525998779L;
	private boolean primary;
	private int deviceIndex;
	private Set<String> vips;

	public IptNetworkInterface() {
	}

	public IptNetworkInterface(NetworkInterface networkInterface) {
		init(networkInterface);
	}

	public IptNetworkInterface(InstanceNetworkInterface networkInterface) {
		super.setDescription(networkInterface.getDescription());
		super.setGroups(networkInterface.getGroups());
		super.setMacAddress(networkInterface.getMacAddress());
		super.setNetworkInterfaceId(networkInterface.getNetworkInterfaceId());
		super.setOwnerId(networkInterface.getOwnerId());
		super.setPrivateDnsName(networkInterface.getPrivateDnsName());
		super.setPrivateIpAddress(networkInterface.getPrivateIpAddress());
		super.setSourceDestCheck(networkInterface.getSourceDestCheck());
		super.setStatus(networkInterface.getStatus());
		super.setSubnetId(networkInterface.getSubnetId());
		super.setVpcId(networkInterface.getVpcId());
		deviceIndex = networkInterface.getAttachment().getDeviceIndex();
	}

	private void init(NetworkInterface networkInterface) {
		super.setAssociation(networkInterface.getAssociation());
		super.setAttachment(networkInterface.getAttachment());
		super.setAvailabilityZone(networkInterface.getAvailabilityZone());
		super.setDescription(networkInterface.getDescription());
		super.setGroups(networkInterface.getGroups());
		super.setMacAddress(networkInterface.getMacAddress());
		super.setNetworkInterfaceId(networkInterface.getNetworkInterfaceId());
		super.setOwnerId(networkInterface.getOwnerId());
		super.setPrivateDnsName(networkInterface.getPrivateDnsName());
		super.setPrivateIpAddress(networkInterface.getPrivateIpAddress());
		super.setPrivateIpAddresses(networkInterface.getPrivateIpAddresses());
		super.setRequesterId(networkInterface.getRequesterId());
		super.setRequesterManaged(networkInterface.getRequesterManaged());
		super.setSourceDestCheck(networkInterface.getSourceDestCheck());
		super.setStatus(networkInterface.getStatus());
		super.setSubnetId(networkInterface.getSubnetId());
		super.setTagSet(networkInterface.getTagSet());
		super.setVpcId(networkInterface.getVpcId());
	}

	public void setNetworkInterface(NetworkInterface networkInterface) {
		init(networkInterface);
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public int getDeviceIndex() {
		return deviceIndex;
	}

	public void setDeviceIndex(int deviceIndex) {
		this.deviceIndex = deviceIndex;
	}

	public Set<String> getVips() {
		return vips;
	}

	public void setVips(Set<String> vips2) {
		this.vips = vips2;
	}

}
