package com.ipt.ebsa.environment.build.execute.action;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ipt.ebsa.environment.build.hiera.EBFirewallHieraManager;
import com.ipt.ebsa.environment.build.util.UpdateBehaviourMapper;
import com.ipt.ebsa.environment.data.model.FirewallHieraAction;
import com.ipt.ebsa.environment.hiera.BeforeAfter;
import com.ipt.ebsa.environment.hiera.UpdateBehaviour;
import com.ipt.ebsa.ssh.ExecReturn;

public class FirewallHieraActionPerformer extends ActionPerformer {

	private FirewallHieraAction action;
	private EBFirewallHieraManager manager;
	
	public FirewallHieraActionPerformer(FirewallHieraAction action) {
		this.action = action;
	}
	
	@Override
	public FirewallHieraAction getAction() {
		return action;
	}
	
	@Override
	protected void doPrepare() {
		manager = new EBFirewallHieraManager(getBuildContext(), getActionContext().getHieraRepoUrl(),
				getActionContext().getFirewallRepoUrl(), getActionContext().getFirewallPath(),
				getActionContext().getScope(), getActionContext().getZones(),
				getActionContext().getUpdateBehaviour());
		manager.prepare();
	}
	
	@Override
	protected ExecReturn doExecute() {
		manager.execute();
		return new ExecReturn(0);
	}

	@Override
	public void cleanUp() {
		if (null != manager) {
			manager.cleanUp();
		}
	}
	
	@Override
	public FirewallHieraActionContext getActionContext() {
		return new FirewallHieraActionContext();
	}
	
	public class FirewallHieraActionContext extends ActionContext {

		public String getHieraRepoUrl() {
			return getBuildContext().substituteParams(action.getHieraRepoUrl());
		}
		
		public String getFirewallRepoUrl() {
			return getBuildContext().substituteParams(action.getFirewallRepoUrl());
		}
		
		public String getFirewallPath() {
			return getBuildContext().substituteParams(action.getFirewallPath());
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
			map.put("firewallrepourl", getFirewallRepoUrl());
			map.put("firewallpath", getFirewallPath());
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
