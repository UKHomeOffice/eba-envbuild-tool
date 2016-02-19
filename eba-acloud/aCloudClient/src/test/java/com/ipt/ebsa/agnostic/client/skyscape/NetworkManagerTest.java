package com.ipt.ebsa.agnostic.client.skyscape;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.vmware.vcloud.sdk.VCloudException;

public class NetworkManagerTest extends VmWareBaseTest {

	private static Logger logger = LogManager.getLogger(NetworkManagerTest.class);

	@BeforeClass
	public static void setupClass() throws StrategyFailureException, VCloudException, InvalidStrategyException, ConnectionException,
			InterruptedException, UnresolvedDependencyException {
		// create
		cloudManager.confirmVirtualMachineContainer(CmdStrategy.DOESNOTEXIST, null, vmc, null);
		cloudManager.createVirtualMachineContainer(CmdStrategy.OVERWRITE, environment, vmc, geographic);
		cloudManager.confirmVirtualMachineContainer(CmdStrategy.EXISTS, null, vmc, null);
	}

	@AfterClass
	public static void tareDownClass() throws Exception {
		stopAndDelete(vmc);
	}

	/**
	 * Create an org network
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateAssignRemoveOrgNetwork() throws Exception {
		try {
		cloudManager.createOrganisationNetwork(CmdStrategy.CREATE_ONLY, environment, orgNetwork1, geographic);
		} catch(StrategyFailureException e) {
			//If it exists it will throw this exception
		}
		cloudManager.confirmOrganisationNetwork(CmdStrategy.EXISTS, environment, orgNetwork1);
		cloudManager.createAssignOrgNetwork(CmdStrategy.CREATE_ONLY, environment, vmc, orgNetwork1);
		cloudManager.confirmAssignOrgNetwork(CmdStrategy.EXISTS, environment, vmc, orgNetwork1);
		cloudManager.deleteOrgNetworkFromVApp(vmc, orgNetwork1);
		cloudManager.confirmOrgNetworkInVApp(CmdStrategy.DOESNOTEXIST, vmc, orgNetwork1);
	}

	/**
	 * Create an org network
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateDeleteAppNetwork() throws Exception {

		cloudManager.confirmApplicationNetwork(CmdStrategy.DOESNOTEXIST, vmc, appNet1);
		cloudManager.createApplicationNetwork(CmdStrategy.CREATE_ONLY,null, vmc, appNet1, null);
		cloudManager.confirmApplicationNetwork(CmdStrategy.EXISTS, vmc, appNet1);
		cloudManager.deleteApplicationNetwork(vmc, appNet1);
		cloudManager.confirmApplicationNetwork(CmdStrategy.DOESNOTEXIST, vmc, appNet1);

	}
}
