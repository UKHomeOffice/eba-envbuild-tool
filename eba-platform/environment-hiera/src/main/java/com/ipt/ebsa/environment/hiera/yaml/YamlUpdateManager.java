package com.ipt.ebsa.environment.hiera.yaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.template.TemplateManager;
import com.ipt.ebsa.util.OrgEnvUtil;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.hiera.route.RouteManager;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.hiera.NodeMissingBehaviour;
import com.ipt.ebsa.yaml.YamlInjector;
import com.ipt.ebsa.yaml.YamlUtil;

/**
 * Base helper for generating hiera for inside the target environment.
 * @author James Shepherd
 */
public abstract class YamlUpdateManager {

	private static final Logger LOG = Logger.getLogger(YamlUpdateManager.class);
	
	private TemplateManager templateManager;
	private HieraMachineState hieraFile;
	private YamlInjector yamlInjector = new YamlInjector();
	private Set<String> scope;
	VirtualMachine vm;
	RouteManager routeManager;

	/**
	 * 
	 * @param templateManager
	 * @param hieraFile
	 * @param scope list of (escaped paths) that we are allowed to create/update, or null for no filter
	 */
	YamlUpdateManager(TemplateManager templateManager, HieraMachineState hieraFile, Set<String> scope, VirtualMachine vm, RouteManager routeManager) {
		this.templateManager = templateManager;
		this.hieraFile = hieraFile;
		this.scope = scope;
		this.vm = vm;
		this.routeManager = routeManager;
	}
	
	public List<HieraEnvironmentUpdate> prepareYamlUpdates() {
		resetTemplateManager();
		List<HieraEnvironmentUpdate> updates = new ArrayList<>();
		
		for(String templatePath : getTemplatePaths()) {
			updates.addAll(updateFromTemplate(templatePath));
		}
		
		return updates;
	}

	private List<HieraEnvironmentUpdate> updateFromTemplate(String templatePath) {
		if (getTemplateManager().templateExists(templatePath)) {
			try {
				String yaml = getTemplateManager().render(templatePath);
				yaml = YamlUtil.filterByPaths(yaml, scope);
				
				if (StringUtils.isNotBlank(yaml)) {
					LOG.debug(String.format("yaml:[\n%s\n]", yaml));
					return yamlInjector.updateYamlWithBlock(getHieraFile(), "", yaml, NodeMissingBehaviour.INSERT_ALL, "ButtonMoon", getHieraFile().getEnvironmentName());
				} else {
					LOG.debug(String.format("Empty template for [%s]", templatePath));
					return Collections.emptyList();
				}
			} catch (Exception e) {
				throw new RuntimeException(String.format("Failed to update yaml file [%s] from template [%s]",
						getHieraFile().getFile().getAbsolutePath(),
						templatePath), e);
			}
		}
		
		return Collections.emptyList();
	}
	
	private void resetTemplateManager() {
		getTemplateManager().resetContext();
		setupContext();
	}

	protected void setupContext() {
		getTemplateManager().put("role", getHieraFile().getRoleOrFQDN().substring(0, 3));
		getTemplateManager().put("vm", vm);
		getTemplateManager().put("zone", OrgEnvUtil.getDomainForPuppet(vm.getVirtualmachinecontainer().getName()));
		getTemplateManager().put("org", OrgEnvUtil.getOrganisationName(vm.getVirtualmachinecontainer().getName()));
		getTemplateManager().put("routes", routeManager.getRoutes(vm));
	}

	abstract List<String> getTemplatePaths();

	protected TemplateManager getTemplateManager() {
		return templateManager;
	}

	protected HieraMachineState getHieraFile() {
		return hieraFile;
	}
}
