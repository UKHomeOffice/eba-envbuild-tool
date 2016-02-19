package com.ipt.ebsa.environment.build.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * The persistent class for the nic database table.
 * 
 */
@Entity
@NamedQuery(name="Nic.findAll", query="SELECT n FROM Nic n")
public class Nic implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private Integer indexNumber;

	private String ipAssignment;

	private Boolean primaryNic;
	
	private String networkName;
	
	//bi-directional many-to-one association to Interface
	@OneToMany(mappedBy="nic")
	@OrderBy
	private List<Interface> interfaces = new ArrayList<Interface>();
	
	//bi-directional many-to-one association to VirtualMachine
	@ManyToOne
	@JoinColumn(name="virtualmachineid")
	private VirtualMachine virtualmachine;

	public Nic() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getIndexNumber() {
		return this.indexNumber;
	}

	public void setIndexNumber(Integer indexNumber) {
		this.indexNumber = indexNumber;
	}

	public String getIpAssignment() {
		return this.ipAssignment;
	}

	public void setIpAssignment(String ipAssignment) {
		this.ipAssignment = ipAssignment;
	}

	public Boolean getPrimaryNic() {
		return this.primaryNic;
	}

	public void setPrimaryNic(Boolean primaryNic) {
		this.primaryNic = primaryNic;
	}

	public VirtualMachine getVirtualmachine() {
		return this.virtualmachine;
	}

	public void setVirtualmachine(VirtualMachine virtualmachine) {
		this.virtualmachine = virtualmachine;
	}

	public List<Interface> getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(List<Interface> interfaces) {
		this.interfaces = interfaces;
	}
	
	public Interface addInterface(Interface anInterface) {
		getInterfaces().add(anInterface);
		anInterface.setNic(this);

		return anInterface;
	}

	public String getNetworkName() {
		return networkName;
	}

	public void setNetworkName(String networkName) {
		this.networkName = networkName;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("indexNumber", indexNumber)
			.append("isPrimary", primaryNic)
			.toString();
	}
}