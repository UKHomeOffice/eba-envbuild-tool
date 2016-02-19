package com.ipt.ebsa.ssh.testhelper;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;

public class EbsaCommandFactory implements CommandFactory {

	CommandRegister cr;
	
	public EbsaCommandFactory(CommandRegister commandRegistry) {
		cr = commandRegistry;
	}
	
	/**
	 * Parses a command string and verifies that the basic syntax is
	 * correct. If parsing fails the responsibility is delegated to
	 * the configured {@link CommandFactory} instance; if one exist.
	 *
	 * @param command
	 *            command to parse
	 * @return configured {@link Command} instance
	 * @throws IllegalArgumentException
	 */
	public Command createCommand(String command) {
		cr.insertCommand(command);
		return new EbsaScpCommand("scp -f -t");
	}

}
