package com.ipt.ebsa.agnostic.client.skyscape.module;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.skyscape.comparator.NicDetailComparator;
import com.ipt.ebsa.agnostic.client.skyscape.comparator.VirtualDiskComparator;
import com.ipt.ebsa.agnostic.client.skyscape.comparator.VirtualNetworkCardComparator;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.skyscape.module.NicDetail.IPAddressingMode;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.RecomposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.SourcedCompositionItemParamType;
import com.vmware.vcloud.api.rest.schema.ovf.CimBoolean;
import com.vmware.vcloud.api.rest.schema.ovf.CimString;
import com.vmware.vcloud.api.rest.schema.ovf.RASDType;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.VirtualCpu;
import com.vmware.vcloud.sdk.VirtualDisk;
import com.vmware.vcloud.sdk.VirtualMemory;
import com.vmware.vcloud.sdk.VirtualNetworkCard;
import com.vmware.vcloud.sdk.constants.BusSubType;
import com.vmware.vcloud.sdk.constants.BusType;
import com.vmware.vcloud.sdk.constants.IpAddressAllocationModeType;
import com.vmware.vcloud.sdk.constants.NetworkAdapterType;
import com.vmware.vcloud.sdk.constants.UndeployPowerActionType;
import com.vmware.vcloud.sdk.constants.VMStatus;

/**
 * Provides generally useful, stateless method calls for working with VMs
 *
 *
 */
public class VMModule {

	private static final String MB = "MB";

	private static final String GB = "GB";

	private Logger logger = LogManager.getLogger(VMModule.class);

	@Inject
	private VAppModule vappModule;

	/**
	 * Removes a VM from a VAPP
	 *
	 * @param vdc
	 * @param vapp
	 * @param vmName
	 * @param vappName
	 * @throws VCloudException
	 */
	public void deleteVM(Vdc vdc, Vapp vapp, String vmName) throws VCloudException {
		logger.debug(String.format("Deleting vm '%s' from vapp '%s' within vdc '%s'", vmName, vapp.getReference().getName(), vdc.getReference().getName()));

		VM vm = findVM(vapp, vmName);

		if (vm != null) {

			powerOffVMIfNeeded(vmName, vm);

			logger.debug(String.format("Recomposing VApp without vm '%s'", vmName));
			RecomposeVAppParamsType recomposeVapp = new ObjectFactory().createRecomposeVAppParamsType();
			recomposeVapp.getDeleteItem().add(vm.getReference());
			new TaskUtil().waitForTask(vapp.recomposeVapp(recomposeVapp));
			logger.debug(String.format("Finished recomposing VApp without vm '%s'", vmName));

		} else {
			logger.debug(String.format("Could not find vm with the name '%s', no action will be taken.", vmName));
		}
	}

	/**
	 *
	 * @param vAppLoader
	 * @param vmName
	 * @param vm
	 */
	private void powerOffVMIfNeeded(String vmName, VM vm) {
		VMStatus vmStatus = vm.getVMStatus();
		try {
			if (vmStatus == VMStatus.POWERED_ON || vmStatus == VMStatus.SUSPENDED) {
				logger.debug(String.format("VM current state is %s. Powering off '%s' to PARTIALLY_POWERED_OFF", vmStatus, vmName));
				new TaskUtil().waitForTask(vm.powerOff());
				logger.debug(String.format("Finished powering off '%s'", vmName));
			} else {
				logger.debug(String.format("VM state is %s. No powering off required for '%s'", vmStatus, vmName));
			}
			// : The VM must be undeployed otherwise it cannot be deleted
			if (vm.isDeployed()) {
				logger.debug(String.format("VM %s is being undeployed in preparation for deletion", vmName));
				new TaskUtil().waitForTask(vm.undeploy(UndeployPowerActionType.POWEROFF));
				logger.debug(String.format("Finished undeploying '%s'", vmName));
			} else {
				logger.debug(String.format("VM is undeployed. No undeploy required in preparation for deletion of '%s'", vmName));
			}
		} catch (Exception e) {
			logger.error("Error powering off and undeploying", e);
		}
	}

	/**
	 *
	 * @param vcloudClient
	 * @param vdc
	 * @param vapp
	 * @param deploy
	 * @param poweron
	 * @param vmName
	 * @param computerName
	 * @param description
	 * @param catalogName
	 * @param storageProfile
	 * @param numberOfCPUs
	 * @param gbMemory
	 * @param mbMemory
	 * @param diskAndSizes
	 *            - A multidimensional array - first dimension is the size,
	 *            second dimension is the units. 0=GB, 1=MB
	 * @param vAppLoader
	 * @throws VCloudException
	 * @throws UnresolvedDependencyException
	 */
	public void createVM(VcloudClient vcloudClient, Vdc vdc, final Vapp vapp, boolean deploy, boolean poweron, String vmName, String computerName, String description, String templateName, Vdc templateVdc, String storageProfile, Integer numberOfCPUs, Integer gbMemory, Integer mbMemory, DiskDetail[] diskAndSizes, NicDetail[] networkCardDetails, String guestCustomisationScript, boolean overwriteEmptyTemplateMachines) throws VCloudException, UnresolvedDependencyException {
		logger.debug(String.format("Creating virtual machine '%s' in vapp '%s' within vdc '%s'", vmName, vapp.getReference().getName(), vdc.getReference().getName()));

		String vappname = vapp.getResource().getName();

		/* Check there is no VM with this name already */
		VM checkVm = findVM(vapp, templateName);
		if (checkVm != null) {
			if (overwriteEmptyTemplateMachines) {
				logger.debug(String.format("Destroying empty template %s in vapp %s before creating new virtual machine as the option overwriteEmptyTemplateMachines has been set.", templateName, vappname));
				deleteVM(vdc, vapp, templateName);
			} else {
				throw new RuntimeException(String.format("Unable to create vm '%s' from catalog '%s'.  There is already a VM with the same name as the catalog in the vapp '%s', this may be left over from a previously failed VM creation or there may be another process (or person) creating VMs from that catalog in this vapp at the same time. The VM in question needs to be deleted or renamed before this program can continue to create other VMs.", vmName, templateName, vappname));
			}
		}
		checkVm = findVM(vapp, vmName);
		if (checkVm != null) {
			throw new RuntimeException(String.format("Unable to create vm '%s'.  There is already a VM with the same name in the vapp '%s'.", vmName, templateName, vappname));
		}

		/* Configure the template for use */
		ReferenceType vmTemplate = getTemplateReference(templateVdc, templateName);
		if (vmTemplate == null) {
			throw new UnresolvedDependencyException(String.format("Cannot find template %s", templateName));
		}
		ReferenceType vmTemplateVMRef = new ReferenceType();
		vmTemplateVMRef.setHref(vmTemplate.getHref());
		vmTemplateVMRef.setName(vmName);

		/* Create a VM configuration */
		SourcedCompositionItemParamType vmTemplateItem = new SourcedCompositionItemParamType();
		vmTemplateItem.setSource(vmTemplateVMRef);

		/* Pass that into a VVApp recompose */
		final RecomposeVAppParamsType recomposeVapp = new ObjectFactory().createRecomposeVAppParamsType();
		recomposeVapp.getSourcedItem().add(vmTemplateItem);

		/* Do the recompose VApp action */
		logger.debug(String.format("Issuing command to recompose Vapp '%s' by adding virtual machine '%s' (please be patient, this does take a minute or two)", vappname, vmName));
		new TaskUtil().waitForTask(vapp.recomposeVapp(recomposeVapp));
		logger.debug(String.format("Finished recomposing Vapp '%s'", vappname));

		/*
		 * Relookup the VApp (we cannot use the old reference any more) and use
		 * it to find the newly created VM so we can reconfigure it
		 * appropriately
		 */
		Vapp newVapp = vappModule.getVApp(vcloudClient, vdc, vappname);
		updateVM(newVapp, vmName, templateName, description, computerName, numberOfCPUs, gbMemory, mbMemory, diskAndSizes, networkCardDetails, guestCustomisationScript);

		logger.debug(String.format("Finished creating virtual machine '%s'", vmName));
	}

	/**
	 *
	 * @param vcloudClient
	 * @param vdc
	 * @param vapp
	 * @param deploy
	 * @param poweron
	 * @param vmName
	 * @param computerName
	 * @param description
	 * @param catalogName
	 * @param storageProfile
	 * @param numberOfCPUs
	 * @param gbMemory
	 * @param mbMemory
	 * @param diskAndSizes
	 *            - A multidimensional array - first dimension is the size,
	 *            second dimension is the units. 0=GB, 1=MB
	 * @throws VCloudException
	 */
	public void updateVM(VcloudClient vcloudClient, Vdc vdc, final Vapp vapp, boolean deploy, boolean poweron, String newName, String oldName, String computerName, String description, String templateName, Vdc templateVdc, String storageProfile, Integer numberOfCPUs, Integer gbMemory, Integer mbMemory, DiskDetail[] diskAndSizes, NicDetail[] networkCardDetails, String guestCustomisationScript) throws VCloudException {
		logger.debug(String.format("Updating virtual machine '%s' in vapp '%s' within vdc '%s'", oldName, vapp.getReference().getName(), vdc.getReference().getName()));
		updateVM(vapp, newName, oldName, description, computerName, numberOfCPUs, gbMemory, mbMemory, diskAndSizes, networkCardDetails, guestCustomisationScript);
		logger.debug(String.format("Finished updating virtual machine '%s'", newName));
	}

	/**
	 * Helper method for running though the facts of a virtual machine
	 *
	 * @param vmName
	 * @param description
	 * @param numberOfCPUs
	 * @param gbMemory
	 * @param mbMemory
	 * @param diskAndSizes
	 * @param vms
	 * @param vmTemplate
	 * @throws VCloudException
	 */
	private void updateVM(Vapp vapp, String newName, String oldName, String description, String computerName, Integer numberOfCPUs, Integer gbMemory, Integer mbMemory, DiskDetail[] diskAndSizes, NicDetail[] networkCardDetails, String customisationScript) throws VCloudException {
		List<VM> vms = vapp.getChildrenVms();
		for (final VM vm : vms) {
			if (vm.getReference().getName().equals(oldName)) {

				logger.debug(String.format("Updating vm '%s'", oldName));

				updateNameAndDescription(vm, newName, description);

				updateGuestCustomisations(vm, computerName, customisationScript);

				updateMemory(vm, gbMemory, mbMemory);

				updateCPUs(vm, numberOfCPUs);

				updateDisks(vm, diskAndSizes);

				updateNetworkCards(vm, networkCardDetails);
				break;
			}
		}
	}

	/**
	 *
	 * @param vm
	 * @param vmName
	 * @param description
	 * @throws VCloudException
	 */
	private void updateNameAndDescription(final VM vm, String vmName, String description) throws VCloudException {
		/* Do the update */
		String previousName = vm.getResource().getName();
		String previousDescription = vm.getResource().getDescription();

		boolean vmnameSame = false;
		boolean descriptionSame = false;
		if(previousName != null && vmName != null ) {
			//Both not null
			if(StringUtils.isNotBlank(previousName) && StringUtils.isNotBlank(vmName)) {
				//both have content
				if(previousName.equalsIgnoreCase(vmName)) {
					vmnameSame = true;
					logger.debug("New VMName specified and already correct. No changes being made for VM Name");
				} else {
					logger.debug("New VMName not equal to old VMName, changes will be made.");
				}
			} else if(StringUtils.isBlank(previousName) && StringUtils.isNotBlank(vmName)) {
				//previous name is blank
				logger.debug("New VMName specified and previous vmname was blank, changes will be made");
			} else if(StringUtils.isNotBlank(previousName) && StringUtils.isBlank(vmName)) {
				//new name is blank
				logger.debug("New VMName is blank and previous vmname was not, changes will be made");
			}
		} else if(previousName != null || vmName == null) {
			//new vmname is null
			logger.debug("New VMName is null and previous vmname was not, changes will be made");
		} else if(previousName == null || vmName != null) {
			//old vmname is null
			logger.debug("New VMName is specified and previous vmname was null, changes will be made");
		}

		//  - descriptions are being updated when both are null or blank
		if (previousDescription == null && description == null ) {
			logger.debug("New description and old description are both null. No changes being made.");
			descriptionSame = true;
		} else if(previousDescription != null && description != null ) {
			//Both not null
			if(StringUtils.isNotBlank(previousDescription) && StringUtils.isNotBlank(description)) {
				//both have content
				if(previousDescription.equalsIgnoreCase(description)) {
					descriptionSame = true;
					logger.debug("New description specified and already correct. No changes being made.");
				} else {
					logger.debug("New description not equal to old description, changes will be made.");
				}
			} else if(StringUtils.isBlank(previousDescription) && StringUtils.isNotBlank(description)) {
				//previous desc is blank
				logger.debug("New description is not blank and previous description is blank, changes will be made");
			} else if(StringUtils.isNotBlank(previousDescription) && StringUtils.isBlank(description)) {
				//new desc is blank
				logger.debug("New description is not blank and previous description was not, changes will be made");
			} else if(StringUtils.isBlank(previousDescription) && StringUtils.isBlank(description)) {
				//  - descriptions are being updated when both are null or blank
				// Both are blank
				descriptionSame = true;
				logger.debug("New description and old description are both blank. No changes being made.");
			}
		} else if(previousDescription != null || description == null) {
			//new desc is null
			logger.debug("New description is null and previous description was not, changes will be made");

		} else if(previousDescription == null || description != null) {
			//Old desc null
			logger.debug("New description is not null and previous description was null, changes will be made");
		}

		if ( vmnameSame && descriptionSame) {
			logger.debug("Name and description not specified or already correct. No changes being made.");
		} else {
			logger.debug(String.format("Customising name, description: new='%s', '%s' (old='%s', '%s')", vmName, description, previousName, previousDescription));
			vm.getResource().setName(vmName);
			vm.getResource().setDescription(description);
			new TaskUtil().waitForTask(vm.updateVM(vm.getResource()));
			logger.debug("Completed customising name");
		}
	}

	/**
	 *
	 * @param vm
	 * @param vmName
	 * @param description
	 * @throws VCloudException
	 */
	private void updateGuestCustomisations(final VM vm, String computername, String customisationScript) throws VCloudException {
		/* Do the update */
		String previousName = vm.getGuestCustomizationSection().getComputerName();
		String previousScript = vm.getGuestCustomizationSection().getCustomizationScript();
		boolean changed = false;
		if ((previousName == null && computername == null) || (previousName != null && previousName.equals(computername)) || (computername != null && computername.equals(previousName))) {
			logger.debug("Computer name not specified or already correct. No changes being made.");
		} else {
			logger.debug(String.format("Will perform guest customisation to set computer name (hostname): new='%s' (old='%s')", computername, previousName));
			vm.getGuestCustomizationSection().setComputerName(computername);
			changed = true;
		}

		if ((previousScript == null && customisationScript == null) || (previousScript != null && previousScript.equals(customisationScript)) || (customisationScript != null && customisationScript.equals(previousScript))) {
			logger.debug("Guest customisation script has not been specified or is already correct. No changes being made.");
		} else {
			if(!vm.getVMStatus().equals(VMStatus.POWERED_OFF)) {
				String warning = "You cannot change the customizationScript for a powered on VM. CustomizationScript will NOT be changed.";
				logger.warn(warning);
			}
			else{
				logger.debug(String.format("Will perform guest customisation to set guestCustomisationScript to a %snull value", customisationScript == null ? "" : "non "));
				vm.getGuestCustomizationSection().setCustomizationScript(customisationScript);
				changed = true;
			}
		}
		if (changed) {
			/*
			TODO: Override the password with that from your own Password Manager here, if you wish so.

			String previousPassword = vm.getGuestCustomizationSection().getAdminPassword();
			vm.getGuestCustomizationSection().setAdminPassword(previousPassword); // set the new admin password here
			vm.getGuestCustomizationSection().setAdminPasswordEnabled(true);
			*/

			vm.getGuestCustomizationSection().setAdminAutoLogonCount(0);
			vm.getGuestCustomizationSection().setAdminAutoLogonEnabled(false);
			new TaskUtil().waitForTask(vm.updateSection(vm.getGuestCustomizationSection()));
			logger.debug("Completed updating guest customisation script");
		}
	}

	/**
	 * Updates the number of CPUs in the virtual machine if the number of CPUs
	 * is not already correct. It does nothing if the number of CPUs is already
	 * correct.
	 *
	 * @param vm
	 * @param numberOfCPUs
	 * @throws VCloudException
	 */
	private void updateCPUs(final VM vm, Integer numberOfCPUs) throws VCloudException {
		VirtualCpu cpu = vm.getCpu();
		if (numberOfCPUs == null || numberOfCPUs.equals(cpu.getNoOfCpus())) {
			logger.debug("Number of CPUs not specified or already correct. No changes being made.");
		} else {
			logger.debug(String.format("Customising CPUs: new=%s  (old=%s)", numberOfCPUs, cpu.getNoOfCpus()));
			cpu.setNoOfCpus(numberOfCPUs);
			new TaskUtil().waitForTask(vm.updateCpu(cpu));
			logger.debug("Completed customising CPUs");
		}
	}

	/**
	 * Updates the memory of the virtual machine if the memory parameters are
	 * not already correct. It does nothing if the parameters are already
	 * correct.
	 *
	 * @param vm
	 * @param gbMemory
	 * @param mbMemory
	 * @throws VCloudException
	 */
	private void updateMemory(final VM vm, Integer gbMemory, Integer mbMemory) throws VCloudException {
		VirtualMemory memory = vm.getMemory();
		CimString old = memory.getItemResource().getVirtualQuantityUnits();
		String newUnits = gbMemory != null ? GB : (mbMemory != null ? MB : null);
		BigInteger oldValue = memory.getMemorySize();
		BigInteger newValue = gbMemory != null ? BigInteger.valueOf(gbMemory.longValue()) : (mbMemory != null ? BigInteger.valueOf(mbMemory.longValue()) : null);
		if (newUnits.equals(GB)) {
			// convert to MB
			newValue = newValue.multiply(BigInteger.valueOf(1024L));
		}

		if (newValue == null || newValue.equals(oldValue)) {
			logger.debug("Memory values not specified or already correct. No changes being made.");
		} else {
			logger.debug(String.format("Customising memory: new=%sMB (old=%sMB)", newValue, oldValue));
			CimString units = new CimString();
			units.setValue(newUnits);
			memory.setMemorySize(newValue);
			memory.getItemResource().setVirtualQuantityUnits(units);
			new TaskUtil().waitForTask(vm.updateMemory(memory));
			logger.debug("Completed customising memory");
		}
	}

	/**
	 * Reconcile disks currently in the virtual machine with what is in the
	 * configuration which is being applied. NOTE: vm.getDisks(); returns the
	 * disk and its associated controllers. We need to do a check to see if we
	 * are looking at a disk before we try and update the disk size. If this
	 * code removes a disk it leaves the controllers there. TBD if this is the
	 * right policy
	 *
	 * @param vm
	 * @param diskDetails
	 * @throws VCloudException
	 */
	private void updateDisks(final VM vm, DiskDetail[] diskDetails) throws VCloudException {
		if (diskDetails != null && diskDetails.length > 0) {
			int checkedCount = 0;
			List<VirtualDisk> disksToRemove = new ArrayList<VirtualDisk>();
			List<VirtualDisk> disks = vm.getDisks();

			//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
			Collections.sort(disks, new VirtualDiskComparator());

			logger.debug("Customising disks");
			boolean changesMade = false;
			for (VirtualDisk disk : disks) {
				if (disk.isHardDisk()) {
					//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
					if(logger.isDebugEnabled()){
						// Log out everything we know about the current Disk
						logVirtualDiskInfo(disk);
					}
					//  - change to use ElementName as identifier, instead of AddressOnParent
					String liveDiskNumber = StringUtils.substringAfterLast(disk.getItemResource().getElementName().getValue(), " ");
					Integer diskNumber = Integer.parseInt(liveDiskNumber);
					if (checkedCount < diskDetails.length) {
						// 
						logger.debug(String.format("Existing Disk No. from Element Name is %s, diskNumber in config is %s", diskNumber, diskDetails[checkedCount].getDiskNumber()));
						BigInteger oldSize = disk.getHardDiskSize();
						BigInteger newDiskSize = diskDetails[checkedCount].getSizeInMB();
						if (!oldSize.equals(newDiskSize)) {
							logger.debug(String.format("Disk[%s] will be resized: new=%sMB (old=%sMB)", checkedCount, newDiskSize, oldSize));
							disk.updateHardDiskSize(newDiskSize);
							changesMade = true;
						} else {
							logger.debug(String.format("Disk[%s] size (%sMB) is as per configuration. No changes will be made.", checkedCount, oldSize));
						}
					} else {
						// 
						logger.debug(String.format("Existing Disk No. from Element Name is %s, no related disk in config", diskNumber));
						logger.debug(String.format("Extraneous disk [%s] will be removed as it is not in the configuration.", checkedCount));
						disksToRemove.add(disk);
						changesMade = true;
					}
					checkedCount++;
				}
			}
			/* If changes are ready to be made, make them */
			if (changesMade || checkedCount < diskDetails.length) {

				if (disksToRemove.size() > 0) {
					for (VirtualDisk disk : disksToRemove) {
						disks.remove(disk);
					}
				}
				if (checkedCount < diskDetails.length) {
					for (int i = checkedCount; i < diskDetails.length; i++) {
						BigInteger size = diskDetails[i].getSizeInMB();
						logger.debug(String.format("Disk[%s] will be added: new=%sMB ", i, size));
						VirtualDisk disk = new VirtualDisk(size, BusType.SCSI, BusSubType.LSI_LOGIC);
						disks.add(disk);
					}
				}
				new TaskUtil().waitForTask(vm.updateDisks(disks));

			}

		} else {
			logger.debug("No disk configuration specified. No changes being made.");
		}

		// new TaskUtil().waitForTask(vm.updateDisks(disks));
		logger.debug("Completed customising disks");
	}

	private void logVirtualDiskInfo(VirtualDisk vDisk){
		try{
			logger.debug("About to log out the current VirtualDisk details ::");
			logger.debug(String.format("HardDiskBusType is :: %s", vDisk.getHardDiskBusType()));
			logger.debug(String.format("HardDiskSize is :: %s", vDisk.getHardDiskSize()));
			logger.debug(String.format("ItemResource is :: %s", vDisk.getItemResource()));

			if(vDisk.getItemResource() != null){
				logger.debug(String.format("Logging vDisk.getItemResource() Values if not null. %s", ""));

				RASDType itemRes = vDisk.getItemResource();

				logger.debug(String.format("Description().getValue() is :: %s", itemRes.getDescription() != null ? itemRes.getDescription().getValue() : "null"));
				logger.debug(String.format("ElementName().getValue() is :: %s", itemRes.getElementName() != null ? itemRes.getElementName().getValue() : "null"));
				logger.debug(String.format("InstanceID().getValue() is :: %s", itemRes.getInstanceID().getValue()));
				if(logger.isTraceEnabled())
				{
					logger.trace(String.format("Address().getValue() is :: %s", itemRes.getAddress() != null ? itemRes.getAddress().getValue() : "null"));
					logger.trace(String.format("AddressOnParent().getValue( is :: %s", itemRes.getAddressOnParent() != null ? itemRes.getAddressOnParent().getValue() : "null"));
					logger.trace(String.format("AllocationUnits().getValue() is :: %s", itemRes.getAllocationUnits() != null ? itemRes.getAllocationUnits().getValue() : "null"));
					logger.trace(String.format("AutomaticAllocation().isValue() is :: %s", itemRes.getAutomaticAllocation() != null ? itemRes.getAutomaticAllocation().isValue() : "null"));
					logger.trace(String.format("AutomaticDeallocation().isValue() is :: %s", itemRes.getAutomaticDeallocation() != null ? itemRes.getAutomaticDeallocation().isValue() : "null"));
					logger.trace(String.format("Bound() is :: %s",  itemRes.getBound()));
					logger.trace(String.format("Caption().getValue() is :: %s", itemRes.getCaption() != null ? itemRes.getCaption().getValue() : "null"));
					logger.trace(String.format("ChangeableType().getValue() is :: %s", itemRes.getChangeableType() != null ? itemRes.getChangeableType().getValue() : "null"));
					logger.trace(String.format("Configuration() is :: %s", itemRes.getConfiguration()));
					logger.trace(String.format("ConfigurationName().getValue() is :: %s", itemRes.getConfigurationName() != null ? itemRes.getConfigurationName().getValue() : "null"));
					logger.trace(String.format("ConsumerVisibility().getValue() is :: %s", itemRes.getConsumerVisibility() != null ? itemRes.getConsumerVisibility().getValue() : "null"));
					logger.trace(String.format("Generation().getValue() is :: %s", itemRes.getGeneration() != null ? itemRes.getGeneration().getValue() : "null"));
					logger.trace(String.format("HostResource().size() is :: %s", itemRes.getHostResource() != null ? itemRes.getHostResource().size() : "null"));
					logger.trace(String.format("Limit().getValue() is :: %s", itemRes.getLimit() != null ? itemRes.getLimit().getValue() : "null"));
					logger.trace(String.format("MappingBehavior().getValue() is :: %s", itemRes.getMappingBehavior() != null ? itemRes.getMappingBehavior().getValue() : "null"));
					logger.trace(String.format("OtherResourceType().getValue() is :: %s", itemRes.getOtherResourceType() != null ? itemRes.getOtherResourceType().getValue() : "null"));
					logger.trace(String.format("Parent().getValue() is :: %s", itemRes.getParent() != null ? itemRes.getParent().getValue() : "null"));
					logger.trace(String.format("PoolID().getValue() is :: %s", itemRes.getPoolID() != null ? itemRes.getPoolID().getValue() : "null"));
					logger.trace(String.format("Reservation().getValue() is :: %s", itemRes.getReservation() != null ? itemRes.getReservation().getValue() : "null"));
					logger.trace(String.format("ResourceSubType().getValue() is :: %s", itemRes.getResourceSubType() != null ? itemRes.getResourceSubType().getValue() : "null"));
					logger.trace(String.format("VirtualQuantity().getValue() is :: %s", itemRes.getVirtualQuantity() != null ? itemRes.getVirtualQuantity().getValue() : "null"));
					logger.trace(String.format("VirtualQuantityUnits().getValue() is :: %s", itemRes.getVirtualQuantityUnits() != null ? itemRes.getVirtualQuantityUnits().getValue() : "null"));
					logger.trace(String.format("Weight().getValue() is :: %s", itemRes.getWeight() != null ? itemRes.getWeight().getValue() : "null"));
					if(itemRes.getOtherAttributes() != null && itemRes.getOtherAttributes().keySet() != null){
						logger.trace(String.format("OtherAttributes().keySet().toString() is :: %s", itemRes.getOtherAttributes().keySet().toString()));
					}
					if(itemRes.getOtherAttributes() != null && itemRes.getOtherAttributes().values() != null){
						logger.trace(String.format("OtherAttributes().values().toString() is :: %s", itemRes.getOtherAttributes().values().toString()));
					}
				}

			}

		}catch(VCloudException vce)
		{
			logger.error("VCloudExcetpoin caught during logging.", vce);
		}
	}

	/**
	 * Reconcile actual network cards with what is in the configuration.
	 *
	 * @param vm
	 * @param diskDetails
	 * @throws VCloudException
	 */
	private void updateNetworkCards(final VM vm, NicDetail[] newNetworkCardDetails) throws VCloudException {
		// AA 04/03/14 - Heavily modified, will now no longer replace the NICs if they have not changed, this has been introduced as a powered on VM will fail if you try to replace the NICs - the code was trying to do this even if none had changed.
		if (isNICsMatch(vm, newNetworkCardDetails)) {
			// Do not replace the NICs as they all match
			logger.debug("Network card configuration matches the existing configuration. No changes being made.");
		} else {
			// The number of NICs or the attributes of the new / existing NICs differ so just replace them all
			logger.debug("Customising NICs");
			try {
				//replaceNetworkCards(vm, newNetworkCardDetails);
				replaceNetworkCardsNonDestructive(vm, newNetworkCardDetails);
			} catch (VCloudException e) {
				replaceNetworkCards(vm, newNetworkCardDetails);
			}
		}
		logger.debug("Completed updating network cards");
	}


	/**
	 * This simple method just replaces the existing nics with the ones in the
	 * configuration. It is easy to understand but not very gracious.
	 *
	 * @param vm
	 * @param networkCardDetails
	 * @param index
	 * @throws VCloudException
	 */
	private void replaceNetworkCardsNonDestructive(final VM vm, NicDetail[] networkCardDetails) throws VCloudException {
		List<VirtualNetworkCard> existingVMNics = vm.getNetworkCards();

		//Sort the list into the correct order, nic 0 first then so on.
		Collections.sort(existingVMNics, new VirtualNetworkCardComparator());
		Arrays.sort(networkCardDetails,new NicDetailComparator());

		HashMap<Integer,VirtualNetworkCard> existingNics = new HashMap<Integer,VirtualNetworkCard>(existingVMNics.size() * 2);
		for(VirtualNetworkCard nic:existingVMNics) {
			existingNics.put(new Integer(nic.getItemResource().getAddressOnParent().getValue()), nic);
		}
		existingVMNics.clear();

		for (int i = 0; i < networkCardDetails.length; i++) {
			VirtualNetworkCard existingNicConfig = existingNics.get(i);
			if(existingNicConfig != null) {
				existingVMNics.add(existingNicConfig);
			}
			boolean update = false;
			boolean create = false;

			NicDetail currentNicFromConfig = networkCardDetails[i];
			boolean isPrimaryNic = currentNicFromConfig.isPrimaryNic();

			String newIpMode = currentNicFromConfig.getIpAddressingMode().toString();
			String newIPAddress = currentNicFromConfig.getIpAddress() == null ? "" : networkCardDetails[i].getIpAddress();
			String newNetworkname = currentNicFromConfig.getNetworkname();

			// AA 26/02/14 - Added the NIC number to the schema as the order returned from getNetworkCards is unreliable, if a VM has been added and removed on its own to a vApp
			Integer nicNumber = currentNicFromConfig.getNicNumber();
			if (nicNumber == null) {
				String error = "You are trying to add a network card without a nic number. Your configuration is wrong and needs to be reviewed.";
				logger.error(error);
				throw new IllegalStateException(error);
			}

			if(existingNicConfig == null) {
				create = true;
				// E1000, PCNet32, VMXNET, PCNet32, VMXNET2, VMXNET3
				String adapterType = "E1000";
				VirtualNetworkCard nic = new VirtualNetworkCard(nicNumber, true, newNetworkname, isPrimaryNic, com.vmware.vcloud.sdk.constants.IpAddressAllocationModeType.valueOf(newIpMode), newIPAddress);
				RASDType nicResource = nic.getItemResource();
				CimString resourceSubType = new CimString();
				resourceSubType.setValue(adapterType);
				nicResource.setResourceSubType(resourceSubType);
				vm.getNetworkCards().add(nic);
			} else {

				if(	isPrimaryNic != existingNicConfig.isPrimaryNetworkConnection() ||
						!newIpMode.equals(existingNicConfig.getIpAddressingMode().toString()) ||
						!newIPAddress.equals(existingNicConfig.getIpAddress()) ||
						!newNetworkname.equals(existingNicConfig.getNetwork()) ){
					update=true;
					updateNicNonDestructive(nicNumber, true, newNetworkname, isPrimaryNic, com.vmware.vcloud.sdk.constants.IpAddressAllocationModeType.valueOf(newIpMode), newIPAddress,NetworkAdapterType.E1000, existingNicConfig.getItemResource());
				}
			}

			if(!create && update && isPrimaryNic) {
				//We have an update
				if(!vm.getVMStatus().equals(VMStatus.POWERED_OFF)) {
					String error = "You cannot edit the primary NIC for a powered on VM. This is a fatal error. Please check your configuration as this is probably a mistake in your XML.";
					logger.error(error);
					throw new IllegalStateException(error);
				}
			}
			logger.debug(String.format("New Nic[%s] (mode=%s ip=%s network=%s nicNumber=%s) ", i, newIpMode, newIPAddress, newNetworkname, nicNumber));
		}
		new TaskUtil().waitForTask(vm.updateNetworkCards(existingVMNics));
	}

	@SuppressWarnings("unchecked")
	private void updateNicNonDestructive(Integer nicId, Boolean isConnected, String networkName,
										 Boolean isPrimaryNetworkConnection,
										 IpAddressAllocationModeType ipAddressingMode, String ipAddress,
										 NetworkAdapterType adapterType,  RASDType itemResource) {
		CimString networkConfig = new CimString();
		networkConfig.setValue(networkName);
		Map<QName, String> cimAttributes = networkConfig.getOtherAttributes();
		cimAttributes.put(new QName("http://www.vmware.com/vcloud/v1.5",
				"ipAddress", "vcloud"), ipAddress);

		cimAttributes.put(new QName("http://www.vmware.com/vcloud/v1.5",
						"primaryNetworkConnection", "vcloud"),
				isPrimaryNetworkConnection.toString());

		cimAttributes.put(new QName("http://www.vmware.com/vcloud/v1.5",
				"ipAddressingMode", "vcloud"), ipAddressingMode.value());

//		CimString elementName = new CimString();
//		elementName.setValue("");
//		CimString instanceId = new CimString();
//		instanceId.setValue("");
//		CimString nicIndex = new CimString();
//		nicIndex.setValue(nicId.toString());
		CimBoolean connected = new CimBoolean();
		connected.setValue(isConnected.booleanValue());
//		ResourceType networkResourceType = new ResourceType();
//		networkResourceType.setValue("10");

		CimString resourceSubType = new CimString();
		resourceSubType.setValue(adapterType.value());

		RASDType networkCard = itemResource;
		//networkCard.setElementName(elementName);
		//networkCard.setInstanceID(instanceId);
		//networkCard.setResourceType(networkResourceType);
		//networkCard.setResourceSubType(resourceSubType);
		//networkCard.setAddressOnParent(nicIndex);
		networkCard.setAutomaticAllocation(connected);
		@SuppressWarnings("rawtypes")
		List networkAttributes = networkCard.getConnection();
		networkAttributes.clear();
		networkAttributes.add(networkConfig);
	}


	/**
	 * This simple method just replaces the existing nics with the ones in the
	 * configuration. It is easy to understand but not very gracious.
	 *
	 * @param vm
	 * @param networkCardDetails
	 * @param index
	 * @throws VCloudException
	 */
	private void replaceNetworkCards(final VM vm, NicDetail[] networkCardDetails) throws VCloudException {
		List<VirtualNetworkCard> existingNics = vm.getNetworkCards();
		existingNics.clear();

		for (int i = 0; i < networkCardDetails.length; i++) {
			String newIpMode = networkCardDetails[i].getIpAddressingMode().toString();
			String newIPAddress = networkCardDetails[i].getIpAddress() == null ? "" : networkCardDetails[i].getIpAddress();
			String newNetworkname = networkCardDetails[i].getNetworkname();
			boolean isPrimaryNic = networkCardDetails[i].isPrimaryNic();
			// AA 26/02/14 - Added the NIC number to the schema as the order returned from getNetworkCards is unreliable, if a VM has been added and removed on its own to a vApp
			Integer nicNumber = networkCardDetails[i].getNicNumber();
			if (nicNumber == null) {
				nicNumber = new Integer(i);
			}

			// E1000, PCNet32, VMXNET, PCNet32, VMXNET2, VMXNET3
			String adapterType = "E1000";
			VirtualNetworkCard nic = new VirtualNetworkCard(nicNumber, true, newNetworkname, isPrimaryNic, com.vmware.vcloud.sdk.constants.IpAddressAllocationModeType.valueOf(newIpMode), newIPAddress);
			RASDType nicResource = nic.getItemResource();
			CimString resourceSubType = new CimString();
			resourceSubType.setValue(adapterType);
			nicResource.setResourceSubType(resourceSubType);

			vm.getNetworkCards().add(nic);
			logger.debug(String.format("New Nic[%s] (mode=%s ip=%s network=%s nicNumber=%s) ", i, newIpMode, newIPAddress, newNetworkname, nicNumber));
		}
		new TaskUtil().waitForTask(vm.updateNetworkCards(existingNics));
	}

	/**
	 * Returns the template reference
	 *
	 * @param vdc
	 * @param templateName
	 * @return
	 */
	private ReferenceType getTemplateReference(Vdc vdc, String templateName) {
		logger.debug(String.format("Looking up template '%s' in vdc '%s'", templateName, vdc.getReference().getName()));
		for (ReferenceType vappTemplateRef : vdc.getVappTemplateRefs()) {
			if (vappTemplateRef.getName().equals(templateName)) {
				logger.debug("Found template");
				return vappTemplateRef;
			}
		}
		logger.debug("Did not find template");
		return null;
	}

	/**
	 * Does a lookup for the particular Vm
	 *
	 * @param vapp
	 * @param vmName
	 * @return
	 * @throws VCloudException
	 */
	public VM findVM(Vapp vapp, String vmName) throws VCloudException {
		logger.debug(String.format("Looking for vm '%s' in vapp '%s'", vmName, vapp.getReference().getName()));
		for (final VM vm : vapp.getChildrenVms()) {
			if (vm.getReference().getName().equals(vmName)) {
				return vm;
			}
		}
		return null;
	}

	/**
	 * Check to see if there are differences between the new and the existing NICs
	 *
	 * @param vm
	 * @param existingNetworkCardDetails
	 * @return boolean
	 * @throws VCloudException
	 */
	private boolean isNICsMatch(final VM vm, NicDetail[] newNetworkCardDetails) throws VCloudException {
		List<VirtualNetworkCard> existingNics = vm.getNetworkCards();
		if (existingNics != null && newNetworkCardDetails != null && (newNetworkCardDetails.length == existingNics.size())) {
			// The number of NICs is the same, now compare the details, compose a map, where the key is the NIC number and the value is 
			Map<Integer, NicDetail> newNicMap = new HashMap<Integer, NicDetail>();
			// Loop around the new network cards
			for (int i = 0; i < newNetworkCardDetails.length; i++) {
				Integer newNicNumber = newNetworkCardDetails[i].getNicNumber();
				if (newNicNumber == null) {
					newNicNumber = new Integer(i);
					newNetworkCardDetails[i].setNicNumber(newNicNumber);
				}
				newNicMap.put(newNicNumber, newNetworkCardDetails[i]);
			}
			// Loop around the existing network cards
			for (VirtualNetworkCard nic : existingNics) {
				NicDetail existingNicDetail = new NicDetail();
				Integer existingNicNumber = Integer.parseInt(nic.getItemResource().getAddressOnParent().getValue());
				existingNicDetail.setNicNumber(existingNicNumber);
				existingNicDetail.setNetworkname(nic.getNetwork());
				existingNicDetail.setAddressingMode(IPAddressingMode.valueOf(nic.getIpAddressingMode()));
				if (!nic.getIpAddressingMode().equals(IpAddressAllocationModeType.DHCP.toString())
						&& !nic.getIpAddressingMode().equals(IPAddressingMode.NONE.toString())) {
					existingNicDetail.setIpAddress(nic.getIpAddress());
				}
				existingNicDetail.setPrimaryNic(nic.isPrimaryNetworkConnection());
				NicDetail newNicDetail = newNicMap.get(existingNicNumber);
				if (newNicDetail == null) {
					// No new NIC found for existing NIC
					logger.debug(String.format("No new NIC found for existing NIC - %s", existingNicDetail.toString()));
					return false;
				} else if (newNicDetail.equals(existingNicDetail)) {
					// The details of the matching NIC match, continue to see if the rest match
					logger.debug(String.format("NICs match - new NIC - %s", newNicDetail.toString()));
					logger.debug(String.format("NICs match - old NIC - %s", existingNicDetail.toString()));
				} else {
					// The details of the matching NIC do not match
					logger.debug(String.format("NICs DON'T match - new NIC - %s", newNicDetail.toString()));
					logger.debug(String.format("NICs DON'T match - old NIC - %s", existingNicDetail.toString()));
					return false;
				}
			}
		} else {
			if (existingNics != null) {
				logger.debug(String.format("The number of old [%s] and new [%s] NICs do not match", existingNics.size(), newNetworkCardDetails.length));
			} else {
				logger.debug("There are no existing NICs");
			}
			return false;
		}
		return true;
	}
}