package com.ipt.ebsa.environment.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an environment-agnostic sequence of actions which may be used to create an environment.
 * 
 * @author David Manning
 */
public class Build extends ParameterisedNode {

	final List<SequencePlaceHolder> sequences = new ArrayList<SequencePlaceHolder>();
	private final EnvironmentDataImpl environmentData;
	
	/**
	 * Parameters defined by the xml, key-value pairs 
	 */
	private final HashMap<String, String> parameters = new HashMap<String, String>();
	
	/**
	 * Parameters which will be displayed to the user and added to the context. The
	 * key is the id for the parameter and the value is the display name. These parameters
	 * may be used in placeholders throughout the plans object graph in exactly the same
	 * way as regular parameters.
	 */
	private final HashMap<String, String> userParameters = new HashMap<String, String>();
	
	public Build(String id, EnvironmentDataImpl environmentData) {
		super(id);
		this.environmentData = environmentData;
	}

	
	public List<ParameterisedNode> getChildren() {
		List<ParameterisedNode> sequences = new ArrayList<ParameterisedNode>();
		for (SequencePlaceHolder sequence : this.sequences) {
			if (environmentData.getSequencePlaceholders().containsKey(sequence.getId())) {
				sequences.add(new Sequence(environmentData, environmentData.getSequencePlaceholders().get(sequence.getId()), sequence.getParameters()));
			} else {
				throw new IllegalStateException("Sequence with id [" + sequence.getId() + "] unknown.");
			}
		}
		return sequences;
	}


	@Override
	public Map<String, String> getParameters() {
		return parameters;
	}


	public HashMap<String, String> getUserParameters() {
		return userParameters;
	}
}
