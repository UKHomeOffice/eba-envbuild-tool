package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * The persistent class for the storage database table.
 * 
 */
@Entity
@NamedQuery(name="Storage.findAll", query="SELECT s FROM Storage s")
public class Storage implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private String busSubType;

	private String busType;

	private Integer indexNumber;

	private Integer size;

	private String sizeUnit;
	
	private String deviceMount;

	//bi-directional many-to-one association to VirtualMachine
	@ManyToOne
	@JoinColumn(name="virtualmachineid")
	private VirtualMachine virtualmachine;

	public Storage() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getBusSubType() {
		return this.busSubType;
	}

	public void setBusSubType(String busSubType) {
		this.busSubType = busSubType;
	}

	public String getBusType() {
		return this.busType;
	}

	public void setBusType(String busType) {
		this.busType = busType;
	}

	public Integer getIndexNumber() {
		return this.indexNumber;
	}

	public void setIndexNumber(Integer indexNumber) {
		this.indexNumber = indexNumber;
	}

	public Integer getSize() {
		return this.size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public String getSizeUnit() {
		return this.sizeUnit;
	}

	public void setSizeUnit(String sizeUnit) {
		this.sizeUnit = sizeUnit;
	}

	public VirtualMachine getVirtualmachine() {
		return this.virtualmachine;
	}

	public void setVirtualmachine(VirtualMachine virtualmachine) {
		this.virtualmachine = virtualmachine;
	}
	
	public String getDeviceMount() {
		return deviceMount;
	}

	public void setDeviceMount(String deviceMount) {
		this.deviceMount = deviceMount;
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.toString();
	}
}