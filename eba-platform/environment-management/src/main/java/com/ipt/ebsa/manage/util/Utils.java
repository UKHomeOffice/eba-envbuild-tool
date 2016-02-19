package com.ipt.ebsa.manage.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.ssh.HostnameUsernamePort;

public class Utils {
	
	public static List<HostnameUsernamePort> getYumRepoJumpHost(Organisation organisation, String defaultUsername) {
		return makeHostnameUsernamePortObjects(defaultUsername, Configuration.getYumRepoJumpHosts(organisation));
	}
	
	public static File getYumRepoUpdateDir(Organisation organisation) {
		return new File(Configuration.getYumRepoDir(organisation));
	}
	
	public static List<HostnameUsernamePort> getJumphostsForPuppetMaster(Organisation organisation, String defaultUsername) {
		return makeHostnameUsernamePortObjects(defaultUsername, Configuration.getPuppetMasterJumphosts(organisation));
	}

	private static List<HostnameUsernamePort> makeHostnameUsernamePortObjects(String defaultUsername, String hosts) {
		ArrayList<HostnameUsernamePort> output = new ArrayList<>();
		
		if (StringUtils.isNotBlank(hosts)) {
			for(String spec : hosts.split(",")) {
				output.add(new HostnameUsernamePort(spec, defaultUsername));
			}
		}
		
		return output;
	}
}
