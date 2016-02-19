package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * The persistent class for the nat database table.
 * 
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@NamedQuery(name="Nat.findAll", query="SELECT n FROM Nat n")
public class Nat implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private String appliedOn;

	private Boolean enabled;

	private String originalSourceIpOrRange;

	private String translatedSourceIpOrRange;

	//bi-directional many-to-one association to Gateway
	@ManyToOne
	@JoinColumn(name="gatewayid")
	private Gateway gateway;
	
	public Nat() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAppliedOn() {
		return this.appliedOn;
	}

	public void setAppliedOn(String appliedOn) {
		this.appliedOn = appliedOn;
	}

	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getOriginalSourceIpOrRange() {
		return this.originalSourceIpOrRange;
	}

	public void setOriginalSourceIpOrRange(String originalSourceIpOrRange) {
		this.originalSourceIpOrRange = originalSourceIpOrRange;
	}

	public String getTranslatedSourceIpOrRange() {
		return this.translatedSourceIpOrRange;
	}

	public void setTranslatedSourceIpOrRange(String translatedSourceIpOrRange) {
		this.translatedSourceIpOrRange = translatedSourceIpOrRange;
	}

	public Gateway getGateway() {
		return this.gateway;
	}

	public void setGateway(Gateway gateway) {
		this.gateway = gateway;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.toString();
	}

}