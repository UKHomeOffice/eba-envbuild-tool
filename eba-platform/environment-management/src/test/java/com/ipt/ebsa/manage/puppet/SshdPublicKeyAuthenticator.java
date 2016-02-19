package com.ipt.ebsa.manage.puppet;

import java.security.PublicKey;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

public class SshdPublicKeyAuthenticator implements PublickeyAuthenticator {

	@Override
	public boolean authenticate(String username, PublicKey key, ServerSession session) {
		System.out.println(username);
		return true;
	}

}
