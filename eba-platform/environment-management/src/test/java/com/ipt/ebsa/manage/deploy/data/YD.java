package com.ipt.ebsa.manage.deploy.data;


/**
 * YD stands for YAML data
 * @author scowx
 *
 */
public class YD {
	String componentName;
	String version;
	String role;
	public YD(String componentName, String version, String yamlFile) {
		super();
		this.componentName = componentName;
		this.version = version;
		this.role = yamlFile;
	}
	@Override
	public String toString() {
		return "YD [componentName=" + componentName + ", version=" + version + ", role=" + role + "]";
	}
	
	
}
