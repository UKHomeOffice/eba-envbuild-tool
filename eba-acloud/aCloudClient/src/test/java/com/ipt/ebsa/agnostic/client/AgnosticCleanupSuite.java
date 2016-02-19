package com.ipt.ebsa.agnostic.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.ipt.ebsa.agnostic.client.aws.AwsCleanupTest;
import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;
import com.ipt.ebsa.agnostic.client.skyscape.SkyscapeCleanupTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	AwsCleanupTest.class,
	SkyscapeCleanupTest.class
	
})
public class AgnosticCleanupSuite {
	public AgnosticCleanupSuite() {
		ConfigurationFactory.resetProperties();
	}

}
