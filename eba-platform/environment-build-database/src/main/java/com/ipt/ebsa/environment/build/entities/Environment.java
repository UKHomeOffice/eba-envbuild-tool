package com.ipt.ebsa.environment.build.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Type;


/**
 * The persistent class for the environment database table.
 * 
 */
@Entity
@NamedQueries({
	@NamedQuery(name="Environment.findAll", query="SELECT e FROM Environment e"),
	@NamedQuery(name="Environment.findByName", query="SELECT e FROM Environment e where e.name = :name"),
	@NamedQuery(name="Environment.findByNameAndProvider", query="SELECT e FROM Environment e join e.environmentcontainer ec where e.name = :name and ec.provider = :provider")
})
public class Environment implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private String name;
	
	private String EnvironmentGroupName;

	@Lob
	// This type is needed for the CLOB to work properly on H2 and Postgres
	@Type(type = "org.hibernate.type.StringClobType")
	private String notes;

	private Boolean validated;

	//bi-directional many-to-one association to EnvironmentDefinition
	@OneToMany(mappedBy="environment")
	@OrderBy
	private List<EnvironmentDefinition> environmentdefinitions = new ArrayList<EnvironmentDefinition>();

	//bi-directional many-to-one association to EnvironmentContainer
	@ManyToOne
	@JoinColumn(name="environmentcontainerid")
	private EnvironmentContainer environmentcontainer;

	public Environment() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEnvironmentGroupName() {
		return EnvironmentGroupName;
	}

	public void setEnvironmentGroupName(String environmentGroupName) {
		EnvironmentGroupName = environmentGroupName;
	}

	public String getNotes() {
		return this.notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Boolean getValidated() {
		return this.validated;
	}

	public void setValidated(Boolean validated) {
		this.validated = validated;
	}

	public List<EnvironmentDefinition> getEnvironmentdefinitions() {
		return this.environmentdefinitions;
	}

	public void setEnvironmentdefinitions(List<EnvironmentDefinition> environmentdefinitions) {
		this.environmentdefinitions = environmentdefinitions;
	}

	public EnvironmentDefinition addEnvironmentdefinition(EnvironmentDefinition environmentdefinition) {
		getEnvironmentdefinitions().add(environmentdefinition);
		environmentdefinition.setEnvironment(this);

		return environmentdefinition;
	}

	public EnvironmentDefinition removeEnvironmentdefinition(EnvironmentDefinition environmentdefinition) {
		getEnvironmentdefinitions().remove(environmentdefinition);
		environmentdefinition.setEnvironment(null);

		return environmentdefinition;
	}

	public EnvironmentContainer getEnvironmentcontainer() {
		return this.environmentcontainer;
	}

	public void setEnvironmentcontainer(EnvironmentContainer environmentcontainer) {
		this.environmentcontainer = environmentcontainer;
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("name", name)
			.toString();
	}
}