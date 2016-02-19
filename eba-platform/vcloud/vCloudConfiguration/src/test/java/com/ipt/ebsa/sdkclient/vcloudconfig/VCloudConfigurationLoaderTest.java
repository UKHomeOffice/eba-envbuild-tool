package com.ipt.ebsa.sdkclient.vcloudconfig;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.skyscape.command.v2.CmdCommand;
import com.ipt.ebsa.skyscape.command.v2.CmdDetail;
import com.ipt.ebsa.skyscape.command.v2.CmdErrorStrategy;
import com.ipt.ebsa.skyscape.command.v2.CmdExecute;
import com.ipt.ebsa.skyscape.command.v2.CmdStrategy;
import com.ipt.ebsa.skyscape.command.v2.CmdVirtualApplication;
import com.ipt.ebsa.skyscape.config.v2.XMLOrganisationType;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualApplicationType;


/**
 * Tests the VCloudSpecificationLoader
 * @author Stephen Cowx
 *
 */
public class VCloudConfigurationLoaderTest {

	@Test
	public void testLoadVCSL2() throws Exception  {
		VCloudConfigurationLoader loader = new VCloudConfigurationLoader();
		XMLOrganisationType vCloud = loader.loadVC(new File("src/test/resources/testVCloudv2.xml"));
		
		
		List<XMLVirtualApplicationType> type =  vCloud.getEnvironment().getVirtualApplication();
		for (XMLVirtualApplicationType application : type) {
			Assert.assertNotNull(application.getName());
			Assert.assertNotNull(application.getDescription());
			Assert.assertNotNull(application.getRuntimeLease());
			Assert.assertNotNull(application.getServiceLevel());
			Assert.assertNotNull(application.getStorageLease());			
		}
		
		
		Assert.assertNotNull(vCloud);
	}
	
	@Test
	public void testLoadJob() throws Exception  {
		VCloudConfigurationLoader loader = new VCloudConfigurationLoader();
		CmdExecute job = loader.loadJob(new File("src/test/resources/testCommandv2.xml"));
		List<CmdVirtualApplication> applications = job.getEnvironment().getVirtualApplication();
		
		for (CmdVirtualApplication appCmd : applications) {
			
			CmdCommand ac = appCmd.getCommand();
			Assert.assertEquals(CmdCommand.CONFIRM, ac);
			Assert.assertEquals(".*",appCmd.getExcludes());
			Assert.assertEquals(".*", appCmd.getIncludes());
			Assert.assertEquals(CmdStrategy.EXISTS,appCmd.getStrategy());
			Assert.assertEquals(CmdErrorStrategy.EXIT,appCmd.getErrorStrategy());
			
			
			List<CmdDetail> appNet = appCmd.getApplicationNetwork();
			for (CmdDetail cmdDetail : appNet) {
				CmdCommand command = cmdDetail.getCommand();
				Assert.assertEquals(CmdCommand.CONFIRM, command);
				Assert.assertEquals(".*",cmdDetail.getExcludes());
				Assert.assertEquals(".*", cmdDetail.getIncludes());
				Assert.assertEquals(CmdStrategy.EXISTS,cmdDetail.getStrategy());
				Assert.assertEquals(CmdErrorStrategy.EXIT,cmdDetail.getErrorStrategy());
			}
			List<CmdDetail> orgNet = appCmd.getOrganisationNetwork();
			for (CmdDetail cmdDetail : orgNet) {
				CmdCommand command = cmdDetail.getCommand();
				Assert.assertEquals(CmdCommand.CONFIRM, command);
				Assert.assertEquals(".*",cmdDetail.getExcludes());
				Assert.assertEquals(".*", cmdDetail.getIncludes());
				Assert.assertEquals(CmdStrategy.EXISTS,cmdDetail.getStrategy());
				Assert.assertEquals(CmdErrorStrategy.EXIT,cmdDetail.getErrorStrategy());
			}
			List<CmdDetail> machines = appCmd.getVirtualMachine();
			for (CmdDetail cmdVm : machines) {
				
				CmdCommand vmc = cmdVm.getCommand();
				Assert.assertEquals(CmdCommand.CREATE, vmc);
				Assert.assertEquals(".*",cmdVm.getExcludes());
				Assert.assertEquals(".*", cmdVm.getIncludes());
				Assert.assertEquals(CmdStrategy.CREATE_ONLY,cmdVm.getStrategy());
				Assert.assertEquals(CmdErrorStrategy.EXIT,cmdVm.getErrorStrategy());
				
			}
		}
		
		Assert.assertNotNull(job);
	}

}
