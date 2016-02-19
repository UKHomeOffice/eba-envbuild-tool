package com.ipt.ebsa.environment.build;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.environment.build.diff.BuildDiffer;

public class PlanRegressionTests {

	
	@Test
	public void testForRegression() throws URISyntaxException {
		URI uri = ClassLoader.getSystemResource("TestLoadEnvironmentData_resources").toURI();
		URI uri2 = ClassLoader.getSystemResource("TestLoadEnvironmentData_resources2").toURI();
		
		new BuildDiffer().calculateDifferences(new File(uri), new File(uri2), "HO_IPT_NP_PRP1_MABC", XMLProviderType.SKYSCAPE.toString(), "b1");
	}
}
