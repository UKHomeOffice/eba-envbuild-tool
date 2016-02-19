package com.ipt.ebsa.manage.puppet;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.util.Utils;
import com.ipt.ebsa.puppet.PuppetManager;
import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.ssh.JschManager;
import com.ipt.ebsa.ssh.SshJumpConfig;
import com.ipt.ebsa.util.OrgEnvUtil;
import com.ipt.ebsa.yaml.YamlUtil;

/**
 * @author James Shepherd
 *
 */
public class EMPuppetManager {

	private PuppetManager puppetManager;
	private SshJumpConfig sshJumpConfig;
	
	public EMPuppetManager(JschManager sshManager) {
		puppetManager = new PuppetManager(sshManager);
		puppetManager.setMcoEnabled(Configuration.getIsMcoEnabled());
		puppetManager.setPuppetRunRpcCommand(Configuration.getMCollectiveCommandMask());

		sshJumpConfig = new SshJumpConfig();
		sshJumpConfig.setUsername(Configuration.getPuppetMasterLoginUsername());
		sshJumpConfig.setPort(Configuration.getPuppetMasterPort());
		sshJumpConfig.setTimeout(Configuration.getPuppetDoPuppetTimeout());
	}

	public int updatePuppetMaster(Organisation organisation) {
		sshJumpConfig.setHostname(Configuration.getPuppetMaster(organisation));
		sshJumpConfig.setJumphosts(Utils.getJumphostsForPuppetMaster(organisation, Configuration.getPuppetMasterLoginUsername()));
		puppetManager.setUpdatePuppetCommand(Configuration.getPuppetMasterUpdateCommand(organisation));
		return puppetManager.updatePuppetMaster(sshJumpConfig);
	}
	
	public ExecReturn doPuppetRunWithRetry(Organisation organisation, String zone, String roleOrHost, int maxRetryCount, int retryDelaySeconds) {
		return doPuppetRunWithRetry(organisation, PuppetManager.buildMap(zone, roleOrHost), maxRetryCount, retryDelaySeconds);
	}

	public ExecReturn doPuppetRunWithRetry(Organisation organisation, Map<String, Set<String>> zoneRolesOrHosts, int maxRetryCount, int retryDelaySeconds) {
		sshJumpConfig.setHostname(Configuration.getPuppetMaster(organisation));
		sshJumpConfig.setJumphosts(Utils.getJumphostsForPuppetMaster(organisation, Configuration.getPuppetMasterLoginUsername()));
		puppetManager.setPuppetDoPuppetCommand(Configuration.getPuppetDoPuppetCommand());
		puppetManager.setGonzoCommand(Configuration.getPuppetRunCommand());
		return puppetManager.doPuppetRunWithRetry2(sshJumpConfig, buildDomainMap(zoneRolesOrHosts), maxRetryCount, retryDelaySeconds);
	}
	
	private Map<String, Set<String>> buildDomainMap(Map<String, Set<String>> zoneRolesOrHosts) {
		Map<String, Set<String>> domainMap = new LinkedHashMap<>();
		for (String zone : zoneRolesOrHosts.keySet()) {
			domainMap.put(OrgEnvUtil.getDomainForPuppet(zone), YamlUtil.getRolesOrHostsFromYaml(zoneRolesOrHosts.get(zone)));
		}
		return domainMap;
	}

	public int doMCollectiveOperation(Organisation organisation, Collection<ResolvedHost> hosts, String command) {
		return doMCollectiveOperationWithOutput(organisation, hosts, command).getReturnCode();
	}
	
	public ExecReturn doMCollectiveOperationWithOutput(Organisation organisation, Collection<ResolvedHost> hosts, String command) {
		setupForMCollectiveOperation(organisation);
		return puppetManager.doMCollectiveOperationWithOutput(sshJumpConfig, buildDomainMap(hosts), command);
	}
	
	public ExecReturn doMCollectiveOperationWithOutput(Organisation organisation, Collection<ResolvedHost> hosts, String command, boolean unescapeJava, int timeout) {
		setupForMCollectiveOperation(organisation);
		return puppetManager.doMCollectiveOperationWithOutput(sshJumpConfig, buildDomainMap(hosts), command, unescapeJava, timeout);
	}

	private void setupForMCollectiveOperation(Organisation organisation) {
		sshJumpConfig.setHostname(Configuration.getPuppetMaster(organisation));
		sshJumpConfig.setJumphosts(Utils.getJumphostsForPuppetMaster(organisation, Configuration.getPuppetMasterLoginUsername()));
		puppetManager.setPuppetRunRpcCommand(Configuration.getMCollectiveCommandMask());
	}

	private Map<String, Set<String>> buildDomainMap(Collection<ResolvedHost> hosts) {
		// Maintain insertion order
		Map<String, Set<String>> domainMap = new LinkedHashMap<>();
		for (ResolvedHost host : hosts) {
			String domain = OrgEnvUtil.getDomainForPuppet(host.getZone());
			if (domainMap.containsKey(domain)) {
				domainMap.get(domain).add(host.getHostOrRole());
			} else {
				// Maintain insertion order
				Set<String> domainHostsOrRoles = new LinkedHashSet<>();
				domainHostsOrRoles.add(host.getHostOrRole());
				domainMap.put(domain, domainHostsOrRoles);
			}
		}
		return domainMap;
	}

	public ExecReturn doMCollectiveOperationWithOutput(Organisation organisation, String zoneName, String mcCommand) {
		setupForMCollectiveOperation(organisation);
		return puppetManager.doMCollectiveOperationWithOutput(sshJumpConfig, OrgEnvUtil.getDomainForPuppet(zoneName), null, mcCommand);
	}
	
	public ExecReturn doMCollectiveOperationWithOutput(Organisation organisation, Set<String> zoneNames, String mcCommand, boolean unescapeJava, int timeout) {
		setupForMCollectiveOperation(organisation);
		
		Map<String, Set<String>> domainMap = new LinkedHashMap<>();
		for (String zone : zoneNames) {
			domainMap.put(OrgEnvUtil.getDomainForPuppet(zone), new HashSet<String>());
		}
		return puppetManager.doMCollectiveOperationWithOutput(sshJumpConfig, domainMap, mcCommand, unescapeJava, timeout);
	}
}
