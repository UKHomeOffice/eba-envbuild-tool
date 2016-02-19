package com.ipt.ebsa.ssh;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.util.LogOutputStream;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ConfigRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class JschManager {

	private static final Pattern GET_FILE_METADATA_PARSE = Pattern.compile("^C\\d{4} ([\\d]+) (.*)$");
	
	private static Logger LOG	= LogManager.getLogger(JschManager.class);
	private JSch	jsch;
	private File openSshConfigFile;
	private InputStream openSshKnownHosts;
	private boolean usePty;

	/**
	 * @param usePty e.g. Configuration.getOpenSSHUsePty()
	 * @param identitiesFile e.g. Configuration.getOpenSSHIdentityFile()
	 * @param openSshKnownHosts e.g. new FileInputStream(Configuration.getOpenSSHKnownHosts())
	 * @param OpenSshConfigFile e.g. Configuration.getOpenSSHConfig()
	 * @throws JSchException 
	 * @throws FileNotFoundException 
	 */
	public JschManager(InputStream openSshKnownHosts, File identitiesFile, File openSshConfigFile, boolean usePty) {
		this(openSshKnownHosts, identitiesFile, openSshConfigFile, usePty, null);
	}
	
	/**
	 * @param usePty e.g. Configuration.getOpenSSHUsePty()
	 * @param identitiesFile e.g. Configuration.getOpenSSHIdentityFile()
	 * @param openSshKnownHosts e.g. new FileInputStream(Configuration.getOpenSSHKnownHosts())
	 * @param OpenSshConfigFile e.g. Configuration.getOpenSSHConfig()
	 * @throws JSchException 
	 * @throws FileNotFoundException 
	 */
	public JschManager(InputStream openSshKnownHosts, File identitiesFile, File openSshConfigFile, boolean usePty, Set<Level> explicitLogLevels) {
		JSch.setLogger(new JschLogger(LOG, explicitLogLevels));
		jsch = new JSch();
		this.openSshKnownHosts = openSshKnownHosts;
		knownHosts(jsch, openSshKnownHosts);
		//identities(jsch, identitiesFile);
		if(openSshConfigFile == null || !openSshConfigFile.exists()) {
			//knownHosts(jsch, openSshKnownHosts);
			LOG.error("No ssh options file. This is needed to discover identities!");
		}
		this.setOpenSshConfigFile(openSshConfigFile);
		this.usePty = usePty;
		Vector identNames = null;
		try {
			identNames = jsch.getIdentityNames();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOG.debug(String.format("ConfigRepository[%s] HostKeyRepository[%s] IdentityNames[%s] IdentityRepository[%s]", 
		jsch.getConfigRepository(),jsch.getHostKeyRepository(),identNames == null?identNames:identNames.toString(),jsch.getIdentityRepository()));
	}
	
	public JSch getJsch() {
		return jsch;
	}

	private void knownHosts(final JSch sch, final InputStream in) {
		try {
			sch.setKnownHosts(in);
		} catch (JSchException e) {
			throw new RuntimeException("Failed to set known hosts file", e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * If needed this method can be used to load multiple certificate identity's for use in the SSH connection.
	 * 
	 * @param sch
	 * @param priv
	 */
	private static void loadIdentity(final JSch sch, final File priv) {
		try {
			sch.addIdentity(priv.getAbsolutePath());
		} catch (JSchException e) {
			LOG.warn("Adding identity", e);
		}
	}

	private void identities(final JSch sch, File identityFile) {
		loadIdentity(sch, identityFile);
	}

	/**
	 * Run a command on a remote host using ssh to get there.
	 * @param timeout connection timeout in milliseconds
	 * @param command command to execute on remote host
	 * @param username username to use when username is not specified in hosts param
	 * @param host destination host
	 * @param jumphosts possibly empty list of HostnameUsernamePort objects
	 * @param port ssh post used on all hosts in hosts
	 * @return exit code of command executed on host
	 */
	public int runSSHExec(int timeout, String command, String username, String host, int port, List<HostnameUsernamePort> jumphosts) {
		return runSSHExecWithOutput(false, timeout, command, username, host, port, jumphosts, true).getReturnCode();
	}
	
	/**
	 * Run a command on a remote host using ssh to get there.
	 * @param timeout connection timeout in milliseconds
	 * @param command command to execute on remote host
	 * @param username username to use when username is not specified in hosts param
	 * @param host destination host
	 * @param jumphosts possibly empty list of HostnameUsernamePort objects
	 * @param port ssh post used on all hosts in hosts
	 * @param unescapeJava Should we unescape the output from the commands? (ie, \\n to \n)
	 * @return exit code of command executed on host
	 */
	public ExecReturn runSSHExecWithOutput(int timeout, String command, String username, String host, int port, List<HostnameUsernamePort> jumphosts, boolean unescapeJava) {
		return runSSHExecWithOutput(true, timeout, command, username, host, port, jumphosts, unescapeJava);
	}
	
	/**
	 * Run 'ls -1' on a remote host using ssh to get there.
	 * @param timeout connection timeout in milliseconds
	 * @param dir dir to ls
	 * @param username username to use when username is not specified in hosts param
	 * @param host destination host
	 * @param jumphosts possibly empty list of HostnameUsernamePort objects
	 * @param port ssh post used on all hosts in hosts
	 * @return exit code of command executed on host
	 */
	public ExecReturn runLsSSH(int timeout, File dir, String username, String host, int port, List<HostnameUsernamePort> jumphosts) {
		return runSSHExecWithOutput(true, timeout, "ls -1 " + dir.getPath(), username, host, port, jumphosts, true);
	}
	
	/**
	 * Run a command on a remote host using ssh to get there.
	 * @param withOutput true if should store output in return object
	 * @param timeout connection timeout in milliseconds
	 * @param command command to execute on remote host
	 * @param username username to use when username is not specified in hosts param
	 * @param host destination host
	 * @param jumphosts possibly empty list of HostnameUsernamePort objects
	 * @param port ssh post used on all hosts in hosts
	 * @return exit code of command executed on host
	 */
	public ExecReturn runSSHExecWithOutput(boolean withOutput, int timeout, String command, String username, String host, int port, List<HostnameUsernamePort> jumphosts, boolean unescapeJava) {
		try {
			LOG.debug(String.format(
					"Calling Jsch at %s with options: timeout=%s command=%s, username=%s, host=%s, port=%s, jumphosts=%s",
					System.currentTimeMillis(), timeout, command, username, host, port, jumphosts.toString()));
			
			Session[] sessions = getSshSessions(timeout, username, host, port, jumphosts);
			Session session = sessions[sessions.length - 1];
			LOG.debug(String.format(
					"Using Session Host[%s] FingerPrint[%s] HostKeyHost[%s] HostKey[%s] HostKeyMarker[%s] HostKeyType[%s] HostKeyAlias[%s] Username[%s]", 
					session.getHost(), session.getHostKey().getFingerPrint(jsch), session.getHostKey().getHost(), 
					session.getHostKey().getKey(), session.getHostKey().getMarker(), 
					session.getHostKey().getType(), session.getHostKeyAlias(), session.getUserName()));
			
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			((ChannelExec) channel).setPty(usePty);
			LOG.info(usePty ? "Using openssh with a Pseudo-Terminal" : 
				"Using openssh WITHOUT a Pseudo-Terminal so this remote call may not generate an output. Add the following config to ensure an output: open.ssl.use.pty=true");		
	
			LogOutputStream jschLogDebug = new LogOutputStream(LOG, Level.DEBUG);
			LogOutputStream jschLogError = new LogOutputStream(LOG, Level.ERROR);
	
			channel.setOutputStream(jschLogDebug);
			((ChannelExec) channel).setErrStream(jschLogError);
	
			InputStream in = channel.getInputStream();
			channel.connect();
			
			StringBuilder output = new StringBuilder();
	
			byte[] tmp = new byte[1024];
			while (true) {
				StringBuilder commandOut = new StringBuilder();
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					String b = new String(tmp, 0, i);
					commandOut.append(b);
				}

				String finalOutput = commandOut.toString();
				if (StringUtils.isNotBlank(finalOutput)) {
					if (unescapeJava) {
						finalOutput = StringEscapeUtils.unescapeJava(finalOutput);
					}
					if (withOutput) {
						output.append(finalOutput);
					}
					LOG.debug(finalOutput);
				}
				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					LOG.debug(String.format("Exit status: %s", channel.getExitStatus()));
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
			int retCode = channel.getExitStatus();
			channel.disconnect();
			
			closeSessions(sessions);
			
			LOG.debug(String.format("Calling JSch finished at %s", System.currentTimeMillis()));
			
			ExecReturn ret = new ExecReturn();
			ret.setReturnCode(retCode);
			ret.setStdOut(output.toString());
			return ret;
		} catch (IOException e) {
			throw new RuntimeException("Failed to run ssh command", e);
		} catch (JSchException e) {
			throw new RuntimeException("Failed to run ssh command", e);
		}
	}

	private void closeSessions(Session[] sessions) {
		for (int i = sessions.length - 1; i >= 0; i--) {
			sessions[i].disconnect();
		}
	}
	
	/**
	 * 
	 * @param timeout connection timeout in milliseconds
	 * @param username username to use when username is not specified in hosts param
	 * @param host destination host
	 * @param jumphosts possibly empty list of HostnameUsernamePort objects
	 * @param port ssh post used on all hosts in hosts
	 * @param fileContents
	 * @param destFile
	 * @throws IOException
	 * @throws JSchException
	 */
	public void scpUploadFileContents(int timeout, String username, String host, int port, List<HostnameUsernamePort> jumphosts, String fileContents, File destFile) {
		LOG.debug(String.format(
				"Calling JSch at %s with options: timeout=%s username=%s, host=%s, port=%s, jumphosts=%s, destPath=%s",
				System.currentTimeMillis(), timeout, username, host, port, jumphosts.toString(), destFile.toString()));
		
		Session[] sessions = getSshSessions(timeout, username, host, port, jumphosts);
		Session session = sessions[sessions.length - 1];

		try {
			sendFile(session, destFile, fileContents);
		} finally {
			LOG.debug("About to close sessions");
			closeSessions(sessions);
			LOG.debug("Sessions closed");
		}
	}
	
	/**
	 * 
	 * @param timeout connection timeout in milliseconds
	 * @param username username to use when username is not specified in hosts param
	 * @param host destination host
	 * @param jumphosts possibly empty list of HostnameUsernamePort objects
	 * @param port ssh post used on all hosts in hosts
	 * @param sourceFile
	 * @throws Exception 
	 */
	public String scpDownloadFileContents(int timeout, String username, String host, int port, List<HostnameUsernamePort> jumphosts, File sourceFile) {
		LOG.debug(String.format(
				"Calling JSch at %s with options: timeout=%s username=%s, host=%s, port=%s, jumphosts=%s, sourceFile=%s",
				System.currentTimeMillis(), timeout, username, host, port, jumphosts.toString(), sourceFile.toString()));
		
		Session[] sessions = getSshSessions(timeout, username, host, port, jumphosts);
		Session session = sessions[sessions.length - 1];
		String output = null;
		
		try {
			output = readFile(session, sourceFile);
		} finally {
			closeSessions(sessions);
		}
		
		return output;
	}

	
	/**
	 * Sends data to the destination file using the session provided.
	 *
	 * @throws IOException
	 * @throws JSchException
	 */
	private void sendFile(Session session, File destFile, String data) {
		ChannelExec c = null;
		try {
			c = (ChannelExec) session.openChannel("exec");
	        String command = "scp -t " + destFile.getParent();
	        c.setCommand(command);
	        OutputStream os = c.getOutputStream();
	        InputStream is = c.getInputStream();
	        c.connect();
	        LOG.debug(String.format("Connected to exec channel, command run was [%s]", command));
	        
	        if (is.read() != 0) {
	        	throw new RuntimeException("Failed initial handshake");
	        }
	        
	        byte[] buf = data.getBytes("UTF-8"); 
	        
	        command = "C7777 "+ buf.length + " " + destFile.getName() + "\n";
			os.write(command.getBytes());
	        os.flush();
	        LOG.debug(String.format("Executed file creation command [%s]", command));
	        
	        if (is.read() != 0) {
	        	throw new RuntimeException("Failed to get acknowledgement of header");
	        }
	        
	        os.write(buf, 0, buf.length);
	        os.flush();
	        LOG.debug("Flushed .csv contents to interactive scp terminal");
	        
	        os.write(0); // A final byte which will prompt the interactive terminal to stop listening for more bytes to write
	        os.flush();
	        LOG.debug("Flushed 0 byte to terminal");

	        if (is.read() != 0) {
	        	throw new RuntimeException("Failed to get acknowledgement of file data");
	        }
		} catch (JSchException e) {
			throw new RuntimeException("Failed to send file", e);
        } catch (IOException e) {
        	throw new RuntimeException("Failed to send file", e);
		} finally {
        	// otherwise ssh server sometimes throws an error
	        try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				LOG.info("Interrupted while sleeping");
			}
	        if (null != c) {
	        	c.disconnect();
	        	LOG.debug("Disconnected");
	        }
        }
    }
	
	private String readFile(Session session, File sourceFile) {
        ChannelExec c = null;
		try {
			c = (ChannelExec) session.openChannel("exec");
	        OutputStream os = c.getOutputStream();
	        InputStream is = c.getInputStream();
	        String command = "scp -f " + sourceFile.getPath();
	        LOG.info(String.format("Command: [%s]", command));
	        c.setCommand("scp -f " + sourceFile.getPath());
	        c.connect();
	
	        byte[] buffer = null;

	        os.write(0);
	        os.flush();
	        
	        String header = readLine(is);
	        LOG.info(String.format("FILE_HEADER: [%s]", header));
	        Matcher m = GET_FILE_METADATA_PARSE.matcher(header);
	        
	        if (!m.matches()) {
	        	throw new RuntimeException(String.format("Header format not recognised: [%s]", header));
	        }
	        
	        String mode = m.group(0);
	        String filesize = m.group(1);
	        String filename = m.group(2);
	        
	        if (null == mode || null == filesize || null == filename) {
	        	throw new RuntimeException(String.format("Something wrong with header: [%s]", header));
	        }
	        
	        LOG.info(String.format("FILE_METADATA - Mode: [%s] Size:[%s] Name:[%s]", mode, filesize, filename));
	        
	        if (!filename.equals(sourceFile.getName())) {
	        	throw new RuntimeException(String.format("Not sent expected file: [%s] sent: [%s]", sourceFile.getName(), filename));
	        }
	        
	        int expectedLength = Integer.parseInt(filesize);
	        os.write(0);
	        os.flush();
	
	        buffer = readBytes(is, expectedLength);
	        
	        if (expectedLength != buffer.length) {
	        	throw new RuntimeException(String.format("Received file not correct length, Expected: [%s] Actual: [%s]", expectedLength, buffer.length));
	        }
	        
	        if (0 != is.read()){
	        	throw new RuntimeException("Failed to confirm end of file");
	        }
	        
	        os.write(0);
	        os.flush();
	        return new String(buffer);
        } catch (JSchException e) {
        	throw new RuntimeException("failed to read file", e);
        } catch (IOException e) {
        	throw new RuntimeException("failed to read file", e);
		} finally {
        	// otherwise ssh server sometimes throws an error
	        try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				LOG.info("Interrupted while sleeping");
			}
	        if (null != c) {
	        	c.disconnect();
	        }
        }
    }
	
	private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            int c = in.read();
            if (c == '\n') {
                return baos.toString("UTF-8");
            } else if (c == -1) {
                throw new RuntimeException("Error, end of stream");
            } else {
                baos.write(c);
            }
        }
    }
	
	private byte[] readBytes(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < maxBytes; i++) {
            int c = in.read();
            if (c == -1) {
                break;
            } else {
                baos.write(c);
            }
        }
        
        return baos.toByteArray();
	}
	
	private Session[] getSshSessions(int timeout, String username, String host, int port, List<HostnameUsernamePort> jumphosts) {
		try {
			JSch jsch = getJsch();
			ConfigRepository configRepository = null;
			
			// Use default OpenSSH config file
			// Override OpenSSH config logic has been removed
			// It is expected that the caller would provide a property to override the absolute path to the environment OpenSSH config file
			// e.g. puppet.openssl.config.filename=/opt/environment-management/config/ssh_config
			String configAbsolutePath = getOpenSshConfigFile().getAbsolutePath();
			LOG.debug("Using openssh config dir " + configAbsolutePath);
			configRepository = com.jcraft.jsch.OpenSSHConfig.parseFile(configAbsolutePath);
			jsch.setConfigRepository(configRepository);
			
			try(BufferedReader br = new BufferedReader(new FileReader(configAbsolutePath))) {
			    for(String line; (line = br.readLine()) != null; ) {
			    	if(line.contains("IdentityFile")) {
			    		String[] files = line.trim().split("IdentityFile");
						try {
							LOG.debug("adding ident file [" + files[1]+"] "+new File(files[1].trim()).exists());
							jsch.addIdentity(files[1].trim());
						} catch (Exception e) {

						}
			    	}
			    	
			    	if(line.contains("UserKnownHostsFile")) {
			    		String[] files = line.trim().split("UserKnownHostsFile");
						try {
							LOG.debug("adding UserKnownHostsFile file [" + files[1]+"] "+new File(files[1].trim()).exists());
							jsch.setKnownHosts(files[1].trim());
						} catch (Exception e) {

						}
			    	}
			        // process the line.
			    }
			    // line is not visible here.
			}
			
			HostnameUsernamePort destinationHost = new HostnameUsernamePort();
			destinationHost.setHostname(host);
			destinationHost.setUsername(username);
			destinationHost.setPort(port);
			
			ArrayList<HostnameUsernamePort> jumps = new ArrayList<>(jumphosts);
			jumps.add(destinationHost);
			
			Session[] sessions = new Session[jumps.size()];
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			
			Session session = null;
			
			for (int i = 0; i < jumps.size(); i++) {
				HostnameUsernamePort thisJump = jumps.get(i);
				String hostname = thisJump.getHostname();
			    String user = thisJump.getUsername();
			    int sessionPort = thisJump.getPort();
			    
				if (i > 0) {
			        int assignedPort = session.setPortForwardingL(0, hostname, sessionPort);
			        LOG.info(String.format("Session %s@%s:%s is port forwarding: localhost:%s -> %s:%s", session.getUserName(), session.getHost(), session.getPort(), assignedPort, hostname, sessionPort));
			        sessions[i] = session = jsch.getSession(user, "127.0.0.1", assignedPort);
				} else {
					sessions[0] = session = jsch.getSession(user, hostname, sessionPort);				
				}
				
				session.setConfig(config);
				session.setHostKeyAlias(hostname);
		        session.connect(timeout);
		        LOG.info(String.format("The session has been established to %s@%s:%s key[%s] HostKeyAlias[%s]", session.getUserName(), session.getHost(), session.getPort(), session.getHostKey(), session.getHostKeyAlias()));
		        LOG.debug("Using session :"+ReflectionToStringBuilder.reflectionToString(session));
			}
			
			if (session == null) {
				throw new IllegalArgumentException("No hosts provided");
			}
			return sessions;
		} catch (IOException e) {
			throw new RuntimeException("Failed to get ssh sessions", e);
		} catch (JSchException e) {
			throw new RuntimeException("Failed to get ssh sessions", e);
		}
	}
	
	public File getOpenSshConfigFile() {
		return openSshConfigFile;
	}

	public void setOpenSshConfigFile(File openSshConfigFile) {
		this.openSshConfigFile = openSshConfigFile;
	}

	private static class JschLogger implements com.jcraft.jsch.Logger {
		
		private Logger log4jLogger;
		// Null means ALL levels enabled
		private Set<Level> explicitLevels;
		
		// Maps Jsch logger levels to Log4j levels
		private static final Map<Integer, Level> LEVELS = new TreeMap<>();
	    static {
	    	LEVELS.put(new Integer(DEBUG), Level.DEBUG);
	    	LEVELS.put(new Integer(INFO), Level.INFO);
	    	LEVELS.put(new Integer(WARN), Level.WARN);
	    	LEVELS.put(new Integer(ERROR), Level.ERROR);
	    	LEVELS.put(new Integer(FATAL), Level.FATAL);
	    }
		
		private JschLogger(Logger log4jLogger, Set<Level> explicitLevels) {
			this.log4jLogger = log4jLogger;
			
			if (explicitLevels != null) {
				for (Level explicitLevel : explicitLevels) {
					if (explicitLevel != null) {
						if (this.explicitLevels == null) {
							this.explicitLevels = new HashSet<>();
						}
						
						this.explicitLevels.add(explicitLevel);
					}
				}
			}
		}
		
		@Override
		public boolean isEnabled(int level) {
			return explicitLevels == null ? true : explicitLevels.contains(LEVELS.get(level));
		}

		@Override
		public void log(int level, String message) {
			log4jLogger.log(LEVELS.get(level), "Jsch: " + message);
		}
	}
}
