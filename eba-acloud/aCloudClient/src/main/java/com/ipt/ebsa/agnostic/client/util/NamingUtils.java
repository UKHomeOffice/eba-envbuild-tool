package com.ipt.ebsa.agnostic.client.util;

import org.apache.commons.lang3.StringUtils;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;

public class NamingUtils {
	
	public static final String getVmFQDN(XMLVirtualMachineType vm, XMLVirtualMachineContainerType vmc) {
		StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotBlank(vmc.getDomain()) && !vm.getVmName().contains(vmc.getDomain())) {
			sb.append(vm.getVmName());
			sb.append(".");
			sb.append(vmc.getDomain());
		} else {
			sb.append(vm.getVmName());
		}
		return sb.toString();
	}
	
	public static final String getComputerNameFQDN(XMLVirtualMachineType vm, XMLVirtualMachineContainerType vmc) {
		StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotBlank(vmc.getDomain()) && !vm.getComputerName().contains(vmc.getDomain())) {
			sb.append(vm.getComputerName());
			sb.append(".");
			sb.append(vmc.getDomain());
		} else {
			sb.append(vm.getComputerName());
		}
		return sb.toString();
	}

}
