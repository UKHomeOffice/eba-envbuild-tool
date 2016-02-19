package com.ipt.ebsa.environment.build.execute.action;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ipt.ebsa.environment.hiera.BeforeAfter;

/**
 * @author James Shepherd
 */
public abstract class ActionContext {
	
	/**
	 * @return Map of resolved parameters for this action
	 */
	abstract public Map<String, String> getActionContextMap();
	
	/**
	 * Additional info for displaying to the user.
	 * This implementation returns null, which means not additional data.
	 * @see #getGuiTableBody()
	 * @return GUI table header
	 */
	public List<String> getGuiTableHead() {
		return null;
	}
	
	/**
	 * Additional info for displaying to the user
	 * This implementation returns null, which means not additional data.
	 * @see #getGuiTableHead()
	 * @return GUI table body
	 */
	public List<List<String>> getGuiTableBody() {
		return null;
	}
	
	/**
	 * This implementation returns null.
	 * @return For showing a diff in the GUI
	 */
	public Set<BeforeAfter> getBeforeAfter() {
		return null;
	}
}
