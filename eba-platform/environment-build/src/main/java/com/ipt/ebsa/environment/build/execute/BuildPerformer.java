package com.ipt.ebsa.environment.build.execute;

import java.util.List;

import com.ipt.ebsa.environment.build.execute.action.ActionPerformer;

public class BuildPerformer {

	private List<ActionPerformer> actionPerformers;
	
	private BuildNode build;
	
	public BuildPerformer(BuildNode build, List<ActionPerformer> actionPerformers) {
		this.actionPerformers = actionPerformers;
		this.build = build;
	}

	public List<ActionPerformer> getActionPerformers() {
		return actionPerformers;
	}

	public BuildNode getBuild() {
		return build;
	}
}
