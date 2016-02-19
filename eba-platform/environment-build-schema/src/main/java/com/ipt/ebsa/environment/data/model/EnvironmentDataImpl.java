package com.ipt.ebsa.environment.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.ImmutableSet;
import com.ipt.ebsa.environment.data.factory.XMLHelper;
import com.ipt.ebsa.environment.v1.build.XMLActionCallType;
import com.ipt.ebsa.environment.v1.build.XMLActionType;
import com.ipt.ebsa.environment.v1.build.XMLBuildRefType;
import com.ipt.ebsa.environment.v1.build.XMLBuildType;
import com.ipt.ebsa.environment.v1.build.XMLBuildType.XMLSequenceref;
import com.ipt.ebsa.environment.v1.build.XMLBuildsType;
import com.ipt.ebsa.environment.v1.build.XMLEnvironmentContainerType;
import com.ipt.ebsa.environment.v1.build.XMLEnvironmentType;
import com.ipt.ebsa.environment.v1.build.XMLFirewallHieraActionDefinitionType;
import com.ipt.ebsa.environment.v1.build.XMLGlobalParametersType;
import com.ipt.ebsa.environment.v1.build.XMLInfrastructureProvisioningActionDefinitionType;
import com.ipt.ebsa.environment.v1.build.XMLInternalHieraActionDefinitionType;
import com.ipt.ebsa.environment.v1.build.XMLParamType;
import com.ipt.ebsa.environment.v1.build.XMLSSHCommandActionDefinitionType;
import com.ipt.ebsa.environment.v1.build.XMLSequenceChoiceType;
import com.ipt.ebsa.environment.v1.build.XMLSequenceType;
import com.ipt.ebsa.environment.v1.build.XMLUserParameterType;

/**
 * An in-memory representation of the Environment build sequences.
 * 
 * This class represents a store of components from which a build can be comprised, and information
 * about which order to bung those components together in. It encapsulates:
 * - 'fully-formed' sequences and actions - These fully describe a particular base sequence or base action from the xml and
 * 	 are used as building blocks from which full builds may be made up
 * - Placeholders for subsequences and sub-actions - These are typically just collections of identifiers for the
 *   building blocks.
 *   
 * It is necessary to maintain the blocks and identifier separately as blocks may be spread across XML files and can
 * therefore only be put together once all files have been loaded.
 * 
 * @author David Manning
 */
public class EnvironmentDataImpl implements EnvironmentData {

	private static final Log LOG = LogFactory.getLog(EnvironmentDataImpl.class);
	
	private final Map<String, Build> builds = new HashMap<String, Build>();
	
	private final Map<String, List<BuildRef>> buildsPerEnvironment = new HashMap<String, List<BuildRef>>();
	
	private final Map<String, List<BuildRef>> buildsPerContainer = new HashMap<String, List<BuildRef>>();
	
	private final Map<String, SequencePlaceHolder> sequencePlaceholders = new HashMap<String, SequencePlaceHolder>();
	
	private final Map<String, ActionPlaceHolder> actionPlaceHolders = new HashMap<String, ActionPlaceHolder>();

	private static final Pattern CONTEXT_PARAMS = Pattern.compile("(.*)=(.*)");
	
	private final Map<String, String> globalConfig = new HashMap<String, String>();
	
	private XMLBuildsType allXML = new XMLBuildsType();
	
	private EnvironmentDetailsHolder details;
	
	/**
	 * Adds a top-level build to the store. Checks one with the same id isn't already there.
	 */
	public void addBuild(XMLBuildType rawBuild) {
		LOG.debug("Adding build [" + rawBuild.getId() + "] to store");
		Build build = buildbuildfrom(rawBuild);
		Build previous = builds.put(rawBuild.getId(), build);
		if (previous != null && !previous.equals(rawBuild)) {
			throw new IllegalStateException("A build with the same key already exists but is not equivalent (build id [" + build.getId() + "])");
		}
		allXML.getBuild().add(rawBuild);
	}

	
	/**
	 * Builds a build from a build (build).
	 */
	private Build buildbuildfrom(XMLBuildType rawBuild) {
		Build build = new Build(rawBuild.getId(), this);
		for (XMLSequenceref xmlSequence : rawBuild.getSequenceref()) {
			SequencePlaceHolder sequencePlaceHolder = new SequencePlaceHolder(xmlSequence.getSequenceid(), this);
			build.sequences.add(sequencePlaceHolder);
		}
		for (XMLParamType rawParam : rawBuild.getParam()) {
			build.getParameters().put(rawParam.getName(), rawParam.getValue());
		}
		return build;
	}

	
	/**
	 * Adds a sequence to the store. Checks one with the same id isn't already there.
	 */
	public void addSequence(XMLSequenceType rawSequence) {
		LOG.debug("Adding sequence [" + rawSequence.getId() + "] to store");
		SequencePlaceHolder sequence = new SequencePlaceHolder(rawSequence.getId(), this);
		for (Object object : rawSequence.getStepOrSequenceref()) {
			if (object instanceof XMLSequenceChoiceType.XMLStep) {
				StepPlaceHolder stepPlaceHolder = new StepPlaceHolder(this, ((XMLSequenceChoiceType.XMLStep)object).getActionid());
				stepPlaceHolder.parameters.putAll(readParamsFromContext(((XMLSequenceChoiceType.XMLStep) object).getContext(), ((XMLSequenceChoiceType.XMLStep) object).getActionid()));
				sequence.sequencesAndSteps.add(stepPlaceHolder);
			} else if (object instanceof XMLSequenceChoiceType.XMLSequenceref) {
				SequencePlaceHolder sequencePlaceHolder = new SequencePlaceHolder(((XMLSequenceChoiceType.XMLSequenceref) object).getSequenceid(), this);
				sequencePlaceHolder.parameters.putAll(readParamsFromContext(((XMLSequenceChoiceType.XMLSequenceref) object).getContext(), ((XMLSequenceChoiceType.XMLSequenceref) object).getSequenceid()));
				sequence.sequencesAndSteps.add(sequencePlaceHolder);
			} else {
				throw new UnsupportedOperationException("Step or Sequence type [" + object.getClass() + "] not yet supported");
			}
		}
		for (XMLParamType rawParam : rawSequence.getParam()) {
			sequence.parameters.put(rawParam.getName(), rawParam.getValue());
		}
		SequencePlaceHolder previous = sequencePlaceholders.put(rawSequence.getId(), sequence);
		if (previous != null && !previous.equals(sequence)) {
			throw new IllegalStateException("A sequence with the same key already exists but is not equivalent (sequence id [" + rawSequence.getId() + "])");
		}
		
		allXML.getSequence().add(rawSequence);
	}
	
	
	/**
	 * Adds a global key-value config to the store. Checks one with the same id isn't already there.
	 */
	public void addGlobalConfig(XMLParamType xmlParamType) {
		LOG.debug("Adding global config [" + xmlParamType.getName() + "] to store");
		if (globalConfig.containsKey(xmlParamType.getName()) && 
			!StringUtils.equals(globalConfig.get(xmlParamType.getName()), xmlParamType.getValue())) {
				throw new IllegalArgumentException(String.format("Attempting to insert global config value [%s] with key [%s] but it's already defined as [%s]", xmlParamType.getValue(), xmlParamType.getName(), globalConfig.get(xmlParamType.getName())));
		}
		globalConfig.put(xmlParamType.getName(), xmlParamType.getValue());
		
		if (null == allXML.getGlobalparams()) {
			allXML.setGlobalparams(new XMLGlobalParametersType());
		}
		
		allXML.getGlobalparams().getParam().add(xmlParamType);
	}
	
	
	public void addEnvironment(XMLEnvironmentType xmlEnvironmentType) {
		LOG.debug("Adding environment [" + xmlEnvironmentType.getName() + "] to store");
		addBuildRef(xmlEnvironmentType.getName(), xmlEnvironmentType.getBuildref(), buildsPerEnvironment);
		allXML.getEnvironment().add(xmlEnvironmentType);
	}
	
	
	public void addEnvironmentContainer(XMLEnvironmentContainerType xmlEnvironmentContainerType) {
		LOG.debug("Adding environment container [" + xmlEnvironmentContainerType.getName() + "] to store");
		addBuildRef(xmlEnvironmentContainerType.getName(), xmlEnvironmentContainerType.getBuildref(), buildsPerContainer);
		allXML.getEnvironmentcontainer().add(xmlEnvironmentContainerType);
	}
	
	private void addBuildRef(String envOrContainer, List<XMLBuildRefType> refs, Map<String, List<BuildRef>> refStore) {
		if (!refStore.containsKey(envOrContainer)) {
			refStore.put(envOrContainer, new ArrayList<BuildRef>());
		}
		for (XMLBuildRefType xmlBuildRefType : refs) {
			List<XMLUserParameterType> userParameters = new ArrayList<XMLUserParameterType>();
			List<XMLParamType> generalParams = new ArrayList<XMLParamType>();
			splitParamTypes(xmlBuildRefType, userParameters, generalParams);
			BuildRef ref = new BuildRef(xmlBuildRefType.getId(), xmlBuildRefType.getDisplayname(), xmlBuildRefType.getBuildid(), xmlBuildRefType.getProvider(), userParameters, generalParams);
			if (!addBuildRef(ref, refStore.get(envOrContainer))) {
				throw new IllegalStateException(envOrContainer + " contains a duplicate buildref id: " + ref.getId());
			}
		}
	}
	
	/**
	 * Adds newBuildRef to the list of existingBuildRefs only if there is not already a build ref with the same id in the list
	 * @since EBSAD-19298
	 * @param newBuildRef
	 * @param existingBuildRefs
	 * @return whether the newBuildRef was added to the list
	 */
	private boolean addBuildRef(BuildRef newBuildRef, List<BuildRef> existingBuildRefs) {
		String newBuildRefId = newBuildRef.getId();
		for (BuildRef existingBuildRef : existingBuildRefs) {
			if (existingBuildRef.getId().equals(newBuildRefId)) {
				return false;
			}
		}
		existingBuildRefs.add(newBuildRef);
		return true;
	}

	/**
	 * Split out the user parameters from the general parameters
	 * @param xmlBuildRefType
	 * @param userParameters
	 * @param generalParams
	 */
    public void splitParamTypes(XMLBuildRefType xmlBuildRefType, List<XMLUserParameterType> userParameters, List<XMLParamType> generalParams) {
    	List<Object> objects = xmlBuildRefType.getParameterOrUserparameter();
    	if (objects != null && objects.size() > 0) {
			for (Object obj : objects) {
				if (obj instanceof XMLParamType) {
					generalParams.add((XMLParamType)obj);
				}
				else if (obj instanceof XMLUserParameterType) {
					userParameters.add((XMLUserParameterType)obj);
				}
			}
		}
    }
    
	public void addAction(XMLActionType xmlActionType) {
		LOG.debug("Adding action [" + xmlActionType.getId() + "] to store");
		// First place holder represents the <eb:action> part (so it has an id)
		ActionCollectionPlaceHolder collection = new ActionCollectionPlaceHolder(xmlActionType.getId(), this);
		for (Object actionType : xmlActionType.getInfraOrCallOrSshcommand()) {
			ActionPlaceHolder action;
			// Child placeholder represent the SSH command, infra action etc.
			if (actionType instanceof XMLInfrastructureProvisioningActionDefinitionType) {
				action = new InfraActionPlaceHolder(xmlActionType.getId(), this, (XMLInfrastructureProvisioningActionDefinitionType) actionType);
			} else if (actionType instanceof XMLActionCallType) {
				// Really this is the only things that needs to be a placeholder, but meh.
				action = new CallActionPlaceHolder((String) ((XMLActionCallType) actionType).getId(), this, (XMLActionCallType) actionType);
			} else if (actionType instanceof XMLSSHCommandActionDefinitionType) {
				action = new SshActionPlaceHolder(xmlActionType.getId(), this, (XMLSSHCommandActionDefinitionType) actionType);
			} else if(actionType instanceof XMLInternalHieraActionDefinitionType) {
				action = new InternalHieraActionPlaceHolder(xmlActionType.getId(), this, (XMLInternalHieraActionDefinitionType) actionType);
			} else if(actionType instanceof XMLFirewallHieraActionDefinitionType) {
				action = new FirewallHieraActionPlaceHolder(xmlActionType.getId(), this, (XMLFirewallHieraActionDefinitionType) actionType);
			} else {
				throw new UnsupportedOperationException("XML action type [" + actionType.getClass().getSimpleName() + "] not supported at present");
			}
			collection.actions.add(action);
		}
		actionPlaceHolders.put(xmlActionType.getId(), collection);
		
		allXML.getAction().add(xmlActionType);
	}


	/**
	 * Assumes a context of the form x=y, a=b, c=d and splits it into key-value pairs.
	 */
	private Map<String, String> readParamsFromContext(String context, String sequenceOrStepId) {
		Map<String, String> resolvedParams = new HashMap<String, String>();
		if (context == null || context.isEmpty()) {
			return resolvedParams;
		}
		for (String rawParam : context.split(",")) {
			Matcher matcher = CONTEXT_PARAMS.matcher(rawParam);
			if (!matcher.find()) {
				LOG.error("Context parameter for sequence or step [" + sequenceOrStepId + "] invalid [" + rawParam + "]");
			}
			resolvedParams.put(matcher.group(1).trim(), matcher.group(2).trim());
		}
		return resolvedParams;
	}
	
		
	@Override
	public Set<Build> getBuilds() {
		return ImmutableSet.copyOf(builds.values());
	}
	
	@Override
	public SortedSet<AvailableBuildOrContainer> getAvailableBuilds(String provider) {
		SortedSet<AvailableBuildOrContainer> builds = new TreeSet<>();
		for (Entry<String, List<BuildRef>> entry : buildsPerEnvironment.entrySet()) {
			for (BuildRef mode: entry.getValue()) {
				String buildRefProvider = mode.getProvider();
				if (StringUtils.isEmpty(buildRefProvider) || buildRefProvider.equals(provider)) {
					builds.add(new AvailableBuildOrContainer(entry.getKey(), mode));
				}
			}
		}
		return builds;
	}
	
	@Override
	public SortedSet<AvailableBuildOrContainer> getAvailableContainers(String provider) {
		SortedSet<AvailableBuildOrContainer> builds = new TreeSet<>();
		for (Entry<String, List<BuildRef>> entry : buildsPerContainer.entrySet()) {
			for (BuildRef mode: entry.getValue()) {
				String buildRefProvider = mode.getProvider();
				if (StringUtils.isEmpty(buildRefProvider) || buildRefProvider.equals(provider)) {
					builds.add(new AvailableBuildOrContainer(entry.getKey(), mode));
				}
			}
		}
		return builds;
	}
	
	@Override
	public Map<String, String> getGlobalParameters() {
		return globalConfig;
	}
	
	
	/**
	 * Determine which build to use for a given environment and mode.
	 */
	@Override
	public Build getBuildForEnvironmentAndBuildId(String environment, String id) {
		LOG.info("Loading build for environment [" + environment + "] with ref id [" + id + "]");
		if(!buildsPerEnvironment.containsKey(environment) || getBuildRefForId(buildsPerEnvironment.get(environment), id) == null) {
			throw new IllegalArgumentException("Unable to retrieve build for environment [" + environment + "] for build Id [" + id + "]");
		}
		
		BuildRef buildRef = getBuildRefForId(buildsPerEnvironment.get(environment), id);
		
		LOG.debug("Environment [" + environment + "] with build ref [" + id + "] configured to use build plan [" + buildRef.buildName + "]");
		if (!builds.containsKey(buildRef.buildName)) {
			throw new IllegalStateException("No build defined with id [" + buildRef.id + "]");
		}
		Build build = builds.get(buildRef.buildName);
		build.getUserParameters().putAll(buildRef.uiParams);
		build.getParameters().putAll(buildRef.genParams);
		
		return build;
	}


	@Override
	public Build getBuildForEnvironmentContainerAndBuildId(String container, String id) {
		LOG.info("Loading build for environment [" + container + "] with ref id [" + id + "]");
		if(!buildsPerContainer.containsKey(container) || getBuildRefForId(buildsPerContainer.get(container), id) == null) {
			throw new IllegalArgumentException("Unable to retrieve build for container [" + container + "] for build Id [" + id + "]");
		}
		
		BuildRef buildRef = getBuildRefForId(buildsPerContainer.get(container), id);
		
		LOG.debug("Container [" + container + "] with build ref [" + id + "] configured to use build plan [" + buildRef.buildName + "]");
		if (!builds.containsKey(buildRef.buildName)) {
			throw new IllegalStateException("No build defined with id [" + buildRef.id + "]");
		}
		Build build = builds.get(buildRef.buildName);
		build.getUserParameters().putAll(buildRef.uiParams);
		build.getParameters().putAll(buildRef.genParams);
		
		return build;
	}


	private BuildRef getBuildRefForId(List<BuildRef> refs, String id) {
		for (BuildRef ref : refs) {
			if (ref.id.equals(id)) {
				return ref;
			}
		}
		return null;
	}
	
	
	public Map<String, ActionPlaceHolder> getActionPlaceHolders() {
		return actionPlaceHolders;
	}

	/**
	 * @return one XML document with all the XML that we have encountered
	 */
	public String getAllXmlAsXml() {
		return new XMLHelper().marshallBuildPlanXML(allXML);
	}


	public Map<String, SequencePlaceHolder> getSequencePlaceholders() {
		return sequencePlaceholders;
	}


	@Override
	public EnvironmentDetailsHolder getEnvironmentDetails() {
		return details;
	}
	
	public void setEnvironmentDetails(EnvironmentDetailsHolder details) {
		this.details= details;
	}
}
