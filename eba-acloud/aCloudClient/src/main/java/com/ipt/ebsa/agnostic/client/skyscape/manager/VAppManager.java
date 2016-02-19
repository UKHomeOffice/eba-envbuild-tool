package com.ipt.ebsa.agnostic.client.skyscape.manager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.skyscape.connection.SkyscapeCloudValues;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppStartupSectionUpdateException;
import com.ipt.ebsa.agnostic.client.skyscape.module.VAppModule;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler.Action;
import com.ipt.ebsa.agnostic.client.util.NamingUtils;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.vmware.vcloud.api.rest.schema.ovf.StartupSectionItem;
import com.vmware.vcloud.api.rest.schema.ovf.StartupSectionType;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.Vdc;

/**
 * 
 *
 */
public class VAppManager  {

	private Logger logger = LogManager.getLogger(VAppManager.class);
		
	@Inject
	private VAppModule vappModule;
	
	@Inject 
	private StrategyHandler strategyHandler;
	
	@Inject
	private SkyscapeCloudValues cv;
	
	private static final String DEFAULT_START_ACTION = "powerOn";
	private static final String DEFAULT_STOP_ACTION = "powerOff";
	
	private static final String VM_STARTUP_VAL_MSG = "The list of vApp child VMs passed in for StartupSection update does not match the list of actual VMs in the vApp.";
	private static final String VM_STARTUP_SECTION_MISSING_MSG = "The vApp %s does not have a StartupSection, so cannot proceed with the update.";
	
	/**
	 * Create a VApp given the strategy and VApp parameters
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @return
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 */
	public Vapp createVApp(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig) throws StrategyFailureException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("createVApp entry - strategy:" + strategy);
		
		String vappName = vappConfig.getName();
		// AA - Use the vDC from the Environment Definition XML, not the config file - this allows multiple vApps to have different vDCs.
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		Vapp vapp = vappModule.getVApp(cv.getClient(), vDC, vappName);
		Action action = strategyHandler.resolveCreateStrategy(strategy, vapp, "VApp", vappName,"");
		switch (action) {
		case DESTROY_THEN_CREATE:
			logger.info("Deleting Vapp '" + vappName + "'.");
			vappModule.deleteVApp(cv.getClient(), vDC, vapp);
			logger.info("Vapp '" + vappName + "' deleted.");
		case CREATE:
			logger.info("Creating Vapp '" + vappName + "'.");
			String vappDescription = vappConfig.getDescription();
			boolean deploy = vappConfig.isDeploy();
			boolean powerOn = vappConfig.isPowerOn();
			vapp = vappModule.createEmptyVApp(cv.getClient(), cv.getOrg(), vDC, vappName, vappDescription, deploy, powerOn);
			logger.info("Vapp '" + vappName + "' created.");
		default:
			break;
		}

		logger.debug("createVApp exit");
		return vapp;
	}

	/**
	 * Delete a VApp given the strategy and VApp parameters
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @return
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 */
	public void deleteVApp(XMLVirtualMachineContainerType vappConfig) throws StrategyFailureException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("deleteVApp entry");
		
		String vappName = vappConfig.getName();
		// AA - Use the vDC from the Environment Definition XML, not the config file - this allows multiple vApps to have different vDCs.
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());

		Vapp vapp = vappModule.getVApp(cv.getClient(), vDC, vappName);
		Action action = strategyHandler.resolveDeleteStrategy(vapp, "VApp", vappName,"");
		switch (action) {
		case DELETE:
			logger.info("Deleting Vapp '" + vappName + "'.");
			vappModule.deleteVApp(cv.getClient(), vDC, vapp);
			logger.info("Vapp '" + vappName + "' deleted.");
			break;
		default:
			break;
		}

		logger.debug("deleteVApp exit");
	}

	/**
	 * COnfirms something about the VApp
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @return
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 */
	public Vapp confirmVApp(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig) throws StrategyFailureException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("confirmVApp entry - strategy:" + strategy);
		
		String vappName = vappConfig.getName();
		// AA - Use the vDC from the Environment Definition XML, not the config file - this allows multiple vApps to have different vDCs.
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		
		Vapp vapp = vappModule.getVApp(cv.getClient(), vDC, vappName);
		// This always returns the same thing, we are only waiting for possible
		// exceptions
		strategyHandler.resolveConfirmStrategy(strategy, vapp, "VApp", vappName,"");
		logger.debug("confirmVApp exit");
		return vapp;
	}
	
	/**
	 *  - Allow update to Power On and Off ordering of child VMs.
	 *  - Also allow setting of the Start and Stop Actions.
	 * 
	 * @throws VCloudException 
	 * @throws ConnectionException 
	 * @throws VAppStartupSectionUpdateException 
	 */
	public void updateStartupSection(XMLVirtualMachineContainerType vAppConfig) throws VCloudException, ConnectionException, VAppStartupSectionUpdateException{
		
		String vappName = vAppConfig.getName();
		Vdc vDC = cv.getVdc(vAppConfig.getServiceLevel());
		
		Vapp vApp = vappModule.getVApp(cv.getClient(), vDC, vappName);
		List<XMLVirtualMachineType> vms = vAppConfig.getVirtualMachine();
		
		// Only process if the number of VMs is not zero - empty vApp will not hve any updates and will fail if attempted.
		if(vApp != null && vApp.getChildrenVms() != null && vApp.getChildrenVms().size() != 0 && vms != null && vms.size() != 0)
		{
			if(validateStartUpVMsExist(vApp, vms, vAppConfig))
			{		
				// Get the startup section. This List will still be referenced by the owning vApp
				StartupSectionType startupSection = vApp.getStartUpSection();
				
				// If the vApp doesn't have a startup section, we need to exit. This sometimes occurs for as yet unknown reasons.
				if(startupSection == null){
					throw new VAppStartupSectionUpdateException(String.format(VM_STARTUP_SECTION_MISSING_MSG, vApp.getResource().getName()));
				}
				
				// Get the list of items from the actual VM
				List<StartupSectionItem> actualItems = startupSection.getItem();
				
				if(logger.isDebugEnabled())
				{			
					logger.debug("Actual vApp StartupSectionItems >>");
					logStartUpItems(actualItems);
				}
				
				// We'll clear the list and add our new items in. This List will still be referenced by the owning vApp StartupSectionType
				actualItems.clear();
				
				// For each VM in the XML config, set the appropriate parameters on an update item object and add to the list
				for(XMLVirtualMachineType vm: vms)
				{
					StartupSectionItem item = new StartupSectionItem();
					
					item.setId(NamingUtils.getVmFQDN(vm, vAppConfig));
					//  - Allow Start and Stop Actions to be set from the definition file
					item.setStartAction( (vm.getVMStartAction() != null) ? vm.getVMStartAction() : DEFAULT_START_ACTION);
					item.setStopAction( (vm.getVMStopAction() != null) ? vm.getVMStopAction() : DEFAULT_STOP_ACTION);
					item.setOrder( (vm.getVMOrder() != null) ? vm.getVMOrder() : 0 );
					item.setStartDelay( (vm.getVMStartDelay() != null) ? vm.getVMStartDelay() : 0 );
					item.setStopDelay( (vm.getVMStopDelay() != null) ? vm.getVMStopDelay() : 0 );			
				
					actualItems.add(item);
				}
				
				if(logger.isDebugEnabled())
				{
					// Log the items we are going to update with - they may not be the same as before...
					logger.debug("Update values for StartupSectionItems >>");
					logStartUpItems(startupSection.getItem());
				}
					
				
				vappModule.updateVappStatupSection(vApp, startupSection);
			}
			else
			{
				throw new VAppStartupSectionUpdateException(VM_STARTUP_VAL_MSG);
			}
		}
	}
	
	/**
	 * Make sure the VMs passed in for update already exist in the vApp.
	 * @param vApp
	 * @param vms
	 * @return
	 * @throws VCloudException
	 */
	private boolean validateStartUpVMsExist(Vapp vApp, List<XMLVirtualMachineType> vms, XMLVirtualMachineContainerType vmc) throws VCloudException{
		
		logger.debug("IN >> validateStartUpVMsExist");
		
		boolean isValid = false;
		
		List<VM> actualVMs = vApp.getChildrenVms();
		
		int sizeActual = actualVMs.size();
		int sizeUpdate = vms.size();
		
		logger.debug(String.format("The size of Actual VM List is %s, the size of Update VM List is %s.", sizeActual, sizeUpdate));
		
		if (sizeActual != sizeUpdate)
		{
			logger.debug(String.format("The number of VMs (%s) in the definition file does not match the number of actual VMs (%s) in the vApp.", vms.size(), actualVMs.size()));
			return isValid;
		}
		else
		{
			List<String> updateVmNames = new ArrayList<String>();
			List<String> actualVmNames = new ArrayList<String>();
			
			// Create lists of VM names that are to be updated and that exist
			for(XMLVirtualMachineType vm: vms)
			{
				String updateName = NamingUtils.getVmFQDN(vm, vmc);
				logger.debug(String.format("Adding VM %s to list of update VMs.", updateName));
				updateVmNames.add(updateName);
			}
			for(VM vm: actualVMs)
			{
				String actualName = vm.getResource().getName();
				logger.debug(String.format("Adding VM %s to list of actual VMs.", actualName));
				actualVmNames.add(actualName);
			}
			
			// Ensure that the Lists are identical
			if(actualVmNames.containsAll(updateVmNames) && updateVmNames.containsAll(actualVmNames))
			{
				logger.debug("The list of VMs to be updated matches the list of VMs in the vApp.");
				isValid = true;
			}
		}
		
		logger.debug(String.format("isValid is :: ", isValid));
		logger.debug("validateStartUpVMsExist >> OUT");
		
		return isValid;
	}
	
	private void logStartUpItems(List<StartupSectionItem> items)
	{
		logger.debug("Logging StartupSectionItems >>");
		// Log the StartupSectionItem values
		for(StartupSectionItem item: items){
			logger.debug(String.format("Item Id is %s", item.getId()));
			logger.debug(String.format("Item Order is %s", item.getOrder()));
			logger.debug(String.format("Item Start Delay is %s", item.getStartDelay()));
			logger.debug(String.format("Item Stop Delay is %s", item.getStopDelay()));
			logger.debug(String.format("Item Start Action is %s", item.getStartAction()));
			logger.debug(String.format("Item Stop Action is %s", item.getStopAction()));
		}
	}
	
}
