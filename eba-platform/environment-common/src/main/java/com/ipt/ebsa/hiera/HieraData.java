package com.ipt.ebsa.hiera;

/**
 * Provides a means to load all of the HieraData and reference it thereafter.
 * It has a copy method which does a deep copy allowing the state to be preserved at any point in time.
 * @author scowx
 *
 */
public final class HieraData {
	public static final String HIERA_SYSTEM_PACKAGES = "system::packages";
	public static final String SEPARATOR = "/";
	
	/**
	 * Used as:
	 * - a marker within the engine to indicate a component is to be made present on the target
	 * - the String incorporated into update yaml files to action installing a component if it's not already present  
	 */
	public static final String ENSURE = "ensure";
	
	/**
	 * Used as:
	 * - a marker within the engine to indicate a component is to be made absent on the target
	 * - the String incorporated into update yaml files to action making a component absent
	 */
	public static final String ABSENT = "absent";
	
    /**
    * Returns the path you would compose if you were looking to update a "ensure" scalar value for a system package
    * e.g. "system::packages/[componentName]/ensure"
    * @param componentName
    * @return
    */
    public static String getEnsurePath(String componentName) {
	    return HieraData.HIERA_SYSTEM_PACKAGES + "/"+componentName+"/" + HieraData.ENSURE;
    }
}
