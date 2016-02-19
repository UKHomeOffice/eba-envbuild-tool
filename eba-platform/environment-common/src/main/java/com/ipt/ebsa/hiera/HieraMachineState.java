package com.ipt.ebsa.hiera;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.yaml.YamlUtil;

public class HieraMachineState extends MachineState implements Comparable<HieraMachineState> {

	private File file;
	
	public HieraMachineState(String environmentName, String roleOrFQDN, File file,  Map<String,Object> state) {
		super(environmentName, roleOrFQDN, state);
		this.file = file;
	}
	
	private HieraMachineState(String environmentName, String roleOrFQDN, File file,  Map<String,Object> state, Set<String> outOfScopeApps) {
		super(environmentName, roleOrFQDN, state, outOfScopeApps);
		this.file = file;
	}
	
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	
	public String getSourceName() {
		return this.file.getName();
	}
		
	/**
	 * Does a deep copy of the YamlFile as it is in memory.
	 */
	public HieraMachineState copyOf() {
		Map<String, Object> obj = YamlUtil.deepCopyOfYaml(this.getState());
		
		HieraMachineState f =  new HieraMachineState(this.getEnvironmentName(), this.getRoleOrFQDN(), new File(file.getAbsolutePath()), obj, new HashSet<>(outOfScopeApps));
		return f;
	}
	
	public boolean canBeUpdated(){
		return !this.getFile().exists();
	}

	@Override
	public int compareTo(HieraMachineState o) {
		if (null == getFile()) {
			return 1;
		}
		
		if (null == o.getFile()) {
			return -1;
		}
		
		return getFile().compareTo(o.getFile());
	}
	
	@Override
	public int hashCode() {
		return null == getFile() ? 0 : getFile().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (null == o || ! (o instanceof HieraMachineState)) {
			return false;
		}
		
		HieraMachineState other = (HieraMachineState) o;
		
		return getFile().equals(other.getFile());
	}
	
}
