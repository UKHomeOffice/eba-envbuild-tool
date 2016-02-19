package com.ipt.ebsa.manage.deploy.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Plan;
import com.ipt.ebsa.deployment.descriptor.XMLPlanType;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;

/**
 * Provides functionality relating to deployment plans
 * 
 * @author scowx
 * 
 */
public class PlanManager {

	private Logger log = LogManager.getLogger(PlanManager.class);
	
	public Map<String, Plan> findBestDeploymentPlans(Map<ComponentId, ComponentDeploymentData> components, Map<String, DeploymentDescriptor> deploymentDescriptors) throws Exception {
		/* Init the minimum plan to -1 for each application */
		Map<String, Integer> minPlans = new HashMap<>();
		for (String applicationShortName : deploymentDescriptors.keySet()) {
			minPlans.put(applicationShortName, -1);
		}
		
		/* Step 1 - find the minimum plan number we can execute */
		for (Entry<ComponentId, ComponentDeploymentData> entry : components.entrySet()) {
			ComponentId componentId = entry.getKey();
			ComponentDeploymentData component = entry.getValue();
			
			String applicationShortName = component.getApplicationShortName(); 
			if (applicationShortName != null) {			
				int minComponentPlan = getMinimumPlan(component);
				log.debug(String.format("Minimum plan for '%s' is %d", componentId, minComponentPlan));
				
				if (minComponentPlan > minPlans.get(applicationShortName).intValue()) {
					minPlans.put(applicationShortName, minComponentPlan);
				}
			} else {
				log.warn(String.format("No application short name set for '%s' - this is likely to be an error of some kind.", componentId));
			}
		}
		
		/* Step 2 - find a plan that matches that number */
		Map<String, Plan> bestLaidPlans = new LinkedHashMap<>();
		for (Entry<String, DeploymentDescriptor> deploymentDescriptor : deploymentDescriptors.entrySet()) {
			String applicationShortName = deploymentDescriptor.getKey();
			log.debug(String.format("Finding plan for %s", applicationShortName));
			
			int minPlan = minPlans.get(applicationShortName);
			if (minPlan >= 0) {
				List<Plan> plans = deploymentDescriptor.getValue().getPlans();
				for (Plan plan : plans) {
					XMLPlanType xmlPlan = plan.getXMLType();
					Integer impactLevel = xmlPlan.getImpactLevel();
					log.debug(String.format("Impact level %d minimum level of %d", impactLevel, minPlan));
					
					if (impactLevel != null) {
						if (impactLevel.intValue() == minPlan) {
							bestLaidPlans.put(applicationShortName, plan);
							log.debug(String.format("Found a plan at impact level %d", impactLevel));
							break;
						}
					}
				}
				if (bestLaidPlans.get(applicationShortName) == null) {
					throw new Exception(String.format("Cannot find a plan with an impact level %d for '%s'", minPlan, applicationShortName));
				}
			} else {
				log.info(String.format("Minimum plan for '%s' is %d - this is likely to be because there are no changes for any of the application's components.", applicationShortName, minPlan));
			}
		}
		return bestLaidPlans;
	}
	
	/**
	 * This matches up the deployment actions for the components that need to be
	 * deployed with the set of plans available for them in the deployment
	 * descriptor. It matches the minimum plan for each component with the set
	 * of plans. It throws an exception if it cannot find one. Currently it
	 * ignores FAIL and NO_CHANGE deployment actions.
	 * 
	 * @param components
	 * @param deploymentDescriptor
	 * @return
	 * @throws Exception
	 */
	public XMLPlanType findBestDeploymentPlan(Map<ComponentId, ComponentDeploymentData> components, DeploymentDescriptor deploymentDescriptor) throws Exception {

		/* Step 1 - find the minimum plan number we can execute */
		int minPlan = -1;
		for (Entry<ComponentId, ComponentDeploymentData> entry : components.entrySet()) {
			int minimumPlan = getMinimumPlan(entry.getValue());
			log.debug("Minimum plan for '"+entry.getKey()+"' is " + minimumPlan);
			if (minimumPlan > minPlan) {
				minPlan = minimumPlan;
			}
		}
        if (minPlan < 0) {
        	log.warn("Minimum plan is '"+minPlan+"' this is likely to be an error of some kind.");
        }
		
		/* Step 2 - find a plan that matches that number */
		XMLPlanType selectedPlan = null;
		List<XMLPlanType> plans = deploymentDescriptor.getXMLType().getPlans().getPlan();
		for (XMLPlanType plan : plans) {
			Integer impactLevel = plan.getImpactLevel();
			log.debug("Impact level " + impactLevel + " minimum level of " + minPlan);
			if (impactLevel != null) {
				if (impactLevel.intValue() == minPlan) {
					selectedPlan = plan;
					log.debug("Found a plan at impact level '" + impactLevel + "'");
					break;
				}
			}
		}
		if (selectedPlan == null) {
			throw new Exception("Cannot find a plan with an impact level " + minPlan );
		}

		return selectedPlan;
	}

	/**
	 * Returns an integer value indicating the minimum plan for this component
	 * if: 1) it has a deployment descriptor 2) the minimum version has been set
	 * for this component in the deployment descriptor 3) The deployment actions
	 * for this component are one of UPGRADE,DOWNGRADE,DEPLOY,UNDEPLOY:
	 * 
	 * @param component
	 * @return
	 */
	private int getMinimumPlan(ComponentDeploymentData component) {
		if (component == null) {
			throw new IllegalArgumentException("Input parameter may not be null");
		}

		log.debug(String.format("Getting minimum plan for component [%s]", component.getComponentName()));
		
		int min = -1;
		if (component.getChangeSets() != null && component.getChangeSets().size() > 0) {
			
			boolean isAMaterialChange = false;
			OUTER:
			for (ChangeSet changeSet : component.getChangeSets()) {
				if (changeSet.isComplexChange()) {
					List<Change> subTasks = changeSet.getSubTasks();
					for (Change change : subTasks) {
						if (DifferenceManager.isAMaterialChange(change)) {
							isAMaterialChange = true;
							break OUTER;
						}
					}
				}
				else {
					isAMaterialChange = DifferenceManager.isAMaterialChange(changeSet.getPrimaryChange());
					
					if (isAMaterialChange) {
						break OUTER;
					}
				}							
			}
			
			if (isAMaterialChange) {
				if (component.getDeploymentDescriptorDef() != null && component.getDeploymentDescriptorDef().getXMLType().getMinimumPlan() != null) {
					int minimumPlan = component.getDeploymentDescriptorDef().getXMLType().getMinimumPlan().intValue();
					if (minimumPlan > min) {
						min = minimumPlan;
					}
				}
				else {
					log.debug("Minimum plan has not been set for "+component.getComponentName()+" as " + (component.getDeploymentDescriptorDef() == null ? "the deployment descriptor is null" : "the value for minimum plan is null."));
				}
			}
			else {
				log.debug("Minimum plan has not been set for "+component.getComponentName()+" as there is no material change to perform.");
			}
		}	
		return min;
	}
}
