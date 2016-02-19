package com.ipt.ebsa.ssh;

import java.util.ArrayList;
import java.util.List;

public class SshJumpConfig extends HostnameUsernamePort {

	public static final int DEFAULT_TIMEOUT = 30000;
	
	private int timeout = DEFAULT_TIMEOUT;
	private List<HostnameUsernamePort> jumphosts = new ArrayList<>();
	
	/**
	 * {@see HostnameUsernamePort()}
	 */
	public SshJumpConfig(){
		super();
	}
	
	/**
	 * {@see HostnameUsernamePort(String)}
	 * @param spec
	 */
	public SshJumpConfig(String spec) {
		super(spec);
	}
	
	/**
	 * @return the timeout in milliseconds
	 */
	public int getTimeout() {
		return timeout;
	}
	
	/**
	 * @param timeout the timeout to set in milliseconds
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	/**
	 * @return the jumphosts
	 */
	public List<HostnameUsernamePort> getJumphosts() {
		return jumphosts;
	}
	
	/**
	 * @param jumpHosts the jumpHosts to set
	 */
	public void setJumphosts(List<HostnameUsernamePort> jumphosts) {
		this.jumphosts = jumphosts;
	}
	
	/**
	 * @param csJumphosts comma separated list of jumphosts, e.g. user1@host2.com,user3@host1.com:2222
	 */
	public void addJumphosts(String csJumphosts) {
		for (String spec : csJumphosts.split(",")) {
			jumphosts.add(new HostnameUsernamePort(spec));
		}
	}
	
	@Override
	public String toString() {
		return String.format("host[%s] timeout[%s] jumphosts[%s]", super.toString(), getTimeout(), getJumphosts());  
	}
}
