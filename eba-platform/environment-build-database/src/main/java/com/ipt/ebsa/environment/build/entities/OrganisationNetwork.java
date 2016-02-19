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
 * The persistent class for the organisationnetwork database table.
 * 
 */
@Entity
@NamedQueries({
	@NamedQuery(name="OrganisationNetwork.findAll", query="SELECT n FROM OrganisationNetwork n"),
	@NamedQuery(name="OrganisationNetwork.findByName", query="SELECT n FROM OrganisationNetwork n where n.name = :name"),
	@NamedQuery(name="OrganisationNetwork.findForEnvContainerDef", query="select n FROM OrganisationNetwork n where n.environmentcontainerdefinition.id = :envConDefId order by n.id"),
	@NamedQuery(name="OrganisationNetwork.findForEnvironmentNameAndVersion", query="select n FROM OrganisationNetwork n, EnvironmentContainerDefinition ecd, EnvironmentContainer ec, Environment e where e.name = :environmentName and e.environmentcontainer.id = ec.id and ec.id = ecd.environmentcontainer.id and ecd.version = :version and n.environmentcontainerdefinition.id = ecd.id order by n.id")
})
public class OrganisationNetwork extends Network {
	private static final long serialVersionUID = 1L;
	
	private String peerNetworkName;
	
	private String peerEnvironmentName;
	
	//bi-directional many-to-one association to EnvironmentContainerDefinition
	@ManyToOne
	@JoinColumn(name="environmentcontainerdefinitionid")
	private EnvironmentContainerDefinition environmentcontainerdefinition;

	//bi-directional many-to-one association to OrganisationNetworkMetaData
	@OneToMany(mappedBy="organisationNetwork")
	@OrderBy
	private List<OrganisationNetworkMetaData> metadata = new ArrayList<OrganisationNetworkMetaData>();
	
	//bi-directional many-to-one association to Gateway	
	@ManyToOne
	@JoinColumn(name="gatewayid")
	private Gateway gateway;
	
	public OrganisationNetwork() {
	}

	public EnvironmentContainerDefinition getEnvironmentcontainerdefinition() {
		return this.environmentcontainerdefinition;
	}

	public void setEnvironmentcontainerdefinition(EnvironmentContainerDefinition environmentcontainerdefinition) {
		this.environmentcontainerdefinition = environmentcontainerdefinition;
	}

	public List<OrganisationNetworkMetaData> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<OrganisationNetworkMetaData> metadata) {
		this.metadata = metadata;
	}
	
	public OrganisationNetworkMetaData addMetadata(OrganisationNetworkMetaData metadata) {
		getMetadata().add(metadata);
		metadata.setOrganisationNetwork(this);

		return metadata;
	}

	public OrganisationNetworkMetaData removeMetadata(OrganisationNetworkMetaData metadata) {
		getMetadata().remove(metadata);
		metadata.setOrganisationNetwork(null);

		return metadata;
	}
	
	public Gateway getGateway() {
		return gateway;
	}

	public void setGateway(Gateway gateway) {
		this.gateway = gateway;
	}

	public String getPeerNetworkName() {
		return peerNetworkName;
	}

	public void setPeerNetworkName(String peerNetworkName) {
		this.peerNetworkName = peerNetworkName;
	}

	public String getPeerEnvironmentName() {
		return peerEnvironmentName;
	}

	public void setPeerEnvironmentName(String peerEnvironmentName) {
		this.peerEnvironmentName = peerEnvironmentName;
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("name", getName())
			.toString();
	}
}