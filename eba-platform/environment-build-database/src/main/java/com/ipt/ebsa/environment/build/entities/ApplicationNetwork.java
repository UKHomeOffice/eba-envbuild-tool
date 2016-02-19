package com.ipt.ebsa.environment.build.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * The persistent class for the applicationNetwork database table.
 * 
 */
@Entity
@NamedQueries({
	@NamedQuery(name="ApplicationNetwork.findAll", query="SELECT n FROM ApplicationNetwork n"),
	@NamedQuery(name="ApplicationNetwork.findByName", query="SELECT n FROM ApplicationNetwork n where n.name = :name")
})
public class ApplicationNetwork extends Network {
	private static final long serialVersionUID = 1L;
	
	//bi-directional many-to-one association to VirtualMachineContainer
	@ManyToOne
	@JoinColumn(name="virtualmachinecontainerid")
	private VirtualMachineContainer virtualmachinecontainer;

	//bi-directional many-to-one association to ApplicationNetworkMetaData
	@OneToMany(mappedBy="applicationNetwork")
	@OrderBy
	private List<ApplicationNetworkMetaData> metadata = new ArrayList<ApplicationNetworkMetaData>();
	
	public ApplicationNetwork() {
	}

	public VirtualMachineContainer getVirtualmachinecontainer() {
		return this.virtualmachinecontainer;
	}

	public void setVirtualmachinecontainer(VirtualMachineContainer virtualmachinecontainer) {
		this.virtualmachinecontainer = virtualmachinecontainer;
	}
	
	public List<ApplicationNetworkMetaData> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<ApplicationNetworkMetaData> metadata) {
		this.metadata = metadata;
	}
	
	public ApplicationNetworkMetaData addMetadata(ApplicationNetworkMetaData metadata) {
		getMetadata().add(metadata);
		metadata.setApplicationNetwork(this);

		return metadata;
	}

	public ApplicationNetworkMetaData removeMetadata(ApplicationNetworkMetaData metadata) {
		getMetadata().remove(metadata);
		metadata.setApplicationNetwork(null);

		return metadata;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("name", getName())
			.toString();
	}
}