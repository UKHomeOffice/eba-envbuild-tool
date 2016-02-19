package com.ipt.ebsa.agnostic.client.skyscape;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ipt.ebsa.agnostic.client.bridge.AgnosticClientBridge;
import com.ipt.ebsa.agnostic.client.bridge.BridgeConfig;
import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppUnavailableControlException;
import com.ipt.ebsa.agnostic.client.util.NamingUtils;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdCommand;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdDetail;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdVirtualApplication;
import com.ipt.ebsa.agnostic.cloud.command.v1.Execute;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.vmware.vcloud.sdk.VCloudException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SkyscapeClientBridgeTest extends VmWareBaseTest {

	@BeforeClass
	public static void setupClass() throws StrategyFailureException, VCloudException, InvalidStrategyException, ConnectionException,
			InterruptedException, UnresolvedDependencyException {
	}

	@AfterClass
	public static void tareDownClass() throws Exception {
		stopAndDelete(vmc);
		stopAndDelete(vmcAdditional);
	}

	/**
	 * Tests that we successfully create a connection to skyscape. Test cleans
	 * down, then runs a build that will throw an exception.
	 * 
	 * @throws Exception
	 */
	@Test
	public void atestBridgeVmcCommands() throws Exception {

		Properties stTestProperties = ConfigurationFactory.getProperties();
		CmdExecute job = getBaseInstruction();
		job.setEnvironmentContainer(getBaseEnvironmentContainerInstruction(CmdCommand.CONFIRM, CmdStrategy.EXISTS));
		CmdEnvironmentType env = getBaseEnvironmentInstruction(CmdCommand.CONFIRM, CmdStrategy.EXISTS);
		job.getEnvironmentContainer().setEnvironment(env);
		CmdVirtualApplication vmcCmd = getBasicVMC(CmdCommand.DEPLOY, CmdStrategy.EXISTS);
		env.getVirtualMachineContainer().add(vmcCmd);

		AgnosticClientBridge bridge = new AgnosticClientBridge();
		BridgeConfig config = new BridgeConfig();

		config.setDefinitionXML(geographic);
		config.setInstructionXML(job);
		config.setProperties(stTestProperties);

		try {
			bridge.execute(config);
			fail("VAppUnavailableControlException not caught as expected.");
		} catch (Exception udex) {
			assertTrue("VAppUnavailableControlException not caught as expected.", udex.getCause() instanceof VAppUnavailableControlException);
			assertTrue(udex.getCause().getMessage().contains(vmc.getName()));
		}

		try {
			vmcCmd.setCommand(CmdCommand.RESUME);
			vmcCmd.setStrategy(CmdStrategy.DOESNOTEXIST);
			bridge.execute(config);
			fail("VAppUnavailableControlException not caught as expected.");
		} catch (Exception udex) {
			assertTrue("VAppUnavailableControlException not caught as expected.", udex.getCause() instanceof VAppUnavailableControlException);
			assertTrue(udex.getCause().getMessage().contains(vmc.getName()));
		}

		try {
			vmcCmd.setCommand(CmdCommand.SUSPEND);
			vmcCmd.setStrategy(CmdStrategy.DOESNOTEXIST);
			bridge.execute(config);
			fail("VAppUnavailableControlException not caught as expected.");
		} catch (Exception udex) {
			assertTrue("VAppUnavailableControlException not caught as expected.", udex.getCause() instanceof VAppUnavailableControlException);
			assertTrue(udex.getCause().getMessage().contains(vmc.getName()));
		}

		try {
			vmcCmd.setCommand(CmdCommand.UNDEPLOY);
			vmcCmd.setStrategy(CmdStrategy.DOESNOTEXIST);
			bridge.execute(config);
			fail("VAppUnavailableControlException not caught as expected.");
		} catch (Exception udex) {
			assertTrue("VAppUnavailableControlException not caught as expected.", udex.getCause() instanceof VAppUnavailableControlException);
			assertTrue(udex.getCause().getMessage().contains(vmc.getName()));
		}

		vmcCmd.setCommand(CmdCommand.UPDATE_START_SECTION);
		vmcCmd.setStrategy(CmdStrategy.DOESNOTEXIST);
		bridge.execute(config);

		try {
			vmcCmd.setCommand(CmdCommand.START);
			vmcCmd.setStrategy(CmdStrategy.DOESNOTEXIST);
			bridge.execute(config);
			fail("VAppUnavailableControlException not caught as expected.");
		} catch (Exception udex) {
			assertTrue("VAppUnavailableControlException not caught as expected.", udex.getCause() instanceof VAppUnavailableControlException);
			assertTrue(udex.getCause().getMessage().contains(vmc.getName()));
		}

		try {
			vmcCmd.setCommand(CmdCommand.STOP);
			vmcCmd.setStrategy(CmdStrategy.DOESNOTEXIST);
			bridge.execute(config);
			fail("VAppUnavailableControlException not caught as expected.");
		} catch (Exception udex) {
			assertTrue("VAppUnavailableControlException not caught as expected.", udex.getCause() instanceof VAppUnavailableControlException);
			assertTrue(udex.getCause().getMessage().contains(vmc.getName()));
		}

		vmcCmd.setCommand(CmdCommand.CONFIRM);
		vmcCmd.setStrategy(CmdStrategy.DOESNOTEXIST);
		bridge.execute(config);

		vmcCmd.setCommand(CmdCommand.CREATE);
		vmcCmd.setStrategy(CmdStrategy.OVERWRITE);
		bridge.execute(config);

		vmcCmd.setCommand(CmdCommand.DELETE);
		vmcCmd.setStrategy(CmdStrategy.MERGE);
		bridge.execute(config);

	}

	/**
	 * Tests that we successfully create a connection to skyscape. Test cleans
	 * down, then runs a build that will throw an exception.
	 * 
	 * @throws Exception
	 */
	@Test
	public void btestBridgeVmCommands() throws Exception {
		vmcAdditional = vmc;
		geographic = getBaseTestGeographicContainer(true, testPrefixIdentAdditionalVpc);
		vmc = getBaseTestVirtualMachineContainer(true, testPrefixIdentAdditionalVpc);
		vm1 = getBaseTestVirtualMachine1(true, testPrefixIdentAdditionalVpc);
		appNet1 = getBaseTestAppNetwork1(true, testPrefixIdentAdditionalVpc);
		/* clean up test org */
		// String configFile =
		// "src/test/resources/skyscape/vmware-test-config.properties";
		// ConfigurationFactory.setConfigFile(new File(configFile));

		Properties stTestProperties = ConfigurationFactory.getProperties();
		CmdExecute job = getBaseInstruction();

		job.setEnvironmentContainer(getBaseEnvironmentContainerInstruction(CmdCommand.CONFIRM, CmdStrategy.EXISTS));
		CmdEnvironmentType env = getBaseEnvironmentInstruction(CmdCommand.CONFIRM, CmdStrategy.EXISTS);
		job.getEnvironmentContainer().setEnvironment(env);
		// CmdDetail orgNet1Create = getBasicCmdDetail(CmdCommand.CREATE,
		// CmdStrategy.MERGE, orgNetwork1.getName());
		CmdDetail orgNet1Confirm = getBasicCmdDetail(CmdCommand.CONFIRM,CmdStrategy.EXISTS, orgNetwork1.getName());
		// job.getEnvironmentContainer().getOrganisationNetwork().add(orgNet1Create);
		job.getEnvironmentContainer().getOrganisationNetwork().add(orgNet1Confirm);
		CmdVirtualApplication vmcCmd = getBasicVMC(CmdCommand.CREATE, CmdStrategy.CREATE_ONLY);
		env.getVirtualMachineContainer().add(vmcCmd);

		CmdDetail appNet1a = getBasicCmdDetail(CmdCommand.CREATE, CmdStrategy.CREATE_ONLY);
		appNet1a.setIncludes(appNet1.getName());
		vmcCmd.getApplicationNetwork().add(appNet1a);

		CmdDetail vm1a = getBasicCmdDetail(CmdCommand.CREATE, CmdStrategy.CREATE_ONLY, NamingUtils.getVmFQDN(vm1, vmc));
		vmcCmd.getVirtualMachine().add(vm1a);

		CmdDetail vm1b = getBasicCmdDetail(CmdCommand.CONFIRM, CmdStrategy.EXISTS, NamingUtils.getVmFQDN(vm1, vmc));
		vmcCmd.getVirtualMachine().add(vm1b);

		CmdDetail vm1h = getBasicCmdDetail(CmdCommand.CONFIRM_HDD, CmdStrategy.EXISTS, NamingUtils.getVmFQDN(vm1, vmc));
		vmcCmd.getVirtualMachine().add(vm1h);

		CmdDetail vm1c = getBasicCmdDetail(CmdCommand.START, CmdStrategy.EXISTS, NamingUtils.getVmFQDN(vm1, vmc));
		vmcCmd.getVirtualMachine().add(vm1c);

		CmdDetail vm1d = getBasicCmdDetail(CmdCommand.SUSPEND, CmdStrategy.EXISTS, NamingUtils.getVmFQDN(vm1, vmc));
		vmcCmd.getVirtualMachine().add(vm1d);

		CmdDetail vm1e = getBasicCmdDetail(CmdCommand.RESUME, CmdStrategy.EXISTS, NamingUtils.getVmFQDN(vm1, vmc));
		vmcCmd.getVirtualMachine().add(vm1e);

		CmdDetail vm1f = getBasicCmdDetail(CmdCommand.SHUTDOWN, CmdStrategy.EXISTS, NamingUtils.getVmFQDN(vm1, vmc));
		vmcCmd.getVirtualMachine().add(vm1f);

		CmdDetail vm1g = getBasicCmdDetail(CmdCommand.STOP, CmdStrategy.EXISTS, NamingUtils.getVmFQDN(vm1, vmc));
		vmcCmd.getVirtualMachine().add(vm1g);

		XMLNICType nic1 = getBaseTestVirtualMachineNIC2a(false, testPrefixIdent);
		nic1.setIndexNumber(new BigInteger("1"));
		vm1.getNIC().add(nic1);

		AgnosticClientBridge bridge = new AgnosticClientBridge();
		BridgeConfig config = new BridgeConfig();

		// Need to use the properties file determined by getConfigFilename()
		// here so that Jenkins gets the proxy properties (otherwise the
		// Connection will timeout)!
		config.setDefinitionXML(geographic);
		config.setInstructionXML(job);
		config.setProperties(stTestProperties);
		bridge.execute(config);

	}
}
