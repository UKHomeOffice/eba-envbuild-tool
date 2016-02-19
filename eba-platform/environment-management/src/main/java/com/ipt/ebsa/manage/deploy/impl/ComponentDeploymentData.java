package com.ipt.ebsa.manage.deploy.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Component;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.environment.StateSearchResult;

/**
 * Contains all of the data which we gather about this component for the purposes of this deployment.
 * Aggregates everything into one class for reference later on.
 * @author scowx
 *
 */
public class ComponentDeploymentData {
	
	/**
	 *  Unique ID for this component
	 */
	private final ComponentId componentId;
	
	/**
	 * This is a reference to the in memory copy of the component from the deployment descriptor.
	 */
	private Component deploymentDescriptorDef;
	
	/**
	 * This is a reference deployment descriptor that contains an in memory copy of the XML DD.
	 */
	private DeploymentDescriptor deploymentDescriptorParent;
	
	/**
	 * The resolved deployment targets 
	 */
	private Collection<ResolvedHost> hosts;
	
	/**
	 * This is a reference to the YAML for the component in the deployment descriptor (essentially the template).  Note that in this 
	 * copy in memory the contents are actually one level lower than they are on the XML file (i.e the component name has been stripped out.)
	 * This is for ease of use later.
	 */
	private Map<String, Object> deploymentDescriptorYaml;
	private ComponentVersion targetComponentVersion;
	private ComponentVersion existingComponentVersion;
	private List<StateSearchResult> existingComponentState;
	//TODO: Populate and Use the below.
	private List<StateSearchResult> existingMCOComponentState;
	/**
	 * originalExistingYaml is a copy of the existingYaml. It is never updated by the Deployment Engine.
	 * It is used to obtain the existing version for display on the Deployment Plan (Report.java)
	 * @since EBSAD-9338
	 */
	private List<StateSearchResult> originalExistingComponentState;
	private List<Exception> exceptions = new ArrayList<Exception>();
	/**
	 * There are more than one deployment actions because a component may be described in more than one YAML file
	 * (i.e it is deployed on more than one box. )  If it is on more than one box then there are potentially different versions
	 * on each box and so the deployment actions for each of these might be different.  The actions are in the same 
	 * order as the YAML search results and are the same in number and so we know which deployment action belongs in which Heira file. 
	 */
	private List<ChangeSet> changeSets = new ArrayList<ChangeSet>();
	private List<ComponentDeploymentData> upstreamDependencies = new ArrayList<ComponentDeploymentData>();
	private int maximumDepth = -1;
	private List<ComponentDeploymentData> downstreamDependencies = new ArrayList<ComponentDeploymentData>();
	private TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyGraph;
	
	public ComponentDeploymentData(String componentName, String applicationShortName) {
		this(new ComponentId(componentName, applicationShortName));
	}
	
	public ComponentDeploymentData(ComponentId componentId) {
		this.componentId = componentId;
	}

	@Override
	public String toString() {
		return "ComponentDeploymentData [componentName=" + componentId + "]";
	}

	/**
	 * Sets the maximum depth.  If the depth has already been set at a deeper level then the old depth is kept
	 * otherwise the new depth is used.  It returns the maximum depth that has been set.
	 * @param depth
	 */
	public int setMaximumDepth(int depth) {
		if (depth > maximumDepth) {
			maximumDepth = depth;
		}
		return maximumDepth;
	}

	public int getMaximumDepth() {
		return maximumDepth;
	}

	public Component getDeploymentDescriptorDef() {
		return deploymentDescriptorDef;
	}

	public void setDeploymentDescriptorDef(Component deploymentDescriptorDef) {
		this.deploymentDescriptorDef = deploymentDescriptorDef;
	}

	public Map<String, Object> getDeploymentDescriptorYaml() {
		return deploymentDescriptorYaml;
	}

	public void setDeploymentDescriptorYaml(Map<String, Object> deploymentDescriptorYaml) {
		this.deploymentDescriptorYaml = deploymentDescriptorYaml;
	}

	public ComponentVersion getTargetCmponentVersion() {
		return targetComponentVersion;
	}

	public void setTargetComponentVersion(ComponentVersion targetComponentVersion) {
		this.targetComponentVersion = targetComponentVersion;
	}

	/**
	 * YAML files which this component appears in *before* any changes are made to the zone. This can include
	 * yaml files which aren't referenced in the deployment descriptor. Maybe it should... :$
	 */
	public List<StateSearchResult> getExistingState() {
		return existingComponentState;
	}

	public void setExistingState(List<StateSearchResult> existingState) {
		this.existingComponentState = existingState;
		if (existingState != null && originalExistingComponentState == null) {
			// Store a copy of the existing Yaml for use in the Deployment Plan report (EBSAD-9338)
			originalExistingComponentState = new ArrayList<StateSearchResult>(existingState.size());
			for (StateSearchResult yamlSearchResult : existingState) {
				originalExistingComponentState.add(yamlSearchResult.copyOf());
			}
			originalExistingComponentState = Collections.unmodifiableList(originalExistingComponentState);
		}
	}

	/**
	 * Returns the original existing Yaml (never modified by the Deployment Engine)
	 * @since EBSAD-9338
	 * @return originalExistingYaml
	 */
	public List<StateSearchResult> getOriginalExistingComponentState() {
		return originalExistingComponentState;
	}
	
	public void setExistingMCOComponentState(List<StateSearchResult> existingState) {
		existingMCOComponentState = Collections.unmodifiableList(existingState);
	}
	
	public List<StateSearchResult> getExistingMCOComponentState() {
		return existingMCOComponentState;
	}
	
	public List<ChangeSet> getChangeSets() {
		return changeSets;
	}

	public ComponentVersion getExistingComponentVersion() {
		return existingComponentVersion;
	}

	public void setExistingComponentVersion(ComponentVersion existingComponentVersion) {
		this.existingComponentVersion = existingComponentVersion;
	}

	public ComponentId getComponentId() {
		return componentId;
	}

	public List<Exception> getExceptions() {
		return exceptions;
	}

	public List<ComponentDeploymentData> getUpstreamDependencies() {
		return upstreamDependencies;
	}

	public List<ComponentDeploymentData> getDownstreamDependencies() {
		return downstreamDependencies;
	}
	
	public TreeMap<ComponentId, TreeMap<ComponentId, ?>> getDependencyGraph() {
		return dependencyGraph;
	}

	public void setDependencyGraph(TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyGraph) {
		this.dependencyGraph = dependencyGraph;
	}

	public DeploymentDescriptor getDeploymentDescriptorParent() {
		return deploymentDescriptorParent;
	}

	public void setDeploymentDescriptorParent(
			DeploymentDescriptor deploymentDescriptorParent) {
		this.deploymentDescriptorParent = deploymentDescriptorParent;
	}

	public String getApplicationShortName() {
		return componentId.getApplicationShortName();
	}
	
	public String getComponentName() {
		return componentId.getComponentName();
	}
	
	public Collection<ResolvedHost> getHosts() {
		return hosts;
	}

	public void setHosts(Collection<ResolvedHost> hosts) {
		this.hosts = hosts;
	}

	/**
	 * Unique identifier for a deployed component - 
	 * the combination of component name and application short name
	 * @author Dan McCarthy
	 *
	 */
	public static class ComponentId implements Comparable<ComponentId> {
		
		private static final String PREPEND = " [";
		private static final String APPEND = "]";
		
		private final String componentName;
		private final String applicationShortName;
		
		public ComponentId(String componentName, String applicationShortName) {
			super();
			this.componentName = componentName;
			this.applicationShortName = applicationShortName;
		}

		public String getComponentName() {
			return componentName;
		}

		public String getApplicationShortName() {
			return applicationShortName;
		}

		@Override
		public int compareTo(ComponentId o) {
			return new CompareToBuilder()
			.append(this.componentName, o.componentName)
			.append(this.applicationShortName, o.applicationShortName)
			.toComparison();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null) { return false; }
			if (obj == this) { return true; }
			if (obj.getClass() != getClass()) {
				return false;
			}
			ComponentId rhs = (ComponentId) obj;
			return new EqualsBuilder()
			.append(this.componentName, rhs.componentName)
			.append(this.applicationShortName, rhs.applicationShortName)
			.isEquals();
		}
		
		@Override
		public int hashCode() {
			return new HashCodeBuilder(557, 499)
			.append(this.componentName)
			.append(this.applicationShortName)
			.toHashCode();
		}
		
		@Override
		public String toString() {
			return new StringBuilder()
			.append(componentName)
			.append(PREPEND)
			.append(applicationShortName)
			.append(APPEND)
			.toString();
		}
		
	}

}
