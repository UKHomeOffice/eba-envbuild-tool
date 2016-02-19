package com.ipt.ebsa.manage.hiera;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.environment.EnvironmentUtil;
import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.hiera.HieraData;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.hiera.HieraSearch;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.environment.EnvironmentStateManager;
import com.ipt.ebsa.util.OrgEnvUtil;
import com.ipt.ebsa.yaml.YamlUtil;

/**
 * An implementation of EnvironmentStateManager which uses Hiera as its source of information
 * Based on the original HieraData class
 * @author Ben Noble
 *
 */
public class HieraEnvironmentStateManager implements EnvironmentStateManager {

	/**
	 * In the new world, yaml is in the ext dir.
	 */
	public static final String EXT_DIRNAME = "ext";
	
	public static final String ORG_PLACEHOLDER = "$organisation";
	public static final String ENV_PLACEHOLDER = "$environment";
	
	private static Logger log = LogManager.getLogger(HieraEnvironmentStateManager.class);
	
	private Organisation organisation;
	private File hieraFolder;
	// Keyed on zone, e.g. 'IPT_ST_SIT1_COR1'
	private Map<String, Map<String, HieraMachineState>> zoneState = new TreeMap<>();
	private boolean hieraShouldBeCreated;
	
	/*
	 * template for where to create hiera files
	 */
	private String createFilePathTemplate = ORG_PLACEHOLDER + File.separator + 
			ENV_PLACEHOLDER + File.separator + EXT_DIRNAME;

	@Override
	public boolean load(Deployment dep, Organisation org, Set<String> zones, Map<String, Collection<ResolvedHost>> scopes) {
		File folder = dep.getHieraFolder();
		return load(folder, org, zones, scopes);
	}

	public boolean load(File hieraFolder, Organisation org, Set<String> zones, Map<String, Collection<ResolvedHost>> scopes) {
		this.hieraFolder = hieraFolder == null ? null : hieraFolder.getAbsoluteFile();
		this.hieraShouldBeCreated = Configuration.getHieraShouldBeCreated();
		this.organisation = org;
		
		if (this.organisation == null) {
			throw new IllegalArgumentException("Organisation required to load HieraData");
		}
		
		Collection<File> vappFolder = FileUtils.listFiles(this.hieraFolder, new String[]{"yaml"}, true);
		for (File yamlFile : vappFolder) {
			
			String zoneName = HieraEnvironmentStateManager.convertYamlFolderNameToZoneNameFormat(yamlFile, this.organisation);
			
			try {
			      String roleOrFQDN = YamlUtil.getRoleOrHostFromYaml(yamlFile.getName());
			      log.debug("About to read Yaml for '" + zoneName + "' '" + roleOrFQDN + "'");
			      Map<String, Object> obj = YamlUtil.readYaml(yamlFile);
				  HieraMachineState newYamlFile = new HieraMachineState(zoneName, roleOrFQDN, yamlFile, obj);
				  
				  Map<String, HieraMachineState> yamlFilesForEnv = zoneState.get(zoneName);
				  if (yamlFilesForEnv == null) {
					  yamlFilesForEnv = new TreeMap<String, HieraMachineState>();
					  zoneState.put(zoneName, yamlFilesForEnv);
				  }
				  if (scopes != null && !scopes.isEmpty()) {
					  for (Entry<String, Collection<ResolvedHost>> hostsPerApp : scopes.entrySet()) {
						  if (hostsPerApp.getValue() != null && !hostsPerApp.getValue().isEmpty() && !hostsPerApp.getValue().contains(new ResolvedHost(roleOrFQDN, zoneName))) {
							  newYamlFile.addOutOfScopeApp(hostsPerApp.getKey());
							  log.debug("Hiera file out of scope for application [" + hostsPerApp.getKey() + "]");
						  } else {
							  log.debug("Hiera file in scope for application [" + hostsPerApp.getKey() + "]");
						  }
					  }
				  }
				  
				  yamlFilesForEnv.put(roleOrFQDN, newYamlFile);
				
			} catch (Exception e1) {
				log.error(String.format("Error loading '%s'", yamlFile), e1);
			}	
		}
		
		return true;
	}
	
	@Override
	public List<StateSearchResult> findComponent(String componentName, Set<String> zones) {
		List<StateSearchResult> resultsInZones = new ArrayList<>();
		for (String zone : zones) {
			List<StateSearchResult> resultsInZone = findComponent(componentName, zone);
			if (resultsInZone != null) {
				resultsInZones.addAll(resultsInZone);
			}
		}
		return resultsInZones;
	}

	@Override
	public List<StateSearchResult> findComponent(String componentName, String zone) {
		String path = HieraData.HIERA_SYSTEM_PACKAGES + HieraData.SEPARATOR + componentName;
		return find(path, zone);
	}
	
    /**
     * Finds all occurrences of this path in the hieradata for a particular environment
     * @param path
     * @return
     */
	private List<StateSearchResult> find(String path, String zone) {
		List<StateSearchResult> occurrences = new ArrayList<StateSearchResult>();
		Map<String, HieraMachineState> hieraFiles = zoneState.get(zone);
		if (hieraFiles == null) {
			log.debug("Hierafile not found, will list environments.");
			Set<String> keySet = zoneState.keySet();
			for (String string : keySet) {
				log.debug(string);
			}

			log.warn("Cannot find any hieraFiles for environment '"+zone+"'");
			return null;
		}
		find(path, hieraFiles, occurrences);
		if (occurrences.size() < 1) {
			return null;
		}
		else {
			return occurrences;
		}
	}
	
	public Map<String, Map<String, HieraMachineState>> getEnvironments() {
		return zoneState;
	}
	

	/**
	 * Check that the host/role  is in the explicitly defined
	 * scope, and if there's no defined scope, assume the whole environment is in scope.
	 */
	@Override
	public boolean doesRoleOrHostExist(String roleOrHost, String zone) {
		Collection<String> schemeScope = new ArrayList<>();

		Map<String, HieraMachineState> hf = this.getEnvironments().get(zone);
		if (null != hf) {
			Set<Entry<String, HieraMachineState>> entrySet = hf.entrySet();
			for (Map.Entry<String, HieraMachineState> hiera : entrySet) {
				schemeScope.add(hiera.getKey()) ;
			}
		}
		
		return schemeScope.contains(roleOrHost);
	}

	/**
	 * If there is such an environment and such a HieraFile in it then is returned, 
	 * otherwise check if we can create a file and return that, 
	 * otherwise null is returned if the file does not exist and it cannot be created.
	 * @param zone
	 * @param roleOrFQDN
	 * @param hieraShouldBeCreated
	 * @return
	 */
	@Override
	public HieraMachineState getEnvironmentState(String zone, String roleOrFQDN) {
		Map<String, HieraMachineState> hieraFilesForEnvironment = getEnvironments().get(zone);
		if (hieraFilesForEnvironment != null) {
		   HieraMachineState file = hieraFilesForEnvironment.get(roleOrFQDN);
		   if (file == null && hieraShouldBeCreated){
				//Hiera file to be created.
				String path =  hieraFolder + File.separator + createFilePathTemplate + File.separator + 
					       roleOrFQDN;
				path = path.replace(ORG_PLACEHOLDER, OrgEnvUtil.getOrganisationName(zone));
				path = path.replace(ENV_PLACEHOLDER, OrgEnvUtil.getEnvironmentName(zone));
				
				if (!EnvironmentUtil.isRole(roleOrFQDN)) {
					path += "." + OrgEnvUtil.getDomainForPuppet(zone);
				}
				path +=  ".yaml";
				log.debug("Creating Hiera file at Path: " + path);
				file = new HieraMachineState(zone, roleOrFQDN, new File(path), new TreeMap<String, Object>());
				//Add file to environments
				hieraFilesForEnvironment.put(roleOrFQDN, file);
		   }
		   return file;
		}
		else {
			log.debug("No Hiera files found for environment: " + zone);
			throw new IllegalStateException(String.format("No hiera files found for zone %s", zone));
		}
	}

	public static String convertYamlFolderNameToZoneNameFormat(File yamlFile, Organisation organisation) {
		File zoneAncestor = yamlFile.getParentFile();
		
		if (HieraEnvironmentStateManager.EXT_DIRNAME.equals(zoneAncestor.getName())) {
			zoneAncestor = zoneAncestor.getParentFile();
		}
		
		String zonePrefix = ConfigurationFactory.getEnvironmentDefinitionPrefix(organisation);
		if (StringUtils.isBlank(zonePrefix)) {
			throw new IllegalArgumentException("Organisation environmentDefinitionPrefix was empty");
		}
		return zonePrefix + zoneAncestor.getName().toUpperCase().replaceAll("-", "_");
	}
	
	/**
	 * Searches through the supplied list of files (from one environment) and populates the result list
	 * with files which contain the given path.
	 */
	private void find(String path, Map<String, HieraMachineState> files, List<StateSearchResult> occurrences) {
		if (files != null) {
			for (HieraMachineState file : files.values()) {
				HieraSearch search = new HieraSearch();
				StateSearchResult result = search.search(file.getState(), path);
				result.setSource(file);
				if (result.getComponentState() != null ) {
					occurrences.add(result);
				}
			}
		}
	}

}
