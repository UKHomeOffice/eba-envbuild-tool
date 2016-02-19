package com.ipt.ebsa.environment.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.commons.lang.StringUtils;

/**
 * A set of subsequences and steps responsible for some element of an environment build.
 *
 * @author David Manning
 */
public class Sequence extends ParameterisedNode {

	private EnvironmentDataImpl data;
	private SequencePlaceHolder placeHolder;
	private Map<String, String> context;

	Sequence(EnvironmentDataImpl data, SequencePlaceHolder placeHolder, Map<String, String> context) {
		super(placeHolder.getId());
		this.data = data;
		this.placeHolder = placeHolder;
		this.context = context;
	}
	
	public List<ParameterisedNode> getChildren() {
		List<ParameterisedNode> sequencesAndSteps = new ArrayList<>();
		for (ParameterizedPlaceHolder subSequenceOrStep : placeHolder.sequencesAndSteps) {
			if (subSequenceOrStep instanceof SequencePlaceHolder) {
				if (!data.getSequencePlaceholders().containsKey(subSequenceOrStep.getId())) {
					throw new IllegalArgumentException("Sequence with id [" + subSequenceOrStep + "] not found.");
				}
				Sequence sequence = new Sequence(data, data.getSequencePlaceholders().get(subSequenceOrStep.getId()), subSequenceOrStep.getParameters());
				sequencesAndSteps.add(sequence);
			} else if (subSequenceOrStep instanceof StepPlaceHolder) {
				sequencesAndSteps.add(new Step((StepPlaceHolder) subSequenceOrStep, data));
			} else {
				throw new UnsupportedOperationException("Unable to extract actions from type [" + subSequenceOrStep.getClass() + "]");
			}
		}
		return sequencesAndSteps;
	}
	
	@Override
	public Map<String, String> getParameters() {
		HashMap<String, String> combinedParams = new HashMap<String, String>();
		combinedParams.putAll(placeHolder.getParameters()); // stuff from the context attribute
		combinedParams.putAll(data.getSequencePlaceholders().get(placeHolder.getId()).getParameters()); // params from actual eb:param nodes
		combinedParams.putAll(context);
		return combinedParams;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Sequence)) {
			return false;
		}
		if (!StringUtils.equals(getId(), ((Sequence)obj).getId())) {
			return false;
		}
		if (!context.equals(((Sequence)obj).context)) {
			return false;
		}
		
		return true;
	}
}
