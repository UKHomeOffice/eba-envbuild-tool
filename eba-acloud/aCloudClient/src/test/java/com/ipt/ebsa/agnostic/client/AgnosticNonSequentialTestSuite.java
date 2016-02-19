package com.ipt.ebsa.agnostic.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.ipt.ebsa.agnostic.client.skyscape.SkyscapeNonSequentialTestSuite;
import com.mycila.junit.concurrent.ConcurrentSuiteRunner;

@RunWith(ConcurrentSuiteRunner.class)
@Suite.SuiteClasses({
	SkyscapeNonSequentialTestSuite.class,
	//AwsNonSequentialTestSuite.class
})
public class AgnosticNonSequentialTestSuite {

}
