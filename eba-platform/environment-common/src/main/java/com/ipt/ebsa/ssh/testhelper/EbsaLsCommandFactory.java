/**
 * 
 */
package com.ipt.ebsa.ssh.testhelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

/**
 * @author James Shepherd
 *
 */
public class EbsaLsCommandFactory implements CommandFactory {

	private static final Logger	LOG = LogManager.getLogger(EbsaLsCommandFactory.class);
	
	public EbsaLsCommandFactory() {
	}

	@Override
	public Command createCommand(String command) {
		return new EbsaLsCommand(command);
	}
	
	public class EbsaLsCommand implements Command, Runnable {
		
		private String command;
		// private InputStream in;
		private OutputStream out;
		//private OutputStream err;
		private ExitCallback callback;
		
		public EbsaLsCommand(String command) {
			this.command = command;
			LOG.info("Received command: " + command);
		}

		@Override
		public void setInputStream(InputStream in) {
			// this.in = in;
		}

		@Override
		public void setOutputStream(OutputStream out) {
			this.out = out;
		}

		@Override
		public void setErrorStream(OutputStream err) {
			// this.err = err;
		}

		@Override
		public void setExitCallback(ExitCallback callback) {
			this.callback = callback;
		}

		@Override
		public void start(Environment env) throws IOException {
			new Thread(this).start();
		}

		@Override
		public void destroy() {
		}

		@Override
		public void run() {
			if (!command.startsWith("ls -1 ")) {
				LOG.error("Command is not 'ls -1 '");
				callback.onExit(1, "Is not 'ls -1 '");
			}
			
			String path = command.substring(6);
			File dirPath = new File(path);
			LOG.info("ls -1 '" + path + "'");
			
			if (!dirPath.isDirectory()) {
				LOG.error("Path is not a directory");
				callback.onExit(2, "Path is not a directory");
			}
			
			PrintWriter pw = new PrintWriter(out);
			
			String[] ls = dirPath.list();
			Arrays.sort(ls);
			
			for (String fn : ls) {
				// don't use println to force proper (UNIX) line-endings
				pw.print(fn);
				pw.print('\n');
			}
			
			pw.close();
			
			callback.onExit(0);
		}
	}
}
