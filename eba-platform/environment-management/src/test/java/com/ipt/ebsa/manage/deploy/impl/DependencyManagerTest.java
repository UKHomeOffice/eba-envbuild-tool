package com.ipt.ebsa.manage.deploy.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.xml.sax.SAXException;

import com.ipt.ebsa.deployment.descriptor.DDConfigurationLoader;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;

public class DependencyManagerTest {
	
	@Test
	public void testResolveUpstreamCircularDependencyChains() throws Exception {
		assertCircularDependencyDetectedFor("src/test/resources/UpstreamCircularDependencyDeploymentDescriptor.xml");
	}
	
	
	@Test
	public void testResolveDownstreamCircularDependencyChains() throws Exception {
		assertCircularDependencyDetectedFor("src/test/resources/DownstreamCircularDependencyDeploymentDescriptor.xml");
	}

	
	private void assertCircularDependencyDetectedFor(String deploymentDescriptor) throws SAXException, IOException {
		Map<ComponentId, ComponentDeploymentData> components = new TreeMap<>();
		//Load the deployment descriptor into our memory structure nicely
		DDConfigurationLoader loader = new DDConfigurationLoader();
		DeploymentDescriptor dd = loader.loadDD(new File(deploymentDescriptor), "APP");
		new ComponentDeploymentDataManager().listComponentsInDeploymentDescriptor(dd, components);
	
		try {
			new DependencyManager().resolveDependencyChains(components);
			fail("Expected to see a circular dependency detected here.");
		} catch (Exception e) {
			assertTrue("Exception relates to circular dependency", e.getMessage().startsWith("Circular dependency detected, component"));
		}
	}
	
	
	@Test
	public void testResolveComplexDependencyChains() throws Exception {
		Map<ComponentId, ComponentDeploymentData> components = new TreeMap<>();
		
		//Load the deployment descriptor into our memory structure nicely
		DDConfigurationLoader loader = new DDConfigurationLoader();
		DeploymentDescriptor dd = loader.loadDD(new File("src/test/resources/rangeOfDependenciesDeploymentDescriptor.xml"), "APP");
		new ComponentDeploymentDataManager().listComponentsInDeploymentDescriptor(dd, components);

		//Now test the function
		DependencyManager manager = new DependencyManager();
		TreeMap<ComponentId, TreeMap<ComponentId, ?>> chains = manager.resolveDependencyChains(components);
		
		assertNotNull(chains);
	}
}
