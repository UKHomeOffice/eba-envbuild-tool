package com.ipt.ebsa.environment.build.execute.action;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ipt.ebsa.environment.build.hiera.EBInternalHieraManager;
import com.ipt.ebsa.environment.build.util.UpdateBehaviourMapper;
import com.ipt.ebsa.environment.data.model.InternalHieraAction;
import com.ipt.ebsa.environment.hiera.BeforeAfter;
import com.ipt.ebsa.environment.hiera.UpdateBehaviour;
import com.ipt.ebsa.ssh.ExecReturn;

public class InternalHieraActionPerformer extends ActionPerformer {

	private InternalHieraAction action;
	private EBInternalHieraManager manager;
	
	public InternalHieraActionPerformer(InternalHieraAction action) {
		this.action = action;
	}
	
	@Override
	protected void doPrepare() {
		manager = new EBInternalHieraManager(getBuildContext(), getActionContext().getHieraRepoUrl(),
			getActionContext().getRoutesRepoUrl(), getActionContext().getRoutesPath(),
			getActionContext().getScope(), getActionContext().getZones(), getActionContext().getUpdateBehaviour());
		manager.prepare();
	}
	
	@Override
	protected ExecReturn doExecute() {
		manager.execute();
		return new ExecReturn(0);
	}

	@Override
	public InternalHieraAction getAction() {
		return action;
	}

	@Override
	public void cleanUp() {
		if (null != manager) {
			manager.cleanUp();
		}
	}
	
	@Override
	public InternalHieraActionContext getActionContext() {
		return new InternalHieraActionContext();
	}
	
	public class InternalHieraActionContext extends ActionContext {

		public String getHieraRepoUrl() {
			return getBuildContext().substituteParams(action.getHieraRepoUrl());
		}
		
		public String getRoutesRepoUrl() {
			return getBuildContext().substituteParams(action.getRoutesRepoUrl());
		}
		
		public String getRoutesPath() {
			return getBuildContext().substituteParams(action.getRoutesPath());
		}
		
		public Set<String> getZones() {
			return action.getZones();
		}
		
		public Set<String> getScope() {
			return action.getScope();
		}
		
		public UpdateBehaviour getUpdateBehaviour() {
			return UpdateBehaviourMapper.map(action.getUpdateBehaviour());
		}
		
		@Override
		public Map<String, String> getActionContextMap() {
			TreeMap<String, String> map = new TreeMap<>();
			map.put("hierarepourl", getHieraRepoUrl());
			map.put("routesrepourl", getRoutesRepoUrl());
			map.put("routespath", getRoutesPath());
			map.put("zones", getZones().toString());
			map.put("scope", getScope().toString());
			map.put("updatebehaviour", getUpdateBehaviour().name());
			return map;
		}
		
		@Override
		public Set<BeforeAfter> getBeforeAfter() {
			return manager.getBeforeAfter();
		}
	}

	@Override
	public String getActionDisplayName() {
		return "Internal Hiera";
	}
}
