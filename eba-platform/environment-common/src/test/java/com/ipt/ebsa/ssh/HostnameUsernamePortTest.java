package com.ipt.ebsa.ssh;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.ssh.HostnameUsernamePort;

/**
 * @author James Shepherd
 *
 */
public class HostnameUsernamePortTest {
	
	@Test
	public void testHostname() {
		HostnameUsernamePort hup = new HostnameUsernamePort("mega.example.com", "misspiggy");
		
		Assert.assertEquals("mega.example.com", hup.getHostname());
		Assert.assertEquals("misspiggy", hup.getUsername());
		Assert.assertEquals(22, hup.getPort());
	}
	
	@Test
	public void testHostnamePort() {
		HostnameUsernamePort hup = new HostnameUsernamePort("mega.example.com:2222", "misspiggy");
		
		Assert.assertEquals("mega.example.com", hup.getHostname());
		Assert.assertEquals("misspiggy", hup.getUsername());
		Assert.assertEquals(2222, hup.getPort());
	}
	
	@Test
	public void testHostnameUsername() {
		HostnameUsernamePort hup = new HostnameUsernamePort("chief@mega.example.com", "misspiggy");
		
		Assert.assertEquals("mega.example.com", hup.getHostname());
		Assert.assertEquals("chief", hup.getUsername());
		Assert.assertEquals(22, hup.getPort());
	}
	
	@Test
	public void testHostnameUsernamePort() {
		HostnameUsernamePort hup = new HostnameUsernamePort("burrito@mega.example.com:2222", "misspiggy");
		
		Assert.assertEquals("mega.example.com", hup.getHostname());
		Assert.assertEquals("burrito", hup.getUsername());
		Assert.assertEquals(2222, hup.getPort());
	}
	
	@Test
	public void testOtherConstructor() {
		HostnameUsernamePort hup = new HostnameUsernamePort("burrito@mega.example.com:2222");
		Assert.assertEquals("mega.example.com", hup.getHostname());
		Assert.assertEquals("burrito", hup.getUsername());
		Assert.assertEquals(2222, hup.getPort());
		
		hup = new HostnameUsernamePort("mega.example.com:2222");
		Assert.assertEquals("mega.example.com", hup.getHostname());
		Assert.assertEquals("", hup.getUsername());
		Assert.assertEquals(2222, hup.getPort());
	}
	
	@Test
	public void sshJumpConfigTest() {
		SshJumpConfig s = new SshJumpConfig("user1@host1.com");
		s.addJumphosts(" user2@mega.com:33 ,user3@mindless.com");
		
		Assert.assertEquals("user1", s.getUsername());
		Assert.assertEquals("host1.com", s.getHostname());
		Assert.assertEquals(22, s.getPort());
		Assert.assertEquals("user2", s.getJumphosts().get(0).getUsername());
		Assert.assertEquals("mega.com", s.getJumphosts().get(0).getHostname());
		Assert.assertEquals(33, s.getJumphosts().get(0).getPort());
		Assert.assertEquals("user3", s.getJumphosts().get(1).getUsername());
		Assert.assertEquals("mindless.com", s.getJumphosts().get(1).getHostname());
		Assert.assertEquals(22, s.getJumphosts().get(1).getPort());
	}
}
