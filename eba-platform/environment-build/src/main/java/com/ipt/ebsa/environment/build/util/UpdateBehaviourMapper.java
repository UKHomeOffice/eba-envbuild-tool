package com.ipt.ebsa.environment.build.util;

import com.ipt.ebsa.environment.hiera.UpdateBehaviour;
import com.ipt.ebsa.environment.v1.build.XMLUpdateBehaviour;

public class UpdateBehaviourMapper {

	public static UpdateBehaviour map(XMLUpdateBehaviour in) {
		switch (in) {
			case OVERWRITE_ALL:
				return UpdateBehaviour.OVERWRITE_ALL;
			case ADD_AND_UPDATE_ONLY:
				return UpdateBehaviour.ADD_AND_UPDATE_ONLY;
			default:
				throw new RuntimeException("Unkown UpdateBehaviour");
		}
	}
}
