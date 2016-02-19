package com.ipt.ebsa.agnostic.client.aws.manager;

import java.util.HashMap;

import com.amazonaws.AmazonServiceException;

public class AwsExceptionManager {
	
	static HashMap<String, Boolean> error = new HashMap<String, Boolean>();
	
	static {
		error.put("InvalidAMIID.NotFound", true);
	}
	
	public static boolean isFatal (AmazonServiceException e) {
		
		try {
		    Boolean fatal = new Boolean(error.get(e.getErrorCode()));
		    return fatal.booleanValue();
		} catch(NullPointerException npe) {
			//ignore
		}
		return false;
	}

}
