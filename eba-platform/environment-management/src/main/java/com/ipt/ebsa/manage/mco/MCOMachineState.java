package com.ipt.ebsa.manage.mco;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.yaml.YamlUtil;

public class MCOMachineState extends MachineState {

	public MCOMachineState(String environmentName, String roleOrFQDN, Map<String,Object> state) {
		super(environmentName, roleOrFQDN, state);
	}
	
	private MCOMachineState(String environmentName, String roleOrFQDN, Map<String,Object> state, Set<String> outOfScopeApps) {
		super(environmentName, roleOrFQDN, state, outOfScopeApps);
	}

	@Override
	public String getSourceName() {
		return getRoleOrFQDN();
	}

	@Override
	public boolean canBeUpdated() {
		//Don't have the functionality to update MCO state at all.
		return false;
	}

	@Override
	public MCOMachineState copyOf() {
		//We use YamlUtil here as it will take care of most of the issues of deep copying 
		//a Map which contains Maps/Lists/Sets or any other non-cloneable class.
		Map<String, Object> obj = YamlUtil.deepCopyOfYaml(this.getState());
		
		MCOMachineState m =  new MCOMachineState(getEnvironmentName(), getRoleOrFQDN(), obj, new HashSet<>(outOfScopeApps));
		
		return m;
	}

}
