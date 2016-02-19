package com.ipt.ebsa.manage.puppet;

import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.test.TestHelper;
import com.ipt.ebsa.ssh.testhelper.CommandRegister;
import com.ipt.ebsa.ssh.testhelper.EbsaCommandFactory;

public class EMPuppetTest {
	/** 
	 * Some of the unit tests don't seem to work on some versions of Windows. Given the tests take time AND
	 * the deployments will be via a Linux box, we'll skip them on Windows.
	 */
	private static final Boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
	
	CommandRegister cr;
	SshServer sshd;
	int port;
	
	
	@Before
	public void setUp() throws Exception {
		if (IS_WINDOWS){
			return;
		}

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
		
		TestHelper.setupTestConfig();
		ConfigurationFactory.getProperties().put("st.puppet.master.host", "localhost");
		ConfigurationFactory.getProperties().put("np.puppet.master.host", "localhost");
		ConfigurationFactory.getProperties().put("puppet.master.update.login.port", String.valueOf(port));
		ConfigurationFactory.getProperties().put(Configuration.ENABLE_MCO, "true");
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
		if (IS_WINDOWS) {
			return;
		}
		sshd.stop(true);
	}
	
	@Test
	public void testUpdatePuppet() {
		assumeFalse(IS_WINDOWS);
		EMPuppetManager pm = new EMPuppetManager(new SshManager());
		Organisation organisation = ConfigurationFactory.getOrganisations().get("st");
		pm.updatePuppetMaster(organisation);
		Assert.assertEquals(Configuration.getPuppetMasterUpdateCommand(organisation), cr.getLastCommand());
	}
	
	@Test
	public void testPuppetRun() {
		assumeFalse(IS_WINDOWS);
		EMPuppetManager pm = new EMPuppetManager(new SshManager());
		Organisation organisation = ConfigurationFactory.getOrganisations().get("np");
		pm.doPuppetRunWithRetry(organisation, "HO_IPT_NP_PRP1_COBC", "tst", 0, 0);
		Assert.assertEquals("sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp1-cobc.ipt.ho.local and (role=tst))\"", cr.getLastCommand());
	}
}
