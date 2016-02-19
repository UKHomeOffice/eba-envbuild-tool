package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

/**
 * The persistent class for the virtualmachinecontainermetadata database table.
 * 
 */
@Entity
@NamedQuery(name="VirtualMachineContainerMetaData.findAll", query="SELECT m FROM VirtualMachineContainerMetaData m")
public class VirtualMachineContainerMetaData extends MetaData {
	private static final long serialVersionUID = 1L;
	
	//bi-directional many-to-one association to VirtualMachineContainer
	@ManyToOne
	@JoinColumn(name="virtualmachinecontainerid")
	private VirtualMachineContainer virtualmachinecontainer;

	public VirtualMachineContainer getVirtualmachinecontainer() {
		return this.virtualmachinecontainer;
	}

	public void setVirtualmachinecontainer(VirtualMachineContainer virtualmachinecontainer) {
		this.virtualmachinecontainer = virtualmachinecontainer;
	}
}
