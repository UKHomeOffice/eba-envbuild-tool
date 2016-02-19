package com.ipt.ebsa.agnostic.client.skyscape.module;

/**
 * Details for the nic
 *
 */
public class NicDetail {

	private Integer nicNumber;
    private boolean isPrimaryNic;
    private String networkname;
    private String ipAddress;
    private IPAddressingMode ipAddressingMode;
    public enum IPAddressingMode {
    	POOL, MANUAL, DHCP, NONE
    }
    public Integer getNicNumber() {
    	return nicNumber;
    }
    public void setNicNumber(Integer nicNumber) {
    	this.nicNumber = nicNumber;
    }
	public boolean isPrimaryNic() {
		return isPrimaryNic;
	}
	public void setPrimaryNic(boolean isPrimaryNic) {
		this.isPrimaryNic = isPrimaryNic;
	}
	public String getNetworkname() {
		return networkname;
	}
	public void setNetworkname(String networkname) {
		this.networkname = networkname;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public IPAddressingMode getIpAddressingMode() {
		return ipAddressingMode;
	}
	public void setAddressingMode(IPAddressingMode ipAddressingMode) {
		this.ipAddressingMode = ipAddressingMode;
	}
	/**
	 * Check to see if this NIC matches another 
	 * @param otherNic
	 * @return true if this NIC matches another, false otherwise
	 */
    public boolean equals(NicDetail otherNic) {
    	if ((otherNic != null) &&
    		(isPrimaryNic() == otherNic.isPrimaryNic) &&
    		((getNicNumber() == null && otherNic.getNicNumber() == null) || (getNicNumber() != null && getNicNumber().equals(otherNic.getNicNumber()))) &&
    		((getNetworkname() == null && otherNic.getNetworkname() == null) || (getNetworkname() != null && getNetworkname().equals(otherNic.getNetworkname()))) &&
    		((getIpAddress() == null && otherNic.getIpAddress() == null) || (getIpAddress() != null && getIpAddress().equals(otherNic.getIpAddress()))) &&
    		((getIpAddressingMode() == null && otherNic.getIpAddressingMode() == null) || (getIpAddressingMode() != null && getIpAddressingMode().equals(otherNic.getIpAddressingMode())))) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Provide a consistent mechanism to print out the NicDetail for debugging purposes
     * @return String
     */
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append("NIC Number [");
    	sb.append(getNicNumber());
    	sb.append("] | ");
    	sb.append("Is Primary NIC [");
    	sb.append(isPrimaryNic());
    	sb.append("] | ");
    	sb.append("Network Name [");
    	sb.append(getNetworkname());
    	sb.append("] | ");
    	sb.append("IP Address [");
    	sb.append(getIpAddress());
    	sb.append("] | ");
    	sb.append("IP Addressing Mode [");
    	sb.append(getIpAddressingMode());
    	sb.append("]");
    	return sb.toString();
    }
}