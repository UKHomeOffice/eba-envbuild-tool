package com.ipt.ebsa.agnostic.client.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * 
 *
 */
public class MandatoryCheck {
	
	private static Pattern cidrPatternIpV4 = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/(\\d|[1-2]\\d|3[0-2]))?$");
	private static Pattern ipV4 = Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");
	
	public static final String VNET = "Virtual Network Name:";
	public static final String VPC = "Virtual Private Cloud Name:";
	public static final String VPC_ID = "Virtual Private Cloud ID:";
	public static final String VPC_PEER_ENVIRONMENT = "Virtual Private Peer Environment:";
	public static final String VPC_PEER_NETWORK = "Virtual Private Peer Network:";
	public static final String VAPP = "Virtual Application Name:";
	public static final String VM = "Virtual Machine Name:";
	public static final String ENVIRONMENT = "Environment Container Name:";
	public static final String VMC = "Virtual Machine Container Name:";
	public static final String TAG_KEY = "Tag Key Name:";
	public static final String TAG_VALUE = "Tag Key Value Name:";
	public static final String CLOUD_SERVICE = "Cloud Service Name:";
	public static final String VM_IMAGE = "Virtual Machine Image Name:";
	public static final String SUBNET = "Subnet Name:";
	public static final String CIDR = "CIDR:";
	public static final String IP_ADDRESS = "IP:";
	public static final String RESPONSEDTO = "ResponseDTO:";
	public static final String TAG_COLLECTION = "Tag Collection:";
	public static final String TAGGABLE_RESOURCE_NAME = "Taggable Resource Name:";
	public static final String TAGGABLE_RESOURCE_VMC_NAME = "Taggable Resource Vmc Name:";
	public static final String TAGGABLE_RESOURCE_ID = "Taggable Resource ID:";
	public static final String VOLUME_RESOURCE_ID = "Volume Resource ID:";
	public static final String SNAPSHOT_RESOURCE_ID = "Snapshot Resource ID:";
	public static final String NIC_RESOURCE_ID = "Network Interface Resource ID:";
	public static final String VM_RESOURCE_ID = "VM Resource ID:";
	public static final String INSTANCE_PROFILE_ARN = "Instance Profile Arn:";
	public static final String VM_BOOSTRAP_STRING = "VM Bootstrap String:";
	public static final String STATE = "State Type:";
	public static final String CONFIG = "Config Entry:";
	public static final String ROUTE_TABLE = "Route Table:";
	
	public static String checkIpV4(String ip) {
		checkNotNull(IP_ADDRESS, ip);
		Matcher matcher = ipV4.matcher(ip);		
		if (!matcher.find()) {
			throw new IllegalArgumentException(IP_ADDRESS+ip+" failed to match a valid IPv4 pattern");
		}
		return ip;
	}
	
	public static String checkCidr(String cidr) {
		checkNotNull(CIDR, cidr);
		Matcher matcher = cidrPatternIpV4.matcher(cidr);		
		if (!matcher.find()) {
			throw new IllegalArgumentException(CIDR+cidr+" failed to match a valid CIDR pattern");
		}
		return cidr;
	}
	
	public static String checkNotNull(String inputType, String input) {
		if(StringUtils.isEmpty(input)) {	
			throw new IllegalArgumentException(inputType+" was null");
		}
		return input;
	}
	
	public static <T> T checkNotNull(String inputType, T input) {
		if(input == null) {	
			throw new IllegalArgumentException(inputType+" was null");
		}
		return input;
	}
	
	public static <T> T checkIsNull(String inputType, T input) {
		if(input != null) {	
			throw new IllegalArgumentException(inputType+" was not null");
		}
		return input;
	}

}
