package com.ipt.ebsa.environment.build.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * The persistent class for the environmentdefinition database table. findByEnvironmentNameTypeVersionAndProvider
 * 
 */
@Entity
@NamedQueries({
	@NamedQuery(name="EnvironmentDefinition.findAll", query="SELECT e FROM EnvironmentDefinition e"),
	@NamedQuery(name="EnvironmentDefinition.findByEnvironmentNameTypeAndVersion", query="SELECT ed FROM EnvironmentDefinition ed join ed.environment e where e.name = :environmentName and ed.definitionType = :type and ed.version = :version"),
	@NamedQuery(name="EnvironmentDefinition.findByEnvironmentNameAndType", query="SELECT ed FROM EnvironmentDefinition ed join ed.environment e where e.name = :environmentName and ed.definitionType = :type order by ed.version desc"),
	@NamedQuery(name="EnvironmentDefinition.findByEnvironmentNameAndTypeAndProvider", query="SELECT ed FROM EnvironmentDefinition ed join ed.environment e join e.environmentcontainer ec where ec.provider = :provider and e.name = :environmentName and ed.definitionType = :type order by ed.version desc"),
	@NamedQuery(name="EnvironmentDefinition.findByEnvironmentNameTypeVersionAndProvider", query="SELECT ed FROM EnvironmentDefinition ed join ed.environment e join e.environmentcontainer ec where ec.provider = :provider and e.name = :environmentName and ed.definitionType = :type and ed.version = :version order by ed.version desc")
})
public class EnvironmentDefinition implements DBEntity {
	private static final long serialVersionUID = 1L;
	
	/** Types of Environment Definition */
	public static enum DefinitionType {
		Logical, Physical, Blueprint
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private String definitionType;

	private String name;

	private String version;
	
	private String cidr;
	
	//bi-directional many-to-one association to Environment
	@ManyToOne
	@JoinColumn(name="environmentid")
	private Environment environment;

	//bi-directional many-to-one association to VirtualMachineContainer
	@OneToMany(mappedBy="environmentdefinition")
	@OrderBy
	private List<VirtualMachineContainer> virtualmachinecontainers = new ArrayList<VirtualMachineContainer>();

	//bi-directional many-to-one association to EnvironmentDefinitionMetaData
	@OneToMany(mappedBy="environmentdefinition")
	@OrderBy
	private List<EnvironmentDefinitionMetaData> metadata = new ArrayList<EnvironmentDefinitionMetaData>();
	
	public EnvironmentDefinition() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDefinitionType() {
		return this.definitionType;
	}

	public void setDefinitionType(String definitionType) {
		this.definitionType = definitionType;
	}
	
	public void setDefinitionType(DefinitionType definitionType) {
		this.definitionType = definitionType.toString();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public List<VirtualMachineContainer> getVirtualmachinecontainers() {
		return this.virtualmachinecontainers;
	}

	public void setVirtualmachinecontainers(List<VirtualMachineContainer> virtualmachinecontainers) {
		this.virtualmachinecontainers = virtualmachinecontainers;
	}

	public VirtualMachineContainer addVirtualmachinecontainer(VirtualMachineContainer virtualmachinecontainer) {
		getVirtualmachinecontainers().add(virtualmachinecontainer);
		virtualmachinecontainer.setEnvironmentdefinition(this);

		return virtualmachinecontainer;
	}

	public VirtualMachineContainer removeVirtualmachinecontainer(VirtualMachineContainer virtualmachinecontainer) {
		getVirtualmachinecontainers().remove(virtualmachinecontainer);
		virtualmachinecontainer.setEnvironmentdefinition(null);

		return virtualmachinecontainer;
	}
	
	public List<EnvironmentDefinitionMetaData> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<EnvironmentDefinitionMetaData> metadata) {
		this.metadata = metadata;
	}
	
	public EnvironmentDefinitionMetaData addMetadata(EnvironmentDefinitionMetaData metadata) {
		getMetadata().add(metadata);
		metadata.setEnvironmentDefinition(this);

		return metadata;
	}

	public EnvironmentDefinitionMetaData removeMetadata(EnvironmentDefinitionMetaData metadata) {
		getMetadata().remove(metadata);
		metadata.setEnvironmentDefinition(null);

		return metadata;
	}
	
	public String getCidr() {
		return cidr;
	}

	public void setCidr(String cidr) {
		this.cidr = cidr;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("name", name)
			.append("version", version)
			.toString();
	}
}