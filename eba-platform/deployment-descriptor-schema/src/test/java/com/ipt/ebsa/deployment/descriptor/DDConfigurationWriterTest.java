package com.ipt.ebsa.deployment.descriptor;

import java.io.File;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Pettifor, Stephen Cowx
 *
 */
public class DDConfigurationWriterTest {

	@Test
	public void testFailureConditions() throws Exception  {
		try {
			new DDConfigurationWriter().writeTo(null, null);
			Assert.fail("Expected an exception");
		} catch (Exception e) { }
		try {
			new DDConfigurationWriter().writeTo(null, new StringWriter());
			Assert.fail("Expected an exception");
		} catch (Exception e) { }
		
	}
	
	@Test
	public void testWrite() throws Exception  {
		try {
			DDConfigurationLoader loader = new DDConfigurationLoader();
			DeploymentDescriptor deploymentDescriptor = loader.loadDD(new File("src/test/resources/testDDv1.xml"), "SOME_APP");
			Assert.assertNotNull(deploymentDescriptor);
			StringWriter writer = new StringWriter();
			new DDConfigurationWriter().writeTo(deploymentDescriptor.getXMLType(), writer);
			Assert.assertNotNull(writer.toString());
			Assert.assertTrue(writer.toString().contains("path=\"control_managed_instances/startAPPWLSMS1/action\""));
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		
	}

}
