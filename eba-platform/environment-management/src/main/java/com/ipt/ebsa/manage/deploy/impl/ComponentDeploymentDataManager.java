package com.ipt.ebsa.manage.deploy.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Component;
import com.ipt.ebsa.deployment.descriptor.XMLChainBehaviourType;
import com.ipt.ebsa.deployment.descriptor.XMLComponentType;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType.XMLDeploy;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType.XMLUndeploy;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;

/**
 * The ComponentDeploymentData items need to be populated with various facts as we merge everything together from the different sources.
 * This class provides a set of methods which are useful for dealing with the ComponentDeploymentData items. 
 * @author scowx
 *
 */
public class ComponentDeploymentDataManager {

	private static final Logger LOG = Logger.getLogger(ComponentDeploymentDataManager.class); 
	
     /**
     * Null safe way of reaching down to find out what the chain behaviour is for this component.  Returns NULL if nothing has been declared
     * otherwise it returns the ChainBehaviour.
     * @param isUndeploy if we are doing an undeploy
     * @param component
     * @return
     */
	public static XMLChainBehaviourType getChainBehaviour(boolean isUndeploy, ComponentDeploymentData component) {
		XMLComponentType dd = component.getDeploymentDescriptorDef().getXMLType();
		if (dd != null) {
		    XMLHintsType hints = dd.getHints();
		    if (hints != null) {
		    	
		    	if (isUndeploy) {
		    		XMLUndeploy undeploy = hints.getUndeploy();
		    		
		    		if (null != undeploy) {
		    			return undeploy.getChainBehaviour();
		    		}
		    	} else {
		    		XMLDeploy deploy = hints.getDeploy();
		    		
		    		if (null != deploy) {
		    			return deploy.getChainBehaviour();
		    		}
		    	}
		    }			
		}		
		return null;
	}	
	
	/**
	 * Nice toString method for this XML Object
	 * @param XML type
	 * @return
	 */
	public static String toString(XMLComponentType type) {
		if (type == null) {
			return null;
		}		
		else {
		  return "XMLComponentType [yaml=" + normaliseYaml(type.getYaml()) + ", hints=" + type.getHints() + ", minimumPlan=" + type.getMinimumPlan() + ", hostnames=" + type.getHostnames() + ", require=" + type.getRequire() + "]";
		}
	}

	private static String normaliseYaml(String yaml) {
		return yaml == null ? "null" : StringUtils.normalizeSpace(yaml.replaceAll(System.getProperty("line.separator"),""));
	}
	
	/**
	 * Goes through the component descriptor, gets a list of all of the components and adds them into memory.
	 * @param deployment descriptor
	 * @param list of components, populated as we go with additional data from deployment descriptor
	 */
	@SuppressWarnings("unchecked")
	public boolean listComponentsInDeploymentDescriptor(DeploymentDescriptor deploymentDescriptor, Map<ComponentId, ComponentDeploymentData> components) {
		boolean output = true;
		
		String applicationShortName = deploymentDescriptor.getApplicationShortName();
		List<Component> describedComponents = deploymentDescriptor.getComponents();
		
		for (Component component : describedComponents) {
			Yaml yaml = new Yaml();
			Map<String, Object> yamlObject = (Map<String,Object>) yaml.load(component.getXMLType().getYaml());
			Set<Entry<String, Object>> entrySet = yamlObject.entrySet();
			
			//Assume that the components are declared at the top level of the YAML snippet in the XML tag.  Which will mean that there is only a single key and that is for the component.
			Entry<String, Object> componentEntry = entrySet.iterator().next();
			String componentNameStr = componentEntry.getKey();
			Map<String, Object> ddYaml = (Map<String, Object>) yamlObject.get(componentNameStr);
			
			if (null == ddYaml) {
				// could be a key, with no value, this is returned as null, but we want to
				// track that component is in DD, even if it has no yaml.
				ddYaml = new LinkedHashMap<String, Object>();
			}
			
			ComponentId componentId = new ComponentId(componentNameStr, applicationShortName);
			ComponentDeploymentData componentData = getDeploymentDataForComponent(componentId, components, false);			
			
			if (null != componentData) {
				String text = String.format("Component [%s] appears more than once in deployment descriptor", componentId);
				componentData.getChangeSets().add(DifferenceManager.getFailDeploymentActionGroup(componentData, ChangeType.FAIL, text));
				componentData.getExceptions().add(new RuntimeException(text));
				LOG.warn(text);
				output = false;
			}
			
			getDeploymentDataForComponent(componentId, components, true).setDeploymentDescriptorYaml(ddYaml);
			getDeploymentDataForComponent(componentId, components, true).setDeploymentDescriptorDef(component);
			getDeploymentDataForComponent(componentId, components, true).setDeploymentDescriptorParent(deploymentDescriptor);
		}
		
		return output;
	}
	
	/**
	 * Goes through the list of components in the Application Version adding them into the aggregated data in memory
	 * @param application version
	 * @param component list, populated as we go with components from application version
	 */
	public void listComponentsInApplicationVersion(ApplicationVersion applicationVersion, Map<ComponentId, ComponentDeploymentData> components) {
		for (ComponentVersion componentVersion : applicationVersion.getComponents()) {
			String packageName = componentVersion.getRpmPackageName();
			getDeploymentDataForComponent(new ComponentId(packageName, applicationVersion.getApplication().getShortName()), components, true).setTargetComponentVersion(componentVersion);
		}
	}
	
	
	/**
	 * Looks up the component by name.  If it does not find one it creates a new aggregate and sticks it in the map and then returns it.
	 * @param unique component Id
	 * @param list of components
	 * @param create if not found
	 * @return component data, or null if not found and not creating
	 */
	private ComponentDeploymentData getDeploymentDataForComponent(ComponentId componentName, Map<ComponentId, ComponentDeploymentData> components, boolean createIfNotExists) {
		ComponentDeploymentData componentDeploymentData = components.get(componentName);
		if (componentDeploymentData == null && createIfNotExists) {
			componentDeploymentData = new ComponentDeploymentData(componentName);
			components.put(componentName, componentDeploymentData);
		}
		
		return componentDeploymentData;
	}
}
