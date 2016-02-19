package com.ipt.ebsa.manage.deploy.impl;

import java.util.ArrayList;
import java.util.List;

import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.StepItem;
import com.ipt.ebsa.environment.StateSearchResult;

/**
 * Holds the details for a single change which needs to happen
 * 
 * @author scowx
 * 
 */
public class Change {

	private ChangeType				changeType;
	private Boolean					prepared	= Boolean.FALSE;
	private Integer					transitionId;
	private String					reasonForFailure;
	private String					warning;
	private StateSearchResult		searchResult;
	private List<List<StepItem>>	before		= new ArrayList<>();
	private List<List<StepItem>>	after		= new ArrayList<>();

	public Change(ChangeType changeType) {
		super();
		this.changeType = changeType;
	}

	@Override
	public String toString() {
		return "Change@" + this.hashCode() + " [changeType=" + changeType + ", prepared=" + prepared + ", transitionId=" + transitionId + ", searchResult=" + searchResult + "]";
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public void setChangeType(ChangeType changeType) {
		this.changeType = changeType;
	}

	public boolean isPrepared() {
		return prepared;
	}

	public void setPrepared(boolean prepared) {
		this.prepared = prepared;
	}

	public Integer getTransitionId() {
		return transitionId;
	}

	public void setTransitionId(Integer transitionId) {
		this.transitionId = transitionId;
	}

	public String getReasonForFailure() {
		return reasonForFailure;
	}

	public void setReasonForFailure(String reasonForFailure) {
		this.reasonForFailure = reasonForFailure;
	}

	public String getWarning() {
		return warning;
	}

	public void setWarning(String warning) {
		this.warning = warning;
	}

	public StateSearchResult getSearchResult() {
		return searchResult;
	}

	public void setSearchResult(StateSearchResult searchResult) {
		this.searchResult = searchResult;
	}

	public List<List<StepItem>> getBefore() {
		return before;
	}

	public void setBefore(List<List<StepItem>> before) {
		this.before = before;
	}

	public List<List<StepItem>> getAfter() {
		return after;
	}

	public void setAfter(List<List<StepItem>> after) {
		this.after = after;
	}

	
}
