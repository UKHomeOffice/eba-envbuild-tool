package com.ipt.ebsa.ssh.testhelper;

import org.apache.sshd.server.shell.ProcessShellFactory;

public class EbsaProcessShellFactory extends ProcessShellFactory {

	public EbsaProcessShellFactory(String[] strings) {
		super(strings);
	}

}
