package com.ipt.ebsa.manage.puppet;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.EnvironmentManagementCLI;
import com.ipt.ebsa.manage.deploy.impl.Change;
import com.ipt.ebsa.manage.deploy.impl.ChangeSet;
import com.ipt.ebsa.manage.deploy.impl.ChangeType;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.deploy.impl.JitYumUpdateManager;
import com.ipt.ebsa.manage.test.TestHelper;
import com.ipt.ebsa.ssh.testhelper.CommandRegister;
import com.ipt.ebsa.ssh.testhelper.EbsaCommandFactory;
import com.ipt.ebsa.ssh.testhelper.EbsaLsCommandFactory;

public class SshTest {

	private static final Logger	LOG = LogManager.getLogger(SshTest.class);
	
	private List<SshServer> sshServers = new ArrayList<>();
	private File tmpDir = null;

	/** 
	 * Some of the unit tests don't seem to work on some versions of Windows. Given the tests take time AND
	 * the deployments will be via a Linux box, we'll skip them on Windows.
	 */
	private static final Boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
	
	@Before
	public void setUp() throws FileNotFoundException, IOException {
		TestHelper.setupTestConfig();
		ConfigurationFactory.getProperties().put("st.yum.repo.update.enabled", "true");
	}

	@After
	public void tearDownSsh() throws InterruptedException, IOException {
		for(SshServer sshd : sshServers) {
			stopSSH(sshd);
		}
		
		if (tmpDir != null && tmpDir.exists()) {
			FileUtils.deleteDirectory(tmpDir);
		}
	}
	
	/**
	 * Tests case when createrepo-q-tool succeeds with failure log
	 * @throws Exception
	 */
	@Test
	public void testJitYumFailed() throws Exception {
		final JitYumUpdateManager yumManager = createYumManager();
		
		assumeFalse(IS_WINDOWS);
		tmpDir = org.eclipse.jgit.util.FileUtils.createTempDir("ls-", "-test", null);
		final File uploadDir = new File(tmpDir, "upload");
		Assert.assertTrue(uploadDir.mkdir());
		final File qDir = new File(tmpDir, "createrepo-q-tool");
		Assert.assertTrue(qDir.mkdir());
		final File qDirInbox = new File(qDir, "batch_inbox");
		Assert.assertTrue(qDirInbox.mkdir());
		final File qDirOutbox = new File(qDir, "batch_outbox");
		Assert.assertTrue(qDirOutbox.mkdir());
		Assert.assertEquals("no of files in dir", 0, qDirInbox.list().length);
		final String message = "Lenny, you will have saved the lives of millions of registered voters";
		
		int port = startLsScpSshServer();
		
		configureForTestCreateRepoQ(uploadDir, qDir, port);

		Thread backgroundThread = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					LOG.error("Interrupted", e);
				}
				String[] ls = qDirInbox.list();
				
				if (ls.length > 0) {
					String mailName = ls[0];
					LOG.info("Found file " + mailName);
					
					/**
					 * suffix comes from createrepo-q-tool
					 */
					File response = new File(qDirOutbox, mailName + "_failed.log");
					try {
						FileUtils.write(response, message);
					} catch (IOException e) {
						LOG.error("Failed to make response", e);
					}
				} else {
					LOG.error("Failed to find file in inbox");
				}
			}
		});
		
		backgroundThread.start();
		
		try {
			yumManager.doJitYumRepoUpdate(ConfigurationFactory.getOrganisations().get("st"));
			fail();
		} catch (RuntimeException e) {
			Assert.assertEquals("createrepo-q-tool failed", "createrpo-q-tool failed", e.getMessage());
		}

		
		backgroundThread.join(10000);
		
		Assert.assertEquals("no of files in inbox", 1, qDirInbox.list().length);
		Assert.assertEquals("no of files in upload", 1, uploadDir.list().length);
		Assert.assertEquals("no of files in outbox", 1, qDirOutbox.list().length);
		
		Assert.assertEquals("log", message, yumManager.getCreaterepoQToolLog());
	}
	
	/**
	 * Tests case when createrepo-q-tool succeeds with succes log
	 * @throws Exception
	 */
	@Test
	public void testJitYumSuccess() throws Exception {
		final JitYumUpdateManager yumManager = createYumManager();
		
		assumeFalse(IS_WINDOWS);
		
		tmpDir = org.eclipse.jgit.util.FileUtils.createTempDir("ls-", "-test", null);
		final File uploadDir = new File(tmpDir, "upload");
		Assert.assertTrue(uploadDir.mkdir());
		final File qDir = new File(tmpDir, "createrepo-q-tool");
		Assert.assertTrue(qDir.mkdir());
		final File qDirInbox = new File(qDir, "batch_inbox");
		Assert.assertTrue(qDirInbox.mkdir());
		final File qDirOutbox = new File(qDir, "batch_outbox");
		Assert.assertTrue(qDirOutbox.mkdir());
		Assert.assertEquals("no of files in dir", 0, qDirInbox.list().length);
		final String message = "If somebody asks you if you're a god you say YES!";
		
		int port = startLsScpSshServer();
		
		configureForTestCreateRepoQ(uploadDir, qDir, port);

		Thread backgroundThread = new Thread(new Runnable() {
			public void run() {
				do {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						LOG.error("Interrupted", e);
					}
					
					String[] ls = qDirInbox.list();
					
					if (ls.length > 0) {
						String mailName = ls[0];
						LOG.info("Found file " + mailName);
						
						/**
						 * suffix comes from createrepo-q-tool
						 */
						File response = new File(qDirOutbox, mailName + ".log");
						try {
							FileUtils.write(response, message);
							LOG.info("Writen log file: " + response.getAbsolutePath());
							return;
						} catch (IOException e) {
							LOG.error("Failed to make response", e);
						}
					} else {
						LOG.error("Failed to find file in inbox");
					}
				} while (true);
			}
		});
		
		backgroundThread.start();
		yumManager.doJitYumRepoUpdate(ConfigurationFactory.getOrganisations().get("st"));
		backgroundThread.join(10000);
		
		Assert.assertEquals("no of files in inbox", 1, qDirInbox.list().length);
		Assert.assertEquals("no of files in upload", 1, uploadDir.list().length);
		Assert.assertEquals("no of files in outbox", 1, qDirOutbox.list().length);
		
		Assert.assertEquals("log", message, yumManager.getCreaterepoQToolLog());
	}

	private void configureForTestCreateRepoQ(final File uploadDir, final File qDir, int port) {
		Properties props = ConfigurationFactory.getProperties();
		props.setProperty(Configuration.CREATEREPO_Q_TOOL_DIR, qDir.getPath());
		props.setProperty("st.yum.repo.host", "localhost");
		props.setProperty("st.yum.repo.port", "" + port);
		props.setProperty("st.yum.repo.username", "olga");
		props.setProperty("st.yum.repo.dir", uploadDir.getPath());
		props.setProperty(Configuration.YUM_REPO_UPDATE_POLL_TIMEOUT_SECONDS, "240");
	}
	
	/**
	 * Tests case when createrepo-q-tool fails to respond
	 * @throws Exception
	 */
	@Test
	public void testJitYumCreateQFailure() throws Exception {
		final JitYumUpdateManager yumManager = createYumManager();
		
		assumeFalse(IS_WINDOWS);

		tmpDir = org.eclipse.jgit.util.FileUtils.createTempDir("ls-", "-test", null);
		final File uploadDir = new File(tmpDir, "upload");
		Assert.assertTrue(uploadDir.mkdir());
		final File qDir = new File(tmpDir, "createrepo-q-tool");
		Assert.assertTrue(qDir.mkdir());
		final File qDirInbox = new File(qDir, "batch_inbox");
		Assert.assertTrue(qDirInbox.mkdir());
		final File qDirOutbox = new File(qDir, "batch_outbox");
		Assert.assertTrue(qDirOutbox.mkdir());
		Assert.assertEquals("no of files in inbox (pre)", 0, qDirInbox.list().length);
		Assert.assertEquals("no of files in upload (pre)", 0, uploadDir.list().length);
		
		int port = startLsScpSshServer();
		
		configureForTestCreateRepoQ(uploadDir, qDir, port);
		ConfigurationFactory.getProperties().setProperty(Configuration.YUM_REPO_UPDATE_POLL_TIMEOUT_SECONDS, "20");

		Exception ex = null;
		try {
			yumManager.doJitYumRepoUpdate(ConfigurationFactory.getOrganisations().get("st"));
		} catch (RuntimeException e) {
			ex = e;
		}
		Assert.assertEquals("Correct exception thrown", "Failed to receive output from createrepo-q-tool - never received log", ex.getMessage());
		
		Assert.assertEquals("no of files in inbox", 1, qDirInbox.list().length);
		Assert.assertEquals("no of files in upload", 1, uploadDir.list().length);
	}

	private JitYumUpdateManager createYumManager() {
		final JitYumUpdateManager yumManager = new JitYumUpdateManager();
		TreeMap<ComponentId, ComponentDeploymentData> components = new TreeMap<>();
		yumManager.setComponents(components);
		
		// create one component with deploy
		ComponentDeploymentData cdd1 = new ComponentDeploymentData("component-one", "APP");
		components.put(cdd1.getComponentId(), cdd1);
		
		ComponentVersion cv1 = new ComponentVersion();
		cv1.setArtifactId("component-one-artifact");
		cv1.setClassifier("classifier");
		cv1.setComponentVersion("1.0.2");
		cv1.setGroupId("group-one");
		cv1.setName("component-one");
		cv1.setPackaging("rpm");
		cv1.setRpmPackageName("component-one-package");
		cv1.setRpmPackageVersion("1.0.2-1");
		cdd1.setTargetComponentVersion(cv1);
		
		ChangeSet cs1 = new ChangeSet();
		Change c1 = new Change(ChangeType.DEPLOY);
		cs1.setPrimaryChange(c1);
		cdd1.getChangeSets().add(cs1);
		
		yumManager.createRepoQCsv();
		
		Assert.assertEquals("Check CSV", "component-one-package,1.0.2-1,group-one,component-one-artifact,1.0.2,rpm,classifier\n", yumManager.getCsv());
		return yumManager;
	}
	
	private int startLsScpSshServer() throws IOException {
		LOG.info("starting ls ssh server");
		int port = getPort();
		SshServer sshd = getSshServer(port);
        sshd.setCommandFactory(new ScpCommandFactory(new EbsaLsCommandFactory()));
		sshd.start();
        LOG.info("ssh ls server started");
        return port;
	}
	
	private SshServer getSshServer(int port) {
		SshServer sshd = SshServer.setUpDefaultServer();
        sshServers.add(sshd);
        sshd.setPort(port);
        // for some reason the below line makes tests fail on jenkins
        //sshd.setHost("localhost");
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
			@Override
			public boolean authenticate(String username, String password, ServerSession session) {
				return true;
			}
		});
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("src/test/resources/sshd/hostkey.ser"));
		return sshd;
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

	// Exit code from linkTest tests
	int exitCode = 0;

	@Test
	public void linkTestIPT() throws IOException, InterruptedException {
		assumeFalse(IS_WINDOWS);
		// CommandRegister holds the commands issued in the SSH session
		CommandRegister commandRegister = new CommandRegister();
		// Start local SSH server
		startSSH(commandRegister, 60000, false);
		// Override the default exit handler to store the exit code rather
		// than exiting the JVM!
		EnvironmentManagementCLI.setExitHandler(new EnvironmentManagementCLI.ExitHandler() {
			@Override
			public void exit(int code) {
				exitCode = code;
			}
		});
		// Invoke the link test
		String[] args = { "--config=src/test/resources/testconfig.properties", "--command=linkTest", "--linkTestOrg=np",
				"np.puppet.master.jumphosts= ", "np.puppet.master.host=localhost", "np.puppet.master.port=60000" };
		EnvironmentManagementCLI.main(args);
		// Verify the command issued in the link test
		Assert.assertEquals("command was whoami", "whoami", commandRegister.getLastCommand());
	}

	@Test
	public void linkTestHO_IPT() throws IOException, InterruptedException {
		assumeFalse(IS_WINDOWS);
	
		// CommandRegister holds the commands issued in the SSH session
		CommandRegister commandRegister = new CommandRegister();

		// Start local SSH server
		startSSH(commandRegister, 60000, false);
		// Override the default exit handler to store the exit code rather
		// than exiting the JVM!
		EnvironmentManagementCLI.setExitHandler(new EnvironmentManagementCLI.ExitHandler() {
			@Override
			public void exit(int code) {
				exitCode = code;
			}
		});
		// Invoke the link test
		String[] args = { "--config=src/test/resources/testconfig.properties", "--command=linkTest", "--linkTestOrg=np",
				"np.puppet.master.jumphosts= ", "np.puppet.master.host=localhost", "puppet.master.update.login.port=60000" };
		EnvironmentManagementCLI.main(args);
		// Verify the command issued in the link test
		Assert.assertEquals("command was whoami", "whoami", commandRegister.getLastCommand());
	}

	@Test
	public void linkTestHO_IPTWith1Jump() throws IOException, InterruptedException {
		assumeFalse(IS_WINDOWS);

		// CommandRegister holds the commands issued in the SSH session
		CommandRegister commandRegister = new CommandRegister();

		// Start local SSH server
		startSSH(commandRegister, 60000, false);
		// Start a 2nd local SSH server which will act as the jump server to
		// sshd1
		startSSH(null, 61000, true);
		// Override the default exit handler to store the exit code rather
		// than exiting the JVM!
		EnvironmentManagementCLI.setExitHandler(new EnvironmentManagementCLI.ExitHandler() {
			@Override
			public void exit(int code) {
				exitCode = code;
			}
		});
		// Invoke the link test specifying the jump server
		String[] args = { "--config=src/test/resources/testconfig.properties", "--command=linkTest", "--linkTestOrg=np",
				"np.puppet.master.jumphosts=user1@localhost:61000", "np.puppet.master.host=localhost", "np.puppet.master.port=60000" };
		EnvironmentManagementCLI.main(args);
		// Verify the command issued in the link test
		Assert.assertEquals("command was whoami", "whoami", commandRegister.getLastCommand());
	}

	@Test
	public void linkTestHO_IPTWith2Jumps() throws IOException, InterruptedException {
		assumeFalse(IS_WINDOWS);

		// CommandRegister holds the commands issued in the SSH session
		CommandRegister commandRegister = new CommandRegister();
		// Start local SSH server
		startSSH(commandRegister, 60000, false);
		// Start a 2nd local SSH server which will act as the jump server to
		// sshd1
		startSSH(null, 61000, true);
		// Start a 3rd local SSH server which will act as the jump server to
		// sshd2
		startSSH(null, 62000, true);
		// Override the default exit handler to store the exit code rather
		// than exiting the JVM!
		EnvironmentManagementCLI.setExitHandler(new EnvironmentManagementCLI.ExitHandler() {
			@Override
			public void exit(int code) {
				exitCode = code;
			}
		});
		// Invoke the link test specifying the jump servers
		String[] args = { "--config=src/test/resources/testconfig.properties", "--command=linkTest", "--linkTestOrg=np",
				"np.puppet.master.jumphosts=user1@localhost:62000,user2@localhost:61000", "np.puppet.master.host=localhost", "puppet.master.update.login.port=60000" };
		EnvironmentManagementCLI.main(args);
		// Verify the command issued in the link test
		Assert.assertEquals("command was whoami", "whoami", commandRegister.getLastCommand());
	}

	@Test
	public void linkTestUnknownEnv() {
		// Override the default exit handler to store the exit code rather than
		// exiting the JVM!
		EnvironmentManagementCLI.setExitHandler(new EnvironmentManagementCLI.ExitHandler() {
			@Override
			public void exit(int code) {
				exitCode = code;
			}
		});
		// Invoke the link test
		String[] args = { "--config=src/test/resources/testconfig.properties", "--command=linkTest", "--linkTestOrg=????" };
		EnvironmentManagementCLI.main(args);
		// Verify the exit code
		Assert.assertEquals("Exit code == 1", 1, exitCode);
	}

	@Test
	public void linkTestMissingEnv() {
		// Override the default exit handler to store the exit code rather than
		// exiting the JVM!
		EnvironmentManagementCLI.setExitHandler(new EnvironmentManagementCLI.ExitHandler() {
			@Override
			public void exit(int code) {
				exitCode = code;
			}
		});
		// Invoke the link test
		String[] args = { "--config=src/test/resources/testconfig.properties", "--command=linkTest" };
		EnvironmentManagementCLI.main(args);
		// Verify the exit code
		Assert.assertEquals("Exit code == 1", 1, exitCode);
	}

	/**
	 * Starts a local SSH server
	 * 
	 * @param commandRegister
	 *            Records the commands that were issued in the SSH session (null
	 *            if not needed)
	 * @param port
	 *            The port the server listens on
	 * @param portForwarding
	 *            Whether TCP port forwarding should be enabled
	 * @return
	 * @throws IOException
	 */
	private SshServer startSSH(CommandRegister commandRegister, int port, boolean portForwarding) throws IOException {
		SshServer sshd = getSshServer(port);
		if (commandRegister != null) {
			sshd.setCommandFactory(new EbsaCommandFactory(commandRegister));
		}
		if (portForwarding) {
			sshd.setTcpipForwardingFilter(new ForwardingFilter() {
				@Override
				public boolean canListen(SshdSocketAddress address, Session session) {
					return true;
				}

				@Override
				public boolean canForwardX11(Session session) {
					return true;
				}

				@Override
				public boolean canForwardAgent(Session session) {
					return true;
				}

				@Override
				public boolean canConnect(SshdSocketAddress address, Session session) {
					return true;
				}
			});
		}
		sshd.start();
		return sshd;
	}

	/**
	 * Stops the SSH server
	 * 
	 * @param sshd
	 *            The SSH server(s) to stop
	 * @throws InterruptedException
	 */
	private void stopSSH(SshServer... sshd) throws InterruptedException {
		InterruptedException ioe = null;
		for (SshServer sshServer : sshd) {
			try {
				sshServer.stop();
			} catch (InterruptedException e) {
				e.printStackTrace();
				ioe = e;
			}
		}
		if (ioe != null) {
			throw ioe;
		}
	}
}
