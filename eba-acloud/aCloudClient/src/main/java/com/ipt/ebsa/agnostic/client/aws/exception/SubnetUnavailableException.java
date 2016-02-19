package com.ipt.ebsa.agnostic.client.aws.exception;

import java.util.List;

import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNetworkType;

public class SubnetUnavailableException extends Exception {

	public SubnetUnavailableException(String string, XMLNetworkType subnet) {
		super(String.format("Message:[%s] Subnet name:%s cidr:%s type:%s", string, AwsNamingUtil.getNetworkName(subnet), subnet.getCIDR(), subnet.getClass().getName()));
	}

	public SubnetUnavailableException(String string, XMLNetworkType subnet, XMLNICType nic) {
		super(String.format("Message:[%s] Subnet [name:%s cidr:%s type:%s] Nic [%s]", string, AwsNamingUtil.getNetworkName(subnet), subnet.getCIDR(), subnet.getClass()
				.getName(), nic.getIndexNumber(), printInterfaces(nic.getInterface())));
	}

	private static String printInterfaces(List<XMLInterfaceType> interfaces) {
		StringBuilder sb = new StringBuilder();

		for (XMLInterfaceType inter : interfaces) {
			sb.append("Interface " + inter.getInterfaceNumber());
			sb.append(" Name: ");
			sb.append(AwsNamingUtil.getInterfaceName(inter));
			sb.append(" Ip: ");
			sb.append(inter.getStaticIpAddress());
			sb.append("***");
		}

		return sb.toString();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 4648985883093862378L;

}
