package com.ipt.ebsa.environment.build.execute;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.ipt.ebsa.environment.build.git.GitMultiplexer;
import com.ipt.ebsa.environment.hiera.HieraFileManager;
import com.ipt.ebsa.util.collection.PlaceholderMap;

/**
 * Data which is passed down the actions tree and built upon by each node. Can be used to 
 * share data between actions, for example, where the same object needs to be updated during
 * preparation or execution of multiple action performers.
 * 
 * @author James Shepherd
 */
public class BuildContext implements Cloneable {

	private PlaceholderMap parameterMap = new PlaceholderMap();
	private PropertyReferences refs = new PropertyReferences();
	
	private static final Logger LOG = Logger.getLogger(BuildContext.class);
	
	/**
	 * @return the environment
	 */
	public String getEnvironment() {
		return refs.environment;
	}
	
	/**
	 * Could be an Environment Version or an Organisation Version, depending on what else is
	 * set on this object
	 * @return the version
	 */
	public String getVersion() {
		return refs.version;
	}
	
	/**
	 * @return the organisation
	 */
	public String getOrganisation() {
		return refs.organisation;
	}

	/**
	 * @param organisation the organisation to set
	 */
	public void setOrganisation(String organisation) {
		refs.organisation = organisation;
	}

	/**
	 * @param environment the environment to set
	 */
	public void setEnvironment(String environment) {
		refs.environment = environment;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		refs.version = version;
	}

	/**
	 * @return the provider
	 */
	public String getProvider() {
		return refs.provider;
	}
	
	/**
	 * 
	 * @param provider the provider to set
	 */
	public void setProvider(String provider) {
		refs.provider = provider;
	}
	
	/**
	 * @return the envDefnXmlPath
	 */
	public String getEnvDefnXmlPath() {
		return refs.envDefnXmlPath;
	}

	/**
	 * @param envDefnXmlPath the envDefnXmlPath to set
	 */
	public void setEnvDefnXmlPath(String envDefnXmlPath) {
		refs.envDefnXmlPath = envDefnXmlPath;
	}

	public File getWorkDir() {
		return refs.workDir;
	}

	public void setWorkDir(File workDir) {
		refs.workDir = workDir;
	}

	public void setGitMultiplexer(GitMultiplexer gitMultiplexer) {
		refs.gitMultiplexer = gitMultiplexer;
	}
	
	public GitMultiplexer getGitMultiplexer() {
		return refs.gitMultiplexer;
	}
	
	public void setHieraFileManager(HieraFileManager hieraFileManager) {
		refs.hieraFileManager = hieraFileManager;
	}
	
	public HieraFileManager getHieraFileManager() {
		return refs.hieraFileManager;
	}
	
	/**
	 * @return
	 */
	public Set<Map.Entry<String, String>> parameterMapEntrySet() {
		return ImmutableSet.copyOf(parameterMap.entrySet());
	}
	
	/**
	 * @param key
	 * @return
	 * @see com.ipt.ebsa.util.collection.PlaceholderMap#containsKey(java.lang.Object)
	 */
	public boolean parameterMapContainsKey(String key) {
		return parameterMap.containsKey(key);
	}

	/**
	 * @param key
	 * @return
	 * @see com.ipt.ebsa.util.collection.PlaceholderMap#get(java.lang.Object)
	 */
	public String parameterMapGet(String key) {
		String string = parameterMap.get(key);
		return string;
	}

	/**
	 * @param key
	 * @param value
	 * @return
	 * @see com.ipt.ebsa.util.collection.PlaceholderMap#put(java.lang.String, java.lang.String)
	 */
	public String parameterMapPut(String key, String value) {
		String put = parameterMap.put(key, value);
		LOG.debug("Putting key [" + key + "] value [" + value + "]");
		return put;
	}

	/**
	 * @param arg0
	 * @see com.ipt.ebsa.util.collection.PlaceholderMap#putAll(java.util.Map)
	 */
	public void parameterMapPutAll(Map<? extends String, ? extends String> arg0) {
		for (Entry<? extends String, ? extends String> entry : arg0.entrySet()) {
			LOG.debug("Putting key [" + entry.getKey() + "] value [" + entry.getValue() + "]");
		}
		parameterMap.putAll(new TreeMap<>(arg0));
	}
	
	/**
	 * 
	 * @param template String with ${placeholder}'s in
	 * @return String with resolved parameters from this context
	 */
	public String substituteParams(String template) {
		String string = parameterMap.resolvePlaceholders(template);
		LOG.debug("Template [" + template +"] resolved to  [" + string + "]");
		return string;
	}
	
	/**
	 * Copy of this context, the intention being that the clone has a child parameterMap added to it
	 * and is passed on to a lower level, leaving this BuildContext unchanged.
	 */
	@Override
	public BuildContext clone() {
		BuildContext newContext = new BuildContext();
		newContext.refs = this.refs;
		newContext.parameterMap = this.parameterMap.clone();
		return newContext;
	}
	
	private static final class PropertyReferences {
		private String environment;
		private String organisation;
		private String version;
		private String provider;
		private String envDefnXmlPath;
		private File workDir;
		private GitMultiplexer gitMultiplexer;
		private HieraFileManager hieraFileManager;
	}
}
