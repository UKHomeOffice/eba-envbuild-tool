package com.ipt.ebsa.environment.hiera;

/**
 * Models the options of recreating all hiera data or just updating
 * @author James Shepherd
 */
public enum UpdateBehaviour {
	/**
	 * destroy and recreate all
	 */
	OVERWRITE_ALL,
	
	/**
	 * only add or update, no deleting
	 */
	ADD_AND_UPDATE_ONLY;
}
