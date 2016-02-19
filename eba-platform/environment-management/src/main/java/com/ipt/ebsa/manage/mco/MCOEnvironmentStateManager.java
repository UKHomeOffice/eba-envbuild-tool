package com.ipt.ebsa.manage.mco;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.environment.EnvironmentStateManager;
import com.ipt.ebsa.manage.puppet.EMPuppetManager;
import com.ipt.ebsa.manage.puppet.SshManager;
import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.util.OrgEnvUtil;

public class MCOEnvironmentStateManager implements EnvironmentStateManager {
	
	private static Logger LOG = LogManager.getLogger(MCOEnvironmentStateManager.class);

	private static final String YUM_LIST_INSTALLED_COMMAND = "runscript run_script scriptToRun=/usr/local/bin/list_installed_rpms.sh -j";
	
	// Keyed on zone, e.g. 'IPT_ST_SIT1_COR1'
	protected Map<String, Map<String, MCOMachineState>> zoneState = new TreeMap<>();
	private EMPuppetManager puppet;
	// Not actually used - we could use this in future to restrict the hosts MCO will query
	protected Collection<ResolvedHost> scope;
	protected Set<String> zones;
	
	private static final String INSTALLED_REGEX = "(.*?Installed Packages.*?\n)(?<serverData>.*?)\\Z";
	private final Pattern installedPattern;

	public MCOEnvironmentStateManager() {
		super();
		installedPattern = Pattern.compile(INSTALLED_REGEX, Pattern.DOTALL);
	}

	public MCOEnvironmentStateManager(EMPuppetManager puppetManager) {
		this();
		this.puppet = puppetManager;
	}

	@Override
	public boolean load(Deployment deploy, Organisation org, Set<String> zones, Map<String, Collection<ResolvedHost>> scopes) {
		this.zones = zones;
		// Ask MCO for a list of installed applications
		ExecReturn output = this.getPuppetManager()
				.doMCollectiveOperationWithOutput(org, zones, YUM_LIST_INSTALLED_COMMAND, false, Configuration.getEnvironmentStateMCOTimeout());
		String mcoOutput = output.getStdOut();
		Map<String, String> jsonOutput = MCOUtils.parseJson(mcoOutput);
		this.zoneState = parseMCOYumList(jsonOutput);
		return true;
	}

	@Override
	public List<StateSearchResult> findComponent(String componentName, Set<String> zones) {
		List<StateSearchResult> occurrencesInZones = new ArrayList<>();
		for (String zone : zones) {
			occurrencesInZones.addAll(findComponent(componentName, zone));
		}
		return occurrencesInZones;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<StateSearchResult> findComponent(String componentName, String zone) {
		LOG.debug(String.format("Searching for %s in %s", componentName, zone));
		List<StateSearchResult> occurrences = new ArrayList<StateSearchResult>();

		for (Map.Entry<String, Map<String, MCOMachineState>> entry : this.zoneState.entrySet()) {
			if (!OrgEnvUtil.zoneIsInEnvironment(entry.getKey(), zone)) {
				LOG.info("Skipping zone: " + entry.getKey());
				continue;
			}
			LOG.info("Searching zone: " + entry.getKey());
			Map<String, MCOMachineState> currentEnv = entry.getValue();
			for (Map.Entry<String, MCOMachineState> machine : currentEnv
					.entrySet()) {
				MCOMachineState ms = machine.getValue();

				LOG.info(String.format("Searching machine: %s, %s packages",
						machine.getKey(), ms.getState().size()));

				StateSearchResult ssr = new MCOStateSearchResult();
				ssr.setSource(ms);

				Object componentState = ms.getState().get(componentName);

				if (componentState != null) {
					LOG.info(String.format("Found component: %s",
							componentState));
					ssr.setComponentState((Map<String, Object>) componentState);
					occurrences.add(ssr);
				}
			}
		}
		return occurrences;
	}

	@Override
	public boolean doesRoleOrHostExist(String roleOrHost, String zone) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public MCOMachineState getEnvironmentState(String zone, String hostOrRole) {
		if (this.zoneState == null) {
			throw new NullPointerException("Environments have not been loaded");
		}
		
		Map<String, MCOMachineState> machines = zoneState.get(zone);
		if (machines == null) {
			throw new NullPointerException("Environment not found");
		}
		
		return machines.get(hostOrRole);
	}

	private EMPuppetManager getPuppetManager() {
		if (puppet == null) {
			try {
				puppet = new EMPuppetManager(new SshManager());
			} catch (Exception e) {
				throw new RuntimeException(
						"Failed to instantiate PuppetManager", e);
			}
		}
		return puppet;
	}

	/**
	 * Parses output from MCO and Yum into a set of environment states
	 * 
	 * @param mcoOutput
	 *            The output from MCO and Yum containing the list of packages
	 *            per machine, as a map of servername vs output.
	 * @return A map of the environments, and their machines.
	 */
	protected Map<String, Map<String, MCOMachineState>> parseMCOYumList(Map<String, String> mcoOutput) {
		// A map of the environments, and their machines.
		Map<String, Map<String, MCOMachineState>> results = new TreeMap<String, Map<String, MCOMachineState>>();

		// For each server data block we find...
		for (Map.Entry<String, String> entry : mcoOutput.entrySet()) {
			String currentData = entry.getValue();
			String serverName = entry.getKey();
			String envName = OrgEnvUtil.getEnvironmentNameForServer(serverName);
			
			Map<String, Object> state = parsePackages(currentData);

			MCOMachineState ms = new MCOMachineState(envName, serverName, state);
			Map<String, MCOMachineState> envState = new TreeMap<String, MCOMachineState>();
			if (results.containsKey(envName)) {
				envState = results.get(envName);
			} else {
				results.put(envName, envState);
			}

			if (!envState.containsKey(serverName)) {
				LOG.debug("Found server: " + serverName);
				envState.put(serverName, ms);
			} else {
				LOG.warn(String.format(
						"Server %s already exists in environment state",
						serverName));
			}
		}

		return results;
	}

	/**
	 * Parses the packages for a machine
	 * 
	 * @param mcoOutputForMachine
	 *            - the package list output from Yum
	 * @return a Map where componentName is the key and a map of properties of
	 *         that component (eg, its version) are the value.
	 */
	private Map<String, Object> parsePackages(String mcoOutputForMachine) {
		// Set up a Matcher to get the installed package list from the server data
		Matcher installedMatcher = installedPattern.matcher("");
		
		// Refine the data down to package names...
		installedMatcher.reset(mcoOutputForMachine);
		Map<String, Object> machineComponents = new TreeMap<String, Object>();
		if (installedMatcher.find()) {
			mcoOutputForMachine = installedMatcher.group("serverData");

			String[] lines = mcoOutputForMachine.split("\\r?\\n");
		
			// Columns are separated by 1 or more spaces
			String seperatorRegex = "\\s+";
	
			// All lines from here onwards are packages (or part thereof)
			for (int i = 0; i < lines.length; i++) {
				String currentString = lines[i].trim();
				String[] splitLine = currentString.split(seperatorRegex);
	
				// Should be three "columns" per line, if there aren't, the
				// remaining columns can be found on the next line
				int expectedColumns = 3;
	
				// If we didn't get the expected number of columns, take a look at
				// the next line in the mcoOutput
				while (splitLine.length < expectedColumns && i + 1 < lines.length) {
					i++;
					currentString = lines[i].trim();
	
					// Combine the columns from this line, to the columns from the
					// previous line
					splitLine = (String[]) ArrayUtils.addAll(splitLine,
							currentString.split(seperatorRegex));
				}
	
				// If there's more or less than *expectedColumns*, something went
				// wrong.
				if (splitLine.length == expectedColumns) {
					// Tidy up the component name
					String componentPackage = splitLine[0];
					String componentName = componentPackage.split("\\.")[0];
					//componentName = componentName.replace("-rpm", "");
					String version = splitLine[1];
	
					// Create the component properties map.
					Map<String, Object> componentProperties = new TreeMap<String, Object>();
					componentProperties.put(MCOStateSearchResult.PACKAGE_KEY, componentPackage);
					componentProperties.put(MCOStateSearchResult.VERSION_KEY, version);
	
					// Add the component properties to the machine components map.
					machineComponents.put(componentName, componentProperties);
					
					LOG.debug(String.format("Found component: %s (%s)", componentName, version));
				}
			}
			LOG.debug(String.format("Found %s packages on server", machineComponents.size()));
		} else {
			LOG.warn("Unable to parse installed packages for server");
		}
		return machineComponents;
	}
}
