package com.ipt.ebsa.ssh.testhelper;

import java.io.IOException;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.command.ScpCommand;

public class EbsaScpCommand extends ScpCommand {

	public EbsaScpCommand(String command) {
		super(command);
	}

	@Override
	public void start(Environment env) {
		
		//throw new StartedCommandException();
		new Thread(this, "ScpCommand: " + name).start();
		
	}

	@Override
	public void run() {
		String message = new String("Mimic command");
		try {
			out.write(0);
			out.write(message.getBytes());
			out.write('\n');
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (callback != null) {
			callback.onExit(0, message);

		}
	}

}
