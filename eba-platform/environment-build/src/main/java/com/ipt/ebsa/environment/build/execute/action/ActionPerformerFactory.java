package com.ipt.ebsa.environment.build.execute.action;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.environment.build.Configuration;
import com.ipt.ebsa.environment.build.execute.BuildContext;
import com.ipt.ebsa.environment.data.model.FirewallHieraAction;
import com.ipt.ebsa.environment.data.model.InfraAction;
import com.ipt.ebsa.environment.data.model.InternalHieraAction;
import com.ipt.ebsa.environment.data.model.ParameterisedNode;
import com.ipt.ebsa.environment.data.model.SshAction;
import com.ipt.ebsa.ssh.JschManager;

public class ActionPerformerFactory {
	
	private static final Logger LOG = Logger.getLogger(ActionPerformerFactory.class);
	
	private JschManager jschManager = null;
	
	public ActionPerformer build(ParameterisedNode thisNode, BuildContext buildContext) {
		ActionPerformer performer = null;
		if (thisNode instanceof SshAction) {
			LOG.debug("SshAction found: " + thisNode.getId());
			performer = new SshActionPerformer((SshAction) thisNode, getJschManager());
		} else if (thisNode instanceof InfraAction) {
			LOG.debug("InfraAction found: " + thisNode.getId());
			String provider = buildContext.getProvider();
			if (XMLProviderType.SKYSCAPE.toString().equals(provider)) {
				performer = new SkyscapeInfraActionPerformer((InfraAction) thisNode);
			} else if (XMLProviderType.AWS.toString().equals(provider)) {
				performer = new AwsInfraActionPerformer((InfraAction) thisNode);
			} else {
				throw new RuntimeException("InfraActionPerformer not implemented for provider: " + provider + " for node: " + thisNode.getId());
			}
		} else if (thisNode instanceof InternalHieraAction) {
			LOG.debug("InternalHieraAction found: " + thisNode.getId());
			performer = new InternalHieraActionPerformer((InternalHieraAction) thisNode);
		} else if (thisNode instanceof FirewallHieraAction) {
			LOG.debug("FirewallHieraAction found: " + thisNode.getId());
			performer = new FirewallHieraActionPerformer((FirewallHieraAction) thisNode);
		}
		
		if (null == performer) {
			throw new RuntimeException("Leaf node not a recognised Action: " + thisNode.getId());
		}

		return performer;
	}
	
	public synchronized JschManager getJschManager() {
		if (null == jschManager) {
			InputStream knownHostsIS;
			try {
				knownHostsIS = new FileInputStream(Configuration.getSshKnownhosts());
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Failed to open known hosts file: " + Configuration.getSshKnownhosts());
			}
			jschManager = new JschManager(knownHostsIS, Configuration.getSshIdentity(), Configuration.getSshConfig(), true);
		}
		
		return jschManager;
	}
}
