package com.ipt.ebsa.agnostic.client.skyscape;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	SkyscapeClientBridgeTest.class,
	VMManagerTest.class,
	VMManagerAuthenticationModuleTest.class,
	XPathHandlerTest.class,
	NetworkManagerTest.class
})
public class SkyscapeSequentialTestSuite {

}
