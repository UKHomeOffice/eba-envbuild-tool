package com.ipt.ebsa.manage.deploy.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a class with a bit of a split personality.  It reflects the fact that a deployment step for a component
 * may well be composed of a number of sub steps if that is what the component  demands.  E.g. an upgrade might require a
 * an UNDEPLOY and then a REDEPLOY.  In most cases however the UPGRADE is sufficient to be done in one change.
 *   
 * @author scowx
 *
 */
public class ChangeSet {

	private Change primaryChange;
	private List<Change> subTasks = new ArrayList<Change>();
	
	@Override
	public String toString() {
		String s = "";
		if (isComplexChange()) {
			s+= " {";
			for (Change ac : subTasks) {
				s += (":"+ac);
			}
			s += "}";
		}
		return primaryChange +" [" + s + "]";
	}

	public Change getPrimaryChange() {
		return primaryChange;
	}

	public void setPrimaryChange(Change primaryChange) {
		this.primaryChange = primaryChange;
	}

	/**
	 * Returns true if this Deployment action is composed of more than one sub task.
	 * @return
	 */
	public boolean isComplexChange() {
		return subTasks.size() > 0;
	}
	
	public List<Change> getSubTasks() {
		return subTasks;
	}
	
	
}
