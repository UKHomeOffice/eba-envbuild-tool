package com.ipt.ebsa.environment.build.execute.action;

import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.build.execute.BuildNode;
import com.ipt.ebsa.environment.build.prepare.PrepareContext;
import com.ipt.ebsa.environment.data.model.Action;
import com.ipt.ebsa.ssh.ExecReturn;

public abstract class ActionPerformer extends BuildNode {

	private static final Logger LOG = Logger.getLogger(ActionPerformer.class);

	abstract public String getActionDisplayName();
	
	/**
	 * Implement contra-variantly (i.e. change return type to the real type of the Action)
	 * @return
	 */
	abstract public Action getAction();
	
	/**
	 * Perform the Action. Subclasses should not override this, but override doExecute;
	 * @throws RuntimeException if BuildContext or Action are not set
	 */
	final public ExecReturn execute() {
		if (null == getAction()) {
			throw new RuntimeException("Action is null");
		}
		
		if (null == getBuildContext()) {
			throw new RuntimeException("BuildContext is null");
		}
		
		LOG.info("Starting to execute: " + getAction().getId());
		ExecReturn ret = doExecute();
		LOG.info("Finished executing: " + getAction().getId());
		return ret;
	}

	/**
	 * prepare the Action. Subclasses should not override this, but override doPrepare;
	 * @throws RuntimeException if BuildContext or Action are not set
	 */
	final public void prepare(PrepareContext context) {
		if (null == getAction()) {
			throw new RuntimeException("Action is null");
		}
		
		if (null == getBuildContext()) {
			throw new RuntimeException("BuildContext is null");
		}
		
		LOG.info("Starting to prepare: " + getAction().getId());
		doPrepare(context);
		LOG.info("Finished preparing: " + getAction().getId());
	}

	/**
	 * Optionally implement to do something during prepare phase
	 */
	protected void doPrepare(PrepareContext context) {
	}
	
	/**
	 * prepare the Action. Subclasses should not override this, but override doPrepare;
	 * @throws RuntimeException if BuildContext or Action are not set
	 */
	final public void prepare() {
		if (null == getAction()) {
			throw new RuntimeException("Action is null");
		}
		
		if (null == getBuildContext()) {
			throw new RuntimeException("BuildContext is null");
		}
		
		LOG.info("Starting to prepare: " + getAction().getId());
		doPrepare();
		LOG.info("Finished preparing: " + getAction().getId());
	}

	/**
	 * Optionally implement to do something during prepare phase
	 */
	protected void doPrepare() {
	}
	
	/**
	 * Subclasses should implement this to do their stuff.
	 * @return
	 */
	protected abstract ExecReturn doExecute();
	
	/**
	 * Called after {@link #doExecute()} or on error for
	 * ActionPerformer to clean up any resources. This implemention
	 * is no-op.
	 */
	public void cleanUp() {}
	
	/**
	 * Implement contra-variantly.
	 * @return the actual final parameters that the action will use, all placeholders resolved
	 */
	public abstract ActionContext getActionContext();

	
	public String getId() {
		return getAction().getId();
	}
}
