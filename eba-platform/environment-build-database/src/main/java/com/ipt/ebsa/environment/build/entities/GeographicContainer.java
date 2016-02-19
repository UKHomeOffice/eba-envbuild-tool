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
 * The persistent class for the geographiccontainer database table.
 * 
 */
@Entity
@NamedQueries({
	@NamedQuery(name="GeographicContainer.findAll", query="SELECT g FROM GeographicContainer g")
})
public class GeographicContainer implements DBEntity {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;
	
	private String account;
	
	private String region;
	
	//bi-directional many-to-one association to EnvironmentContainer
	@ManyToOne
	@JoinColumn(name="environmentcontainerid")
	private EnvironmentContainer environmentcontainer;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public EnvironmentContainer getEnvironmentcontainer() {
		return environmentcontainer;
	}

	public void setEnvironmentcontainer(EnvironmentContainer environmentcontainer) {
		this.environmentcontainer = environmentcontainer;
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("account", account)
			.toString();
	}
}
