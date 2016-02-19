package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

/**
 * The persistent class for the virtualmachinemetadata database table.
 * 
 */
@Entity
@NamedQuery(name="VirtualMachineMetaData.findAll", query="SELECT m FROM VirtualMachineMetaData m")
public class VirtualMachineMetaData extends MetaData {
	private static final long serialVersionUID = 1L;
	
	//bi-directional many-to-one association to VirtualMachine
	@ManyToOne
	@JoinColumn(name="virtualmachineid")
	private VirtualMachine virtualmachine;

	public VirtualMachine getVirtualmachine() {
		return this.virtualmachine;
	}

	public void setVirtualmachine(VirtualMachine virtualmachine) {
		this.virtualmachine = virtualmachine;
	}
}