package com.ipt.ebsa.manage.deploy;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.DDConfigurationLoader;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.deployment.descriptor.XMLEnvironmentType;
import com.ipt.ebsa.deployment.descriptor.XMLZoneType;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.deploy.impl.HostnameResolver;
import com.ipt.ebsa.manage.deploy.record.DeploymentRecord;
import com.ipt.ebsa.manage.deploy.rpmfailfile.RPMFailFileManager;
import com.ipt.ebsa.manage.environment.EnvironmentStateManager;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.transitions.Transition;
import com.ipt.ebsa.manage.util.EnvironmentStateManagerFactory;
import com.ipt.ebsa.manage.util.VersionCompare;

import static com.ipt.ebsa.util.OrgEnvUtil.*;

/**
 * Central container class for all of the facts about a deployment.  This is also a jump off point for the actual execution.
 * Implements the run() method on the basis that it might execute as a Thread in some way.
 * @author scowx
 *
 */
public class ApplicationDeployment implements Deployment {

	private Logger log = LogManager.getLogger(ApplicationDeployment.class);
	
	private final String applicationShortName;
	private final ApplicationVersion applicationVersion;
	
	private String environmentName;
	private String defaultZoneName;
	// Used for YUM and RPM fail file lookup
	private Set<String> zonesInScope = new LinkedHashSet<>();
	
	private DeploymentDescriptor deploymentDescriptor;
	
	/**
	 * Optional, mostly null. The scheme/zone to use when deploying to the zone (from UI).  
	 */
	private String schemeName;
	private Collection<ResolvedHost> schemeScope;
	
	private EnvironmentStateManager environmentStateManager;
	private EnvironmentStateManager alternateEnvironmentStateManager;
	private DeploymentRecord deploymentRecord;
	private DeploymentEngine deploymentEngine;
	private Map<ComponentId, ComponentDeploymentData> components = new TreeMap<>();
	private List<Transition> transitions;
	private TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains; 
	private String jobId = UUID.randomUUID().toString();
	private Organisation organisation;
	private EMGitManager gitManager;
	private Map<String, String> failFilesByFQDN;
		
	public ApplicationDeployment(ApplicationVersion applicationVersion) {
		super();
		this.applicationShortName = applicationVersion.getApplication().getShortName();
		this.applicationVersion = applicationVersion;
	}
	
	/**
	 * 	//Need to detect checking under the job and log accordingly.
	 *  //Version in the deployment plan!!
	 */
	public void run(String zoneOrEnvName, boolean isEnv) throws Exception {
		try {
			
			deploymentRecord = new DeploymentRecord();
			deploymentRecord.setStartTime(new Date());
			deploymentRecord.setStatus("Initialising.");
			
			deploymentRecord.log("Starting deployment " + getId());
		
			
			//Check that we have everything we need
			if (applicationVersion == null) {
				throw new IllegalArgumentException("No ApplicationVersion specified, cannot continue if there is nothing to deploy.");
			}
			
			if (zoneOrEnvName == null || zoneOrEnvName.isEmpty()) {
				throw new IllegalArgumentException("No zoneOrEnvName specified, cannot continue if there is nowhere to deploy to.");
			}
			
			if (deploymentEngine == null) {
				throw new IllegalArgumentException("No deploymentEngine specified, cannot continue if there is nothing to execute the deployment.");
			}
			
			// check organisation is there
			Organisation org = getOrganisation();
			
			//Load the deployment descriptor
			DDConfigurationLoader loader = new DDConfigurationLoader();
			deploymentDescriptor = loader.loadDD(getDeploymentDescriptorFile(applicationShortName), applicationShortName);
			
			// Derive env or zone from UI input
			deriveEnvironmentOrDefaultZone(zoneOrEnvName, isEnv);
			
			//Load the Hieradata
			HashMap<String, Collection<ResolvedHost>> scope = new HashMap<String, Collection<ResolvedHost>>();
			scope.put(deploymentDescriptor.getApplicationShortName(), schemeScope);
			
			//Load the Primary Environment State
			environmentStateManager = EnvironmentStateManagerFactory.getInstanceOfType(Configuration.getPrimaryEnvironmentStateClass(), this, org, zonesInScope, scope);
			
			if (Configuration.getEnableAlternativeEnvironmentState()) {
				//Load the MCO State
				alternateEnvironmentStateManager = EnvironmentStateManagerFactory.getInstanceOfType(Configuration.getAlternativeEnvironmentStateClass(), this, org, zonesInScope, scope);
			}
			
			/* Discover if there are any RPM fail files in the zone */
			if (Configuration.getRPMFailFileReportEnabled(getOrganisation())) {
				Map<String, String> failFilesByFQDN = new RPMFailFileManager().reportFailFiles(getOrganisation(), zonesInScope);
				setFailFilesByFQDN(failFilesByFQDN);
			} else {
				log.info(String.format("RPM fail file report disabled for %s", getOrganisation()));
			}
			
			deploymentRecord.setStatus("Preparing.");	

			boolean continueDeployment = deploymentEngine.prepare();
			if (continueDeployment && !Configuration.isPrepareOnly()) {
				deploymentRecord.setStatus("Finished preparing.");
				deploymentRecord.setStatus("Validating.");	
				deploymentEngine.validate();
				deploymentRecord.setStatus("Finished validating.");
				deploymentRecord.setStatus("Executing.");	
				deploymentEngine.execute();
				deploymentRecord.setStatus("Finished executing.");
			}
			else if (!continueDeployment) {
				throw new Exception("No changes can be made, deployment halted!");
			}
		} catch (Exception e) {
			deploymentRecord.setStatus("Exiting abnormally.");
			deploymentRecord.log(e, "Exception occured");
			e.printStackTrace();
			
			throw e;
		} finally {
			if (null != deploymentEngine) {
				deploymentEngine.cleanup();
			}
		}
	}

	private void deriveEnvironmentOrDefaultZone(String zoneOrEnvName, boolean isEnv) {
		if (deploymentDescriptor.containsEnvironments()) {
			// SS3 style
			// Search through DD envs/zones for a match
			List<XMLEnvironmentType> environments = deploymentDescriptor.getXMLType().getEnvironments().getEnvironment();
			String zoneArg = null;
			
			if (isEnv) {
				// Not currently used but might be supported in future (when front-end changes to pass
				// through an environment rather than a zone
				// UI arg is an environment
				for (XMLEnvironmentType env : environments) {
					String envUCWithOrgPrefix = getZoneOrEnvUCWithOrgPrefix(env.getName());
					if (zoneOrEnvName.equals(envUCWithOrgPrefix)) {
						// Match! UI arg found as an environment in DD
						environmentName = envUCWithOrgPrefix;
						
						// Populate the set of zones that are in scope for this deployment
						populateScopedZones(env.getZone());
						
						break;
					}
				}
				if (environmentName == null) {
					throw new IllegalArgumentException(String.format("No match in DD environments for %s", getZoneOrEnvLCNoOrgPrefix(zoneOrEnvName)));
				}
			} else {
				// UI arg is a zone
				ENVS: for (XMLEnvironmentType env : environments) {
					// Inspect this environment (provided the variant matches, if it's been specified)
					if (schemeName == null || (env.getVariant() != null && env.getVariant().equals(schemeName))) {
						String envUCWithOrgPrefix = getZoneOrEnvUCWithOrgPrefix(env.getName());
						for (XMLZoneType zone : env.getZone()) {
							String zoneWithOrg = getZoneOrEnvUCWithOrgPrefix(zone.getReference());
							
							if (zoneOrEnvName.equals(zoneWithOrg)) {
								// Match! UI arg found as a zone in DD so get environment from parent element
								environmentName = envUCWithOrgPrefix;
								
								zoneArg = zoneOrEnvName;
								
								// Populate the set of zones that are in scope for this deployment
								populateScopedZones(env.getZone());
								
								break ENVS;
							}
						}
					}
				}
				if (environmentName == null) {
					if (schemeName != null) {
						throw new IllegalArgumentException(String.format("Unable to derive environment from zone [%s] with variant [%s]", getZoneOrEnvLCNoOrgPrefix(zoneOrEnvName), schemeName));
					} else {
						throw new IllegalArgumentException(String.format("Unable to derive environment from zone [%s]", getZoneOrEnvLCNoOrgPrefix(zoneOrEnvName)));
					}
				}
			}
			// Env name will be of the form 'HO_IPT_NP_PRP2' or 'IPT_ST_SIT1' 
			schemeScope = HostnameResolver.getEnvironmentScopes(deploymentDescriptor, environmentName, zoneArg, schemeName);
		} else {
			// Old SS2 stylee
			if (isEnv) {
				// Error combination of environment user arg and (old) scheme-style deployment descriptor
				throw new IllegalArgumentException(String.format("Invalid deployment descriptor, unable to derive zone from provided environment", getZoneOrEnvLCNoOrgPrefix(zoneOrEnvName))); 
			} else {
				// Use provided zone as default
				defaultZoneName = zoneOrEnvName;
				zonesInScope.add(zoneOrEnvName);
			}
			// Zone name will be of the form 'HO_IPT_NP_PRP2_DAZO' or 'IPT_ST_SIT1_COR1' 
			schemeScope = HostnameResolver.getSchemeScope(deploymentDescriptor, defaultZoneName, schemeName);
		}
	}

	private void populateScopedZones(List<XMLZoneType> xmlZones) {
		// Populate the set of zones that are in scope for this deployment
		for (XMLZoneType zone : xmlZones) {
			String zoneWithOrg = getZoneOrEnvUCWithOrgPrefix(zone.getReference());
			zonesInScope.add(zoneWithOrg);
		}
	}


	/**
	 *  Uses logic to figure out which Hiera folder to use.
	 *  Rules are:
	 *  if the hieraFolder has been specified then it will use that, otherwise it will use the checkout directory.
	 * 
	 * @return
	 */
	@Override
	public File getHieraFolder() {
		
		String hieraFolder = Configuration.getHieraFolder();
		if (hieraFolder != null) {
			File file = new File(hieraFolder);
			if (! file.exists()){
				throw new RuntimeException("Hiera folder '"+file.getAbsolutePath()+"' specified with property '"+Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER+"' does not exist.");
			}
			return file;
		}
		else {
			try {
				// this is a bit hidden. But it shouldn't be created when we run tests
				// and the if here tells us if we are in a test.
				gitManager = new EMGitManager(jobId, getApplicationVersion(applicationShortName).getRelatedJiraIssue(), organisation);
				gitManager.checkoutHiera();
			} catch (Exception e) {
				throw new RuntimeException("Git failed during making the checkout branch, fatal", e);
			}
			return gitManager.getCheckoutDir();
		}
	}

	/**
	 * Uses logic to figure out which deployment descriptor to use.
	 * Rules are: 
	 * If Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX is not null and exists, use that, otherwise 
	 * search for one in the  EnvironmentDeploymentDescriptors directory
	 * @return
	 */
	private File getDeploymentDescriptorFile(String applicationShortName) {
		
		String deploymentDescriptorFile = Configuration.getDeploymentDescriptorFile();
		if (deploymentDescriptorFile != null ) {
			File file = new File(deploymentDescriptorFile);
			if (! file.exists()){
				throw new RuntimeException("DeploymentDescriptor file '"+file.getAbsolutePath()+"' specified with property '"+Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR+"' does not exist.");
			}
			return file;
		}
		else {
			
			if (!Configuration.isDontCheckoutDeploymentDescriptorsFolder()) {
				log.debug("Checking out deployment descriptor files");
			    EMGitManager.checkoutRemoteEnvironmentDeploymentDescriptorsFiles();
			}
			
			File ddDir = new File(Configuration.getLocalEnvironmentDeploymentDescriptorsCheckoutDir());
			//find the right directory
			String shortName = getApplicationVersion(applicationShortName).getApplication().getShortName();
			String version = getApplicationVersion(applicationShortName).getVersion();
			
			log.debug(String.format("Shortname:%s Version:%s", shortName, version));
			File directory = new File(ddDir, shortName);
			if (!directory.exists()) {
				throw new RuntimeException("Cannot find deployment descriptor sub-directory for applicaitons with a shot name of '"+shortName+"'..  There should be a subfolder of '"+ddDir.getAbsolutePath()+"' with the same name as the application short name of the application being deployed and the deployment descriptors for that application should be in there.");
			}
			File[] possibleFiles = directory.listFiles();
			
			File currentSelectedDD = null;
			
			Map<String, File> orderedDDs = orderDDsByMinVersion(possibleFiles, applicationShortName);
			
			// Loop through DDs is ascending minApplicationVersion order
			for (String minVersion : orderedDDs.keySet()) {
				VersionCompare versionCompare = new VersionCompare();
				log.debug(String.format("Comparing: '%s' '%s'", version, minVersion));
				int result = versionCompare.compare(version, minVersion);
				log.debug(String.format("Compare Result: %s", result));			
				
				if (result < 0) {
					// This version is less than the minimum version supported by the descriptor we are looking at.		
					break;
				} else {
					// This version is supported by the deployment descriptor
					log.debug(String.format("Setting file: %s MinVersion: %s", orderedDDs.get(minVersion).getAbsolutePath(), minVersion));
					currentSelectedDD = orderedDDs.get(minVersion);					
				}
			}			
			
			if (currentSelectedDD == null) {
				throw new RuntimeException("Could not find a suitable deployment descriptor.");
			}
			
			return currentSelectedDD;
		}
	}
	
	private Map<String, File> orderDDsByMinVersion(File[] possibleFiles, String applicationShortName) {
		Map<String, File> ordered = new TreeMap<>(new VersionCompare());
		DDConfigurationLoader loader = new DDConfigurationLoader();
		
		for (File file : possibleFiles) {			
			try {
				DeploymentDescriptor d = loader.loadDD(file, applicationShortName);
				String minVersion = d.getXMLType().getMetadata().getMinApplicationVersion().trim();
				
				if (ordered.containsKey(minVersion)) {
					throw new RuntimeException(String.format("Error parsing deployment descriptor %s. Deployment descriptor %s already exists with minApplicationVersion=%s", file.getAbsolutePath(), ordered.get(minVersion).getAbsolutePath(), minVersion));
				} else {
					ordered.put(minVersion, file);
				}
			} catch (SAXException e) {
				throw new RuntimeException(String.format("Failed to parse file: %s", file.getAbsolutePath()), e);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to parse file: %s", file.getAbsolutePath()), e);
			}
			
		}
		return ordered;
	}
	
	/*
	 * Scheme scope getters and setters
	 */
	public Collection<ResolvedHost> getSchemeScope(String applicationShortName) {
		if (this.applicationShortName.equals(applicationShortName)) {
			return schemeScope;
		}
		return null;
	}

	public Collection<ResolvedHost> getSchemeScope(ComponentDeploymentData componentData) {
		return getSchemeScope(componentData.getApplicationShortName());
	}

	/*
	 * Scheme getters and setters
	 */
	public String getSchemeName(ComponentDeploymentData componentData) {
		return getSchemeName(componentData.getApplicationShortName());
	}
	
	public String getSchemeName(String applicationShortName) {
		if (this.applicationShortName.equals(applicationShortName)) {
			return schemeName;
		}
		return null;
	}
	
	public void setSchemeName(String schemeName) {
		this.schemeName = schemeName;
	}
	
	/*
	 * Application version getters and setters
	 */
	public ApplicationVersion getApplicationVersion() {
		return applicationVersion;
	}
	
	public ApplicationVersion getApplicationVersion(String applicationShortName) {
		if (this.applicationShortName.equals(applicationShortName)) {
			return applicationVersion;
		}
		return null;
	}

	public ApplicationVersion getApplicationVersion(ComponentDeploymentData component) {
		return getApplicationVersion(component.getApplicationShortName());
	}

	/*
	 * Environment state manager data getters and setters
	 */
	public EnvironmentStateManager getEnvironmentStateManager() {
		return environmentStateManager;
	}

	public void setEnvironmentStateManager(EnvironmentStateManager environmentStateManager) {
		this.environmentStateManager = environmentStateManager;
	}
	
	/*
	 * Deployment descriptor getters and setters
	 */
	public DeploymentDescriptor getDeploymentDescriptor(ComponentDeploymentData component) {
		return getDeploymentDescriptor(component.getApplicationShortName());
	}

	public DeploymentDescriptor getDeploymentDescriptor(String applicationShortName) {
		if (this.applicationShortName.equals(applicationShortName)) {
			return deploymentDescriptor;
		}
		return null;
	}

	public void setDeploymentDescriptor(DeploymentDescriptor deploymentDescriptor) {		
		this.deploymentDescriptor = deploymentDescriptor;
	}
	
	/*
	 * Other getters and setters 
	 */
	public Organisation getOrganisation() {
		if (null == organisation) {
			//Load the Organisation with the given orgPrefix
			String orgPrefix = ConfigurationFactory.getConfiguration(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX);
			if (StringUtils.isBlank(orgPrefix)) {
				throw new IllegalArgumentException("No Organisation prefix specified (" + Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX + ")");
			}
			organisation = ConfigurationFactory.getOrganisations().get(orgPrefix);
			if (organisation == null) {
				throw new IllegalArgumentException("No Organisation (" + ConfigurationFactory.ORGANISATIONS + ") found with prefix: " + orgPrefix);
			}
		}
		
		return organisation;
	}
	
	public DeploymentRecord getDeploymentRecord() {
		return deploymentRecord;
	}
	
	public void setDeploymentRecord(DeploymentRecord deploymentRecord) {
		this.deploymentRecord = deploymentRecord;
	}

	public DeploymentEngine getDeploymentEngine() {
		return deploymentEngine;
	}

	public void setDeploymentEngine(DeploymentEngine deploymentEngine) {
		this.deploymentEngine = deploymentEngine;
	}
	
	public List<Transition> getTransitions() {
		return transitions;
	}

	public Map<ComponentId, ComponentDeploymentData> getComponents() {
		return components;
	}
	
	public TreeMap<ComponentId, TreeMap<ComponentId, ?>> getDependencyChains() {
		return dependencyChains;
	}

	public void setDependencyChains(TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains) {
		this.dependencyChains = dependencyChains;
	}

	public void setTransitions(List<Transition> transitions) {
		this.transitions = transitions;
	}
	
	public String getId() {
		return jobId;
	}
	
	public EMGitManager getGitManager() {
		return gitManager;
	}

	public void setGitManager(EMGitManager git) {
		gitManager = git;
	}

	public Map<String, String> getFailFilesByFQDN() {
		return failFilesByFQDN;
	}

	public void setFailFilesByFQDN(Map<String, String> failFilesByFQDN) {
		this.failFilesByFQDN = failFilesByFQDN;
	}
	
	public String getApplicationShortName() {
		return applicationShortName;
	}

	@Override
	public Map<String, ApplicationVersion> getApplicationVersions() {
		return buildMap(applicationShortName, applicationVersion);
	}

	private <T> Map<String, T> buildMap(String key, T value) {
		Map<String, T> map = new HashMap<>(1);
		map.put(key, value);
		return map;
	}

	@Override
	public Map<String, DeploymentDescriptor> getDeploymentDescriptors() {
		return buildMap(applicationShortName, deploymentDescriptor);
	}

	@Override
	public String getEnvironmentName() {
		return environmentName;
	}

	@Override
	public String getDefaultZoneName() {
		return defaultZoneName;
	}
	
	public EnvironmentStateManager getAlternateEnvironmentStateManager() {
		return this.alternateEnvironmentStateManager;
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
}
