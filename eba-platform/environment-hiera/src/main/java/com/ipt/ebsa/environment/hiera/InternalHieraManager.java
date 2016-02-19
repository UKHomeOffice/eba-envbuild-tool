package com.ipt.ebsa.environment.hiera;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.hiera.route.RouteManager;
import com.ipt.ebsa.environment.hiera.yaml.YamlUpdateManager;
import com.ipt.ebsa.environment.hiera.yaml.YamlUpdateManagerHost;
import com.ipt.ebsa.environment.hiera.yaml.YamlUpdateManagerRole;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.template.TemplateManager;
import com.ipt.ebsa.util.FileUtil;
import com.ipt.ebsa.util.OrgEnvUtil;

/**
 * Overall manager for hiera inside the target environment
 * @author James Shepherd
 */
public class InternalHieraManager extends BaseHieraManager {
	private static Logger LOG = Logger.getLogger(InternalHieraManager.class);
	
	private RouteManager routeManager;
	private TreeSet<HieraMachineState> hieraFiles = new TreeSet<>();
	
	public InternalHieraManager(File hieraDirRoot, String environment, String environmentVersion, String environmentProvider, TemplateManager templateManager,
			RouteManager routeManager, HieraFileManager hieraFileManager, Set<String> scope, Set<String> zones, UpdateBehaviour updateBehaviour) {
		super(environment, environmentVersion, environmentProvider, hieraDirRoot, templateManager, hieraFileManager, scope, zones, updateBehaviour);
		this.routeManager = routeManager;
	}

	public void prepare() {
		reset();
		FileUtil.checkDirExistsOrCreate(hieraDirRoot);
		fetchTargetEnvironmentVMsAndZones();
		prepareYamlChanges();
		addYamlDeletes();
	}

	protected void reset() {
		super.reset();
		targetEnvironmentVMs = new TreeMap<>();
	}

	private void prepareYamlChanges() {
		LOG.info(String.format("Finding yaml files in [%s]", hieraDirRoot.getAbsolutePath()));
		for (VirtualMachine vm : targetEnvironmentVMs.values()) {
			String host = getFqdn(vm);
			String role = vm.getComputerName().substring(0, 3);
			String zone = OrgEnvUtil.getEnvironmentName(vm.getVirtualmachinecontainer().getName());
			String org = OrgEnvUtil.getOrganisationName(vm.getVirtualmachinecontainer().getName());
			LOG.debug(String.format("org [%s], zone [%s]", org, zone));
			File zoneHieraDir = new File(new File(hieraDirRoot, org), zone);
			String hostYamlBasename = host + ".yaml";
			String roleYamlBasename = role + ".yaml";
			LOG.debug(String.format("Looking for vm [%s], host yaml [%s], role yaml [%s], in [%s]", host, hostYamlBasename, roleYamlBasename, zoneHieraDir.getAbsolutePath()));
			HieraMachineState yamlFileRole = findYamlFile(zoneHieraDir, role, roleYamlBasename);
			HieraMachineState yamlFileHost = findYamlFile(zoneHieraDir, host, hostYamlBasename);
			hieraFiles.add(yamlFileRole);
			hieraFiles.add(yamlFileHost);
			makeUpdates(yamlFileRole, vm);
			makeUpdates(yamlFileHost, vm);
		}
	}

	protected void makeUpdates(HieraMachineState hiera, VirtualMachine vm) {
		YamlUpdateManager yum;
		boolean isRole = hiera.getRoleOrFQDN().length() == 3;
		if (isRole) {
			yum = new YamlUpdateManagerRole(templateManager, hiera, scope, vm, routeManager);
		} else {
			yum = new YamlUpdateManagerHost(templateManager, hiera, scope, vm, routeManager);
		}
		
		List<HieraEnvironmentUpdate> yamlUpdates0 = yum.prepareYamlUpdates();
		LOG.info(String.format("There are [%s] yaml updates for [%s]", hieraUpdates.size(), hiera.getRoleOrFQDN()));
		
		for (HieraEnvironmentUpdate yu : yamlUpdates0) {
			if (yu.changeMade()) {
				LOG.debug(String.format("Change in hiera [%s]", yu.getSource().getRoleOrFQDN()));
				hieraUpdates.add(yu);
			} else {
				LOG.debug(String.format("No change in hiera [%s]", yu.getSource().getRoleOrFQDN()));
			}
		}
	}

	public List<HieraEnvironmentUpdate> getAllYamlUpdates() {
		return hieraUpdates;
	}
}
