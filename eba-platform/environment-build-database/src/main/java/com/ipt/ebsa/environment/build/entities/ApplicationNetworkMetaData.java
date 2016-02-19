package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

/**
 * The persistent class for the applicationnetworkmetadata database table.
 * 
 */
@Entity
@NamedQuery(name="ApplicationNetworkMetaData.findAll", query="SELECT m FROM ApplicationNetworkMetaData m")
public class ApplicationNetworkMetaData extends MetaData {
	private static final long serialVersionUID = 1L;
	
	//bi-directional many-to-one association to ApplicationNetwork
	@ManyToOne
	@JoinColumn(name="applicationnetworkid")
	private ApplicationNetwork applicationNetwork;

	public ApplicationNetwork getApplicationNetwork() {
		return this.applicationNetwork;
	}

	public void setApplicationNetwork(ApplicationNetwork applicationNetwork) {
		this.applicationNetwork = applicationNetwork;
	}
}
