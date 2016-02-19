package com.ipt.ebsa.agnostic.client.skyscape.module;

import java.math.BigInteger;

/**
 * Container for disk inforation
 *
 */
public class DiskDetail {

	public enum Units {
		GB, MB
	}
	Integer size;
	Units units;
	Integer diskNumber;
	String busType;
	String busSubType;
	
	/**
	 * Figures out the size in MB after working out what the units are
	 * @param size
	 * @param units
	 * @return
	 */
	public BigInteger getSizeInMB() {
		
    	BigInteger newDiskSize = BigInteger.valueOf(size.longValue());
    	if (units == Units.GB){
    		//convert to MB
    		newDiskSize = newDiskSize.multiply(BigInteger.valueOf(1024L));
    	}
    	return newDiskSize;
	}
	
	public Integer getSize() {
		return size;
	}
	public void setSize(Integer size) {
		this.size = size;
	}
	public Units getUnis() {
		return units;
	}
	public void setUnis(Units unis) {
		this.units = unis;
	}
	public Integer getDiskNumber() {
		return diskNumber;
	}
	public void setDiskNumber(Integer diskNumber) {
		this.diskNumber = diskNumber;
	}
	public String getBusType() {
		return busType;
	}
	public void setBusType(String busType) {
		this.busType = busType;
	}
	public String getBusSubType() {
		return busSubType;
	}
	public void setBusSubType(String busSubType) {
		this.busSubType = busSubType;
	}
	
	
	
}
