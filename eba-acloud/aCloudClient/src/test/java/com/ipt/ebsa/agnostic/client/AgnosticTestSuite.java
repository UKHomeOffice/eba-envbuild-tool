package com.ipt.ebsa.agnostic.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.mycila.junit.concurrent.ConcurrentSuiteRunner;

@RunWith(ConcurrentSuiteRunner.class)
@Suite.SuiteClasses({
	AgnosticNonSequentialTestSuite.class,
	AgnosticSequentialTestSuite.class
})
public class AgnosticTestSuite {

}
