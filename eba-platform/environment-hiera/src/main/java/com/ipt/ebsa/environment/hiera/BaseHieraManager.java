package com.ipt.ebsa.environment.hiera;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition.DefinitionType;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.template.TemplateManager;
import com.ipt.ebsa.util.OrgEnvUtil;
import com.ipt.ebsa.yaml.YamlInjector;
import com.ipt.ebsa.yaml.YamlUtil;

public abstract class BaseHieraManager {
	private static final Logger LOG = Logger.getLogger(BaseHieraManager.class);
	
	protected List<HieraEnvironmentUpdate> hieraUpdates;
	protected File hieraDirRoot;
	protected String environment;
	protected String environmentVersion;
	protected String environmentProvider;
	protected TemplateManager templateManager;
	protected HieraFileManager hieraFileManager;
	protected Set<String> scope;
	protected UpdateBehaviour updateBehaviour;
	protected Set<String> zones;
	protected Map<String, VirtualMachine> targetEnvironmentVMs = new TreeMap<>();
	
	private Set<BeforeAfter> beforeAfters;
	private YamlInjector yamlInjector = new YamlInjector();


	/**
	 * 
	 * @param environment
	 * @param environmentVersion
	 * @param environmentProvider
	 * @param hieraDirRoot
	 * @param templateManager
	 * @param hieraFileManager
	 * @param scope only paths with these prefixes will be touched, null means no restriction
	 * @param zones 
	 * @param updateBehaviour
	 */
	protected BaseHieraManager(String environment, String environmentVersion, String environmentProvider, File hieraDirRoot, TemplateManager templateManager,
			HieraFileManager hieraFileManager, Set<String> scope, Set<String> zones, UpdateBehaviour updateBehaviour) {
		this.environment = environment;
		this.environmentVersion = environmentVersion;
		this.environmentProvider = environmentProvider;
		this.hieraDirRoot = hieraDirRoot;
		this.templateManager = templateManager;
		this.hieraFileManager = hieraFileManager;
		this.scope = scope;
		this.zones = zones;
		this.updateBehaviour = updateBehaviour;
		
		if (UpdateBehaviour.OVERWRITE_ALL == updateBehaviour) {
			// then you must have supplied scope (paths that are being touched)
			// and zones (a limit on what is being touched) and no related zones
			if (null == zones || zones.isEmpty()) {
				throw new RuntimeException("Cannot have update behaviour of 'overwrite all' without specifying zones");
			}
			
			if (null == scope || scope.isEmpty()) {
				throw new RuntimeException("Cannot have update behaviour of 'overwrite all' without specifying scope (yaml paths)");
			}
		}
	}
	
	public abstract void prepare();
	
	protected String filterYamlByPath(String yaml) {
		return YamlUtil.filterByPaths(yaml, scope);
	}
	
	protected void addYamlDeletes() {
		if (UpdateBehaviour.OVERWRITE_ALL != updateBehaviour) {
			return;
		}
		
		Map<String, Set<String>> createdUpdatedPaths = consolidateUpdates();
		
		// have already done checks, we have scope and zone and no related zone
		// now need to find the hiera dir for each zone
		
		for (String zone : zones) {
			File zoneHieraDir = hieraDirForZone(zone);
			String[] list = allYamlFilesInDir(zoneHieraDir);
			
			if (null != list) {
				for (String basename : list) {
					LOG.debug(String.format("adding deletes for [%s]", basename));
					String hostOrRole = basename.substring(0, basename.indexOf(".") - 1);
					HieraMachineState hieraFile = findYamlFile(zoneHieraDir, hostOrRole, basename);
					Map<String, Object> flattened = YamlUtil.flattenYaml(YamlUtil.escapeKeys(hieraFile.getState()));
					LOG.debug(String.format("Flattened [%s]", flattened));
					Map<String, Object> filtered = YamlUtil.filterByPaths(scope, flattened);
					
					// So now we have all the yaml that is in the file. If any is not
					// in our yamlupdates then it needs deleting
					TreeSet<String> removePaths = new TreeSet<>(); 
					removePaths.addAll(filtered.keySet());
					Set<String> dontDelete = createdUpdatedPaths.get(basename);
					
					if (null != dontDelete) {
						LOG.debug(String.format("Don't delete [%s]", dontDelete));
						LOG.debug(removePaths);
						removePaths.removeAll(dontDelete);
						LOG.debug(String.format("Yaml file [%s], deleting [%s]", hieraFile.getFile(), removePaths));
					}
					
					for (String toRemove : removePaths) {
						HieraEnvironmentUpdate update = yamlInjector.remove(toRemove, hieraFile.getState(), "TheClangers", hieraFile.getEnvironmentName());
						update.setSource(hieraFile);
						hieraUpdates.add(update);
					}
				}
			}
		}
	}

	private String[] allYamlFilesInDir(File zoneHieraDir) {
		// now need to get all files in that dir as HieraFiles
		String[] list = zoneHieraDir.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".yaml");
			}
		});
		return list;
	}

	private File hieraDirForZone(String zone) {
		String domain = OrgEnvUtil.getEnvironmentName(zone);
		String org = OrgEnvUtil.getOrganisationName(zone);
		File zoneHieraDir = new File(new File(hieraDirRoot, org), domain);
		return zoneHieraDir;
	}

	private Map<String, Set<String>> consolidateUpdates() {
		// need a list of all paths that we are creating/updating
		// we use the fact that we flatten the paths before updating
		Map<String, Set<String>> createdUpdatedPaths = new TreeMap<>();
		for (HieraEnvironmentUpdate update : hieraUpdates) {
			String key = update.getSource().getSourceName();
			Set<String> updatesForFile = createdUpdatedPaths.get(key);
			
			if (null == updatesForFile) {
				updatesForFile = new TreeSet<>();
				createdUpdatedPaths.put(key, updatesForFile);
			}
			
			updatesForFile.add(update.getRequestedPath());
		}
		return createdUpdatedPaths;
	}
	
	public void execute() {
		writeYamlChangesToDisk();
	}
	
	public List<HieraEnvironmentUpdate> getYamlUpdates() {
		return hieraUpdates;
	}
	
	public Set<BeforeAfter> getBeforeAfter() {
		if (null == beforeAfters) {
			beforeAfters = new TreeSet<>();
			Map<String, Map<String, BeforeAfter>> beforeAfterZoneMap = new TreeMap<>();
			Map<String, Map<String, Map<String, Object>>> yamlZoneMap = new TreeMap<>();
			
			for (HieraEnvironmentUpdate update : hieraUpdates) {
				String basename = update.getSource().getSourceName();
				File hieraFile = ((HieraMachineState) update.getSource()).getFile();
				File hieraDir = hieraFile.getParentFile();
				String domain = hieraDir.getName();
				Map<String, BeforeAfter> beforeAfterMap = beforeAfterZoneMap.get(domain);
				Map<String, Map<String, Object>> yamlMap = yamlZoneMap.get(domain);
				
				LOG.debug(String.format("Generating BeforeAfter for domain [%s] file [%s]", domain, basename));
				
				if (null == beforeAfterMap) {
					beforeAfterMap = new TreeMap<>();
					yamlMap = new TreeMap<>();
					beforeAfterZoneMap.put(domain, beforeAfterMap);
					yamlZoneMap.put(domain, yamlMap);
				}
				
				BeforeAfter beforeAfter = beforeAfterMap.get(basename);
				Map<String, Object> afterYaml = yamlMap.get(basename); 
						
				if (null == beforeAfter) {
					beforeAfter = new BeforeAfter();
					beforeAfter.setBasename(basename);
					beforeAfter.setDomain(domain);
					beforeAfterMap.put(basename, beforeAfter);

					if (hieraFile.exists()) {
						try {
							beforeAfter.setBefore(FileUtils.readFileToString(hieraFile));
							afterYaml = YamlUtil.readYaml(hieraFile);
						} catch (IOException e) {
							throw new RuntimeException(String.format("Failed to read yaml file [%s]", hieraFile.getAbsolutePath()));
						}
					}
					
					yamlMap.put(basename, afterYaml);
				}
				
				if (null == afterYaml) {
					afterYaml = new TreeMap<>();
					yamlMap.put(basename, afterYaml);
				}
					
				try {
					yamlInjector.apply(afterYaml, update);
				} catch (Exception e) {
					throw new RuntimeException(String.format("Yaml apply failed for [%s]", update));
				}
			}

			for (Entry<String, Map<String, BeforeAfter>> entry : beforeAfterZoneMap.entrySet()) {
				for (Entry<String, BeforeAfter> me : entry.getValue().entrySet()) {
					Map<String, Object> afterYaml = yamlZoneMap.get(entry.getKey()).get(me.getKey());
					StringWriter writer = new StringWriter();
					YamlUtil.write(afterYaml, writer);
					BeforeAfter ba = me.getValue();
					ba.setAfter(writer.toString());
					beforeAfters.add(ba);
					LOG.debug(String.format("Added [%s]", ba));
				}
			}
		}
		
		// remove where there is no change
		for (Iterator<BeforeAfter> it = beforeAfters.iterator(); it.hasNext(); ) {
			BeforeAfter ba = it.next();
			if (ba.getBefore().replace("\r", "").equals(ba.getAfter().replace("\r", ""))) {
				it.remove();
			}
		}
		
		return beforeAfters;
	}
	
	protected void reset() {
		hieraUpdates = new ArrayList<>();
	}
	
	protected HieraMachineState findYamlFile(File zoneHieraDir, String hostOrRole, String basename) {
		return hieraFileManager.getHieraFile(new File(zoneHieraDir, basename), environment, hostOrRole);
	}
	
	private void writeYamlChangesToDisk() {
		String commitMessage = String.format("environment [%s], version [%s]", environment, environmentVersion);
		for (HieraEnvironmentUpdate yamlUpdate : hieraUpdates) {
			try {
				yamlUpdate.doUpdate(environment, commitMessage);
			} catch (Exception e) {
				throw new RuntimeException(String.format("Failed to update hiera file [%s]",
						((HieraMachineState) yamlUpdate.getSource()).getFile().getAbsolutePath()), e);
			}
		}
	}

	protected void fetchTargetEnvironmentVMsAndZones() {
		ReadManager rm = new ReadManager();
		TreeSet<String> foundZones = new TreeSet<>();
		try {
			EnvironmentDefinition envDef = rm.getEnvironmentDefinition(environment, environmentVersion, DefinitionType.Physical, environmentProvider);
			for (VirtualMachineContainer zone : envDef.getVirtualmachinecontainers()) {
				if (null == zones || zones.isEmpty() || zones.contains(zone.getName())) {
					foundZones.add(zone.getName());
					
					for (VirtualMachine vm : zone.getVirtualmachines()) {
						String fqdn = getFqdn(vm);
						LOG.debug(String.format("Found VM [%s]", fqdn));
						targetEnvironmentVMs.put(fqdn, vm);
					}
				}
			}
			
			if (zones == null || zones.isEmpty()) {
				// take all zones if we haven't been given any
				zones = foundZones;
			}
			LOG.debug(String.format("Zones: [%s]", zones));
		} catch (Exception e) {
			throw new RuntimeException(String.format("Failed to get environment definition from database for environment [%s] version [%s]", environment, environmentVersion), e);
		}
	}

	protected String getFqdn(VirtualMachine vm) {
		return vm.getComputerName() + "." + OrgEnvUtil.getDomainForPuppet(vm.getVirtualmachinecontainer().getName());
	}
}
