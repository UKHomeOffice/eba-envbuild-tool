package com.ipt.ebsa.puppet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.ssh.JschManager;
import com.ipt.ebsa.ssh.SshJumpConfig;

public class PuppetManager {
	private static Logger log = LogManager.getLogger(PuppetManager.class);
	
	public static final String DEFAULT_UPDATE_PUPPET_COMMAND = "./syncPuppetConfig.strategic.sh";
	public static final String DEFAULT_RUN_RPC_COMMAND = "sudo -u peadmin /opt/puppet/bin/mco rpc -t $timeout $cmd $environment $filter";
	public static final String DEFAULT_DO_PUPPET_COMMAND = "sudo -u peadmin /opt/puppet/bin/mco rpc -t $timeout gonzo $cmd $environment $filter";
	public static final String DEFAULT_GONZO_COMMAND = "run";
	
	private JschManager jschManager;
	private boolean isMcoEnabled = true;
	private String runPuppetCommand;
	private String updatePuppetCommand = DEFAULT_UPDATE_PUPPET_COMMAND;
	private String puppetRunRpcCommand = DEFAULT_RUN_RPC_COMMAND;
	private String puppetDoPuppetCommand = DEFAULT_DO_PUPPET_COMMAND;
	private String gonzoCommand = DEFAULT_GONZO_COMMAND;
	
	public PuppetManager(JschManager sshManager) {
		this.jschManager = sshManager;
	}

	/**
	 * Runs the update puppet master script
	 * @param sshJumpConfig ssh details
	 * @return 0 on success
	 */
	public int updatePuppetMaster(SshJumpConfig sshJumpConfig) {
		if (!isMcoEnabled()) {
			return 0;
		}
		
		return jschManager.runSSHExec(sshJumpConfig.getTimeout(), getUpdatePuppetCommand(), sshJumpConfig.getUsername(), sshJumpConfig.getHostname(), sshJumpConfig.getPort(), sshJumpConfig.getJumphosts());
	}
	
	/**
	 * Executes a puppet run across a set of domains for a given role or host with retries. 
	 * Retries will be honoured up to a maximum number of retries and a fixed retry interval.
	 * @param sshJumpConfig
	 * @param domains
	 * @param roleOrHost
	 * @param maxRetryCount
	 * @param retryDelaySeconds
	 * @return
	 */
	@Deprecated
	public int doPuppetRunWithRetry(SshJumpConfig sshJumpConfig, Map<String, Set<String>> domainRolesOrHosts, final int maxRetryCount, int retryDelaySeconds){
		return doPuppetRunWithRetry2(sshJumpConfig, domainRolesOrHosts, maxRetryCount, retryDelaySeconds).getReturnCode();
	}
	
	/**
	 * Executes a puppet run across a set of domains for a given role or host with retries. 
	 * Retries will be honoured up to a maximum number of retries and a fixed retry interval.
	 * @param sshJumpConfig
	 * @param domains
	 * @param roleOrHost
	 * @param maxRetryCount
	 * @param retryDelaySeconds
	 * @return
	 */
	public ExecReturn doPuppetRunWithRetry2(SshJumpConfig sshJumpConfig, Map<String, Set<String>> domainRolesOrHosts, final int maxRetryCount, int retryDelaySeconds){
		int numPuppetRuns = 0;
		PuppetError puppetError = null;
		ExecReturn puppetRunReturn = null;
		boolean willRetry = false;

		do {
			numPuppetRuns++;
			willRetry = false;
			puppetRunReturn = doPuppetRun(sshJumpConfig, domainRolesOrHosts);
			if (puppetRunReturn.getReturnCode() != 0) {
				puppetError = PuppetError.getPuppetErrorForLog(puppetRunReturn.getStdOut());
				log.warn(String.format("Did not successfully perform the Puppet run. SS2 Message: %s", puppetError.toString()));
				if (numPuppetRuns < maxRetryCount && puppetError.getCanRetry()) {
					log.warn(String.format("Will retry Puppet run after %s seconds (Attempt %s of %s)", retryDelaySeconds, numPuppetRuns, maxRetryCount));
					willRetry = true;
					try {
						Thread.sleep(retryDelaySeconds * 1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				} else {
					if (puppetError.getCanRetry()) {
						log.warn(String.format("Have done maximum of %s retries. Ending.", numPuppetRuns));
					} else {
						log.warn("Unable to retry with the error given. Please check the messages above and take the necessary action.");
					}
				}
			}
		} while (willRetry);
		
		return puppetRunReturn;
	}
	
	/**
	 * Executes a puppet run on a domain for a given role or host with retries. 
	 * Retries will be honoured up to a maximum number of retries and a fixed retry interval.
	 * @param sshJumpConfig
	 * @param domain
	 * @param roleOrHost
	 * @param maxRetryCount
	 * @param retryDelaySeconds
	 * @return
	 */
	@Deprecated
	public int doPuppetRunWithRetry(SshJumpConfig sshJumpConfig, String domain, String roleOrHost, final int maxRetryCount, int retryDelaySeconds){
		return doPuppetRunWithRetry(sshJumpConfig, buildMap(domain, roleOrHost), maxRetryCount, retryDelaySeconds);
	}
	
	public static Map<String, Set<String>> buildMap(String zone, String roleOrHosts) {
		String[] roleOrHostsSplit = new String[0];
		if (roleOrHosts != null && roleOrHosts.length() > 0) {
			roleOrHostsSplit = roleOrHosts.split(",");
		}
		Map<String, Set<String>> map = new HashMap<>(roleOrHostsSplit.length);
		Set<String> set = new HashSet<>(roleOrHostsSplit.length);
		for (String roleOrHost : roleOrHostsSplit) {
			set.add(roleOrHost);
		}
		map.put(zone, set);
		return map;
	}
	
	/**
	 * Do a puppet run against the given role or host across a set of domains
	 * @param sshJumpConfig ssh details
	 * @param domains
	 * @param roleOrHost
	 * @return 0 on success
	 */
	public ExecReturn doPuppetRun(SshJumpConfig sshJumpConfig, Map<String, Set<String>> domainRolesOrHosts) {
		if (!isMcoEnabled()) {
			return new ExecReturn(0);
		}
		
		StringBuilder filter = getDomainRolesOrHostsFilter(domainRolesOrHosts);
		
		String environmentResult = getPuppetDoPuppetCommand().replace("$environment", "");
		String filterResult = environmentResult.replace("$filter", filter.toString());
		String timeoutResult = filterResult.replace("$timeout", String.valueOf(sshJumpConfig.getTimeout()));
		String commandResult = timeoutResult.replace("$cmd", getGonzoCommand());
		
		return jschManager.runSSHExecWithOutput(sshJumpConfig.getTimeout(), commandResult, sshJumpConfig.getUsername(), sshJumpConfig.getHostname(), sshJumpConfig.getPort(), sshJumpConfig.getJumphosts(), true);
	}

	private StringBuilder getDomainRolesOrHostsFilter(
		Map<String, Set<String>> domainRolesOrHosts) {
		StringBuilder filter = new StringBuilder("-S \"");
		int domainI = 0;
		for (String domain : domainRolesOrHosts.keySet()) {
			if (domainI++ > 0) {
				filter.append(" or ");
			}
			filter.append("(domain=");
			filter.append(domain);
			Set<String> roleOrHostsSet = domainRolesOrHosts.get(domain);
			if (roleOrHostsSet.size() > 0) {
				filter.append(" and (");
				int roleOrHostI = 0;
				for (String roleOrHost : roleOrHostsSet) {
					if (roleOrHostI++ > 0) {
						filter.append(" or ");
					}
					filter.append(getRoleOrFQDNParamString(roleOrHost, domain));
				}
				filter.append(")");
			}
			filter.append(")");
		}
		filter.append("\"");
		return filter;
	}

	/**
	 * Do a puppet run against the given role or host in the given domain
	 * @param sshJumpConfig ssh details
	 * @param domain
	 * @param roleOrHost
	 * @return 0 on success
	 */
	public ExecReturn doPuppetRun(SshJumpConfig sshJumpConfig, String domain, String roleOrHost) {
		return doPuppetRun(sshJumpConfig, buildMap(domain, roleOrHost));
	}

	/**
	 * Returns a string that represents either the ROLE (if roleOrHost is 3 chars) or the 
	 * FQDN which is the host + fullstop + domain
	 * @param roleOrHost
	 * @param domain
	 * @return
	 */
	public static String getRoleOrFQDNParamString(String roleOrHost, String domain) {
		StringBuilder sb = new StringBuilder();
		if(roleOrHost.length() == 3) {
			sb.append("role=");
			sb.append(roleOrHost);
		} else if (roleOrHost.length() > 3) {
			sb.append("fqdn=");
			sb.append(roleOrHost);
			sb.append(".");
			sb.append(domain);
		} else {
			throw new RuntimeException("The role or host was not valid for conversion to a ROLE or FQDN ["+roleOrHost+"]");
		}
		return sb.toString();
	}
	
	/**
	 * Runs mco with given command against the given domain and roles/hosts and returns the response code
	 * @param sshJumpConfig ssh details
	 * @param domain
	 * @param rolesOrHosts
	 * @param command
	 * @return
	 */
	public int doMCollectiveOperation(SshJumpConfig sshJumpConfig, String domain, String rolesOrHosts, String command) {
		return doMCollectiveOperationWithOutput(sshJumpConfig, domain, rolesOrHosts, command).getReturnCode();
	}
	
	/**
	 * Runs mco with given command against the given domain and roles/hosts and returns the response data
	 * Uses the -S syntax MCO syntax to construct the command, e.g.
	 * 
	 * /opt/puppet/bin/mco rpc -t 3000000 service status service=weblogic-app  
	 * -S "(domain=st-sst1-app1.ipt.local and (fqdn=soatzm01.st-sst1-app1.ipt.local or fqdn=soatzm02.st-sst1-app1.ipt.local))" --verbose
	 * 
	 * @param sshJumpConfig ssh details
	 * @param domain
	 * @param rolesOrHosts, comma-separated, could be null
	 * @param command
	 * @return
	 */
	public ExecReturn doMCollectiveOperationWithOutput(SshJumpConfig sshJumpConfig, String domain, String rolesOrHosts, String command) {
		return doMCollectiveOperationWithOutput(sshJumpConfig, buildMap(domain, rolesOrHosts), command, true, sshJumpConfig.getTimeout());
	}
	
	public ExecReturn doMCollectiveOperationWithOutput(SshJumpConfig sshJumpConfig, Map<String, Set<String>> domainRolesOrHosts, String command) {
		return doMCollectiveOperationWithOutput(sshJumpConfig, domainRolesOrHosts, command, true, sshJumpConfig.getTimeout());
	}
	
	public ExecReturn doMCollectiveOperationWithOutput(SshJumpConfig sshJumpConfig, Map<String, Set<String>> domainRolesOrHosts, String command, boolean unescapeJava, int timeout) {
		if (!isMcoEnabled()) {
			return new ExecReturn(0);
		}
		
		StringBuilder filter = getDomainRolesOrHostsFilter(domainRolesOrHosts);
		
		String commandResult = getPuppetRunRpcCommand().replace("$cmd", command);
		String timeoutResult = commandResult.replace("$timeout", Integer.toString(timeout));
		String environmentResult = timeoutResult.replace("$environment", "");
		String finalResult = environmentResult.replace("$filter", filter.toString());

		// Override OpenSSH config logic has been removed
		// It is expected that the caller would provide a property to override the absolute path to the environment OpenSSH config file
		// e.g. puppet.openssl.config.filename=/opt/environment-management/config/ssh_config
		return jschManager.runSSHExecWithOutput(sshJumpConfig.getTimeout(), finalResult, sshJumpConfig.getUsername(), sshJumpConfig.getHostname(), sshJumpConfig.getPort(), sshJumpConfig.getJumphosts(), unescapeJava);
	}

	public boolean isMcoEnabled() {
		return isMcoEnabled;
	}

	public void setMcoEnabled(boolean isMcoEnabled) {
		this.isMcoEnabled = isMcoEnabled;
	}

	public String getRunPuppetCommand() {
		return runPuppetCommand;
	}

	public void setRunPuppetCommand(String runPuppetCommand) {
		this.runPuppetCommand = runPuppetCommand;
	}

	public String getUpdatePuppetCommand() {
		return updatePuppetCommand;
	}

	public void setUpdatePuppetCommand(String updatePuppetCommand) {
		this.updatePuppetCommand = updatePuppetCommand;
	}

	public String getPuppetRunRpcCommand() {
		return puppetRunRpcCommand;
	}

	/**
	 * Should contain placeholders: $timeout $cmd $environment $filter
	 * @param puppetRunCommand
	 */
	public void setPuppetRunRpcCommand(String puppetRunCommand) {
		this.puppetRunRpcCommand = puppetRunCommand;
	}

	/**
	 * Should contain placeholders: $timeout gonzo $cmd $environment $filter
	 * @param puppetRunCommand
	 */
	public String getPuppetDoPuppetCommand() {
		return puppetDoPuppetCommand;
	}

	public void setPuppetDoPuppetCommand(String puppetDoPuppetCommand) {
		this.puppetDoPuppetCommand = puppetDoPuppetCommand;
	}

	public String getGonzoCommand() {
		return gonzoCommand;
	}

	public void setGonzoCommand(String gonzoCommand) {
		this.gonzoCommand = gonzoCommand;
	}
}
