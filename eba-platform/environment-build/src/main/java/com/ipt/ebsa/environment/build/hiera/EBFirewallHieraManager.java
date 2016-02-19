package com.ipt.ebsa.environment.build.hiera;

import java.io.File;
import java.util.Set;

import com.ipt.ebsa.environment.build.Configuration;
import com.ipt.ebsa.environment.build.execute.BuildContext;
import com.ipt.ebsa.environment.hiera.BaseHieraManager;
import com.ipt.ebsa.environment.hiera.FirewallHieraManager;
import com.ipt.ebsa.environment.hiera.UpdateBehaviour;
import com.ipt.ebsa.environment.hiera.firewall.FirewallManager;
import com.ipt.ebsa.environment.hiera.firewall.FirewallUtil;

public class EBFirewallHieraManager extends EBBaseHieraManager {

	public EBFirewallHieraManager(BuildContext buildContext, String hieraRepoUrl, String sheetRepoUrl, String sheetPath,
			Set<String> scope, Set<String> zones, UpdateBehaviour updateBehaviour) {
		super(buildContext, hieraRepoUrl, sheetRepoUrl, sheetPath, scope, zones, updateBehaviour);
		setHieraCheckoutDir(new File(getBuildContext().getWorkDir(), "firewall_hiera"));
	}
	
	@Override
	protected BaseHieraManager buildManager() {
		FirewallManager firewallManager = new FirewallManager(new File(getSheetCheckoutDir(), getSheetPath()));
		FirewallHieraManager manager =  new FirewallHieraManager(getBuildContext().getEnvironment(), getBuildContext().getVersion(),
				getBuildContext().getProvider(), firewallManager, getHieraCheckoutDir(), getTemplateManager(), getBuildContext().getHieraFileManager(),
				getScope(), getZones(), getUpdateBehaviour());
		
		if (null != Configuration.getVyattaHostnameRegexp()) {
			FirewallUtil.setVyattaFirewallHostnamePattern(Configuration.getVyattaHostnameRegexp());
		}
		
		manager.setVyattaEnabled(Configuration.isVyattaForwardRuleGenerationEnabled());
		
		return manager;
	}
	
	@Override
	protected String getTemplateGitUrl() {
		return Configuration.getFirewallHieraTemplatesGitUrl();
	}
	
	@Override
	protected String getTemplateSubDir() {
		return Configuration.getFirewallHieraTempatesDir();
	}
	
	@Override
	protected File getSheetCheckoutDir() {
		return new File(getBuildContext().getWorkDir(), "firewall_sheet");
	}
	
	@Override
	protected File getTemplateCheckoutDir() {
		return new File(getBuildContext().getWorkDir(), "firewall_templates");
	}
	
	@Override
	protected boolean isCommitEnabled() {
		return Configuration.isCommitEnabledFirewallHiera();
	}
}
