package com.ipt.ebsa.ssh;


/**
 * Holds part of what we need to connect to an SSH host.
 * @author James Shepherd
 */
public class HostnameUsernamePort {
	
	private static final int DEFAULT_SSH_PORT = 22;
	private String hostname;
	private String username;
	private int port = DEFAULT_SSH_PORT;
	
	public HostnameUsernamePort(){
		// default constructor
	}
	
	/**
	 * e.g. user@host.com:22
	 * Hostname is required, others are not needed
	 * @param spec user host port spec
	 * @param defaultUsername username to use if none specified in spec
	 * @throws NumberFormatException if spec includes a port and it isn't an int
	 */
	public HostnameUsernamePort(String spec, String defaultUsername) {
		int indexOfAt = spec.indexOf('@');
		String hostPort;
		
		if (indexOfAt == -1) {
			setUsername(defaultUsername.trim());
			hostPort = spec;
		} else {
			setUsername(spec.substring(0, indexOfAt).trim());
			hostPort = spec.substring(indexOfAt + 1);
		}
		
		int indexOfColon = hostPort.indexOf(':');
		
		if (indexOfColon == -1) {
			setHostname(hostPort.trim());
		} else {
	        setHostname(hostPort.substring(0, indexOfColon).trim());
	        setPort(Integer.parseInt(hostPort.substring(indexOfColon + 1).trim()));
		}
	}

	/**
	 * e.g. user@host.com:22
	 * Hostname and username is required, port optional. If username is not
	 * specified, then this will return an empty string as the username.
	 * @param spec user host port spec
	 * @throws NumberFormatException if spec includes a port and it isn't an int
	 */
	public HostnameUsernamePort(String spec) {
		this(spec, "");
	}
	
	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Defaults to {@link Configuration.DEFAULT_SSH_PORT} if none is set 
	 * @return
	 */
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	@Override
	public String toString() {
		return String.format("%s@%s:%d", getUsername(), getHostname(), getPort());
	}
}
