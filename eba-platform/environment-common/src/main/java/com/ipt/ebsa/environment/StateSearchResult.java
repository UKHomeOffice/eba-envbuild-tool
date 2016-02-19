package com.ipt.ebsa.environment;

import java.util.Map;

/**
 * A list of YamlMatches
 * @author scowx
 *
 */
public abstract class StateSearchResult {
	
	private Map<String, Object> componentState;
	private MachineState source;
	
	public void setComponentState(Map<String, Object> e) {
		componentState = e;
	}

	public Map<String, Object> getComponentState() {
		return componentState;
	}
	
	public MachineState getSource() {
		return source;
	}

	public void setSource(MachineState newSource) {
		this.source = newSource;
	}
	
	public String toString() {
		return this.getComponentState() != null ? this.getComponentState().toString() + (source == null ? "" : " from source: [" + source.getSourceName() + "]") : 
							    "No results";
	}
	
	/**
	 * Returns a deep copy of this search result
	 * @since EBSAD-9338
	 * @return
	 */
	public abstract StateSearchResult copyOf();
	
	public abstract String getComponentVersion();
}
