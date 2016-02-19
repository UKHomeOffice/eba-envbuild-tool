package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * The persistent class for the datacentre database table.
 * 
 */
@Entity
@NamedQueries({
	@NamedQuery(name="DataCentre.findAll", query="SELECT d FROM DataCentre d")
})
public class DataCentre implements DBEntity {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;
	
	private String name;
	
	//bi-directional many-to-one association to EnvironmentContainerDefinition
	@ManyToOne
	@JoinColumn(name="environmentcontainerdefinitionid")
	private EnvironmentContainerDefinition environmentcontainerdefinition;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public EnvironmentContainerDefinition getEnvironmentcontainerdefinition() {
		return this.environmentcontainerdefinition;
	}

	public void setEnvironmentcontainerdefinition(EnvironmentContainerDefinition environmentcontainerdefinition) {
		this.environmentcontainerdefinition = environmentcontainerdefinition;
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("name", name)
			.toString();
	}

}
