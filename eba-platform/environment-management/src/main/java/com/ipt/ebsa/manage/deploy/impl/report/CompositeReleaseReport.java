package com.ipt.ebsa.manage.deploy.impl.report;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.environment.EnvironmentUpdate;
import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeployment;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleasePhase;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeploymentEngine;
import com.ipt.ebsa.manage.deploy.impl.Change;
import com.ipt.ebsa.manage.deploy.impl.ChangeSet;
import com.ipt.ebsa.manage.deploy.impl.ChangeType;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.transitions.MCOCommand;
import com.ipt.ebsa.manage.transitions.Transition;

/**
 * This provides a report of the actions which will be taken by the {@link CompositeReleaseDeploymentEngine}.
 * 
 * @author David Manning
 */
public class CompositeReleaseReport extends Report {
	
	private Logger log = LogManager.getLogger(CompositeReleaseReport.class);
	private boolean autoExpand = false;
	private boolean withStatus = false;
	
	/**
	 * 
	 * @param autoExpand Should we automatically expand the currently running phase?
	 * @param withStatus Should we output the transition status to the report?
	 */
	public CompositeReleaseReport(boolean autoExpand, boolean withStatus) {
		super();
		this.autoExpand = autoExpand;
		this.withStatus = withStatus;
	}
	
	@Override
	public void generateReport(Deployment deployment) throws Exception {
		if (deployment instanceof CompositeReleaseDeployment) {
			dumpTableToFile((CompositeReleaseDeployment)deployment);
		} else {
			throw new RuntimeException("Unable to generate report for deployment of type: " + deployment.getClass().getName());
		}
	}
	
	/**
	 * Dump out the deployment table
	 */
	private void dumpTableToFile(CompositeReleaseDeployment deployment) throws Exception {
		log.debug("Writing out deployment report");

		JsonObjectBuilder root = Json.createObjectBuilder();
		dumpDeploymentData(deployment, root);
		dumpDeploymentActions(deployment, root);

		dumpDependencyChains(deployment.getComponents(), deployment.getDependencyChains(), root);		
		
		dumpTransitions(deployment, root);
		
		dumpDeploymentDescriptors(deployment, root);
				
		if (deployment.getFailFilesByFQDN() != null) {
			dumpFailFiles(deployment, root);
		}
		
		File output = determineFile(deployment.getId());
		writeToFile(output, root);
		log.debug("Finished writing out deployment report to '"+output.getAbsolutePath()+"'");
	}
	
	private void dumpDeploymentData(CompositeReleaseDeployment deployment, JsonObjectBuilder root) {
		if (this.withStatus) {
			JsonObjectBuilder dd = Json.createObjectBuilder();
			dd.add("environment", deployment.getEnvironmentName());
			if (deployment.getReleaseVersion() != null) {
				String release = String.format("%s : %s", deployment.getReleaseVersion().getName(), deployment.getReleaseVersion().getVersion());
				dd.add("releaseVersion", release);
			}
			if (deployment.getStatus() != null) {
				dd.add("status", deployment.getStatus().toString());
			}
			if (deployment.getDeploymentRecord() != null) {
				if (deployment.getDeploymentRecord().getStartTime() != null) {
					dd.add("started", deployment.getDeploymentRecord().getStartTime().toString());
				}
				if (deployment.getDeploymentRecord().getEndTime() != null) {
					dd.add("finished", deployment.getDeploymentRecord().getEndTime().toString());
				}
			}
			root.add("details", dd);
		}
	}
	
	private void dumpFailFiles(Deployment deployment, JsonObjectBuilder root) {
		/**
		 * 	RPM Fail Files	
		 * 
		 *  ------------------------------------------------------------
		 *  |            Host                 |         Status         |
		 *  ------------------------------------------------------------
		 *  | doctzm01.st-sit1-cor1.ipt.local | Failed RPM File exists |
		 *  | etctzm01.st-sit1-cor1.ipt.local | None found             |
		 *  | ...                             | ...                    |
		 *  ------------------------------------------------------------		 
		 *  
		 */
		Map<String, String> failFilesByFQDN = deployment.getFailFilesByFQDN();
		if (!failFilesByFQDN.isEmpty()) {
			JsonArrayBuilder ffs = Json.createArrayBuilder();
			for (Entry<String, String> failFiles : failFilesByFQDN.entrySet()) {
				JsonObjectBuilder ff = Json.createObjectBuilder();
				ff.add("host", failFiles.getKey());
				boolean fileFound = !failFiles.getValue().isEmpty();
				ff.add("exists", fileFound);
				ff.add("status", fileFound ? failFiles.getValue() : "None found");
				ffs.add(ff);
			}
			root.add("failFiles", ffs);
		} 
	}

	private void dumpDeploymentActions(Deployment deployment, JsonObjectBuilder root) {
		Map<ComponentId, ComponentDeploymentData> components = deployment.getComponents();
		boolean showDeployedVersion = (deployment.getAlternateEnvironmentStateManager() != null);

		Map<String, DeploymentOutput> deployments = new HashMap<>();
		
		for (Entry<ComponentId, ComponentDeploymentData> entry : components.entrySet()) {
			if (!deployments.containsKey(entry.getKey().getApplicationShortName())) {
				String version = deployment.getApplicationVersion(entry.getKey().getApplicationShortName()) == null ? "unknown" : deployment.getApplicationVersion(entry.getKey().getApplicationShortName()).getVersion();
				deployments.put(entry.getKey().getApplicationShortName(), new DeploymentOutput(entry.getKey().getApplicationShortName(), version));
			}
			DeploymentOutput deploymentOutput = deployments.get(entry.getKey().getApplicationShortName());
			
			ComponentDeploymentData v = entry.getValue();
			ComponentVersion targetCmponentVersion = v.getTargetCmponentVersion();
			
			String version = targetCmponentVersion == null ? null : targetCmponentVersion.getComponentVersion() + "   [ "+targetCmponentVersion.getRpmPackageVersion()+" ]";
			List<StateSearchResult> searchResults = v.getOriginalExistingComponentState();
			
			String currentVersions = "";
			if (searchResults != null) {
				for (StateSearchResult searchResult : searchResults) {
					String versionInYaml = searchResult.getComponentVersion();
					currentVersions += (currentVersions.equals("") ? versionInYaml : ", " + versionInYaml);
				}
			}
			
			JsonObjectBuilder jc = Json.createObjectBuilder();
			jc.add("name", v.getComponentId().getComponentName());
			jc.add("existing", currentVersions);
			jc.add("maxDepth", Integer.toString(v.getMaximumDepth()));
			jc.add("minPlan", v.getDeploymentDescriptorDef() == null ? " " : v.getDeploymentDescriptorDef().getXMLType().getMinimumPlan().toString());
			jc.add("target", targetCmponentVersion == null ? " " : version);
			jc.add("actions", getTasks(entry.getValue()));
			if (showDeployedVersion) {
				List<StateSearchResult> deployedVersions = v.getExistingMCOComponentState();
				String deployedVersionString = "";
				if (deployedVersions != null) {
					for (StateSearchResult searchResult : deployedVersions) {
						String deployedTo = searchResult.getSource().getRoleOrFQDN().replaceAll(".ipt(.ho)??.local", "");
						String versionInMCO = String.format("%s (%s)", searchResult.getComponentVersion(), deployedTo);
						deployedVersionString += (deployedVersionString.equals("") ? versionInMCO : "<br />" + versionInMCO);
					}
				}
				jc.add("deployedVersion", deployedVersionString);
			}
			deploymentOutput.addComponent(jc, entry.getValue());
		}
		
		JsonArrayBuilder deploymentsJson = buildDeployments(deployments);
		root.add("deployments", deploymentsJson);
	}

	/**
	 * @param deployments
	 * @return
	 */
	private JsonArrayBuilder buildDeployments(Map<String, DeploymentOutput> deployments) {
		JsonArrayBuilder jsonDeployments = Json.createArrayBuilder();
		for (DeploymentOutput d : deployments.values()) {
			JsonObjectBuilder jd = Json.createObjectBuilder();
			jd.add("name", d.name);
			jd.add("version", d.version);
			jd.add("components", d.getComponents());
			addCountForTypes(jd, d);
			jsonDeployments.add(jd);
		}
		return jsonDeployments;
	}
	
	private void addCountForTypes(JsonObjectBuilder jd, DeploymentOutput d) {
		for (Entry<ChangeType, Integer> changeType : d.changeTypeCounts.entrySet()) {
			jd.add(changeType.getKey().toString().toLowerCase(), d.getCount(changeType.getKey()));
		}
	}

	private static class DeploymentOutput {
		private String name, version;
		
		private JsonArrayBuilder components = Json.createArrayBuilder();
		
		EnumMap<ChangeType, Integer> changeTypeCounts = new EnumMap<>(ChangeType.class);
		
		DeploymentOutput(String name, String version) {
			this.name = name;
			this.version = version;
		}
		
		private void addComponent(JsonObjectBuilder jc, ComponentDeploymentData componentDeploymentData) {
			components.add(jc);
			registerChangeSets(componentDeploymentData.getChangeSets());
		}
		
		private void registerChangeSets(List<ChangeSet> changeSets) {
			for (ChangeSet changeSet : changeSets) {
				ChangeType changeType = changeSet.getPrimaryChange().getChangeType();
				registerChange(changeType);
				for (Change change : changeSet.getSubTasks()) {
					registerChange(change.getChangeType());
				}
			}
		}

		private void registerChange(ChangeType changeType) {
			if (!changeTypeCounts.containsKey(changeType)) {
				changeTypeCounts.put(changeType, 0);
			}
			Integer count = changeTypeCounts.get(changeType) + 1;
			changeTypeCounts.put(changeType, count);
		}
		
		public JsonArrayBuilder getComponents() {
			return components;
		}
		
		public Integer getCount(ChangeType type) {
			return changeTypeCounts.get(type);
		}
	}
	
	
	/**
	 * Builds uan unordered list from the Changes
	 * 
	 * @param v
	 * @return
	 */
	private JsonArrayBuilder getTasks(ComponentDeploymentData v) {
		List<ChangeSet> changeSets = v.getChangeSets();
		JsonArrayBuilder primaryChanges = Json.createArrayBuilder();
		if (changeSets != null) {
			for (ChangeSet changeSet : changeSets) {
				Change change = changeSet.getPrimaryChange();
				JsonObjectBuilder primaryChange = Json.createObjectBuilder();
				change(change, primaryChange);
				if (changeSet.isComplexChange()) {
					JsonArrayBuilder secondaryChanges = Json.createArrayBuilder();
					for (Change subChange : changeSet.getSubTasks()) {
						JsonObjectBuilder secondaryChange = Json.createObjectBuilder();
						change(subChange, secondaryChange);
						secondaryChanges.add(secondaryChange);
					}
					primaryChange.add("secondaryChanges", secondaryChanges);
				}
				
				primaryChanges.add(primaryChange);
			}
		}
		return primaryChanges;
	}

	/**
	 * The output for a single change
	 * 
	 * @param cs
	 * @param change
	 * @param primaryChange 
	 */
	private void change(Change change, JsonObjectBuilder primaryChange) {
		String reasonForFailure = change.getReasonForFailure() == null ? "" : ",[" + change.getReasonForFailure() + "]";
		String warning = change.getWarning() == null ? "" : "["+change.getWarning()+"]";
		String prepared = change.isPrepared() == false ? "" : " [Prepared]";
		String hostOrRole = change.getSearchResult() == null ? "" : " (" + change.getSearchResult().getSource().getRoleOrFQDN() + ")";
		String s = change.getChangeType() + prepared + reasonForFailure + warning + hostOrRole;
		
		String t = "";
		if (change.getChangeType() == ChangeType.FAIL) {
			t =  "<span class=\"update_failed\">";
			t += s;
			t += "</span>";
		}
		else {
			t = s;
		}
		
		primaryChange.add("change", t);
	}

	/**
	 * Writes all the data out to an output file.
	 */
	private void writeToFile(File file, JsonObjectBuilder data) throws IOException {
		String dataJson = data.build().toString();
		String bodyContent = IOUtils.toString(getClass().getResourceAsStream("/report-body.html")).replace("|data|", dataJson);
		StringBuffer b = new StringBuffer();
		b.append(buildHead());
		b.append(bodyContent);
		if (Configuration.isAutoRefreshEnabled()) {
			b.append(buildAutoRefreshScript());
			b.append("</body></html>");
		}
		this.writeToFile(file, b);
	}
	
	
	private String buildAutoRefreshScript() {
		return "<script type=\"text/javascript\">window.setUpAutoRefresh()</script>";
	}

	/**
	 * Builds an HTML HEAD section with appropriate scripts for the standalone report.
	 */
	private String buildHead() {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html><html><head>");
		try {
			sb.append("<script type=\"text/javascript\">");
			sb.append(IOUtils.toString(getClass().getResourceAsStream("/js/report-utils.js")));
			sb.append("</script>");
		} catch (IOException e) {
			throw new RuntimeException("Failed to add report-utils.js", e);
		}
		sb.append("</head><body>");
		return sb.toString();
	}

	private Logger dumpLog = LogManager.getLogger("chaindump");

	/**
	 * Dumps the dependency chains as a set of unordered lists
	 * 
	 * @param components
	 * @param chain
	 * @param b
	 */
	@SuppressWarnings("unchecked")
	private void dumpDependencyChains(Map<ComponentId, ComponentDeploymentData> components, TreeMap<ComponentId, TreeMap<ComponentId, ?>> chain, JsonObjectBuilder root) {
		dumpLog.debug("Dumping dependency chains");
		if (chain != null) {
			Set<ComponentId> keySet = chain.keySet();
			JsonArrayBuilder chainsJson = Json.createArrayBuilder();
			int i = 1; 
			for (ComponentId componentId : keySet) {
				JsonObjectBuilder comp = Json.createObjectBuilder();
				comp.add("componentId", componentId.toString());
				dumpLog.debug((i++) + ": " + componentId);
				TreeMap<ComponentId, TreeMap<ComponentId, ?>> chains = (TreeMap<ComponentId, TreeMap<ComponentId, ?>>) chain.get(componentId);
				if (chains != null) {
					dump(components, chains, "   ", comp);
				}
				chainsJson.add(comp);
			}
			root.add("dependency_chains", chainsJson);
		} else {
			dumpLog.debug("Chain was null");
		}
		dumpLog.debug("Chains complete");
	}

	/**
	 * The inside of the recursive loop which traverses the dependency chains
	 * 
	 * @param components
	 * @param chain
	 * @param prefix
	 * @param b
	 */
	@SuppressWarnings("unchecked")
	private void dump(Map<ComponentId, ComponentDeploymentData> components, TreeMap<ComponentId, TreeMap<ComponentId, ?>> chain, String prefix, JsonObjectBuilder root) {
		Set<ComponentId> keySet = chain.keySet();
		int size = keySet.size();
		int i = 0;
		JsonArrayBuilder componentsJson = Json.createArrayBuilder();
		for (ComponentId componentId : keySet) {
			JsonObjectBuilder child = Json.createObjectBuilder();
			child.add("componentId", componentId.toString());
			child.add("maxDepth", components.get(componentId).getMaximumDepth());
			dumpLog.debug(prefix + (size > 1 ? "+-> " : "--> ") + componentId + " " + components.get(componentId).getMaximumDepth());
			TreeMap<ComponentId, TreeMap<ComponentId, ?>> chains = (TreeMap<ComponentId, TreeMap<ComponentId, ?>>) chain.get(componentId);
			if (chains != null) {
				dump(components, chains, prefix + (size > 1 && i < size - 1 ? "|  " : "   "), child);
			}
			i++;
			componentsJson.add(child);
		}
		root.add("children", componentsJson);
	}
	

    /**
     * Writes the deployment descriptor out (for reference)
     * @param deployment
     * @param root
     */
	private void dumpDeploymentDescriptors(CompositeReleaseDeployment deployment, JsonObjectBuilder root) {
		JsonObjectBuilder descriptors = Json.createObjectBuilder();
		JsonArrayBuilder appDescriptors = Json.createArrayBuilder();
		if (!deployment.getApplicationDeploymentDescriptorFiles().isEmpty()) {
			for (Entry<String, File> ddEntry : deployment.getApplicationDeploymentDescriptorFiles().entrySet()) {
					JsonObjectBuilder ddJson = Json.createObjectBuilder();
					ddJson.add("appName", deployment.getApplicationVersion(ddEntry.getKey()).getApplication().getName());
					ddJson.add("shortName", ddEntry.getKey());
					String url = String.format(Configuration.getSS2StashUrlTemplate(), ddEntry.getKey(), ddEntry.getValue().getName());
					if (deployment.getappDDsGitRevisionHash() != null) {
						url += "&until=" + deployment.getappDDsGitRevisionHash();
					}
					ddJson.add("fileURL", url);
					appDescriptors.add(ddJson);
			}
			descriptors.add("appDescriptors", appDescriptors);
		}
		
		
		String url = String.format(Configuration.getSS3StashUrlTemplate(), deployment.getReleaseDeploymentDescriptorRelativeFilePath().replaceAll("\\\\", "/")); // 4 slashes to replace a single backslash. yep, Java.
		if (deployment.getReleaseDDsGitRevisionHash() != null) {
			url += "&until=" + deployment.getReleaseDDsGitRevisionHash();
		}
		descriptors.add("releaseDescriptor", url);
		root.add("descriptors", descriptors);
	}

	/**
	 * Dumps transitions into the report data
	 */
	private void dumpTransitions(CompositeReleaseDeployment deployment, JsonObjectBuilder root) {
		
		if (deployment.getTransitions() != null && deployment.getTransitions().size() > 0) {
			log.info("Adding " + deployment.getTransitions().size() + " transitions to report");
			JsonArrayBuilder phases = Json.createArrayBuilder();
			for (CompositeReleasePhase p : deployment.getPhases()) {
				JsonObjectBuilder phase = Json.createObjectBuilder();
				JsonArrayBuilder transitions = Json.createArrayBuilder();
				
				for (Transition transition : p.getTransitions()) {
					JsonObjectBuilder transitionJson = Json.createObjectBuilder();
					JsonArrayBuilder updates = Json.createArrayBuilder();
					JsonArrayBuilder commands = Json.createArrayBuilder();
					dumpResults(deployment, transition.getUpdates(), updates);
					dumpCommands(deployment, transition.getCommands(), commands);
					transitionJson.add("updates", updates);
					transitionJson.add("commands", commands);
					if (this.withStatus) {
						transitionJson.add("status", transition.getStatus().toString());
						if (!StringUtils.isEmpty(transition.getStatusMessage())){
							transitionJson.add("statusMessage", transition.getStatusMessage());
						}
						if (transition.getException() != null) {
							transitionJson.add("exception", transition.getException().toString());
							transitionJson.add("stackTrace", ExceptionUtils.getStackTrace(transition.getException()));
						}
						if (!StringUtils.isEmpty(transition.getLog())) {
							JsonArrayBuilder logArr = Json.createArrayBuilder();
							String[] logLines = transition.getLog().split("\n");
							for (String logLine : logLines ) {
								logArr.add(logLine);
							}
							transitionJson.add("log", logArr);
						}
					}
					if (transition.isStopAfter()) {
						transitionJson.add("stopAfter", true);
						transitionJson.add("stopMessage", transition.getStopMessage());
					}
					if (transition.getWaitSeconds() > 0) {
						transitionJson.add("waitInterval", transition.getWaitSeconds());
					}
					transitions.add(transitionJson);
				}
				String applicationNames = Joiner.on(",").join(p.getApplicationShortNames());
				phase.add("applications", applicationNames);
				phase.add("transitions", transitions);
				if (this.withStatus) {
					phase.add("status", p.getStatus().toString());
				}
				phases.add(phase);
			}
			
			root.add("phases", phases);
			if (this.autoExpand) {
				root.add("autoExpand", true);
			}
		}
		else {
			log.info("No Transitions to add");
		}
	}
	
	private void dumpCommands(Deployment deployment, List<MCOCommand> commands, JsonArrayBuilder commandsJson) {
		if (commands != null && commands.size() > 0) {
			for (MCOCommand command : commands) {
				if (command != null) {
					JsonObjectBuilder commandJson = Json.createObjectBuilder();
					commandJson.add("application", checkNull(command.getApplicationShortName()));
					commandJson.add("command", checkNull(command.getCommand()));
					commandJson.add("hosts", checkNull(command.getHosts()));
					commandsJson.add(commandJson);
				}
			}
		}
	}

	/**
	 * Dumps transitions into a table
	 * 
	 * @param deployment
	 * @param updatesArray 
	 */
	private void dumpResults(Deployment deployment, List<EnvironmentUpdate> updates, JsonArrayBuilder updatesArray) {
		if (updates != null && updates.size() > 0) {
			for (EnvironmentUpdate update : updates) {
				JsonObjectBuilder updateJson = Json.createObjectBuilder();
				if (update != null) {
					HieraEnvironmentUpdate hieraUpdate = null;
					if (update instanceof HieraEnvironmentUpdate) {
						hieraUpdate = (HieraEnvironmentUpdate)update;
					}
					updateJson.add("application", checkNull(update.getApplicationName()));
					if(hieraUpdate != null) {
						updateJson.add("existingPath", checkNull(hieraUpdate.getExistingPath()));
					}
					updateJson.add("existingValue", checkNull(update.getExistingValue()));
					if(hieraUpdate != null) {
						updateJson.add("requestedPath", checkNull(hieraUpdate.getRequestedPath()));
					}
					updateJson.add("requestedValue", checkNull(update.getRequestedValue()));
					if(hieraUpdate != null) {
						updateJson.add("pathsAdded", checkNull(hieraUpdate.getPathElementsAdded()));
						updateJson.add("pathsRemoved", checkNull(hieraUpdate.getPathElementsRemoved()));
					}
					if (update.getSource() instanceof HieraMachineState) {
						HieraMachineState f = (HieraMachineState) update.getSource();
						updateJson.add("hieraFile", trimPath(deployment, f.getFile().getPath()));
					} else {
						updateJson.add("hieraFile", trimPath(deployment, trimPath(deployment, update.getSource().getSourceName())));
					}
					updatesArray.add(updateJson);
				}
			}
		}
	}
		
	private static String checkNull(Object s) {		
		return s == null || StringUtils.isBlank(s.toString()) ? "" : s.toString();
	}
	
	private static String trimPath(Deployment deployment, String path) {
		String hieraFolder = Configuration.getHieraFolder();
		EMGitManager gitManager = deployment.getGitManager();
		if (hieraFolder == null && null != gitManager) {
			hieraFolder = gitManager.getCheckoutDir().getAbsolutePath();
		}
		
		if (hieraFolder == null) {
			return path;
		}
		
		File file = new File(hieraFolder);
		String fullPath = file.getAbsolutePath();
		String s = path;
		if (path != null && path.length() > fullPath.length()) {
			s = path.substring(fullPath.length());
		}
		return checkNull(s);
	}
}
