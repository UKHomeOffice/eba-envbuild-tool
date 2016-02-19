package com.ipt.ebsa.agnostic.client.skyscape.error;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.skyscape.exception.ControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppEntityBusyControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppForceCustParamControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppNotRunningControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppSkyscapeErrorControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppUnableToProcessControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppUnavailableControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppUnstableStatusControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppVMsNotPoweredOnControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VAppVMsNotSuspendedControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VMNotPoweredOnControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VMNotRunningControlException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.VMNotSuspendedControlException;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdCommand;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdDetail;

/**
 *  Control command error handling for Optimistic error strategy.
 * 
 * This class is used to determine whether to continue or exit for control commands
 * which have an optimistic error strategy. The determining factors are:
 * 
 *  1) What kind of control action was being requested
 *  2) What kind of exception was caught.
 *  
 */
public class ControlErrorHandler {

	private static Logger logger = LogManager.getLogger(ControlErrorHandler.class);

	/**
	 *  Control command error handling for Optimistic error strategy.</p>
	 * </p>
	 * This method is used to determine whether to continue or exit for control commands
	 * which have an optimistic error strategy. The determining factors are:</p>
	 * </p>
	 *  1) What kind of control action was being requested</p>
	 *  2) What kind of exception was caught.</p>
	 * </p>
	 * Control command actions values are:-</p>
	 * </p>
	 *  CmdCommand.DEPLOY;</p>
	 * 	CmdCommand.REBOOT;</p>
	 * 	CmdCommand.RESUME;</p>
	 * 	CmdCommand.SHUTDOWN;</p>
	 * 	CmdCommand.START;</p>
	 * 	CmdCommand.STOP;</p>
	 * 	CmdCommand.SUSPEND;</p>
	 * 	CmdCommand.UNDEPLOY;</p>
	 * 
	 * @param cmdDetail
	 * @param e
	 */
	public static void determineOptimisticControlAction(CmdDetail cmdDetail, ControlException e)
	{
		logger.debug("Trying to continue using Optimistic error strategy.");
		
		/**
		 * The following conditions are non-recoverable, so we wrap and throw a RuntimeException to the calling process.
		 */		
		if(		e instanceof VAppUnavailableControlException ||
				e instanceof VAppUnableToProcessControlException ||
				e instanceof VAppEntityBusyControlException ||
				e instanceof VAppUnstableStatusControlException )
		{
			logger.debug("Processing Non-recoverable Exception.");
			// This is a non-recoverable state for all operations - wrap and throw the exception.
			wrapAndThrowRuntimeException(e);
		}			
		
		/**
		 * The following Exception conditions are potentially recoverable, so we determine for each 
		 * whether or not to wrap and throw a RuntimException to the calling process.
		 */		
		// vApp Control Exceptions
		else if(e instanceof VAppNotRunningControlException)
		{
			logger.debug("Processing VAppNotRunningControlException.");
			if(cmdDetail.getCommand().equals(CmdCommand.STOP)){
				// This is a recoverable state for power_off/stop so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.UNDEPLOY)){
				// This is a recoverable state for undeploy so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.SUSPEND)){
				// This is a recoverable state for suspend so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.REBOOT)){
				// This is a non-recoverable state for reboot operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e);
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.SHUTDOWN)){
				// This is a recoverable state for shutdown so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else{				
				// This is considered a non-recoverable state for all other operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e);
			}
		}				
		else if(e instanceof VAppVMsNotPoweredOnControlException)
		{
			logger.debug("Processing VAppVMsNotPoweredOnControlException.");
			
			if(cmdDetail.getCommand().equals(CmdCommand.STOP)){
				// This is a recoverable state for power_off/stop so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.SUSPEND)){
				// This is a recoverable state for suspend so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.REBOOT)){
				// This is a non-recoverable state for reboot operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e);
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.SHUTDOWN)){
				// This is a recoverable state for shutdown so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else{				
				// This is considered a non-recoverable state for all other operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e);
			}
		}
		else if(e instanceof VAppVMsNotSuspendedControlException)
		{
			logger.debug("Processing VAppVMsNotSuspendedControlException.");
			if(cmdDetail.getCommand().equals(CmdCommand.RESUME)){
				// This is a recoverable state for discardSuspend/resume so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else
			{
				// This is considered a non-recoverable state for all other operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e);
			}
		}	
		else if(e instanceof VAppForceCustParamControlException)
		{
			logger.debug("Processing VAppForceCustParamControlException.");
			if(cmdDetail.getCommand().equals(CmdCommand.DEPLOY)){
				// This is a non-recoverable state for deploy operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e, "VApp must be Powered on to Deploy using forceCustomization option.");
			}
		}
		else if(e instanceof VAppSkyscapeErrorControlException)
		{
			logger.debug("Processing VAppSkyscapeErrorControlException.");
			// This is a non-recoverable state for deploy operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e, "Skyscape Error encountered - this is non-recoverable.");
		}		
		// VM Control Exceptions
		else if(e instanceof VMNotRunningControlException)
		{
			logger.debug("Processing VMNotRunningControlException.");
			if(cmdDetail.getCommand().equals(CmdCommand.STOP)){
				// This is a recoverable state for power_off/stop so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.SUSPEND)){
				// This is a recoverable state for suspend so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.REBOOT)){
				// This is a non-recoverable state for reboot operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e);
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.SHUTDOWN)){
				// This is a recoverable state for shutdown so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else{				
				// This is considered a non-recoverable state for all other operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e);
			}
		}
		else if(e instanceof VMNotPoweredOnControlException)
		{
			logger.debug("Processing VMNotPoweredOnControlException.");
			
			if(cmdDetail.getCommand().equals(CmdCommand.STOP)){
				// This is a recoverable state for power_off/stop so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.SUSPEND)){
				// This is a recoverable state for suspend so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.REBOOT)){
				// This is a non-recoverable state for reboot operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e);
			}
			else if(cmdDetail.getCommand().equals(CmdCommand.SHUTDOWN)){
				// This is a recoverable state for shutdown so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else{				
				// This is considered a non-recoverable state for all other operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e);
			}
		}
		else if(e instanceof VMNotSuspendedControlException)
		{
			logger.debug("Processing VMNotSuspendedControlException.");
			if(cmdDetail.getCommand().equals(CmdCommand.RESUME)){
				// This is a recoverable state for discardSuspend/resume so we will continue.
				logger.debug(String.format("State %s is recoverable, continuing.", cmdDetail.getCommand().value()));
				return;
			}
			else
			{
				// This is considered a non-recoverable state for all other operations - wrap and throw the exception.
				wrapAndThrowRuntimeException(e);
			}
		}		
		else
		{
			logger.debug("Processing general ControlException.");
			wrapAndThrowRuntimeException(e, "General Control Error encountered, this is non-recoverable.");
		}
	}
	
	/**
	 * Method to log, wrap and throw a ControlException.
	 * 
	 * @param e
	 */
	private static void wrapAndThrowRuntimeException(ControlException e)
	{
		wrapAndThrowRuntimeException(e, "");
	}
	
	/**
	 * Method to log, wrap and throw a ControlException.
	 * 
	 * @param e
	 */
	private static void wrapAndThrowRuntimeException(ControlException e, String message)
	{
		logger.debug(String.format("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. %s", message), e);
		throw new RuntimeException(String.format("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. %s", message), e);
	}


}
