package com.ipt.ebsa.manage.deploy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Plan;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.transitions.Transition;

public abstract class BaseDeploymentEngine implements DeploymentEngine {
	
	private static final Logger LOG = Logger.getLogger(BaseDeploymentEngine.class);
	
	public static enum PrepareStatus {
		OK, FAIL, NO_CHANGES;
	}
	
	protected PrepareStatus doPrepare(Deployment deployment, String applicationShortName) throws Exception {
		Set<String> set = new HashSet<>(1);
		set.add(applicationShortName);
		return doPrepare(deployment, set);
	}
	
	protected PrepareStatus doPrepare(Deployment deployment, Set<String> applicationShortNames) throws Exception {
		for (String applicationShortName : applicationShortNames) {
			DeploymentDescriptor deploymentDescriptor = deployment.getDeploymentDescriptor(applicationShortName);
			
			/* Add the list of components from the deployment descriptor */
			if (!getDeploymentDataManager().listComponentsInDeploymentDescriptor(deploymentDescriptor, deployment.getComponents())) {
				return PrepareStatus.FAIL;
			}
		}
		
		for (String applicationShortName : applicationShortNames) {
			DeploymentDescriptor deploymentDescriptor = deployment.getDeploymentDescriptor(applicationShortName);
			
			/* Resolve all of the hostnames which will be used in this deployment.  Error if we cannot resolve all of them. */
			boolean hostNamesResolved = getHostnameResolver().doHostnameResolution(deployment, deploymentDescriptor);
			if (!hostNamesResolved) {
				LOG.error("Host resolution failed!! Deployment cannot proceed");
				//throw new RuntimeException("Host resolution failed.  Please see log for more details.");
				return PrepareStatus.FAIL;
			}
		}
		
		for (String applicationShortName : applicationShortNames) {
			ApplicationVersion applicationVersion = deployment.getApplicationVersion(applicationShortName);
			
			/* Add the list of components from the Component Versions */
			getDeploymentDataManager().listComponentsInApplicationVersion(applicationVersion, deployment.getComponents());
		}
		
		/* Search for all of the components in the existing YAML */
		getYamlManager().findComponentsInStateManager(deployment.getEnvironmentStateManager(), deployment.getAlternateEnvironmentStateManager(), deployment.getComponents());
			
		/* 1) Works out what primary actions need to be performed for each component.
		 * 2) Works out if any of the primary actions require secondary actions (i.e. a upgrade might consist of an undeploy then a redeploy) */
		getDifferenceManager().calculateDifferencesAndAssignActions(deployment);
		
		/* 3) Create a series of dependency chains so that we can look at the impact of changes and also orchestrate the deployments */
		TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains = getDependencyManager().resolveDependencyChains(deployment.getComponents());
		deployment.setDependencyChains(dependencyChains);
		
		/* 4) Checkpoint - if there are errors or no changes then we need to stop processing */
		if (!getDifferenceManager().hasChanges(deployment.getComponents())) {
			LOG.info("No changes detected, not continuing");
			return PrepareStatus.NO_CHANGES;
		}
		
		List<Exception> exceptions = getDifferenceManager().getFailures(deployment.getComponents());
		if (exceptions.size() > 0) {
			LOG.error("Deployment Failures Detected:");
			for (Exception e : exceptions) {
				LOG.error(e);
			}
			throw new RuntimeException("Some failures have been detected and these need to be addressed before the deployment can continue.");
		}
		
		/* 5) Go through all deployment actions for all components and decides whether dependents or ancestors 
		 *    also need to be re-deployed as a result of the change.  */
		getDependencyManager().applyDependencyChains(deployment.getDependencyChains(), deployment);
		
		/* 6) Find a plan which matches the deployment actions for the components which have deployment actions */
		Map<String, Plan> selectedPlans = getPlanManager().findBestDeploymentPlans(deployment.getComponents(), deployment.getDeploymentDescriptors());
		
		/* 7) We go through all of the deployment actions, put them into a transition and then do the hiera transforms in memory */
		List<Transition> transitions = getTransitionManager().createTransitions(deployment, selectedPlans, deployment.getDependencyChains(), deployment.getApplicationVersions());
		deployment.setTransitions(transitions);
		
		/* 8) Work out what rpms need to be in the repo */
		if (Configuration.getYumRepoUpdateEnabled(deployment.getOrganisation())) {
			getYumManager().setComponents(deployment.getComponents());
			getYumManager().createRepoQCsv();
		} else {
			LOG.info("Yum Repo Update disabled for prepare");
		}
		
		return PrepareStatus.OK;
	}

}
