package com.ipt.ebsa.manage.transitions;

import java.util.Collection;

import com.ipt.ebsa.deployment.descriptor.ResolvedHost;

public class MCOCommand {
	
	private final String command;
	private final Collection<ResolvedHost> hosts;
	private final String applicationShortName;
	
	public MCOCommand(String command, Collection<ResolvedHost> hosts,
			String applicationShortName) {
		this.command = command;
		this.hosts = hosts;
		this.applicationShortName = applicationShortName;
	}
	
	public String getCommand() {
		return command;
	}
	
	public Collection<ResolvedHost> getHosts() {
		return hosts;
	}

	public String getApplicationShortName() {
		return applicationShortName;
	}
	
}
