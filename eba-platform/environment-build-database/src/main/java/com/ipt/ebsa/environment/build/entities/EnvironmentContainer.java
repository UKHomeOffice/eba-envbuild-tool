package com.ipt.ebsa.environment.build.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * The persistent class for the environmentcontainer database table.
 * 
 */
@Entity
@NamedQueries({
	@NamedQuery(name="EnvironmentContainer.findAll", query="SELECT e FROM EnvironmentContainer e"),
	@NamedQuery(name="EnvironmentContainer.findByName", query="SELECT e FROM EnvironmentContainer e where e.name = :name"),
	@NamedQuery(name="EnvironmentContainer.findByNameAndProvider", query="SELECT e FROM EnvironmentContainer e where e.name = :name and e.provider = :provider")
})
public class EnvironmentContainer implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private String name;

	private String provider;
	
	//bi-directional many-to-one association to Environment
	@OneToMany(mappedBy="environmentcontainer")
	@OrderBy
	private List<Environment> environments = new ArrayList<Environment>();

	//bi-directional many-to-one association to EnvironmentContainerDefinition
	@OneToMany(mappedBy="environmentcontainer")
	@OrderBy
	private List<EnvironmentContainerDefinition> environmentcontainerdefinitions = new ArrayList<EnvironmentContainerDefinition>();

	//bi-directional many-to-one association to GeographicContainer
	@OneToMany(mappedBy="environmentcontainer")
	@OrderBy
	private List<GeographicContainer> geographiccontainers = new ArrayList<GeographicContainer>();
	
	public EnvironmentContainer() {
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

	public String getProvider() {
		return this.provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public List<Environment> getEnvironments() {
		return this.environments;
	}

	public void setEnvironments(List<Environment> environments) {
		this.environments = environments;
	}

	public Environment addEnvironment(Environment environment) {
		getEnvironments().add(environment);
		environment.setEnvironmentcontainer(this);

		return environment;
	}

	public Environment removeEnvironment(Environment environment) {
		getEnvironments().remove(environment);
		environment.setEnvironmentcontainer(null);

		return environment;
	}

	public List<EnvironmentContainerDefinition> getEnvironmentcontainerdefinitions() {
		return this.environmentcontainerdefinitions;
	}

	public void setEnvironmentcontainerdefinitions(List<EnvironmentContainerDefinition> environmentcontainerdefinitions) {
		this.environmentcontainerdefinitions = environmentcontainerdefinitions;
	}

	public EnvironmentContainerDefinition addEnvironmentcontainerdefinition(EnvironmentContainerDefinition environmentcontainerdefinition) {
		getEnvironmentcontainerdefinitions().add(environmentcontainerdefinition);
		environmentcontainerdefinition.setEnvironmentcontainer(this);

		return environmentcontainerdefinition;
	}

	public EnvironmentContainerDefinition removeEnvironmentcontainerdefinition(EnvironmentContainerDefinition environmentcontainerdefinition) {
		getEnvironmentcontainerdefinitions().remove(environmentcontainerdefinition);
		environmentcontainerdefinition.setEnvironmentcontainer(null);

		return environmentcontainerdefinition;
	}

	public List<GeographicContainer> getGeographiccontainers() {
		return geographiccontainers;
	}

	// TODO Make the relationship with GeographicContainer a 1 to 1 mapping 
	public GeographicContainer getGeographicContainer() {
		return geographiccontainers.size() > 0 ? geographiccontainers.get(0) : null;
	}

	public void setGeographiccontainers(
			List<GeographicContainer> geographiccontainers) {
		this.geographiccontainers = geographiccontainers;
	}
	
	public GeographicContainer addGeographiccontainer(GeographicContainer geographiccontainer) {
		getGeographiccontainers().add(geographiccontainer);
		geographiccontainer.setEnvironmentcontainer(this);

		return geographiccontainer;
	}

	public GeographicContainer removeGeographiccontainer(GeographicContainer geographiccontainer) {
		getGeographiccontainers().remove(geographiccontainer);
		geographiccontainer.setEnvironmentcontainer(null);

		return geographiccontainer;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("name", name)
			.append("provider", provider)
			.toString();
	}
}