package com.ipt.ebsa.manage.deploy.comprelease;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.release.XMLApplicationType;
import com.ipt.ebsa.deployment.descriptor.release.XMLPhaseType;
import com.ipt.ebsa.deployment.descriptor.release.XMLReleaseDeploymentDescriptorType;
import com.ipt.ebsa.manage.environment.EnvironmentStateManager;
import com.ipt.ebsa.manage.git.EMGitManager;

/**
 * Handles the construction of phases for a deployment by parsing the composite release
 * deployment descriptor. Creates before/after transitions that cover phase level
 * injections/removals/MCO commands/waits/stops.
 * 
 * Currently assumes that hostnames are defined in the DD in the form <hostOrRole>.<zone>
 * e.g. dbs.st-sit1-cor1 or soatzm01.np-prp1-dazo
 * 
 * @author Dan McCarthy
 *
 */
public class CompositeReleasePhaseBuilder {
	
	private static final Logger LOG = Logger.getLogger(CompositeReleasePhaseBuilder.class);
	/**
	 * 
	 * @param deployment
	 * @return
	 * @throws Exception
	 */
	public Set<CompositeReleasePhase> buildPhases(CompositeReleaseDeployment deployment) throws Exception {
		Set<CompositeReleasePhase> phases = new LinkedHashSet<>();
		
		String environmentName = deployment.getEnvironmentName();
		Organisation org = deployment.getOrganisation();
		EMGitManager git = deployment.getGitManager();
		EnvironmentStateManager state = deployment.getEnvironmentStateManager();
		
		XMLReleaseDeploymentDescriptorType releaseDD = deployment.getReleaseDeploymentDescriptor();
		
		// Application names across the whole DD
		Set<String> allAppShortNames = new HashSet<>();
		
		// Clone the set of application names - the names will be removed from this set as we iterate below
		Set<String> releaseAppShortNames = new LinkedHashSet<>(deployment.getApplicationShortNames());
		
		// Loop through the DD phases
		for (int i = 0; i < releaseDD.getPhase().size(); i++) {
			XMLPhaseType xmlPhase = releaseDD.getPhase().get(i);
			
			LOG.info(String.format("Phase %d: Examining deployment descriptor", i));
			
			// Gather the application short names from the DD for this phase
			Set<String> phaseAppShortNames = new LinkedHashSet<>();
			for (XMLApplicationType xmlApp : xmlPhase.getApplication()) {
				String shortName = xmlApp.getShortName();
				LOG.info(String.format("Phase %d: Found application %s in release deployment descriptor", i, shortName));
				phaseAppShortNames.add(shortName);
				
				// Watch out for duplicate applications across the whole release DD
				if (!allAppShortNames.add(shortName)) {
					throw new IllegalArgumentException(String.format("Phase %d: Application %s appears more than once in the release deployment descriptor - unable to proceed", i, shortName));
				}
			}
			
			// Build the release phase
			CompositeReleasePhase phase = new CompositeReleasePhase(environmentName, org, git, state, xmlPhase);
			for (String phaseAppShortName : phaseAppShortNames) {
				// Remove from cloned list as we go so we can see if any are left at the end
				if (releaseAppShortNames.remove(phaseAppShortName)) {
					// Application in DD and in release
					LOG.info(String.format("Phase %d: Adding application %s to phase", i, phaseAppShortName));
					phase.addApplicationVersion(deployment.getApplicationVersion(phaseAppShortName));
					phase.addDeploymentDescriptor(phaseAppShortName, deployment.getDeploymentDescriptor(phaseAppShortName));
				} else {
					// Application in DD but not in release - error as release is superset
					throw new IllegalArgumentException(String.format("Phase %d: Application %s is in the deployment descriptor but not in the release - unable to proceed", i, phaseAppShortName));
				}
			}
			
			if (!phase.getApplicationShortNames().isEmpty()) {
				LOG.info(String.format("Phase %d: Created for %s", i, phase.getApplicationShortNames()));
				phases.add(phase);
			} else {
				LOG.info(String.format("Phase %d: Skipping empty phase", i));
			}
		}
		// Application names removed from set as we iterated so log any that are left over
		if (!releaseAppShortNames.isEmpty()) {
			// Application in release but not in DD - this is OK as the release is the superset
			LOG.info(String.format("Skipping application(s) %s that are in the release but are not in the deployment descriptor", releaseAppShortNames));
		}
		return phases;
	}
	
	
	
}
