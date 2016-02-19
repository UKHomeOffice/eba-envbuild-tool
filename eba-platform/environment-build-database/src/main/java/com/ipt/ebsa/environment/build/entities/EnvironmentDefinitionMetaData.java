package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;


/**
 * The persistent class for the environmentdefinitionmetadata database table.
 * 
 */
@Entity
@NamedQuery(name="EnvironmentDefinitionMetaData.findAll", query="SELECT m FROM EnvironmentDefinitionMetaData m")
public class EnvironmentDefinitionMetaData extends MetaData {
	private static final long serialVersionUID = 1L;
	
	//bi-directional many-to-one association to EnvironmentDefinition
	@ManyToOne
	@JoinColumn(name="environmentdefinitionid")
	private EnvironmentDefinition environmentdefinition;

	public EnvironmentDefinition getEnvironmentDefinition() {
		return this.environmentdefinition;
	}

	public void setEnvironmentDefinition(EnvironmentDefinition environmentdefinition) {
		this.environmentdefinition = environmentdefinition;
	}
}