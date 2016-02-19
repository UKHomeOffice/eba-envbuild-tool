package com.ipt.ebsa.environment.hiera.firewall;
/**
 * Holds the data from one row in the spreadsheet
 * @author James Shepherd
 */
public class ForwardFirewallDetails {

	private String type;
	private String host;
	private String zone;
	private String source;
	private String dest;
	private String port;
	private String protocol;
	private String desc;
	private String relatedZones;
	private String forumSupport;
	private String comments;
	private String ruleNumber;
	private String version;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getZone() {
		return zone;
	}
	public void setZone(String zone) {
		this.zone = zone;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getDest() {
		return dest;
	}
	public void setDest(String dest) {
		this.dest = dest;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
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
	public String getRelatedZones() {
		return relatedZones;
	}
	public void setRelatedZones(String relatedZones) {
		this.relatedZones = relatedZones;
	}
	public String getForumSupport() {
		return forumSupport;
	}
	public void setForumSupport(String forumSupport) {
		this.forumSupport = forumSupport;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public String getRuleNumber() {
		return ruleNumber;
	}
	public void setRuleNumber(String ruleNumber) {
		this.ruleNumber = ruleNumber;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	
	public boolean isVyatta() {
		return FirewallUtil.isHostnameVyatta(getHost());
	}
	
	@Override
	public String toString() {
		return String.format("source=[%s] dest=[%s] port=[%s] host=[%s] zone=[%s] relatedZones=[%s] ruleNumber=[%s]"
				, getSource(), getDest(), getPort(), getHost(), getZone(), getRelatedZones(), getRuleNumber());
	}
}
