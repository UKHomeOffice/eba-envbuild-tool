package com.ipt.ebsa.manage.deploy.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.BeforeAfterSteps;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Component;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Step;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.StepItem;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.deployment.descriptor.XMLChangeMethodType;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType.XMLDeploy;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType.XMLDowngrade;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType.XMLUndeploy;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType.XMLUpgrade;
import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.hiera.HieraData;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.hiera.HieraEnvironmentStateManager;
import com.ipt.ebsa.manage.util.VersionCompare;
import com.ipt.ebsa.yaml.YamlUtil;

public class DifferenceManager {
	
	private static final String VERSION_NOT_PRESENT = "Version not present";
	private static final Logger log = LogManager.getLogger(DifferenceManager.class);
	
	/**
	 * Returns true for UPGRADE, DOWNGRADE, DEPLOY, UNDEPLOY and false for NO_CHANGE, FIX and FAIL.
	 * @param change
	 * @return
	 */
	public static boolean isAMaterialChange(Change change) {
		boolean isAChange = false;
		switch (change.getChangeType()) {
		case UPGRADE:
		case DOWNGRADE:
		case DEPLOY:
		case UNDEPLOY:
		case FIX:
			isAChange = true;
			break;
		default:
			log.debug("No material changes found.  Change type is " + change.getChangeType());
		}
		return isAChange;
	}
	
	/**
	 * Returns true if there are any failures which will prevent a deployment from taking place.
	 * @param components
	 * @return
	 */
	public boolean hasFailures(Map<ComponentId, ComponentDeploymentData> components) {
		boolean hasFailures = false;
		Set<Entry<ComponentId, ComponentDeploymentData>> entries = components.entrySet();
		for (Entry<ComponentId, ComponentDeploymentData> entry : entries) {
			if (entry.getValue().getExceptions() != null && entry.getValue().getExceptions().size() > 0) {
				hasFailures = true;
				break;
			}
		}
		return hasFailures;
	}

	public List<Exception> getFailures(Map<ComponentId, ComponentDeploymentData> components) {
		List<Exception> exceptions = new ArrayList<Exception>();
		Set<Entry<ComponentId, ComponentDeploymentData>> entries = components.entrySet();
		for (Entry<ComponentId, ComponentDeploymentData> entry : entries) {
			List<Exception> entryExceptions =  entry.getValue().getExceptions();
			if (entryExceptions != null && entryExceptions.size() > 0) {
				for (Exception e : entryExceptions) {
					exceptions.add(e);
				}
			}
		}
		return exceptions;
	}

	/**
	 * Returns true if there are any material changes on any components
	 * @param components
	 * @return
	 */
	public boolean hasChanges(Map<ComponentId, ComponentDeploymentData> components) {
		Set<Entry<ComponentId, ComponentDeploymentData>> entries = components.entrySet();
		for (Entry<ComponentId, ComponentDeploymentData> entry : entries) {
			List<ChangeSet> changeSets = entry.getValue().getChangeSets();
			for (ChangeSet changeSet : changeSets) {
				if (changeSet.isComplexChange()) {
					List<Change> subTasks = changeSet.getSubTasks();
					for (Change change : subTasks) {
						if (isAMaterialChange(change)) {
							return true;
						}
					}
				}
				else {
					if (isAMaterialChange(changeSet.getPrimaryChange())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * This method is the heart of the process of determining what will change in an environment as a result of a deployment.
	 * It works through all of the data and decides for each component what the difference is between what needs to be deployed 
	 * and what is deployed.  It probably looks scary but it is really only an implementation of a matrix.  Each row of the 
	 * matrix is listed in the if statement as a comment and the matrix is documented on Confluence in the multi-component deploy design.
	 * 
	 * @param deployment
	 */
	public void calculateDifferencesAndAssignActions(Deployment deployment) {
		String prefix = "   > ";
		Set<Entry<ComponentId, ComponentDeploymentData>> entries = deployment.getComponents().entrySet();
		for (Entry<ComponentId, ComponentDeploymentData> entry : entries) {
			log.debug(String.format("Difference manager examining component '%s'",entry.getKey()));
		    ComponentDeploymentData cdd = entry.getValue();
		    ComponentVersion targetComponentVersion = cdd.getTargetCmponentVersion();

		    String unadjustedComponentVersion = null;// ie. version directly from the maven build
		    String adjustedComponentVersion = null;// ie. the rpm package name
		    if (null != targetComponentVersion) {
		    	unadjustedComponentVersion = targetComponentVersion.getComponentVersion();
		    	adjustedComponentVersion = targetComponentVersion.getRpmPackageVersion();
		    }
			
			if (cdd.getDeploymentDescriptorDef() != null) {
				log.debug(String.format(prefix + "Deployment descriptor is '%s'.",ComponentDeploymentDataManager.toString(cdd.getDeploymentDescriptorDef().getXMLType())));
		    	if (targetComponentVersion != null) {
		    		log.debug(String.format(prefix + "ComponentVersion is '%s', adjusted it is '%s'",unadjustedComponentVersion, adjustedComponentVersion));
		    		if (cdd.getExistingState() != null) {
		    			log.debug(String.format(prefix + "Existing State is '%s'",cdd.getExistingState()));
		    			
		    			//This is the sweet spot for one of a number of cases
		    			//NO_CHANGE, UPGRADE, DOWNGRADE, DEPLOY, UNDEPLOY, FIX, FAIL;
		    			List<String> versions = getExistingVersions(cdd);
		    			
		    			log.debug(String.format(prefix + "%s version entries found",versions.size()));
		    			/* Special cases for the ComponentVersion */
		    			if (StringUtils.isBlank(unadjustedComponentVersion)) {
		    				//5. New Version is blank - FAIL (Component Version is invalid or corrupt)
		    				String text = String.format("ComponentVersion '%s' does not have a valid version number",targetComponentVersion);
							cdd.getChangeSets().add(getFailDeploymentActionGroup(cdd,ChangeType.FAIL, text));
		    				cdd.getExceptions().add(new RuntimeException(text));
		    				log.debug(prefix + "Matrix # 5. New Version is blank - FAIL (Component Version is invalid or corrupt)");
		    			}
		    			else if (unadjustedComponentVersion.equals(HieraData.ABSENT)) {
		    				//6. New version is "Absent" and existing versions is anything - UNDEPLOY
		    				cdd.getChangeSets().add(getDeploymentActionGroup(deployment,cdd, ChangeType.UNDEPLOY, prefix));
		    				log.debug(prefix + "Matrix # 6. New version is 'Absent' and existing versions is anything - UNDEPLOY");
		    			}
		    			else {
		    				// List of StateSearchResults which can be removed after this part of the processing so they
		    				// don't trigger unnecessary future processing
		    				List<StateSearchResult> resultsToRemove = new ArrayList<StateSearchResult>();
							for (int indexOfSearchResult = 0; indexOfSearchResult < versions.size(); indexOfSearchResult++) {
								String versionInState = versions.get(indexOfSearchResult);
								StateSearchResult searchResult = cdd.getExistingState().get(indexOfSearchResult);
			    				if(searchResult.getSource() != null && searchResult.getSource().isOutOfScope(cdd.getApplicationShortName())) {
									ChangeSet set = getDeploymentActionGroup(deployment, cdd, ChangeType.NO_CHANGE_OUT_OF_SCOPE, indexOfSearchResult, prefix);
									cdd.getChangeSets().add(set);
									set.getPrimaryChange().setWarning(String.format("Component is deployed to out-of-scope host/role", searchResult.getSource().getRoleOrFQDN()));
									resultsToRemove.add(searchResult);
									
			                    	log.debug(prefix + "Matrix # -1^(1/2) Component is present but in out-of-scope hiera file - NO_CHANGE");
								} 
								else if (StringUtils.isBlank(versionInState) || versionInState.equals(VERSION_NOT_PRESENT)) {
				    				/* Special cases for the YamlVersion */
				    				//4. New version specified, existing yaml specified but existing version is blank - FIX
				    				cdd.getChangeSets().add(getDeploymentActionGroup(deployment,cdd, ChangeType.FIX, indexOfSearchResult, prefix));
				    				log.debug(prefix + "Matrix # 4. New version specified, existing yaml specified but existing version is blank - FIX");
				    			}
				    			else if (versionInState.equals(HieraData.ABSENT)) {
				    				StateSearchResult result = searchResult;
				    				if (isRoleOrHostInScopeForDeployment(deployment, cdd, result)) {
					    				//7. New version is anything and existing version is "Absent" - DEPLOY
					    				cdd.getChangeSets().add(getDeploymentActionGroup(deployment, cdd, ChangeType.DEPLOY, indexOfSearchResult, prefix));  
					    				log.debug(prefix + "Matrix # 7. New version is anything and existing version is \"Absent\" - DEPLOY");
				    				} else {
				    					// This case means that the component to be deployed exists as absent in a YAML file not defined in the DD
				    					// Steve Cowx thinks this case should fail the deployment but I'm (Dan McCarthy) not convinced
				    					log.debug(prefix + "Will not deploy in yaml " + (result.getSource() == null ? "" : result.getSource().getRoleOrFQDN()) + " as not in deployment descriptor");
				    				}
				    			}
				    			else {
				    				if (isRoleOrHostInScopeForUndeploy(deployment, searchResult.getSource().getRoleOrFQDN(), searchResult.getSource().getEnvironmentName(), cdd)
				    						&& !isRoleOrHostInScopeForDeployment(deployment, cdd, searchResult)) {
				    					log.debug(prefix + "Should not be deployed to " + searchResult.getSource().getRoleOrFQDN());
				    					cdd.getChangeSets().add(getDeploymentActionGroup(deployment,cdd, ChangeType.UNDEPLOY, indexOfSearchResult, prefix));
				    				} else {
					    				/* Regular cases for YamlVersion, just compare the version numbers */
					    				int resultOfCompare = new VersionCompare().compare(adjustedComponentVersion, versionInState);
						    			
					    				if (resultOfCompare == 0) {
					    				   //1. Versions are the same - NO_CHANGE
					    					
					    					// but could be that the yaml is not upto date.
					    					ChangeSet changeSet = yamlOnlyUpdate(deployment, cdd, searchResult, prefix);
					    					
					    					if (null == changeSet) {
					    						// NO_CHANGE as YAML is in sync
					    						changeSet = getDeploymentActionGroup(deployment,cdd, ChangeType.NO_CHANGE, indexOfSearchResult, prefix);
					    						log.debug(prefix + "Matrix # 1. Versions are the same - NO_CHANGE");
					    					} else {
					    						// Apply changes common to all primary actions, even though this is only a FIX
					    						createActionChanges(changeSet.getPrimaryChange(), cdd.getDeploymentDescriptorDef().getSteps().getBefore(), true);
						    					createActionChanges(changeSet.getPrimaryChange(), cdd.getDeploymentDescriptorDef().getSteps().getAfter(), false);
					    					}
					    					
					    					log.debug(prefix + "yaml update without version change - FIX");
					    					cdd.getChangeSets().add(changeSet);
					    				}
					    				else if (resultOfCompare == 1) {
										    //2. New version greater than existing version - UPGRADE
					    					cdd.getChangeSets().add(getDeploymentActionGroup(deployment,cdd, ChangeType.UPGRADE, indexOfSearchResult, prefix));
					    					log.debug(prefix + "Matrix # 2. New version greater than existing version - UPGRADE");
					    				}
					    				else {
					    					//3. New version is less than existing version - DOWNGRADE
					    					cdd.getChangeSets().add(getDeploymentActionGroup(deployment,cdd, ChangeType.DOWNGRADE, indexOfSearchResult, prefix));
					    					log.debug(prefix + "Matrix # 3. New version is less than existing version - DOWNGRADE");
					    				}
				    				}
				    			}
			    			}
			    			
			    			cdd.getExistingState().removeAll(resultsToRemove);
			    			
			    			// May be that a host/role in deployment descriptor has not been found in any existing yaml
			    			List<ResolvedHost> hostsInDDNotFoundInHiera = hostnamesOrRolesInDeploymentDescriptorNotFoundInHiera(deployment, cdd, prefix);
			    			if (!hostsInDDNotFoundInHiera.isEmpty()) {
			    				log.debug(String.format(prefix + "Existing YAML is null for '%s' - DEPLOY", hostsInDDNotFoundInHiera));
			    				ChangeSet group = new ChangeSet();
			    				Change primaryChange = new Change(ChangeType.DEPLOY);
			    				group.setPrimaryChange(primaryChange);
			    				cdd.getChangeSets().add(group);
			    			}
		    			}
		    		}
		    		else {
		    			log.debug(String.format(prefix + "Existing YAML is null for '%s'",entry.getKey()));
		    		
		    			//11. No existing YAML - DEPLOY	
		    			cdd.getChangeSets().add(getDeploymentActionGroup(deployment,cdd, ChangeType.DEPLOY, prefix));
		    			log.debug(prefix + "Matrix # 11. No existing YAML - DEPLOY");
		    		}
		    	}
		    	else {
		    		log.debug(String.format(prefix + "ComponentVersion is null for '%s'",entry.getKey()));
	    		
		    		if (cdd.getExistingState() != null) { 
                    	List<String> versions = getExistingVersions(cdd);
                    	
                    	List<StateSearchResult> resultsToRemove = new ArrayList<StateSearchResult>();
                    	for (int indexOfSearchResult = 0; indexOfSearchResult < versions.size(); indexOfSearchResult++) {
							String versionInYaml = versions.get(indexOfSearchResult);
							/* Special case for if the Hiera file is out of scope */
							StateSearchResult searchResult = cdd.getExistingState().get(indexOfSearchResult);
							if(searchResult.getSource() != null && searchResult.getSource().isOutOfScope(cdd.getApplicationShortName())) {
								ChangeSet set = getDeploymentActionGroup(deployment,cdd, ChangeType.NO_CHANGE_OUT_OF_SCOPE, indexOfSearchResult, prefix);
								cdd.getChangeSets().add(set);
								set.getPrimaryChange().setWarning(String.format("Component is deployed to out-of-scope host/role", searchResult.getSource().getRoleOrFQDN()));
								resultsToRemove.add(searchResult);
		                    	log.debug(prefix + "Matrix # -1^(1/2) Component is present but in out-of-scope hiera file - NO_CHANGE");
							}
							else if (StringUtils.isBlank(unadjustedComponentVersion) && versionInYaml.equals(HieraData.ABSENT)) {
			    				/* Special cases for the YamlVersion */

			    				//10. New version not specified & existing version is "Absent" - NO_CHANGE
		                    	cdd.getChangeSets().add(getDeploymentActionGroup(deployment,cdd, ChangeType.NO_CHANGE, indexOfSearchResult, prefix));
		                    	log.debug(prefix + "Matrix # 10. New version not specified & existing version is \"Absent\" - NO_CHANGE");
			    			}
			    			else {
			    				//9. New Version not specified and existing version exists- FAIL
			    				//STEVE C Changing this to an undeploy as it makes more sense as a way to remove components... keep them in the descriptor and 
			    				// remove them from the environment, liek a way fo ensuring absent.
			    				String text = String.format("A relevant ComponentVersion does not exist for component '%s', however a version '%s' of the component is deployed in the environment.  This will result in an UNDEPLOY.",cdd.getComponentName(), versionInYaml);
				    			ChangeSet set = getDeploymentActionGroup(deployment,cdd, ChangeType.UNDEPLOY, indexOfSearchResult, prefix);
				    			set.getPrimaryChange().setWarning(text);
				    			cdd.getChangeSets().add(set);
				    			log.debug(prefix + "Matrix # 9. New Version not specified and existing version exists- UNDEPLOY");
			    			}
						}
						cdd.getExistingState().removeAll(resultsToRemove);
		    		} else {
		    			//9. New Version not specified and no existing version either - NO_CHANGE
		    			
		    			cdd.getChangeSets().add(getDeploymentActionGroup(deployment,cdd, ChangeType.NO_CHANGE, prefix));
                    	log.debug(prefix + "Matrix # 9. New version not specified & no existing version either - NO_CHANGE");
		    		}
		    	}
		    	
		    } else {
		    	log.debug(String.format(prefix + "Deployment descriptor is null for '%s'",entry.getKey()));
		    	//8. This is a fail, there is nothing we can do without a deployment descriptor
		    	String text = String.format("Deployment descriptor for component '%s', cannot be null.",cdd.getComponentName());
    			cdd.getChangeSets().add(getFailDeploymentActionGroup(cdd,ChangeType.FAIL, text));   
    			cdd.getExceptions().add(new RuntimeException(text));
    			log.debug(prefix + "Matrix # 8. This is a fail, there is nothing we can do without a deployment descriptor");
		    }
		}
		
	}

	/**
	 * @param deployment
	 * @param cdd
	 * @param yamlSearchResult
	 * @param prefix
	 * @return null if yaml is in sync, ChangeSet if it isn't
	 */
	private ChangeSet yamlOnlyUpdate(Deployment deployment, ComponentDeploymentData cdd, StateSearchResult searchResult, String prefix) {
		Map<String, Object> ddYaml = cdd.getDeploymentDescriptorYaml();
		Map<String, Object> hieraYaml = YamlUtil.deepCopyOfYaml(searchResult.getComponentState());
		hieraYaml.remove(HieraData.ENSURE);
		
		if (YamlUtil.deepCompareYaml(ddYaml, hieraYaml)) {
			return null;
		}
		
		ChangeSet group = new ChangeSet();
		Change primaryChange = new Change(ChangeType.FIX);
		group.setPrimaryChange(primaryChange);
		log.debug(String.format("%sYAML update required (excluding ensure) deployment descriptor: [%s] hiera: [%s]", prefix, ddYaml, hieraYaml));
		primaryChange.setSearchResult(searchResult);
		
		return group;
	}

	private List<ResolvedHost> hostnamesOrRolesInDeploymentDescriptorNotFoundInHiera(Deployment deployment, ComponentDeploymentData cdd, String prefix) {
		List<ResolvedHost> output = new ArrayList<>();
		
		// we check for each role/fqdn in the deployment descriptor
		// that we have some corresponding yaml in hiera
		Collection<ResolvedHost> hosts = cdd.getHosts();
		for (ResolvedHost host : hosts) {
			// If it's out of scope we don't want to touch it under *any* circumstance 
			Collection<ResolvedHost> schemeScope = deployment.getSchemeScope(cdd.getApplicationShortName());
			if (schemeScope != null && !schemeScope.contains(host)) {
				continue;
			}
			boolean found = false;
			
			for (StateSearchResult yaml : cdd.getExistingState()) {
				ResolvedHost existingHost = new ResolvedHost(yaml.getSource().getRoleOrFQDN(), yaml.getSource().getEnvironmentName());
				if (host.equals(existingHost)) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				output.add(host);
			}
		}
		
		return output;
	}
	
	/**
	 * Sets up a fail action
	 * @param component
	 * @param primaryAction
	 * @param reasonForFailure
	 * @return
	 */
	public static ChangeSet getFailDeploymentActionGroup(ComponentDeploymentData component, ChangeType primaryAction, String reasonForFailure) {
		if (primaryAction != ChangeType.FAIL) {
			throw new IllegalArgumentException("Incorrect use of method");
		}
		ChangeSet group = new ChangeSet();
		group.setPrimaryChange(new Change(primaryAction));
		group.getPrimaryChange().setReasonForFailure(reasonForFailure);
		
		return group;
	}
	
	/**
	 * Uses the component descriptor definition hints to determine whether this component needs more than one step to be executed 
	 * and sets up the subtasks if it does.
	 * and returns 
	 * @param deployment TODO
	 * @param component
	 * @param primaryAction
	 * @return
	 */
	protected ChangeSet getDeploymentActionGroup(Deployment deployment, ComponentDeploymentData component, ChangeType primaryAction, String logPrefix) {
		return getDeploymentActionGroup(deployment, component, primaryAction, 0, logPrefix);
	}
	
	/**
	 * Uses the component descriptor definition hints to determine whether this component needs more than one step to be executed 
	 * and sets up the subtasks if it does.
	 * and returns 
	 * @param deployment TODO
	 * @param component
	 * @param primaryAction
	 * @return
	 */
	protected ChangeSet getDeploymentActionGroup(Deployment deployment, ComponentDeploymentData componentData, ChangeType primaryAction, int indexOfSearchResult, String logPrefix) {
		
		ChangeSet group = new ChangeSet();
		Change primaryChange = new Change(primaryAction);
		group.setPrimaryChange(primaryChange);
		
		StateSearchResult searchResult = null;
		log.debug(logPrefix + "Search result index " + indexOfSearchResult);
		if (indexOfSearchResult >= 0 && componentData.getExistingState() != null) {
		   searchResult = componentData.getExistingState().get(indexOfSearchResult);
		   log.debug(logPrefix + "Search result " + searchResult + " " + componentData.getComponentName());
		   primaryChange.setSearchResult(searchResult);
		}
		else {
			log.debug(logPrefix + "No search result for " + componentData.getComponentName());		   
		}

		if (componentData.getDeploymentDescriptorDef() != null) {
			createSubTasks(deployment, componentData, primaryAction, group, searchResult);

			/* Add before and after actions applicable to all actions */
			if (isAMaterialChange(primaryChange)) {
				createActionChanges(primaryChange, componentData.getDeploymentDescriptorDef().getSteps().getBefore(), true);
				createActionChanges(primaryChange, componentData.getDeploymentDescriptorDef().getSteps().getAfter(), false);
			}
		}
		
		return group;
	}

	/**
	 * This method adds undeploy and deploy subtasks if they are needed and also add before and after tasks if they are needed.
	 * @param deployment
	 * @param component
	 * @param primaryAction
	 * @param group
	 * @param hints
	 * @param yamlSearchResult
	 */
	private void createSubTasks(Deployment deployment, ComponentDeploymentData componentData, ChangeType primaryAction, ChangeSet group, StateSearchResult searchResult) {
		XMLHintsType hints = componentData.getDeploymentDescriptorDef().getXMLType().getHints();
		
		switch (primaryAction) {
			case DOWNGRADE:
				XMLDowngrade downgradeHint = hints != null ? hints.getDowngrade() : null;
				if (downgradeHint != null) {
					
					/* Add undeploy redeploy */
					XMLChangeMethodType method = downgradeHint.getMethod();
					switch (method) {
					case undeployRedeploy:
						createSubtask(deployment, componentData, group, ChangeType.UNDEPLOY, searchResult);
						createSubtask(deployment, componentData, group, ChangeType.DEPLOY, searchResult);
					default:
						break;
					}
					
					/* Add before and after actions*/
					BeforeAfterSteps downgradeSteps = componentData.getDeploymentDescriptorDef().getDowngradeSteps();
					createActionChanges(group.getPrimaryChange(), downgradeSteps.getBefore(), true);
					createActionChanges(group.getPrimaryChange(), downgradeSteps.getAfter(), false);
				}
				break;
			case UPGRADE:
				XMLUpgrade upgradeHint = hints != null ? hints.getUpgrade() : null;
				if (upgradeHint != null) {
	
					/* Add undeploy redeploy */XMLChangeMethodType method = upgradeHint.getMethod();
					switch (method) {
					case undeployRedeploy:
						createSubtask(deployment, componentData, group, ChangeType.UNDEPLOY, searchResult);
						createSubtask(deployment, componentData, group, ChangeType.DEPLOY, searchResult);
					default:
						break;
					}
	
					/* Add before and after actions*/
					BeforeAfterSteps upgradeSteps = componentData.getDeploymentDescriptorDef().getUpgradeSteps();
					createActionChanges(group.getPrimaryChange(), upgradeSteps.getBefore(), true);
					createActionChanges(group.getPrimaryChange(), upgradeSteps.getAfter(), false);
				}
				break;
			case DEPLOY:
				//Nothing special to do here except add before and after tasks. No subtasks are required
				XMLDeploy deployHint = hints != null ? hints.getDeploy() : null;
				if (deployHint != null) {
					/* Add before and after actions*/
					BeforeAfterSteps deploySteps = componentData.getDeploymentDescriptorDef().getDeploySteps();
					createActionChanges(group.getPrimaryChange(), deploySteps.getBefore(), true);
					createActionChanges(group.getPrimaryChange(), deploySteps.getAfter(), false);
				}
				break;
			case UNDEPLOY:
				/* Nothing to do here, it either undeploys or it doesn't */
				XMLUndeploy undeployHint = hints != null ? hints.getUndeploy() : null;
				if (undeployHint != null) {
					/* Add before and after actions*/
					BeforeAfterSteps undeploySteps = componentData.getDeploymentDescriptorDef().getUndeploySteps();
					createActionChanges(group.getPrimaryChange(), undeploySteps.getBefore(), true);
					createActionChanges(group.getPrimaryChange(), undeploySteps.getAfter(), false);
				}
				break;
			case FIX:
			case FAIL:
			case NO_CHANGE:
			default:
		}
	}

	/**
	 * Creates subtasks which correspond to the actions defined in the hints section of this component or in the main body for this component 
	 * @param deployment
	 * @param component
	 * @param actions
	 */
	private static void createActionChanges(Change change, List<Step> steps, boolean before) {
		for (Step step : steps) {
			List<StepItem> items = step.getItems();
			if (items != null) {
				if (before) {
					log.debug("Adding a before action with " + items.size() + " steps/commands");
					change.getBefore().add(items);
				}
				else {
					log.debug("Adding an after action with " + items.size() + " steps/commands");
					change.getAfter().add(items);
				}
			}
		}
	}

	/**
	 * Set up a new subtask set for undeploy and redeploy if they do not already exist in the group
	 * @param deployment 
	 * @param group
	 */
	public static void createSubtask(Deployment deployment, ComponentDeploymentData componentData, ChangeSet group, ChangeType action, StateSearchResult searchResult) {
		//Check if it already exists, if not return
		for (Change change: group.getSubTasks()) {
			if (change.getChangeType() == action) { 
				return;
			}
		}
		
		// if is an undeployment, and all yaml fragments show absent or this environment is out of scope, do nothing
		if (ChangeType.UNDEPLOY == action && (HieraData.ABSENT.equals(getVersion(searchResult)) || !isRoleOrHostInScopeForUndeploy(deployment, searchResult.getSource().getRoleOrFQDN(), searchResult.getSource().getEnvironmentName(), componentData))) {
			return;
		}
		
		// If a. this is *not* a deployment, or b. this *is* a deployment and it's in scope for deployment, then...
		if (ChangeType.DEPLOY != action || isRoleOrHostInScopeForDeployment(deployment, componentData, searchResult)) {
			Change e = new Change(action);
			e.setSearchResult(searchResult);
			group.getSubTasks().add(e);
			
			Component component = componentData.getDeploymentDescriptorDef();
			XMLHintsType hints = component.getXMLType().getHints();
			XMLUndeploy undeployHint = (hints != null && action == ChangeType.UNDEPLOY) ? hints.getUndeploy() : null; 
			if (undeployHint != null) {
				/* Add before and after actions specific to undeploy */
				createActionChanges(e, component.getUndeploySteps().getBefore(), true);
				createActionChanges(e, component.getUndeploySteps().getAfter(), false);
			}
			XMLDeploy deployHint = (hints != null && action == ChangeType.DEPLOY) ? hints.getDeploy() : null;
			if (deployHint != null) {
				/* Add before and after actions specific to deploy */
				createActionChanges(e, component.getDeploySteps().getBefore(), true);
				createActionChanges(e, component.getDeploySteps().getAfter(), false);
			}
			
			// FIXME - Aren't we missing similar behaviour for upgrade and downgrade

			/* Add before and after actions applicable to all actions */
			createActionChanges(e, component.getSteps().getBefore(), true);
			createActionChanges(e, component.getSteps().getAfter(), false);
		}
	}

	
	/**
	 * If we're trying to *undeploy* the component, check that the host/role we're about to undeploy from is in the explicitly defined
	 * scheme scope before doing so, and if there's no defined scheme scope, assume the whole environment is in scope.
	 */
	private static boolean isRoleOrHostInScopeForUndeploy(Deployment deployment, String undeploymentTarget, String zone, ComponentDeploymentData componentData) {
		Collection<ResolvedHost> schemeScope = deployment.getSchemeScope(componentData);
		if (schemeScope == null) {
			// If there is no explicit scope then everything in the environment is fair game
			HieraEnvironmentStateManager stateManager = ((HieraEnvironmentStateManager) deployment.getEnvironmentStateManager());
			Map<String, HieraMachineState> hf = stateManager.getEnvironments().get(zone);
			Set<Entry<String, HieraMachineState>> entrySet = hf.entrySet();
			schemeScope = new ArrayList<>();
			for (Map.Entry<String, HieraMachineState> hiera : entrySet) {
				schemeScope.add(new ResolvedHost(hiera.getKey(), zone));
			}
		}
		
		return schemeScope.contains(new ResolvedHost(undeploymentTarget, zone));
	}
	
	
	/**
	 * If we're trying to *deploy* the component, check that the host is meant to be deployed to according to the deployment descriptor.
	 * Any host explicit in the deployment descriptor is in scope.
	 */
	private static boolean isRoleOrHostInScopeForDeployment(Deployment deployment, ComponentDeploymentData cdd, StateSearchResult searchResult) {
		MachineState source = searchResult.getSource();
		if (null == source) {
			return false;
		}
		
		ResolvedHost yamlHost = new ResolvedHost(source.getRoleOrFQDN(), source.getEnvironmentName());
		Collection<ResolvedHost> componentHosts = cdd.getHosts();
		
		Collection<ResolvedHost> sanitisedComponentHosts = new ArrayList<>();
		for (ResolvedHost host : componentHosts) {
			// We only need this sanitisation step because the host/role may contain a YAML file extension 
			ResolvedHost sanitisedHost = new ResolvedHost(YamlUtil.getRoleOrHostFromYaml(host.getHostOrRole()), host.getZone());
			if (deployment.getSchemeScope(cdd) != null && !deployment.getSchemeScope(cdd).contains(sanitisedHost)) {
				continue;
			}
			
			sanitisedComponentHosts.add(sanitisedHost);
		}
		boolean isRoleOrFqdnFoundInDepDesc = sanitisedComponentHosts.contains(yamlHost);
		
		if (!isRoleOrFqdnFoundInDepDesc) {
			log.debug(String.format("Role or FQDN [%s] not found in Deployment Descriptor hostnames [%s]", yamlHost, componentHosts));
		}
		
		return isRoleOrFqdnFoundInDepDesc;
	}
	
	/**
	 * Returns a list which shows all of the existing versions (in the same order as the search results for the
	 * existing yaml in the ComponentDeploymentData so that they can be married up later) 
	 * @return
	 */
	private List<String> getExistingVersions(ComponentDeploymentData v) {
		List<String> oldVersions = new ArrayList<String>();
	    List<StateSearchResult> searchResults = v.getExistingState();
	    for (StateSearchResult searchResult : searchResults) {
			String version = getVersion(searchResult);
			if (version != null) {
			   oldVersions.add(version);
			}
			else {
				oldVersions.add(VERSION_NOT_PRESENT);
			}
		}
	    return oldVersions;
	}

	private static String getVersion(StateSearchResult searchResult) {
		return searchResult.getComponentState().get(HieraData.ENSURE).toString();
	}
}
