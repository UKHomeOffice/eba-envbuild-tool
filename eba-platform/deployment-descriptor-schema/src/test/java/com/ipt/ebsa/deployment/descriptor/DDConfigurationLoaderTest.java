package com.ipt.ebsa.deployment.descriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Test;

/**
 * @author Daniel Pettifor, Stephen Cowx
 *
 */
public class DDConfigurationLoaderTest {

	@Test
	public void testFailureConditions() throws Exception  {
		try {
			new DDConfigurationLoader().loadDD(null, "SOME_APP");
			fail("Expected an exception");
		} catch (Exception e) { }
		try {
			new DDConfigurationLoader().loadDD(new File("some/file/that/does/not/exist"), "SOME_APP");
			fail("Expected an exception");
		} catch (Exception e) { }
		
	}
	
	@Test
	public void testLoadVCSL2() throws Exception  {
		DDConfigurationLoader loader = new DDConfigurationLoader();
		DeploymentDescriptor deploymentDescriptor = loader.loadDD(new File("src/test/resources/testDDv1.xml"), "SOME_APP");
		
		assertNotNull(deploymentDescriptor);
		
		XMLPlansType type =  deploymentDescriptor.getXMLType().getPlans();
		List<XMLPlanType> plans = type.getPlan();
		
		assertEquals("one", plans.get(0).getName());
		assertEquals("most difficult plan", plans.get(0).getDescription());
		assertEquals(new Integer(2), plans.get(0).getImpactLevel());
		
		assertEquals("Number of environments", 1, deploymentDescriptor.getXMLType().getEnvironments().getEnvironment().size());
		XMLEnvironmentType ddEnvironment = deploymentDescriptor.getXMLType().getEnvironments().getEnvironment().get(0);
		assertEquals("Environment name", "PRP1", ddEnvironment.getName());
		assertEquals("Number of zones", 1, ddEnvironment.getZone().size());
		assertEquals("Zone name", "dazo", ddEnvironment.getZone().get(0).getName());
		assertEquals("hiera location name", "np_prp1_dazo", ddEnvironment.getZone().get(0).getReference());
		assertEquals("Number of targets in dazo", 1, ddEnvironment.getZone().get(0).getTarget().size());
		assertEquals("Target name", "soa", ddEnvironment.getZone().get(0).getTarget().get(0).getName());
		assertEquals("Target hostnames", "soatzm01", ddEnvironment.getZone().get(0).getTarget().get(0).getHostnames());
		
		List<XMLStepItemType> transitions = plans.get(0).getStep();
		int count = 0;
		for (XMLStepItemType xmlStepItemType : transitions) {
			
			List<XMLStepCommandType> list = xmlStepItemType.getInjectOrPerformOrExecute();
			
			if (count == 0) {
				//Step 1
				XMLStepCommandType xmlStepCommandType = list.get(0);
				assertEquals("description",((XMLInjectType)xmlStepCommandType).getDescription());
				assertEquals("soatzm01,soatzm02",((XMLInjectType)xmlStepCommandType).getHostnames());
				assertEquals(XMLFailureActionsType.FAIL,((XMLInjectType)xmlStepCommandType).getIfMissing());
				assertEquals("control_managed_instances/startAPPWLSMS1/action",((XMLInjectType)xmlStepCommandType).getPath());
				assertEquals("stop",((XMLInjectType)xmlStepCommandType).getValue());
				
				xmlStepCommandType = list.get(1);
				assertEquals("desc",((XMLInjectType)xmlStepCommandType).getDescription());
				assertEquals("soatzm01",((XMLInjectType)xmlStepCommandType).getHostnames());
				assertEquals(null,((XMLInjectType)xmlStepCommandType).getIfMissing());
				assertEquals("action",((XMLInjectType)xmlStepCommandType).getPath());
				assertEquals("\n"+
                        "						something:\n"+
                        "						- 'Class[Profile::Wls::Ipt_custom]'\n"+
                        "						- 'Class[Profile::Wls::Startwls_managed]'\n"+
                        "					",((XMLInjectType)xmlStepCommandType).getYaml());
			}
			else if (count == 1) {
				XMLStepCommandType xmlStepCommandType = list.get(0);
                                    XMLPerformType perform = ((XMLPerformType) xmlStepCommandType);
                                    assertEquals("Do everything in one go",perform.getDescription());
				assertEquals(null,((XMLPerformType)xmlStepCommandType).getHostnames());
				assertEquals(XMLDeploymentActionType.all,((XMLPerformType)xmlStepCommandType).getFilter());
				 
			}
			else if (count == 2) {
				XMLStepCommandType xmlStepCommandType = list.get(0);
                                    XMLExecuteType execute = ((XMLExecuteType) xmlStepCommandType);
                                    assertEquals("Run a command",execute.getDescription());
				assertEquals("soatzm01",execute.getHostnames());
                                    assertEquals("ps -ef",execute.getCommand());
			}
			count++;

		}			
		
		assertEquals("one", plans.get(0).getName());
		assertEquals("most difficult plan", plans.get(0).getDescription());
		assertEquals(new Integer(2), plans.get(0).getImpactLevel());
	}
}
