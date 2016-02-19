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
 * The persistent class for the virtualmachine database table.
 * 
 */
@Entity
@NamedQuery(name="VirtualMachine.findAll", query="SELECT v FROM VirtualMachine v")
public class VirtualMachine implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private String computerName;

	private Integer cpuCount;

	private String customisationScript;

	private String description;

	private Integer memory;

	private String memoryUnit;

	private String storageProfile;

	private String templateName;

	private String templateServiceLevel;

	private String vmName;
	
	private String hardwareProfile;
	
	private String haType;

	//bi-directional many-to-one association to VirtualMachineMetaData
	@OneToMany(mappedBy="virtualmachine")
	@OrderBy
	private List<VirtualMachineMetaData> metadata = new ArrayList<VirtualMachineMetaData>();

	//bi-directional many-to-one association to Nic
	@OneToMany(mappedBy="virtualmachine")
	@OrderBy
	private List<Nic> nics = new ArrayList<Nic>();

	//bi-directional many-to-one association to Storage
	@OneToMany(mappedBy="virtualmachine")
	@OrderBy
	private List<Storage> storages = new ArrayList<Storage>();

	//bi-directional many-to-one association to VirtualMachineContainer
	@ManyToOne
	@JoinColumn(name="virtualmachinecontainerid")
	private VirtualMachineContainer virtualmachinecontainer;

	public VirtualMachine() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getComputerName() {
		return this.computerName;
	}

	public void setComputerName(String computerName) {
		this.computerName = computerName;
	}

	public Integer getCpuCount() {
		return this.cpuCount;
	}

	public void setCpuCount(Integer cpuCount) {
		this.cpuCount = cpuCount;
	}

	public String getCustomisationScript() {
		return this.customisationScript;
	}

	public void setCustomisationScript(String customisationScript) {
		this.customisationScript = customisationScript;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getMemory() {
		return this.memory;
	}

	public void setMemory(Integer memory) {
		this.memory = memory;
	}

	public String getMemoryUnit() {
		return this.memoryUnit;
	}

	public void setMemoryUnit(String memoryUnit) {
		this.memoryUnit = memoryUnit;
	}

	public String getStorageProfile() {
		return this.storageProfile;
	}

	public void setStorageProfile(String storageProfile) {
		this.storageProfile = storageProfile;
	}

	public String getTemplateName() {
		return this.templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getTemplateServiceLevel() {
		return this.templateServiceLevel;
	}

	public void setTemplateServiceLevel(String templateServiceLevel) {
		this.templateServiceLevel = templateServiceLevel;
	}

	public String getVmName() {
		return this.vmName;
	}

	public void setVmName(String vmName) {
		this.vmName = vmName;
	}

	public List<VirtualMachineMetaData> getMetadata() {
		return this.metadata;
	}

	public void setMetadata(List<VirtualMachineMetaData> metadata) {
		this.metadata = metadata;
	}

	public VirtualMachineMetaData addMetadata(VirtualMachineMetaData metadata) {
		getMetadata().add(metadata);
		metadata.setVirtualmachine(this);

		return metadata;
	}

	public VirtualMachineMetaData removeMetadata(VirtualMachineMetaData metadata) {
		getMetadata().remove(metadata);
		metadata.setVirtualmachine(null);

		return metadata;
	}
	
	public List<Nic> getNics() {
		return this.nics;
	}

	public void setNics(List<Nic> nics) {
		this.nics = nics;
	}

	public Nic addNic(Nic nic) {
		getNics().add(nic);
		nic.setVirtualmachine(this);

		return nic;
	}

	public Nic removeNic(Nic nic) {
		getNics().remove(nic);
		nic.setVirtualmachine(null);

		return nic;
	}

	public List<Storage> getStorages() {
		return this.storages;
	}

	public void setStorages(List<Storage> storages) {
		this.storages = storages;
	}

	public Storage addStorage(Storage storage) {
		getStorages().add(storage);
		storage.setVirtualmachine(this);

		return storage;
	}

	public Storage removeStorage(Storage storage) {
		getStorages().remove(storage);
		storage.setVirtualmachine(null);

		return storage;
	}

	public VirtualMachineContainer getVirtualmachinecontainer() {
		return this.virtualmachinecontainer;
	}

	public void setVirtualmachinecontainer(VirtualMachineContainer virtualmachinecontainer) {
		this.virtualmachinecontainer = virtualmachinecontainer;
	}
	
	public String getHardwareProfile() {
		return hardwareProfile;
	}

	public void setHardwareProfile(String hardwareProfile) {
		this.hardwareProfile = hardwareProfile;
	}

	public String getHaType() {
		return haType;
	}

	public void setHaType(String haType) {
		this.haType = haType;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("vmName", vmName)
			.toString();
	}
}