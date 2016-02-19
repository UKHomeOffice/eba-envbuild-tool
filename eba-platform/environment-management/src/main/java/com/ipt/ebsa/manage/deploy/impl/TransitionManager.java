package com.ipt.ebsa.manage.deploy.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Plan;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Step;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.StepItem;
import com.ipt.ebsa.deployment.descriptor.release.XMLPhaseAdditionalActionsType;
import com.ipt.ebsa.deployment.descriptor.release.XMLPhaseStepItemType;
import com.ipt.ebsa.deployment.descriptor.release.XMLStopType;
import com.ipt.ebsa.deployment.descriptor.release.XMLWaitType;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.deployment.descriptor.XMLChainBehaviourType;
import com.ipt.ebsa.deployment.descriptor.XMLDeploymentActionType;
import com.ipt.ebsa.deployment.descriptor.XMLExecuteType;
import com.ipt.ebsa.deployment.descriptor.XMLInjectType;
import com.ipt.ebsa.deployment.descriptor.XMLPerformType;
import com.ipt.ebsa.deployment.descriptor.XMLRemoveType;
import com.ipt.ebsa.environment.EnvironmentUpdate;
import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.NodeMissingBehaviour;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.transitions.MCOCommand;
import com.ipt.ebsa.manage.transitions.Transition;
import com.ipt.ebsa.manage.util.DeploymentDescriptorUtils;
import com.ipt.ebsa.yaml.YamlUtil;

/**
 * This class provides functionality for handling transitions.
 * This is really the heart of everything where all of the complicated logic is for determining what happens in 
 * what order during the deployment.
 * 
 * 
 * @author scowx
 * 
 */
public class TransitionManager {

	private static final String	CHANGE_TYPE_GROUP_UNDEPLOY	= "undeploy";
	private static final String	CHANGE_TYPE_GROUP_OTHER		= "other";
	private Logger				log							= LogManager.getLogger(TransitionManager.class);
	private YamlManager			yamlManager					= new YamlManager();
	private static final String YAML_VALUE_KEY				= "theValueKey";

	/**
	 * Goes through all of the steps in the plan making up transitions for them to do.
	 * 
	 * @param deployment
	 * @param selectedPlan
	 * @param dependencyChains
	 * @throws Exception
	 */
	public List<Transition> createTransitions(Deployment deployment, String applicationShortName, Plan selectedPlan, TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains) throws Exception {
		List<Transition> transitions = new ArrayList<Transition>();

		/*
		 * Parse the XML transitions from the DeploymentDescriptor, create a
		 * Transition object and update the end state YAML for each transition
		 */
		List<Step> steps = selectedPlan.getSteps();
		for (Step step : steps) {
			Transition currentTransition = null;
			/*
			 * Execute the transition commands on the end state YAML to take it
			 * from starting state to ending state
			 */
			List<StepItem> items = step.getItems();
			for (StepItem item : items) {
				if (item.getXmlType() instanceof XMLPerformType) {
					log.debug("Executing perform command.");
					currentTransition = null;
					executeCommands(deployment, deployment.getComponents(), transitions, null, dependencyChains);
				}
				else if (item.getXmlType() instanceof XMLInjectType || item.getXmlType() instanceof XMLRemoveType) {
					log.debug("Executing inject or remove command.");
					currentTransition = addInjectionOrRemoveAction(item, applicationShortName, deployment, transitions, currentTransition, null, null);
				}
				else if (item.getXmlType() instanceof XMLExecuteType) {
					currentTransition = addCommandAction(item, applicationShortName, deployment, transitions, currentTransition, null);
				}
			}
		}

		return transitions;
	}
	
	/**
	 * Goes through all of the steps across all the selected plans making up transitions for them to do.
	 * The perform is done first, then for each plan any pre/post steps are slotted in at the right positions.
	 * 
	 * @param deployment
	 * @param selectedPlans
	 * @param dependencyChains
	 * @param applicationVersions
	 * @throws Exception
	 */
	public List<Transition> createTransitions(Deployment deployment, Map<String, Plan> selectedPlans, TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains, Map<String, ApplicationVersion> applicationVersions) throws Exception {
		// Use the 'old' way when there is only one plan 
		if (selectedPlans.size() == 1) {
			Entry<String, Plan> selectedPlan = selectedPlans.entrySet().iterator().next();
			return createTransitions(deployment, selectedPlan.getKey(), selectedPlan.getValue(), dependencyChains);
		}
		
		
		List<Transition> transitions = new ArrayList<>();
		// Assume there is a perform command so execute this first (across all plans)
		// TODO: Is this assumption correct? 
		log.debug("Executing perform command.");
		executeCommands(deployment, deployment.getComponents(), transitions, null, dependencyChains);
		
		// Loop through each plan and insert each before and after 
		for (Entry<String, Plan> selectedPlan : selectedPlans.entrySet()) {
			String applicationShortName = selectedPlan.getKey();
			
			// In practice this should have only a single transition: one step = one transition 
			List<Transition> stepTransitions = new ArrayList<>(1);
			
			for (Step step : selectedPlan.getValue().getSteps()) {
				Transition currentTransition = null;
				
				List<StepItem> items = step.getItems();
				for (StepItem item : items) {
					if (item.getXmlType() instanceof XMLPerformType) {
						currentTransition = null;
						
						if (!stepTransitions.isEmpty()) {
							// We have encountered a perform so step transitions up this point have been before steps
							// Find the first transition that involves a component of the application associated with this plan
							// and insert the transitions before that
							int index = -1;
							for (int i = 0; i < transitions.size(); i++) {							
								for (EnvironmentUpdate update : transitions.get(i).getUpdates()) {
									if (applicationShortName.equals(update.getApplicationName())) {
										index = i;
										break;
									}
								}
								if (index >= 0) {
									break;
								}
							}
							if (index >= 0) {
								log.debug(String.format("Inserting %d new pre-transitions at index %d against plan for %s", 
										stepTransitions.size(), index, applicationShortName));
								
								transitions.addAll(index, stepTransitions);
								
								// Bump the transition ids
								for (int i = index; i < transitions.size(); i++) {
									int currentSequenceNumber = transitions.get(i).getSequenceNumber();
									
									log.debug(String.format("Updating sequence number of transition %d to %d", currentSequenceNumber, i));
									
									transitions.get(i).setSequenceNumber(i);
								}
							}
							stepTransitions = new ArrayList<>();
						}
					} else if (item.getXmlType() instanceof XMLInjectType || item.getXmlType() instanceof XMLRemoveType) {
						log.debug("Executing inject or remove command.");
						currentTransition = addInjectionOrRemoveAction(item, applicationShortName, deployment, stepTransitions, currentTransition, null, null);
					} else if (item.getXmlType() instanceof XMLExecuteType) {
						currentTransition = addCommandAction(item, applicationShortName, deployment, stepTransitions, currentTransition, null);
					}
				}
			}
			if (!stepTransitions.isEmpty()) {
				// We reached the end so transitions steps since the perform have been after steps
				// Find the last transition that involves a component of the application associated with this plan
				// and insert the transitions after that
				int index = -1;
				for (int i = transitions.size() - 1; i >= 0; i--) {							
					for (EnvironmentUpdate update : transitions.get(i).getUpdates()) {
						if (applicationShortName.equals(update.getApplicationName())) {
							// Insert after this transition
							index = i + 1;
							break;
						}
					}
					if (index >= 0) {
						break;
					}
				}
				if (index >= 0) {
					log.debug(String.format("Inserting %d new post-transitions at index %d against plan for %s", 
							stepTransitions.size(), index, applicationShortName));
					
					transitions.addAll(index, stepTransitions);
					
					// Bump the transition ids
					for (int i = index; i < transitions.size(); i++) {
						int currentSequenceNumber = transitions.get(i).getSequenceNumber();
						
						log.debug(String.format("Updating sequence number of transition %d to %d", currentSequenceNumber, i));
						
						transitions.get(i).setSequenceNumber(i);
					}
				}
				stepTransitions = new ArrayList<>();
			}
		}
		
		return transitions;
	}

	/**
	 * This executes all of the commands for this transition (does not actually run the command it just updates the YAML in memory, to check it can and validate the plan)
	 * @param deployment
	 * @param components
	 * @param transition
	 * @param actionFilter
	 * @throws Exception
	 */
	private void executeCommands(Deployment deployment, Map<ComponentId, ComponentDeploymentData> components, List<Transition> transitions, XMLDeploymentActionType actionFilter, TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains) throws Exception {

		/* Split the components into two groups, "undeploy" changes and "other" changes */
		Map<String, List<ComponentDeploymentData>> changeTypeBuckets = getChangeTypeBuckets(components.values());

		/* Split the "undeploy" changes into sub bucket and order them according to their dependency chains, taking into account all factors */
		List<ComponentDeploymentData> undeploys = changeTypeBuckets.get(TransitionManager.CHANGE_TYPE_GROUP_UNDEPLOY);
		Buckets undeployBuckets = doComponentOrdering(true, undeploys);

		/* Split the "other" changes into sub buckets and order them according to their dependency chains, taking into account all factors */
		List<ComponentDeploymentData> others = changeTypeBuckets.get(TransitionManager.CHANGE_TYPE_GROUP_OTHER);
		Buckets otherBuckets = doComponentOrdering(false, others);

		/* Now create all of the transitions from the various buckets of changes that are left.  Each bucket is a transition */
		createTransitions(deployment, transitions, true, undeployBuckets);
		createTransitions(deployment, transitions, false, otherBuckets);
	}


	/**
	 * Enables a complex return type from a method call
	 * @author scowx
	 *
	 */
	static class Buckets {
		List<String>								bucketOrder;
		Map<String, List<ComponentDeploymentData>>	buckets;

		public Buckets(List<String> bucketOrder, Map<String, List<ComponentDeploymentData>> buckets) {
			this.bucketOrder = bucketOrder;
			this.buckets = buckets;
		}
	}

	/**
	 * For the bucket passed in it merges changes downwards into the least number of buckets.  Each bucket represents a transition.
	 * @param deployment
	 * @param type
	 * @param changeList
	 * @return
	 * @throws Exception
	 */
	private Buckets doComponentOrdering(boolean isUndeploy, List<ComponentDeploymentData> changeList) throws Exception {

		log.debug("Sub-dividing " + (isUndeploy ? "'undeploy'" : "'other'") + " bucket");

		/* Split them into buckets based on their component orderings */
		Map<String, List<ComponentDeploymentData>> orderBuckets = separateByComponentOrderAndYamlFile(changeList);

		/* Now we sort each bucket according to the order of the components in that bucket */
		List<String> list = getSortedListOfBuckets(orderBuckets, isUndeploy);

		Map<Integer, String> index = new TreeMap<Integer, String>();
		for (int i = 0; i < list.size(); i++) {
			index.put(new Integer(i), list.get(i));
		}

		log.debug("Listing" + (isUndeploy ? "'undeploy'" : "'other'") + " bucket");
		for (int i = 0; i < list.size(); i++) {
			log.debug("Bucket: " + index.get(i) + ":" + orderBuckets.get(index.get(i)));
		}

		log.debug("");
		log.debug("Processing " + (isUndeploy ? "'undeploy'" : "'other'") + " bucket");

		/* If all of the items in two consecutive buckets are not multi-transition then we can put them all in the same bucket */
		for (int i = 0; i < list.size(); i++) {
			if (i >= list.size() - 1) {
				//We have come the the last bucket.  We are finished now, if the items in this bucket
				//were able to be merged down then they would already have been.
				break;
			}

			List<ComponentDeploymentData> thisBucket = orderBuckets.get(index.get(i));
			List<ComponentDeploymentData> nextBucket = orderBuckets.get(index.get(i + 1));
			log.debug("  " + index.get(i) + ":" + thisBucket);
			log.debug("  " + index.get(i + 1) + ":" + nextBucket);

			if (canBucketsBeMerged(isUndeploy, thisBucket, nextBucket)) {
				log.debug("Buckets can be merged, merging");
				/* Move all components down a bucket */
				for (ComponentDeploymentData cd : nextBucket) {
					log.debug("Moving " + cd.getComponentName() + " from bucket '" + list.get(i + 1) + "' into bucket '" + list.get(i) + "'");
					thisBucket.add(cd);
				}

				/* remove the bucket */
				orderBuckets.remove(index.get(i + 1));
				list.remove(i + 1);
				/* Update the index */
				index.clear();
				for (int t = 0; t < list.size(); t++) {
					index.put(new Integer(t), list.get(t));
				}
				i--;
			}
		}
		log.debug("Finished processing " + (isUndeploy ? "'undeploy'" : "'other'") + " bucket");
		for (int i = 0; i < list.size(); i++) {
			log.debug("  " + list.get(i) + "," + index.get(i) + ":" + orderBuckets.get(index.get(i)));
		}
		return new Buckets(list, orderBuckets);

	}

	/**
	 * Returns true if these two buckets can be merged (which mean that the changes can be performed in the same puppet run). The criteria for merging are that:
	 * - Both bucks display no chain behaviour, or
	 * - the 'second' bucket is dependents-only and there are no dependents to consider
	 * 
	 * Note that if we're deploying, 'this' bucket is higher up the chain (ie. it's nearer to the root of the dependency tree) than the 'next' bucket. If we're 
	 * undeploying though, the buckets have already been re-ordered in preparation for creation of the transitions later, so 'this' bucket is lower down the chain, 
	 * and the 'next' bucket is nearer to the root of the chain, so the comparison we carry out here has to be reversed.
	 * 
	 * @param isUndeploy
	 * @param thisBucket
	 * @param nextBucket
	 * @return
	 */
	private boolean canBucketsBeMerged(boolean isUndeploy, List<ComponentDeploymentData> thisBucket, List<ComponentDeploymentData> nextBucket) {
		XMLChainBehaviourType thisBucketBehaviour = findWorstBehaviourInBucket(isUndeploy, thisBucket);
		XMLChainBehaviourType nextBucketBehaviour = findWorstBehaviourInBucket(isUndeploy, nextBucket);

		boolean areOnTheSameMachine = false;
		if (!isUndeploy) {
			areOnTheSameMachine = noComponentsAreCrossMachineDependencies(thisBucket, nextBucket);
			return areOnTheSameMachine && (thisBucketBehaviour == null || nextBucketBehaviour == null || (nextBucketBehaviour == XMLChainBehaviourType.dependents_only_multi_transition && !existsAComponentInBucketHavingDownstreamDependencies(nextBucket)));
		} else {
			areOnTheSameMachine = noComponentsAreCrossMachineDependencies(nextBucket, thisBucket);
			return areOnTheSameMachine && thisBucketBehaviour == null && (nextBucketBehaviour == null || (nextBucketBehaviour == XMLChainBehaviourType.dependents_only_multi_transition && !existsAComponentInBucketHavingDownstreamDependencies(nextBucket)));
		}
	}

	/**
	 * Create all of the transitions for a single bucket.
	 * @param deployment
	 * @param transitions
	 * @param isUndeploy
	 * @param buckets
	 * @throws Exception
	 */
	private void createTransitions(Deployment deployment, List<Transition> transitions, boolean isUndeploy, Buckets buckets) throws Exception {
		/* For each bucket we have left we have a transition, we will however add "sub transitions" for before and after actions as we go */
		for (int i = 0; i < buckets.bucketOrder.size(); i++) {
			log.debug("Processing bucket " + i);
			Transition transition = createTransition(deployment, transitions);

			List<ComponentDeploymentData> thisBucket = buckets.buckets.get(buckets.bucketOrder.get(i));
			for (ComponentDeploymentData component : thisBucket) {
				log.debug("Processing component " + component.getComponentName());
				transition = performChanges(deployment, isUndeploy, transition, transitions, component);
			}

			if (transitions.get(transitions.size() - 1).isEmpty()) {
				/* The last transition might have been created opportunistically and in fact not be needed. */
				log.debug("Transition " + transitions.get(transitions.size() - 1).getSequenceNumber() + "is empty, tidying-up by removing");
				transitions.remove(transitions.size() - 1);
			}
		}
	}

	/**
	* Executes the relevant changes against the YAML and inserts further transitions for sub tasks
	* @param deployment
	* @param isUndeploy
	* @param transition
	* @param component
	* @throws Exception
	*/
	private Transition performChanges(Deployment deployment, boolean isUndeploy, Transition transition, List<Transition> transitions, ComponentDeploymentData component) throws Exception {
		List<ChangeSet> changeSets = component.getChangeSets();
		for (ChangeSet changeSet : changeSets) {
			if (changeSet.isComplexChange()) {
				List<Change> subTasks = changeSet.getSubTasks();
				for (Change change : subTasks) {
					transition = doChange(deployment, transition, transitions, component, isUndeploy, changeSet, change);
				}
			}
			else {
				Change change = changeSet.getPrimaryChange();
				transition = doChange(deployment, transition, transitions, component, isUndeploy, changeSet, change);
			}
		}
		return transition;
	}

	/**
	 * Injects Pre-Post actions if there are any.  Insert transitions as needed.  Returns a transition that can be worked with, it is either the one
	 * passed in if no transitions were added or a brand new empty one any transitions have been added.
	 * @param deployment
	 * @param transitions
	 * @param beforeOrAfter
	 * @param change
	 * @throws Exception
	 */
	private Transition doPrePost(Deployment deployment, Transition transition, ComponentDeploymentData component, List<Transition> transitions, List<List<StepItem>> beforeOrAfterList, Change change, boolean preActions) throws Exception {
		if (beforeOrAfterList != null && beforeOrAfterList.size() > 0) {
			log.debug("Component " + component.getComponentName() + " has " + beforeOrAfterList.size() + " " + (preActions ? "before" : "after") + " lists to execute.");
			for (List<StepItem> beforeOrAfter : beforeOrAfterList) {
				if (beforeOrAfter != null && beforeOrAfter.size() > 0) {
					log.debug("Component " + component.getComponentName() + " has " + beforeOrAfter.size() + " " + (preActions ? "before" : "after") + " steps/commands to execute.  Adding new transitions for these.");
					//insert a new transition, we cannot continue with the old one
					if (!transition.isEmpty()) {
						transition = createTransition(deployment, transitions);
					}
					for (StepItem item : beforeOrAfter) {
						if (item.getXmlType() instanceof XMLInjectType || item.getXmlType() instanceof XMLRemoveType) {
							addInjectionOrRemoveAction(item, component.getApplicationShortName(), deployment, transitions, transition, component, change);
						}
						else if (item.getXmlType() instanceof XMLExecuteType) {
							addCommandAction(item, component.getApplicationShortName(), deployment, transitions, transition, component);
						}
						else {
							throw new UnsupportedOperationException("Step commands of type " + item.getClass().getName() + " are not supported.");
						}
					}
					transition = createTransition(deployment, transitions);
				} 
			}
		} 
		return transition;
	}

	/**
	 * Performs an injection step.
	 * @param item
	 * @param applicationShortName, will be null if this is phase level and not related to a specific application
	 * @param deployment
	 * @param transitions
	 * @param transition
	 * @param component, will be null if this is a plan level action
	 * @param change, will be null if this action isn't related part of a before/after deploy/undeploy 
	 * @throws Exception 
	 */
	public Transition addInjectionOrRemoveAction(StepItem item, String applicationShortName, Deployment deployment, List<Transition> transitions, Transition currentTransition, ComponentDeploymentData component, Change change) throws Exception {
		XMLInjectType inject = null;
		XMLRemoveType remove = null;
		if (item.getXmlType() instanceof XMLInjectType) {
			inject = (XMLInjectType) item.getXmlType();
		} else if (item.getXmlType() instanceof XMLRemoveType) {
			remove = (XMLRemoveType) item.getXmlType();
		} else {
			throw new IllegalArgumentException("XMLStepCommandType not instanceof XMLInjectType or XMLRemoveType");
		}
		
		if (currentTransition == null) {
			currentTransition = createTransition(deployment, transitions);
		}
		log.debug("Doing bespoke update");
		
		// Get hosts from command
		Collection<ResolvedHost> hosts = item.getHosts();
		if (hosts == null) {
			if (component != null) {
				// Component level inject or remove so get zone from component
				hosts = component.getHosts();
			} else {
				throw new IllegalArgumentException("Unable to create transition for bespoke action as no hosts have been provided and cannot inherit from component");
			}
		}
		
		log.debug(String.format("Looking for Hiera files for hosts %s", hosts));
		
		for (ResolvedHost host : hosts) {
			// Assume that if we're adding the same values to multiple hieras, it's ok to do the updates as part of the same transition
			MachineState toBeUpdated = deployment.getEnvironmentStateManager().getEnvironmentState(host.getZone(), host.getHostOrRole());			
			
			// TODO: May need to reference the change's zone at this point too
			String changeRoleOrFQDN = null;
			if (change != null && change.getSearchResult() != null && change.getSearchResult().getSource() != null) {
				changeRoleOrFQDN = change.getSearchResult().getSource().getRoleOrFQDN();
			}
			
			if (item.getHosts() != null || changeRoleOrFQDN == null || changeRoleOrFQDN.equals(toBeUpdated.getRoleOrFQDN())) {				
				// Either hostnames specified explicitly at injection level (continue as normal) OR change role/FQDN not specified (continue as normal) OR
				// IF change role/FQDN is specified then make sure the change's role/FQDN matches the Hiera file's role/FQDN for this injection
				// so that we don't get repeats as the changes are split out by VM when the transitions are executed
				List<HieraEnvironmentUpdate> multipleResults = new ArrayList<HieraEnvironmentUpdate>();
				if (null != inject) {
					NodeMissingBehaviour b = DeploymentDescriptorUtils.translateBehaviour(inject.getIfMissing());
					String value = inject.getValue();
					if (StringUtils.isNotBlank(value)) {
						//Create a yaml String (eg, "theValue: value)
						String yaml = String.format("%s: %s", YAML_VALUE_KEY, value);
						//Parse this into a Yaml object
						Map<String, Object> yamlObj = YamlUtil.getYamlFromString(yaml);
						//Get the actual value as it's expected type, as parsed by snakeyaml
						Object valueObj = YamlUtil.getObjectAtPath(yamlObj, YAML_VALUE_KEY);
						//Add this value to our yaml changes
						multipleResults.add(yamlManager.updateYaml(toBeUpdated, inject.getPath(), valueObj, b, applicationShortName, host.getZone()));
					} else {
						//Value is blank, assume we've got a block of YAML to insert
						multipleResults = yamlManager.updateYamlWithBlock(toBeUpdated, inject.getPath(), inject.getYaml(), b, applicationShortName, host.getZone());
					}
				} else {
					multipleResults.add(yamlManager.removeYaml(toBeUpdated, remove.getPath(), applicationShortName, host.getZone()));
				}
				
				if (multipleResults != null) {
					for (HieraEnvironmentUpdate result : multipleResults) {
						currentTransition.getUpdates().add(result);
					}
				}
			} else {			
				log.debug("Skipping this action as it is most likely not applicable to this change");
			}
		}
		return currentTransition;
	}
	
	/**
	 * If the current transition is null, creates a new transition (adding it to the list) and
	 * sets a wait on the transition before returning it.
	 * @param waitSeconds
	 * @param currentTransition
	 * @param transitions
	 * @return
	 */
	private Transition addWaitAction(int waitSeconds, Transition currentTransition, List<Transition> transitions) {
		if (waitSeconds > 0) {
			if (currentTransition == null) {
				currentTransition = createTransition(null, transitions);
			}
			currentTransition.setWaitSeconds(waitSeconds);
			
		}
		return currentTransition;
	}
	
	/**
	 * If the current transition is null, creates a new transition (adding it to the list) and
	 * sets a stop flag (and message) on the transition before returning it.
	 * @param message
	 * @param currentTransition
	 * @param transitions
	 * @return
	 */
	private Transition addStopAction(String message, Transition currentTransition, List<Transition> transitions) {
		if (currentTransition == null) {
			currentTransition = createTransition(null, transitions);
		}
		currentTransition.setStopAfter(true);
		currentTransition.setStopMessage(message);
		return currentTransition;
	}

	/**
	 * Inserts a Command object into the transitions table.
	 * @param item
	 * @param deployment
	 * @param transitions
	 * @param currentTransition
	 * @param component, will be null if this action isn't at component level
	 * @return
	 * @throws Exception
	 */
	public Transition addCommandAction(StepItem item, String applicationShortName, Deployment deployment, List<Transition> transitions, Transition currentTransition, ComponentDeploymentData component) throws Exception {
		if (currentTransition == null) {
			currentTransition = createTransition(deployment, transitions);
		}
		log.debug("Doing command execution");
		Collection<ResolvedHost> hosts = item.getHosts();
		if (hosts == null) {
			if (component != null) {
				/* Inherit hostnames from component if actions is within a component and hostnames on action are not specified. */
				hosts = component.getHosts();
			} else {
				throw new IllegalArgumentException("Unable to create transition for command as no hosts have been provided and cannot inherit from component");
			}
		}
		
		//There is no sense in validating that there are hiera files corresponding to these commands.  
		//We do not need hiera files for them as in theory they can be executed on any host or role. 
		String command = ((XMLExecuteType) item.getXmlType()).getCommand();
		currentTransition.getCommands().add(new MCOCommand(command, hosts, applicationShortName));
		log.debug(String.format("Created new command in transition %s '%s' with hostnames '%s'", currentTransition.getSequenceNumber(), command, item.getHosts()));
		return currentTransition;
	}

	/**
	 * If a change needs to be made then it will make the YAML change otherwise it won't.  Undeploys are done first regardless so we have two passes.
	 * In the first pass we pick out the undeploys and in the second pass we pick out the deploys.  This method differentiates for a parcitular pass
	 * @param deployment
	 * @param transition
	 * @param transitions TODO
	 * @param component
	 * @param isUndeploy
	 * @param changeSet
	 * @param change
	 * @throws Exception
	 */
	private Transition doChange(Deployment deployment, Transition transition, List<Transition> transitions, ComponentDeploymentData componentData, boolean isUndeploy, ChangeSet changeSet, Change change) throws Exception {
		// These are mutual exclusive
		// TODO if the source is not in the scope for the scheme then we can't deploy it
		//		DeploymentDescriptorUtils.getSchemeScope(deployment.getDeploymentDescriptor(), deployment.getEnvironmentName(), deployment.getScheme());
		if (isUndeploy && change.getChangeType() == ChangeType.UNDEPLOY) {
			//undeploys
			log.debug("Doing [undeploy] YAML update '" + componentData.getComponentName() + "'");
			
			transition = doPrePost(deployment, transition, componentData, transitions, change.getBefore(), change, true);
			// Transition may or may not be the same as the transition passed in at this point, however
			// it's the transition we should use as the 'current' or 'working' transition from this point forwards
			executeYAML(deployment, transition, null, componentData, changeSet, change);
			transition = doPrePost(deployment, transition, componentData, transitions, change.getAfter(), change, false);
		}
		else if (!isUndeploy && change.getChangeType() != ChangeType.UNDEPLOY) {
			//others
			log.debug("Doing [deploy, upgrade, downgrade or fix] YAML update for '" + componentData.getComponentName() + "'");
			transition = doPrePost(deployment, transition, componentData, transitions, change.getBefore(), change, true);
			executeYAML(deployment, transition, null, componentData, changeSet, change);
			transition = doPrePost(deployment, transition, componentData, transitions, change.getAfter(), change, false);
		}
		else {
			log.debug("Not doing YAML update '" + componentData.getComponentName() + "'");
		}
		return transition;
	}

	/**
	 * Puts some context around making the YAML updates
	 * @param deployment
	 * @param transition
	 * @param actionFilter
	 * @param component
	 * @param deploymentActionIndex
	 * @param changeSet
	 * @param change
	 * @throws Exception
	 */
	private void executeYAML(Deployment deployment, Transition transition, XMLDeploymentActionType actionFilter, ComponentDeploymentData component, ChangeSet changeSet, Change change) throws Exception {
		doUpdates(deployment, transition, component, changeSet, change);
		change.setPrepared(true);
		change.setTransitionId(transition.getSequenceNumber());
	}

	/**
	 * Returns true if none of the components in the bucket reference dependencies on other hosts
	 * @param thisBucket
	 * @param nextBucket
	 * @return
	 */
	private boolean noComponentsAreCrossMachineDependencies(List<ComponentDeploymentData> thisBucket, List<ComponentDeploymentData> nextBucket) {
		for (ComponentDeploymentData cd : nextBucket) {
			if (cd.getDeploymentDescriptorDef().getXMLType().getRequire() != null) {
				for (ComponentDeploymentData tcd : thisBucket) {
					/* These could be string arrays and the orders could be different but they could still be the same.
					 * We need a like for like comparison across all of the host names */
					Collection<ResolvedHost> tcdHosts = tcd.getHosts();
					Collection<ResolvedHost> cdHosts = cd.getHosts();
					log.debug("Comparing '" + tcdHosts + "' to '" + cdHosts + "'");
					if (!compareHosts(tcdHosts, cdHosts)) {
						log.debug("The host or list of hosts have differences.");
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Compares two host collections in a finicky way.  It gets finicky if the collections of hosts being compared are not in the 
	 * same order but semantically the same.
	 * @param tcdHosts
	 * @param cdHosts
	 * @return
	 */
	private boolean compareHosts(Collection<ResolvedHost> tcdHosts, Collection<ResolvedHost> cdHosts) {
		if (tcdHosts.size() != cdHosts.size()) {
			return false;
		}
		int findCount = 0;
		for (ResolvedHost cdHost : cdHosts) {
			boolean found = false;
			for (ResolvedHost tcdHost : tcdHosts) {
				if (tcdHost.equals(cdHost)) {
					found = true;
					findCount++;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		if (findCount == cdHosts.size()) {
			return true;
		}
		return false;
	}

	/**
	 * This returns a list of the bucket keys in ascending order (1,2,3,4) if it is a deploy and descending (4,3,2,1) if it is an undeploy.
	 * This is so tat we can order the operations properly
	 * @param buckets
	 * @param isUndeploy
	 * @return
	 */
	private List<String> getSortedListOfBuckets(Map<String, List<ComponentDeploymentData>> buckets, boolean isUndeploy) {
		List<String> list = new ArrayList<String>();
		for (Entry<String, List<ComponentDeploymentData>> orderEntry : buckets.entrySet()) {
			list.add(orderEntry.getKey());
		}
		if (isUndeploy) {
			Collections.sort(list, Collections.reverseOrder());
		}
		else {
			Collections.sort(list);
		}
		return list;
	}

	/**
	 * Returns true if any of the components in this bucket need updating and have downstream dependencies.
	 * @param list
	 * @return
	 */
	private boolean existsAComponentInBucketHavingDownstreamDependencies(List<ComponentDeploymentData> theNextBucket) {
		for (ComponentDeploymentData cd : theNextBucket) {
			List<ComponentDeploymentData> downstreamDependencies = cd.getDownstreamDependencies();
			if (downstreamDependencies != null) {
				for (ComponentDeploymentData downDep : downstreamDependencies) {
					List<ChangeSet> changeSets = downDep.getChangeSets();
					for (ChangeSet changeSet : changeSets) {
						if (changeSet.isComplexChange() == false) {
							if (changeTypeActionedByVersionChange(changeSet.getPrimaryChange().getChangeType()))
								return true;
						} else {
							for (Change subChange : changeSet.getSubTasks()) {
								if (changeTypeActionedByVersionChange(subChange.getChangeType()))
									return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean changeTypeActionedByVersionChange(ChangeType changeType) {
		switch (changeType) {
			case DEPLOY:
			case UNDEPLOY:
			case DOWNGRADE:
			case UPGRADE:
				return true;
			case NO_CHANGE:
			case FIX:
			case FAIL:
			default:
				return false;
		}
	}

	/**
	 * A whole chain re-deployment is bad, a dependencies only behaviour is less bad and neither of these is the best.
	 * @param bucket
	 * @param isUndeploy 
	 * @return
	 */
	private XMLChainBehaviourType findWorstBehaviourInBucket(boolean isUndeploy, List<ComponentDeploymentData> bucket) {
		XMLChainBehaviourType behaviour = null;
		for (ComponentDeploymentData component : bucket) {
			behaviour = setBadBehaviour(isUndeploy, behaviour, component);
		}
		return behaviour;
	}

	/**
	 * Sets the behaviour on a sliding scale of badness
	 * @param behaviour
	 * @param component
	 * @param isUndeploy 
	 * @return
	 */
	private XMLChainBehaviourType setBadBehaviour(boolean isUndeploy, XMLChainBehaviourType behaviour, ComponentDeploymentData component) {
		//We are only interested in multi_transition behaviours and we want to know the worst case
		XMLChainBehaviourType b = ComponentDeploymentDataManager.getChainBehaviour(isUndeploy, component);

		if (b != null) {
			switch (b) {
			case dependents_only_multi_transition:
				if (behaviour == null) {
					behaviour = b;
				}
				break;
			case whole_chain_multi_transition:
				behaviour = b;
				break;
			default:
				break;
			}
		}
		return behaviour;
	}

	/**
	 * Returns the relevant hiera file for this components
	 * @param component
	 * @retur
	 */
	private String findFileForComponent(ComponentDeploymentData component) {
		List<ChangeSet> actions = component.getChangeSets();
		for (ChangeSet changeSet : actions) {
			if (changeSet.isComplexChange()) {
				List<Change> subTaskActions = changeSet.getSubTasks();
				for (Change change : subTaskActions) {
					return getNullSafeYamlFileName(change);
				}
			}
			else {
				Change change = changeSet.getPrimaryChange();
				return getNullSafeYamlFileName(change);
			}
		}
		return null;
	}

	/**
	 * Returns the name of the file in the first YAMLSearchResiult it finds. 
	 * @param change
	 * @return
	 */
	private String getNullSafeYamlFileName(Change change) {
		String name = null;
		if (change != null) {
			StateSearchResult searchResult = change.getSearchResult();
			if (searchResult != null) {
				MachineState source = searchResult.getSource();
				if (source != null) {
					name = source.getSourceName();
				}
			}
		}
		return name;
	}

	/**
	 * Find the right bucket, put it in.  If the bucket does not exist, make it.  It either goes in an "undeploy" bucket or an "other" bucket
	 * @param fileBuckets
	 * @param component
	 * @param change
	 */
	private void putChangeInUndeployOrOtherBucket(Map<String, List<ComponentDeploymentData>> changeTypeBuckets, ComponentDeploymentData component, Change change) {
		String key = change.getChangeType() == ChangeType.UNDEPLOY ? TransitionManager.CHANGE_TYPE_GROUP_UNDEPLOY : TransitionManager.CHANGE_TYPE_GROUP_OTHER;
		List<ComponentDeploymentData> componentsList = changeTypeBuckets.get(key);
		if (componentsList == null) {
			componentsList = new ArrayList<ComponentDeploymentData>();
			changeTypeBuckets.put(key, componentsList);
		}
		if (!componentsList.contains(component) && DifferenceManager.isAMaterialChange(change)) {
			log.debug("Putting '" + component.getComponentName() + "' into the '" + key + "' bucket");
			componentsList.add(component);
		}
	}

	/**
	 * Select distinct maximumDepth/yamlFile combinations into different "buckets"  so each bucket contains only components whose chain depth is the same
	 * and whose YAML file is the same 
	 * @param changeTypeEntry
	 * @return
	 */
	private Map<String, List<ComponentDeploymentData>> separateByComponentOrderAndYamlFile(List<ComponentDeploymentData> changeList) {
		Map<String, List<ComponentDeploymentData>> orderBuckets = new TreeMap<String, List<ComponentDeploymentData>>();
		if (changeList != null) {
			for (ComponentDeploymentData component : changeList) {
				Integer depth = component.getMaximumDepth();
				String key2 = findFileForComponent(component);
				/*
				 * EBSAD-9423-Order-incorrect-on-mixedmultifile-deploy sort
				 * order should be depth first then name
				 */
				String compoundKey = String.format("%06d", depth) + "_" + key2;
				List<ComponentDeploymentData> componentsList = orderBuckets.get(compoundKey);
				if (componentsList == null) {
					componentsList = new ArrayList<ComponentDeploymentData>();
					orderBuckets.put(compoundKey, componentsList);
				}
				if (!componentsList.contains(component)) {
					log.debug("Putting '" + component.getComponentName() + "' into the '" + compoundKey + "' bucket");
					componentsList.add(component);
				}
			}
		}
		return orderBuckets;
	}

	/**
	 * Split the components out into buckets by change type
	 * @param entry
	 * @return
	 */
	private Map<String, List<ComponentDeploymentData>> getChangeTypeBuckets(Collection<ComponentDeploymentData> components) {
		log.debug("Splitting changes into either the 'undeploy' or the 'other' bucket");
		Map<String, List<ComponentDeploymentData>> changeTypeBuckets = new TreeMap<String, List<ComponentDeploymentData>>();
		for (ComponentDeploymentData component : components) {
			List<ChangeSet> actions = component.getChangeSets();
			for (ChangeSet changeSet : actions) {
				if (changeSet.isComplexChange()) {
					List<Change> subTaskActions = changeSet.getSubTasks();
					for (Change change : subTaskActions) {
						putChangeInUndeployOrOtherBucket(changeTypeBuckets, component, change);
					}
				}
				else {
					putChangeInUndeployOrOtherBucket(changeTypeBuckets, component, changeSet.getPrimaryChange());
				}
			}
		}
		return changeTypeBuckets;
	}

	/**
	 * Does the actual Updates
	 */
	private void doUpdates(Deployment deployment, Transition transition, ComponentDeploymentData component, ChangeSet changeSet, Change change) throws Exception {
		StateSearchResult searchResult = change.getSearchResult();
		if (component.getExistingState() != null && null != searchResult) {
			log.debug(String.format("Updating HieraData for '%s' in transition '%s' %s changeSetIsComplex=%s change%s", component.getComponentName(), transition.getSequenceNumber(), change.getChangeType(), changeSet.isComplexChange(), change.hashCode()));
			MachineState source = searchResult.getSource();
			Collection<ResolvedHost> componentHosts = component.getHosts();
			for (StateSearchResult existing : component.getExistingState()) {
				if (componentHosts.contains(new ResolvedHost(existing.getSource().getRoleOrFQDN(), existing.getSource().getEnvironmentName()))) {
					// Fine, found an Environment State we expect to be there 
				}
				else {
					// Component exists in a yaml we weren't expecting. Is this because the deployment descriptor
					// has been changed to no longer require this component in this zone? Maybe this zone
					// has multiple apps with shared components deployed to it? We just don't know.
					log.warn(String.format("Component [%s] already present in file [%s] but not mapped to that zone by the deployment descriptor. Deployment descriptor maps to [%s]", component.getComponentName(), existing.getSource().getSourceName(), componentHosts));
				}
			}

			MachineState toBeUpdated = deployment.getEnvironmentStateManager().getEnvironmentState(source.getEnvironmentName(), source.getRoleOrFQDN());
			if (toBeUpdated != null) {
				HieraEnvironmentUpdate result = yamlManager.updateYaml(component, change.getChangeType(), toBeUpdated);
				if (result != null) {
					transition.getUpdates().add(result);
				}
			}
		}
		else {
			// Adding new hiera for this component
			log.debug("Adding HieraData for '" + component.getComponentName() + "' in transition '" + transition.getSequenceNumber() + "'");
			Collection<ResolvedHost> componentHosts = component.getHosts();
			for (ResolvedHost host : componentHosts) {
				log.debug(String.format("Looking for Hiera file for %s", host));
				
				// Assume that if we're adding the same component to multiple hieras, it's ok to do the updates as part of the same transition
				MachineState toBeUpdated = deployment.getEnvironmentStateManager().getEnvironmentState(host.getZone(), host.getHostOrRole());
				
				if (!YamlManager.isStateFoundForThisComponent(component, toBeUpdated)) {
					if (toBeUpdated != null) {
						String message = String.format("Environment State for '%s' in '%s' will be created", host.getHostOrRole(), host.getZone());
						log.info(message);
						addChangeWarning(change, message);
						
						HieraEnvironmentUpdate result = yamlManager.updateYaml(component, change.getChangeType(), toBeUpdated);
						
						if (result != null) {
							transition.getUpdates().add(result);
						}
					} else {
						String message = String.format("Environment state for '%s' in zone '%s' does not exist and cannot be created", host.getHostOrRole(), host.getZone());
						log.warn(message);
						addChangeWarning(change, message);
					}
				}
			}
		}
	}
	
	private void addChangeWarning(Change change, String warning) {
		if (change.getWarning() == null) {
			change.setWarning(warning);
		} else {
			change.setWarning(String.format("%s, %s", change.getWarning(), warning));
		}
	}

	/**
	 * Responsible for creating a transition with all the right bits in it
	 * @param deployment
	 * @param ti
	 * @param xmlStep
	 * @param transitions 
	 * @return
	 */
	private Transition createTransition(Deployment deployment, List<Transition> transitions) {
		Transition transition = new Transition();
		transitions.add(transition);
		int ti = transitions.size() - 1;
		transition.setSequenceNumber(ti);
		log.debug("Created new transition '" + ti + "'");
		return transition;
	}
	
	public List<Transition> createPhasePreTransitions(XMLPhaseAdditionalActionsType xmlPhaseActions, Deployment deployment) throws Exception {
		return createPhasePrePostTransitions(xmlPhaseActions, deployment);
	}
	
	public List<Transition> createPhasePostTransitions(XMLPhaseAdditionalActionsType xmlPhaseActions, XMLStopType xmlStopType, Deployment deployment) throws Exception {
		List<Transition> transitions = createPhasePrePostTransitions(xmlPhaseActions, deployment);
		
		// Stop after this phase?
		if (xmlStopType != null) {
			// Pass null as current transition so a new one is created and added to the list
			addStopAction(xmlStopType.getMessage(), null, transitions);
			log.info(String.format("Added stop to phase post transitions"));
		}
		
		return transitions;
	}
	
	private List<Transition> createPhasePrePostTransitions(XMLPhaseAdditionalActionsType xmlPhaseActions, Deployment deployment) throws Exception {
		List<Transition> transitions = new ArrayList<>();
		if (xmlPhaseActions != null && xmlPhaseActions.getStep() != null) {
			for (XMLPhaseStepItemType step : xmlPhaseActions.getStep()) {
				Transition currentTransition = null;
				
				if (step.getInjectOrExecuteOrRemove() != null) {
					for (JAXBElement<?> stepItem : step.getInjectOrExecuteOrRemove()) {
						Object stepItemValue = stepItem.getValue();
						if (stepItemValue instanceof XMLInjectType) {
							currentTransition = addPhaseInjectOrRemove(new StepItem((XMLInjectType) stepItemValue), deployment, transitions, currentTransition);
						} else if (stepItemValue instanceof XMLExecuteType) {
							currentTransition = addPhaseExecute(new StepItem((XMLExecuteType) stepItemValue), transitions);
						} else if (stepItemValue instanceof XMLRemoveType) {
							currentTransition = addPhaseInjectOrRemove(new StepItem((XMLRemoveType) stepItemValue), deployment, transitions, currentTransition);
						} else if (stepItemValue instanceof XMLWaitType) {
							currentTransition = addPhaseWait((XMLWaitType) stepItemValue, transitions);
						} else {
							throw new IllegalArgumentException(String.format("Invalid XML type %s. Fatal", stepItemValue));
						}
					}
				}
			}
		}
		return transitions;
	}
	
	private Transition addPhaseInjectOrRemove(StepItem item, Deployment deployment, List<Transition> transitions, Transition transition) throws Exception {
		item.setHosts(HostnameResolver.buildPhaseResolvedHosts(item.getXmlType().getHostnames()));
		return addInjectionOrRemoveAction(item, null, deployment, transitions, transition, null, null);
	}
	
	private Transition addPhaseWait(XMLWaitType xmlWait, List<Transition> transitions) {
		BigInteger xmlSeconds = xmlWait.getSeconds();
		
		int maxWaitSecs = Configuration.getDeploymentMaxWaitSecs();
		if (xmlSeconds.intValue() > maxWaitSecs) {
			throw new IllegalArgumentException(String.format("The wait defined in the deployment descriptor of %d secs is greater than the allowed maximum of %d secs", xmlSeconds.intValue(), maxWaitSecs));
		}
		
		// Pass null as the current transition so that a fresh one will be created for this wait
		addWaitAction(xmlSeconds.intValue(), null, transitions);
		// Return null so that no subsequent items are added to this wait transition
		return null;
	}
	
	private Transition addPhaseExecute(StepItem item, List<Transition> transitions) throws Exception {
		item.setHosts(HostnameResolver.buildPhaseResolvedHosts(item.getXmlType().getHostnames()));
		// Pass null as the current transition so that a fresh one will be created for this command 
		addCommandAction(item, "", null, transitions, null, null);
		// Return null so that no subsequent items are added to this command transition
		return null;
	}
	
	

}
