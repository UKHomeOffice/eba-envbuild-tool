package com.ipt.ebsa.agnostic.client.skyscape;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.agnostic.client.skyscape.error.ControlErrorHandler;
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
 * Test suite to verify that the correct Exception handling baheaviour is displayed by the 
 * ControlErrorHandler class.
 *
 * The following Control Commands are available:-
 * 
 * CmdCommand.DEPLOY
 * CmdCommand.REBOOT
 * CmdCommand.RESUME
 * CmdCommand.SHUTDOWN
 * CmdCommand.START
 * CmdCommand.STOP
 * CmdCommand.SUSPEND
 * CmdCommand.UNDEPLOY
 * 
 *
 */
public class ControlErrorHandlerTest {

	/**
	 *  Our Mock collaboration objects
	 */
	
	// The parameter passed in and Exceptions
	CmdDetail cmdDetailMock;
	ControlException controlExMock;

	// vApp ControlExceptions
	VAppUnableToProcessControlException vAppUnableToProcessControlExMock;
	VAppUnavailableControlException vAppUnavailableControlExMock;
	VAppEntityBusyControlException vAppEntityBusyControlExMock;
	VAppUnstableStatusControlException vAppUnstableStatusControlExMock;
	VAppNotRunningControlException vAppNotRunningControlExMock;
	VAppVMsNotPoweredOnControlException vAppVMsNotPoweredOnControlExMock;
	VAppVMsNotSuspendedControlException vAppVMsNotSuspendedControlExMock;
	VAppForceCustParamControlException vAppForceCustParamControlExMock;
	VAppSkyscapeErrorControlException vAppSkyscapeErrorControlExMock;
	
	// VM ControlExceptions
	VMNotRunningControlException vMNotRunningControlExMock;
	VMNotPoweredOnControlException vMNotPoweredOnControlExMock;
	VMNotSuspendedControlException vMNotSuspendedControlExMock;
	
    @Before
    public void setUp()
    {    
    	cmdDetailMock = mock(CmdDetail.class);
        controlExMock = mock(ControlException.class);

        vAppUnableToProcessControlExMock = mock(VAppUnableToProcessControlException.class);
    	vAppUnavailableControlExMock = mock(VAppUnavailableControlException.class);
    	vAppEntityBusyControlExMock = mock(VAppEntityBusyControlException.class);
    	vAppUnstableStatusControlExMock = mock(VAppUnstableStatusControlException.class);
    	vAppNotRunningControlExMock = mock(VAppNotRunningControlException.class);
    	vAppVMsNotPoweredOnControlExMock = mock(VAppVMsNotPoweredOnControlException.class);
    	vAppVMsNotSuspendedControlExMock = mock(VAppVMsNotSuspendedControlException.class);
    	vAppForceCustParamControlExMock = mock(VAppForceCustParamControlException.class);
    	vAppSkyscapeErrorControlExMock = mock(VAppSkyscapeErrorControlException.class);
    	
    	vMNotRunningControlExMock = mock(VMNotRunningControlException.class);
    	vMNotPoweredOnControlExMock = mock(VMNotPoweredOnControlException.class);
    	vMNotSuspendedControlExMock = mock(VMNotSuspendedControlException.class);
    }
    
    @After
    public void tearDown()
    {
    	cmdDetailMock = null;
        controlExMock = null;

        vAppUnableToProcessControlExMock = null;
    	vAppUnavailableControlExMock = null;
        vAppEntityBusyControlExMock = null;
        vAppUnstableStatusControlExMock = null;        
    	vAppNotRunningControlExMock = null;
    	vAppSkyscapeErrorControlExMock = null;
    	vAppVMsNotPoweredOnControlExMock = null;
    	vAppVMsNotSuspendedControlExMock = null;
    	vAppForceCustParamControlExMock = null;
    	vAppSkyscapeErrorControlExMock = null;
    	
    	vMNotRunningControlExMock = null;
    	vMNotPoweredOnControlExMock = null;
    	vMNotSuspendedControlExMock = null;
    }

    
    /**
     * The following Exceptions are never recoverable:<br>
     * <br>
     * VAppUnavailableControlException<br>
	 * VAppUnableToProcessControlException<br>
	 * VAppEntityBusyControlException<br>
	 * VAppUnstableStatusControlException<br>
	 * VAppSkyscapeErrorControlException<br>
	 * ControlException<br>
     */
   
    /**
     * Test method to ensure that the correct Runtime Exception behaviour is shown for a 
     * non-recoverable Exception type.
     */
	@Test
	public void processVAppUnavailableControlException()
	{				
		
		try
		{
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppUnavailableControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppUnavailableControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
    /**
     * Test method to ensure that the correct Runtime Exception behaviour is shown for a 
     * non-recoverable Exception type..
     */
	@Test
	public void processVAppUnableToProcessControlException()
	{				
		
		try
		{
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppUnableToProcessControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppUnableToProcessControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
    /**
     * Test method to ensure that the correct Runtime Exception behaviour is shown for a 
     * non-recoverable Exception type..
     */
	@Test
	public void processVAppEntityBusyControlException()
	{				
		
		try
		{
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppEntityBusyControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppEntityBusyControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
    /**
     * Test method to ensure that the correct Runtime Exception behaviour is shown for a 
     * non-recoverable Exception type..
     */
	@Test
	public void processVAppUnstableStatusControlException()
	{				
		
		try
		{
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppUnstableStatusControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppUnstableStatusControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
     * Test method to ensure that the correct Runtime Exception behaviour is shown for a 
     * non-recoverable Exception type..
     */
	@Test
	public void processVAppSkyscapeErrorControlException()
	{				
		
		try
		{
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppSkyscapeErrorControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppSkyscapeErrorControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. Skyscape Error encountered - this is non-recoverable.", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
     * Test method to ensure that the correct Runtime Exception behaviour is shown for a 
     * non-recoverable Exception type..
     */
	@Test
	public void processControlException()
	{				
		
		try
		{
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, controlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof ControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. General Control Error encountered, this is non-recoverable.", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	
	/**
	 * VAppNotRunningControlException cases<br>
	 * <br>
	 * STOP - recoverable<br>
	 * UNDEPLOY - recoverable<br>
	 * SUSPEND - recoverable<br>
	 * SHUTDOWN - recoverable<br>
	 * REBOOT - non-recoverable<br>
	 * 
	 */
	
    /**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVAppNotRunningControlExceptionSTOP()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.STOP).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVAppNotRunningControlExceptionUNDEPLOY()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.UNDEPLOY).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVAppNotRunningControlExceptionSUSPEND()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.SUSPEND).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVAppNotRunningControlExceptionSHUTDOWN()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.SHUTDOWN).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}	
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVAppNotRunningControlExceptionREBOOT()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.REBOOT).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppNotRunningControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}

	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVAppNotRunningControlExceptionDEPLOY()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.DEPLOY).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppNotRunningControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVAppNotRunningControlExceptionRESUME()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.RESUME).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppNotRunningControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVAppNotRunningControlExceptionSTART()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.START).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppNotRunningControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
	 * VMNotPoweredOnControlException cases<br>
	 * <br>
	 * STOP - recoverable<br>
	 * SUSPEND - recoverable<br>
	 * SHUTDOWN - recoverable<br>
	 * REBOOT - error thrown<br>
	 * 
	 */
	
    /**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVAppVMsNotPoweredOnControlExceptionSTOP()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.STOP).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppVMsNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}	
		
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVAppVMsNotPoweredOnControlExceptionSUSPEND()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.SUSPEND).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppVMsNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVAppVMsNotPoweredOnControlExceptionSHUTDOWN()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.SHUTDOWN).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppVMsNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
		
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVAppVMsNotPoweredOnControlExceptionREBOOT()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.REBOOT).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppVMsNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppVMsNotPoweredOnControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}	
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVAppVMsNotPoweredOnControlExceptionDEPLOY()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.DEPLOY).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppVMsNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppVMsNotPoweredOnControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVAppVMsNotPoweredOnControlExceptionRESUME()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.RESUME).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppVMsNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppVMsNotPoweredOnControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVAppVMsNotPoweredOnControlExceptionSTART()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.START).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppVMsNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppVMsNotPoweredOnControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVAppVMsNotPoweredOnControlExceptionUNDEPLOY()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.UNDEPLOY).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppVMsNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppVMsNotPoweredOnControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}	
	
	/**
	 * VAppVMsNotSuspendedControlException cases<br>
	 * <br>
	 * RESUME - recoverable<br>
	 * 
	 */
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVAppVMsNotSuspendedControlExceptionRESUME()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.RESUME).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppVMsNotSuspendedControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
	
	
	/**
	 * VAppForceCustParamControlException cases<br>
	 * <br>
	 * DEPLOY - non-recoverable<br>
	 * 
	 */
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVAppForceCustParamControlExceptionREBOOT()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.REBOOT).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vAppForceCustParamControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VAppForceCustParamControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
	 * END of vApp ControlException cases
	 */
	
	/**
	 * START of VMControlException cases
	 */
	
	/**
	 * VMNotRunningControlException cases<br>
	 * <br>
	 * STOP - recoverable<br>
	 * SUSPEND - recoverable<br>
	 * SHUTDOWN - recoverable<br>
	 * REBOOT - non-recoverable<br>
	 * 
	 */
	
    /**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotRunningControlExceptionSTOP()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.STOP).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotRunningControlExceptionSUSPEND()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.SUSPEND).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotRunningControlExceptionSHUTDOWN()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.SHUTDOWN).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}	
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotRunningControlExceptionREBOOT()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.REBOOT).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VMNotRunningControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}

	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotRunningControlExceptionRESUME()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.RESUME).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VMNotRunningControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotRunningControlExceptionSTART()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.START).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotRunningControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VMNotRunningControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
	 * VAppVMsNotPoweredOnControlException cases<br>
	 * <br>
	 * STOP - recoverable<br>
	 * SUSPEND - recoverable<br>
	 * SHUTDOWN - recoverable<br>
	 * REBOOT - non-recoverable<br>
	 * 
	 */
	
    /**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotPoweredOnControlExceptionSTOP()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.STOP).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}	
		
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotPoweredOnControlExceptionSUSPEND()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.SUSPEND).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotPoweredOnControlExceptionSHUTDOWN()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.SHUTDOWN).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
		
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotPoweredOnControlExceptionREBOOT()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.REBOOT).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VMNotPoweredOnControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}	
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotPoweredOnControlExceptionRESUME()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.RESUME).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VMNotPoweredOnControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * non-recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotPoweredOnControlExceptionSTART()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.START).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotPoweredOnControlExMock);
		}
		catch(RuntimeException rex)
		{
			Assert.assertTrue(rex.getCause() instanceof VMNotPoweredOnControlException);
			Assert.assertEquals("Optimistic error strategy will result in an exit. ControlException being wrapped and thrown. ", rex.getMessage());
		}
		catch(Exception gex)
		{
			Assert.fail("Expected RuntimeException not thrown.");
		}
	}
	
	/**
	 * VMNotSuspendedControlException cases<br>
	 * <br>
	 * RESUME - recoverable<br>
	 * 
	 */
	
	/**
     * Test method to ensure that the correct behaviour is shown for a 
     * recoverable Exception/command combination ..
     */
	@Test
	public void processVMNotSuspendedControlExceptionRESUME()
	{						
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(CmdCommand.RESUME).
			when(cmdDetailMock).getCommand();
			
			ControlErrorHandler.determineOptimisticControlAction(cmdDetailMock, vMNotSuspendedControlExMock);
		}
		catch(RuntimeException rex)
		{			
			Assert.fail("RuntimeException thrown unexpectedly.");
		}
		catch(Exception gex)
		{
			Assert.fail("Exception thrown unexpectedly.");
		}
	}
	
}
