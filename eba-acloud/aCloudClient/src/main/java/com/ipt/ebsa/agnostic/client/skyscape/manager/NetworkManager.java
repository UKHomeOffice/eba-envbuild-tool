package com.ipt.ebsa.agnostic.client.skyscape.manager;

import java.util.ArrayList;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.skyscape.connection.SkyscapeCloudValues;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.skyscape.module.NetModule;
import com.ipt.ebsa.agnostic.client.skyscape.module.VAppModule;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler.Action;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLApplicationNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.vmware.vcloud.api.rest.schema.AvailableNetworksType;
import com.vmware.vcloud.api.rest.schema.IpAddressesType;
import com.vmware.vcloud.api.rest.schema.IpRangesType;
import com.vmware.vcloud.api.rest.schema.IpScopeType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.ovf.NetworkSectionNetwork;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.Vdc;

/**
 * 
 *
 */
public class NetworkManager {

	private Logger logger = LogManager.getLogger(NetworkManager.class);
	
	@Inject 
	private StrategyHandler strategyHandler;
	
	@Inject
	private NetModule networkModule;
	
	@Inject
	private VAppModule vappModule;
	
	@Inject
	private SkyscapeCloudValues cv;
	
	@Inject
	private EdgeGatewayManager edgeGatewayManager;

	/**
	 * Assign an organisation network into a vApp given the strategy and VApp parameters
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 */
	public void createOrgNetworkInVApp(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLOrganisationalNetworkType orgNetwork) throws StrategyFailureException,
			UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("createOrgNetworkInVApp entry - strategy:" + strategy);
		
		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		String networkName = orgNetwork.getName();

		Vapp vapp = resolveVAppDependency(cv, vDC, vappName, "Organisation network '" + networkName + "'");
		NetworkSectionNetwork network = vapp.getNetworkByName(networkName);
		Action action = strategyHandler.resolveCreateStrategy(strategy, network, "Organisation network", networkName," in Vapp '"+vappName+"' ");
		switch (action) {
		case DESTROY_THEN_CREATE:
			logger.info("Deleting Organisation network '" + networkName + "'.");
			networkModule.deleteNetworkFromVApp(vDC, vapp, networkName);
			logger.info("Organisation network '" + networkName + "' deleted.");
		case CREATE:
			logger.info("Creating Organisation network '" + networkName + "'.");
			networkModule.createOrganisationNetworkInVApp(vDC, vapp, networkName, orgNetwork.getFenceMode());
			logger.info("Organisation network '" + networkName + "' created.");
		default:
			break;
		}

		logger.debug("createOrgNetworkInVApp exit");
	}
	
	/**
	 * Create an organisation network given the strategy
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 */
	public void createOrganisationNetwork(CmdStrategy cmdStrategy, XMLOrganisationalNetworkType organisationNetwork) throws StrategyFailureException, UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException {

		logger.debug("createOrganisationNetwork entry - strategy: " + cmdStrategy);
		Organization organisation = cv.getOrg();
		Vdc vDC = cv.getVdc(organisationNetwork.getDataCenterId().getName());
		String name = organisationNetwork.getName();
		ReferenceType existingOrganisationNetworkRefType = findOrganisationNetworkByName(vDC, name);
		Action action = strategyHandler.resolveCreateStrategy(cmdStrategy, existingOrganisationNetworkRefType, "Organisation network", name," within the Organisation'");
		switch (action) {
			case DESTROY_THEN_CREATE:
				logger.info("Deleting Organisation network '" + name + "'.");
				networkModule.deleteOrganisationNetwork(cv.getClient(), organisation, existingOrganisationNetworkRefType, name);
				logger.info("Organisation network '" + name + "' deleted.");
			case CREATE:
				logger.info("Creating Organisation network '" + organisationNetwork.getName() + "'.");
				XMLGatewayType gateway = (XMLGatewayType)organisationNetwork.getGatewayId();
				String edgeGatewayName = "";
				if(gateway != null) {
					edgeGatewayName = gateway.getName();
				} 

				ReferenceType edgeGatewayReference = edgeGatewayManager.findEdgeGatewayReferenceByName(vDC, edgeGatewayName);
			
				String description = organisationNetwork.getDescription();
				boolean share = organisationNetwork.isShared();
			
				String fenceMode = organisationNetwork.getFenceMode();
				String gatewayAddress = organisationNetwork.getGatewayAddress();
				String networkMask = organisationNetwork.getNetworkMask();
				String primaryDNS = organisationNetwork.getPrimaryDns();
				String secondaryDNS = organisationNetwork.getSecondaryDns();
				String dnsSuffix = organisationNetwork.getDnsSuffix();
				
				String[][] rangeAddresses = new String[1][2];
	        	rangeAddresses[0][0] = organisationNetwork.getIpRangeStart();
	        	rangeAddresses[0][1] = organisationNetwork.getIpRangeEnd();
				
				IpRangesType rangesType = networkModule.getIpRangesType(rangeAddresses);
				IpScopeType ipScope = networkModule.getIPScopeType(null, rangesType, primaryDNS, secondaryDNS, dnsSuffix, gatewayAddress, networkMask, true, false);
	    	
				networkModule.createOrganisationNetwork(vDC, edgeGatewayReference, fenceMode, name, description, share, ipScope);
			
				logger.info("Organisation network '" + name + "' created.");
				break;
			case UPDATE:
				logger.info("Updating Organisation network '" + organisationNetwork.getName() + "'.");
				String errorMessage = "You cannot update an organisation network, you must delete and then recreate. There is nothing useful we can update so you must recreate a new instance.";
				logger.error(errorMessage);
				throw new InvalidStrategyException(errorMessage);
			default:
				break;
		}

		logger.debug("createOrgNetworkInVApp exit");
	}
	
	/**
	 * Create an application network given the strategy and VApp parameters
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 */
	public void createAppNetworkInVApp(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLApplicationNetworkType appNetwork) throws StrategyFailureException,
			UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("createOrgAppNetworkInVApp entry - strategy:" + strategy);
		
		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		String networkName = appNetwork.getName();

		Vapp vapp = resolveVAppDependency(cv, vDC, vappName, "Application network '" + networkName + "'");
		NetworkSectionNetwork network = vapp.getNetworkByName(networkName);
		Action action = strategyHandler.resolveCreateStrategy(strategy, network, "Application network", networkName," in Vapp '"+vappName+"' ");
		switch (action) {
		case DESTROY_THEN_CREATE:
			logger.info("Deleting Application network '" + networkName + "'.");
			networkModule.deleteNetworkFromVApp(vDC, vapp, networkName);
			logger.info("Application network '" + networkName + "' deleted.");
		case CREATE:
			logger.info("Creating Application network '" + networkName + "'.");
			
        	String gatewayAddress = appNetwork.getGatewayAddress();
        	String networkMask = appNetwork.getNetworkMask();
        	String primaryDNS = appNetwork.getPrimaryDns();
        	String secondaryDNS = appNetwork.getSecondaryDns();
        	String dnsSuffix = appNetwork.getDnsSuffix();
        	String addressRange = appNetwork.getStaticIpPool();
        	String[][] rangeAddresses = new String[1][2];
        	rangeAddresses[0][0] = appNetwork.getIpRangeStart();
        	rangeAddresses[0][1] = appNetwork.getIpRangeEnd();
        	
        	ArrayList<String> ipAddresses = new ArrayList<String>();
        	ipAddresses.add(addressRange);
			IpAddressesType allocatedIPAddresses = networkModule.getIpAddressesType(ipAddresses);
			IpRangesType ranges = networkModule.getIpRangesType(rangeAddresses);
        	IpScopeType ipScope = networkModule.getIPScopeType(allocatedIPAddresses, ranges, primaryDNS, secondaryDNS, dnsSuffix, gatewayAddress,networkMask, true, false);
        	networkModule.createApplicationNetwork(vDC, vapp, networkName, appNetwork.getFenceMode(), ipScope);
			logger.info("Application network '" + networkName + "' created.");
		default:
			break;
		}

		logger.debug("createAppNetworkInVApp exit");
	}


	/**
	 * Delete an assignement of an organisation network into a vApp given the strategy and VApp parameters
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @return
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 * @throws UnstrategyManager.resolvedDependencyException
	 */
	public void deleteOrgNetworkFromVApp(XMLVirtualMachineContainerType vappConfig, XMLOrganisationalNetworkType orgNetwork) throws StrategyFailureException,
			VCloudException, InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		logger.debug("deleteOrgNetworkFromVApp entry");
        deleteNamedNetworkFromVApp(vappConfig,  orgNetwork.getName(), "Organisation");
		logger.debug("deleteOrgNetworkFromVApp exit");
	}
	
	/**
	 * Delete an organisation network given the strategy
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @return
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 * @throws UnstrategyManager.resolvedDependencyException
	 */
	public void deleteOrganisationNetwork(XMLOrganisationalNetworkType organisationNetwork) throws StrategyFailureException, VCloudException, InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		logger.debug("deleteOrganisationNetwork entry");
        deleteNamedNetwork(organisationNetwork);
		logger.debug("deleteOrganisationNetwork exit");
	}
	
	/**
	 * Delete a Network given the strategy and VApp parameters
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @return
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 * @throws UnstrategyManager.resolvedDependencyException
	 */
	public void deleteAppNetworkFromVApp(XMLVirtualMachineContainerType vappConfig, XMLApplicationNetworkType appNetwork) throws StrategyFailureException,
			VCloudException, InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		logger.debug("deleteAppNetworkFromVApp entry");
        deleteNamedNetworkFromVApp(vappConfig, appNetwork.getName(), "Application");
		logger.debug("deleteAppNetworkFromVApp exit");
	}
	
	/**
	 * Delete a VApp given the strategy and VApp parameters
	 * 
	 * @param vappConfig
	 * @return
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 * @throws UnstrategyManager.resolvedDependencyException
	 */
	public void deleteNamedNetworkFromVApp(XMLVirtualMachineContainerType vappConfig, String networkName, String networkType) throws StrategyFailureException,
			VCloudException, InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		
		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		
		Vapp vapp = resolveVAppDependency(cv, vDC, vappName, networkType + " network '" + networkName + "'");
		NetworkSectionNetwork network = vapp.getNetworkByName(networkName);

		Action action = strategyHandler.resolveDeleteStrategy(network, networkType + " network", networkName, " from Vapp '" + vappName + "' ");
		switch (action) {
		case DELETE:
			logger.info("Deleting "+networkType + " network '" + networkName + "'.");
			networkModule.deleteNetworkFromVApp(vDC, vapp, networkName);
			logger.info(networkType + " network '" + networkName + "' deleted.");
			break;
		default:
			break;
		}
	}

	/**
	 * Delete an organisation network given the strategy
	 * 
	 * @param organisationNetwork
	 * @param networkName
	 * @param networkType
	 * @return
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 * @throws UnstrategyManager.resolvedDependencyException
	 */
	public void deleteNamedNetwork(XMLOrganisationalNetworkType organisationNetwork) throws StrategyFailureException, VCloudException, InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		
		Organization organisation = cv.getOrg();
		Vdc vDC = cv.getVdc(organisationNetwork.getDataCenterId().getName());
		String name = organisationNetwork.getName();
		String fenceMode = organisationNetwork.getFenceMode();
		
		ReferenceType existingOrganisationNetworkRefType = findOrganisationNetworkByName(vDC, name);
		Action action = strategyHandler.resolveDeleteStrategy(existingOrganisationNetworkRefType, fenceMode + " organisation network", name, " within the Organisation'");

		switch (action) {
			case DELETE:
				logger.info("Deleting "+ fenceMode + " organisation network '" + name + "'.");
				networkModule.deleteOrganisationNetwork(cv.getClient(),organisation,  existingOrganisationNetworkRefType, name);
				logger.info(fenceMode + " organisation network '" + name + "' deleted.");
				break;
			default:
				break;
		}
	}
	
	/**
	 * Confirms the organisation network is assigned into the vApp
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 * @throws UnstrategyManager.resolvedDependencyException
	 */
	public void confirmOrgNetworkInVApp(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLOrganisationalNetworkType orgNetwork) throws StrategyFailureException, VCloudException, InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		logger.debug("confirmOrgNetworkInVApp entry - strategy:" + strategy);
		confirmNamedNetworkInVAppExists( strategy, vappConfig, orgNetwork.getName(), "Organisation");
		logger.debug("confirmOrgNetworkInVApp exit");
	}

	/**
	 * Confirms the organisation network is present
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 * @throws UnstrategyManager.resolvedDependencyException
	 */
	public void confirmOrganisationNetwork(CmdStrategy cmdStrategy, XMLOrganisationalNetworkType organisationNetwork) throws StrategyFailureException, VCloudException, InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		logger.debug("confirmOrganisationNetwork entry - strategy: " + cmdStrategy);
		confirmNamedNetworkExists(cmdStrategy, organisationNetwork);
		logger.debug("confirmOrganisationNetwork exit");
	}
	
	/**
	 * Confirms something about the VApp
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 * @throws UnstrategyManager.resolvedDependencyException
	 */
	public void confirmAppNetworkInVApp(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLApplicationNetworkType appNetwork) throws StrategyFailureException,
			VCloudException, InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		logger.debug("confirmAppNetworkInVApp entry - strategy:" + strategy);
		confirmNamedNetworkInVAppExists( strategy, vappConfig, appNetwork.getName(), "Application");
		logger.debug("confirmAppNetworkInVApp exit");
	}

    /**
     * Shared confirm function
     * @param strategyHandler
     * @param strategy
     * @param vappConfig
     * @param networkName
     * @param networkType
     * @throws VCloudException
     * @throws UnresolvedDependencyException
     * @throws StrategyFailureException
     * @throws InvalidStrategyException
     * @throws ConnectionException 
     */
	private void confirmNamedNetworkInVAppExists(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, String networkName, String networkType) throws VCloudException, UnresolvedDependencyException,
			StrategyFailureException, InvalidStrategyException, ConnectionException {
		
		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		
		Vapp vapp = resolveVAppDependency(cv, vDC, vappName, networkType + " network '" + networkName + "'");
		NetworkSectionNetwork network = vapp.getNetworkByName(networkName);
		// This always returns the same thing, we are only waiting for possible
		// exceptions
		strategyHandler.resolveConfirmStrategy(strategy, network, networkType + " network", networkName, " in Vapp '" + vappName + "' ");		
	}

    /**
     * Shared confirm function
     * @param strategyHandler
     * @param strategy
     * @param vappConfig
     * @param networkName
     * @param networkType
     * @throws VCloudException
     * @throws UnresolvedDependencyException
     * @throws StrategyFailureException
     * @throws InvalidStrategyException
     * @throws ConnectionException 
     */
	private void confirmNamedNetworkExists(CmdStrategy cmdStrategy, XMLOrganisationalNetworkType organisationNetwork) throws VCloudException, UnresolvedDependencyException, StrategyFailureException, InvalidStrategyException, ConnectionException {
		String name = organisationNetwork.getName();
		String networkType = organisationNetwork.getFenceMode();
		Vdc vDC = cv.getVdc(organisationNetwork.getDataCenterId().getName());
		ReferenceType existingOrganisationNetworkRefType = findOrganisationNetworkByName(vDC, name);
		// This always returns the same thing, we are only waiting for possible exceptions
		strategyHandler.resolveConfirmStrategy(cmdStrategy, existingOrganisationNetworkRefType, networkType + " organisation network ", name, " within the Organisation'");		
	}

	/**
	 * Looks for a vapp and throws an appropriate error if it does not exist
	 * 
	 * @param cv
	 * @param vappName
	 * @param networkName
	 * @return
	 * @throws VCloudException
	 * @throws UnresolvedDependencyException
	 * @throws ConnectionException 
	 */
	public Vapp resolveVAppDependency(SkyscapeCloudValues cv, Vdc vDC, String vappName, String message) throws VCloudException, UnresolvedDependencyException, ConnectionException {
		Vapp vapp = vappModule.getVApp(cv.getClient(), vDC, vappName);
		if (vapp == null) {
			throw new UnresolvedDependencyException("Cannot create " + message + " in VApp '" + vappName + "' as there is no VApp with that name.");
		}
		return vapp;
	}
	
	
	/**
	 * Find an Organisation Network given its name
	 * 
	 * @param vDC
	 * @param organisationNetworkName
	 * @return
	 * @throws VCloudException
	 */
	private ReferenceType findOrganisationNetworkByName(Vdc vDC, String organisationNetworkName) throws VCloudException {
		AvailableNetworksType avaliableNetworks = vDC.getResource().getAvailableNetworks();
		if (avaliableNetworks != null) {
			for (ReferenceType availableNetworkReferenceType : avaliableNetworks.getNetwork()) {
				if (availableNetworkReferenceType.getName().equals(organisationNetworkName)) {
					return availableNetworkReferenceType;
				}
			}
		}
		// It is acceptable to not find any
		return null;
	}
	
}