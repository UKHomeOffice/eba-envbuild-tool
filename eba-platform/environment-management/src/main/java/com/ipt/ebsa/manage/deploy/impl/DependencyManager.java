package com.ipt.ebsa.manage.deploy.impl;

import static com.ipt.ebsa.hiera.HieraData.ABSENT;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.deployment.descriptor.XMLChainBehaviourType;
import com.ipt.ebsa.environment.EnvironmentUtil;
import com.ipt.ebsa.hiera.requires.Clazz;
import com.ipt.ebsa.hiera.requires.Exec;
import com.ipt.ebsa.hiera.requires.Mount;
import com.ipt.ebsa.hiera.requires.Other;
import com.ipt.ebsa.hiera.requires.PckageSTR;
import com.ipt.ebsa.hiera.requires.Require;
import com.ipt.ebsa.hiera.requires.RequireSyntax;
import com.ipt.ebsa.hiera.requires.Service;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;

/**
 * This class contains logic for working out dependency chains so that they can be used in deployment logic.
 * It works off the ComponentDeploymntData and does two passes through the data.  The first is to go and find 
 * all of the upstream and downstream dependencies of all of the components and the second is to put them all 
 * together into chains which can be traversed.
 *  
 * @author scowx
 */
public class DependencyManager {

	private Logger log = LogManager.getLogger(DependencyManager.class);
	
	/**
	 * Indicates how a given {@link ChangeType} will manifest in terms of the actual deployment.
	 */
	private static enum Choice { undeploy, undeployredeploy, deploy, none }

    
    /**
     * This function looks at all of the deployment actions in context of all the other deployment actions to find out if 
     * there are any deployment actions which affect other components.
     * 
     * An excellent example is two components which are joined together in a dependency chain.  If one of the components is being redeployed
     * and the rules for these components mean that the chain needs to be installed end to end then we need to uninstall all the components in the chain
     * in order and redeploy them again in order just to affect the change that we need.
     * 
     * @param components dependency tree
     * @param deployment object
     */
    public void applyDependencyChains(TreeMap<ComponentId, TreeMap<ComponentId, ?>> chains, Deployment deployment) {
    	log.debug("APPLYING CHAINS");
    	for (Entry<ComponentId, ComponentDeploymentData> entry : deployment.getComponents().entrySet()) {
			String logPrefix = "  ";
			ComponentDeploymentData component = entry.getValue();
		    log.debug(logPrefix + "Examining component '"+component.getComponentName()+"'");
		    List<ChangeSet> actions = component.getChangeSets();
		    log.debug(logPrefix + component.getComponentName()+" has '"+actions.size()+"' changesets");
			for (int deploymentActionIndex = 0; deploymentActionIndex < actions.size(); deploymentActionIndex++) {
				logPrefix = "    ";
				ChangeSet action = actions.get(deploymentActionIndex);
				Change primary = action.getPrimaryChange();
				log.debug(logPrefix + component.getComponentName()+" action["+deploymentActionIndex+"] primary:'"+primary.getChangeType()+"' isComplex:" + action.isComplexChange());
				if (action.isComplexChange()) {
					List<Change> subTaskActions = action.getSubTasks();
					log.debug(logPrefix + component.getComponentName()+" action["+deploymentActionIndex+"] has '"+subTaskActions.size()+"' sub tasks.");
                    for (int subtaskIndex = 0; subtaskIndex < subTaskActions.size(); subtaskIndex++) {
                    	logPrefix = "      ";
                    	Change a = subTaskActions.get(subtaskIndex);
                    	log.debug(logPrefix + component.getComponentName()+" action["+deploymentActionIndex+"] subtask["+subtaskIndex+"] is '"+a.getChangeType()+"'.");
                    	analyseAction(deployment, component, primary, a, logPrefix + "  ");
					}					
				}
				else {
					analyseAction(deployment, component, primary, primary, logPrefix + "  ");
				}
			}
		}
    }

    /**
     * Determines the chain behaviour for the component then adds undeploy/deploy steps as necessary.
     * 
     * @param components The components graph.
     * @param component The component under inspection
     * @param primaryChange
     * @param secondaryChange
     */
	@SuppressWarnings("serial")
	private void analyseAction(Deployment deployment, ComponentDeploymentData component, Change primaryChange, Change secondaryChange, String logPrefix) {
		log.debug(logPrefix + "Change Type: " + secondaryChange.getChangeType());

		if (ChangeType.UNDEPLOY == secondaryChange.getChangeType() || ChangeType.DEPLOY == secondaryChange.getChangeType()) {
			Boolean isUndeploy = (ChangeType.UNDEPLOY == secondaryChange.getChangeType());
			XMLChainBehaviourType behaviour = ComponentDeploymentDataManager.getChainBehaviour(isUndeploy, component);

			log.debug(logPrefix + "Change behaviour: " + behaviour);
			
			Boolean addFullChain = null;
			if (behaviour != null) {
				switch (behaviour) {
					case dependents_only_multi_transition:
					case dependents_only_single_transition:
						//If this component is undeployed then upstream components need to be undeployed too
						addFullChain = Boolean.FALSE;
						break;
					case whole_chain_multi_transition:
					case whole_chain_single_transition:
						//If this component is undeployed then upstream and downstream components need to be undeployed too
						addFullChain = Boolean.TRUE;
						break;
					case isolated:
						//This component can be undeployed in isolation so no problem here
						break;
				}	
			}
			log.debug(logPrefix + "Add all dependents: " + addFullChain);    
			if (addFullChain != null) {
				/* Does this mean that the entire tree needs to be undeployed and redeployed?  
				 * I think it does, all branches too even if they have nothing to do 
				 * with the original component - the moral is, don't make crazy chains .... make daisy chains ;)
				 */
				TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains = component.getDependencyGraph();
				
				for (final Entry<ComponentId, TreeMap<ComponentId, ?>> treeMap : dependencyChains.entrySet()) {
					TreeMap<ComponentId, TreeMap<ComponentId, ?>> chain = new TreeMap<ComponentId, TreeMap<ComponentId, ?>>() {{put(treeMap.getKey(), treeMap.getValue());}};
					if (componentInChain(chain, component.getComponentId())) {
						log.debug(logPrefix + "About to find all components in the same chain and update them.");  
						Choice choice;
						
						switch (primaryChange.getChangeType()) {
							case DEPLOY:
								choice = Choice.deploy;
								break;
							case UNDEPLOY:
								choice = Choice.undeploy;
								break;
							default:
								choice = Choice.undeployredeploy;
						}
						
						traverseTheTree(chain, deployment, component, choice, addFullChain, logPrefix + "  ");
					}
				}
			}
		}
	}
	
	
	/**
	 * @return {@code true} if the component appears in the tree, {@code false} if not.
	 */
	@SuppressWarnings("unchecked")
	private boolean componentInChain(TreeMap<ComponentId, TreeMap<ComponentId, ?>> chain, ComponentId componentId) {
		if (chain.containsKey(componentId)) {
			log.debug("Found match for " + componentId);
			return true;
		}
		log.debug("Component " + componentId + "does not match, so considering it's children");
		
		for (ComponentId sibling : chain.keySet()) {
			log.debug("Considering [" + sibling + "]") ;
			if (componentInChain((TreeMap<ComponentId, TreeMap<ComponentId, ?>>) chain.get(sibling), componentId)) {
				return true;
			}
		}
		return false;
	}
	

    /**
     * Top to bottom tree traversal which adds sub tasks into the entries it finds. If the "all" flag is true from the start then all components 
     * will have subtasks added if they have not been added already. If the "all" flag is not true from the start then it will become true as soon as it gets
     * to the component in question.  This is how we add the tasks to either the entire tree of just the dependencies upstream of the component.
     *
     * @param chain Representation of the full component object graph
     * @param components The full set of components in the graph
     * @param component The component currently being considered (where dependents-only chain behaviour is in play, this is
     * the component which will mark the start in the chain from which dependents will be updated)
     * @param choice The deployment action to take.
     * @param updateWholeTree Indicates whether the whole tree should be updated (for whole-chain transitions) or not (for dependents-only transitions)
     * @param logPrefix
     */
    @SuppressWarnings("unchecked")
	private void traverseTheTree(TreeMap<ComponentId, TreeMap<ComponentId, ?>> chain, Deployment deployment, ComponentDeploymentData component, Choice choice, boolean updateWholeTree, String logPrefix) {
    	for (ComponentId componentId : chain.keySet()) {
    		
    		if (updateWholeTree && deployment.getComponents().get(componentId).getTargetCmponentVersion() != null 
    				&& !deployment.getComponents().get(componentId).getTargetCmponentVersion().getComponentVersion().equals(ABSENT)) { // shouldn't be adding subtasks for absent components,
																														 // even if requested to via the 'add' param
				log.debug(logPrefix + "Adding subtasks to " + componentId);
				addSubTasks(deployment, choice, componentId);
			}
			if (chain.get(componentId) != null) {
				boolean addBelow = updateWholeTree;
				if (!addBelow) {
					addBelow = componentId.equals(component.getComponentId());
				}
				log.debug(logPrefix + "Traversing again " + componentId);
				traverseTheTree((TreeMap<ComponentId, TreeMap<ComponentId, ?>>)chain.get(componentId), deployment, component, choice, addBelow, logPrefix + "  ");	
			}
		}
    }

	private void addSubTasks(Deployment deployment, Choice choice, ComponentId componentId) {
		ComponentDeploymentData componentDeploymentData = deployment.getComponents().get(componentId);
		List<ChangeSet> changeSets = componentDeploymentData.getChangeSets();
		
		for (ChangeSet group : changeSets) {
			Choice thisChoice = choice;
			
			log.debug("PrimaryChange: " + group.getPrimaryChange().getChangeType() + " choice: " + thisChoice);
			
			/*
			 * added to fix EBSAD-8418, where whole-chain-multi-transition was undeploying dependencies and not redeploying
			 * them if they were not to be undeployed.
			 */
			switch(group.getPrimaryChange().getChangeType()) {
				case NO_CHANGE_OUT_OF_SCOPE:
					thisChoice = Choice.none;
					break;
				case NO_CHANGE:
				case FIX:
					thisChoice = Choice.undeployredeploy;
					break;
				case DEPLOY:
					thisChoice = Choice.deploy;
					break;
				case UNDEPLOY:
					thisChoice = Choice.undeploy;
					break;
				default:
					break;
			}
			
			switch (thisChoice) {
			    case undeploy: 
			    	DifferenceManager.createSubtask(deployment, componentDeploymentData, group, ChangeType.UNDEPLOY, group.getPrimaryChange().getSearchResult());
			    	break;
			    case undeployredeploy:
			    	DifferenceManager.createSubtask(deployment, componentDeploymentData, group, ChangeType.UNDEPLOY, group.getPrimaryChange().getSearchResult());
			    	DifferenceManager.createSubtask(deployment, componentDeploymentData, group, ChangeType.DEPLOY, group.getPrimaryChange().getSearchResult());
			    	break;
			    case deploy:
			    	// Don't need a secondary action for a straight deploy at the moment
			    	break;
				default:
					break;
			}
		}
	}
	
	/**
	 * This is the main entry point into this class.  the components map needs to be populated with components 
	 * @param components dependency tree
	 * @return populated components dependency tree
	 */
     public TreeMap<ComponentId, TreeMap<ComponentId, ?>> resolveDependencyChains(Map<ComponentId, ComponentDeploymentData> components) {
    	log.debug("RESOLVING CHAINS");
    	String logPrefix = " ";
		/*  First pass through, look for dependencies, add parents to children and vice versa . */
		for (Entry<ComponentId, ComponentDeploymentData> entry : components.entrySet()) {
			ComponentDeploymentData child = entry.getValue();
		    String applicationShortName = child.getApplicationShortName();
			Map<String, Object> deploymentDescriptorYaml = child.getDeploymentDescriptorYaml();
		    log.debug(logPrefix + "Examining component: " + child.getComponentName());
			if (deploymentDescriptorYaml != null) {
		    	List<String> requires = EnvironmentUtil.getListFromStringOrArray(deploymentDescriptorYaml, "require");
				List<Require<?>> parsedRequires = parseRequires(requires);
				log.debug(logPrefix + " " + parsedRequires.size() + " require statements in deployment descriptor YAML");
				for (Require<?> require : parsedRequires) {
					String componentName = (String) require.getRequired();
					lookupAndAddRequiredComponent(components, child, new ComponentId(componentName, applicationShortName), logPrefix + "  ");
				}
		    }
		    else {
		    	log.warn(logPrefix + " " + child.getComponentName() + " does not have a deployment descriptor with YAML in it. ");
		    }
		    
		    /* We also need to look at the require attribute on the deployment descriptor */
			String requiresFromDD = child.getDeploymentDescriptorDef() != null ? child.getDeploymentDescriptorDef().getXMLType().getRequire() : null;
			if (requiresFromDD != null) {
				String[] requiresItems = requiresFromDD.split(",");
				log.debug(logPrefix + " " + requiresItems.length + "require statements in deployment descriptor component element.");
				for (int i = 0; i < requiresItems.length; i++) {
					lookupAndAddRequiredComponent(components, child, new ComponentId(requiresItems[i].trim(), applicationShortName), logPrefix + "  ");
				}
			}
			else {
				log.debug(logPrefix + " " + "0 require statements in deployment descriptor component element");
			}
		}
		
		/* Second pass through, resolve the dependencies into connected chains which we can traverse easily */
		TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains = new TreeMap<>();
		populateChains(components, dependencyChains, logPrefix );
		return dependencyChains;
		
	}

	private void lookupAndAddRequiredComponent(Map<ComponentId, ComponentDeploymentData> components, ComponentDeploymentData child, ComponentId componentName, String logPrefix) {
		log.debug(logPrefix + "Required: " + componentName);
		ComponentDeploymentData parent = components.get(componentName);
		if (parent == null) {
			/* This component has no upstream dependencies */
		}
		else {
		  /* This one does */
		  parent.getDownstreamDependencies().add(child);
		  child.getUpstreamDependencies().add(parent);
		}
	}
	
	/**
	 * Entry point to a recursive set of methods which populates all of the dependency chains for all of the components
	 * 
	 * @param components The raw component data to create the component graph from
	 * @param dependencyChains The component graph
	 * @param logPrefix
	 */
	private void populateChains(Map<ComponentId, ComponentDeploymentData> components, TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyChains, String logPrefix) {
		for (Entry<ComponentId, ComponentDeploymentData> entry : components.entrySet()) {
		    ComponentDeploymentData value = entry.getValue();
		    log.debug(logPrefix + "Component: " + value.getComponentName());
		    populateDependencyChainsUpwards(dependencyChains, value, logPrefix + " ", new ArrayList<ComponentId>());
		}
	}

	/**
	 * Recursive method to follow dependency chains upwards and then follow them down again. It returns the count of the number of
	 * places that this component is from the top of its tree
	 * 
	 * @param dependencyGraph The component dependency graph.
	 * @param value The component currently under inspection, used as the starting point from which to move up or down then chain.
	 * @param prefix logging prefix.
	 * @param breadcrumbTrail List of components already seen (in the order they were seen) whilst moving up (or down) the chain.
	 */
	@SuppressWarnings("unchecked")
	private void populateDependencyChainsUpwards(TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyGraph, ComponentDeploymentData value, String prefix, ArrayList<ComponentId> breadcrumbTrail) {
		log.debug(prefix + "Going upward >>  " + value.getComponentName());
		
		// First check *up* the ancestry to see if we have been here before
		checkForCircularDependencies(breadcrumbTrail, value.getComponentId());
		
		// Now go further
		List<ComponentDeploymentData> upstreamDependencies = value.getUpstreamDependencies();
		if (upstreamDependencies == null || upstreamDependencies.size() < 1) {
			// This component does not depend on anything, we are at the top of a chain.
			TreeMap<ComponentId, TreeMap<ComponentId, ?>> newLink = (TreeMap<ComponentId, TreeMap<ComponentId, ?>>) dependencyGraph.get(value.getComponentId());
			if (newLink == null) {
			   newLink = new TreeMap<ComponentId, TreeMap<ComponentId, ?>>();
			   dependencyGraph.put(value.getComponentId(), newLink);
			   log.debug(prefix + "Adding '"+dependencyGraph + "' to '"+value.getDependencyGraph()+"'");
			   value.setDependencyGraph(dependencyGraph);
			   log.debug(prefix + "Adding "+value.getComponentName()+" to base chains");
			   value.setMaximumDepth(0);
			} else {
				log.debug(prefix + "Chain exists for "+value.getComponentName()+".");
			}
						
			/* This is the recursive bit, we look at all of the children that depend on this component and add them to the list */
			List<ComponentDeploymentData> itemsThatDependOnThisComponent = value.getDownstreamDependencies();
			if (itemsThatDependOnThisComponent.size() > 0) {
				log.debug(prefix + "Digging downwards.");	
			}
			for (ComponentDeploymentData child : itemsThatDependOnThisComponent) {
				populateDependencyChainsDownwards(dependencyGraph, newLink, child, prefix + " ", 1, new LinkedList<ComponentId>());	
			}
			log.debug(prefix + "Finished digging downwards.");
		} else {
			// This component has dependencies lets keep digging until we hit the top of the chain
			log.debug(prefix + "Component " + value.getComponentName() + " has " + upstreamDependencies.size() + " upstream dependencies.");
			for (ComponentDeploymentData parent : upstreamDependencies) {
				ArrayList<ComponentId> trail = breadcrumbTrail;
				if (upstreamDependencies.size() > 1) {
					// We split off here to follow the trail upstream so we need a breadcrumb trail for each split
					trail = (ArrayList<ComponentId>) breadcrumbTrail.clone();
				}
				populateDependencyChainsUpwards(dependencyGraph, parent, prefix + " ", trail);				
			}
		}
	}
	
	/**
	 * Another recursive method, this time to follow dependency chains downwards.
	 * Note the behaviour of the depth parameter.  If this component has previously been set to a deeper depth then the depth jumps to the deeper level
	 *
	 * @param dependencyGraph The full dependency graph.
	 * @param parentInChain A top level component in the graph (ie. one which has no depdencies of its own, but is depended on by others).
	 * @param value The component currently being considered.
	 * @param prefix logging prefix.
	 * @param depth The depth of {@code value} in comparison to the top level component.
	 * @param breadcrumbTrail The list of components we've already encountered.
	 */
	@SuppressWarnings("unchecked")
	private void populateDependencyChainsDownwards(TreeMap<ComponentId, TreeMap<ComponentId, ?>> dependencyGraph, TreeMap<ComponentId, TreeMap<ComponentId, ?>> parentInChain, ComponentDeploymentData value, String prefix, int depth, LinkedList<ComponentId> breadcrumbTrail) {
		ComponentId componentId = value.getComponentId();
		
		// Check *down* through our descendants to check we won't mysteriously sire a replica of ourselves
		checkForCircularDependencies(breadcrumbTrail, componentId);
		
		List<ComponentDeploymentData> dependencies = value.getDownstreamDependencies();
		TreeMap<ComponentId, TreeMap<ComponentId, ?>> chain = null;
		
		if (parentInChain.get(componentId) != null) {
		    log.debug(prefix + "Chainlink found for component "+componentId + ", there are '"+dependencies.size()+"' downstream dependencies (depth="+depth+")");
		    chain = (TreeMap<ComponentId, TreeMap<ComponentId, ?>>) parentInChain.get(componentId) ;
		} else {
			log.debug(prefix + "Adding chainlink for component "+componentId + ", there are '"+dependencies.size()+"' downstream dependencies (depth="+depth+")");
			chain = new TreeMap<ComponentId, TreeMap<ComponentId, ?>>();
		    parentInChain.put(componentId, chain);
		    depth = value.setMaximumDepth(depth);
		    log.debug(prefix + "Adding '"+dependencyGraph + "' to '"+value.getDependencyGraph()+"'");
		    value.setDependencyGraph(dependencyGraph);
		}
		
		if (dependencies != null && dependencies.size() > 0) {
			for (ComponentDeploymentData componentDeploymentData : dependencies) {
				LinkedList<ComponentId> trail = breadcrumbTrail;
				if (dependencies.size() > 1) {
					// We split off here to follow the trail downstream so we need a breadcrumb trail for each split
					trail = (LinkedList<ComponentId>) breadcrumbTrail.clone();
				}
				populateDependencyChainsDownwards(dependencyGraph, chain, componentDeploymentData, prefix + " ", depth+1, trail);	
			}
		}
			
	}

	private void checkForCircularDependencies(List<ComponentId> breadCrumbTrail, ComponentId componentId) {
		if (breadCrumbTrail.contains(componentId)) {
			throw new RuntimeException("Circular dependency detected, component '"+componentId+"' already exists in this chain '"+breadCrumbTrail+"'");
		} else {
			breadCrumbTrail.add(componentId);
		}
	}
	
	/**
	 * This takes a list of Requires objects and formalises them into something we can work with
	 * @param require
	 * @return
	 */
	public List<Require<?>> parseRequires(List<String> require) {
		List<Require<?>> parsedRequires = new ArrayList<Require<?>>();										
		if (require != null) {
			for (String string : require) {
				int index1 = string.indexOf("[");
				int index2 = string.indexOf("]");
				if (index1 > 0 && index2 > 0) {
					String classifier = string.substring(0, index1);
					String value = string.substring(index1+1, index2);
					RequireSyntax r = RequireSyntax.fromSyntax(classifier);
					switch (r) {
					   case Class: parsedRequires.add(new Clazz(value));break;
					   case Exec: parsedRequires.add(new Exec(value));break;
					   case File: parsedRequires.add(new com.ipt.ebsa.hiera.requires.File(value));break;
					   case Mount: parsedRequires.add(new Mount(value));break;
					   case Package: parsedRequires.add(new PckageSTR(value));break;
					   case Service: parsedRequires.add(new Service(value));	   break;		
					   default: parsedRequires.add(new Other(value));	   break; 
					}
				}
				else {
					log.error("I have no idea: " + string, new Exception("Some Hiera syntax has been encountered which the tool is unable to comprehend."));
				}
				
			}
		}
		return parsedRequires;
	}
	
}
