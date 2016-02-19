package com.ipt.ebsa.agnostic.client.skyscape;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ipt.ebsa.agnostic.client.bridge.AgnosticClientBridge;
import com.ipt.ebsa.agnostic.client.bridge.BridgeConfig;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppUnavailableControlException;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.vmware.vcloud.sdk.VCloudException;
		

/**
 * Test case to excercise the use of the Authentication Module.
 * 
 * The decision to use the AuthenticationModule is taken in the COnnector class connect()
 * method. This test case uses config values as below that will point the Connector to 
 * use the AuthenticationModule.
 * 
 * NB - You WILL see a lot of red exception trace in the console log. This is fine, as it 
 * is a by-product of logging exceptions.
 * 
 *
 */
public class VMManagerAuthenticationModuleTest extends VmWareBaseTest{

	private Logger logger = LogManager.getLogger(VMManagerAuthenticationModuleTest.class);

	private static final String INVALID_AUTH_PARAMS_ERROR_MSG = 
			"Call to Authentication Module failed - AuthenticationException caught:AuthenticationParameters are not valid.";
	
	static AgnosticClientBridge bridge = null;
	static BridgeConfig bridgeConfig = null;
	
	@BeforeClass
	public static void setUp() throws Exception
	{
		CmdExecute job = controller.loadInstructionsForTest(new File("src/test/resources/skyscape/GENERIC_vApp_Deploy.xml"));
		
		bridge = new AgnosticClientBridge();
		bridgeConfig = new BridgeConfig();
		
		// Need to use the properties file determined by getConfigFilename() here so that Jenkins gets the proxy properties (otherwise the Connection will timeout)!
		bridgeConfig.setDefinitionXML(geographic);
		bridgeConfig.setInstructionXML(job);
	}
	
	private void executeSkyscapeCall(String filename) throws Exception {
		try {
			Properties props = new Properties();
			Reader configFile = new FileReader(filename);
			props.load(configFile);
			bridgeConfig.setProperties(props);
			bridge.execute(bridgeConfig);
			fail("VAppUnavailableControlException not caught as expected.");
		} catch (Exception udex) {
			if(udex.getCause() instanceof VAppUnavailableControlException && udex.getCause().getMessage().contains(vmc.getName())) {
				logger.debug("Expected error path, talked to skyscape");
			} else {
				throw udex;
			}
			
		}
	}

}
