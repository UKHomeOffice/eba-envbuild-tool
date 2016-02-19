package com.ipt.ebsa.agnostic.client.skyscape.module;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppEntityBusyControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppForceCustParamControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppNotRunningControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppSkyscapeErrorControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppUnstableStatusControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppVMsNotPoweredOnControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppVMsNotSuspendedControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VMNotPoweredOnControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VMNotRunningControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VMNotSuspendedControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VmBusyException;
import com.jcabi.aspects.RetryOnFailure;
import com.vmware.vcloud.api.rest.schema.LinkType;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.constants.UndeployPowerActionType;
import com.vmware.vcloud.sdk.constants.VMStatus;
import com.vmware.vcloud.sdk.constants.VappStatus;
/**
 * For starting and stopping things.  I suppose one day it will suspend and resume and do othe rnice things
 *
 */
public class ControlModule {
	
	private Logger logger = LogManager.getLogger(ControlModule.class);
	
	public enum ControlAction {
		POWER_ON("Powering on", "action/powerOn"), POWER_OFF("Powering off", "action/powerOff"), 
		SHUTDOWN("Shutting down", "action/shutdown"), REBOOT("Rebooting", "action/reboot"), 
		SUSPEND("Suspending", "action/suspend"), DISCARD_SUSPEND("Discarding suspension", "action/discardSuspendedState"), 
		UNDEPLOY("Undeploying", "action/undeploy"), DEPLOY("Deploying", "action/deploy");
		
		// The friendly human readable name of the action
		private String statusText;
		
		// The action link REST Href as used by the vCloud Client API
		private String actionHref;
		
		private ControlAction(String statusText, String actionHref) {
			this.statusText = statusText;
			this.actionHref = actionHref;
		}
		public String getStatusText() {
			return statusText;
		}
		
		public String getActionHrefText() {
			return actionHref;
		}
	}
	
	// Enum representing whether a vApp is in a stable state to process a control command
	private enum ControlStatus {		
		SHOULD_PROCESS(), SHOULD_NOT_PROCESS;		
	}	
	
	// A list of the currently understood unstable vApp statuses
	private final List<VappStatus> unstableVAppStatuses = new ArrayList<VappStatus>(){
		private static final long serialVersionUID = 8917260853104613260L;
		{
			add(VappStatus.INCONSISTENT_STATE); add(VappStatus.FAILED_CREATION); add(VappStatus.UNRECOGNIZED); 
			add(VappStatus.UNKNOWN); add(VappStatus.WAITING_FOR_INPUT); add(VappStatus.MIXED);
		}
	};

	// A list of the currently understood unstable VM statuses
	private final List<VMStatus> unstableVMStatuses = new ArrayList<VMStatus>(){
		private static final long serialVersionUID = 8917260853104613260L;
		{
			add(VMStatus.INCONSISTENT_STATE); add(VMStatus.FAILED_CREATION); add(VMStatus.UNRECOGNIZED); 
			add(VMStatus.UNKNOWN); add(VMStatus.WAITING_FOR_INPUT);
		}
	};	
	
	// VApp known VCloudException messages
	private static final String VAPP_ENTITY_BUSY = "is busy completing an operation"; // VM in vApp is busy
	private static final String VAPP_VMS_NOT_POWERED_ON = "does not have any powered on VMs"; // VMs in vApp not powered on
	private static final String VAPP_NOT_RUNNING_STARTS_WITH = "The requested operation could not be executed since vApp";
	private static final String VAPP_NOT_RUNNING_ENDS_WITH = "is not running."; // Error when shutting down / undeploying a non-running vApp
	private static final String VAPP_FORCE_CUST_PARAM_NOT_SUPPORTED = "Parameter forceCustomization is not supported for vApps."; // Parameter forceCustomization is not supported for vApps
	private static final String VAPP_VMS_NOT_SUSPENDED = "does not have any suspended VMs.";
	private static final String VAPP_VMWARE_TOOLS_NOT_AVAILABLE = "Cannot perform the action because VMware Tools is unavailable";
	private static final String VAPP_CONTACT_CLOUD_ADMIN = "Unable to perform this action. Contact your cloud administrator.";
	
	// VM Known Error Messages
	private static final String VM_NOT_POWERED_ON_STARTS_WITH = "The requested operation could not be executed since VM";
	private static final String VM_NOT_POWERED_ON_ENDS_WITH = "is not powered on.";
	private static final String VM_NOT_SUSPENDED_STARTS_WITH = "Cannot discard suspend state since VM ";
	private static final String VM_NOT_SUSPENDED_ENDS_WITH = "is not suspended.";
	private static final String VM_NOT_RUNNING_STARTS_WITH = "The requested operation could not be executed since VM";
	private static final String VM_NOT_RUNNING_ENDS_WITH = "is not running.";
	
	/**
	 * Executes the relevant control command based on the desired action
	 * @param start
	 * @param vdc
	 * @param vapp
	 * @throws VCloudException
	 */
	public void controlVApp(ControlAction action, Vdc vdc, Vapp vapp, boolean isOptimisticCtrlCmnd) throws VCloudException, ControlException {
		
		//  - we only want to carry out the optimistic processing if necessary to avoid breaking existing implementation
		if(isOptimisticCtrlCmnd)
		{
			controlVAppOptimistic(action, vdc, vapp);
		}
		else
		{
			logger.info(String.format("%s vapp '%s' within vdc '%s'", action.getStatusText(), vapp.getReference().getName(), vdc.getReference().getName()));
			
			try {
				switch (action) {
				case POWER_ON:
					new TaskUtil().waitForTask(vapp.powerOn());
					break;
				case POWER_OFF:
					new TaskUtil().waitForTask(vapp.powerOff());
					break;
				case REBOOT:
					new TaskUtil().waitForTask(vapp.reboot());
					break;
				case SHUTDOWN:
					new TaskUtil().waitForTask(vapp.shutdown());
					break;
				case SUSPEND:
					new TaskUtil().waitForTask(vapp.suspend());
					break;
				case DISCARD_SUSPEND:
					new TaskUtil().waitForTask(vapp.discardSuspend());
					break;	
				case DEPLOY:
					new TaskUtil().waitForTask(vapp.deploy(true, 0, true));
					break;	
				case UNDEPLOY:
					new TaskUtil().waitForTask(vapp.undeploy(UndeployPowerActionType.DEFAULT));
					break;	
				}
				
			} catch (Exception e) {
				logger.error(String.format("%s resulted in an error",action.getStatusText()),e);
			}
			logger.debug(String.format("%s of vapp '%s' complete", action.getStatusText(), vapp.getResource().getName()));
		}
	
	}
	
	/** 
	 * Executes the relevant control command based on the desired action.</p>
	 * 
	 * : There are a number of checks to make to see whether the vApp is in a stable state to 
	 * service the control action request. There are three main factors here:-</p>
	 * 
	 * 1) The current status of the vApp and whether the control command is to put the vApp into this state.<br>
	 * 2) Whether he control action requested is available to perform on the vApp.<br>
	 * 3) If the action is determined to be appropriate and available, is a know VCloudException thrown from the vCloud Client method call?<br>
	 * <br>
	 * Depending on the outcome of the first two factors, the action may or may not be carried out. The calling process will 
	 * either receive a standard void return from the method, indicating that it has completed, or an Exception of the 
	 * appropriate type will be thrown to advise the calling process of what went wrong.<br>
	 * <br>
	 * NB - if we have gotten this far, it means the calling process was able to resolve the vApp dependency, so it is guaranteed to exist.<br>
	 * 
	 * @param start
	 * @param vdc
	 * @param vapp
	 * @throws VCloudException
	 * @throws ControlException 
	 */
	public void controlVAppOptimistic(ControlAction action, Vdc vdc, Vapp vapp) throws VCloudException, ControlException {
		
		logger.info(String.format("%s vapp '%s' within vdc '%s'", action.getStatusText(), vapp.getReference().getName(), vdc.getReference().getName()));

		{				
			try {
				VappStatus status = vapp.getVappStatus();
				
				switch (action) {
				case POWER_ON:
					if (status.equals(VappStatus.POWERED_ON)) {
						logger.debug("The VApp is already powered on. No changes being made.");
					}
					else {		
						// We'll first check to see if the requested control action is available to our current vApp
						if(isVappActionValid(action, vapp)) {
							new TaskUtil().waitForTask(vapp.powerOn());
						}
						// Otherwise we need to see whether to stop processing or continue based on the vApp state
						else {
							if(determineStateOfVapp(action, vapp).equals(ControlStatus.SHOULD_PROCESS)) {
								// The vApp should be able to process the call, so we will proceed
								new TaskUtil().waitForTask(vapp.powerOn());
							}
						}
					}
					break;
				case POWER_OFF:
					if (status.equals(VappStatus.POWERED_OFF)) {
						logger.debug("The VApp is already powered off. No changes being made.");
					}
					else {		
						// We'll first check to see if the requested control action is available to our current vApp
						if(isVappActionValid(action, vapp)) {
							new TaskUtil().waitForTask(vapp.powerOff());
						}
						else {
							if(determineStateOfVapp(action, vapp).equals(ControlStatus.SHOULD_PROCESS)) {
								new TaskUtil().waitForTask(vapp.powerOff());
							}
						}
					}
					break;
				case REBOOT:
					// We'll first check to see if the requested control action is available to our current vApp
					if(isVappActionValid(action, vapp)) {
						new TaskUtil().waitForTask(vapp.reboot());
					}
					else {
						if(determineStateOfVapp(action, vapp).equals(ControlStatus.SHOULD_PROCESS)) {
							new TaskUtil().waitForTask(vapp.reboot());
						}
					}
					break;
				case SHUTDOWN:
					if (!status.equals(VappStatus.POWERED_ON)) {
						logger.debug("The VApp is not powered on, so cannot shutdown VMs. No changes being made.");
					}					
					else {
						// Shutdown shuts down all child VMs
						// We'll first check to see if the requested control action is available to our current vApp
						if(isVappActionValid(action, vapp)) {
							new TaskUtil().waitForTask(vapp.shutdown());
						}
						else {
							if(determineStateOfVapp(action, vapp).equals(ControlStatus.SHOULD_PROCESS)) {
								new TaskUtil().waitForTask(vapp.shutdown());
							}
						}
					}
					break;
				case SUSPEND:
					if (status.equals(VappStatus.SUSPENDED)) {
						logger.debug("The VApp is already suspended. No changes being made.");
					}
					else{		
						// We'll first check to see if the requested control action is available to our current vApp
						if(isVappActionValid(action, vapp)) {
							new TaskUtil().waitForTask(vapp.suspend());
						}
						else {
							if(determineStateOfVapp(action, vapp).equals(ControlStatus.SHOULD_PROCESS)) {
								new TaskUtil().waitForTask(vapp.suspend());
							}
						}
					}
					break;
				case DISCARD_SUSPEND:						
					// We'll first check to see if the requested control action is available to our current vApp
					if(isVappActionValid(action, vapp)) {
						new TaskUtil().waitForTask(vapp.discardSuspend());
					}
					else {
						if(determineStateOfVapp(action, vapp).equals(ControlStatus.SHOULD_PROCESS)) {
							new TaskUtil().waitForTask(vapp.discardSuspend());
						}
					}
					break;	
				case DEPLOY:
					if (vapp.isDeployed().equals(true) && status.equals(VappStatus.POWERED_ON)) {
						logger.debug("The VApp is already deployed and powered on. No changes being made.");
					}
					else {
						// We'll first check to see if the requested control action is available to our current vApp
						if(isVappActionValid(action, vapp)) {
							if(!status.equals(VappStatus.POWERED_ON)){
								// Cannot use forceCustomization on powered off or suspended vApp
								new TaskUtil().waitForTask(vapp.deploy(true, 0, false));
							}
							else{
								new TaskUtil().waitForTask(vapp.deploy(true, 0, true));
							}
						}
						else {
							if(determineStateOfVapp(action, vapp).equals(ControlStatus.SHOULD_PROCESS)) {
								if(!status.equals(VappStatus.POWERED_ON)){
									// Cannot use forceCustomization on powered off or suspended vApp
									new TaskUtil().waitForTask(vapp.deploy(true, 0, false));
								}
								else{
									new TaskUtil().waitForTask(vapp.deploy(true, 0, true));
								}
							}
						}
					}
					break;	
				case UNDEPLOY:
					if (!vapp.isDeployed().equals(true)) {
						logger.debug("The VApp is not already deployed. No changes being made.");
					}
						else {
						// We'll first check to see if the requested control action is available to our current vApp
						if(isVappActionValid(action, vapp)) {
							new TaskUtil().waitForTask(vapp.undeploy(UndeployPowerActionType.DEFAULT));
						}
						else {
							if(determineStateOfVapp(action, vapp).equals(ControlStatus.SHOULD_PROCESS)) {
								new TaskUtil().waitForTask(vapp.undeploy(UndeployPowerActionType.DEFAULT));
							}
						}
					}
					break;					
				default:
					logger.error(String.format("%s has not been recognised as a valid vApp control action.", action.getStatusText()));
					throw new ControlException(String.format("%s has not been recognised as a valid vApp control action.", action.getStatusText()));
				}
				
			} catch (Exception e) {
				logger.error(String.format("%s resulted in an error.", action.getStatusText()), e);
				handleControlError(action, e);
			}
		
		}
		
		logger.debug(String.format("%s of vapp '%s' complete", action.getStatusText(), vapp.getResource().getName()));
	
	}
	
	/**
	 * Executes the relevant control command based on the desired action
	 * @param start
	 * @param vdc
	 * @param vapp
	 * @param vmName
	 * @throws VCloudException
	 * @throws VmBusyException 
	 */
	@RetryOnFailure(attempts = 20, delay = 30, unit = TimeUnit.SECONDS, types = { VmBusyException.class } , randomize = false)
	public void controlVM(ControlAction action, Vdc vdc, Vapp vapp, String vmName, boolean isOptimisticCtrlCmnd) throws VCloudException, ControlException, VmBusyException {
		
		logger.info(String.format("%s vm '%s' in vapp '%s' within vdc '%s'", action.getStatusText(), vmName, vapp.getReference().getName(), vdc.getReference().getName()));

		//  - we only want to carry out the optimistic processing if necessary to avoid breaking existing implementation
		if(isOptimisticCtrlCmnd)
		{
			controlVMOptimistic(action, vdc, vapp, vmName);
		}
		else
		{
			/* Find the vm to start or stop */
			boolean found = false;
			List<VM> vms = vapp.getChildrenVms();
			for (final VM vm : vms) {
				if(vm.getTasks().size() > 0) {
					for(Task thisTask : vm.getTasks()) {
						if(thisTask.getProgress() < 100) {
							logger.warn("Current task progress is only "+thisTask.getProgress());
						}
						if(thisTask.isBlockingTask()) {
							logger.warn("Current task is blocking");
						}
						
					}
					//Allow it to happen anyway
					//throw new VmBusyException();
				}
				if (vm.getReference().getName().equals(vmName)) {
					logger.debug(String.format("%s '%s'", action.getStatusText(), vmName));
					found = true;
					try {
						switch (action) {
						case POWER_ON:
							if (vm.getVMStatus().equals(VMStatus.POWERED_ON)) {
								logger.debug("The VM is already powered on. No changes being made.");
							} 						
							else {
								new TaskUtil().waitForTask(vm.powerOn());
							}
							break;
						case POWER_OFF:
							new TaskUtil().waitForTask(vm.powerOff());
							break;
						case REBOOT:
							new TaskUtil().waitForTask(vm.reboot());
							break;
						case SHUTDOWN:
							new TaskUtil().waitForTask(vm.shutdown());
							break;
						case SUSPEND:
							new TaskUtil().waitForTask(vm.suspend());
							break;
						case DISCARD_SUSPEND:
							new TaskUtil().waitForTask(vm.discardSuspend());
							break;
						}
						
					} catch (Exception e) {
						logger.error(String.format("%s resulted in an error",action.getStatusText()),e);
					}
					logger.debug(String.format("%s of VM '%s' complete", action.getStatusText(), vmName));
					break;
				}
			}
	
			if (!found) {
				logger.debug(String.format("Could not find VM with the name '%s', no action will be taken.", vmName));
			}
		}
	}
		
		
	/**
	 * Executes the relevant control command based on the desired action.</p>
	 * 
	 * : There are a number of checks to make to see whether the VM is in a stable state to 
	 * service the control action request. There are three main factors here:-</p>
	 * 
	 * 1) The current status of the vApp and whether the control command is to put the VM into this state.<br>
	 * 2) Whether he control action requested is available to perform on the VM.<br>
	 * 3) If the action is determined to be appropriate and available, is a know VCloudException thrown from the vCloud Client method call?<br>
	 * <br>
	 * Depending on the outcome of the first two factors, the action may or may not be carried out. The calling process will 
	 * either receive a standard void return from the method, indicating that it has completed, or an Exception of the 
	 * appropriate type will be thrown to advise the calling process of what went wrong.<br>
	 * <br>
	 * 
	 * @param start
	 * @param vdc
	 * @param vapp
	 * @throws VCloudException
	 * @throws ControlException 
	 * @throws VmBusyException 
	 */
	@RetryOnFailure(attempts = 20, delay = 30, unit = TimeUnit.SECONDS, types = { VmBusyException.class } , randomize = false)
	public void controlVMOptimistic(ControlAction action, Vdc vdc, Vapp vapp, String vmName) throws VCloudException, ControlException, VmBusyException {
		
		logger.info(String.format("%s vm '%s' in vapp '%s' within vdc '%s'", action.getStatusText(), vmName, vapp.getReference().getName(), vdc.getReference().getName()));
		
		/* Find the vm to start or stop */
		boolean found = false;
		List<VM> vms = vapp.getChildrenVms();
				
		for (final VM vm : vms) {
			
			if (vm.getReference().getName().equals(vmName)) {
				if(vm.getTasks().size() > 0) {
					throw new VmBusyException();
				}							
				logger.debug(String.format("%s '%s'", action.getStatusText(), vmName));
				found = true;
				
				VMStatus status = vm.getVMStatus();
				
				try {
					switch (action) {
					case POWER_ON:
						if (status.equals(VMStatus.POWERED_ON)) {
							logger.debug("The VM is already powered on. No changes being made.");
						} 
						else {		
							// We'll first check to see if the requested control action is available to our current VM
							if(isVMActionValid(action, vm)) {
								new TaskUtil().waitForTask(vm.powerOn());
							}
							// Otherwise we need to see whether to stop processing or continue based on the VM state
							else {
								if(determineStateOfVM(action, vm).equals(ControlStatus.SHOULD_PROCESS)) {
									// The vApp should be able to process the call, so we will proceed
									new TaskUtil().waitForTask(vm.powerOn());
								}
							}
						}
						break;
					case POWER_OFF:
						if (status.equals(VMStatus.POWERED_OFF)) {
							logger.debug("The VM is already powered off. No changes being made.");
						} 
						else {		
							// We'll first check to see if the requested control action is available to our current VM
							if(isVMActionValid(action, vm)) {
								new TaskUtil().waitForTask(vm.powerOff());
							}
							else {
								if(determineStateOfVM(action, vm).equals(ControlStatus.SHOULD_PROCESS)) {
									new TaskUtil().waitForTask(vm.powerOff());
								}
							}
						}
						break;
					case REBOOT:
						// We'll first check to see if the requested control action is available to our current VM
						if(isVMActionValid(action, vm)) {
							new TaskUtil().waitForTask(vm.reboot());
						}
						else {
							if(determineStateOfVM(action, vm).equals(ControlStatus.SHOULD_PROCESS)) {
								new TaskUtil().waitForTask(vm.reboot());
							}
						}
						break;
					case SHUTDOWN:
						if (!status.equals(VMStatus.POWERED_ON)) {
							logger.debug("The VM is not already powered on. No changes being made.");
						} else {
							// We'll first check to see if the requested control action is available to our current VM
							if(isVMActionValid(action, vm)) {
								new TaskUtil().waitForTask(vm.shutdown());
							}
							else {
								if(determineStateOfVM(action, vm).equals(ControlStatus.SHOULD_PROCESS)) {
									new TaskUtil().waitForTask(vm.shutdown());
								}
							}
						}
						break;
					case SUSPEND:
						if (status.equals(VMStatus.SUSPENDED)) {
							logger.debug("The VM is already suspended. No changes being made.");
						} else{		
							// We'll first check to see if the requested control action is available to our current VM
							if(isVMActionValid(action, vm)) {
								new TaskUtil().waitForTask(vm.suspend());
							}
							else {
								if(determineStateOfVM(action, vm).equals(ControlStatus.SHOULD_PROCESS)) {
									new TaskUtil().waitForTask(vm.suspend());
								}
							}
						}
						break;
					case DISCARD_SUSPEND:
						// We'll first check to see if the requested control action is available to our current VM
						if(isVMActionValid(action, vm)) {
							new TaskUtil().waitForTask(vm.discardSuspend());
						}
						else {
							if(determineStateOfVM(action, vm).equals(ControlStatus.SHOULD_PROCESS)) {
								new TaskUtil().waitForTask(vm.discardSuspend());
							}
						}
						break;
					default:
						logger.error(String.format("%s has not been recognised as a valid VM control action.", action.getStatusText()));
						throw new ControlException(String.format("%s has not been recognised as a valid VM control action.", action.getStatusText()));
					}
					
				} catch (Exception e) {
					logger.error(String.format("%s resulted in an error",action.getStatusText()),e);
					handleControlError(action, e);
				}
				logger.debug(String.format("%s of VM '%s' complete", action.getStatusText(), vmName));
				break;
			}
		}

		if (!found) {
			logger.debug(String.format("Could not find VM with the name '%s', no action will be taken.", vmName));
		}
	}
	
	/**
	 *  - Control command error handling.</p>
	 * 
	 * Method to log, process and and re-throw if required an Exception caught during a control operation as a new ControlException.
	 * 
	 * @param ex - Original Exception
	 * @throws ConnectionException
	 */
	private void handleControlError(ControlAction action, Exception ex) throws ControlException
	{		
		if(ex instanceof VCloudException)
		{
			String message = ex.getMessage();
			
			if(StringUtils.isBlank(message)){
				// We don't know what the problem is, so throw a general ControlException
				logger.error(String.format("Unexpected Exception thrown during a control operation '%s'", action.getStatusText()), ex);
				throw new ControlException(String.format("Exception thrown during a control operation '%s'", action.getStatusText()), ex);
			}			
			// Check if it one we know about and can re-throw with a specific ControlException. This list is likely to remain fairly small.
			if(message.contains(VAPP_ENTITY_BUSY))
			{
				// The action cannot be processed due to a busy sub-entity - we must stop processing immediately.
				logger.error(String.format("Unexpected VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
				throw new VAppEntityBusyControlException(String.format("VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
			}
			if(message.contains(VAPP_VMS_NOT_POWERED_ON))
			{
				// The action cannot be processed due to a busy sub-entity - we must stop processing immediately.
				logger.error(String.format("Unexpected VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
				throw new VAppVMsNotPoweredOnControlException(String.format("VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
			}
			if(message.startsWith(VAPP_NOT_RUNNING_STARTS_WITH) && message.endsWith(VAPP_NOT_RUNNING_ENDS_WITH))
			{
				// The action cannot be processed due to a busy sub-entity - we must stop processing immediately.
				logger.error(String.format("Unexpected VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
				throw new VAppNotRunningControlException(String.format("VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
			}
			if(message.contains(VAPP_FORCE_CUST_PARAM_NOT_SUPPORTED))
			{
				// The action cannot be processed due to a busy sub-entity - we must stop processing immediately.
				logger.error(String.format("Unexpected VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
				throw new VAppForceCustParamControlException(String.format("VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
			}
			if(message.contains(VAPP_VMS_NOT_SUSPENDED))
			{
				// The action cannot be processed due to a busy sub-entity - we must stop processing immediately.
				logger.error(String.format("Unexpected VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
				throw new VAppVMsNotSuspendedControlException(String.format("VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
			}
			if(message.contains(VAPP_VMWARE_TOOLS_NOT_AVAILABLE))
			{
				// The action cannot be processed due to a busy sub-entity - we must stop processing immediately.
				logger.error(String.format("Unexpected VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
				throw new VAppSkyscapeErrorControlException(String.format("VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
			}
			// VM Exception Messages
			if(message.startsWith(VM_NOT_POWERED_ON_STARTS_WITH) && message.endsWith(VM_NOT_POWERED_ON_ENDS_WITH))
			{
				// The action cannot be processed due to a busy sub-entity - we must stop processing immediately.
				logger.error(String.format("Unexpected VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
				throw new VMNotPoweredOnControlException(String.format("VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
			}
			if(message.startsWith(VM_NOT_RUNNING_STARTS_WITH) && message.endsWith(VM_NOT_RUNNING_ENDS_WITH))
			{
				// The action cannot be processed due to a busy sub-entity - we must stop processing immediately.
				logger.error(String.format("Unexpected VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
				throw new VMNotRunningControlException(String.format("VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
			}
			if(message.startsWith(VM_NOT_SUSPENDED_STARTS_WITH) && message.endsWith(VM_NOT_SUSPENDED_ENDS_WITH))
			{
				// The action cannot be processed due to a busy sub-entity - we must stop processing immediately.
				logger.error(String.format("Unexpected VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
				throw new VMNotSuspendedControlException(String.format("VCloudException thrown during a control operation '%s'", action.getStatusText()), ex);
			}
		}
		else
		{
			// We have a more serious, non-Skyscape-notified problem and must stop processing
			logger.error(String.format("Unexpected Exception thrown during a control operation '%s'", action.getStatusText()), ex);
			throw new ControlException(String.format("Exception thrown during a control operation '%s'", action.getStatusText()), ex);
		}
	}
	
	/**
	 *  - Control command error handling.</p>
	 * 
	 * We will examine the currently available actions that the retrieved vApp is reporting it is able to perform.
	 * If our chosen ControlAction is among them, we let the calling process know this.
	 * 
	 * @param action
	 * @param vApp
	 * @return
	 */
	private Boolean isVappActionValid(ControlAction action, Vapp vApp)
	{
		logger.debug("isVappActionValid >> IN");
		Boolean result = false;
		
		// We'll iterate over the available control actions for this vApp and see if out action is supported in the vApp's current state
		for(LinkType link : vApp.getResource().getLink())
		{
			if(StringUtils.endsWith(link.getHref(), action.getActionHrefText()))
			{
				logger.debug(String.format("Control action '%s' is available to use on vApp %s", action.getStatusText(), vApp.getReference().getName()));
				result = true;
				break;
			}
		}
		
		logger.debug(String.format("result is: %s", result));
		return result;
	}
	
	/**
	 *  - Control command error handling.</p>
	 * 
	 * Method to determine whether the vApp is in a state that should be able to process control command requests.</p>
	 * 
	 * vApp statuses are as below:</p>
	 * 
	 * FAILED_CREATION(-1) (unstable) - Transient entity state, e.g., model object is created but the corresponding VC backing does not exist yet. This is further sub-categorized in the respective entities. <br>
	 * UNRESOLVED(0) (unstable) - Entity is whole, e.g., VM creation is complete and all the required model objects and VC backings are created. <br>
	 * RESOLVED(1) - Entity is resolved. <br>
	 * DEPLOYED(2) - Entity is deployed. <br>
	 * SUSPENDED(3) - All VMs of the vApp are suspended. <br>
	 * POWERED_ON(4) - All VMs of the vApp are powered on. <br>
	 * WAITING_FOR_INPUT(5) (unstable) - VM is pending response on a question. <br>
	 * UNKNOWN(6) (unstable) - Entity state could not be retrieved from the inventory, e.g., VM power state is null. <br>
	 * UNRECOGNIZED(7) (unstable) - Entity state was retrieved from the inventory but could not be mapped to an internal state. <br>
	 * POWERED_OFF(8) - All VMs of the vApp are powered off. <br>
	 * INCONSISTENT_STATE(9) (unstable) - Apply to VM status, if a vm is POWERED_ON, or WAITING_FOR_INPUT, but is undeployed, it is in inconsistent state. <br>
	 * MIXED(10) (unstable) - vApp status is set to MIXED when the VMs in the vApp are in different power states.<br>
	 */
	private ControlStatus determineStateOfVapp(ControlAction action, Vapp vApp) throws ControlException
	{			
		logger.debug("determineStateOfVapp >> IN");
		
		// Check if the vApp is unstable
		if(isVappStatusUnstable(vApp))
		{
			logger.error(String.format("vApp %s's current status %s is not suitable to perform control action %s", 
					vApp.getReference().getName(), vApp.getVappStatus(), action.getStatusText() ));
			
			// We'll throw an unstable status exception here as we cannot proceed any further in processing
			throw new VAppUnstableStatusControlException(String.format("The current state (%s) of vApp is not in suitable to perform control action %s", 
					vApp.getVappStatus(), vApp.getReference().getName(), action.getStatusText()));
		}
		else {
			// Otherwise the vApp *should* be in a stable condition to process a control command, although it may fail downstream for other reasons.
			logger.debug("The vApp should be able to process the control action");
			return ControlStatus.SHOULD_PROCESS;
		}		
	}
	
	/**
	 *  - Control command error handling.</p>
	 * 
	 * Helper method to check if the current status of the VApp is considered stable to perform operations.</p>
	 * 
	 * @param vApp
	 * @return
	 */
	private Boolean isVappStatusUnstable(Vapp vApp)
	{		
		logger.debug("isVappStatusUnstable >> IN");
		Boolean isUnstable = false;
		
		// Check if the vApp is unstable
		if(unstableVAppStatuses.contains(vApp.getVappStatus()))
		{
			isUnstable = true;
		}
		
		logger.debug(String.format("isUnstable is: %s", isUnstable));
		return isUnstable;
	}
	
	
	/**
	 *  - Control command error handling.</p>
	 * 
	 * We will examine the currently available actions that the retrieved VM is reporting it is able to perform.
	 * If our chosen ControlAction is among them, we let the calling process know this.
	 * 
	 * @param action
	 * @param VM
	 * @return
	 */
	private Boolean isVMActionValid(ControlAction action, VM vm)
	{
		logger.debug("isVMActionValid >> IN");
		Boolean result = false;
		
		// We'll iterate over the available control actions for this VM and see if out action is supported in the VM's current state
		for(LinkType link : vm.getResource().getLink())
		{
			if(StringUtils.endsWith(link.getHref(), action.getActionHrefText()))
			{
				logger.debug(String.format("Control action '%s' is available to use on VM %s", action.getStatusText(), vm.getReference().getName()));
				result = true;
				break;
			}
		}
		
		logger.debug(String.format("result is: %s", result));
		return result;
	}
	
	/**
	 *  - Control command error handling.</p>
	 * 
	 * Method to determine whether the VM is in a state that should be able to process control command requests.</p>
	 * 
	 * VM statuses are as below:</p>
	 * 
	 * FAILED_CREATION (unstable) - Transient entity state, e.g., model object is created but the corresponding VC backing does not exist yet. This is further sub-categorized in the respective entities. <br>
	 * UNRESOLVED (unstable) - Entity is whole, e.g., VM creation is complete and all the required model objects and VC backings are created. <br>
	 * RESOLVED - Entity is resolved. <br>
	 * SUSPENDED - VM is suspended. <br>
	 * POWERED_ON - VM is powered on. <br>
	 * WAITING_FOR_INPUT (unstable) - VM is pending response on a question. <br>
	 * UNKNOWN (unstable) - Entity state could not be retrieved from the inventory, e.g., VM power state is null. <br>
	 * UNRECOGNIZED (unstable) - Entity state was retrieved from the inventory but could not be mapped to an internal state. <br>
	 * POWERED_OFF - VM is powered off. <br>
	 * INCONSISTENT_STATE (unstable) - If a vm is POWERED_ON, or WAITING_FOR_INPUT, but is undeployed, it is in inconsistent state. <br>
	 *
	 * @param action
	 * @param vm
	 * @return
	 * @throws ControlException
	 */
	private ControlStatus determineStateOfVM(ControlAction action, VM vm) throws ControlException
	{			
		logger.debug("determineStateOfVM >> IN");
		
		// Check if the VM is unstable
		if(isVMStatusUnstable(vm))
		{
			logger.error(String.format("VM %s's current status %s is not suitable to perform control action %s", 
					vm.getReference().getName(), vm.getVMStatus(), action.getStatusText() ));
			
			// We'll throw an unstable status exception here as we cannot proceed any further in processing
			throw new VAppUnstableStatusControlException(String.format("The current state (%s) of VM is not in suitable to perform control action %s", 
					vm.getVMStatus(), vm.getReference().getName(), action.getStatusText()));
		}
		else {
			// Otherwise the VM *should* be in a stable condition to process a control command, although it may fail downstream for other reasons.
			logger.debug("The VM should be able to process the control action");
			return ControlStatus.SHOULD_PROCESS;
		}		
	}
	
	/**
	 *  - Control command error handling.</p>
	 * 
	 * Helper method to check if the current status of the VM is considered stable to perform operations.</p>
	 * 
	 * @param VM
	 * @return
	 */
	private Boolean isVMStatusUnstable(VM vm)
	{		
		logger.debug("isVMStatusUnstable >> IN");
		Boolean isUnstable = false;
		
		// Check if the vApp is unstable
		if(unstableVMStatuses.contains(vm.getVMStatus()))
		{
			isUnstable = true;
		}
		
		logger.debug(String.format("isUnstable is: %s", isUnstable));
		return isUnstable;
	}
		
}
