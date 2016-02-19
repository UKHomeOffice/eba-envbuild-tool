package com.ipt.ebsa.environment.hiera.route;

/**
 * Holds data from a single row in the spreadsheet.
 * @author James Shepherd
 */
public class RouteDetails {
	
	private String zone;
	private String compartment;
	private String dest;
	private String route;
	private String iface;
	private String desc;
	private String relatedZones;
	private String template;
	private String notes;
	private String version;
	
	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public String getCompartment() {
		return compartment;
	}

	public void setCompartment(String compartment) {
		this.compartment = compartment;
	}

	public String getDest() {
		return dest;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
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

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public String toString() {
		return getZone() + ":" + getCompartment() + ":" + getDest() + ":" + getRoute() + ":" + getIface();
	}
}