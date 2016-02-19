package com.ipt.ebsa.yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

/**
 * Provides some YAML functionality
 * @author scowx
 *
 */
public class YamlUtil {

	private static Logger log = Logger.getLogger(YamlUtil.class);
	
	private static final String SEP_REGEX = "(?<!\\\\)/";

	/**
	* Read YAML files
	* @param yamlFile
	* @param environmentName
	* @param roleOrFQDN
	* @return
	* @throws FileNotFoundException
	*/
	@SuppressWarnings("unchecked")
	public static Map<String, Object> readYaml(File yamlFile) throws FileNotFoundException {
		Yaml yaml = new Yaml();
		Map<String,Object> obj = null;
		if (yamlFile.exists()){
			log.debug("Loading hiera file '"+yamlFile.getAbsolutePath()+"'");
			Reader fileReader = new FileReader(yamlFile);
			try {
				obj = (Map<String,Object>) yaml.load(fileReader);
			} finally {
				IOUtils.closeQuietly(fileReader);
			}
		} else {
			log.warn("Yaml file does not exist '"+yamlFile.getAbsolutePath()+"'");
		}
		if (obj == null) {
			log.warn("No yaml data found in file '"+yamlFile.getAbsolutePath()+"'");
			obj = new TreeMap<String,Object>();
		}
		return obj;
	}
	
	/**
	 * @param yamlFilename
	 * @return role or hostname (substring before the first .)
	 */
	public static String getRoleOrHostFromYaml(String yamlFilename) {
		int index = yamlFilename.indexOf(".");
		if (index != -1) {
			return yamlFilename.substring(0, index);	
		} else {
			return yamlFilename;
		}
	}
	
	/**
	 * @param yamlFilenames
	 * @return set of roles or hostnames (substring before the first .)
	 */
	public static Set<String> getRolesOrHostsFromYaml(Set<String> yamlFilenames) {
		// Maintain insertion order
		Set<String> rolesOrHosts = new LinkedHashSet<>();
		for (String yaml : yamlFilenames) {
			rolesOrHosts.add(getRoleOrHostFromYaml(yaml));
		}
		return rolesOrHosts;
	}
	
    /**
     * Does a deep copy of the YAML by serialising and the deserialising it into a new object tree.
     * @param yaml
     * @return
     */
	public static Map<String, Object> deepCopyOfYaml(Map<String, Object> yaml) {
		/* Serialise it and then materialise it into a brand new map which is an exact copy */
		Yaml snakeYaml = new Yaml();
		StringWriter writer = new StringWriter();
		snakeYaml.dump(yaml, writer);
		@SuppressWarnings("unchecked")
		Map<String,Object> obj = (Map<String,Object>) snakeYaml.load(writer.toString());
		return obj;
	}
	
	public static String[] getKeys(String keyPath) {
		String[] keys = keyPath.split(SEP_REGEX);
		String[] unEscapedKeys = new String[keys.length];
		for (int i = 0; i < keys.length; i++) {
			unEscapedKeys[i] = unEscapeYamlKey(keys[i]);
		}
		
		return unEscapedKeys;
	}
	 
	/**
     * Does a deep compare of the YAML by serialising it into a string.
     * @param yaml
     * @return
     */
	public static boolean deepCompareYaml(Map<String, Object> yaml1, Map<String, Object> yaml2) {
		if (null == yaml1) {
			throw new RuntimeException("first YAML is null");
		}
		
		if (null == yaml2) {
			throw new RuntimeException("second YAML is null");
		}
		
		/* Serialise it and then materialise it into a brand new map which is an exact copy */
		Yaml snakeYaml = new Yaml();
		StringWriter writer1 = new StringWriter();
		StringWriter writer2 = new StringWriter();
		snakeYaml.dump(yaml1, writer1);
		snakeYaml.dump(yaml2, writer2);
		
		return writer1.toString().equals(writer2.toString());
	}
	
	/**
	 * Writes the YAML out to a file with FLOWStye BLOCk, indent of 4, and pretty printed.
	 * NOTE: it does not close the writer, that is up to you.
	 * @param yamlData
	 * @param writer
	 */
	public static void write(Map<String, Object> yamlData, Writer writer) {
		
		try {
			Yaml yaml = new Yaml();
			DumperOptions d = new DumperOptions();
			d.setDefaultFlowStyle(FlowStyle.BLOCK);
			d.setIndent(4);

			yaml = new Yaml(d);
			d.setPrettyFlow(true);
			yaml.dump(yamlData, writer);

		} catch (Exception e1) {
			log.error("Error writing YAML", e1);
		}
	}
	
	/**
	 * 
	 * @param yaml Yaml as Map
	 * @param path / delimited path which should have value a map
	 * @return value map
	 */
	public static Map<String, Object> getMapAtPath(Map<String, Object> yaml, String path) {
		Object value = getObjectAtPath(yaml, path);
		if (!(value instanceof Map<?,?>)) {
			throw new RuntimeException("Value is not a Map at path " + path);
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) value;
		return map;
	}
	
	/**
	 * 
	 * @param yaml Yaml as Map
	 * @param path path / delimited path which should have value a map
	 * @return
	 */
	public static String getStringAtPath(Map<String, Object> yaml, String path) {
		Object value = getObjectAtPath(yaml, path);
		if (!(value instanceof String)) {
			throw new RuntimeException("Value is not a String at path " + path);
		}
		
		return (String) value;
	}		
		
	/**
	 * @param yaml yaml as Map
	 * @param path path / delimited
	 * @return whatever is at the path
	 */
	public static Object getObjectAtPath(Map<String, Object> yaml, String path) {
		String[] keys = path.split("/");
		
		String nextPath = keys[0];
		
		if (keys.length < 1) {
			return yaml;
		} else if (keys.length < 2) {
			return yaml.get(keys[0]);
		}
		
		for (int i = 1; i < keys.length - 1; i++) {
			nextPath += "/" + keys[i];
		}
		
		Object parent = getObjectAtPath(yaml, nextPath);
		
		if (! (parent instanceof Map<?,?>)) {
			throw new RuntimeException("Not map at path: " + nextPath);
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> parentMap = (Map<String, Object>) parent;
		return parentMap.get(keys[keys.length - 1]);
	}
	
	/**
	 * Creates a Map<String, Object> from yaml as a string
	 * @param yaml as String
	 * @return yaml as Map
	 */
	public static Map<String, Object> getYamlFromString(String yamlAsString) {
		Yaml snakeYaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, Object> yamlMap = (Map<String, Object>)snakeYaml.load(yamlAsString);
		yamlMap = escapeKeys(yamlMap);
		return yamlMap;
	}
	
	public static Map<String, Object> escapeKeys(Map<String, Object> yaml) {
		Map<String, Object> escapedYaml = new TreeMap<String, Object>();
		for (String key : yaml.keySet()) {
			String escapedKey = escapeYamlKey(key);
			Object currentObj = yaml.get(key);
			if (currentObj instanceof Map) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Map<String, Object> subMap = (Map)currentObj;
				currentObj = escapeKeys(subMap);
			}
			escapedYaml.put(escapedKey, currentObj);
		}
		return escapedYaml;
	}
	
	/**
	 * @param yaml with escaped keys
	 * @return
	 */
	public static Map<String, Object> flattenYaml(Map<String, Object> yaml) {
		TreeMap<String, Object> flattenedYaml = new TreeMap<String, Object>();
		for (String key : yaml.keySet()) {
			Object o = yaml.get(key);
			if (o instanceof Map){
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Map<String, Object> subMap = flattenYaml((Map)o);
				for (String subKey : subMap.keySet()) {
					String newKey = String.format("%s/%s", key, subKey);
					log.debug(String.format("Flattening: %s, %s", newKey, subMap.get(subKey)));
					flattenedYaml.put(newKey, subMap.get(subKey));
				}
			} else {
				log.debug(String.format("Flattening: %s, %s", key, yaml.get(key)));
				flattenedYaml.put(key, o);
			}
		}
		return flattenedYaml;
	}
	
	public static String escapeYamlKey(String key) {
		log.info("Escaping key: " + key);
		return key.replace("/", "\\/");
	}
	
	public static String unEscapeYamlKey(String key) {
		return key.replace("\\/", "/");
	}
	
	
	/**
	 * Will filter out paths from the given yaml that are not prefixed by one of the
	 * given paths.
	 * @param yaml
	 * @param paths should be escaped. If null or empty, input yaml is returned.
	 * @return
	 */
	public static String filterByPaths(String yaml, Set<String> paths) {
		if (null == paths || paths.size() < 1) {
			return yaml;
		}
		
		if (StringUtils.isBlank(yaml)) {
			return "";
		}
		
		Map<String, Object> filteredByPaths = filterByPaths(paths, YamlUtil.flattenYaml(YamlUtil.getYamlFromString(yaml)));
		
		if (filteredByPaths.isEmpty()) {
			return "";
		}
		
		StringWriter writer = new StringWriter();
		YamlUtil.write(unFlatten(filteredByPaths), writer);
		return writer.toString();
	}

	/**
	 * Will filter out paths from the given yaml that are not prefixed by one of the
	 * given paths.
	 * @param flattenedYaml map of flattened yaml (with escaped keys)
	 * @param paths should be escaped.
	 * @return
	 */
	public static Map<String, Object> filterByPaths(Set<String> paths, Map<String, Object> flattenedYaml) {
		for (String path : paths) {
			log.debug(String.format("Filtering by path [%s]", path));
		}
		
		Map<String, Object> output = new TreeMap<>(flattenedYaml);
		
		for (Iterator<Entry<String,Object>> it = output.entrySet().iterator(); it.hasNext(); ) {
			boolean found = false;
			Entry<String,Object> me = it.next();
			String key = me.getKey();
			
			for (String path : paths) {
				if (key.startsWith(path)) {
					found = true;
					log.debug(String.format("Preserving path [%s]", key));
					break;
				}
			}
			
			if (!found) {
				log.debug(String.format("Removing [%s]", key));
				it.remove();
			}
		}
		
		return output;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> unFlatten(Map<String, Object> flattenedYaml) {
		TreeMap<String, Object> output = new TreeMap<>();
		
		for (Entry<String, Object> me : flattenedYaml.entrySet()) {
			String wholePath = me.getKey();
			String[] pathComponents = YamlUtil.getKeys(wholePath);
			
			TreeMap<String, Object> iBeHere = output;
			
			for (int i = 0; i < pathComponents.length; i++) {
				String key = pathComponents[i];
				
				if (iBeHere.containsKey(key)) {
					Object value = iBeHere.get(key);
					if (i + 1 < pathComponents.length) {
						if (value instanceof Map) {
							iBeHere = (TreeMap<String, Object>) value;
						} else {
							throw new RuntimeException(String.format("Premature end of Map at [%s]", wholePath));
						}
					} else {
						throw new RuntimeException(String.format("Should be at end of Map at [%s]", wholePath));
					}
				} else {
					if (i + 1 < pathComponents.length) {
						TreeMap<String, Object> newMap = new TreeMap<>();
						iBeHere.put(key, newMap);
						iBeHere = newMap;
					} else {
						iBeHere.put(key, me.getValue());
					}
				}
			}
		}
		
		return output;
	}
}
