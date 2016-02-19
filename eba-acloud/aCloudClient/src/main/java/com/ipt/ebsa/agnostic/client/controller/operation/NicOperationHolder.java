package com.ipt.ebsa.agnostic.client.controller.operation;

import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.ipt.ebsa.agnostic.client.aws.extensions.IptNetworkInterface;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;

/**
 * 
 *
 */
public class NicOperationHolder {
	NicOperationtype operation;
	XMLNICType configNic;
	XMLVirtualMachineContainerType vmc;
	XMLVirtualMachineType vm;
	InstanceNetworkInterface instanceNic;
	IptNetworkInterface createdNic;
	public static final Integer PRIMARY_NIC_INDEX = new Integer(0);

	public NicOperationtype getOperation() {
		return operation;
	}

	public void setOperation(NicOperationtype operation) {
		this.operation = operation;
	}

	public void setConfigNic(XMLNICType configNic) {
		this.configNic = configNic;
	}

	public void setInstanceNic(InstanceNetworkInterface instanceNic) {
		this.instanceNic=instanceNic;
	}

	public XMLNICType getConfigNic() {
		return configNic;
	}

	public InstanceNetworkInterface getInstanceNic() {
		return instanceNic;
	}

	public IptNetworkInterface getCreatedNic() {
		return createdNic;
	}

	public void setCreatedNic(IptNetworkInterface createdNic) {
		this.createdNic = createdNic;
	}
	
	public IptNetworkInterface getNicForVM() {
		IptNetworkInterface retVal = null;
		switch(operation) {
		case CREATE:
		case REPLACE:
			retVal = createdNic;
			break;
		case MODIFY:
		case UNCHANGED:
			retVal = new IptNetworkInterface(instanceNic);
			if(configNic != null) {
				Set<String> vips = new HashSet<String>();
				vips.add(AwsNamingUtil.getVipNameFromVm(vm, vmc));
				retVal.setVips(vips);
			}
			break;
		case DELETE:
		default:
			break;
		
		}
		if(retVal != null) {
			retVal.setDeviceIndex(configNic.getIndexNumber().intValue());
		}
		return retVal;
	}
	
	public IptNetworkInterface getNicForRoleCheck() {
		IptNetworkInterface retVal = null;
		boolean hasVip = false;
		if(configNic != null) {
			for(XMLInterfaceType interf :configNic.getInterface()) {
				if(interf.isIsVip()) {
					hasVip = true;
					break;
				}
			}
		}
		
		if(hasVip) {
			retVal = new IptNetworkInterface();
			Set<String> vips = new HashSet<String>();
			vips.add(AwsNamingUtil.getVipNameFromVm(vm, vmc));
			retVal.setVips(vips);
		}
		
		return retVal;
	}
	
	public boolean isVmStoppedOperation() {
		boolean stop = false;
		boolean primaryNic = false;
		if(instanceNic != null && instanceNic.getAttachment().getDeviceIndex().equals(PRIMARY_NIC_INDEX)) {
			primaryNic = true;
		}
		switch(operation) {
		case CREATE:
		case REPLACE:
		case DELETE:
		case UNCHANGED:
			if(primaryNic){
				stop = true;
			}
			break;
		default:
			break;
		}
		
		return stop;
	}
	
	public boolean isVmTerminateOperation() {
		boolean terminate = false;
		boolean primaryNic = false;
		if(instanceNic != null && instanceNic.getAttachment().getDeviceIndex().equals(PRIMARY_NIC_INDEX)) {
			primaryNic = true;
		}
		switch(operation) {
		case CREATE:
		case REPLACE:
		case DELETE:
			if(primaryNic){
				terminate = true;
			}
		case UNCHANGED:
			break;
		default:
			break;
		}
		
		return terminate;
	}

	public XMLVirtualMachineContainerType getVmc() {
		return vmc;
	}

	public void setVmc(XMLVirtualMachineContainerType vmc) {
		this.vmc = vmc;
	}

	public XMLVirtualMachineType getVm() {
		return vm;
	}

	public void setVm(XMLVirtualMachineType vm) {
		this.vm = vm;
	}
}
