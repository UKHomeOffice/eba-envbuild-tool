/**
 * 
 */
package com.ipt.ebsa.manage.puppet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.ssh.JschManager;

/**
 * @author James Shepherd
 */
public class SshManager extends JschManager {
	
	public SshManager() {
		super(getKnownHostsIS(), Configuration.getOpenSSHIdentityFile(), Configuration.getOpenSSHConfig(), Configuration.getOpenSSHUsePty(), Configuration.getOpenSSHLoggingLevels());
	}
	
	private static InputStream getKnownHostsIS() {
		try {
			return new FileInputStream(Configuration.getOpenSSHKnownHosts());
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Failed to open known hosts file", e);
		}
	}
}
