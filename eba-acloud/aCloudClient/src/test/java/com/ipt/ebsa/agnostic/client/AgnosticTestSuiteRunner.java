package com.ipt.ebsa.agnostic.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	AgnosticTestSuite.class,
	AgnosticCleanupSuite.class
})
public class AgnosticTestSuiteRunner {
	public AgnosticTestSuiteRunner() {
		ConfigurationFactory.resetProperties();
	}

}
