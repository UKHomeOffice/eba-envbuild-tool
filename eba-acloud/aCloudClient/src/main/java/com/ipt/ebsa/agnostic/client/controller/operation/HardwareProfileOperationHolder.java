package com.ipt.ebsa.agnostic.client.controller.operation;

import com.amazonaws.services.ec2.model.Instance;


/**
 * 
 *
 */
public class HardwareProfileOperationHolder {
	HardwareOperationType operation;
	String hardwareProfile;
	Instance vm;
	
	public HardwareProfileOperationHolder(HardwareOperationType operation, String hardwareProfile, Instance vm) {
		this.operation = operation;
		this.hardwareProfile = hardwareProfile;
		this.vm = vm;
	}

	public HardwareOperationType getOperation() {
		return operation;
	}

	public void setOperation(HardwareOperationType operation) {
		this.operation = operation;
	}

	public String getHardwareProfile() {
		return hardwareProfile;
	}

	public void setHardwareProfile(String hardwareProfile) {
		this.hardwareProfile = hardwareProfile;
	}

	public Instance getVm() {
		return vm;
	}

	public void setVm(Instance vm) {
		this.vm = vm;
	}
	
}
