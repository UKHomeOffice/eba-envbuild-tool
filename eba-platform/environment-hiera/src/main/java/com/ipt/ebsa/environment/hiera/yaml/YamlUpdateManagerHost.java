package com.ipt.ebsa.environment.hiera.yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.hiera.route.RouteManager;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.template.TemplateManager;

/**
 * Generates the hiera for host yaml files for inside the target environment
 * @author James Shepherd
 */
public class YamlUpdateManagerHost extends YamlUpdateManager {

	public static final String HOST_GENERIC_TEMPLATE = "HOST.yaml";
	
	public YamlUpdateManagerHost(TemplateManager templateManager, HieraMachineState hieraFile, Set<String> scope, VirtualMachine vm, RouteManager routeManager) {
		super(templateManager, hieraFile, scope, vm, routeManager);
	}

	@Override
	List<String> getTemplatePaths() {
		ArrayList<String> output = new ArrayList<>(Arrays.asList(
			HOST_GENERIC_TEMPLATE
		));
		
		for (int i = 0; i <= vm.getComputerName().length(); i++) {
			output.add("HOST-" + vm.getComputerName().substring(0, i) + ".yaml");
		}
		
		return output;
	}
}
	
