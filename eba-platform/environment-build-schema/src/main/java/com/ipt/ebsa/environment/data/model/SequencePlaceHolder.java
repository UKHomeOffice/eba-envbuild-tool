package com.ipt.ebsa.environment.data.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * We can't resolve sequences til we've loaded all the XML, so keep a note of the subsequence ids, steps and parameters and 
 * we'll resolve the actual sequences later.
 *
 * @author David Manning
 */
final class SequencePlaceHolder extends ParameterizedPlaceHolder {
	
	final List<ParameterizedPlaceHolder> sequencesAndSteps = new ArrayList<>();
			
	public SequencePlaceHolder(String id, EnvironmentData environmentData) {
		super(id, environmentData);
	}

	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SequencePlaceHolder) {
			if (!((SequencePlaceHolder) obj).sequencesAndSteps.equals(sequencesAndSteps)) return false;
			return super.equals(obj);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(sequencesAndSteps).append(super.hashCode()).toHashCode();
	}
}