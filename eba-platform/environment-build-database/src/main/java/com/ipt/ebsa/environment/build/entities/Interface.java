package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * The persistent class for the interface database table.
 *
 */
@Entity
public class Interface implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private Integer interfaceNumber;

	private String name;
	
	private String staticIpAddress;
	
	private String staticIpPool;
	
	private String networkMask;
	
	private Integer vrrp;
	
	private boolean vip = false;

	//bi-directional many-to-one association to Nic
	@ManyToOne
	@JoinColumn(name="nicId")
	private Nic nic;
	
	public Interface() {
	}
	
	@Override
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	
	public Integer getInterfaceNumber() {
		return interfaceNumber;
	}

	public void setInterfaceNumber(Integer interfaceNumber) {
		this.interfaceNumber = interfaceNumber;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStaticIpAddress() {
		return staticIpAddress;
	}

	public void setStaticIpAddress(String staticIpAddress) {
		this.staticIpAddress = staticIpAddress;
	}

	public String getStaticIpPool() {
		return staticIpPool;
	}

	public void setStaticIpPool(String staticIpPool) {
		this.staticIpPool = staticIpPool;
	}

	public String getNetworkMask() {
		return networkMask;
	}

	public void setNetworkMask(String networkMask) {
		this.networkMask = networkMask;
	}

	public Integer getVrrp() {
		return vrrp;
	}

	public void setVrrp(Integer vrrp) {
		this.vrrp = vrrp;
	}

	public boolean isVip() {
		return vip;
	}

	public void setVip(boolean vip) {
		this.vip = vip;
	}

	public Nic getNic() {
		return nic;
	}

	public void setNic(Nic nic) {
		this.nic = nic;
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("name", getId())
			.append("interfaceNumber", interfaceNumber)
			.append("vip", vip)
			.toString();
	}
}
