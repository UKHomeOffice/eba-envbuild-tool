package com.ipt.ebsa.environment.build.hiera;

import java.io.File;
import java.util.Set;

import com.ipt.ebsa.environment.build.Configuration;
import com.ipt.ebsa.environment.build.execute.BuildContext;
import com.ipt.ebsa.environment.hiera.BaseHieraManager;
import com.ipt.ebsa.environment.hiera.InternalHieraManager;
import com.ipt.ebsa.environment.hiera.UpdateBehaviour;
import com.ipt.ebsa.environment.hiera.route.RouteManager;

public class EBInternalHieraManager extends EBBaseHieraManager {

	public EBInternalHieraManager(BuildContext buildContext, String hieraRepoUrl, String sheetRepoUrl, String sheetPath,
			Set<String> scope, Set<String> zones, UpdateBehaviour updateBehaviour) {
		super(buildContext, hieraRepoUrl, sheetRepoUrl, sheetPath, scope, zones, updateBehaviour);
		setHieraCheckoutDir(new File(getBuildContext().getWorkDir(), "internal_hiera"));
	}
	
	@Override
	protected BaseHieraManager buildManager() {
		RouteManager routeManager = new RouteManager(new File(getSheetCheckoutDir(), getSheetPath()), getScope());
		return new InternalHieraManager(getHieraCheckoutDir(), getBuildContext().getEnvironment(), getBuildContext().getVersion(),
			getBuildContext().getProvider(), getTemplateManager(), routeManager, getBuildContext().getHieraFileManager(),
			getScope(), getZones(), getUpdateBehaviour());
	}
	
	@Override
	protected String getTemplateGitUrl() {
		return Configuration.getInternalHieraTemplatesGitUrl();
	}
	
	@Override
	protected String getTemplateSubDir() {
		return Configuration.getInternalHieraTempatesDir();
	}
	
	@Override
	protected File getSheetCheckoutDir() {
		return new File(getBuildContext().getWorkDir(), "internal_sheet");
	}
	
	@Override
	protected File getTemplateCheckoutDir() {
		return new File(getBuildContext().getWorkDir(), "internal_templates");
	}
	
	@Override
	protected boolean isCommitEnabled() {
		return Configuration.isCommitEnabledInternalHiera();
	}
}
