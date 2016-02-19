package com.ipt.ebsa.environment.build.entities;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * The superclass for the applicationNetwork and organisationNetwork database tables.
 * 
 */
@MappedSuperclass
public class Network implements DBEntity {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private String name;
	
	private String description;
	
	private String fenceMode;

	private String networkMask;

	private String gatewayAddress;
	
	private String primaryDns;

	private String secondaryDns;
	
	private String dnsSuffix;

	private String staticIpPool;
	
	private String ipRangeStart;
	
	private String ipRangeEnd;
	
	private String cidr;
	
	private Boolean shared;
	
	private String dataCentreName;
	
	//one-to-one association to DataCentre	
	@OneToOne
	@JoinColumn(name="datacentreid")
	private DataCentre datacentre;
	
	public Network() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDnsSuffix() {
		return this.dnsSuffix;
	}

	public void setDnsSuffix(String dnsSuffix) {
		this.dnsSuffix = dnsSuffix;
	}

	public String getFenceMode() {
		return this.fenceMode;
	}

	public void setFenceMode(String fenceMode) {
		this.fenceMode = fenceMode;
	}

	public String getGatewayAddress() {
		return this.gatewayAddress;
	}

	public void setGatewayAddress(String gatewayAddress) {
		this.gatewayAddress = gatewayAddress;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNetworkMask() {
		return this.networkMask;
	}

	public void setNetworkMask(String networkMask) {
		this.networkMask = networkMask;
	}

	public String getPrimaryDns() {
		return this.primaryDns;
	}

	public void setPrimaryDns(String primaryDns) {
		this.primaryDns = primaryDns;
	}

	public String getSecondaryDns() {
		return this.secondaryDns;
	}

	public void setSecondaryDns(String secondaryDns) {
		this.secondaryDns = secondaryDns;
	}

	public String getStaticIpPool() {
		return this.staticIpPool;
	}

	public void setStaticIpPool(String staticIpPool) {
		this.staticIpPool = staticIpPool;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCidr() {
		return cidr;
	}

	public void setCidr(String cidr) {
		this.cidr = cidr;
	}

	public Boolean getShared() {
		return shared;
	}

	public void setShared(Boolean shared) {
		this.shared = shared;
	}
	
	public String getIpRangeStart() {
		return ipRangeStart;
	}

	public void setIpRangeStart(String ipRangeStart) {
		this.ipRangeStart = ipRangeStart;
	}

	public String getIpRangeEnd() {
		return ipRangeEnd;
	}

	public void setIpRangeEnd(String ipRangeEnd) {
		this.ipRangeEnd = ipRangeEnd;
	}
	
	public String getDataCentreName() {
		return dataCentreName;
	}

	public void setDataCentreName(String dataCentreName) {
		this.dataCentreName = dataCentreName;
	}
	
	public DataCentre getDataCentre() {
		return datacentre;
	}

	public void setDataCentre(DataCentre datacentre) {
		this.datacentre = datacentre;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("name", name)
			.toString();
	}
}