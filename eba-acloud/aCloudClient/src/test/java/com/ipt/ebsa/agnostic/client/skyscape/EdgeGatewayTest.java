package com.ipt.ebsa.agnostic.client.skyscape;

/**
 * This test class is not used currently as the EdgeGateway code is not complete. It is dangerous to run as
 * it can take out the entire organisation if it goes wrong so its been stopped on purpose as its not possible
 * to test this in an organisation that is not isolated.
 * 
 *
 */
public class EdgeGatewayTest extends VmWareBaseTest {

	
	/**
	 * Create an edge gateway
	 * 
	 * @throws Exception
	 */
	//@Test
//	public void testCreateEdgeGateway() throws Exception {
//		Assert.fail("This is dangerous, dont run these tests as they can delete live config.");
//		try {
//
//			XMLGeographicContainerType org = controller.loadConfiguration(new File("src/test/resources/organisation_network_config.xml"));
//			CmdExecute job = controller.loadInstructions(new File("src/test/resources/create_edge_gateway_job.xml"));		
//			
//			// Read the environment instruction file into memory
//			CmdDetail cmdDetail = job.getConfiguration().getEdgeGateway().get(0);
//
//			// Read the environment configuration file into memory
//			XMLEdgeGatewayType edgeGateway1 = org.getConfiguration().getEdgeGateway().get(0);
//
//			// Reconfigure the edgegateway
//			cloudManager.configureEdgeGatewayServices(cmdDetail.getStrategy(), edgeGateway1);
//			
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw e;
//		}
//		
//	}
}
