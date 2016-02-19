package com.ipt.ebsa.manage.deploy.comprelease;

import static com.ipt.ebsa.manage.Configuration.FILE_SEPARATOR;
import static com.ipt.ebsa.util.OrgEnvUtil.getZoneOrEnvUCWithOrgPrefix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.DDConfigurationLoader;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.deployment.descriptor.XMLEnvironmentType;
import com.ipt.ebsa.deployment.descriptor.XMLZoneType;
import com.ipt.ebsa.deployment.descriptor.release.XMLReleaseDeploymentDescriptorType;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.deploy.DeploymentEngine;
import com.ipt.ebsa.manage.deploy.DeploymentStatus;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.deploy.impl.report.CompositeReleaseReport;
import com.ipt.ebsa.manage.deploy.impl.report.PlaceholderReport;
import com.ipt.ebsa.manage.deploy.impl.HostnameResolver;
import com.ipt.ebsa.manage.deploy.record.DeploymentRecord;
import com.ipt.ebsa.manage.deploy.rpmfailfile.RPMFailFileManager;
import com.ipt.ebsa.manage.environment.EnvironmentStateManager;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.transitions.Transition;
import com.ipt.ebsa.manage.util.EnvironmentStateManagerFactory;
import com.ipt.ebsa.manage.util.VersionCompare;

/**
 * Composite release deployment that gathers data across a collection of application versions.
 * A deployment is actually split into phases and the data from each phase is added to an object
 * of this class as each phase is prepared.
 * 
 * @author Dan McCarthy
 *
 */
public class CompositeReleaseDeployment implements Deployment {

	private Logger log = LogManager.getLogger(CompositeReleaseDeployment.class);
	
	private final String environmentName;
	// Maintain insertion order
	private final Set<String> applicationShortNames = new LinkedHashSet<>();
	private XMLReleaseDeploymentDescriptorType releaseDeploymentDescriptor;
	
	private Map<String, ApplicationVersion> applicationVersions;
	private Map<String, DeploymentDescriptor> deploymentDescriptors;
	
	/**
	 * Optional. The scheme to use when deploying to the zone.  
	 */
	private Map<String, String> schemes;
	private Map<String, Collection<ResolvedHost>> schemeScopes;
	
	private EnvironmentStateManager environmentStateManager;
	private EnvironmentStateManager alternateEnvironmentStateManager;
	private DeploymentRecord deploymentRecord;
	private DeploymentEngine deploymentEngine;
	private String jobId = UUID.randomUUID().toString();
	private Organisation organisation;
	private EMGitManager gitManager;

	private final List<CompositeReleasePhase> phases = new ArrayList<>();

	private Map<String, String> failFilesByFQDN;

	// Will be populated when the deployment is run
	private Map<String, File> applicationDeploymentDescriptorFiles;

	private final String releaseDeploymentDescriptorFile;

	private String appDDsGitRevisionHash;

	private String releaseDDsGitRevisionHash;
	
	private ReleaseVersion releaseVersion;

	public CompositeReleaseDeployment(ReleaseVersion releaseVersion, String environmentName, String deploymentDescriptorFile) throws Exception {
		super();
		this.environmentName = environmentName;
		this.releaseDeploymentDescriptorFile = deploymentDescriptorFile;
		
		// Load release deployment descriptor
		DDConfigurationLoader loader = new DDConfigurationLoader();
		releaseDeploymentDescriptor = loader.loadReleaseDD(getCompositeDeploymentDescriptorFile(deploymentDescriptorFile));
			
		this.releaseVersion = releaseVersion;
		initApplicationVersions(releaseVersion.getApplicationVersions());
	}
	
	private File getCompositeDeploymentDescriptorFile(String deploymentDescriptorFile) {
		if (!Configuration.isDontCheckoutCompositeDeploymentDescriptorsFolder()) {
			log.debug("Checking out release deployment descriptor files");
		    releaseDDsGitRevisionHash = EMGitManager.checkoutRemoteCompositeDeploymentDescriptorsFiles();
		}
		
		// Prepend file separator if not present
		String fileSep = System.getProperty(FILE_SEPARATOR);
		if (!deploymentDescriptorFile.startsWith(fileSep)) {
			deploymentDescriptorFile = String.format("%s%s", fileSep, deploymentDescriptorFile);
		}
		return new File(String.format("%s%s", Configuration.getLocalCompositeDeploymentDescriptorsCheckoutDir(), deploymentDescriptorFile));
	}

	/**
	 * Deployment descriptors and scheme scopes are derived for each application version before the 
	 * deployment is formally prepared.
	 * @throws Exception
	 */
	public void run() throws Exception {
		try {
			
			deploymentRecord = new DeploymentRecord();
			deploymentRecord.setStartTime(new Date());
			deploymentRecord.setStatus("Initialising.");
			
			deploymentRecord.log("Starting deployment " + getId());
			
			if (!Configuration.isPrepareOnly()) {
				new PlaceholderReport().generateReport(this);
			}
		
			
			//Check that we have everything we need
			if (applicationVersions == null || applicationVersions.isEmpty()) {
				throw new IllegalArgumentException("No ApplicationVersion specified, cannot continue if there is nothing to deploy.");
			}
			
			if (deploymentEngine == null) {
				throw new IllegalArgumentException("No deploymentEngine specified, cannot continue if there is nothing to execute the deployment.");
			}
			
			// check organisation is there
			Organisation org = getOrganisation();
			
			applicationDeploymentDescriptorFiles = loadAndSelectDeploymentDescriptorFiles();
			
			Set<String> zones = new HashSet<>();
			for (String applicationShortName : applicationShortNames) {
				try {
					// Load the deployment descriptor
					DDConfigurationLoader loader = new DDConfigurationLoader();
					DeploymentDescriptor deploymentDescriptor = loader.loadDD(applicationDeploymentDescriptorFiles.get(applicationShortName), applicationShortName);
				
					Set<String> zonesInScope = deriveZonesInScope(deploymentDescriptor);
					zones.addAll(zonesInScope);
					
					addDeploymentDescriptor(applicationShortName, deploymentDescriptor);
				
					Collection<ResolvedHost> envScopes = HostnameResolver.getEnvironmentScopes(deploymentDescriptor, environmentName, null, null);
					addSchemeScope(applicationShortName, envScopes);
				} catch (Exception e) {
					throw new RuntimeException(String.format("Fatal exception whilst loading deployment descriptor for %s", applicationShortName), e);
				}
			}
				
			// Load the Hieradata
			environmentStateManager = EnvironmentStateManagerFactory.getInstanceOfType(Configuration.getPrimaryEnvironmentStateClass(), this, org, zones, schemeScopes);

			if (Configuration.getEnableAlternativeEnvironmentState()) {
				// Load the MCO State
				alternateEnvironmentStateManager = EnvironmentStateManagerFactory.getInstanceOfType(Configuration.getAlternativeEnvironmentStateClass(), this, org, zones, schemeScopes);
			}
			
			deploymentRecord.setStatus("Preparing.");	

			// Discover if there are any RPM fail files in the zones in scope
//			if (Configuration.getRPMFailFileReportEnabled(getOrganisation())) {
				Map<String, String> failFilesByFQDN = new RPMFailFileManager().reportFailFiles(getOrganisation(), zones);
				setFailFilesByFQDN(failFilesByFQDN);
//			} else {
//				log.info(String.format("RPM fail file report disabled for %s", getOrganisation()));
//			}
			
			boolean continueDeployment = deploymentEngine.prepare();
			if (continueDeployment && !Configuration.isPrepareOnly()) {
				deploymentRecord.setStatus("Finished preparing.");
				deploymentRecord.setStatus("Validating.");	
				deploymentEngine.validate();
				deploymentRecord.setStatus("Finished validating.");
				deploymentRecord.setStatus("Executing.");	
				deploymentEngine.execute();
				deploymentRecord.setStatus("Finished executing.");
				deploymentRecord.setEndTime(new Date());
				try {
					log.info("Writing out report status after execution");
					new CompositeReleaseReport(true, true).generateReport(this);
				} catch (Exception e) {
					log.error("Failed to dump the report", e);
				}
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
			deploymentRecord.setEndTime(new Date());
			if (!Configuration.isPrepareOnly()) {
				try {
					log.info("Writing out final report status");
					new CompositeReleaseReport(true, true).generateReport(this);
				} catch (Exception e) {
					log.error("Failed to dump the report", e);
				}
			}
			if (null != deploymentEngine) {
				deploymentEngine.cleanup();
			}
		}
	}
	
	private Set<String> deriveZonesInScope(DeploymentDescriptor deploymentDescriptor) {
		if (deploymentDescriptor.getXMLType().getEnvironments() == null || deploymentDescriptor.getXMLType().getEnvironments().getEnvironment() == null) {
			throw new IllegalArgumentException("No environments defined in deployment descriptor. Unable to proceed");
		}
		
		Set<String> zonesInScope = new LinkedHashSet<>();	
		
		// Search through DD envs/zones for a match
		List<XMLEnvironmentType> environments = deploymentDescriptor.getXMLType().getEnvironments().getEnvironment();
		for (XMLEnvironmentType env : environments) {
			String envUCWithOrgPrefix = getZoneOrEnvUCWithOrgPrefix(env.getName());
			if (environmentName.equals(envUCWithOrgPrefix)) {
				// Populate the set of zones that are in scope for this deployment
				for (XMLZoneType zone : env.getZone()) {
					String zoneWithOrg = getZoneOrEnvUCWithOrgPrefix(zone.getReference());
					zonesInScope.add(zoneWithOrg);
				}
			}
		}
		
		return zonesInScope;
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
				StringBuilder sb = new StringBuilder();
				for (String applicationShortName : applicationShortNames) {
					if (sb.length() > 0) {
						sb.append(",");
					}
					sb.append(getApplicationVersion(applicationShortName).getRelatedJiraIssue());
				}
				gitManager = new EMGitManager(jobId, sb.toString(), organisation);
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
	 * If Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA is set, use that, otherwise 
	 * checkout from Git
	 * @return
	 */
	private Map<String, File> loadAndSelectDeploymentDescriptorFiles() {
		if (!Configuration.isDontCheckoutDeploymentDescriptorsFolder()) {
			log.debug("Checking out deployment descriptor files");
		    appDDsGitRevisionHash = EMGitManager.checkoutRemoteEnvironmentDeploymentDescriptorsFiles();
		}
		
		File ddDir = new File(Configuration.getLocalEnvironmentDeploymentDescriptorsCheckoutDir());
		
		Map<String, File> deploymentDescriptorFiles = new LinkedHashMap<>(); 
			
		for (String shortName : getApplicationShortNames()) {
			//find the right directory
			String version = getApplicationVersion(shortName).getVersion();
			
			log.debug(String.format("Shortname:%s Version:%s", shortName, version));
			File directory = new File(ddDir, shortName);
			if (!directory.exists()) {
				throw new RuntimeException(String.format("Cannot find deployment descriptor sub-directory for applications with a shot name of '%s'..  There should be a subfolder of '%s' with the same name as the application short name of the application being deployed and the deployment descriptors for that application should be in there.", shortName, ddDir.getAbsolutePath()));
			}
			File[] possibleFiles = directory.listFiles();
			
			File currentSelectedDD = null;
			
			Map<String, File> orderedDDs = orderDDsByMinVersion(possibleFiles, shortName);
			
			// Loop through DDs is ascending minApplicationVersion order
			for (String minVersion : orderedDDs.keySet()) {
				VersionCompare versionCompare = new VersionCompare();
				log.debug(String.format(String.format("%s: Comparing: '%s' '%s'", shortName, version, minVersion)));
				int result = versionCompare.compare(version, minVersion);
				log.debug(String.format(String.format("%s: Compare Result: %s", shortName, result)));			
				
				if (result < 0) {
					// This version is less than the minimum version supported by the descriptor we are looking at.		
					break;
				} else {
					// This version is supported by the deployment descriptor
					log.debug(String.format(String.format("%s: Setting file: %s MinVersion: %s", shortName, orderedDDs.get(minVersion).getAbsolutePath(), minVersion)));
					currentSelectedDD = orderedDDs.get(minVersion);					
				}
			}			
			
			if (currentSelectedDD == null) {
				throw new RuntimeException(String.format("%s: Could not find a suitable deployment descriptor.", shortName));
			}
			
			deploymentDescriptorFiles.put(shortName, currentSelectedDD);
		}
		
		return deploymentDescriptorFiles;
	}
	
	public String getappDDsGitRevisionHash() {
		return appDDsGitRevisionHash;
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
	
	// Scheme scope getters and setters
	public Collection<ResolvedHost> getSchemeScope(String applicationShortName) {
		return getSchemeScopes().get(applicationShortName);
	}

	public Collection<ResolvedHost> getSchemeScope(ComponentDeploymentData componentData) {
		return getSchemeScopes().get(componentData.getApplicationShortName());
	}

	private Map<String, Collection<ResolvedHost>> getSchemeScopes() {
		if (schemeScopes == null) {
			// Maintain insertion order
			schemeScopes = new LinkedHashMap<>();
		}
		return schemeScopes;
	}

	private void addSchemeScope(String applicationShortName, Collection<ResolvedHost> schemeScope) {
		getSchemeScopes().put(applicationShortName, schemeScope);
	}

	// Scheme getters and setters
	public String getSchemeName(ComponentDeploymentData componentData) {
		return getSchemes().get(componentData.getApplicationShortName());
	}
	
	public String getSchemeName(String applicationShortName) {
		return getSchemes().get(applicationShortName);
	}
	
	private Map<String, String> getSchemes() {
		if (schemes == null) {
			// Maintain insertion order
			schemes = new LinkedHashMap<>();
		}
		return schemes;
	}

	// Application version getters and setters
	public ApplicationVersion getApplicationVersion(String applicationShortName) {
		return getApplicationVersions().get(applicationShortName);
	}

	public ApplicationVersion getApplicationVersion(ComponentDeploymentData component) {
		return getApplicationVersion(component.getApplicationShortName());
	}

	public Map<String, ApplicationVersion> getApplicationVersions() {
		if (applicationVersions == null) {
			// Maintain insertion order
			applicationVersions = new LinkedHashMap<>();
		}
		return applicationVersions;
	}
	
	private void initApplicationVersions(List<ApplicationVersion> applicationVersions) {
		for (ApplicationVersion applicationVersion : applicationVersions) {
			String applicationShortName = applicationVersion.getApplication().getShortName();
			getApplicationShortNames().add(applicationShortName);
			getApplicationVersions().put(applicationShortName, applicationVersion);
		}
	}
	
	public ReleaseVersion getReleaseVersion() {
		return this.releaseVersion;
	}
	
	// Environment state manager data getters and setters
	public EnvironmentStateManager getEnvironmentStateManager() {
		return environmentStateManager;
	}

	public void setHieraData(EnvironmentStateManager environmentStateManager) {
		this.environmentStateManager = environmentStateManager;
	}
	
	// Deployment descriptor getters and setters
	public DeploymentDescriptor getDeploymentDescriptor(ComponentDeploymentData component) {
		return getDeploymentDescriptor(component.getApplicationShortName());
	}

	public DeploymentDescriptor getDeploymentDescriptor(String applicationShortName) {
		return getDeploymentDescriptors().get(applicationShortName);
	}

	private void addDeploymentDescriptor(String applicationShortName,
			DeploymentDescriptor deploymentDescriptor) {
		getDeploymentDescriptors().put(applicationShortName, deploymentDescriptor);
	}

	public Map<String, DeploymentDescriptor> getDeploymentDescriptors() {
		if (deploymentDescriptors == null) {
			// Maintain insertion order
			deploymentDescriptors = new LinkedHashMap<>();
		}
		return deploymentDescriptors;
	}

	// Other getters and setters 
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
	
	/**
	 * Loops through the phases of this deployment
	 * building up a global list of transitions.
	 * For each transition any before or after transitions
	 * are inserted in the correct places.
	 */
	public List<Transition> getTransitions() {
		List<Transition> transitions = new ArrayList<>();
		for (CompositeReleasePhase phase : phases) {
			transitions.addAll(phase.getTransitions());
		}
		
		return transitions;
	}
	
	public List<CompositeReleasePhase> getPhases() {
		return phases;
	}

	public Map<ComponentId, ComponentDeploymentData> getComponents() {
		Map<ComponentId, ComponentDeploymentData> combined = new TreeMap<>();
		for (CompositeReleasePhase phase : phases) {
			combined.putAll(phase.getComponents());
		}
		return combined;
	}
	
	public TreeMap<ComponentId, TreeMap<ComponentId, ?>> getDependencyChains() {
		TreeMap<ComponentId, TreeMap<ComponentId, ?>> combined = new TreeMap<>();
		for (CompositeReleasePhase phase : phases) {
			if (phase.getDependencyChains() != null) {
				combined.putAll(phase.getDependencyChains());
			}
		}
		return combined;
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

	public Set<String> getApplicationShortNames() {
		return applicationShortNames;
	}

	public XMLReleaseDeploymentDescriptorType getReleaseDeploymentDescriptor() {
		return releaseDeploymentDescriptor;
	}
	
	@Deprecated
	@Override
	public void setDependencyChains(TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains) {
		throw new UnsupportedOperationException("Dependency chains should be added at phase level, not deployment level.");
	}

	@Override
	public void setFailFilesByFQDN(Map<String, String> failFilesByFQDN) {
		this.failFilesByFQDN = failFilesByFQDN;
	}

	public String getEnvironmentName() {
		return environmentName;
	}

	@Override
	public String getDefaultZoneName() {
		return null;
	}
	
	@Override
	public EnvironmentStateManager getAlternateEnvironmentStateManager() {
		return this.alternateEnvironmentStateManager;
	}

	public void addPhase(CompositeReleasePhase phase) {
		this.phases.add(phase);		
	}
	
	@Deprecated
	@Override
	public void setTransitions(List<Transition> transitions) {
		throw new UnsupportedOperationException("Transitions should be added at phase level, not deployment level.");
	}

	public Map<String, File> getApplicationDeploymentDescriptorFiles() {
		return applicationDeploymentDescriptorFiles;
	}

	/**
	 * @return Relative file path for release DD (reletive to clone git directory).
	 */
	public String getReleaseDeploymentDescriptorRelativeFilePath() {
		return releaseDeploymentDescriptorFile;
	}

	public String getReleaseDDsGitRevisionHash() {
		return releaseDDsGitRevisionHash;
	}
	
	@Override
	public DeploymentStatus getStatus() {
		//If just one transition is in STARTED, then Deployment is STARTED.
		//If one transition is in ERROR, then Deployment is ERROR.
		//If all transitions are COMPLETE, then Deployment is COMPLETE.
		int completedCount = 0;
		for (CompositeReleasePhase p : this.getPhases()) {
			switch (p.getStatus()) {
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
		if (completedCount == this.getPhases().size()) {
			return DeploymentStatus.COMPLETED;
		}
		return DeploymentStatus.NOT_STARTED;
	}

}
