package com.ipt.ebsa.manage.deploy.rpmfailfile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.mco.MCOUtils;
import com.ipt.ebsa.manage.puppet.EMPuppetManager;
import com.ipt.ebsa.manage.test.TestHelper;
import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.ssh.HostnameUsernamePort;
import com.ipt.ebsa.ssh.JschManager;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Some tests for the JSON parsing. Some tests for verifying the SSH commands that are run on ST and NP.
 * Before the first test the config is reset and then initialised to a config that reflects the real-world.
 * After the last test the config is reset again so that subsequent tests are not affected.
 * @author Dan McCarthy
 *
 */
public class RPMFailFileManagerTest {
	
	@BeforeClass
	public static void beforeFirstTest() throws FileNotFoundException, IOException {
		resetConfig();
		
		// Set props before we start
		TestHelper.mergeProperties("src/test/resources/rpmfailfiles/config_v2.properties");
		ConfigurationFactory.getProperties().put(Configuration.ENABLE_MCO, "true");
	}
	
	@AfterClass
	public static void afterLastTest() {
		resetConfig();
	}
	
	private static void resetConfig() {
		try {
			Method clearConfig = ConfigurationFactory.class.getDeclaredMethod("reset");
			clearConfig.setAccessible(true);
			clearConfig.invoke(null, (Object[]) null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to reset properties", e);
		}
	}
	
	@Test
	public void parseJsonTestWithFailFile() {
		String json = "[{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"etctzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"tzftzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"dbstzm02.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"wpxdzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"tsttzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"schtzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"dbstzm11.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"doctzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":1,\"err\":\"\",\"out\":\"Failed RPM File exists\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"soatzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"etltzm02.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"ldptzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"dzfdzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"reptzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"mzfmzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"etltzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"soatzm02.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"tstdzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"dbstzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}}]";
		
		Map<String, String> failFilesByFQDN = MCOUtils.parseJson(json);
		assertEquals(18, failFilesByFQDN.size());
		assertTrue(failFilesByFQDN.containsKey("doctzm01.st-sit1-cor1.ipt.local"));
		for (Entry<String, String> entry : failFilesByFQDN.entrySet()) {
			assertTrue(entry.getKey().endsWith(".st-sit1-cor1.ipt.local"));
			if (entry.getKey().equals("doctzm01.st-sit1-cor1.ipt.local")) {
				assertEquals("Failed RPM File exists", entry.getValue());
			} else {
				assertEquals("", entry.getValue());
			}
		}
	}
	
	@Test
	public void parseJsonTest() {
		String json = "[{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"etctzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"tzftzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"dbstzm02.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"wpxdzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"tsttzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"schtzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"dbstzm11.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"doctzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":1,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"soatzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"etltzm02.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"ldptzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"dzfdzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"reptzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"mzfmzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"etltzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"soatzm02.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"tstdzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}},"
				+ "{\"agent\":\"failedfiles\",\"action\":\"report\",\"sender\":\"dbstzm01.st-sit1-cor1.ipt.local\",\"statuscode\":0,\"statusmsg\":\"OK\",\"data\":{\"output\":0,\"err\":\"\",\"out\":\"\"}}]";
		
		Map<String, String> failFilesByFQDN = MCOUtils.parseJson(json);
		assertEquals(18, failFilesByFQDN.size());
		for (Entry<String, String> entry : failFilesByFQDN.entrySet()) {
			assertTrue(entry.getKey().endsWith(".st-sit1-cor1.ipt.local"));
			assertEquals("", entry.getValue());
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void doMCOperationTestST() {
		JschManager jsch = mock(JschManager.class);
		EMPuppetManager puppet = new EMPuppetManager(jsch);
		when(jsch.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean()))
			.thenReturn(new ExecReturn(0));
		
		ExecReturn output = new RPMFailFileManager().doMCOperation(new Organisation("st"), "IPT_ST_SIT1_COR1", puppet);
		assertNotNull(output);
		assertEquals(0, output.getReturnCode());
		
		// Assert that the Puppet method was called once with the expected args 
		ArgumentCaptor<Integer> timeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> hostCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> portCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<List> jumpHostsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Boolean> jschUnescapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		verify(jsch).runSSHExecWithOutput(timeoutCaptor.capture(), commandCaptor.capture(), usernameCaptor.capture(), hostCaptor.capture(), portCaptor.capture(), (List<HostnameUsernamePort>) jumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		assertEquals("sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 failedfiles report -j  -S \"(domain=st-sit1-cor1.ipt.local)\" --verbose", commandCaptor.getValue());
		assertEquals(new Integer(3000000), timeoutCaptor.getValue());
		assertEquals("puppetuser", usernameCaptor.getValue());
		assertEquals("masterofpuppets.st-tooling1.ipt.local", hostCaptor.getValue());
		assertEquals(new Integer(22), portCaptor.getValue());
		assertEquals(0, jumpHostsCaptor.getValue().size());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void doMCOperationTestNP() {
		JschManager jsch = mock(JschManager.class);
		EMPuppetManager puppet = new EMPuppetManager(jsch);
		when(jsch.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean()))
			.thenReturn(new ExecReturn(0));
		
		ExecReturn output = new RPMFailFileManager().doMCOperation(new Organisation("np"), "HO_IPT_NP_PRP2_PRZO", puppet);
		assertNotNull(output);
		assertEquals(0, output.getReturnCode());
		
		// Assert that the Puppet method was called once with the expected args 
		ArgumentCaptor<Integer> timeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> hostCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> portCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<List> jumpHostsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Boolean> jschUnescapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		verify(jsch).runSSHExecWithOutput(timeoutCaptor.capture(), commandCaptor.capture(), usernameCaptor.capture(), hostCaptor.capture(), portCaptor.capture(), (List<HostnameUsernamePort>) jumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		assertEquals("sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 failedfiles report -j  -S \"(domain=np-prp2-przo.ipt.ho.local)\" --verbose", commandCaptor.getValue());
		assertEquals(new Integer(3000000), timeoutCaptor.getValue());
		assertEquals("puppetuser", usernameCaptor.getValue());
		assertEquals("masterofpuppets.np-tlg1-mtzo.ipt.ho.local", hostCaptor.getValue());
		assertEquals(new Integer(22), portCaptor.getValue());
		assertEquals(2, jumpHostsCaptor.getValue().size());
		List<HostnameUsernamePort> jumps = jumpHostsCaptor.getValue();
		assertEquals("jump1host", jumps.get(0).getHostname());
		assertEquals("jump1user", jumps.get(0).getUsername());
		assertEquals(22, jumps.get(0).getPort());
		assertEquals("jump2host", jumps.get(1).getHostname());
		assertEquals("jump2user", jumps.get(1).getUsername());
		assertEquals(22, jumps.get(1).getPort());
	}
	
}
