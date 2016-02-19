package com.ipt.ebsa.agnostic.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;

@RunWith(Suite.class)
@Suite.SuiteClasses({
})
public class DefaultTestSuite {
	public DefaultTestSuite() {
		ConfigurationFactory.resetProperties();
	}

}
