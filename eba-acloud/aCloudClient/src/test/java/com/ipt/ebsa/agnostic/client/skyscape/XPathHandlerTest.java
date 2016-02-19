package com.ipt.ebsa.agnostic.client.skyscape;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.ipt.ebsa.AgnosticClientCLI;
import com.ipt.ebsa.agnostic.client.cli.AgnosticCliController;
import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.EnvironmentOverrideException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.util.NamingUtils;
import com.ipt.ebsa.agnostic.client.util.XPathHandler;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdCommand;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdDetail;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType.CmdOverrides;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType.CmdOverrides.CmdOverride;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdVirtualApplication;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.vmware.vcloud.sdk.VCloudException;

public class XPathHandlerTest extends VmWareBaseTest{

	private static Logger logger = LogManager.getLogger(XPathHandlerTest.class);

	private static final String CONFIG_FILE_UPDATE_ORDERING = "src/test/resources/aws/startupShutdownConfig.xml";
	private static final String XPATH_VM1_VMORDER_OVERRIDE = "/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm1']/VMOrder";
	private static final String XPATH_VM2_VMORDER_OVERRIDE = "/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm2']/VMOrder";
	private static final String XPATH_VM3_VMORDER_OVERRIDE = "/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm3']/VMOrder";
	private static final String XPATH_VM4_VMORDER_OVERRIDE = "/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm4']/VMOrder";

	private static final String START_ACTION_POWER_ON = "powerOn";
	private static final String START_ACTION_NONE = "none";
	private static final String STOP_ACTION_POWER_OFF = "powerOff";
	private static final String STOP_ACTION_SHUTDOWN = "guestShutdown";
	private static final String START_ACTION_XPATH_EXT = "VMStartAction";
	private static final String STOP_ACTION_XPATH_EXT = "VMStopAction";
	private static final String VM_ORDER_XPATH_EXT = "VMOrder";
	
	static File tmpConfig = null;
	static File tmpJob = null;
	
	// The class under test
	XPathHandler handler = null;
	static AgnosticCliController controller = null;

	@BeforeClass
	public static void setUpBeforeSkyscapeBaseTestClass() throws InterruptedException, StrategyFailureException, VCloudException, InvalidStrategyException, ConnectionException, UnresolvedDependencyException {
		controller = container.instance().select(AgnosticCliController.class).get();
		
		cloudManager.confirmVirtualMachineContainer(CmdStrategy.DOESNOTEXIST, null, vmc, null);
		cloudManager.createVirtualMachineContainer(CmdStrategy.OVERWRITE, environment, vmc, geographic);
		cloudManager.confirmVirtualMachineContainer(CmdStrategy.EXISTS, null, vmc, null);
		cloudManager.confirmApplicationNetwork(CmdStrategy.DOESNOTEXIST, vmc, appNet1);
		cloudManager.createApplicationNetwork(CmdStrategy.CREATE_ONLY, null, vmc, appNet1, null);
		cloudManager.confirmApplicationNetwork(CmdStrategy.EXISTS, vmc, appNet1);
	}

	@AfterClass
	public static void tareDownAfterAwsStartupShutdownTest() {
		stopAndDelete(vmc);
		try {
			FileUtils.deleteQuietly(new File(XPathHandler.DEFN_OVERRIDE_FILENAME));
			FileUtils.deleteQuietly(tmpConfig);
			FileUtils.deleteQuietly(tmpJob);
			
		} catch (Exception e) {

		}
	}

	/**
	 * Test case which asserts that when a Definition file is passed in with an
	 * existing VMOrder node present, this Node can be overridden by the the
	 * specification of an override. The resultant processed Definition file is
	 * written out with the updated Node value inserted.
	 * 
	 * @throws EnvironmentOverrideException
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testApplyEnvOverridesExistingVMOrderNodesOverridenVMOrderNode() throws EnvironmentOverrideException, SAXException, IOException {

		String definition = CONFIG_FILE_UPDATE_ORDERING;
		List<CmdOverride> overrides = new ArrayList<CmdOverride>();
		File processedDefinitionFile = new File(XPathHandler.DEFN_OVERRIDE_FILENAME);

		String value = "123";

		File result = null;

		CmdOverride overrideVm1 = new CmdOverride();
		overrideVm1.setValue(value);
		overrideVm1.setXpath(XPATH_VM1_VMORDER_OVERRIDE);

		CmdOverride overrideVm2 = new CmdOverride();
		overrideVm2.setValue(value);
		overrideVm2.setXpath(XPATH_VM2_VMORDER_OVERRIDE);

		CmdOverride overrideVm3 = new CmdOverride();
		overrideVm3.setValue(value);
		overrideVm3.setXpath(XPATH_VM3_VMORDER_OVERRIDE);

		CmdOverride overrideVm4 = new CmdOverride();
		overrideVm4.setValue(value);
		overrideVm4.setXpath(XPATH_VM4_VMORDER_OVERRIDE);

		overrides.add(overrideVm1);
		overrides.add(overrideVm2);
		overrides.add(overrideVm3);
		overrides.add(overrideVm4);

		handler = new XPathHandler();

		result = handler.applyEnvOverrides(definition, overrides, processedDefinitionFile);

		Assert.assertNotNull("Result file is null.", result);

		XMLGeographicContainerType geographicOriginal = controller.loadConfigurationForTest(new File(definition));
		XMLEnvironmentContainerType envContainerOriginal = geographicOriginal.getEnvironmentContainer();
		XMLEnvironmentType envOriginal = envContainerOriginal.getEnvironment().get(0);
		XMLVirtualMachineContainerType vmcOriginal = envOriginal.getEnvironmentDefinition().get(0).getVirtualMachineContainer().get(0);

		for (XMLVirtualMachineType vm : vmcOriginal.getVirtualMachine()) {
			Assert.assertEquals(new Integer(0), vm.getVMOrder());
			Assert.assertEquals(new Integer(0), vm.getVMStopDelay());
			Assert.assertEquals(new Integer(0), vm.getVMStartDelay());
			Assert.assertEquals("none", vm.getVMStartAction());
			Assert.assertEquals("guestShutdown", vm.getVMStopAction());
		}

		XMLGeographicContainerType geographicOverriden = controller.loadConfiguration(result);
		XMLEnvironmentContainerType envContainerOverriden = geographicOverriden.getEnvironmentContainer();
		XMLEnvironmentType envOverriden = envContainerOverriden.getEnvironment().get(0);
		XMLVirtualMachineContainerType vmcOverriden = envOverriden.getEnvironmentDefinition().get(0).getVirtualMachineContainer().get(0);

		for (XMLVirtualMachineType vm : vmcOverriden.getVirtualMachine()) {
			Assert.assertEquals(new Integer(123), vm.getVMOrder());
			Assert.assertEquals(new Integer(0), vm.getVMStopDelay());
			Assert.assertEquals(new Integer(0), vm.getVMStartDelay());
			Assert.assertEquals("none", vm.getVMStartAction());
			Assert.assertEquals("guestShutdown", vm.getVMStopAction());
		}
	}

	@Test
	public void testCreateVMsInOrder() throws SAXException, IOException {

		CmdExecute job = getBaseInstruction();

		job.setEnvironmentContainer(getBaseEnvironmentContainerInstruction(CmdCommand.CONFIRM, CmdStrategy.EXISTS));
		CmdEnvironmentType env = getBaseEnvironmentInstruction(CmdCommand.CONFIRM, CmdStrategy.EXISTS);
		job.getEnvironmentContainer().setEnvironment(env);
		CmdDetail orgNet1Confirm = getBasicCmdDetail(CmdCommand.CONFIRM,CmdStrategy.EXISTS, orgNetwork1.getName());

		job.getEnvironmentContainer().getOrganisationNetwork().add(orgNet1Confirm);
		CmdVirtualApplication vmcCmd = getBasicVMC(CmdCommand.CONFIRM, CmdStrategy.EXISTS, vmc.getName());
		env.getVirtualMachineContainer().add(vmcCmd);

		CmdDetail appNet1a = getBasicCmdDetail(CmdCommand.CONFIRM, CmdStrategy.EXISTS);
		appNet1a.setIncludes(appNet1.getName());
		vmcCmd.getApplicationNetwork().add(appNet1a);

		CmdDetail vm1a = getBasicCmdDetail(CmdCommand.CREATE, CmdStrategy.OVERWRITE, NamingUtils.getVmFQDN(vm1, vmc));
		vmcCmd.getVirtualMachine().add(vm1a);

		CmdDetail vm2a = getBasicCmdDetail(CmdCommand.CREATE, CmdStrategy.OVERWRITE, NamingUtils.getVmFQDN(vm2, vmc));
		vmcCmd.getVirtualMachine().add(vm2a);
		
		tmpConfig = controller.writeConfigurationForTest(geographic, "geographic.xml");
		tmpJob = controller.writeInstructionForTest(job, "job.xml");
		String configFile = ConfigurationFactory.getConfigFile();
		
		String[] createArgs = new String[] { "-command", "execute", "-config", configFile, "-definition",
				tmpConfig.getAbsolutePath(), "-executionplan", tmpJob.getAbsolutePath() };
		AgnosticClientCLI.main(createArgs);
		
		vmcCmd.getVirtualMachine().clear();
		
		vmcCmd = getBasicVMC(CmdCommand.UPDATE_START_SECTION, CmdStrategy.MERGE);
		env.getVirtualMachineContainer().add(vmcCmd);
		
		env.getVirtualMachineContainer().add(getBasicVMC(CmdCommand.START, CmdStrategy.EXISTS, vmc.getName()));
		CmdOverrides overides = new CmdOverrides();
		env.setOverrides(overides);
		overides.getOverride().add(createVmCommand(vmc.getName(), NamingUtils.getVmFQDN(vm1, vmc), VM_ORDER_XPATH_EXT, "1"));
		overides.getOverride().add(createVmCommand(vmc.getName(), NamingUtils.getVmFQDN(vm1, vmc), START_ACTION_XPATH_EXT, START_ACTION_POWER_ON));
		overides.getOverride().add(createVmCommand(vmc.getName(), NamingUtils.getVmFQDN(vm1, vmc), STOP_ACTION_XPATH_EXT, STOP_ACTION_POWER_OFF));
		overides.getOverride().add(createVmCommand(vmc.getName(), NamingUtils.getVmFQDN(vm2, vmc), VM_ORDER_XPATH_EXT, "2"));
		overides.getOverride().add(createVmCommand(vmc.getName(), NamingUtils.getVmFQDN(vm2, vmc), START_ACTION_XPATH_EXT, START_ACTION_POWER_ON));
		overides.getOverride().add(createVmCommand(vmc.getName(), NamingUtils.getVmFQDN(vm2, vmc), STOP_ACTION_XPATH_EXT, STOP_ACTION_POWER_OFF));
		
		env.getVirtualMachineContainer().add(getBasicVMC(CmdCommand.STOP, CmdStrategy.EXISTS, vmc.getName()));
		
		FileUtils.deleteQuietly(tmpConfig);
		FileUtils.deleteQuietly(tmpJob);
		
		tmpConfig = controller.writeConfigurationForTest(geographic, "geographic.xml");
		tmpJob = controller.writeInstructionForTest(job, "job.xml");
		
		String configFile2 = ConfigurationFactory.getConfigFile();
		
		String[] stopStartArgs = new String[] { "-command", "execute", "-config", configFile2, "-definition",
				tmpConfig.getAbsolutePath(), "-executionplan", tmpJob.getAbsolutePath() };
		AgnosticClientCLI.main(stopStartArgs);

	}

	private CmdOverride createVmCommand(String vmcName, String vmName, String extension, String value) {
		CmdOverride vm1order = new CmdOverride();
		vm1order.setXpath("/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='"+vmcName+"']/VirtualMachine[vmName='"+vmName+"']/"+extension);
		vm1order.setValue(value);
		return vm1order;
		
	}
}
