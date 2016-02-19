package com.ipt.ebsa.ssh;

import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.junit.Test;

import com.ipt.ebsa.ssh.testhelper.CommandRegister;
import com.ipt.ebsa.ssh.testhelper.EbsaCommandFactory;
import com.ipt.ebsa.ssh.testhelper.EbsaLsCommandFactory;
import com.jcraft.jsch.JSchException;

public class JschManagerTest {
	/** 
	 * Some of the unit tests don't seem to work on some versions of Windows. Given the tests take time AND
	 * the deployments will be via a Linux box, we'll skip them on Windows.
	 */
	private static final Boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
	private static final Logger	LOG = LogManager.getLogger(JschManagerTest.class);
	
	private List<SshServer> sshServers = new ArrayList<>();
	private File tmpDir = null;

	@After
	public void tearDownSsh() throws InterruptedException, IOException {
		for(SshServer sshd : sshServers) {
			stopSSH(sshd);
		}
		
		if (tmpDir != null && tmpDir.exists()) {
			FileUtils.deleteDirectory(tmpDir);
		}
	}
	
	@Test
	public void simpleUploadScpTest() throws IOException, InterruptedException, JSchException {
		
		assumeFalse(IS_WINDOWS);
	
		tmpDir = org.eclipse.jgit.util.FileUtils.createTempDir("scp-", "-test", null);
		File destFile = new File(tmpDir, "test.txt");
		
		Assert.assertFalse("File exists", destFile.exists());
		
		String fileContents = "Wouldn't you like to be a pepper too?";
		int port = startScpServer();
		
		JschManager jsch = getJschManager();
		jsch.scpUploadFileContents(1000, "Danni", "localhost", port, new ArrayList<HostnameUsernamePort>(), fileContents, destFile);
		Assert.assertTrue("File exists", destFile.exists());
		Assert.assertEquals("File contents", fileContents, FileUtils.readFileToString(destFile));
	}
	
	@Test
	public void simpleDownloadScpTest() throws Exception {

	assumeFalse(IS_WINDOWS);

	tmpDir = org.eclipse.jgit.util.FileUtils.createTempDir("scp-", "-test", null);
		File sourceFile = new File(tmpDir, "test2.txt");
		String fileContents = "Hey, laser lips!";
		FileUtils.write(sourceFile, fileContents);
		
		int port = startScpServer();
		
		JschManager jsch = getJschManager();
		Assert.assertEquals("File contents", fileContents, jsch.scpDownloadFileContents(1000, "Madonna", "localhost", port, new ArrayList<HostnameUsernamePort>(), sourceFile));
	}
	
	@Test
	public void jumpUploadScpTest() throws IOException, JSchException {
		assumeFalse(IS_WINDOWS);

		tmpDir = org.eclipse.jgit.util.FileUtils.createTempDir("scp-", "-test", null);
		File destFile = new File(tmpDir, "test.txt");
		
		Assert.assertFalse("File exists", destFile.exists());
		
		String fileContents = "Wouldn't you like to be a pepper too?";
		int port = startScpServer();
		
		int jumpPort = getPort();
		
		startSSH(null, jumpPort, true);
		
		JschManager jsch = getJschManager();
		HostnameUsernamePort jumpbox = new HostnameUsernamePort();
		jumpbox.setUsername("Megan");
		jumpbox.setHostname("localhost");
		jumpbox.setPort(jumpPort);
		
		jsch.scpUploadFileContents(1000, "me", "localhost", port, Arrays.<HostnameUsernamePort>asList(jumpbox), fileContents, destFile);
		Assert.assertTrue("File exists", destFile.exists());
		Assert.assertEquals("File contents", fileContents, FileUtils.readFileToString(destFile));
	}
	
	/**
	 * Tests the test harness for ssh ls
	 * @throws IOException
	 * @throws JSchException
	 */
	@Test
	public void testLs() throws IOException, JSchException {
		assumeFalse(IS_WINDOWS);

		tmpDir = org.eclipse.jgit.util.FileUtils.createTempDir("ls-", "-test", null);
		int port = startLsScpSshServer();
		
		LOG.info("creating tmp files");
		File f1 = new File(tmpDir, "f1");
		Assert.assertTrue("create tmp f1", f1.createNewFile());
		File f2 = new File(tmpDir, "f2");
		Assert.assertTrue("create tmp f2", f2.createNewFile());
		
		JschManager jsch = getJschManager();
		ExecReturn execReturn = jsch.runLsSSH(1000, tmpDir, "kylie", "localhost", port, new ArrayList<HostnameUsernamePort>());
		Assert.assertEquals("ls return code", 0, execReturn.getReturnCode());
		Assert.assertEquals("ls listing", "f1\nf2\n", execReturn.getStdOut());
	}
	
	private int startScpServer() throws IOException {
		int port = getPort();
        SshServer sshd = getSshServer(port);
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.start();

        LOG.info("Started scp server on port: " + port);
        
        return port;
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
	
	public static JschManager getJschManager() {
		InputStream knownHosts;
		File idFile, configFile, knownFile;
		try {
			idFile = new File("target/tmp/identity_file");
			configFile = new File("target/tmp/ssh_config");
			knownFile = new File("target/tmp/known_hosts");
			FileUtils.touch(idFile);
			FileUtils.touch(configFile);
			FileUtils.touch(knownFile);
			knownHosts = new FileInputStream(knownFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Failed to create known file", e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create file", e);
		}
		return new JschManager(knownHosts, new File("target/tmp/identity_file"), new File("target/tmp/ssh_config"), true);
	}
}
