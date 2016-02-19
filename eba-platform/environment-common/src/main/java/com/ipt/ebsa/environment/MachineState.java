package com.ipt.ebsa.environment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class MachineState implements Cloneable {
	
	private String environmentName;
	private String roleOrFQDN;
	private Map<String, Object> state;
	protected final Set<String> outOfScopeApps;
	
	public MachineState(String environmentName, String roleOrFQDN, Map<String, Object> state) {
		super();
		this.environmentName = environmentName;
		this.roleOrFQDN = roleOrFQDN;
		this.state = state;
		this.outOfScopeApps = new HashSet<>();
	}
	
	protected MachineState(String environmentName, String roleOrFQDN, Map<String, Object> state, Set<String> outOfScopeApps) {
		super();
		this.environmentName = environmentName;
		this.roleOrFQDN = roleOrFQDN;
		this.state = state;
		this.outOfScopeApps = outOfScopeApps;
	}
	
	public String getEnvironmentName() {
		return environmentName;
	}
	public void setEnvironmentName(String environment) {
		this.environmentName = environment;
	}
	public String getRoleOrFQDN() {
		return roleOrFQDN;
	}
	public void setRoleOrFQDN(String roleOrFQDN) {
		this.roleOrFQDN = roleOrFQDN;
	}
	
	public Map<String, Object> getState() {
		return state;
	}
	public void setState(Map<String, Object> state) {
		this.state = state;
	}
	
	public boolean isOutOfScope(String applicationShortName) {
		return outOfScopeApps.contains(applicationShortName);
	}

	public void addOutOfScopeApp(String applicationShortName) {
		outOfScopeApps.add(applicationShortName);
	}
	
	public abstract String getSourceName();
	
	public abstract boolean canBeUpdated();
	
	public abstract MachineState copyOf();
	
	@Override
	public String toString() {
		return String.format("Environment: [%s], role/fqdn: [%s], state: [%s]", this.environmentName, this.roleOrFQDN, this.state);
	}
}
