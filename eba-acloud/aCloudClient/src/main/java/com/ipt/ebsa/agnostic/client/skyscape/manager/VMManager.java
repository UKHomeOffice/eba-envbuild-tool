package com.ipt.ebsa.agnostic.client.skyscape.manager;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.config.Config;
import com.ipt.ebsa.agnostic.client.skyscape.connection.SkyscapeCloudValues;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppUnavailableControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VmBusyException;
import com.ipt.ebsa.agnostic.client.skyscape.module.ControlModule;
import com.ipt.ebsa.agnostic.client.skyscape.module.ControlModule.ControlAction;
import com.ipt.ebsa.agnostic.client.skyscape.module.DiskDetail;
import com.ipt.ebsa.agnostic.client.skyscape.module.NicDetail;
import com.ipt.ebsa.agnostic.client.skyscape.module.NicDetail.IPAddressingMode;
import com.ipt.ebsa.agnostic.client.skyscape.module.VAppModule;
import com.ipt.ebsa.agnostic.client.skyscape.module.VMModule;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler.Action;
import com.ipt.ebsa.agnostic.client.util.CustomisationHelper;
import com.ipt.ebsa.agnostic.client.util.NamingUtils;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType.CmdOptions;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType.CmdOptions.CmdOption;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdOptionNameType;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLStorageType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.VirtualDisk;

public class VMManager {

	private Logger logger = LogManager.getLogger(VMManager.class);

	@Inject
	private VMModule vmModule;

	@Inject
	private StrategyHandler strategyHandler;

	@Inject
	private VAppModule vappModule;

	@Inject
	private ControlModule controlModule;

	@Inject
	private SkyscapeCloudValues cv;

	@Inject
	@Config
	private String guestCustScriptDir;

	
	/**
	 * Create a vm in a vapp
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws IOException 
	 * @throws ConnectionException 
	 */
	public void createVMInVApp(CmdExecute executePlan, CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType vm) throws StrategyFailureException, UnresolvedDependencyException, VCloudException,
	InvalidStrategyException, IOException, ConnectionException {
		logger.debug("createVMInVApp entry - strategy:" + strategy);

		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		String vmName = NamingUtils.getVmFQDN(vm, vappConfig);

		boolean overwriteEmptyTemplateMachines = checkOptions(executePlan);

		boolean deploy = true;
		boolean powerOn = true;
		String computerName = NamingUtils.getComputerNameFQDN(vm, vappConfig);
		String description = vm.getDescription();
		String templateName = vm.getTemplateName();
		Vdc templateVdc = cv.getVdc(vm.getTemplateServiceLevel());
		String storageProfile = vm.getStorageProfile();
		Integer numberOfCPUs = vm.getCpuCount().intValue();

		Integer gbMemory = getSizeFromUnit(vm.getMemory(),vm.getMemoryUnit(),"GB");
		Integer mbMemory = getSizeFromUnit(vm.getMemory(),vm.getMemoryUnit(),"MB");

		DiskDetail[] diskSizesAndUnits = getDiskDetails(vm.getStorage());		
		NicDetail[] networkCardDetails = getNicDetails(vm.getNIC());

		//Customisation script
		String custScript = vm.getCustomisationScript();
		String customisationScriptData = CustomisationHelper.readCustomisationScript(custScript, guestCustScriptDir);

		Vapp vapp = resolveVAppDependency(cv, vDC, vappName, "VM '" + vmName + "'");

		VM virtualMachine = vmModule.findVM(vapp, vmName);
		Action action = strategyHandler.resolveCreateStrategy(strategy, virtualMachine, "VM", vmName, " in Vapp '" + vappName + "' ");
		switch (action) {
		case DESTROY_THEN_CREATE:
			logger.info("Deleting VM '" + vmName + "'.");
			vmModule.deleteVM(vDC, vapp, vmName);
			vapp = resolveVAppDependency(cv, vDC, vappName, "VM '" + vmName + "'");
			logger.info("VM '" + vmName + "' deleted.");
		case CREATE:
			logger.info("Creating VM '" + vmName + "' with hostName '"+computerName+"'.");
			vmModule.createVM(cv.getClient(), vDC, vapp, deploy, powerOn, vmName, computerName, description, templateName, templateVdc, storageProfile, numberOfCPUs, gbMemory, mbMemory, diskSizesAndUnits, networkCardDetails, customisationScriptData, overwriteEmptyTemplateMachines);
			logger.info("VM '" + vmName + "' created.");
			break;
		case UPDATE:
			logger.info("Updating VM '" + vmName + "' with hostName '"+computerName+"'.");
			vmModule.updateVM(cv.getClient(), vDC, vapp, deploy, powerOn, vmName, vmName, computerName, description, templateName, templateVdc, storageProfile, numberOfCPUs, gbMemory, mbMemory, diskSizesAndUnits, networkCardDetails,customisationScriptData);
			logger.info("VM '" + vmName + "' created.");
			break;
		default:
			break;
		}

		logger.debug("createVMInVApp exit");
	}

	private Integer getSizeFromUnit(BigInteger size, String unitIn,
			String unitOut) {
		int capacity = 0;
		if (unitIn.equalsIgnoreCase("MB")) {
			if (unitOut.equalsIgnoreCase("MB")) {
				capacity = size.intValue();
			} else if (unitOut.equalsIgnoreCase("GB")) {
				capacity = size.intValue() / 1024;
			}
		} else if (unitIn.equalsIgnoreCase("GB")) {

			if (unitOut.equalsIgnoreCase("MB")) {
				capacity = size.intValue() * 1024;
			} else if (unitOut.equalsIgnoreCase("GB")) {
				capacity = size.intValue();
			}
		}
		return capacity;
	}

	private boolean checkOptions(CmdExecute executePlan) {
		boolean overwriteEmptyTemplateMachines = false;
		if ((executePlan != null) && (executePlan.getEnvironmentContainer() != null)) {
			CmdOptions options2 = executePlan.getEnvironmentContainer().getEnvironment().getOptions();
			if (options2 != null) {
				List<CmdOption> options = options2.getOption();
				for (CmdOption cmdOption : options) {
					if (cmdOption.getName() == CmdOptionNameType.OVERWRITE_EMPTY_TEMPLATE_MACHINES) {
						overwriteEmptyTemplateMachines = true;
					}
				}
			}
		}
		return overwriteEmptyTemplateMachines;
	}

	/**
	 * Logic for working out nic information so that it can be passed on
	 * @param list 
	 * @param virtualHardware
	 * @return
	 */
	private NicDetail[] getNicDetails(List<XMLNICType> nics) {//XMLVirtualHardware virtualHardware) {
		NicDetail[] nicDetails = null;
		if (nics != null && nics.size() > 0){
			nicDetails = new NicDetail[nics.size()];
			int i=0;
			for (XMLNICType xmlNetwrkCardType : nics) {

				NicDetail d = new NicDetail();
				if (xmlNetwrkCardType.getIpAssignment().equalsIgnoreCase(IPAddressingMode.MANUAL.toString())
						|| xmlNetwrkCardType.getIpAssignment().equalsIgnoreCase("Static Manual")){
					d.setAddressingMode(IPAddressingMode.MANUAL);
					d.setIpAddress(xmlNetwrkCardType.getInterface().get(0).getStaticIpAddress());
				}

				if (xmlNetwrkCardType.getIpAssignment().equalsIgnoreCase(IPAddressingMode.DHCP.toString())) {
					d.setAddressingMode(IPAddressingMode.DHCP);
				}

				if (xmlNetwrkCardType.getIpAssignment().equalsIgnoreCase(IPAddressingMode.POOL.toString())
						|| xmlNetwrkCardType.getIpAssignment().equalsIgnoreCase("Static IP Pool")) {
					d.setAddressingMode(IPAddressingMode.POOL);
				}
				
				XMLNetworkType network = (XMLNetworkType)xmlNetwrkCardType.getNetworkID();
				d.setNetworkname(network.getName());
				d.setPrimaryNic(xmlNetwrkCardType.isPrimary());
				d.setNicNumber(xmlNetwrkCardType.getIndexNumber().intValue());
				if (d.getIpAddressingMode() == null) {
					throw new RuntimeException("IPAddressing mode invalid or missing.");	
				}
				nicDetails[i] = d;
				i++;
			}
		}
		return nicDetails;
	}

	/**
	 * Logic for working out disk information so that it can be passed on
	 * @param list 
	 * @param virtualHardware
	 * @return
	 */
	private DiskDetail[] getDiskDetails(List<XMLStorageType> hardDisks){//XMLVirtualHardware virtualHardware) {
		DiskDetail[] diskSizesAndUnits = null;
		if (hardDisks != null && hardDisks.size() > 0){
			diskSizesAndUnits = new DiskDetail[hardDisks.size()];
			int i=0;
			for (XMLStorageType xmlHardDiskType : hardDisks) {
				Integer sizeGB = getSizeFromUnit(xmlHardDiskType.getSize(), xmlHardDiskType.getSizeUnit(), "GB");
				Integer sizeMB = getSizeFromUnit(xmlHardDiskType.getSize(), xmlHardDiskType.getSizeUnit(), "MB");
				DiskDetail.Units newUnits = sizeGB != null ? DiskDetail.Units.GB : ( sizeMB != null ? DiskDetail.Units.GB : null);					
				Integer newValue = sizeGB != null ? sizeGB : ( sizeMB != null ? sizeMB : null);
				DiskDetail d = new DiskDetail();
				d.setSize(newValue);
				d.setUnis(newUnits);
				d.setDiskNumber(xmlHardDiskType.getIndexNumber().intValue());
				diskSizesAndUnits[i] = d;
				i++;
			}
		}
		return diskSizesAndUnits;
	}

	/**
	 * Update a vm in a vapp
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws IOException 
	 * @throws ConnectionException 
	 */
	public void updateVMInVApp(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType vm) throws StrategyFailureException, UnresolvedDependencyException, VCloudException,
	InvalidStrategyException, IOException, ConnectionException {
		logger.debug("updateVMInVApp entry - strategy:" + strategy);

		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		String vmName = NamingUtils.getVmFQDN(vm, vappConfig);
		Vapp vapp = resolveVAppDependency(cv, vDC, vappName, "VM '" + vmName + "'");
				
		logger.info("Updating VM '" + vmName + "'.");

		boolean deploy = true;
		boolean powerOn = true;
		String computerName = NamingUtils.getComputerNameFQDN(vm, vappConfig);
		String description = vm.getDescription();
		String templateName = vm.getTemplateName();
		Vdc templateVdc = cv.getVdc(vm.getTemplateServiceLevel());
		String storageProfile = vm.getStorageProfile();
		Integer numberOfCPUs = vm.getCpuCount().intValue();

		Integer gbMemory = getSizeFromUnit(vm.getMemory(),vm.getMemoryUnit(),"GB");
		Integer mbMemory = getSizeFromUnit(vm.getMemory(),vm.getMemoryUnit(),"MB");

		DiskDetail[] diskSizesAndUnits = getDiskDetails(vm.getStorage());		
		NicDetail[] networkCardDetails = getNicDetails(vm.getNIC());

		//Customisation script
		String custScript = vm.getCustomisationScript();
		String customisationScriptData = CustomisationHelper.readCustomisationScript(custScript, guestCustScriptDir);
		
		vmModule.updateVM(cv.getClient(), vDC, vapp, deploy, powerOn, vmName, vmName, computerName, description, templateName, templateVdc, storageProfile, numberOfCPUs, gbMemory, mbMemory, diskSizesAndUnits, networkCardDetails, customisationScriptData);
		
		logger.info("VM '" + vmName + "' created.");
	

		logger.debug("updateVMInVApp exit");
	}

	public void controlVM(ControlAction action, XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType virtualMachine, boolean isOptimisticCtrlCmnd) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException, VmBusyException {
		logger.debug("controlVM entry");

		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		String vmName = NamingUtils.getVmFQDN(virtualMachine, vappConfig);
		Vapp vapp = resolveVAppDependency(cv, vDC, vappName, "VM '" + vmName + "'");
	
		/**  - resolve the VM dependency here first. throws UnresolvedDependencyException if not in the vApp */
		try{
			confirmVMInVApp(CmdStrategy.EXISTS, vappConfig, virtualMachine);
		}
		catch(InvalidStrategyException isex)
		{
			logger.error(String.format("controlVM - unable to locate requested VM %s in the vApp %s", vmName, vapp));
			throw new UnresolvedDependencyException(String.format(
					"Cannot carry out control action '%s' for VM '%s' as there is no VM found with that name.", action.getStatusText(), vmName));
		}
		catch(StrategyFailureException sfex)
		{
			logger.error(String.format("controlVM - unable to locate requested VM %s in the vApp %s", vmName, vapp));
			throw new UnresolvedDependencyException(String.format(
					"Cannot carry out control action '%s' for VM '%s' as there is no VM found with that name.", action.getStatusText(), vmName));
		}
		
		controlModule.controlVM(action, vDC, vapp, vmName, isOptimisticCtrlCmnd);

		logger.debug("controlVM exit");
	}

	public void controlVApp(ControlAction action, XMLVirtualMachineContainerType vappConfig, boolean isOptimisticCtrlCmnd) throws VCloudException, UnresolvedDependencyException, ConnectionException, ControlException {
		logger.debug("controlVApp entry");

		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		Vapp vapp = null;
		
		//  - Handle Control Command errors
		// Check the vApp exists and throw appropriate ControlException if not.
		try{
			vapp = resolveVAppDependency(cv, vDC, vappName, "VApp '" + vappName + "'");
		}
		catch(UnresolvedDependencyException udex)
		{
			logger.error(String.format("Requested vApp resource %s not found during control command operation %s.", vappName, action.getStatusText()));
			throw new VAppUnavailableControlException(String.format("Requested vApp resource %s not found during control command operation %s.", vappName, action.getStatusText()), udex);
		}
		
		controlModule.controlVApp(action, vDC, vapp, isOptimisticCtrlCmnd);

		logger.debug("controlVApp exit");
	}

	/**
	 * Delete a VM given the strategy and VApp parameters
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
	public void deleteVMFromVApp(XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType vm) throws StrategyFailureException, VCloudException, InvalidStrategyException,
	UnresolvedDependencyException, ConnectionException {
		logger.debug("deleteVMFromVApp entry");

		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		String vmName = NamingUtils.getVmFQDN(vm, vappConfig);
		Vapp vapp = resolveVAppDependency(cv, vDC, vappName, "VM '" + vmName + "'");
		VM virtualMachine = vmModule.findVM(vapp, vmName);

		Action action = strategyHandler.resolveDeleteStrategy(virtualMachine, "VM", vmName, " in Vapp '" + vappName + "' ");
		switch (action) {
		case DELETE:
			logger.info("Deleting VM '" + vmName + "'.");
			vmModule.deleteVM(vDC, vapp, vmName);
			logger.info("VM '" + vmName + "' deleted.");
			break;
		default:
			break;
		}

		logger.debug("deleteVMFromVApp exit");
	}

	/**
	 * COnfirms something about the VApp
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 * @throws UnstrategyManager.resolvedDependencyException
	 */
	public void confirmVMInVApp(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType vm) throws StrategyFailureException, VCloudException, InvalidStrategyException,
	UnresolvedDependencyException, ConnectionException {
		logger.debug("confirmVMInVApp entry - strategy:" + strategy);
		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		String vmName = NamingUtils.getVmFQDN(vm, vappConfig);
		Vapp vapp = resolveVAppDependency(cv, vDC, vappName, "VM '" + vmName + "'");
		VM virtualMachine = vmModule.findVM(vapp, vmName);
		// This always returns the same thing, we are only waiting for possible
		// exceptions
		strategyHandler.resolveConfirmStrategy(strategy, virtualMachine, "VM", vmName, " in Vapp '" + vappName + "' ");
		logger.debug("confirmVMInVApp exit");
	}

	public void confirmHDDsInVM(CmdStrategy strategy, XMLVirtualMachineContainerType vappConfig, XMLVirtualMachineType vm) throws StrategyFailureException, VCloudException, InvalidStrategyException,
	UnresolvedDependencyException, ConnectionException {
		logger.debug("confirmHDDsInVM entry - strategy:" + strategy);
		String vappName = vappConfig.getName();
		Vdc vDC = cv.getVdc(vappConfig.getServiceLevel());
		String vmName = NamingUtils.getVmFQDN(vm, vappConfig);
		Vapp vapp = resolveVAppDependency(cv, vDC, vappName, "VM '" + vmName + "'");
		VM virtualMachine = vmModule.findVM(vapp, vmName);
		List<VirtualDisk> liveDisks = virtualMachine.getDisks();
		
		List<XMLStorageType> configuredDisk= vm.getStorage();
		Map<String, XMLStorageType> indexedDisks = new HashMap<String, XMLStorageType>();
				
		for(XMLStorageType xmlDisk:configuredDisk) {
			indexedDisks.put(String.valueOf(xmlDisk.getIndexNumber()), xmlDisk);
		}
		boolean failed = false;
		for(VirtualDisk vd: liveDisks) {
			if(vd.isHardDisk()) {
				String[] hddDesc = vd.getItemResource().getElementName().getValue().split(" ");
				XMLStorageType xmlDisk = indexedDisks.get(hddDesc[2]);
				if(!vd.getHardDiskSize().equals(BigInteger.valueOf(getSizeFromUnit(xmlDisk.getSize(), xmlDisk.getSizeUnit(), "MB")))) {
					failed = true;
					logger.error(String.format("HDD size for disk %s was %s and should have been %s for '%s' exists %s", xmlDisk.getIndexNumber(),vd.getHardDiskSize(),getSizeFromUnit(xmlDisk.getSize(), xmlDisk.getSizeUnit(), "GB"), vmName, " in Vapp '" + vappName + "' "));
				}
			}
		}
		
		// This always returns the same thing, we are only waiting for possible
		// exceptions
		if(failed) {
			throw new StrategyFailureException(String.format("Strategy %s cannot be completed.  Expected HDD sizes were not set correctly, they did not match.", strategy));
		}
		
		logger.debug("confirmHDDsInVM exit");
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
			throw new UnresolvedDependencyException("Cannot " + message + " in VApp '" + vappName + "' as there is no VApp with that name.");
		}
		return vapp;
	}

}
