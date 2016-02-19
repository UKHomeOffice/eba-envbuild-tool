package com.ipt.ebsa.agnostic.client.skyscape.module;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.vmware.vcloud.api.rest.schema.IpAddressesType;
import com.vmware.vcloud.api.rest.schema.IpRangeType;
import com.vmware.vcloud.api.rest.schema.IpRangesType;
import com.vmware.vcloud.api.rest.schema.IpScopeType;
import com.vmware.vcloud.api.rest.schema.IpScopesType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.OrgVdcNetworkType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.VAppNetworkConfigurationType;
import com.vmware.vcloud.sdk.OrgVdcNetwork;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.admin.AdminOrgVdcNetwork;
import com.vmware.vcloud.sdk.constants.FenceModeValuesType;

/**
 * Provides generally useful, stateless method calls for working with Networks
 *
 */
public class NetModule {

	private Logger logger = LogManager.getLogger(NetModule.class);
	
	/**
	 * Removes a network from a VAPP
	 * @param vdc
	 * @param vapp
	 * @param networkName
	 * @throws VCloudException
	 */
	public void deleteNetworkFromVApp(Vdc vdc, Vapp vapp, String networkName) throws VCloudException {
		logger.debug(String.format("Deleting network '%s' from vapp '%s' within vdc '%s'", networkName, vapp.getReference().getName(), vdc.getReference().getName()));
		
		/* Find the network config to remove */
		boolean found = false;
		NetworkConfigSectionType networkConfigSection = vapp.getNetworkConfigSection();
		List<VAppNetworkConfigurationType> networkConfig2 = networkConfigSection.getNetworkConfig();
		for (VAppNetworkConfigurationType vappNetConfig : networkConfig2) {
			if (vappNetConfig.getNetworkName().equals(networkName)){
				logger.debug(String.format("Found network '%s' removing it from the configuration", networkName));
				/* Remove it */
				networkConfigSection.getNetworkConfig().remove(vappNetConfig);
				found = true;
				break;
			}
		}
		
		if (!found) {
			logger.debug(String.format("Could not found network with the name '%s', no action will be taken.", networkName));
		}
		else {
			updateVApp(vapp, networkName, networkConfigSection);
		}
	}

	/**
	 * Removes an organisation network
	 * @param organisaion
	 * @param name
	 * @throws VCloudException
	 */
	public void deleteOrganisationNetwork(VcloudClient client, Organization organisation, ReferenceType organisationNetwork, String name) throws VCloudException {
		logger.debug(String.format("Deleting network '%s' from organisation '%s'", name, organisation.toString()));
		/* DELETE the network*/
		
		//  Route all VCloud REST calls through a retry mechanism - this call should use TaskUtil
		new TaskUtil().waitForTask(AdminOrgVdcNetwork.delete(client, organisationNetwork));
		
		logger.debug(String.format("Deleted network '%s' from organisation '%s'", name, organisation.toString()));
	}
	
    /**
	 * Creates an organisation Network
	 * @param vdc
	 * @param vapp
	 * @param networkName
	 * @param type
	 * @throws VCloudException
	 */
	public void createOrganisationNetwork(Vdc vDC, ReferenceType edgeGatewayReference, String fenceMode, String name, String description, boolean isShared, IpScopeType ipScopeType) throws VCloudException {
		logger.debug(String.format("Creating organisation network '%s' of type '%s' within edge gateway '%s'", name, fenceMode, edgeGatewayReference != null ? edgeGatewayReference.getName() : "No Edge Gateway"));
		
		// Create organisation network type
		OrgVdcNetworkType organisationNetworkConfig= new ObjectFactory().createOrgVdcNetworkType();
		if(edgeGatewayReference != null) {
			organisationNetworkConfig.setEdgeGateway(edgeGatewayReference);
		}
		organisationNetworkConfig.setName(name);
		organisationNetworkConfig.setDescription(description);
		organisationNetworkConfig.setIsShared(isShared);
		
		// Create network configuration type
		NetworkConfigurationType networkConfigurationType = new ObjectFactory().createNetworkConfigurationType();
		
		networkConfigurationType.setFenceMode(FenceModeValuesType.valueOf(fenceMode).value());
		
	    // Create IP scopes type - setIpScope(ipScope); do not use this....https://communities.vmware.com/message/2240605
		IpScopesType ipScopesType = new ObjectFactory().createIpScopesType();
		ipScopesType.getIpScope().add(ipScopeType);
		
		networkConfigurationType.setIpScopes(ipScopesType);	
		organisationNetworkConfig.setConfiguration(networkConfigurationType);
	
		//  - Route all VCloud REST calls through a retry mechanism
		new RestCallUtil().processVdcRestCall(vDC, organisationNetworkConfig);
	
		logger.debug(String.format("Finished creating organisation network '%s'", name));
	}
	
	/**
     * Creates an organisation Network in the VApp provided
     * @param vdc
     * @param vapp
     * @param networkName
     * @param type
     * @throws VCloudException
     */
	public void createOrganisationNetworkInVApp(Vdc vdc, Vapp vapp, String networkName, String fenceModeType) throws VCloudException {
		logger.debug(String.format("Creating network '%s' of type '%s' in vapp '%s' within vdc '%s'", networkName, fenceModeType, vapp.getReference().getName(), vdc.getReference().getName()));
		
		FenceModeValuesType type = FenceModeValuesType.valueOf(fenceModeType);
		
		/* Variables we will be using */
		ReferenceType organisationNetwork = vdc.getAvailableNetworkRefByName(networkName);
		
		/* Create network configuration */
		NetworkConfigurationType networkConfig= new ObjectFactory().createNetworkConfigurationType();
		networkConfig.setFenceMode(type.value());		
		networkConfig.setParentNetwork(organisationNetwork);		
		
		/*Roll the network configuration into a VApp network configuration */		
		VAppNetworkConfigurationType vAppCnfig = new ObjectFactory().createVAppNetworkConfigurationType();
		vAppCnfig.setNetworkName(networkName);
		vAppCnfig.setConfiguration(networkConfig);
		
		/* Get the existing networkConfig section */
		NetworkConfigSectionType networkConfigSection = vapp.getNetworkConfigSection();
		networkConfigSection.getNetworkConfig().add(vAppCnfig);

		/* Use the section to update the VApp */
		updateVApp(vapp, networkName, networkConfigSection);
		
		logger.debug(String.format("Finished creating network '%s'",networkName));
	}

	
	
	 /**
     * Creates an organisation Network in the VApp provided
     * @param vdc
     * @param vapp
     * @param networkName
     * @param type
     * @throws VCloudException
     */
	public void createApplicationNetwork(Vdc vdc, Vapp vapp, String networkName, String fenceModeType, IpScopeType ipScope) throws VCloudException {
		logger.debug(String.format("Creating network '%s' of type '%s' in vapp '%s' within vdc '%s'", networkName, fenceModeType, vapp.getReference().getName(), vdc.getReference().getName()));
		
		FenceModeValuesType type = FenceModeValuesType.valueOf(fenceModeType);
		
		/* Create network configuration */
		NetworkConfigurationType networkConfig= new ObjectFactory().createNetworkConfigurationType();
		networkConfig.setFenceMode(type.value());		
        // networkConfig.setIpScope(ipScope); do not use this....https://communities.vmware.com/message/2240605
		IpScopesType ipScopes = new ObjectFactory().createIpScopesType();
		ipScopes.getIpScope().add(ipScope);
		networkConfig.setIpScopes(ipScopes);	
		
		
		/*Roll the network configuration into a VApp network configuration */
		VAppNetworkConfigurationType vAppCnfig = new ObjectFactory().createVAppNetworkConfigurationType();
		vAppCnfig.setNetworkName(networkName);
		vAppCnfig.setConfiguration(networkConfig);
		
		/* Get the existing networkConfig section */
		NetworkConfigSectionType networkConfigSection = vapp.getNetworkConfigSection();
		networkConfigSection.getNetworkConfig().add(vAppCnfig);

		/* Use the section to update the VApp */
		updateVApp(vapp, networkName, networkConfigSection);
		
		logger.debug(String.format("Finished creating network '%s'",networkName));
	}

    /**
	 * Updates an organisation Network
	 * @param vdc
	 * @param vapp
	 * @param networkName
	 * @param type
	 * @throws VCloudException
	 */
	public void updateOrganisationNetwork(VcloudClient vCloudClient, Vdc vDC, OrgVdcNetwork existingOrganisationNetwork, String name) throws VCloudException {
		logger.debug(String.format("Updating organisation network '%s' of type '%s' within edge gateway '%s'", name, "X", "Y"));
		
		OrgVdcNetworkType existingOrganisationNetworkType = existingOrganisationNetwork.getResource();
		if (!name.equals(existingOrganisationNetworkType.getName())) {
			existingOrganisationNetworkType.setName(name);
		}
		
		AdminOrgVdcNetwork adminOrgVdcNetwork = AdminOrgVdcNetwork.getOrgVdcNetworkByReference(vCloudClient, existingOrganisationNetwork.getReference());
		adminOrgVdcNetwork.updateOrgVdcNetwork(existingOrganisationNetworkType);
	
		logger.debug(String.format("Finished creating organisation network '%s'", name));
	}
	
	/**
	 * Shared method for updating the VApp
	 * @param vapp
	 * @param networkName
	 * @param networkConfigSection
	 * @throws VCloudException
	 */
	private void updateVApp(final Vapp vapp, String networkName, final NetworkConfigSectionType networkConfigSection) throws VCloudException {
		/* Use the section to update the VApp */
		logger.debug(String.format("Updating VApp with changes %s.", networkName));
		new TaskUtil().waitForTask( vapp.updateSection(networkConfigSection));
		logger.debug(String.format("FInished updating VApp with changes. %s", networkName));
	}

	/**
	 * Creates and returns an IpAddressesType object
	 * @param ipAddresses
	 * @return
	 */
    public IpAddressesType getIpAddressesType(List<String> ipAddresses) {
    	IpAddressesType allocatedIPAddresses = new ObjectFactory().createIpAddressesType();
		for (String ipAddress : ipAddresses) {
			allocatedIPAddresses.getIpAddress().add(ipAddress);
		}
		return allocatedIPAddresses;
    }
    
    public IpRangesType getIpRangesType(String[][] startAndEndAddresses) {
    	IpRangesType ranges = new ObjectFactory().createIpRangesType();
		
		for (int i = 0; i < startAndEndAddresses.length; i++) {
			IpRangeType range = new ObjectFactory().createIpRangeType();
			range.setStartAddress(startAndEndAddresses[i][0]);
			range.setEndAddress(startAndEndAddresses[i][1]);
			ranges.getIpRange().add(range);
		}		
		
		return ranges;
    }

	/**
	 * Creates and returns an IpScopeType object
	 * @param allocatedIPAddresses
	 * @param dns1
	 * @param dns2
	 * @param dnsSuffix
	 * @param gateway
	 * @return
	 */
	public IpScopeType getIPScopeType(IpAddressesType allocatedIPAddresses, IpRangesType ranges, String dns1, String dns2, String dnsSuffix, String gateway, String netmask, boolean enabled, boolean inherited ) {
		logger.debug(String.format("Creating network scope - allocatedIPAddresses:'%s', dns1:'%s', dns2:'%s', dnsSuffix:'%s', gateway:'%s', netmask:'%s', enabled:'%s', inherited:'%s' ", allocatedIPAddresses, dns1, dns2, dnsSuffix, gateway, netmask, enabled, inherited));
		
		IpScopeType ipScope = new ObjectFactory().createIpScopeType();
		if (allocatedIPAddresses !=  null) {
		   ipScope.setAllocatedIpAddresses(allocatedIPAddresses);
		}
		ipScope.setDns1(dns1);
		ipScope.setDns2(dns2);
		ipScope.setDnsSuffix(dnsSuffix);
		ipScope.setGateway(gateway);
		ipScope.setNetmask(netmask);
		ipScope.setIsEnabled(enabled);
		ipScope.setIsInherited(inherited);		
		if (ranges != null) {
		   ipScope.setIpRanges(ranges);
		}
		
		return ipScope;
	}
}