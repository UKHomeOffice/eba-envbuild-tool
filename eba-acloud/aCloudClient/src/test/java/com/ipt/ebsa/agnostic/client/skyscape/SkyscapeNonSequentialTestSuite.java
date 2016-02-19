package com.ipt.ebsa.agnostic.client.skyscape;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.mycila.junit.concurrent.ConcurrentSuiteRunner;

@RunWith(ConcurrentSuiteRunner.class)
@Suite.SuiteClasses({
	StrategyHandlerTest.class,
	TaskUtilTest.class,
	RestCallUtilTest.class,
	VirtualDiskComparatorTest.class,
	ControlErrorHandlerTest.class,
})
public class SkyscapeNonSequentialTestSuite {

}
