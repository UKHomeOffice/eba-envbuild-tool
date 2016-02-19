package com.ipt.ebsa.manage.deploy.comprelease;

import java.util.Collection;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.deployment.descriptor.release.XMLPhaseType;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.deploy.DeploymentStatus;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.environment.EnvironmentStateManager;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.transitions.Transition;

/**
 * Class gathers the deployment data for a phase of a composite release deployment,
 * that is, a collection of independent applications that can be combined and
 * deployed together.
 * 
 * @author Dan McCarthy
 *
 */
public class CompositeReleasePhase implements Deployment {

	private final Set<String> applicationShortNames = new LinkedHashSet<>();
	private final EnvironmentStateManager environmentStateManager;
	private EnvironmentStateManager alternateEnvironmentStateManager;
	private final String jobId = UUID.randomUUID().toString();

	private final Map<String, ApplicationVersion> applicationVersions = new LinkedHashMap<>();
	private final Map<String, DeploymentDescriptor> deploymentDescriptors = new LinkedHashMap<>();
	private final Map<String, String> schemes = new LinkedHashMap<>();
	private final Map<String, Collection<ResolvedHost>> schemeScopes = new LinkedHashMap<>();
	private final Map<ComponentId, ComponentDeploymentData> components = new LinkedHashMap<>();
	
	private TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains;
	private List<Transition> transitions;
	private final EMGitManager gitManager;
	private final Organisation organisation;
	private final String environmentName;
	private final XMLPhaseType xmlPhase; // Phase from deployment descriptor
	
	// Any pre or post transitions for this phase
	private List<Transition> beforeTransitions;
	private List<Transition> afterTransitions;
	
	public CompositeReleasePhase(String environmentName, Organisation organisation, EMGitManager gitManager, 
			EnvironmentStateManager environmentStateManager, XMLPhaseType xmlPhase) {
		this.environmentName = environmentName;
		this.organisation = organisation;
		this.gitManager = gitManager;
		this.environmentStateManager = environmentStateManager;
		this.xmlPhase = xmlPhase;
	}

	public Set<String> getApplicationShortNames() {
		return applicationShortNames;
	}

	@Override
	public ApplicationVersion getApplicationVersion(String applicationShortName) {
		return applicationVersions.get(applicationShortName);
	}

	@Override
	public ApplicationVersion getApplicationVersion(
			ComponentDeploymentData componentData) {
		return applicationVersions.get(componentData.getApplicationShortName());
	}

	@Override
	public DeploymentDescriptor getDeploymentDescriptor(
			String applicationShortName) {
		return deploymentDescriptors.get(applicationShortName);
	}

	@Override
	public DeploymentDescriptor getDeploymentDescriptor(
			ComponentDeploymentData componentData) {
		return deploymentDescriptors.get(componentData.getApplicationShortName());
	}

	@Override
	public String getSchemeName(String applicationShortName) {
		return schemes.get(applicationShortName);
	}

	@Override
	public String getSchemeName(ComponentDeploymentData componentData) {
		return schemes.get(componentData.getApplicationShortName());
	}

	@Override
	public Collection<ResolvedHost> getSchemeScope(String applicationShortName) {
		return schemeScopes.get(applicationShortName);
	}

	@Override
	public Collection<ResolvedHost> getSchemeScope(ComponentDeploymentData componentData) {
		return schemeScopes.get(componentData.getApplicationShortName());
	}

	@Override
	public Map<ComponentId, ComponentDeploymentData> getComponents() {
		return components;
	}

	@Override
	public EnvironmentStateManager getEnvironmentStateManager() {
		return environmentStateManager;
	}

	@Override
	public String getId() {
		return jobId;
	}

	@Override
	public TreeMap<ComponentId, TreeMap<ComponentId, ?>> getDependencyChains() {
		return dependencyChains;
	}
	
	@Deprecated
	@Override
	public Map<String, String> getFailFilesByFQDN() {
		throw new UnsupportedOperationException("Fail files should be added at deployment level, not phase level.");
	}

	@Override
	public List<Transition> getTransitions() {
		List<Transition> allTransitions = new ArrayList<Transition>();
		if (transitions != null && !transitions.isEmpty()) {
			if (beforeTransitions != null) {
				allTransitions.addAll(beforeTransitions);
			}
			allTransitions.addAll(transitions);
			if (afterTransitions != null) {
				allTransitions.addAll(afterTransitions);
			}
		}
		return allTransitions;
	}

	@Override
	public EMGitManager getGitManager() {
		return gitManager;
	}

	@Override
	public Organisation getOrganisation() {
		return organisation;
	}

	public void addDeploymentDescriptor(String applicationShortName,
			DeploymentDescriptor deploymentDescriptor) {
		deploymentDescriptors.put(applicationShortName, deploymentDescriptor);
	}

	public void setDependencyChains(
			TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains) {
		this.dependencyChains = dependencyChains;
	}

	public Map<String, DeploymentDescriptor> getDeploymentDescriptors() {
		return deploymentDescriptors;
	}

	public void setTransitions(List<Transition> transitions) {
		this.transitions = transitions;
	}

	public Map<String, ApplicationVersion> getApplicationVersions() {
		return applicationVersions;
	}
	
	public void addApplicationVersion(ApplicationVersion applicationVersion) {
		String applicationShortName = applicationVersion.getApplication().getShortName();
		applicationShortNames.add(applicationShortName);
		applicationVersions.put(applicationShortName, applicationVersion);
	}
	
	@Deprecated
	public void setFailFilesByFQDN(Map<String, String> failFilesByFQDN) {
		throw new UnsupportedOperationException("Fail files should be added at deployment level, not phase level.");
	}

	@Override
	public String getEnvironmentName() {
		return environmentName;
	}

	@Override
	public String getDefaultZoneName() {
		// TODO: This method shouldn't be on the interface?
		return null;
	}

	@Override
	public EnvironmentStateManager getAlternateEnvironmentStateManager() {
		return this.alternateEnvironmentStateManager;
	}
	
	@Override
	public File getHieraFolder() {
		// TODO: This method shouldn't be on the interface?
		return null;
	}
	
	@Override
	public DeploymentStatus getStatus() {
		//If just one transition is in STARTED, then Deployment is STARTED.
		//If one transition is in ERROR, then Deployment is ERROR.
		//If all transitions are COMPLETE, then Deployment is COMPLETE.
		int completedCount = 0;
		for (Transition t : this.getTransitions()) {
			switch (t.getStatus()) {
				case STARTED:
					return DeploymentStatus.STARTED;
				case ERRORED:
					return DeploymentStatus.ERRORED;
				case COMPLETED:
					completedCount++;
				case NOT_STARTED:
					//Nothing to do.
					break;
			}
		}
		if (completedCount == this.getTransitions().size()) {
			return DeploymentStatus.COMPLETED;
		}
		return DeploymentStatus.NOT_STARTED;
	}

	public List<Transition> getBeforeTransitions() {
		return beforeTransitions;
	}

	public void setBeforeTransitions(List<Transition> beforeTransitions) {
		this.beforeTransitions = beforeTransitions;
	}

	public List<Transition> getAfterTransitions() {
		return afterTransitions;
	}

	public void setAfterTransitions(List<Transition> afterTransitions) {
		this.afterTransitions = afterTransitions;
	}

	public XMLPhaseType getXmlPhase() {
		return xmlPhase;
	}
	
	
}
