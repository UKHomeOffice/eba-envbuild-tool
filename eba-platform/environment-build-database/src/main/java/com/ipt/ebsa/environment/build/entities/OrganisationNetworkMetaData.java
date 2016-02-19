package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

/**
 * The persistent class for the organisationnetworkmetadata database table.
 * 
 */
@Entity
@NamedQuery(name="OrganisationNetworkMetaData.findAll", query="SELECT m FROM OrganisationNetworkMetaData m")
public class OrganisationNetworkMetaData extends MetaData {
	private static final long serialVersionUID = 1L;
	
	//bi-directional many-to-one association to OrganisationNetwork
	@ManyToOne
	@JoinColumn(name="organisationnetworkid")
	private OrganisationNetwork organisationNetwork;

	public OrganisationNetwork getOrganisationNetwork() {
		return this.organisationNetwork;
	}

	public void setOrganisationNetwork(OrganisationNetwork organisationNetwork) {
		this.organisationNetwork = organisationNetwork;
	}
}
