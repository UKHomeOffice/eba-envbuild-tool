package com.ipt.ebsa.agnostic.client.aws;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Vpc;
import com.ipt.ebsa.AgnosticClientCLI;
import com.ipt.ebsa.agnostic.client.aws.extensions.IptInstanceStatus;
import com.ipt.ebsa.agnostic.client.aws.module.AwsEnvironmentModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsVmModule;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.client.cli.AgnosticCliController;
import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;
import com.ipt.ebsa.agnostic.client.exception.InvalidConfigurationException;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.EnvironmentOverrideException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.util.XPathHandler;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType.CmdOverrides.CmdOverride;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.vmware.vcloud.sdk.VCloudException;

public class AwsStartupShutdownTest extends AwsBaseTest {

	private static Logger logger = LogManager.getLogger(AwsStartupShutdownTest.class);

	private static final String CONFIG_FILE_UPDATE_ORDERING = "src/test/resources/aws/startupShutdownConfig.xml";
	private static final String XPATH_VM1_VMORDER_OVERRIDE = "/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm1']/VMOrder";
	private static final String XPATH_VM2_VMORDER_OVERRIDE = "/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm2']/VMOrder";
	private static final String XPATH_VM3_VMORDER_OVERRIDE = "/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm3']/VMOrder";
	private static final String XPATH_VM4_VMORDER_OVERRIDE = "/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm4']/VMOrder";

	// The class under test
	XPathHandler handler = null;
	static AgnosticCliController controller = null;

	@BeforeClass
	public static void setUpBeforeAwsBaseTestClass() throws InterruptedException {
		container = weld.initialize();
		controller = container.instance().select(AgnosticCliController.class).get();
		envModule = container.instance().select(AwsEnvironmentModule.class).get();
		vmModule = container.instance().select(AwsVmModule.class).get();
	}

	@AfterClass
	public static void tareDownAfterAwsStartupShutdownTest() {
		try {
			FileUtils.deleteQuietly(new File(XPathHandler.DEFN_OVERRIDE_FILENAME));
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
	public void testCreateVMsInOrder() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException, SAXException, IOException, StrategyFailureException, VCloudException, InvalidStrategyException,
			UnresolvedDependencyException, ConnectionException, InvalidConfigurationException, EnvironmentOverrideException, ToManyResultsException,
			UnSafeOperationException {
		ConfigurationFactory.getProperties().put("executionplan", "src/test/resources/aws/createAll.xml");
		ConfigurationFactory.getProperties().put("definition", "src/test/resources/aws/startupShutdownConfig.xml");
		controller.loadConfigurationForTest(new File("src/test/resources/aws/startupShutdownConfig.xml"));
		controller.loadInstructionsForTest(new File("src/test/resources/aws/createAll.xml"));
		controller.execute();
		XMLGeographicContainerType geographic = controller.loadConfiguration(new File("src/test/resources/aws/startupShutdownConfig.xml"));
		XMLEnvironmentContainerType envContainer = geographic.getEnvironmentContainer();
		XMLEnvironmentType env = envContainer.getEnvironment().get(0);
		XMLVirtualMachineContainerType vmc = env.getEnvironmentDefinition().get(0).getVirtualMachineContainer().get(0);

		XMLVirtualMachineType virtualMachine1 = null;
		XMLVirtualMachineType virtualMachine2 = null;
		XMLVirtualMachineType virtualMachine3 = null;
		XMLVirtualMachineType virtualMachine4 = null;

		for (XMLVirtualMachineType vm : vmc.getVirtualMachine()) {
			if (vm.getVmName().endsWith("1")) {
				virtualMachine1 = vm;
			} else if (vm.getVmName().endsWith("2")) {
				virtualMachine2 = vm;
			} else if (vm.getVmName().endsWith("3")) {
				virtualMachine3 = vm;
			} else if (vm.getVmName().endsWith("4")) {
				virtualMachine4 = vm;
			}
		}

		Vpc vpc = vmModule.getVpcByName(AwsNamingUtil.getEnvironmentName(env));
		testVpcId = vpc.getVpcId();
		Instance vm1 = vmModule.getInstance(env, virtualMachine1, vmc, vpc);
		Instance vm2 = vmModule.getInstance(env, virtualMachine2, vmc, vpc);
		Instance vm3 = vmModule.getInstance(env, virtualMachine3, vmc, vpc);
		Instance vm4 = vmModule.getInstance(env, virtualMachine4, vmc, vpc);

		vmModule.waitForInstanceStatus(vm1.getInstanceId(), InstanceStateName.Running, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm1.getInstanceId()) == IptInstanceStatus.Running);
		vmModule.waitForInstanceStatus(vm2.getInstanceId(), InstanceStateName.Running, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm2.getInstanceId()) == IptInstanceStatus.Running);
		vmModule.waitForInstanceStatus(vm3.getInstanceId(), InstanceStateName.Running, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm3.getInstanceId()) == IptInstanceStatus.Running);
		vmModule.waitForInstanceStatus(vm4.getInstanceId(), InstanceStateName.Running, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm4.getInstanceId()) == IptInstanceStatus.Running);

		String[] stopArgs = new String[] { "-command", "execute", "-config", "src/test/resources/aws/aws-test-config.properties", "-definition",
				"src/test/resources/aws/startupShutdownConfig.xml", "-executionplan", "src/test/resources/aws/stopAllVapp.xml" };
		AgnosticClientCLI.main(stopArgs);
		// controller.loadConfigurationForTest(new
		// File("src/test/resources/aws/startupShutdownConfig.xml"));
		// controller.loadInstructionsForTest(new
		// File("src/test/resources/aws/stopAllVapp.xml"));
		// controller.execute();

		vmModule.waitForInstanceStatus(vm1.getInstanceId(), InstanceStateName.Stopped, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm1.getInstanceId()) == IptInstanceStatus.Stopped);
		vmModule.waitForInstanceStatus(vm2.getInstanceId(), InstanceStateName.Stopped, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm2.getInstanceId()) == IptInstanceStatus.Stopped);
		vmModule.waitForInstanceStatus(vm3.getInstanceId(), InstanceStateName.Stopped, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm3.getInstanceId()) == IptInstanceStatus.Stopped);
		vmModule.waitForInstanceStatus(vm4.getInstanceId(), InstanceStateName.Stopped, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm4.getInstanceId()) == IptInstanceStatus.Stopped);

		// controller.loadConfigurationForTest(new
		// File("src/test/resources/aws/startupShutdownConfig.xml"));
		// controller.loadInstructionsForTest(new
		// File("src/test/resources/aws/startAllVms.xml"));
		// controller.execute();

		String[] startArgs = new String[] { "-command", "execute", "-config", "src/test/resources/aws/aws-test-config.properties", "-definition",
				"src/test/resources/aws/startupShutdownConfig.xml", "-executionplan", "src/test/resources/aws/startAllVms.xml" };
		AgnosticClientCLI.main(startArgs);

		vmModule.waitForInstanceStatus(vm1.getInstanceId(), InstanceStateName.Running, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm1.getInstanceId()) == IptInstanceStatus.Running);
		vmModule.waitForInstanceStatus(vm2.getInstanceId(), InstanceStateName.Running, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm2.getInstanceId()) == IptInstanceStatus.Running);
		vmModule.waitForInstanceStatus(vm3.getInstanceId(), InstanceStateName.Running, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm3.getInstanceId()) == IptInstanceStatus.Running);
		vmModule.waitForInstanceStatus(vm4.getInstanceId(), InstanceStateName.Running, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm4.getInstanceId()) == IptInstanceStatus.Running);

		vmModule.deleteVirtualMachine(vm1);
		vmModule.deleteVirtualMachine(vm2);
		vmModule.deleteVirtualMachine(vm3);
		vmModule.deleteVirtualMachine(vm4);
		
		vmModule.waitForInstanceStatus(vm1.getInstanceId(), InstanceStateName.Terminated, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm1.getInstanceId()) == IptInstanceStatus.Terminated);
		vmModule.waitForInstanceStatus(vm2.getInstanceId(), InstanceStateName.Terminated, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm2.getInstanceId()) == IptInstanceStatus.Terminated);
		vmModule.waitForInstanceStatus(vm3.getInstanceId(), InstanceStateName.Terminated, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm3.getInstanceId()) == IptInstanceStatus.Terminated);
		vmModule.waitForInstanceStatus(vm4.getInstanceId(), InstanceStateName.Terminated, false);
		Assert.assertTrue(vmModule.getInstanceStatus(vm4.getInstanceId()) == IptInstanceStatus.Terminated);
	}

}
