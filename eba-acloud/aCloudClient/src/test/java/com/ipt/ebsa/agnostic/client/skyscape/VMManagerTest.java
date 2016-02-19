package com.ipt.ebsa.agnostic.client.skyscape;

import java.math.BigInteger;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdCommand;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.vmware.vcloud.sdk.VCloudException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VMManagerTest extends VmWareBaseTest {

	private static Logger logger = LogManager.getLogger(VMManagerTest.class);

	@BeforeClass
	public static void setupClass() throws StrategyFailureException, VCloudException, InvalidStrategyException, ConnectionException,
			InterruptedException, UnresolvedDependencyException {
		getBaseTestGeographicContainer(true, testPrefixIdent);
		// create
		cloudManager.confirmVirtualMachineContainer(CmdStrategy.DOESNOTEXIST, null, vmc, null);
		cloudManager.createVirtualMachineContainer(CmdStrategy.OVERWRITE, environment, vmc, geographic);
		cloudManager.confirmVirtualMachineContainer(CmdStrategy.EXISTS, null, vmc, null);
		cloudManager.confirmApplicationNetwork(CmdStrategy.DOESNOTEXIST, vmc, appNet1);
		cloudManager.createApplicationNetwork(CmdStrategy.CREATE_ONLY, null, vmc, appNet1, null);
		logger.debug("Created test app network "+appNet1.getName());
		cloudManager.confirmApplicationNetwork(CmdStrategy.EXISTS, vmc, appNet1);
	}

	@AfterClass
	public static void tareDownClass() throws Exception {
		stopAndDelete(vmc);
	}

	/**
	 * Create, start, stop, delete
	 * 
	 * @throws Exception
	 */
	@Test
	public void atestCreateAppNetCreateVmUpdateStartStopDeleteVM() throws Exception {
		try {
			cloudManager.confirmApplicationNetwork(CmdStrategy.EXISTS, vmc, appNet1);
			cloudManager.confirmVirtualMachine(CmdStrategy.DOESNOTEXIST, vmc, vm1);
			cloudManager.createVirtualMachine(getBaseInstruction(CmdCommand.CREATE, CmdStrategy.CREATE_ONLY), CmdStrategy.CREATE_ONLY, null, vmc, vm1, null);
			cloudManager.confirmVirtualMachine(CmdStrategy.EXISTS, vmc, vm1);
			cloudManager.confirm_hddVirtualMachine(CmdStrategy.EXISTS, vmc, vm1);

			vm1.setCpuCount(vm1.getCpuCount().add(BigInteger.ONE));
			vm1.getStorage().add(getBaseTestVirtualMachineStorage2a(false, testPrefixIdent));
			vm1.getNIC().add(getBaseTestVirtualMachineNIC2a(true, testPrefixIdent));
			cloudManager.updateVirtualMachine(CmdStrategy.MERGE, vmc, vm1);

			cloudManager.confirm_hddVirtualMachine(CmdStrategy.EXISTS, vmc, vm1);
			cloudManager.startVirtualMachine(vmc, vm1);
			cloudManager.stopVirtualMachine(vmc, vm1);

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void btestCreateVmNoCustStartStopVappVM() throws Exception {
		try {

			cloudManager.confirmVirtualMachine(CmdStrategy.DOESNOTEXIST, vmc, vm2);
			cloudManager.createVirtualMachine(getBaseInstruction(CmdCommand.CREATE, CmdStrategy.CREATE_ONLY), CmdStrategy.CREATE_ONLY, null, vmc, vm2, null);
			cloudManager.confirmVirtualMachine(CmdStrategy.EXISTS, vmc, vm2);
			cloudManager.confirm_hddVirtualMachine(CmdStrategy.EXISTS, vmc, vm2);
			vm2.getNIC().get(0).getInterface().get(0).setStaticIpAddress("10.9.1.13");
			vm2.getStorage().get(0).getSize().add(vm2.getStorage().get(0).getSize());
			cloudManager.updateVirtualMachine(CmdStrategy.MERGE, vmc, vm2);
			cloudManager.confirm_hddVirtualMachine(CmdStrategy.EXISTS, vmc, vm2);
			cloudManager.updateVirtualMachine(CmdStrategy.OVERWRITE, vmc, vm2);
			cloudManager.confirm_hddVirtualMachine(CmdStrategy.EXISTS, vmc, vm2);
			cloudManager.update_start_sectionVirtualMachineContainer(vmc);
			cloudManager.startVirtualMachine(vmc, vm2);
			cloudManager.suspendVirtualMachine(vmc, vm2);
			cloudManager.resumeVirtualMachine(vmc, vm2);
			cloudManager.stopVirtualMachine(vmc, vm2);

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void ctestVMCStartStopSuspendDiscard() throws Exception {
		try {

			cloudManager.update_start_sectionVirtualMachineContainer(vmc);
			cloudManager.startVirtualMachineContainer(vmc);
			cloudManager.suspendVirtualMachineContainer(vmc);
			cloudManager.resumeVirtualMachineContainer(vmc);
			cloudManager.undeployVirtualMachineContainer(vmc);
			cloudManager.deployVirtualMachineContainer(vmc);
			cloudManager.startVirtualMachineContainer(vmc);
			// cloudManager.rebootVirtualMachineContainer(vmc);

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void dtestDeleteVMFromVmc() throws Exception {
		try {
			cloudManager.deleteVirtualMachine(vmc, vm2);
			vmc.getVirtualMachine().remove(1);

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

}
