package com.ipt.ebsa.agnostic.client.skyscape;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.module.ControlModule.ControlAction;
import com.ipt.ebsa.agnostic.client.util.NamingUtils;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.vmware.vcloud.api.rest.schema.AvailableNetworksType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.admin.AdminOrgVdcNetwork;

public class SkyscapeCleanupTest extends VmWareBaseTest {

	private static Logger logger = LogManager.getLogger(SkyscapeCleanupTest.class);

	/**
	 * Test to clean up the test resources that are lingering after any unit
	 * tests have run
	 * 
	 * @throws VCloudException
	 * @throws ConnectionException
	 */
	@Test
	public void cleanup() throws VCloudException, ConnectionException {
		Vdc vDC = cv.getVdc();
		Collection<ReferenceType> vapps = vDC.getVappRefs();
		
//		Vapp vapp = vappModule.getVApp(cv.getClient(), vDC, vappName);
//		NetworkSectionNetwork network = vapp.getNetworkByName(oNet.getName());
//		organisationNetwork.setFenceMode(network.getHref())

		ArrayList<XMLOrganisationalNetworkType> orgNetworks = new ArrayList<XMLOrganisationalNetworkType>();
		AvailableNetworksType networks = vDC.getResource().getAvailableNetworks();
		for (ReferenceType network : networks.getNetwork()) {
			if (network.getName().contains("UNITTEST")) {
				AdminOrgVdcNetwork adminOrgVdcNetwork = AdminOrgVdcNetwork.getOrgVdcNetworkByReference(cv.getClient(), network);
				logger.debug("Found org network " + network.getName());
				XMLOrganisationalNetworkType organisationNetwork = new XMLOrganisationalNetworkType();
				organisationNetwork.setName(network.getName());
				organisationNetwork.setDataCenterId(getBaseTestDataCenter(false, testPrefixIdent));
				organisationNetwork.setFenceMode(adminOrgVdcNetwork.getConfiguration().getFenceMode().toUpperCase());
				orgNetworks.add(organisationNetwork);
			}
		}

		for (ReferenceType vapp : vapps) {
			if (vapp.getName().contains("UNITTEST")) {
				// remove test vapp
				logger.debug("Found vapp " + vapp.getName());
				XMLVirtualMachineContainerType delVapp = new XMLVirtualMachineContainerType();
				delVapp.setServiceLevel(vDC.getResource().getName());
				delVapp.setName(vapp.getName());

				try {
					logger.debug("Stopping vapp " + delVapp.getName());
					stopVmc(delVapp);
					logger.debug("Stopped vapp " + delVapp.getName());
				} catch (Exception e) {
					logger.error("Stopping vapp " + delVapp.getName(), e);
				}

				Vapp vmc = vappModule.getVApp(cv.getClient(), vDC, delVapp.getName());
				int counter = 30;
				do {
					for (VM vm : vmc.getChildrenVms()) {
						XMLVirtualMachineType vmConfig = new XMLVirtualMachineType();
						vmConfig.setVmName(vm.getResource().getName());
						logger.debug("Found vm " + vm.getResource().getName());
						try {
							logger.debug("Deleting vm " + vm.getResource().getName() + " with status " + vm.getVMStatus());
							vmManager.deleteVMFromVApp(delVapp, vmConfig);
						} catch (Exception e) {
							logger.error("Failed deleting vm " + NamingUtils.getVmFQDN(vmConfig, delVapp) + " from vapp " + delVapp.getName(), e);
							try {
								logger.debug("Powering off vm " + NamingUtils.getVmFQDN(vmConfig, delVapp) + " from vapp " + delVapp.getName());
								vmManager.controlVM(ControlAction.POWER_OFF, delVapp, vmConfig, true);
								logger.debug("Powwered off vm " + NamingUtils.getVmFQDN(vmConfig, delVapp) + " from vapp " + delVapp.getName());
							} catch (Exception e1) {
								logger.error("Powering off vm " + NamingUtils.getVmFQDN(vmConfig, delVapp) + " from vapp " + delVapp.getName(), e1);
							}
						}
					}
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					vmc = vappModule.getVApp(cv.getClient(), vDC, vapp.getName());
					counter--;

				} while (vmc.getChildrenVms().size() > 0 || (vmc.getChildrenVms().size() != 0 && counter <= 0));

				for (XMLOrganisationalNetworkType oNet : orgNetworks) {
					try {
						logger.debug("Deleting org network " + oNet.getName() + " from vapp " + vapp.getName());
						networkManager.deleteOrgNetworkFromVApp(delVapp, oNet);
						logger.debug("Deleted org network " + oNet.getName() + " from vapp " + vapp.getName());
					} catch (Exception e) {
						// Don't care as we just want to clean up the networks,
						// might not exist so ignore
						logger.error("Deleting org network " + oNet.getName() + " from vapp " + vapp.getName(), e);
					}
				}

				try {
					logger.debug("Deleting vapp " + vapp.getName());
					stopAndDelete(delVapp);
					logger.debug("Deleted vapp " + vapp.getName());
				} catch (Exception e) {
					logger.error("Deleting vapp " + vapp.getName(), e);
				}
			}
		}

		for (XMLOrganisationalNetworkType organisationNetwork : orgNetworks) {
			logger.debug("Deleting network " + organisationNetwork.getName());

			try {
				logger.debug("Deleting org network " + organisationNetwork.getName());
				networkManager.deleteOrganisationNetwork(organisationNetwork);
				logger.debug("Deleted network " + organisationNetwork.getName());
			} catch (Exception e) {
				logger.error("Deleting org network " + organisationNetwork.getName(), e);
			}

		}
	}

}
