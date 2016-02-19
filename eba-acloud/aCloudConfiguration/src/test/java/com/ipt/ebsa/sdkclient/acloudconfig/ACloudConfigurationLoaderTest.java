package com.ipt.ebsa.sdkclient.acloudconfig;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute.CmdEnvironmentContainer;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;


/**
 * Tests the ACloudSpecificationLoader
 *
 */
public class ACloudConfigurationLoaderTest {

	@Test
	public void testLoadVCSL2() throws Exception  {
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		XMLGeographicContainerType aCloud = loader.loadVC(new File("src/test/resources/testEnvironment.xml"));
		
		List<XMLVirtualMachineContainerType> type =  aCloud.getEnvironmentContainer().getEnvironment().get(0).getEnvironmentDefinition().get(0).getVirtualMachineContainer();
		for (XMLVirtualMachineContainerType application : type) {
			Assert.assertNotNull(application.getName());
			Assert.assertNotNull(application.getDescription());
			Assert.assertNotNull(application.getRuntimeLease());
			Assert.assertNotNull(application.getServiceLevel());
			Assert.assertNotNull(application.getStorageLease());			
		}
		
		Assert.assertNotNull(aCloud);
	}
	
	@Test
	public void testLoadJob() throws Exception  {
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		CmdExecute job = loader.loadJob(new File("src/test/resources/testCommandv2.xml"));
		CmdEnvironmentContainer geographic = job.getEnvironmentContainer();

		
//		for (CmdVirtualApplication appCmd : geographic.) {
//			
//			CmdCommand ac = appCmd.getCommand();
//			Assert.assertEquals(CmdCommand.CONFIRM, ac);
//			Assert.assertEquals(".*",appCmd.getExcludes());
//			Assert.assertEquals(".*", appCmd.getIncludes());
//			Assert.assertEquals(CmdStrategy.EXISTS,appCmd.getStrategy());
//			Assert.assertEquals(CmdErrorStrategy.EXIT,appCmd.getErrorStrategy());
//			
//			
//			List<CmdDetail> appNet = appCmd.getApplicationNetwork();
//			for (CmdDetail cmdDetail : appNet) {
//				CmdCommand command = cmdDetail.getCommand();
//				Assert.assertEquals(CmdCommand.CONFIRM, command);
//				Assert.assertEquals(".*",cmdDetail.getExcludes());
//				Assert.assertEquals(".*", cmdDetail.getIncludes());
//				Assert.assertEquals(CmdStrategy.EXISTS,cmdDetail.getStrategy());
//				Assert.assertEquals(CmdErrorStrategy.EXIT,cmdDetail.getErrorStrategy());
//			}
//			List<CmdDetail> orgNet = appCmd.getOrganisationNetwork();
//			for (CmdDetail cmdDetail : orgNet) {
//				CmdCommand command = cmdDetail.getCommand();
//				Assert.assertEquals(CmdCommand.CONFIRM, command);
//				Assert.assertEquals(".*",cmdDetail.getExcludes());
//				Assert.assertEquals(".*", cmdDetail.getIncludes());
//				Assert.assertEquals(CmdStrategy.EXISTS,cmdDetail.getStrategy());
//				Assert.assertEquals(CmdErrorStrategy.EXIT,cmdDetail.getErrorStrategy());
//			}
//			List<CmdDetail> machines = appCmd.getVirtualMachine();
//			for (CmdDetail cmdVm : machines) {
//				
//				CmdCommand vmc = cmdVm.getCommand();
//				Assert.assertEquals(CmdCommand.CREATE, vmc);
//				Assert.assertEquals(".*",cmdVm.getExcludes());
//				Assert.assertEquals(".*", cmdVm.getIncludes());
//				Assert.assertEquals(CmdStrategy.CREATE_ONLY,cmdVm.getStrategy());
//				Assert.assertEquals(CmdErrorStrategy.EXIT,cmdVm.getErrorStrategy());
//				
//			}
//		}
		
		Assert.assertNotNull(job);
	}

}
