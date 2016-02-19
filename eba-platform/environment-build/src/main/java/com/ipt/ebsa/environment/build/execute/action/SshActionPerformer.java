package com.ipt.ebsa.environment.build.execute.action;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.build.Configuration;
import com.ipt.ebsa.environment.data.model.SshAction;
import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.ssh.JschManager;
import com.ipt.ebsa.ssh.SshJumpConfig;

public class SshActionPerformer extends ActionPerformer {

	private static final Logger LOG = Logger.getLogger(SshActionPerformer.class);
	
	private SshAction action;
	private JschManager jschManager;
	
	public SshActionPerformer(SshAction action, JschManager jschManager) {
		this.action = action;
		this.jschManager = jschManager;
	}
	
	@Override
	public SshAction getAction() {
		return action;
	}
	
	@Override
	protected ExecReturn doExecute() {
		SshActionContext actionContext = getActionContext();
		SshJumpConfig sshJumpConfig = new SshJumpConfig(actionContext.getMachine());
		sshJumpConfig.setTimeout(Configuration.getSshTimeout());
		
		if (StringUtils.isNotBlank(actionContext.getJumpHosts())) {
			sshJumpConfig.addJumphosts(actionContext.getJumpHosts());
		}
		
		if (StringUtils.isNotBlank(actionContext.getSshOptsFile())) {
			jschManager.setOpenSshConfigFile(new File(actionContext.getSshOptsFile()));
		}

		String command = actionContext.getCommand();
		LOG.info(sshJumpConfig + " command=" + command);
		
		if (Configuration.isSshActionEnabled()) {
			return jschManager.runSSHExecWithOutput(sshJumpConfig.getTimeout(), command, sshJumpConfig.getUsername(), sshJumpConfig.getHostname(), sshJumpConfig.getPort(), sshJumpConfig.getJumphosts(), true);
		} else {
			LOG.info("ssh command disabled");
			return new ExecReturn(0);
		}
	}

	@Override
	public SshActionContext getActionContext() {
		return new SshActionContext();
	}
	
	public class SshActionContext extends ActionContext {

		public String getMachine() {
			return getBuildContext().substituteParams(getAction().getMachine());
		}
		
		public String getJumpHosts() {
			return getBuildContext().substituteParams(getAction().getJumpHosts());
		}
		
		public String getSshOptsFile() {
			return getBuildContext().substituteParams(getAction().getSshOptsFile());
		}
		
		public String getCommand() {
			return getBuildContext().substituteParams(getAction().getCommand());
		}
		
		@Override
		public Map<String, String> getActionContextMap() {
			TreeMap<String, String> map = new TreeMap<>();
			map.put("machine", getMachine());
			map.put("jumphosts", getJumpHosts());
			map.put("sshoptsfile", getSshOptsFile());
			map.put("command", getCommand());
			return map;
		}
	}

	@Override
	public String getActionDisplayName() {
		return "SSH Action";
	}
}
