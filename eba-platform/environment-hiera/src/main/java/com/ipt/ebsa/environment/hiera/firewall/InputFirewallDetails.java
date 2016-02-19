/**
 * 
 */
package com.ipt.ebsa.environment.hiera.firewall;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Represents a row in the firewall input rule sheet.
 * @author James Shepherd
 */
public class InputFirewallDetails {
	private String hosts;
	private String zone;
	private String sources;
	private String ports;
	private String protocol;
	private String desc;
	private String version;
	private String comments;
	
	public String getHosts() {
		return hosts;
	}
	public void setHosts(String hosts) {
		this.hosts = hosts;
	}
	public String getZone() {
		return zone;
	}
	public void setZone(String zone) {
		this.zone = zone;
	}
	public String getSources() {
		return sources;
	}
	public void setSources(String sources) {
		this.sources = sources;
	}
	public String getPorts() {
		return ports;
	}
	public void setPorts(String ports) {
		this.ports = ports;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("hosts", getHosts())
				.append("zone", getZone())
				.append("sources", getSources())
				.append("ports", getPorts())
				.append("protocol", getProtocol())
				.append("description", getDesc())
				.append("version", getVersion())
				.append("comments", getComments()).toString();
	}
}
