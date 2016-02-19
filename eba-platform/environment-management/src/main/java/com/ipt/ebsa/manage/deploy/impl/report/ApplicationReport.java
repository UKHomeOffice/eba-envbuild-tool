package com.ipt.ebsa.manage.deploy.impl.report;

import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.deployment.descriptor.DDConfigurationWriter;
import com.ipt.ebsa.deployment.descriptor.XMLDeploymentDescriptorType;
import com.ipt.ebsa.environment.EnvironmentUpdate;
import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.ApplicationDeployment;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.deploy.impl.Change;
import com.ipt.ebsa.manage.deploy.impl.ChangeSet;
import com.ipt.ebsa.manage.deploy.impl.ChangeType;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.transitions.MCOCommand;
import com.ipt.ebsa.manage.transitions.Transition;

/**
 * This provides a report of the actions which will be taken by the deployment utility given all of the 
 * information whih has been passed in.
 * @author scowx
 *
 */
public class ApplicationReport extends Report {
	
	private Logger log = LogManager.getLogger(ApplicationReport.class);
	private final String defaultTemplateStart = "<html><head><title>Deployment report</title><style>body {font-size: small; font-family: Verdana, Arial, Sans-serif; }td {font-size: small; font-family: Verdana, Arial, Sans-serif; }</style></head><body>";
	private final String defaultTemplateEnd = "</body></html>";
	private boolean showTitleDetails;
	
	@Override
	public void generateReport(Deployment deployment) throws Exception {
		if (deployment instanceof ApplicationDeployment) {
			dumpTableToFile((ApplicationDeployment)deployment);
		} else {
			throw new RuntimeException("Unable to generate report for deployment of type: " + deployment.getClass().getName());
		}
	}
	
	/**
	 * Dump out the deployment table
	 * 
	 * @param aggEntries
	 */
	private void dumpTableToFile(ApplicationDeployment deployment) throws Exception {
		log.debug("Writing out deployment report");
		StringBuffer b = new StringBuffer();

		File file = determineFile(deployment.getId());
		
		if (showTitleDetails) {
			b.append("<h2>Deployment details</h2>");
			b.append("<p>");
			b.append(deployment.getId());
			b.append(" ");
			b.append(deployment.getApplicationVersion(deployment.getApplicationShortName()));
			b.append("</p>");
		}
		
		b.append("<h2>Deployment plan</h2>");
		dumpDeploymentActions(deployment, b);

		b.append("<h2>Dependency chains</h2>");
		dumpDependencyChains(deployment.getComponents(), deployment.getDependencyChains(), b);		
		
		b.append("<h2>Transitions</h2>");
		dumpTransitions(deployment, b);
			
		b.append("<h2>Deployment Descriptor [").append(deployment.getApplicationShortName()).append("]</h2>");
		dumpDeploymentDescriptor(deployment.getDeploymentDescriptor(deployment.getApplicationShortName()).getXMLType(), b);
		
		if (deployment.getFailFilesByFQDN() != null) {
			b.append("<h2>RPM Fail Files</h2>");
			dumpFailFiles(deployment, b);
		}
		
		if (Configuration.isWrapReportInTemplate()) {
		  b.insert(0, this.defaultTemplateStart);
		  b.append(defaultTemplateEnd);
		}
		writeToFile(file, b);
		log.debug("Finished writing out deployment report to '"+file.getAbsolutePath()+"'");
	}
	
	private void dumpFailFiles(Deployment deployment, StringBuffer b) {
		/*		  
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
		b.append(getTable() + "id=\"failfiles\"><thead><tr>");
		b.append("<th>Host</th>");
		b.append("<th>Status</th>");
		b.append("</tr></thead><tbody>");
		
		Map<String, String> failFilesByFQDN = deployment.getFailFilesByFQDN();
		if (!failFilesByFQDN.isEmpty()) {
			for (Entry<String, String> failFiles : failFilesByFQDN.entrySet()) {
				b.append("<tr>");
				b.append(String.format("<td valign=\"top\">%s</td>", failFiles.getKey()));
				boolean fileFound = !failFiles.getValue().isEmpty();
				b.append(String.format("<td style=\"color: %s\" valign=\"top\">%s</td>", (fileFound ? "red" : "black"), (fileFound ? failFiles.getValue() : "None found")));
				b.append("</tr>");
			}
		} else {
			b.append("<tr><td style=\"color: red\" colspan=\"2\" valign=\"top\">Unable to report RPM fail files - check the logs for details</td></tr>");
		}
		b.append("</tbody></table>");
	}

	private void dumpDeploymentActions(Deployment deployment, StringBuffer b) {
		Map<ComponentId, ComponentDeploymentData> components = deployment.getComponents();
		boolean showDeployedVersion = (deployment.getAlternateEnvironmentStateManager() != null);
		// Draw a neat table
		b.append(getTable() + "id=\"applicationversionreport\"><thead><tr>");
		b.append("<th>Name</th>");
		b.append("<th>Minimum plan</th>");
		b.append("<th>Target version</th>");
		b.append("<th>Existing version</th>");
		if (showDeployedVersion) {
			b.append("<th>Deployed Version</th>");
		}
		b.append("<th>Max depth</th>");
		b.append("<th>Action(s)</th>");
		b.append("</tr></thead><tbody>");

		for (Entry<ComponentId, ComponentDeploymentData> entry : components.entrySet()) {
			ComponentDeploymentData v = entry.getValue();
			ComponentVersion targetCmponentVersion = v.getTargetCmponentVersion();
			
			String version = targetCmponentVersion == null ? null : targetCmponentVersion.getComponentVersion() + "&nbsp;&nbsp;&nbsp; [ "+targetCmponentVersion.getRpmPackageVersion()+" ]";
			List<StateSearchResult> searchResults = v.getOriginalExistingComponentState();
			
			String currentVersions = "";
			if (searchResults != null) {
				for (StateSearchResult searchResult : searchResults) {
					String versionInYaml = searchResult.getComponentVersion();
					currentVersions += (currentVersions.equals("") ? versionInYaml : ", " + versionInYaml);
				}
			} else {
				currentVersions = "&nbsp;";
			}

			b.append("<tr>");
			b.append(String.format("<td valign=\"top\">%s</td>", v.getComponentId()));
			b.append(String.format("<td valign=\"top\">%s</td>", v.getDeploymentDescriptorDef() == null ? "&nbsp;" : v.getDeploymentDescriptorDef().getXMLType().getMinimumPlan()));
			b.append(String.format("<td valign=\"top\">%s</td>", targetCmponentVersion == null ? "&nbsp;" : version));
			b.append(String.format("<td valign=\"top\">%s</td>", currentVersions));
			if (showDeployedVersion) {
				List<StateSearchResult> deployedVersions = v.getExistingMCOComponentState();
				String deployedVersionString = "";
				if (deployedVersions != null) {
					for (StateSearchResult searchResult : deployedVersions) {
						String deployedTo = searchResult.getSource().getRoleOrFQDN().replaceAll(".ipt(.ho)??.local", "");
						String versionInMCO = String.format("%s (%s)", searchResult.getComponentVersion(), deployedTo);
						deployedVersionString += (deployedVersionString.equals("") ? versionInMCO : "<br />" + versionInMCO);
					}
				} else {
					deployedVersionString = "&nbsp;";
				}
				b.append(String.format("<td valign=\"top\">%s</td>", deployedVersionString));
			}
			b.append(String.format("<td valign=\"top\">%s</td>", v.getMaximumDepth()));
			b.append(String.format("<td valign=\"top\">%s</td>", getTasks(v).toString()));
			b.append("</tr>");
		}
		b.append("</tbody></table>");
	}

	/**
	 * Builds un unordered list from the Changes
	 * 
	 * @param v
	 * @return
	 */
	private StringBuffer getTasks(ComponentDeploymentData v) {
		StringBuffer cs = new StringBuffer();
		List<ChangeSet> changeSets = v.getChangeSets();
		if (changeSets != null) {
			cs.append("<ul>");
			for (ChangeSet changeSet : changeSets) {
				Change change = changeSet.getPrimaryChange();
				cs.append("<li>");
				change(cs, change);
				if (changeSet.isComplexChange()) {
					cs.append("<ul>");
					for (Change subChange : changeSet.getSubTasks()) {
						cs.append("<li>");
						change(cs, subChange);
						cs.append("</li>");
					}
					cs.append("</ul>");
				}
				cs.append("</li>");
			}
			cs.append("</ul>");
		} else {
			cs.append("&nbsp;");
		}
		return cs;
	}

	/**
	 * The output for a single change
	 * 
	 * @param cs
	 * @param change
	 */
	private void change(StringBuffer cs, Change change) {
		String reasonForFailure = change.getReasonForFailure() == null ? "" : ",[" + change.getReasonForFailure() + "]";
		String warning = change.getWarning() == null ? "" : "["+change.getWarning()+"]";
		String prepared = change.isPrepared() == false ? "" : " [Prepared]";
		String hostOrRole = change.getSearchResult() == null ? "" : " (" + change.getSearchResult().getSource().getRoleOrFQDN() + ")";
		String s = change.getChangeType() + prepared + reasonForFailure + warning + hostOrRole;
		
		String t = "";
		if (change.getChangeType() == ChangeType.FAIL) {
			t =  "<span style=\"color: red;\">";
			t += s;
			t += "</span>";
		}
		else {
			t = s;
		}
		
		cs.append(t);
	}

	private Logger dumpLog = LogManager.getLogger("chaindump");

	/**
	 * Dunmps the dependency chaains as a set of unordered lists
	 * 
	 * @param components
	 * @param chain
	 * @param b
	 */
	@SuppressWarnings("unchecked")
	private void dumpDependencyChains(Map<ComponentId, ComponentDeploymentData> components, TreeMap<ComponentId, TreeMap<ComponentId, ?>> chain, StringBuffer b) {
		dumpLog.debug("Dumping dependency chains");
		if (chain != null) {
			Set<ComponentId> keySet = chain.keySet();
			b.append("<ul>");
			int i = 1;
			for (ComponentId componentId : keySet) {
				b.append("<li><strong>" + (i + " - " + componentId) + "</strong>");
				dumpLog.debug((i++) + ": " + componentId);
				TreeMap<ComponentId, TreeMap<ComponentId, ?>> chains = (TreeMap<ComponentId, TreeMap<ComponentId, ?>>) chain.get(componentId);
				if (chains != null) {
					dump(components, chains, "   ", b);
				}
				;
				b.append("</li>");
			}
			b.append("</ul>");
		}
		else {
			if (components == null || components.size() == 0)
			{
				b.append("<p>There are not components and therefore no chains.</p>");
			}
			else {
			    b.append("<p>Dependency chains have not been examined.</p>");
			}
			dumpLog.debug("Chain was null");
		}
		dumpLog.debug("Chains complete");
	}

	/**
	 * The inside of the recursive loop which traverses the depednecy chains
	 * 
	 * @param components
	 * @param chain
	 * @param prefix
	 * @param b
	 */
	@SuppressWarnings("unchecked")
	private void dump(Map<ComponentId, ComponentDeploymentData> components, TreeMap<ComponentId, TreeMap<ComponentId, ?>> chain, String prefix, StringBuffer b) {
		Set<ComponentId> keySet = chain.keySet();
		int size = keySet.size();
		int i = 0;
		b.append("<ul>");
		for (ComponentId componentId : keySet) {
			b.append("<li>" + componentId + " " + components.get(componentId).getMaximumDepth());
			dumpLog.debug(prefix + (size > 1 ? "+-> " : "--> ") + componentId + " " + components.get(componentId).getMaximumDepth());
			TreeMap<ComponentId, TreeMap<ComponentId, ?>> chains = (TreeMap<ComponentId, TreeMap<ComponentId, ?>>) chain.get(componentId);
			if (chains != null) {
				dump(components, chains, prefix + (size > 1 && i < size - 1 ? "|  " : "   "), b);
			}
			i++;
			b.append("</li>");
		}
		b.append("</ul>");
	}
	

    /**
     * Writes the deployment descriptor out (for reference)
     * @param deployment
     * @param b
     */
	private void dumpDeploymentDescriptor(XMLDeploymentDescriptorType deploymentDescriptor, StringBuffer b) throws Exception {
		b.append("<div style=\"height: 200px; width: 1000px; overflow-y: scroll;\">");
		b.append("<pre>");
		StringWriter writer = new StringWriter();
		new DDConfigurationWriter().writeTo(deploymentDescriptor, writer);
		String wrb = writer.toString();
		wrb = wrb.replaceAll("<", "&lt;");
		wrb = wrb.replaceAll(">", "&gt;");
		b.append(wrb);
		b.append("</pre>");
		b.append("</div>");
	}

	/**
	 * Dumps transitions into a table
	 * 
	 * @param deployment
	 */
	private void dumpTransitions(Deployment deployment, StringBuffer b) {
		
		if (deployment.getTransitions() != null && deployment.getTransitions().size() > 0) {
			// Draw a neat table
			b.append(getTable() + "id=\"transitions\"><thead><tr>");
			b.append("<th>Transition #</th>");
			b.append("<th>Changes</th>");
			b.append("</tr></thead><tbody>");
	
			for (Transition transition : deployment.getTransitions()) {
				b.append("<tr>");
				b.append(String.format("<td valign=\"top\">%s</td>", transition.getSequenceNumber()));
				String commands = dumpCommands(deployment, transition.getCommands());
				String updates = dumpResults(deployment, transition.getUpdates());
				String br = StringUtils.isNotBlank(commands) && StringUtils.isNotBlank(updates) ? "<br />" : "";
				String all = commands + br + updates;
				b.append(String.format("<td valign=\"top\">%s</td>", all));
				b.append("</tr>");
			}
			b.append("<tbody></table>");
		}
		else {
			b.append("<p>No relevant changes have been found to apply.</p>");
		}

	}
	
	private String dumpCommands(Deployment deployment, List<MCOCommand> commands) {
		if (commands != null && commands.size() > 0) {
			StringBuffer b = new StringBuffer();
			// Draw a neat table
			b.append(getTable() + "id=\"cmd\"><thead><tr>");
			b.append("<th>Application</th>");
			b.append("<th>Command</th>");
			b.append("<th>Hosts</th>");
			b.append("</tr></thead><tbody>");

			for (MCOCommand command : commands) {

				b.append("<tr>");
				if (command != null) {
					b.append(String.format("<td valign=\"top\">%s</td>", checkNull(command.getApplicationShortName())));
					b.append(String.format("<td valign=\"top\">%s</td>", checkNull(command.getCommand())));
					b.append(String.format("<td valign=\"top\">%s</td>", checkNull(command.getHosts())));
				}
				b.append("</tr>");
			}
			b.append("<tbody></table>");
			return b.toString();
		}
		else {
			return "";
		}
	}

	/**
	 * Dumps transitions into a table
	 * 
	 * @param deployment
	 */
	private String dumpResults(Deployment deployment, List<EnvironmentUpdate> updates) {
		if (updates != null && updates.size() > 0) {
			StringBuffer b = new StringBuffer();
			// Draw a neat table
			b.append(getTable() + "id=\"results\"><thead><tr>");
			b.append("<th>Application</th>");
			b.append("<th>Existing path</th>");
			b.append("<th>Existing value</th>");
			b.append("<th>Requested path</th>");
			b.append("<th>Requested value</th>");
			b.append("<th>Paths added</th>");
			b.append("<th>Removed</th>");
			b.append("<th>HieraFile</th>");
			b.append("</tr></thead><tbody>");
			
			for (EnvironmentUpdate update : updates) {
				b.append("<tr>");
				if (update != null) {
					HieraEnvironmentUpdate hieraUpdate = null;
					if (update instanceof HieraEnvironmentUpdate) {
						hieraUpdate = (HieraEnvironmentUpdate)update;
					}
					b.append(String.format("<td valign=\"top\">%s</td>", checkNull(update.getApplicationName())));
					if(hieraUpdate != null) {
						b.append(String.format("<td valign=\"top\">%s</td>", checkNull(hieraUpdate.getExistingPath())));
					}
					b.append(String.format("<td valign=\"top\">%s</td>", checkNull(update.getExistingValue())));
					if(hieraUpdate != null) {
						b.append(String.format("<td valign=\"top\">%s</td>", checkNull(hieraUpdate.getRequestedPath())));
					}
					b.append(String.format("<td valign=\"top\">%s</td>", checkNull(update.getRequestedValue())));
					if(hieraUpdate != null) {
						b.append(String.format("<td valign=\"top\">%s</td>", checkNull(hieraUpdate.getPathElementsAdded())));
						b.append(String.format("<td valign=\"top\">%s</td>", checkNull(hieraUpdate.getPathElementsRemoved())));
					}
					if (update.getSource() instanceof HieraMachineState) {
						HieraMachineState f = (HieraMachineState) update.getSource();
						b.append(String.format("<td valign=\"top\">%s</td>", trimPath(deployment, f.getFile().getPath())));
					} else {
						b.append(String.format("<td valign=\"top\">%s</td>", trimPath(deployment, update.getSource().getSourceName())));
					}
				}
				else {
					b.append(String.format("<td colspan=\"7\" valign=\"top\">%s</td>", "No Update!"));

				}
				b.append("</tr>");
			}
			b.append("<tbody></table>");
			return b.toString();
		}
		else {
			return "";
		}
	}
		
	private String getTable() {
		//return "<table border=\"1\" ";
		return "<table ";
	}

	public static String checkNull(Object s) {		
		return s == null || StringUtils.isBlank(s.toString()) ? " " : s.toString();
	}
	
	public static String trimPath(Deployment deployment, String path) {
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
