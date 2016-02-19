package com.ipt.ebsa.agnostic.client.aws.util;

import org.apache.commons.lang3.StringUtils;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLApplicationNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;

public class AwsNamingUtil {

	public static String getVmName(XMLVirtualMachineType vm, XMLVirtualMachineContainerType vmc) {
		return replaceHO_AND_IPT(vm.getVmName() + "." + vmc.getDomain());
	}

	public static String getVmcName(XMLVirtualMachineContainerType vmc) {
		return replaceHO_AND_IPT(vmc.getName());
	}

	public static String getNicName(XMLNICType nic, XMLVirtualMachineContainerType vmc) {
		return replaceHO_AND_IPT(nic.getInterface().get(0).getName() + "." + vmc.getDomain());
	}
	
	public static String getEnvironmentName(XMLEnvironmentType env) {
		return replaceHO_AND_IPT(env.getName());
	}
	
	public static String getPeerEnvironmentName(String peerVpc) {
		return replaceHO_AND_IPT(peerVpc);
	}
	
	public static String getPeerNetworkName(String peerNetwork) {
		return replaceHO_AND_IPT(peerNetwork);
	}

	public static String getRoleName(String[] vip, XMLVirtualMachineContainerType vmc) {
		return replaceHO_AND_IPT(vip[0] + "." + vmc.getDomain());
	}

	public static String getPolicyName(String roleName) {
		return replaceHO_AND_IPT(roleName + "_Policy");
	}
	
	public static String getIamInstanceProfileNameFromArn(String instanceProfileArn) {
		return instanceProfileArn.split("/")[1];
	}
	
	public static String getGatewayName(XMLGatewayType gatewayConfig) {
		return replaceHO_AND_IPT(gatewayConfig.getName());
	}
	
	public static String getOrganisationalNetworkName(XMLOrganisationalNetworkType type) {
		return replaceHO_AND_IPT(type.getName());
	}
	
	public static String getOrganisationalNetworkName(XMLApplicationNetworkType type) {
		return replaceHO_AND_IPT(type.getName());
	}
	
	public static String getNetworkName(XMLNetworkType type) {
		return replaceHO_AND_IPT(type.getName());
	}
	
	public static String getInterfaceName(XMLInterfaceType type) {
		return replaceHO_AND_IPT(type.getName());
	}
	
	public static String getVipNameFromVm(XMLVirtualMachineType vm, XMLVirtualMachineContainerType vmc) {
		//jpsem01.
		char[] hostname = vm.getVmName().toCharArray();
		StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < hostname.length ; i++) {
			if(!StringUtils.isNumeric(hostname[i]+"") ) {
				sb.append(hostname[i]);
			}
		}
		sb.append("v");
		return replaceHO_AND_IPT(sb.toString() + "." + vmc.getDomain());
	}
	
	public static String replaceHO_AND_IPT(String inName) {
		String outName = inName.replaceAll("HO", "AA");
		outName = outName.replaceAll("IPT", "001");
		outName = outName.replaceAll("ho", "aa");
		outName = outName.replaceAll("ipt", "001");
		return outName;
	}
}
