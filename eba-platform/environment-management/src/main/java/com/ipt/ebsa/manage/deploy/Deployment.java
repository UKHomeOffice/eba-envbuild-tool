package com.ipt.ebsa.manage.deploy;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.environment.EnvironmentStateManager;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.transitions.Transition;

public interface Deployment {
	
	public ApplicationVersion getApplicationVersion(String applicationShortName);
	public ApplicationVersion getApplicationVersion(ComponentDeploymentData componentData);
	public Map<String, ApplicationVersion> getApplicationVersions();
	public DeploymentDescriptor getDeploymentDescriptor(String applicationShortName);
	public DeploymentDescriptor getDeploymentDescriptor(ComponentDeploymentData componentData);
	public Map<String, DeploymentDescriptor> getDeploymentDescriptors();
	
	public String getSchemeName(String applicationShortName);
	public String getSchemeName(ComponentDeploymentData componentData);
	// This actually represents the scope of either schemes or environments
	public Collection<ResolvedHost> getSchemeScope(String applicationShortName);
	public Collection<ResolvedHost> getSchemeScope(ComponentDeploymentData componentData);
	
	public Map<ComponentId, ComponentDeploymentData> getComponents();
	public EnvironmentStateManager getEnvironmentStateManager();
	public EnvironmentStateManager getAlternateEnvironmentStateManager();
	public String getId();
	public TreeMap<ComponentId, TreeMap<ComponentId, ?>> getDependencyChains();
	public Map<String, String> getFailFilesByFQDN();
	public List<Transition> getTransitions();
	public EMGitManager getGitManager();
	public Organisation getOrganisation();
	public String getEnvironmentName();
	public String getDefaultZoneName();
	
	public void setDependencyChains(TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains);
	public void setTransitions(List<Transition> transitions);
	public void setFailFilesByFQDN(Map<String, String> failFilesByFQDN);
	public File getHieraFolder();
	
	public DeploymentStatus getStatus();
}
