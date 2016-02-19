package com.ipt.ebsa.manage.transitions;

import java.util.ArrayList;
import java.util.List;

import com.ipt.ebsa.environment.EnvironmentUpdate;
import com.ipt.ebsa.manage.deploy.DeploymentStatus;

/**
 * Contains the data for start and end state
 * @author scowx
 *
 */
public class Transition {

	private int						sequenceNumber;
	private List<EnvironmentUpdate>	updates		= new ArrayList<>();
	private List<MCOCommand>		commands	= new ArrayList<>();
	private DeploymentStatus		status		= DeploymentStatus.NOT_STARTED;
	private Exception 				exception	= null;
	private String					statusMessage = null;
	private String					transitionLog = null;

	// These define a wait or stop for this transition
	private int						waitSeconds	= -1;
	private boolean					stopAfter	= false;
	private String					stopMessage	= null;

	public boolean isEmpty() {
		return updates.size() < 1;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public List<EnvironmentUpdate> getUpdates() {
		return updates;
	}

	public List<MCOCommand> getCommands() {
		return commands;
	}

	public DeploymentStatus getStatus() {
		return status;
	}

	public void setStatus(DeploymentStatus status) {
		this.status = status;
	}
	
	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	
	public String getLog() {
		return transitionLog;
	}

	public void setLog(String log) {
		this.transitionLog = log;
	}

	public int getWaitSeconds() {
		return waitSeconds;
	}

	public void setWaitSeconds(int waitSeconds) {
		this.waitSeconds = waitSeconds;
	}

	public boolean isStopAfter() {
		return stopAfter;
	}

	public void setStopAfter(boolean stopAfter) {
		this.stopAfter = stopAfter;
	}

	public String getStopMessage() {
		return stopMessage;
	}

	public void setStopMessage(String stopMessage) {
		this.stopMessage = stopMessage;
	}
	
}
