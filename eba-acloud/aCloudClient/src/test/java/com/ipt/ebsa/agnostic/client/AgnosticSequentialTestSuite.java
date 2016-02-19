package com.ipt.ebsa.agnostic.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.ipt.ebsa.agnostic.client.aws.AwsSequentialTestSuite;
import com.ipt.ebsa.agnostic.client.skyscape.SkyscapeSequentialTestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	AwsSequentialTestSuite.class,
	SkyscapeSequentialTestSuite.class
})
public class AgnosticSequentialTestSuite {

}
