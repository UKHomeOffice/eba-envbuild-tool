package com.ipt.ebsa.manage.deploy;

import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentDataManager;
import com.ipt.ebsa.manage.deploy.impl.DependencyManager;
import com.ipt.ebsa.manage.deploy.impl.DifferenceManager;
import com.ipt.ebsa.manage.deploy.impl.HostnameResolver;
import com.ipt.ebsa.manage.deploy.impl.JitYumUpdateManager;
import com.ipt.ebsa.manage.deploy.impl.PlanManager;
import com.ipt.ebsa.manage.deploy.impl.TransitionManager;
import com.ipt.ebsa.manage.deploy.impl.YamlManager;

public interface DeploymentEngine {

	public void validate();
	public boolean prepare() throws Exception;
	public boolean execute() throws Exception;
	public boolean cleanup() throws Exception;
	
	public ComponentDeploymentDataManager getDeploymentDataManager();
	public HostnameResolver getHostnameResolver();
	public YamlManager getYamlManager();
	public DifferenceManager getDifferenceManager();
	public DependencyManager getDependencyManager();
	public PlanManager getPlanManager();
	public TransitionManager getTransitionManager();
	public JitYumUpdateManager getYumManager();
	
}
