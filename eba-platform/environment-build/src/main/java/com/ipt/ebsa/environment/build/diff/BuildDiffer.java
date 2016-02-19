package com.ipt.ebsa.environment.build.diff;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.javers.core.JaversBuilder;
import org.javers.core.MappingStyle;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ContainerElementChange;
import org.javers.core.diff.changetype.container.ElementValueChange;
import org.javers.core.diff.changetype.container.ListChange;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.diff.changetype.container.ValueRemoved;
import org.javers.core.metamodel.clazz.EntityDefinition;
import org.javers.core.metamodel.clazz.ValueObjectDefinition;
import org.javers.core.metamodel.object.InstanceId;
import org.javers.core.metamodel.object.ValueObjectId;
import org.reflections.Reflections;

import com.ipt.ebsa.environment.build.execute.BuildNode;
import com.ipt.ebsa.environment.build.execute.PerformerFactory;
import com.ipt.ebsa.environment.build.execute.action.AwsInfraActionPerformer;
import com.ipt.ebsa.environment.build.execute.action.ActionContext;
import com.ipt.ebsa.environment.build.execute.action.ActionPerformer;
import com.ipt.ebsa.environment.build.execute.action.ActionPerformerFactory;
import com.ipt.ebsa.environment.build.execute.action.InfraActionPerformer;
import com.ipt.ebsa.environment.build.execute.action.SkyscapeInfraActionPerformer;
import com.ipt.ebsa.environment.data.factory.EnvironmentDataFactory;
import com.ipt.ebsa.environment.data.model.Build;
import com.ipt.ebsa.environment.data.model.EnvironmentData;
import com.ipt.ebsa.environment.data.model.InfraAction;
import com.ipt.ebsa.environment.data.model.Sequence;
import com.ipt.ebsa.environment.data.model.SshAction;

public class BuildDiffer {
	
	private static final Logger LOG = Logger.getLogger(BuildDiffer.class.getName());
	
	private boolean hasChanges = false;
	
	/**
	 * Properties of ActionPerformers which should be ignored (over and above "parent", "buildContext" and "node").
	 */
	@SuppressWarnings("serial")
	private Map<Class<? extends ActionPerformer>, Set<String>> additionalPerformerIgnoredProperties = new HashMap<Class<? extends ActionPerformer>, Set<String>>() {{
		put(InfraActionPerformer.class, newHashSet("action")); // No need to dig into the XML (Action) itself, its diffing its toString is sufficient
	}};
	
	/**
	 * Properties of ActionContexts which should be ignored (over and above "actionContextMap").
	 */
	@SuppressWarnings("serial")
	private Map<Class<? extends ActionContext>, Set<String>> additionalContextIgnoredProperties = new HashMap<Class<? extends ActionContext>, Set<String>>() {{
		put(SkyscapeInfraActionPerformer.InfraActionContext.class, newHashSet("instructionXML")); // No need to dig into the instruction XML itself
		put(AwsInfraActionPerformer.InfraActionContext.class, newHashSet("instructionXML")); // No need to dig into the instruction XML itself
	}};
	
	
	public void calculateDifferences(File original, File current, String environment, String provider, String buildRef) throws URISyntaxException {
		BuildNode buildNode = getBuildNode(original, environment, provider, buildRef);
		BuildNode buildNode2 = getBuildNode(current, environment, provider, buildRef);
		
	    JaversBuilder builder = JaversBuilder.javers()
 			   .withMappingStyle(MappingStyle.BEAN)
 			   .registerEntity(new EntityDefinition(Build.class, "id"))
 			   .registerEntity(new EntityDefinition(BuildNode.class, "id", Arrays.asList("buildContext", "parent", "node")))
			   .registerEntity(new EntityDefinition(Sequence.class, "id"))
			   .registerEntity(new EntityDefinition(InfraAction.class, "id"))
			   .registerEntity(new EntityDefinition(SshAction.class, "id"));
		
	    registerActionPerformers(builder);
	    registerActionContexts(builder);
	    
	    Diff compare = builder.build().compare(buildNode, buildNode2);
	    
	    prettifyResults(compare, buildNode, buildNode2);
	    
	    if (!compare.getChanges().isEmpty()) {
	    	hasChanges = true;
	    }
	}


	private void registerActionPerformers(JaversBuilder builder) {
		for (Class<? extends ActionPerformer> p : getActionPerformerClasses()) {
	    	List<String> ignoredProperties = new ArrayList<>();
	    	ignoredProperties.addAll(asList("parent", "buildContext", "node"));
	    	if (additionalPerformerIgnoredProperties.containsKey(p)) {
	    		ignoredProperties.addAll(additionalPerformerIgnoredProperties.get(p));
	    	}
	    	builder.registerEntity(new EntityDefinition(p, "id", ignoredProperties));
		}
	}
	
	private void registerActionContexts(JaversBuilder builder) {
		for (Class<? extends ActionContext> p : getActionContextClasses()) {
	    	List<String> ignoredProperties = new ArrayList<>();
	    	ignoredProperties.addAll(asList("actionContextMap"));
	    	if (additionalContextIgnoredProperties.containsKey(p)) {
	    		ignoredProperties.addAll(additionalContextIgnoredProperties.get(p));
	    	}
	    	builder.registerValueObject(new ValueObjectDefinition(p, ignoredProperties));
		}
	}
	
	
	private Collection<Class<? extends ActionPerformer>> getActionPerformerClasses() {
		Reflections reflections = new Reflections("com.ipt.ebsa.environment.build");
		return reflections.getSubTypesOf(ActionPerformer.class);
	}
	
	private Collection<Class<? extends ActionContext>> getActionContextClasses() {
		Reflections reflections = new Reflections("com.ipt.ebsa.environment.build");
		return reflections.getSubTypesOf(ActionContext.class);
	}

	private BuildNode getBuildNode(File path, String environment, String provider, String buildRefId) throws URISyntaxException {
		EnvironmentDataFactory environmentDataFactory = new EnvironmentDataFactory();
		EnvironmentData environmentData = environmentDataFactory.getEnvironmentDataInstance(path);
		PerformerFactory performerFactory = new PerformerFactory(environmentData, new ActionPerformerFactory());
		BuildNode p = performerFactory.generateRootNode(environment, provider, buildRefId, new HashMap<String, String>());
		
		List<BuildNode> leaves = new ArrayList<BuildNode>();
		findLeaves(p, leaves);
		
		BuildNode dummy = new BuildNode();
		dummy.setNode(p.getNode());
		dummy.setChildren(leaves);
		
		return dummy;
	}
	
		
	private void findLeaves(BuildNode node, List<BuildNode> leaves) {
		List<BuildNode> children = node.getChildren();
		for (int i = 0; i < children.size(); i++) {
			BuildNode child = children.get(i);
			if (child instanceof ActionPerformer) {
				leaves.add(child);
			} else {
				findLeaves(child, leaves);
			}
		}
	}

	private void prettifyResults(Diff compare, BuildNode buildNode, BuildNode buildNode2) {
		List<String> newActions = new ArrayList<>();
		
		for (Change change : compare.getChanges()) {
			if (change instanceof ListChange) {
				// An action has changed
				ListChange l = (ListChange) change;
				if (l.getAffectedGlobalId() instanceof InstanceId) {
					for (ContainerElementChange c : l.getChanges()) {
						if (c instanceof ElementValueChange) {
							LOG.info(String.format("ACTION CHANGED: Action called at position [%d] has changed. Was [%s], now [%s]", c.getIndex(), ((InstanceId)((ElementValueChange) c).getLeftValue()).getCdoId(),
																																			 ((InstanceId)((ElementValueChange) c).getRightValue()).getCdoId()));
						} else if (c instanceof ValueAdded) {
							LOG.info(String.format("ACTION ADDED: Action [%s] added at position [%d]", ((ValueAdded) c).getAddedValue(), c.getIndex()));
						} else if (c instanceof ValueRemoved) {
							LOG.info(String.format("ACTION REMOVED: Action [%s] removed from position [%d]", ((ValueRemoved) c).getRemovedValue(), c.getIndex()));
						}
					}
				}
			} else if (change instanceof ValueChange) {
				ValueChange v = (ValueChange) change;
				if (v.getAffectedGlobalId() instanceof ValueObjectId) {
					// it's a change to a property
					ValueObjectId id = (ValueObjectId)v.getAffectedGlobalId();
					LOG.info(String.format("ACTION PROPERTY CHANGED: Action with id [%s] property [%s] was [%s] but is now [%s].", id.getOwnerId().getCdoId(), v.getProperty().getName(), v.getLeft(), v.getRight()));
				}
			} else if (change instanceof NewObject) {
				if (change.getAffectedGlobalId() instanceof InstanceId) {
					// we're looking at a new action, add it to the list for displaying later
					newActions.add(((InstanceId)change.getAffectedGlobalId()).getCdoId().toString());
				}
			} else {
				// Some other situation,
				LOG.info(change.toString());
			}
		}
		if (!newActions.isEmpty()) {
			LOG.info("New Actions: " + newActions);
		}
	}

	public boolean hasChanges() {
		return hasChanges;
	}
}
