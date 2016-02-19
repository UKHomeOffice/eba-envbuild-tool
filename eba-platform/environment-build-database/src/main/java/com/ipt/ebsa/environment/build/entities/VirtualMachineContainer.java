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
 * The persistent class for the virtualmachinecontainer database table.
 * 
 */
@Entity
@NamedQuery(name="VirtualMachineContainer.findAll", query="SELECT v FROM VirtualMachineContainer v")
public class VirtualMachineContainer implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private Boolean deploy;

	private String description;

	private String name;

	private Boolean powerOn;

	private String runtimeLease;

	private String serviceLevel;

	private String storageLease;
	
	private String domain;
	
	private String dataCentreName;

	//bi-directional many-to-one association to Network
	@OneToMany(mappedBy="virtualmachinecontainer")
	@OrderBy
	private List<ApplicationNetwork> networks = new ArrayList<ApplicationNetwork>();

	//bi-directional many-to-one association to VirtualMachine
	@OneToMany(mappedBy="virtualmachinecontainer")
	@OrderBy
	private List<VirtualMachine> virtualmachines = new ArrayList<VirtualMachine>();

	//bi-directional many-to-one association to EnvironmentDefinition
	@ManyToOne
	@JoinColumn(name="environmentdefinitionid")
	private EnvironmentDefinition environmentdefinition;
	
	//bi-directional many-to-one association to VirtualMachineContainerMetaData
	@OneToMany(mappedBy="virtualmachinecontainer")
	@OrderBy
	private List<VirtualMachineContainerMetaData> metadata = new ArrayList<VirtualMachineContainerMetaData>();

	public VirtualMachineContainer() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Boolean getDeploy() {
		return this.deploy;
	}

	public void setDeploy(Boolean deploy) {
		this.deploy = deploy;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getPowerOn() {
		return this.powerOn;
	}

	public void setPowerOn(Boolean powerOn) {
		this.powerOn = powerOn;
	}

	public String getRuntimeLease() {
		return this.runtimeLease;
	}

	public void setRuntimeLease(String runtimeLease) {
		this.runtimeLease = runtimeLease;
	}

	public String getServiceLevel() {
		return this.serviceLevel;
	}

	public void setServiceLevel(String serviceLevel) {
		this.serviceLevel = serviceLevel;
	}

	public String getStorageLease() {
		return this.storageLease;
	}

	public void setStorageLease(String storageLease) {
		this.storageLease = storageLease;
	}

	public List<ApplicationNetwork> getNetworks() {
		return this.networks;
	}

	public void setNetworks(List<ApplicationNetwork> networks) {
		this.networks = networks;
	}

	public ApplicationNetwork addNetwork(ApplicationNetwork network) {
		getNetworks().add(network);
		network.setVirtualmachinecontainer(this);

		return network;
	}

	public ApplicationNetwork removeNetwork(ApplicationNetwork network) {
		getNetworks().remove(network);
		network.setVirtualmachinecontainer(null);

		return network;
	}

	public List<VirtualMachine> getVirtualmachines() {
		return this.virtualmachines;
	}

	public void setVirtualmachines(List<VirtualMachine> virtualmachines) {
		this.virtualmachines = virtualmachines;
	}

	public VirtualMachine addVirtualmachine(VirtualMachine virtualmachine) {
		getVirtualmachines().add(virtualmachine);
		virtualmachine.setVirtualmachinecontainer(this);

		return virtualmachine;
	}

	public VirtualMachine removeVirtualmachine(VirtualMachine virtualmachine) {
		getVirtualmachines().remove(virtualmachine);
		virtualmachine.setVirtualmachinecontainer(null);

		return virtualmachine;
	}

	public EnvironmentDefinition getEnvironmentdefinition() {
		return this.environmentdefinition;
	}

	public void setEnvironmentdefinition(EnvironmentDefinition environmentdefinition) {
		this.environmentdefinition = environmentdefinition;
	}
	
	public List<VirtualMachineContainerMetaData> getMetadata() {
		return this.metadata;
	}

	public void setMetadata(List<VirtualMachineContainerMetaData> metadata) {
		this.metadata = metadata;
	}

	public VirtualMachineContainerMetaData addMetadata(VirtualMachineContainerMetaData metadata) {
		getMetadata().add(metadata);
		metadata.setVirtualmachinecontainer(this);

		return metadata;
	}

	public VirtualMachineContainerMetaData removeMetadata(VirtualMachineContainerMetaData metadata) {
		getMetadata().remove(metadata);
		metadata.setVirtualmachinecontainer(null);

		return metadata;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public String getDataCentreName() {
		return dataCentreName;
	}

	public void setDataCentreName(String dataCentreName) {
		this.dataCentreName = dataCentreName;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("name", name)
			.append("dataCentreName", dataCentreName)
			.toString();
	}

}