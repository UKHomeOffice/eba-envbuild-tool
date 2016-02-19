package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.NamedQuery;

import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * The persistent class for the dnat database table.
 * 
 */
@Entity
@NamedQuery(name="DNat.findAll", query="SELECT d FROM DNat d")
public class DNat extends Nat {
	private static final long serialVersionUID = 1L;

	private String protocolIcmpType;

	private String protocolOriginalPort;

	private String protocolType;

	private String translatedPort;

	public DNat() {
	}

	public String getProtocolIcmpType() {
		return this.protocolIcmpType;
	}

	public void setProtocolIcmpType(String protocolIcmpType) {
		this.protocolIcmpType = protocolIcmpType;
	}

	public String getProtocolOriginalPort() {
		return this.protocolOriginalPort;
	}

	public void setProtocolOriginalPort(String protocolOriginalPort) {
		this.protocolOriginalPort = protocolOriginalPort;
	}

	public String getProtocolType() {
		return this.protocolType;
	}

	public void setProtocolType(String protocolType) {
		this.protocolType = protocolType;
	}

	public String getTranslatedPort() {
		return this.translatedPort;
	}

	public void setTranslatedPort(String translatedPort) {
		this.translatedPort = translatedPort;
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.toString();
	}
	
}