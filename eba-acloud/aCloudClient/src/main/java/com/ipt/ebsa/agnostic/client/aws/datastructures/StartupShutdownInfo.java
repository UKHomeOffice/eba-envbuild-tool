package com.ipt.ebsa.agnostic.client.aws.datastructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.amazonaws.services.ec2.model.Instance;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;

public class StartupShutdownInfo {

	private int order;
	private ArrayList<XMLVirtualMachineType> vmList = new ArrayList<XMLVirtualMachineType>();
	private XMLVirtualMachineContainerType vmc;

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public ArrayList<XMLVirtualMachineType> getVmList() {
		return vmList;
	}

	public void setVmList(ArrayList<XMLVirtualMachineType> vmList) {
		this.vmList = vmList;
	}

	public XMLVirtualMachineContainerType getVmc() {
		return vmc;
	}

	public void setVmc(XMLVirtualMachineContainerType vmc) {
		this.vmc = vmc;
	}

	public Collection<String> getInstanceIds(HashMap<String, Instance> instanceMap) {
		Collection<String> ids = new ArrayList<String>();
		for (XMLVirtualMachineType vm : vmList) {
			ids.add(instanceMap.get(AwsNamingUtil.getVmName(vm, vmc)).getInstanceId());
		}
		return ids;
	}

}
