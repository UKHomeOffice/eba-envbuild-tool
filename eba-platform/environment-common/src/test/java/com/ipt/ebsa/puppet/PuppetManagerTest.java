package com.ipt.ebsa.puppet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.ssh.HostnameUsernamePort;
import com.ipt.ebsa.ssh.JschManager;
import com.ipt.ebsa.ssh.JschManagerTest;
import com.ipt.ebsa.ssh.SshJumpConfig;
import com.ipt.ebsa.ssh.testhelper.CommandRegister;
import com.ipt.ebsa.ssh.testhelper.EbsaCommandFactory;

public class PuppetManagerTest {

	/** 
	 * Some of the unit tests don't seem to work on some versions of Windows. Given the tests take time AND
	 * the deployments will be via a Linux box, we'll skip them on Windows.
	 */
	private static final Boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
	
	CommandRegister cr;
	SshServer sshd;
	int port;
	PuppetManager puppetManager;
	SshJumpConfig sshJumpConfig;
	
	
	@Before
	public void setUp() throws Exception {
		if (!IS_WINDOWS) {
	
			cr = new CommandRegister();
			sshd = SshServer.setUpDefaultServer();
			port = getPort();
			sshd.setPort(port);
			sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("src/test/resources/sshd/hostkey.ser"));
			sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
				@Override
				public boolean authenticate(String username, String password, ServerSession session) {
					return true;
				}
			});
			sshd.setCommandFactory(new EbsaCommandFactory(cr));
			sshd.start();
			
			JschManager jsch = JschManagerTest.getJschManager();
			puppetManager = new PuppetManager(jsch);
			puppetManager.setMcoEnabled(true);
		}
		
		sshJumpConfig = new SshJumpConfig();
		sshJumpConfig.setPort(port);
		sshJumpConfig.setHostname("localhost");
		sshJumpConfig.setUsername("UncleBuck");
	}

	/**
	 * @return a port that we know is available
	 * @throws IOException
	 */
	private int getPort() throws IOException {
		ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();
		return port;
	}
	
	@After
	public void close() throws InterruptedException {
		if (!IS_WINDOWS) {
			sshd.stop(true);
		}
	}
	
	@Test
	public void testUpdatePuppet() {
		assumeFalse(IS_WINDOWS);
		puppetManager.updatePuppetMaster(sshJumpConfig);
		Assert.assertEquals(PuppetManager.DEFAULT_UPDATE_PUPPET_COMMAND, cr.getLastCommand());
	}
	
	@Test
	public void testPuppetRun() {
		assumeFalse(IS_WINDOWS);
		puppetManager.doPuppetRun(sshJumpConfig, "np-prp1-cobc.ipt.ho.local", "tst");
		Assert.assertEquals("sudo -u peadmin /opt/puppet/bin/mco rpc -t 30000 gonzo run  -S \"(domain=np-prp1-cobc.ipt.ho.local and (role=tst))\"", cr.getLastCommand());
	}
	
	@Test
	public void testMcoCommand() {
		assumeFalse(IS_WINDOWS);
		puppetManager.doMCollectiveOperation(sshJumpConfig, "np-prp1-cobc.ipt.ho.local", "tst", "twist_and_shout");
		Assert.assertEquals("sudo -u peadmin /opt/puppet/bin/mco rpc -t 30000 twist_and_shout  -S \"(domain=np-prp1-cobc.ipt.ho.local and (role=tst))\"", cr.getLastCommand());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMcoCommandMockitoNullRoleHost() {
		JschManager jm = mock(JschManager.class);
		PuppetManager pm = new PuppetManager(jm);		
		when(jm.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean())).thenReturn(new ExecReturn(0));
		
		pm.doMCollectiveOperationWithOutput(sshJumpConfig, "st-sst1-app1.ipt.local", null, "service status service=weblogic-app");
		
		ArgumentCaptor<Integer> jschTimeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> jschCommandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschUsernameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschHostCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> jschPortCaptor = ArgumentCaptor.forClass(Integer.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> jschJumpHostsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Boolean> jschUnescapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		
		verify(jm, times(1)).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		assertEquals("sudo -u peadmin /opt/puppet/bin/mco rpc -t 30000 service status service=weblogic-app  -S \"(domain=st-sst1-app1.ipt.local)\"", jschCommandCaptor.getValue());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMcoCommandMockitoEmptyRoleHost() {
		JschManager jm = mock(JschManager.class);
		PuppetManager pm = new PuppetManager(jm);		
		when(jm.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean())).thenReturn(new ExecReturn(0));
		
		pm.doMCollectiveOperationWithOutput(sshJumpConfig, "st-sst1-app1.ipt.local", "", "service status service=weblogic-app");
		
		ArgumentCaptor<Integer> jschTimeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> jschCommandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschUsernameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschHostCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> jschPortCaptor = ArgumentCaptor.forClass(Integer.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> jschJumpHostsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Boolean> jschUnescapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		
		verify(jm, times(1)).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		assertEquals("sudo -u peadmin /opt/puppet/bin/mco rpc -t 30000 service status service=weblogic-app  -S \"(domain=st-sst1-app1.ipt.local)\"", jschCommandCaptor.getValue());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMcoCommandMockitoOneRoleHost() {
		JschManager jm = mock(JschManager.class);
		PuppetManager pm = new PuppetManager(jm);		
		when(jm.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean())).thenReturn(new ExecReturn(0));
		
		pm.doMCollectiveOperationWithOutput(sshJumpConfig, "st-sst1-app1.ipt.local", "soatzm01", "service status service=weblogic-app");
		
		ArgumentCaptor<Integer> jschTimeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> jschCommandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschUsernameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschHostCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> jschPortCaptor = ArgumentCaptor.forClass(Integer.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> jschJumpHostsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Boolean> jschUnescapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		
		verify(jm, times(1)).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		assertEquals("sudo -u peadmin /opt/puppet/bin/mco rpc -t 30000 service status service=weblogic-app  -S \"(domain=st-sst1-app1.ipt.local and (fqdn=soatzm01.st-sst1-app1.ipt.local))\"", jschCommandCaptor.getValue());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMcoCommandMockitoMutlipleRoleHost() {
		JschManager jm = mock(JschManager.class);
		PuppetManager pm = new PuppetManager(jm);		
		when(jm.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean())).thenReturn(new ExecReturn(0));
		
		pm.doMCollectiveOperationWithOutput(sshJumpConfig, "st-sst1-app1.ipt.local", "soatzm01,soa,soatzm02", "service status service=weblogic-app");
		
		ArgumentCaptor<Integer> jschTimeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> jschCommandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschUsernameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschHostCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> jschPortCaptor = ArgumentCaptor.forClass(Integer.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> jschJumpHostsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Boolean> jschUnescapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		
		verify(jm, times(1)).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		assertEquals("sudo -u peadmin /opt/puppet/bin/mco rpc -t 30000 service status service=weblogic-app  -S \"(domain=st-sst1-app1.ipt.local and (role=soa or fqdn=soatzm01.st-sst1-app1.ipt.local or fqdn=soatzm02.st-sst1-app1.ipt.local))\"", jschCommandCaptor.getValue());
	}
	
	@Test
	public void getRoleOrFQDNParamStringFromYamlTest() {
		assertEquals("fqdn=soatzm01.st-dev1-ebs2.ipt.local", PuppetManager.getRoleOrFQDNParamString("soatzm01", "st-dev1-ebs2.ipt.local"));
		assertEquals("role=cdp", PuppetManager.getRoleOrFQDNParamString("cdp", "st-dev1-ebs2.ipt.local"));
	}
	
	@Test(expected=RuntimeException.class)
	public void getRoleOrFQDNParamStringFromYamlTestException() {
		// Invalid YAML file is length < 3
		PuppetManager.getRoleOrFQDNParamString("ab", "");
	}
	
	@Test
	public void testPuppetRetrySuccess() throws IOException {
		ExecReturn errorReturn = new ExecReturn(2);
		errorReturn.setStdOut(loadTestLog("locked.txt"));
		
		JschManager jsch = mock(JschManager.class);
		PuppetManager myPuppetManager = new PuppetManager(jsch);
		myPuppetManager.setMcoEnabled(true);
		when(jsch.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean()))
			.thenReturn(errorReturn)
			.thenReturn(new ExecReturn(0));
		
		int returnCode = myPuppetManager.doPuppetRunWithRetry(sshJumpConfig, "np-prp1-cobc.ipt.ho.local", "tst", 2, 2);
		assertEquals(0, returnCode);
	}
	
	@Test
	public void testPuppetRetryFail() throws IOException {
		ExecReturn errorReturn = new ExecReturn(2);
		errorReturn.setStdOut(loadTestLog("locked.txt"));
		
		JschManager jsch = mock(JschManager.class);
		PuppetManager myPuppetManager = new PuppetManager(jsch);
		myPuppetManager.setMcoEnabled(true);
		when(jsch.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean()))
			.thenReturn(errorReturn);
		
		int returnCode = myPuppetManager.doPuppetRunWithRetry(sshJumpConfig, "np-prp1-cobc.ipt.ho.local", "tst", 2, 2);
		assertEquals(2, returnCode);
	}
	
	@Test
	public void testPuppetNoRetryFail() throws IOException {
		ExecReturn errorReturn = new ExecReturn(2);
		errorReturn.setStdOut(loadTestLog("locked.txt"));
		
		JschManager jsch = mock(JschManager.class);
		PuppetManager myPuppetManager = new PuppetManager(jsch);
		myPuppetManager.setMcoEnabled(true);
		when(jsch.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean()))
			.thenReturn(errorReturn)
			.thenReturn(new ExecReturn(3)); 
				
		int returnCode = myPuppetManager.doPuppetRunWithRetry(sshJumpConfig, "np-prp1-cobc.ipt.ho.local", "tst", 2, 2);
		assertEquals(3, returnCode);
	}
	
	private String loadTestLog(String logFileName) throws IOException{
		String logsBaseFolder = "src/test/resources/puppet-logs/";
		byte[] fileContents = Files.readAllBytes(Paths.get(logsBaseFolder, logFileName));
		return new String(fileContents);
	}
}
