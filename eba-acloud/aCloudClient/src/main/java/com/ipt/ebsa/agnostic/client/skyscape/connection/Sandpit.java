package com.ipt.ebsa.agnostic.client.skyscape.connection;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.config.Config;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.module.AdminModule;
import com.ipt.ebsa.agnostic.client.skyscape.module.VAppModule;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;

/**
 * 
 *
 */
public class Sandpit {

	private Logger logger = LogManager.getLogger(Sandpit.class);
	
	@Inject
	@Config
	private String edgeGatewayName;

	@Inject
	@Config
	private String externalNetworkName;
	
	@Inject
	@Config
	private String adminVdc;

	@Inject
	@Config
	private String filepath;
	
	@Inject
	@Config
	private String organisation;

	@Inject
	@Config
	private String vdc;

	@Inject
	@Config
	private String catalog;

	@Inject 
	private SkyscapeConnector connector;
	
	@Inject
	private AdminModule admninModule;
	
	@Inject 
	private VAppModule vappModule;

	/**
	 * Sandpit
	 */
	public void sandpit() {
		logger.debug("sandpit start");
		try {

			boolean deleteExisting = true;
			String vappName = "Stephen test vapp2";// vClConfig.getOrg().getEnvironment().getApplication().getName();
			VcloudClient vcloudClient = connector.connect();
			Organization targetOrganisation = admninModule.getOrganisation(vcloudClient, organisation);
			Vdc targetVdc = admninModule.getVDC(vcloudClient, targetOrganisation, vdc);

			// Create the vapp
			Vapp vapp = vappModule.getVApp(vcloudClient, targetVdc, vappName);
			if (vapp != null && deleteExisting) {
				vappModule.deleteVApp(vcloudClient, targetVdc, vapp);
			}
			if (vapp == null) {
				vapp = vappModule.createEmptyVApp(vcloudClient, targetOrganisation, targetVdc, vappName, "", false, false);
			}

			logger.debug("sandpit end");

			// AdminOrganization adminOrg =
			// adminModule.getAdminOrganisation(vcloudClient, organisation);
			// Organization org = refModule.getOrganisation(vcloudClient,
			// organisation);
			// Vdc vdcThingy = refModule.getVDC(vcloudClient, org, vdc);
			// ReferenceType externalNetRef =
			// refModule.getExternalNetworkRef(vcloudClient,
			// externalNetworkName);
			// System.out.println("External net ref " + externalNetRef.getId() +
			// "-" + externalNetRef.getHref());

		} catch (ConnectionException cex){
			cex.printStackTrace();
		}catch (VCloudException e) {
			e.printStackTrace();
		}

	}
}
