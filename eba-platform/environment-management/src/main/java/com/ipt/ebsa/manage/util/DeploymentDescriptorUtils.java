package com.ipt.ebsa.manage.util;

import com.ipt.ebsa.deployment.descriptor.XMLFailureActionsType;
import com.ipt.ebsa.hiera.NodeMissingBehaviour;

public class DeploymentDescriptorUtils {

	/** 
	 * Returns a NodeMissingBehaviour corresponding to the relevant failure action.
	 * @param ifMissing
	 * @return 
	 */
	public static NodeMissingBehaviour translateBehaviour(XMLFailureActionsType ifMissing) {
		if (ifMissing == null) {
			return null;
		}
		else {
			switch (ifMissing) {
			case FAIL:
				return NodeMissingBehaviour.FAIL;
			case INSERT_ALL:
				return NodeMissingBehaviour.INSERT_ALL;
			case INSERT_KEY_AND_VALUE_AND_PARENT_MAP_ONLY:
				return NodeMissingBehaviour.INSERT_KEY_AND_VALUE_AND_PARENT_MAP_ONLY;
			case INSERT_KEY_AND_VALUE_ONLY:
				return NodeMissingBehaviour.INSERT_KEY_AND_VALUE_ONLY;
			default:
				throw new UnsupportedOperationException("Translation of FailureAction " + ifMissing + " not supported.");
			}
		}
	}
}
