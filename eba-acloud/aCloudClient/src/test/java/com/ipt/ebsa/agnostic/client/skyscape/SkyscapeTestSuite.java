package com.ipt.ebsa.agnostic.client.skyscape;

/**
 * 
 *
 */
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.mycila.junit.concurrent.ConcurrentSuiteRunner;

@RunWith(ConcurrentSuiteRunner.class)
@Suite.SuiteClasses({
	SkyscapeNonSequentialTestSuite.class,
	SkyscapeSequentialTestSuite.class
})
public class SkyscapeTestSuite {

	public SkyscapeTestSuite() {
	}

}
