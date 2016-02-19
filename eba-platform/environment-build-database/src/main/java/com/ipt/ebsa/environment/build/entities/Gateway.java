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
 * The persistent class for the gateway database table.
 * 
 */
@Entity
@NamedQueries({
	@NamedQuery(name="Gateway.findAll", query="SELECT g FROM Gateway g"),
	@NamedQuery(name="Gateway.findByName", query="SELECT g FROM Gateway g where name = :name")
})
public class Gateway implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private String name;

	//bi-directional many-to-one association to Nat
	@OneToMany(mappedBy="gateway")
	@OrderBy
	private List<Nat> nats = new ArrayList<Nat>();

	//bi-directional many-to-one association to EnvironmentContainerDefinition
	@ManyToOne
	@JoinColumn(name="environmentcontainerdefinitionid")
	private EnvironmentContainerDefinition environmentcontainerdefinition;
	
	//bi-directional many-to-one association to Network
	@OneToMany(mappedBy="gateway")
	@OrderBy
	private List<OrganisationNetwork> networks = new ArrayList<OrganisationNetwork>();
	
	public Gateway() {
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

	public List<Nat> getNats() {
		return this.nats;
	}

	public void setNats(List<Nat> nats) {
		this.nats = nats;
	}

	public Nat addNat(Nat nat) {
		getNats().add(nat);
		nat.setGateway(this);

		return nat;
	}

	public Nat removeNat(Nat nat) {
		getNats().remove(nat);
		nat.setGateway(null);

		return nat;
	}

	public List<OrganisationNetwork> getNetworks() {
		return this.networks;
	}

	public void setNetworks(List<OrganisationNetwork> networks) {
		this.networks = networks;
	}

	public OrganisationNetwork addNetwork(OrganisationNetwork network) {
		getNetworks().add(network);
		network.setGateway(this);

		return network;
	}

	public OrganisationNetwork removeNetwork(OrganisationNetwork network) {
		getNetworks().remove(network);
		network.setGateway(null);

		return network;
	}
	
	public EnvironmentContainerDefinition getEnvironmentContainerDefinition() {
		return environmentcontainerdefinition;
	}

	public void setEnvironmentContainerDefinition(EnvironmentContainerDefinition environmentcontainerdefinition) {
		this.environmentcontainerdefinition = environmentcontainerdefinition;
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("name", name)
			.toString();
	}
}