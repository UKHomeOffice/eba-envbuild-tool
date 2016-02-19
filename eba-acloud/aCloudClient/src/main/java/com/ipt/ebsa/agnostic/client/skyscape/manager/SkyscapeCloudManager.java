package com.ipt.ebsa.agnostic.client.skyscape.manager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.ipt.ebsa.agnostic.client.exception.FatalException;
import com.ipt.ebsa.agnostic.client.exception.RetryableException;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.manager.CloudManager;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppStartupSectionUpdateException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VmBusyException;
import com.ipt.ebsa.agnostic.client.skyscape.module.ControlModule.ControlAction;
import com.ipt.ebsa.agnostic.client.skyscape.module.IntrospectionModule;
import com.ipt.ebsa.agnostic.client.util.NamingUtils;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLApplicationNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.microsoft.windowsazure.exception.ServiceException;
import com.vmware.vcloud.sdk.VCloudException;

/**
 * Contains logic for delegating cloud requests to lower level components, its a facade
 * 
 *
 */
public class SkyscapeCloudManager extends CloudManager implements ISkyscapeManager{

	@Inject
	private EdgeGatewayManager edgeGatewayManager;

	@Inject
	private IntrospectionModule introspectionModule;

	private Logger logger = LogManager.getLogger(SkyscapeCloudManager.class);

	@Inject
	private NetworkManager networkManager;
	
	@Inject
	private VAppManager vappManager;

	@Inject
	private VMManager vmManager;

	public void configureEdgeGatewayServices(CmdStrategy strategy, XMLGatewayType edgeGatewayConfig) throws StrategyFailureException,
		UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException {
		edgeGatewayManager.configureServices(strategy, edgeGatewayConfig);
	}

	public void confirmApplicationNetwork(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLApplicationNetworkType appNetwork) throws StrategyFailureException, VCloudException,
			InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		networkManager.confirmAppNetworkInVApp(strategy, vappConfig, appNetwork);
	}

	@Override
	public void confirmOrganisationNetwork(CmdStrategy strategy,
			XMLEnvironmentType envConfig,
			XMLOrganisationalNetworkType orgNetworkType) throws StrategyFailureException, UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("Started createAssignOrgNetwork");
		logger.debug("orgNetworkTypeName="+orgNetworkType.getName());
		logger.debug("orgNetworkTypeDescription="+orgNetworkType.getDescription());
		logger.debug("orgNetworkTypeCIDR="+orgNetworkType.getCIDR());
		networkManager.confirmOrganisationNetwork(strategy, orgNetworkType);
		logger.debug("Finished createAssignOrgNetwork");	
	}

	public void confirmEnvironment(CmdStrategy strategy, XMLEnvironmentType envConfig, XMLGeographicContainerType geographic)
			throws InterruptedException, StrategyFailureException, InvalidStrategyException, ToManyResultsException, UnSafeOperationException,
			ExecutionException, ServiceException, IOException, ParserConfigurationException, SAXException, TransformerException, URISyntaxException,
			NullPointerException, RetryableException, FatalException {
		logger.info("confirmEnvironment is not possible for this cloud provider");
		
	}

	public void confirm_hddVirtualMachine(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws StrategyFailureException, VCloudException,
		InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		vmManager.confirmHDDsInVM(strategy, vappConfig, virtualMachine);
	}

	public void confirmOrgNetworkInVApp(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLOrganisationalNetworkType orgNetwork) throws StrategyFailureException, VCloudException,
			InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		networkManager.confirmOrgNetworkInVApp(strategy, vappConfig, orgNetwork);
	}

	public void confirmVirtualMachine(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws StrategyFailureException, VCloudException,
			InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		vmManager.confirmVMInVApp(strategy, vappConfig, virtualMachine);
	}
	
	public void confirmHddVirtualMachine(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws StrategyFailureException, VCloudException,
	InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		vmManager.confirmHDDsInVM(strategy, vappConfig, virtualMachine);
	}
	
	@Override
	public void confirmVirtualMachineContainer(CmdStrategy strategy, XMLEnvironmentType envConfig, XMLVirtualMachineContainerType vmc,
			XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException, InvalidStrategyException, VCloudException, ConnectionException {
		logger.debug("Started confirmVirtualMachineContainer");
		logger.debug("vmcName="+vmc.getName());
		logger.debug("vmcDescription="+vmc.getDescription());
		vappManager.confirmVApp(strategy, vmc);
		logger.debug("Finished confirmVirtualMachineContainer");
	}
	
	@Override
	public void createApplicationNetwork(CmdStrategy strategy,
			XMLEnvironmentType envConfig,
			XMLVirtualMachineContainerType vmc,
			XMLApplicationNetworkType appNetworkType, XMLGeographicContainerType geographic) throws StrategyFailureException, UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("Started createApplicationNetwork");
		logger.debug("appNetworkTypeName="+appNetworkType.getName());
		logger.debug("appNetworkTypeDescription="+appNetworkType.getDescription());
		logger.debug("appNetworkTypeCIDR="+appNetworkType.getCIDR());
		networkManager.createAppNetworkInVApp(strategy, vmc, appNetworkType);
		logger.debug("Finished createApplicationNetwork");	
	}

	@Override
	public void createAssignOrgNetwork(CmdStrategy strategy,
			XMLEnvironmentType envConfig,
			XMLVirtualMachineContainerType vmc,
			XMLOrganisationalNetworkType orgNetworkType) throws StrategyFailureException, UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("Started createAssignOrgNetwork");
		logger.debug("orgNetworkTypeName="+orgNetworkType.getName());
		logger.debug("orgNetworkTypeDescription="+orgNetworkType.getDescription());
		logger.debug("orgNetworkTypeCIDR="+orgNetworkType.getCIDR());
		networkManager.createOrgNetworkInVApp(strategy, vmc, orgNetworkType);
		logger.debug("Finished createAssignOrgNetwork");	
	}

	@Override
	public void createEdgeGateway(CmdStrategy strategy,
			XMLEnvironmentType envConfig, XMLGatewayType gatewayConfig) {
		logger.info("createEnvironmentContainer is not possible for this cloud provider");	
	}
	
	@Override
	public void createEnvironment(CmdStrategy strategy, XMLEnvironmentType envConfig, XMLGeographicContainerType geographic)
			throws InterruptedException, StrategyFailureException, InvalidStrategyException, ToManyResultsException, UnSafeOperationException,
			ExecutionException, ServiceException, IOException, ParserConfigurationException, SAXException, TransformerException, URISyntaxException,
			NullPointerException, RetryableException, FatalException {
		logger.info("createEnvironment is not possible for this cloud provider");
		
	}
	@Override
	public void createEnvironmentContainer(CmdStrategy strategy,
			XMLEnvironmentContainerType envConfig, XMLGeographicContainerType geographic) {
		logger.info("createEnvironmentContainer is not possible for this cloud provider");	
	}
	
	@Override
	public void createOrganisationNetwork(CmdStrategy strategy,
			XMLEnvironmentType envConfig,
			XMLOrganisationalNetworkType orgNetworkConfig, XMLGeographicContainerType geographic ) throws StrategyFailureException, UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("Started createOrganisationNetwork");	
		logger.debug("orgNetworkConfigName="+orgNetworkConfig.getName());
		logger.debug("orgNetworkConfigCIDR="+orgNetworkConfig.getCIDR());
		logger.debug("orgNetworkConfigDescription="+orgNetworkConfig.getDescription());
		networkManager.createOrganisationNetwork(strategy, orgNetworkConfig);
		logger.debug("Finished createOrganisationNetwork");
		
	}
	
	@Override
	public void createVirtualMachine(CmdExecute execute, CmdStrategy strategy,
			XMLEnvironmentType environment,
			XMLVirtualMachineContainerType vappConfig,
			XMLVirtualMachineType virtualMachine, XMLGeographicContainerType geographic) throws StrategyFailureException, UnresolvedDependencyException, VCloudException, InvalidStrategyException, IOException, ConnectionException {
		logger.debug("Started createVirtualMachine");
		logger.debug("virtualMachineComputerName="+NamingUtils.getComputerNameFQDN(virtualMachine, vappConfig));
		logger.debug("virtualMachineDescription="+virtualMachine.getDescription());
		logger.debug("virtualMachineVMName="+NamingUtils.getVmFQDN(virtualMachine, vappConfig));
		vmManager.createVMInVApp(execute, strategy, vappConfig, virtualMachine);
		logger.debug("Finished createVirtualMachine");
		
	}
	
	@Override
	public void createVirtualMachineContainer(CmdStrategy strategy,
			XMLEnvironmentType envConfig,
			XMLVirtualMachineContainerType vmc, XMLGeographicContainerType geographic) throws StrategyFailureException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("Started createVirtualMachineContainer");
		logger.debug("vmcName="+vmc.getName());
		logger.debug("vmcDescription="+vmc.getDescription());
		vappManager.createVApp(strategy, vmc);
		logger.debug("Finished createVirtualMachineContainer");
		
	}

	public void deleteApplicationNetwork(XMLVirtualMachineContainerType vappConfig, XMLApplicationNetworkType appNetwork) throws StrategyFailureException, VCloudException,
			InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		networkManager.deleteAppNetworkFromVApp(vappConfig, appNetwork);
	}

	public void deleteOrgNetworkFromVApp(XMLVirtualMachineContainerType vappConfig, XMLOrganisationalNetworkType orgNetwork) throws StrategyFailureException, VCloudException,
			InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		networkManager.deleteOrgNetworkFromVApp(vappConfig, orgNetwork);
	}

	public void deleteVirtualMachine(XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws StrategyFailureException, VCloudException,
			InvalidStrategyException, UnresolvedDependencyException, ConnectionException {
		vmManager.deleteVMFromVApp(vappConfig, virtualMachine);
	}

	public void deleteVirtualMachineContainer(XMLEnvironmentType envConfig,
			XMLVirtualMachineContainerType vmc, XMLGeographicContainerType geographic) throws StrategyFailureException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("Started createVirtualMachineContainer");
		logger.debug("vmcName="+vmc.getName());
		logger.debug("vmcDescription="+vmc.getDescription());
		vappManager.deleteVApp(vmc);
		logger.debug("Finished createVirtualMachineContainer");
		
	}

//	public void createVirtualMachine(CmdExecute execute, CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws StrategyFailureException, UnresolvedDependencyException,
//			VCloudException, InvalidStrategyException, IOException, ConnectionException {
//		vmManager.createVMInVApp(execute, strategy, vappConfig, virtualMachine);
//	}
	
	public void deployVirtualMachineContainer(XMLVirtualMachineContainerType vmc) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException {
		logger.debug("Started deployVirtualMachineContainer");
		logger.debug("vmcName="+vmc.getName());
		logger.debug("vmcDescription="+vmc.getDescription());
		vmManager.controlVApp(ControlAction.DEPLOY, vmc, true);
		logger.debug("Finished deployVirtualMachineContainer");
		
	}

	/**
	 * introspect
	 */
	public void introspect() {
		logger.debug("introspect start");
		introspectionModule.introspect();
		logger.debug("introspect end");

	}

	public void rebootVirtualMachine(XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VmBusyException {
		 vmManager.controlVM(ControlAction.REBOOT, vappConfig, virtualMachine, true);
	}
	
	public void rebootVirtualMachineContainer(XMLVirtualMachineContainerType vappConfig) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException {
		 vmManager.controlVApp(ControlAction.REBOOT, vappConfig, true);
	}
	
	public void resumeVirtualMachine(XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VmBusyException {
		 vmManager.controlVM(ControlAction.DISCARD_SUSPEND, vappConfig, virtualMachine, true);
	}
	
	public void resumeVirtualMachineContainer(XMLVirtualMachineContainerType vappConfig) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VmBusyException {
		vmManager.controlVApp(ControlAction.DISCARD_SUSPEND, vappConfig, true);
	}
	
	@Override
	public void sandbox() {
		
	}
	
	public void shutdownVirtualMachine(XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VmBusyException {
		 vmManager.controlVM(ControlAction.SHUTDOWN, vappConfig, virtualMachine, true);
	}
	
	public void shutdownVirtualMachineContainer(XMLVirtualMachineContainerType vappConfig) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VAppStartupSectionUpdateException {
		/**  - Ensure VM ordering is always consistent when shutting down a vApp */
		vappManager.updateStartupSection(vappConfig);   
		vmManager.controlVApp(ControlAction.SHUTDOWN, vappConfig, true);
	}
	
	public void startVirtualMachine(XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VmBusyException {
		 vmManager.controlVM(ControlAction.POWER_ON, vappConfig, virtualMachine, true);
	}
	
	public void startVirtualMachineContainer(XMLVirtualMachineContainerType vappConfig) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VAppStartupSectionUpdateException {
		/**  - Ensure VM ordering is always consistent when starting and stopping a vApp */
		vappManager.updateStartupSection(vappConfig);  
		vmManager.controlVApp(ControlAction.POWER_ON, vappConfig, true);
	}
	
	public void stopVirtualMachine(XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VmBusyException {
		 vmManager.controlVM(ControlAction.POWER_OFF, vappConfig, virtualMachine, true);
	}
	
	public void stopVirtualMachineContainer(XMLVirtualMachineContainerType vappConfig) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VAppStartupSectionUpdateException {
		/**  - Ensure VM ordering is always consistent when starting and stopping a vApp */
		vappManager.updateStartupSection(vappConfig);  
		vmManager.controlVApp(ControlAction.POWER_OFF, vappConfig, true);
	}
	
	public void suspendVirtualMachine(XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VmBusyException {
		 vmManager.controlVM(ControlAction.SUSPEND, vappConfig, virtualMachine, true);
	}
	
	public void suspendVirtualMachineContainer(XMLVirtualMachineContainerType vappConfig) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException {
		 vmManager.controlVApp(ControlAction.SUSPEND, vappConfig, true);
	}
	
	public void undeployVirtualMachineContainer(XMLVirtualMachineContainerType vappConfig) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException {
		 vmManager.controlVApp(ControlAction.UNDEPLOY, vappConfig, true);
	}
	
	public void update_start_sectionVirtualMachineContainer(XMLVirtualMachineContainerType vappConfig) throws VCloudException, ConnectionException, VAppStartupSectionUpdateException {
		vappManager.updateStartupSection(vappConfig); 
	}
	
	public void updateVirtualMachine(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine) throws StrategyFailureException, UnresolvedDependencyException,
			VCloudException, InvalidStrategyException, IOException, ConnectionException {
		vmManager.updateVMInVApp(strategy, vappConfig, virtualMachine);
	}
	
	public void confirmAssignOrgNetwork(CmdStrategy strategy,
			XMLEnvironmentType envConfig,
			XMLVirtualMachineContainerType vmc,
			XMLOrganisationalNetworkType orgNetworkType) throws StrategyFailureException, UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("Started createAssignOrgNetwork");
		logger.debug("orgNetworkTypeName="+orgNetworkType.getName());
		logger.debug("orgNetworkTypeDescription="+orgNetworkType.getDescription());
		logger.debug("orgNetworkTypeCIDR="+orgNetworkType.getCIDR());
		networkManager.confirmOrgNetworkInVApp(strategy, vmc, orgNetworkType);
		logger.debug("Finished createAssignOrgNetwork");	
	}

}
