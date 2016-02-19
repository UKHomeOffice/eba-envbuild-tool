package com.ipt.ebsa.environment.data.model;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentDefinitionType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;

public class EnvironmentDetailsHolder {
	
	String environmentContainerName;
	String environmentProvider;
	String vmcName;
	String domain;
	String version;
	String environmentName;
	String environmentGroupName;
	String buildReferenceid;
	
	XMLVirtualMachineContainerType vmc;
	XMLEnvironmentDefinitionType environmentDefinition;
	XMLEnvironmentType environment;
	XMLEnvironmentContainerType environmentContainer;
	XMLGeographicContainerType geographicContainer;
	
	public String getEnvironmentContainerName() {
		return environmentContainerName;
	}
	public void setEnvironmentContainerName(String environmentContainerName) {
		this.environmentContainerName = environmentContainerName;
	}
	public String getEnvironmentProvider() {
		return environmentProvider;
	}
	public void setEnvironmentProvider(String environmentProvider) {
		this.environmentProvider = environmentProvider;
	}
	public String getVmcName() {
		return vmcName;
	}
	public void setVmcName(String vmcName) {
		this.vmcName = vmcName;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getVersion() {
		return version;
	}
	public String getEnvironmentName() {
		return environmentName;
	}
	public void setEnvironmentName(String envName) {
		this.environmentName = envName;
	}
	public String getEnvironmentGroupName() {
		return environmentGroupName;
	}
	public void setEnvironmentGroupName(String environmentGroupName) {
		this.environmentGroupName = environmentGroupName;
	}
	public String getBuildReferenceid() {
		return buildReferenceid;
	}
	public void setBuildReferenceid(String buildReferenceid) {
		this.buildReferenceid = buildReferenceid;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public XMLVirtualMachineContainerType getVmc() {
		return vmc;
	}
	public void setVmc(XMLVirtualMachineContainerType vmc) {
		this.vmc = vmc;
	}
	public XMLEnvironmentDefinitionType getEnvironmentDefinition() {
		return environmentDefinition;
	}
	public void setEnvironmentDefinition(XMLEnvironmentDefinitionType envDef) {
		this.environmentDefinition = envDef;
	}
	public XMLEnvironmentType getEnvironment() {
		return environment;
	}
	public void setEnvironment(XMLEnvironmentType environment) {
		this.environment = environment;
	}
	public XMLEnvironmentContainerType getEnvironmentContainer() {
		return environmentContainer;
	}
	public void setEnvironmentContainer(XMLEnvironmentContainerType environmentContainer) {
		this.environmentContainer = environmentContainer;
	}
	public XMLGeographicContainerType getGeographicContainer() {
		return geographicContainer;
	}
	public void setGeographicContainer(XMLGeographicContainerType geographicContainer) {
		this.geographicContainer = geographicContainer;
	}

}
