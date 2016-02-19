package com.ipt.ebsa.agnostic.client.bridge;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ipt.ebsa.agnostic.client.BaseTest;
import com.ipt.ebsa.agnostic.client.aws.module.AwsEnvironmentModule;
import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.sdkclient.acloudconfig.ACloudConfigurationLoader;

/**
 * Tests for the Agnostic Client Bridge
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AgnosticClientBridgeTest extends BaseTest {
	
	private static XMLEnvironmentType env = new XMLEnvironmentType();
	private static AwsEnvironmentModule envModule;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		env.setName("EBSAD-20086-ENV");
		container = weld.initialize();
		envModule = container.instance().select(AwsEnvironmentModule.class).get();
		// Delete our Vpc first so we know we're starting clean
		envModule.deleteVpc(env,"");
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		envModule.deleteVpc(env,"");
	}
	
	/**
	 * Tests the bridge over the agnostic client.
	 * Causes an Exception due to invalid credentials when attempting an action at Amazon
	 * @throws Exception
	 */
	@Test
	public void a_testBridgeInvalidCredentials() throws Exception {
		Properties testProperties = ConfigurationFactory.getProperties();
		testProperties.put("user", "invalid-user");
		testProperties.put("password", "invalid-password");
		
		AgnosticClientBridge bridge = new AgnosticClientBridge();
		BridgeConfig config = new BridgeConfig();
		
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		XMLGeographicContainerType definition = loader.loadVC(new File("src/test/resources/aws/awsBridgeTestEnvironment.xml"));
		config.setDefinitionXML(definition);
		CmdExecute instructions = loader.loadJob(new File("src/test/resources/aws/commands", "createVMC.xml"));
		config.setInstructionXML(instructions);
		config.setProperties(testProperties);
		
		try {
			bridge.execute(config);
			Assert.fail("Expected an Exception due to invalid credentials");
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			Assert.assertNotNull(cause);
			Assert.assertTrue(cause.getMessage().contains("credentials"));
		}
	}
	
	@Test
	public void b_testBridgeCreateVPC() throws Exception {
		testBridge("createVPC.xml");
	}
	
	@Test
	public void e_testBridgeCreateVMC() throws Exception {
		testBridge("createVMC.xml");
	}
	
	@Test
	public void l_testBridgeDeleteVMC() throws Exception {
		testBridge("deleteVMC.xml");
	}
	
	@Test
	public void f_testBridgeCreateAppNetwork() throws Exception {
		testBridge("createAppNetwork.xml");
	}
	
	@Test
	public void i_testBridgeDeleteAppNetwork() throws Exception {
		// VM must be deleted first
		//g_testBridgeDeleteVM();
		
		testBridge("deleteAppNetwork.xml");
	}
	
	@Test
	public void g_testBridgeCreateVM() throws Exception {
		testBridge("createVM.xml");
	}
	
	@Test
	public void h_testBridgeDeleteVM() throws Exception {
		testBridge("deleteVM.xml");
	}
	
	@Test
	public void c_testBridgeCreateGateway() throws Exception {
		testBridge("createGateway.xml");
	}
	
	@Test
	public void k_testBridgeDeleteGateway() throws Exception {
		testBridge("deleteGateway.xml");
	}
	
	@Test
	public void d_testBridgeCreateOrgNetwork() throws Exception {
		testBridge("createOrgNetwork.xml");
	}
	
	@Test
	public void j_testBridgeDeleteOrgNetwork() throws Exception {
		testBridge("deleteOrgNetwork.xml");
	}
	
	@Test
	public void m_testBridgeDeleteVPC() throws Exception {
		testBridge("deleteVPC.xml");
	}
	
	private void testBridge(String instructionsFile) throws Exception {
		Properties testProperties = ConfigurationFactory.getProperties();
		// Send empty credentials to pass BridgeConfig validation. AwsConnector will then load credentials from /Users/<username>/.aws/credentials.
		testProperties.put("user", "");
		testProperties.put("password", "");
		
		AgnosticClientBridge bridge = new AgnosticClientBridge();
		BridgeConfig config = new BridgeConfig();
		
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		XMLGeographicContainerType definition = loader.loadVC(new File("src/test/resources/aws/awsBridgeTestEnvironment.xml"));
		config.setDefinitionXML(definition);
		CmdExecute instructions = loader.loadJob(new File("src/test/resources/aws/commands", instructionsFile));
		config.setInstructionXML(instructions);
		config.setProperties(testProperties);
		
		bridge.execute(config);
	}
}
