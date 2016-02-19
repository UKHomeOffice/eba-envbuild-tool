package com.ipt.ebsa.yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.EnvironmentUpdate;
import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.NodeMissingBehaviour;

public class YamlInjector {

	private static Logger LOG = Logger.getLogger(YamlInjector.class);
	
	/**
	 * Assumes the YAML Object is a map, application short name included for context
	 * @param yaml
	 * @param path
	 * @param newValue
	 * @param behaviour
	 * @param applicationShortName
	 * @throws Exception 
	 */
	public HieraEnvironmentUpdate inject(Map<String,Object> yaml, String keyPath, Map<String,Object> newValue, NodeMissingBehaviour behaviour, String applicationShortName, String zoneName) throws Exception {
		HieraEnvironmentUpdate yamlUpdate = new HieraEnvironmentUpdate(applicationShortName, zoneName);
		yamlUpdate.setRequestedPath(keyPath);
		
		// Deep copy the requested value so it can't be modified after it's set!
		if (newValue != null) {
			Map<String, Object> copy = YamlUtil.deepCopyOfYaml(newValue);
			yamlUpdate.setRequestedValue(copy);
		}
		
		yamlUpdate.setNodeMissingBehaviour(behaviour);
		
		if (yaml == null) {
			throw new IllegalArgumentException("Yaml object cannot be null");
		}
		if (StringUtils.isBlank(keyPath)) {
			throw new IllegalArgumentException("Path to object to set in YAML cannot be empty blank or null");
		}
		if (behaviour == null) {
			throw new IllegalArgumentException("NodeMissingBehaviour cannot be null.");
		}

		String[] keys = YamlUtil.getKeys(keyPath);
		findOrCreateAndUpdateNode(yaml, keyPath, keys, 0, newValue, behaviour, yamlUpdate);
		return yamlUpdate;
	}
	
	/**
	 * Applies this YamlUpdate on the Yaml passed in, application short name included for context
	 * @param yaml
	 * @param path
	 * @param newValue
	 * @param behaviour
	 * @param applicationShortName
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public HieraEnvironmentUpdate inject(Map<String,Object> yaml, String keyPath, Object newValue, NodeMissingBehaviour behaviour, String applicationShortName, String zoneName) throws Exception {
		HieraEnvironmentUpdate yamlUpdate = new HieraEnvironmentUpdate(applicationShortName, zoneName);
		yamlUpdate.setRequestedPath(keyPath);
		
		// Deep copy the requested value so it can't be modified after it's set!
		Object copy = newValue;
		if (newValue != null && newValue instanceof Map) {
			copy = YamlUtil.deepCopyOfYaml((Map<String, Object>) newValue);
		}
		
		yamlUpdate.setRequestedValue(copy);
		yamlUpdate.setNodeMissingBehaviour(behaviour);
		
		if (yaml == null) {
			throw new IllegalArgumentException("Yaml object cannot be null");
		}
		if (StringUtils.isBlank(keyPath)) {
			throw new IllegalArgumentException("Path to object to set in YAML cannot be empty blank or null");
		}
		if (behaviour == null) {
			throw new IllegalArgumentException("NodeMissingBehaviour cannot be null.");
		}
		
		String[] keys = YamlUtil.getKeys(keyPath);	
		findOrCreateAndUpdateNode(yaml, keyPath, keys, 0, newValue, behaviour, yamlUpdate);
		return yamlUpdate;
	}
	
	/**
	 * Applies this YamlUpdate on the Yaml passed in 
	 * @param yaml
	 * @param path
	 * @param newValue
	 * @param behaviour
	 * @throws Exception 
	 */
	public EnvironmentUpdate apply(Map<String,Object> yaml, HieraEnvironmentUpdate yamlUpdateToApply) throws Exception {
		if (StringUtils.isNotBlank(yamlUpdateToApply.getPathElementsRemoved())) {
			// TODO: Refactor as new HieraUpdate object is created inside remove()!!
			return remove(yamlUpdateToApply.getPathElementsRemoved(), yaml, yamlUpdateToApply.getApplicationName(), yamlUpdateToApply.getZoneName());
		}
		else {
			// TODO: Refactor as new HieraUpdate object is created inside inject()!!
			Object value = yamlUpdateToApply.getRequestedValue();
			return inject(yaml, yamlUpdateToApply.getRequestedPath(), value , yamlUpdateToApply.getNodeMissingBehaviour(), yamlUpdateToApply.getApplicationName(), yamlUpdateToApply.getZoneName());
		}
	}

	/**
	 * Recursive method to do a search and replace.
	 * @param yaml
	 * @param keys
	 * @param i
	 * @param newValue
	 * @param behaviour
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	private void findOrCreateAndUpdateNode(Map<String, Object> yaml, String keyPath, String[] keys, int i, Object newValue, NodeMissingBehaviour behaviour, HieraEnvironmentUpdate yamlUpdate) throws Exception 
	{
		Object o = yaml.get(keys[i]);
		if (o != null) {
			//This logic is about inserting keys into maps that already exist
			if (o instanceof Map ) {
				if ( i < keys.length-1) {
					//dig down to find the next item
					findOrCreateAndUpdateNode((Map<String, Object>) o, keyPath, keys, i+1, newValue, behaviour, yamlUpdate);
			    }
				else {
					yamlUpdate.setExistingValue(o);
					((Map<String, Object>)yaml).put(keys[i], newValue);
					LOG.debug(String.format("Updating '%s' with '%s' (%s) for key '%s' in path '%s'",yaml,newValue, newValue.getClass().getSimpleName(),keys[i],keyPath));
					LOG.debug(String.format("Result is '%s'",o));
					
				}
			}
			else {
				if ( i < keys.length-1) {
					//We were expecting a map here but we got something else
			    	throw new IllegalArgumentException(String.format("Expected a map of values but path segment '%s' of path '%s' has resolved to an object of type '%s' whose value is '%s'", keys[i], keyPath, o.getClass().getName(), o.toString()));
				}
				else {
					yamlUpdate.setExistingValue(o);
					LOG.debug(String.format("Replacing '%s' with '%s' (%s) for key '%s' in path '%s'",o,newValue, newValue.getClass().getSimpleName(),keys[i],keyPath));
					yaml.put(keys[i], newValue);
				}
			}
		}
		else {
			//This logic is about inserting maps where they don;t exist
			Map<String, Object> newMap = new TreeMap<String, Object>();
			switch (behaviour) {
			case FAIL: throw new Exception(String.format("Cannot find node '%s' from path '%s' in yaml.",keys[i], keyPath));
			case INSERT_ALL:
				 if (i< keys.length-1) {
					 //add this parent
					 yaml.put(keys[i], newMap);
					 yamlUpdate.setPathElementsAdded(yamlUpdate.getPathElementsAdded() == null ? keys[i] : yamlUpdate.getPathElementsAdded() + "/" + keys[i]);
					 LOG.debug(String.format("A: Added new node for key '%s' in path '%s'",keys[i],keyPath));
					 //dig down to find the next item
				     findOrCreateAndUpdateNode(newMap, keyPath, keys, i+1, newValue, behaviour, yamlUpdate);
				 }
				 else if (i==keys.length-1) {
					 yaml.put(keys[i], newValue);
					 yamlUpdate.setPathElementsAdded(yamlUpdate.getPathElementsAdded() == null ? keys[i] : yamlUpdate.getPathElementsAdded() + "/" + keys[i]);
					 LOG.debug(String.format("D: '%s' Adding  '%s' (%s) for key '%s' in path '%s'",i,newValue, (newValue == null ? "null" : newValue.getClass().getSimpleName()),keys[i],keyPath));
				 }
				 break;
			case INSERT_KEY_AND_VALUE_AND_PARENT_MAP_ONLY:
				if ( i == keys.length-2) {
					 //add this parent
					 yaml.put(keys[i], newMap);
					 yamlUpdate.setPathElementsAdded(yamlUpdate.getPathElementsAdded() == null ? keys[i] : yamlUpdate.getPathElementsAdded() + "/" + keys[i]);
					 LOG.debug(String.format("B: Added new node for key '%s' in path '%s'",keys[i],keyPath));
					 findOrCreateAndUpdateNode(newMap, keyPath, keys, i+1, newValue, behaviour, yamlUpdate);
				}
				else if (i == keys.length-1) {
					yaml.put(keys[i], newMap);
					yamlUpdate.setPathElementsAdded(yamlUpdate.getPathElementsAdded() == null ? keys[i] : yamlUpdate.getPathElementsAdded() + "/" + keys[i]);
					LOG.debug(String.format("C: Added new node for key '%s' in path '%s'",keys[i],keyPath));					 
					yaml.put(keys[i], newValue);
					LOG.debug(String.format("C: Adding value '%s' (%s) for key '%s' in path '%s'",newValue, newValue.getClass().getSimpleName(),keys[i],keyPath));
					
				}
				else if (i < keys.length-2 ) {
					throw new Exception(String.format("Cannot find node '%s' from path '%s' in yaml.",keys[i], keyPath));
				}
				break;
			case INSERT_KEY_AND_VALUE_ONLY:
				if (i != keys.length-1) {
					throw new Exception(String.format("Cannot find node '%s' from path '%s' in yaml.",keys[i], keyPath));
				}
				//there is no "else" here as if the map exists (or has been created then it will not fall into this block;
                break;
			}
		}
	}

	/**
	 * @param yamlObj yaml
	 * @param keyPath path to remove
	 * @return actual path removed, null if no path removed
	 */
	@SuppressWarnings("unchecked")
	public String remove(Map<String, Object> yamlObj, String keyPath) {
		if (null == yamlObj) {
			return null;
		}
	
		List<String> keys = Arrays.asList(YamlUtil.getKeys(keyPath));
		LOG.debug(keys);
		ArrayList<Map<String, Object>> ancestors = new ArrayList<Map<String, Object>>();
		ancestors.add(yamlObj);
		
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			Map<String, Object> map = ancestors.get(i); 
			Object element = map.get(key);

			if (null != element && keys.size() - 1 == i) {
				// we have the last element, delete it
				LOG.debug("removing: " + key);
				map.remove(key);
				break;
			}

			if (null == element) {
				LOG.debug("Premature end of path, but no worries, key: '" + key + "' in keys: " + map.keySet());
				break;
			} else if (element instanceof Map<?, ?>) {
				LOG.debug("found path element: " + key);
				ancestors.add((Map<String, Object>) element);
			} else {
				throw new RuntimeException("Premature end of path, should not have value at this level: " + key);
			}
		}
		
		// remove any empty paths
		for (int i = ancestors.size() - 1; i > 0; i--) {
			Map<String, Object> element = ancestors.get(i);
			String key = keys.get(i - 1);
			
			if (element.size() > 0) {
				LOG.debug("Has children, nothing more to for key: " + key);
				return makePath(keys, i);
			}
			
			LOG.debug("removing key '" + key + "' because of no children");
			Map<String, Object> parent = ancestors.get(i - 1);
			parent.remove(key);
		}
		
		return keys.get(0);
	}

	/**
	 * 
	 * @param keys
	 * @param i
	 * @return path formed of the first i segments of keys
	 */
	private String makePath(List<String> keys, int noOfKeys) {
		StringBuilder sb = new StringBuilder(keys.get(0));
		for (int i = 1 ; i <= noOfKeys; i++) {
			sb.append("/");
			sb.append(keys.get(i));
		}
		return sb.toString();
	}
	
	/**
	 * YAML block removed, application short name included for context
	 * @param path to remove
	 * @param yaml
	 * @param applicationShortName
	 * @return update that has removed to the yaml at the path
	 */
	public HieraEnvironmentUpdate remove(String path, Map<String, Object> yaml, String applicationShortName, String zoneName) {
		Map<String, Object> yamlOriginal = YamlUtil.deepCopyOfYaml(yaml);
		String pathRemoved = remove(yaml, path);
		
		LOG.debug("Path removed: '" + pathRemoved + "'");
		
		if (null == pathRemoved) {
			LOG.debug("No yaml to remove");
			return null;
		}
		
		HieraEnvironmentUpdate update = new HieraEnvironmentUpdate(applicationShortName, zoneName);
		update.setExistingPath(pathRemoved);
		update.setExistingValue(YamlUtil.getObjectAtPath(yamlOriginal, pathRemoved));
		update.setPathElementsRemoved(pathRemoved);
		return update;
	}

	/**
	 * The given YAML block is merged with the existing YAML, application short name included for context
	 * @param hieraFile
	 * @param path
	 * @param yamlBlock
	 * @param missingBehaviour
	 * @param applicationShortName
	 * @return
	 * @throws Exception
	 */
	public List<HieraEnvironmentUpdate> updateYamlWithBlock(MachineState state, String path, String yamlBlock, NodeMissingBehaviour missingBehaviour, String applicationShortName, String zoneName)
			throws Exception {
		Map<String, Object> yaml = YamlUtil.getYamlFromString(yamlBlock);
		//Flatten the yaml into individual key/value pairs
		Map<String, Object> flattenedYaml = YamlUtil.flattenYaml(yaml);
		List<HieraEnvironmentUpdate> updates = new ArrayList<HieraEnvironmentUpdate>();
		for (String key : flattenedYaml.keySet()) {
			String flattenedPath = path.isEmpty() ? key : String.format("%s/%s", path, key);
			Object value = flattenedYaml.get(key);
			HieraEnvironmentUpdate update = inject(state.getState(), flattenedPath, value, missingBehaviour, applicationShortName, zoneName);
			update.setSource(state);
			updates.add(update);
		}
		
		return updates;
	}
}
