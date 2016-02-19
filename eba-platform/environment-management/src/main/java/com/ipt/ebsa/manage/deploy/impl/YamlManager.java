package com.ipt.ebsa.manage.deploy.impl;

import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.hiera.HieraData;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.NodeMissingBehaviour;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.environment.EnvironmentStateManager;
import com.ipt.ebsa.yaml.YamlInjector;
import com.ipt.ebsa.yaml.YamlUtil;

/**
 * This class provides functionality for making sensible updates to the YAML based on the deployment instructions.
 *  
 * @author scowx
 *
 */
public class YamlManager {

	private static final Logger LOG = LogManager.getLogger(YamlManager.class);
	
	/**
	 * Updates the YAML file with the values specified at the path specified,
	 * application short name added for context.
	 * 
	 * @param hieraFile
	 * @param path
	 * @param value
	 * @param missingBehaviour
	 * @param applicationShortName
	 * @return
	 * @throws Exception
	 */
	public HieraEnvironmentUpdate updateYaml(MachineState state, String path, Object value, NodeMissingBehaviour missingBehaviour, String applicationShortName, String zoneName) throws Exception {
		YamlInjector injector = new YamlInjector();
		final HieraEnvironmentUpdate update = injector.inject(state.getState(), path, value, missingBehaviour, applicationShortName, zoneName);
		update.setSource(state);
		return update;
	}
	
	/**
	 * Updates the YAML file with the values specified at the path specified,
	 * application short name added for context.
	 * 
	 * @param hieraFile
	 * @param path
	 * @param value
	 * @param missingBehaviour
	 * @param applicationShortName
	 * @return
	 * @throws Exception
	 */
	public List<HieraEnvironmentUpdate> updateYamlWithBlock(MachineState state, String path, String yamlBlock, NodeMissingBehaviour missingBehaviour, String applicationShortName, String zoneName) throws Exception {
		YamlInjector injector = new YamlInjector();
		return injector.updateYamlWithBlock(state, path, yamlBlock, missingBehaviour, applicationShortName, zoneName);
	}

	/**
	 * Removes YAML
	 * 
	 * @param hieraFile 
	 * @param path
	 * @param applicationShortName
	 * @return update, or null if no update needed
	 */
	public HieraEnvironmentUpdate removeYaml(MachineState state, String path, String applicationShortName, String zoneName) {
		YamlInjector injector = new YamlInjector();
		HieraEnvironmentUpdate update = injector.remove(path, state.getState(), applicationShortName, zoneName);

		update.setSource(state);
		return update;
	}

	/**
	 * Executes the YAML updates and returns a YAMLUpdate result.   If no updates were made then the return value will be null.
	 * @param component
	 * @param action
         * @param hieraFile
	 * @param yamlToUpdate
	 * @return
	 * @throws Exception
	 */
	public HieraEnvironmentUpdate updateYaml(ComponentDeploymentData component, ChangeType action, MachineState hieraFile) throws Exception {
		YamlInjector injector = new YamlInjector();
		String componentPath = HieraData.HIERA_SYSTEM_PACKAGES +"/"+component.getComponentName();
		String versionPath = componentPath + "/" + HieraData.ENSURE;
		
		ComponentVersion targetComponentVersion = component.getTargetCmponentVersion();
		String componentVersion;
		if (null != targetComponentVersion) {
			componentVersion = targetComponentVersion.getRpmPackageVersion();
		} else {
			componentVersion = null;
		}
		
		HieraEnvironmentUpdate result = null;
		switch (action) {
			case NO_CHANGE:
				LOG.debug("No change will be made to component '"+component.getComponentName()+"' in file '"+hieraFile.getSourceName()+"'");
			    break;
			case DOWNGRADE:
			case UPGRADE: 
			    result = changeExistingYAML(component, action, hieraFile, injector, componentPath, versionPath, componentVersion);
				break;
			case DEPLOY:
				// There may be yaml for another host, and yet we need to deploy
				// so is there any yaml existing for this host or are we adding
				if (isStateFoundForThisComponent(component, hieraFile)) {
					result = changeExistingYAML(component, action, hieraFile, injector, componentPath, versionPath, componentVersion);
				}
				else {
				     result = addNewYaml(component, action, hieraFile, injector, componentPath, componentVersion);
				}
				break;
			case UNDEPLOY: 
				LOG.debug( action + " change will be made to existing yaml for '"+component.getComponentName()+"' in file '"+hieraFile.getSourceName()+"'");
				result = injector.inject(hieraFile.getState(), versionPath, HieraData.ABSENT, NodeMissingBehaviour.FAIL, component.getApplicationShortName(), hieraFile.getEnvironmentName());
				break;
			case FIX:
				result = changeExistingYAML(component, action, hieraFile, injector, componentPath, versionPath, componentVersion);
				break;
			case FAIL:
				break;								
			default:
		}
		if (result != null) {
			result.setComponentName(component.getComponentName());
			result.setSource(hieraFile);
		}
		return result;
	}

	private HieraEnvironmentUpdate addNewYaml(ComponentDeploymentData component, ChangeType action, MachineState state, YamlInjector injector, String componentPath,
			String version) throws Exception {
		HieraEnvironmentUpdate result;
		LOG.debug( action + " change will be made to yaml for '"+component.getComponentName()+"' in file '"+state.getSourceName()+"'.  We will be adding the entire descriptor.");
		///We get a fresh copy of the raw YAML from the descriptor
		Yaml y = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, Object> rawYaml = (Map<String, Object>)y.load(new StringReader(component.getDeploymentDescriptorDef().getXMLType().getYaml()));
		@SuppressWarnings("unchecked")
		Map<String, Object> yaml = (Map<String, Object>)rawYaml.get(component.getComponentName());
		if (yaml == null) {
			yaml = new TreeMap<String,Object>();
		} else {
			yaml = new TreeMap<String, Object>(yaml);
		}
		//insert the version number into it
		injector.inject(yaml, HieraData.ENSURE, version, NodeMissingBehaviour.INSERT_ALL, component.getApplicationShortName(), state.getEnvironmentName());
						
		//inject the updated copy into the YAML file
		result = injector.inject(state.getState(), componentPath, yaml, NodeMissingBehaviour.INSERT_ALL, component.getApplicationShortName(), state.getEnvironmentName());
		result.setComponentName(component.getComponentName());
		return result;
	}

	private HieraEnvironmentUpdate changeExistingYAML(ComponentDeploymentData component, ChangeType action, MachineState state, YamlInjector injector, String componentPath, String versionPath,
			String version) throws Exception {
		LOG.debug( action + " change will be made to existing yaml for '"+component.getComponentName()+"'");
		
		Map<String, Object> existingYamlSnippetOriginal = YamlUtil.getMapAtPath(state.getState(), componentPath);
		
		Map<String, Object> existingYamlSnippet = YamlUtil.deepCopyOfYaml(existingYamlSnippetOriginal);
		existingYamlSnippet.remove(HieraData.ENSURE);

		HieraEnvironmentUpdate result;
		if (YamlUtil.deepCompareYaml(existingYamlSnippet, component.getDeploymentDescriptorYaml())) {
			LOG.debug("No differences between deployment descriptor YAML and existing YAML (excluding ensure)");
			result = injector.inject(state.getState(), versionPath, version, NodeMissingBehaviour.INSERT_KEY_AND_VALUE_ONLY, component.getApplicationShortName(), state.getEnvironmentName());
		} else {
			LOG.debug("There are differences between deployment descriptor YAML and existing YAML (excluding ensure)");
			Map<String, Object> deploymentDescriptorYaml = YamlUtil.deepCopyOfYaml(component.getDeploymentDescriptorYaml());
			deploymentDescriptorYaml.put(HieraData.ENSURE, version);
			result = injector.inject(state.getState(), componentPath, deploymentDescriptorYaml, NodeMissingBehaviour.INSERT_ALL, component.getApplicationShortName(), state.getEnvironmentName());
		}
		result.setComponentName(component.getComponentName());
		return result;
	}
	
	/**
	 * Searches the existing YAML for the specified zone to find all the relevant snippets of yaml which relate to the 
	 * components in the map passed in
	 */
	public void findComponentsInStateManager(EnvironmentStateManager envState, EnvironmentStateManager altEnvState, Map<ComponentId, ComponentDeploymentData> components) {
		LOG.debug("Calling findComponentsInStateManager");
		if (altEnvState == null) {
			LOG.warn("Alternate Environment State Manager is null");
		}
		
		for (Entry<ComponentId, ComponentDeploymentData> entry : components.entrySet()) {
			LOG.debug("Searching for occurrences of component '" + entry.getKey()+"' in the environment state.");
			
			ComponentDeploymentData componentData = entry.getValue();
			
			if (componentData.getHosts() != null) {
				// Gather unique zones for this component
				Set<String> uniqueZones = new LinkedHashSet<>();
				for (ResolvedHost host : componentData.getHosts()) {
					uniqueZones.add(host.getZone());
				}
					
				List<StateSearchResult> componentOccurrences = envState.findComponent(entry.getKey().getComponentName(), uniqueZones);
				componentData.setExistingState(componentOccurrences);
					
				LOG.debug(String.format("Found %s occurrences of component '%s' in the environment state.", (componentOccurrences != null ? componentOccurrences.size() : "no"), entry.getKey()));
					
				if (altEnvState != null) {
					LOG.debug("Searching for occurrences of component '" + entry.getKey() + "' in the alternative environment state.");
					componentOccurrences = altEnvState.findComponent(entry.getKey().getComponentName(), uniqueZones);
					componentData.setExistingMCOComponentState(componentOccurrences);
						
					LOG.debug(String.format("Found %s occurrences of component '%s' in the alternative environment state.", (componentOccurrences != null ? componentOccurrences.size() : "no"), entry.getKey()));
				}
			}
		}
	}
	
	public static boolean isStateFoundForThisComponent(ComponentDeploymentData component, MachineState state) {
		if (component.getExistingState() == null) {
			return false;
		}
		
		String roleOrHostToUpdate = state.getRoleOrFQDN();
		String zoneToUpdate = state.getEnvironmentName();
		
		boolean found = false;
		for (StateSearchResult sr :  component.getExistingState()) {
			MachineState source = sr.getSource();
			if (source.getRoleOrFQDN().equals(roleOrHostToUpdate) && source.getEnvironmentName().equals(zoneToUpdate)) {
				found = true;
				break;
			}
		}
		return found;
	}
}
