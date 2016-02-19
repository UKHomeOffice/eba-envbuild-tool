package com.ipt.ebsa.agnostic.client.skyscape.connection;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.ipt.ebsa.agnostic.client.config.Config;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.module.AdminModule;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;

/**
 * Container class used to hold some shared values for convenience
 * 
 *
 */
@Singleton
public class SkyscapeCloudValues {
	private VcloudClient client;

	@Inject
	private SkyscapeConnector connector;

	@Inject
	private AdminModule adminModule;

	@Inject
	@Config
	private String organisation;
	
	@Inject
	@Config
	private String vdc;

	@Inject 
	@Config
	private String edgeGatewayName;

	@Inject 
	@Config
	private String externalNetworkName;
	
	public String getEdgeGatewayName() {
		return edgeGatewayName;
	}

	public void setEdgeGatewayName(String edgeGatewayName) {
		this.edgeGatewayName = edgeGatewayName;
	}

	public String getExternalNetworkName() {
		return externalNetworkName;
	}

	public void setExternalNetworkName(String externalNetworkName) {
		this.externalNetworkName = externalNetworkName;
	}
	
	/**
	 * Initialise and/or return a CloudValues object
	 * 
	 * @return
	 * @throws VCloudException
	 * @throws ConnectionException 
	 */
	private void init() throws VCloudException, ConnectionException {
		if (client == null) {
			setClient(connector.connect());			
		}
	}

	public VcloudClient getClient() throws VCloudException, ConnectionException {
		init();
		return client;
	}

	private void setClient(VcloudClient client) {
		this.client = client;
	}

	public Organization getOrg() throws VCloudException, ConnectionException {
		init();
		return adminModule.getOrganisation(client, organisation);
	}
	
	public Vdc getVdc(String vdcName) throws VCloudException, ConnectionException {
		init();
		return adminModule.getVDC(client, getOrg(), vdcName);
	}
	
	public Vdc getVdc() throws VCloudException, ConnectionException {
		init();
		return adminModule.getVDC(client, getOrg(), vdc);
	}

	public String getOrganisationAsStr() {
		return organisation;
	}
}
