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
 * The persistent class for the environmentcontainerdefinition database table. 
 * 
 */
@Entity
@NamedQueries({
	@NamedQuery(name="EnvironmentContainerDefinition.findAll", query="SELECT e FROM EnvironmentContainerDefinition e"),
	@NamedQuery(name="EnvironmentContainerDefinition.findByName", query="SELECT e FROM EnvironmentContainerDefinition e where e.name = :name"),
	@NamedQuery(name="EnvironmentContainerDefinition.findByNameAndVersion", query="SELECT e FROM EnvironmentContainerDefinition e where e.name = :name and e.version = :version"),
	@NamedQuery(name="EnvironmentContainerDefinition.findByEnvironmentNameAndVersion", query="SELECT ecd FROM EnvironmentContainerDefinition ecd, EnvironmentContainer ec, Environment e where e.name = :name and e.environmentcontainer.id = ec.id and ec.id = ecd.environmentcontainer.id and ecd.version = :version"),
	@NamedQuery(name="EnvironmentContainerDefinition.findForEnvironmentContainerName", query="SELECT ecd FROM EnvironmentContainerDefinition ecd join ecd.environmentcontainer ec where ec.name = :environmentContainerName order by ecd.version desc"),
	@NamedQuery(name="EnvironmentContainerDefinition.findForEnvironmentContainerNameAndProvider", query="SELECT ecd FROM EnvironmentContainerDefinition ecd join ecd.environmentcontainer ec where ec.name = :environmentContainerName and ec.provider = :provider order by ecd.version desc"),
	@NamedQuery(name="EnvironmentContainerDefinition.findCurrentlyDeployedForEnvironmentContainerNameAndProvider", query="SELECT ecd FROM EnvironmentContainerDefinition ecd join ecd.environmentcontainer ec join ecd.environmentContainerBuilds ecb where ec.name = :environmentContainerName and ec.provider = :provider and ecb.succeeded = true order by ecb.datecompleted desc")
})
public class EnvironmentContainerDefinition implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private String name;

	private String version;
	
	//bi-directional many-to-one association to EnvironmentContainer
	@ManyToOne
	@JoinColumn(name="environmentcontainerid")
	private EnvironmentContainer environmentcontainer;

	//bi-directional many-to-one association to Network
	@OneToMany(mappedBy="environmentcontainerdefinition")
	@OrderBy
	private List<OrganisationNetwork> networks = new ArrayList<>();
	
	//bi-directional many-to-one association to Gateway
	@OneToMany(mappedBy="environmentcontainerdefinition")
	@OrderBy
	private List<Gateway> gateways = new ArrayList<Gateway>();

	//bi-directional many-to-one association to DataCentre
	@OneToMany(mappedBy="environmentcontainerdefinition")
	@OrderBy
	private List<DataCentre> dataCentres = new ArrayList<DataCentre>();

	//bi-directional many-to-one association to EnvironmentContainerBuild
	@OneToMany(mappedBy="environmentcontainerdefinition")
	@OrderBy
	private List<EnvironmentContainerBuild> environmentContainerBuilds = new ArrayList<EnvironmentContainerBuild>();

	public EnvironmentContainerDefinition() {
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

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public EnvironmentContainer getEnvironmentcontainer() {
		return this.environmentcontainer;
	}

	public void setEnvironmentcontainer(EnvironmentContainer environmentcontainer) {
		this.environmentcontainer = environmentcontainer;
	}

	public List<OrganisationNetwork> getNetworks() {
		return this.networks;
	}

	public void setNetworks(List<OrganisationNetwork> networks) {
		this.networks = networks;
	}

	public OrganisationNetwork addNetwork(OrganisationNetwork network) {
		getNetworks().add(network);
		network.setEnvironmentcontainerdefinition(this);

		return network;
	}

	public OrganisationNetwork removeNetwork(OrganisationNetwork network) {
		getNetworks().remove(network);
		network.setEnvironmentcontainerdefinition(null);

		return network;
	}
	
	public List<Gateway> getGateways() {
		return this.gateways;
	}

	public void setGateways(List<Gateway> gateways) {
		this.gateways = gateways;
	}

	public Gateway addGateway(Gateway gateway) {
		getGateways().add(gateway);
		gateway.setEnvironmentContainerDefinition(this);

		return gateway;
	}

	public Gateway removeEnvironmentcontainerdefinition(Gateway gateway) {
		getGateways().remove(gateway);
		gateway.setEnvironmentContainerDefinition(null);

		return gateway;
	}

	public List<DataCentre> getDataCentres() {
		return this.dataCentres;
	}

	public void setDataCentres(List<DataCentre> dataCentres) {
		this.dataCentres = dataCentres;
	}

	public DataCentre addDataCentre(DataCentre dataCentre) {
		getDataCentres().add(dataCentre);
		dataCentre.setEnvironmentcontainerdefinition(this);

		return dataCentre;
	}

	public DataCentre removeDataCentre(DataCentre dataCentre) {
		getDataCentres().remove(dataCentre);
		dataCentre.setEnvironmentcontainerdefinition(null);

		return dataCentre;
	}

	public List<EnvironmentContainerBuild> getEnvironmentContainerBuilds() {
		return this.environmentContainerBuilds;
	}

	public void setEnvironmentContainerBuilds(List<EnvironmentContainerBuild> environmentContainerBuilds) {
		this.environmentContainerBuilds = environmentContainerBuilds;
	}

	public EnvironmentContainerBuild addEnvironmentContainerBuild(EnvironmentContainerBuild environmentContainerBuild) {
		getEnvironmentContainerBuilds().add(environmentContainerBuild);
		environmentContainerBuild.setEnvironmentContainerDefinition(this);

		return environmentContainerBuild;
	}

	public EnvironmentContainerBuild removeEnvironmentContainerBuild(EnvironmentContainerBuild environmentContainerBuild) {
		getEnvironmentContainerBuilds().remove(environmentContainerBuild);
		environmentContainerBuild.setEnvironmentContainerDefinition(null);

		return environmentContainerBuild;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("name", name)
			.append("version", version)
			.toString();
	}
}