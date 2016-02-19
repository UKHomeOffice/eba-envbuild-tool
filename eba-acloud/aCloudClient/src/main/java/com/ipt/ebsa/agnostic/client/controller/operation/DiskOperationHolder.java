package com.ipt.ebsa.agnostic.client.controller.operation;

import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Volume;

/**
 * 
 *
 */
public class DiskOperationHolder {
	DiskOperationType operation;
	InstanceBlockDeviceMapping instance;
	BlockDeviceMapping config;
	Volume existingVolume;
	Volume newVolume;
	Snapshot snapshot;
	String rootDeviceName;
	String availabilityZone;

	public DiskOperationHolder(DiskOperationType operation, InstanceBlockDeviceMapping instance, BlockDeviceMapping config, Volume volume, String rootDeviceName, String availabilityZone) {
		super();
		this.operation = operation;
		this.instance = instance;
		this.config = config;
		this.existingVolume = volume;
		this.rootDeviceName = rootDeviceName;
		this.availabilityZone = availabilityZone;
	}
	
	public boolean isRootDevice() {
		if(instance != null && instance.getDeviceName().equals(rootDeviceName)) {
			return true;
		} else {
			return false;
		}
	}

	public DiskOperationType getOperation() {
		return operation;
	}

	public void setOperation(DiskOperationType operation) {
		this.operation = operation;
	}

	public InstanceBlockDeviceMapping getInstance() {
		return instance;
	}

	public void setInstance(InstanceBlockDeviceMapping instance) {
		this.instance = instance;
	}

	public BlockDeviceMapping getConfig() {
		return config;
	}

	public void setConfig(BlockDeviceMapping config) {
		this.config = config;
	}

	public Volume getVolume() {
		return existingVolume;
	}

	public void setVolume(Volume volume) {
		this.existingVolume = volume;
	}

	public void setNewVolume(Volume newVolume) {
		this.newVolume = newVolume;	
	}
	
	public Volume getNewVolume() {
		return newVolume;
	}

	public Volume getExistingVolume() {
		return existingVolume;
	}

	public void setExistingVolume(Volume existingVolume) {
		this.existingVolume = existingVolume;
	}

	public Snapshot getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(Snapshot snapshot) {
		this.snapshot = snapshot;
	}
	
	public String getReattachVolumeId() {
		
		String retVal = null;
		switch(operation){
		case CREATE:
		case RESIZE:
			retVal=newVolume.getVolumeId();
			break;
		case UNCHANGED:
			retVal=existingVolume.getVolumeId();
		case DELETE:
		}
		
		return retVal;
	}
	
	
	public String getRootDeviceName() {
		return rootDeviceName;
	}

	public void setRootDeviceName(String rootDeviceName) {
		this.rootDeviceName = rootDeviceName;
	}
	
	public String getAvailabilityZone() {
		return availabilityZone;
	}

	public void setAvailabilityZone(String availabilityZone) {
		this.availabilityZone = availabilityZone;
	}

	public boolean isVmStoppedOperation() {
		boolean reboot = false;
//		boolean rootDisk = false;
//		if (instance != null && instance.getDeviceName().equals(rootDeviceName)) {
//			rootDisk = true;
//		}
		switch (operation) {
		case CREATE:
		case DELETE:
		case UNCHANGED:
			break;
		case RESIZE:
			reboot = true;
			break;
		default:
			break;
		}

		return reboot;
	}
	
	public String getDeviceMount() {
		String retVal = "";
		if(config != null) {
			retVal = config.getDeviceName();
		} else {
			retVal = instance.getDeviceName();
		}
		return retVal;
	}

}
