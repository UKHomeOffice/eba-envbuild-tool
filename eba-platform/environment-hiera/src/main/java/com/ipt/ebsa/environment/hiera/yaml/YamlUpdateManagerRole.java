package com.ipt.ebsa.environment.hiera.yaml;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.hiera.route.RouteManager;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.template.TemplateManager;

/**
 * Generates the hiera for role yaml files for inside the target environment
 * @author James Shepherd
 *
 */
public class YamlUpdateManagerRole extends YamlUpdateManager {

	public static final String ROLE_GENERIC_TEMPLATE = "ROLE.yaml";

	/**
	 * 
	 * @param templateManager
	 * @param hieraFile
	 * @param scope only paths with these prefixes will be touched, null means no restriction
	 */
	public YamlUpdateManagerRole(TemplateManager templateManager, HieraMachineState hieraFile, Set<String> scope, VirtualMachine vm, RouteManager routeManager) {
		super(templateManager, hieraFile, scope, vm, routeManager);
	}

	@Override
	List<String> getTemplatePaths() {
		return Arrays.asList(
			ROLE_GENERIC_TEMPLATE,
			"ROLE-" + getHieraFile().getRoleOrFQDN().substring(0, 3) + ".yaml"
		);
	}
}
