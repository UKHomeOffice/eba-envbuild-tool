package com.ipt.ebsa.environment.build.hiera;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.ipt.ebsa.environment.build.execute.BuildContext;
import com.ipt.ebsa.environment.build.git.EBGitManager;
import com.ipt.ebsa.environment.hiera.BaseHieraManager;
import com.ipt.ebsa.environment.hiera.BeforeAfter;
import com.ipt.ebsa.environment.hiera.UpdateBehaviour;
import com.ipt.ebsa.git.GitManager;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.template.TemplateManager;

public abstract class EBBaseHieraManager {

	private String sheetPath;
	private String sheetRepoUrl;
	private String hieraRepoUrl;
	private BuildContext buildContext;
	private TemplateManager templateManager;
	private BaseHieraManager hieraManager;
	private File hieraCheckoutDir;
	private Set<String> scope;
	private Set<String> zones;
	private UpdateBehaviour updateBehaviour;

	public EBBaseHieraManager(BuildContext buildContext, String hieraRepoUrl, String sheetRepoUrl, String sheetPath,
			Set<String> scope, Set<String> zones, UpdateBehaviour updateBehaviour) {
		this.buildContext = buildContext;
		this.hieraRepoUrl = hieraRepoUrl;
		this.sheetRepoUrl = sheetRepoUrl;
		this.sheetPath = sheetPath;
		this.scope = scope;
		this.zones = zones;
		this.updateBehaviour = updateBehaviour;
	}
	
	public void prepare() {
		doCheckouts();
		buildTemplateManager();
		hieraManager = buildManager();
		hieraManager.prepare();
	}
	
	public void execute() {
		hieraManager.execute();
		if (isCommitEnabled()) {
			commitHiera();
		}
	}

	public List<HieraEnvironmentUpdate> getYamlUpdates() {
		return hieraManager.getYamlUpdates();
	}
	
	public void cleanUp() {
		getBuildContext().getGitMultiplexer().close(hieraRepoUrl);
	}
	
	protected abstract boolean isCommitEnabled();
	
	protected abstract String getTemplateGitUrl();
	
	protected abstract String getTemplateSubDir();
	
	protected abstract File getSheetCheckoutDir();
	

	protected abstract File getTemplateCheckoutDir();
	
	protected abstract BaseHieraManager buildManager();
	
	private void buildTemplateManager() {
		File templateDir = getTemplateCheckoutDir();
		
		if (StringUtils.isNotBlank(getTemplateSubDir())) {
			templateDir = new File(templateDir, getTemplateSubDir());
		}
		
		templateManager = new TemplateManager(templateDir);
	}

	private void doCheckouts(){
		checkout(sheetRepoUrl, getSheetCheckoutDir()).close();
		checkout(getTemplateGitUrl(), getTemplateCheckoutDir()).close();
		setHieraCheckoutDir(getBuildContext().getGitMultiplexer().checkout(hieraRepoUrl, GitManager.MASTER_STARTING_POINT, getHieraCheckoutDir()));
	}
	
	private void commitHiera() {
		getBuildContext().getGitMultiplexer().addCommitPush(hieraRepoUrl, String.format("Hiera data generation for environment [%s], version [%s]",
				getBuildContext().getEnvironment(), getBuildContext().getVersion()));
	}

	private GitManager checkout(String url, File dir) {
		if (dir.exists()) {
			try {
				FileUtils.deleteDirectory(dir);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to delete dir [%s]", dir.getAbsolutePath()));
			}
		}
		GitManager gm = EBGitManager.buildGitManager();
		gm.gitClone(url, dir, "master", false);
		return gm;
	}
	
	public String getSheetPath() {
		return sheetPath;
	}

	public BuildContext getBuildContext() {
		return buildContext;
	}

	public TemplateManager getTemplateManager() {
		return templateManager;
	}

	protected File getHieraCheckoutDir() {
		return hieraCheckoutDir;
	}
	
	protected void setHieraCheckoutDir(File hieraCheckoutDir) {
		this.hieraCheckoutDir = hieraCheckoutDir;
	}

	public Set<String> getScope() {
		return scope;
	}

	public Set<String> getZones() {
		return zones;
	}

	public UpdateBehaviour getUpdateBehaviour() {
		return updateBehaviour;
	}
	
	public Set<BeforeAfter> getBeforeAfter() {
		return hieraManager.getBeforeAfter();
	}
}