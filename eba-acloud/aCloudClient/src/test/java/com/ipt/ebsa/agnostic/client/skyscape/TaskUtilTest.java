package com.ipt.ebsa.agnostic.client.skyscape;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeoutException;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.code.tempusfugit.concurrency.ConcurrentTestRunner;
import com.ipt.ebsa.agnostic.client.skyscape.module.TaskUtil;
import com.jcabi.aspects.Loggable;
import com.vmware.vcloud.api.rest.schema.ErrorType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;

/**
 * Test suite to verify that the correct VCloud Task retry behaviour is processed by 
 * the TaskUtil class.
 * 
 *
 */
@Loggable(prepend = true)
@RunWith(ConcurrentTestRunner.class)
public class TaskUtilTest {

	// The class under test
	TaskUtil taskUtil;
	
	// Our mocked Task object on which we can program behaviours
	Task taskMock;
	
	// Mocked TaskType object that is accessed in TaskUtil
	TaskType taskTypeMock;
	
	// Mocked objects referred yto from the Task Type / Task during logging...
	XMLGregorianCalendar xmlGregCalMock;
	ReferenceType refTypeMock;
	ErrorType errorTypeMock;
	RuntimeException runtimeExcMock;
	
    @Before
    public void setUp() {
    	taskUtil = new TaskUtil(3,1000L);
    	taskMock = mock(Task.class);
    	taskTypeMock = mock(TaskType.class);
    	xmlGregCalMock = mock(XMLGregorianCalendar.class);
    	refTypeMock = mock(ReferenceType.class);
    	errorTypeMock = mock(ErrorType.class);
    	runtimeExcMock = mock(RuntimeException.class);
    	setUpLoggingExpectations();
    }
    
    @After
    public void tearDown() {
    	taskMock = null;
    	taskTypeMock = null;
    	xmlGregCalMock = null;
    	refTypeMock = null;
    	errorTypeMock = null;
    }

    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and no Exceptions are thrown.
     */
	@Test
	public void testWaitForTaskNoErrorsCompletes()
	{				
		try
		{
			doNothing().when(taskMock).waitForTask(0); // Not strictly necessary, but makes it clearer...
			taskUtil.waitForTask(taskMock);
			verify(taskMock).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry once then complete.
     */
	@Test
	public void testWaitForTaskTimeoutExceptionThrownOnceCompletes()
	{	

		try
		{			
			// Set up Task mock object expectations - thrown exception once then do nothing thereafter
			doThrow(new TimeoutException())
			   .doNothing()
			   .when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(2)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry twice then complete.
     */
	@Test
	public void testWaitForTaskTimeoutExceptionThrownTwiceCompletes()
	{	

		try
		{			
			// Set up sequential Task mock object expectations - throw exception x2 then do nothing thereafter
			doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.doNothing()
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(3)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry three times then complete.
     */
	@Test
	public void testWaitForTaskTimeoutExceptionThrownThreeTimesCompletes()
	{	

		try
		{			
			// Set up sequential Task mock object expectations - throw exception x3 then do nothing thereafter
			doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.doNothing()
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}

    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry four times, succeed in cancelling the task, 
     *  then throw a fatal RuntimeException.
     */
	@Test
	public void testWaitForTaskTimeoutExceptionThrownFourTimesFatal()
	{	

		try
		{			
			// Set up sequential Task mock object expectations
			doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(RuntimeException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertEquals("Fatal Error when executing vCloud task", vcex.getMessage());
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry three times, catch a ConnectException on the forth invocation
	 * and return.
     */
	@Test
	public void testWaitForTaskTimeoutExceptionThrownThreeTimesThenOneConnectExceptionCompletes()
	{	

		try
		{			
			// Set up an exception mock with cause of ConnectException
			doReturn(new ConnectException()).when(runtimeExcMock).getCause();
			
			// Set up sequential Task mock object expectations
			doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.doThrow(runtimeExcMock)
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			
			// Make sure 4 invocations were made
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	

    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry three times, catch an Exception from attempting to 
     * cancel the Task, then still throw a fatal RuntimeException.
     */
	@Test
	public void testWaitForTaskTimeoutExceptionThrownFourTimesFatalCancelTaskException()
	{	

		try
		{			
			// Set up sequential Task mock object expectations
			doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.doThrow(new TimeoutException())
				.when(taskMock).waitForTask(0);
			
			doThrow(new RuntimeException()).when(taskMock).cancelTask();
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(RuntimeException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertEquals("Fatal Error when executing vCloud task", vcex.getMessage());
			//
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry once then complete.
     */
	@Test
	public void testWaitForTaskVCloudExceptionThrownOnceCompletes()
	{	

		try
		{			
			// Set up Task mock object expectations - thrown exception once then do nothing thereafter
			doThrow(new VCloudException("Unit Test Exception 1"))
			   .doNothing()
			   .when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(2)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry twice then complete.
     */
	@Test
	public void testWaitForTaskVCloudExceptionThrownTwiceCompletes()
	{	

		try
		{			
			// Set up sequential Task mock object expectations - throw exception x2 then do nothing thereafter
			doThrow(new VCloudException("Unit Test Exception 1"))
				.doThrow(new VCloudException("Unit Test Exception 2"))
				.doNothing()
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(3)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry three times then complete.
     */
	@Test
	public void testWaitForTaskVCloudExceptionThrownThreeTimesCompletes()
	{	

		try
		{			
			// Set up sequential Task mock object expectations - throw exception x3 then do nothing thereafter
			doThrow(new VCloudException("Unit Test Exception 1"))
				.doThrow(new VCloudException("Unit Test Exception 2"))
				.doThrow(new VCloudException("Unit Test Exception 3"))
				.doNothing()
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}

    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry four times, succeed in cancelling the task, 
     *  then throw a fatal RuntimeException.
     */
	@Test
	public void testWaitForTaskVCloudExceptionThrownFourTimesFatal()
	{	

		try
		{			
			// Set up sequential Task mock object expectations
			doThrow(new VCloudException("Unit Test Exception 1"))
				.doThrow(new VCloudException("Unit Test Exception 2"))
				.doThrow(new VCloudException("Unit Test Exception 3"))
				.doThrow(new VCloudException("Unit Test Exception 4"))
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(RuntimeException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertEquals("Fatal Error when executing vCloud task", vcex.getMessage());
			//
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry three times, catch a ConnectException on the forth invocation, 
     * then return.
     */
	@Test
	public void testWaitForTaskVCloudExceptionThrownThreeTimesThenOneConnectExceptionCompletes()
	{	

		try
		{			
			// Set up an exception mock with cause of ConnectException
			doReturn(new ConnectException()).when(runtimeExcMock).getCause();
			
			// Set up sequential Task mock object expectations
			doThrow(new VCloudException("Unit Test Exception 1"))
				.doThrow(new VCloudException("Unit Test Exception 2"))
				.doThrow(new VCloudException("Unit Test Exception 3"))
				.doThrow(runtimeExcMock)
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			
			// Make sure 4 invocations were made
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry three times, catch a ConnectException on the forth invocation, 
     * then return.
     */
	@Test
	public void testWaitForTaskVCloudExceptionThrownThreeTimesThenOneConnectException()
	{	

		try
		{			
			// Set up an exception mock with cause of ConnectException
			doReturn(new ConnectException()).when(runtimeExcMock).getCause();
			
			// Set up sequential Task mock object expectations
			doThrow(new VCloudException("Unit Test Exception 1"))
				.doThrow(new VCloudException("Unit Test Exception 2"))
				.doThrow(new VCloudException("Unit Test Exception 3"))
				.doThrow(runtimeExcMock)
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			
			// Make sure 4 invocations were made
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(VCloudException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertEquals("Error executing task", vcex.getMessage());
			//
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}

	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry three times, catch an Exception from attempting to 
     * cancel the Task, then still throw a fatal RuntimeException.
     */
	@Test
	public void testWaitForTaskVCloudExceptionThrownFourTimesFatalCancelTaskException()
	{	

		try
		{			
			// Set up sequential Task mock object expectations
			doThrow(new VCloudException("Unit Test Exception 1"))
				.doThrow(new VCloudException("Unit Test Exception 2"))
				.doThrow(new VCloudException("Unit Test Exception 3"))
				.doThrow(new VCloudException("Unit Test Exception 4"))
				.when(taskMock).waitForTask(0);
			
			doThrow(new RuntimeException()).when(taskMock).cancelTask();
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(RuntimeException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertEquals("Fatal Error when executing vCloud task", vcex.getMessage());
			//
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry once then complete.
     */
	@Test
	public void testWaitForTaskRuntimeExceptionThrownOnceCompletes()
	{	

		try
		{			
			// Set up Task mock object expectations - thrown exception once then do nothing thereafter
			doThrow(new RuntimeException("Unit Test Exception 1"))
			   .doNothing()
			   .when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(2)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry twice then complete.
     */
	@Test
	public void testWaitForTaskRuntimeExceptionThrownTwiceCompletes()
	{	

		try
		{			
			// Set up sequential Task mock object expectations - throw exception x2 then do nothing thereafter
			doThrow(new RuntimeException("Unit Test Exception 1"))
				.doThrow(new RuntimeException("Unit Test Exception 2"))
				.doNothing()
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(3)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry three times then complete.
     */
	@Test
	public void testWaitForTaskRuntimeExceptionThrownThreeTimesCompletes()
	{	

		try
		{			
			// Set up sequential Task mock object expectations - throw exception x3 then do nothing thereafter
			doThrow(new RuntimeException("Unit Test Exception 1"))
				.doThrow(new RuntimeException("Unit Test Exception 2"))
				.doThrow(new RuntimeException("Unit Test Exception 3"))
				.doNothing()
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}

    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry four times, succeed in cancelling the task, 
     *  then throw a fatal RuntimeException.
     */
	@Test
	public void testWaitForTaskRuntimeExceptionThrownFourTimesFatal()
	{	

		try
		{			
			// Set up sequential Task mock object expectations
			doThrow(new RuntimeException("Unit Test Exception 1"))
				.doThrow(new RuntimeException("Unit Test Exception 2"))
				.doThrow(new RuntimeException("Unit Test Exception 3"))
				.doThrow(new RuntimeException("Unit Test Exception 4"))
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(RuntimeException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertEquals("Fatal Error when executing vCloud task", vcex.getMessage());
			//
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry three times, catch a ConnectException on the forth invocation, 
     * then return.
     */
	@Test
	public void testWaitForTaskRuntimeExceptionThrownThreeTimesThenOneConnectExceptionCompletes()
	{	

		try
		{			
			// Set up an exception mock with cause of ConnectException
			doReturn(new ConnectException()).when(runtimeExcMock).getCause();
			
			// Set up sequential Task mock object expectations
			doThrow(new RuntimeException("Unit Test Exception 1"))
				.doThrow(new RuntimeException("Unit Test Exception 2"))
				.doThrow(new RuntimeException("Unit Test Exception 3"))
				.doThrow(runtimeExcMock)
				.when(taskMock).waitForTask(0);
			
			taskUtil.waitForTask(taskMock);
			
			// Make sure 4 invocations were made
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}

	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test. Result - should retry three times, catch an Exception from attempting to 
     * cancel the Task, then still throw a fatal RuntimeException.
     */
	@Test
	public void testWaitForTaskRuntimeExceptionThrownFourTimesFatalCancelTaskException()
	{	

		try
		{			
			// Set up sequential Task mock object expectations
			doThrow(new RuntimeException("Unit Test Exception 1"))
				.doThrow(new RuntimeException("Unit Test Exception 2"))
				.doThrow(new RuntimeException("Unit Test Exception 3"))
				.doThrow(new RuntimeException("Unit Test Exception 4"))
				.when(taskMock).waitForTask(0);
			
			doThrow(new RuntimeException()).when(taskMock).cancelTask();
			
			taskUtil.waitForTask(taskMock);
			verify(taskMock, times(4)).waitForTask(0);
		}
		catch(RuntimeException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertEquals("Fatal Error when executing vCloud task", vcex.getMessage());
			//
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
	/**
	 * Set up the expectations on our mocked collaborating objects
	 */
	private void setUpLoggingExpectations()
	{
		// Get out mocked Task Type
		when(taskMock.getResource()).thenReturn(taskTypeMock);
		when(taskMock.getProgress()).thenReturn(1);
		when(taskMock.getOrgReference()).thenReturn(refTypeMock);
		when(taskMock.getUserReference()).thenReturn(refTypeMock);
		
		// Return the appropriate value of mock from each TaskType accessor call
		when(taskTypeMock.getDescription()).thenReturn("Description");
		when(taskTypeMock.getDetails()).thenReturn("Details");
		when(taskTypeMock.getStartTime()).thenReturn(xmlGregCalMock);
		when(taskTypeMock.getOperation()).thenReturn("Operation");
		when(taskTypeMock.getId()).thenReturn("Id");
		when(taskTypeMock.getType()).thenReturn("Type");
		when(taskTypeMock.getStatus()).thenReturn("Status");
		when(taskTypeMock.getOrganization()).thenReturn(refTypeMock);
		when(taskTypeMock.getOwner()).thenReturn(refTypeMock);
		when(taskTypeMock.getStartTime()).thenReturn(xmlGregCalMock);
		when(taskTypeMock.getEndTime()).thenReturn(xmlGregCalMock);
		when(taskTypeMock.getError()).thenReturn(errorTypeMock);
		when(taskTypeMock.getHref()).thenReturn("Href");
		when(taskTypeMock.getOperationKey()).thenReturn("Operation Key");
		when(taskTypeMock.getOperationName()).thenReturn("Operation Name");
				
		when(refTypeMock.getName()).thenReturn("This is my name");
		
		when(errorTypeMock.toString()).thenReturn("ErrorType");
		
		when(xmlGregCalMock.toGregorianCalendar()).thenReturn(new GregorianCalendar());
		when(xmlGregCalMock.toString()).thenReturn("Current Time");
	
	}
	
}
