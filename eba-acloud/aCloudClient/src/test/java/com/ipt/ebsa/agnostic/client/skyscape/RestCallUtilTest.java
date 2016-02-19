package com.ipt.ebsa.agnostic.client.skyscape;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.agnostic.client.skyscape.module.RestCallUtil;
import com.vmware.vcloud.api.rest.schema.ComposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.ControlAccessParamsType;
import com.vmware.vcloud.api.rest.schema.OrgVdcNetworkType;
import com.vmware.vcloud.sdk.OrgVdcNetwork;
import com.vmware.vcloud.sdk.ReferenceResult;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.Vdc;

/**
 * Test suite to verify that the correct VCloud Rest Call retry behaviour is processed by 
 * the RestCallUtil class.
 * 
 *
 */
public class RestCallUtilTest {

	// The class under test
	RestCallUtil restUtil;

	/**
	 *  Our Mock collaboration objects
	 */
	
	//vApp.updateControlAccess Tests Cases
	Vapp vAppMock;
	ControlAccessParamsType controlAccessParamsMock;
	
	//Vdc.composeVapp Test Cases
	Vdc vdcMock;
	ComposeVAppParamsType composeVappParamsMock;
	
	//Vdc.createOrgVdcNetwork  Test Cases
	OrgVdcNetworkType networkParamsMock;	
	
	
	/**
	 * Non-static method callers to be mock tested:-
	 * 
	 * ControlAccessParamsType processVAppRestCall(Vapp vApp, ControlAccessParamsType controlAccessParams)
	 * Vapp processVdcRestCall(Vdc targetVdc, ComposeVAppParamsType vAppParams)
	 * OrgVdcNetwork processVdcRestCall(Vdc targetVdc, OrgVdcNetworkType networkParams)
	 * ReferenceResult processVdcRestCall(Vdc targetVdc)
	 * 
	 * Static method callers, same input params types, different static method names:-
	 * 
	 * Vapp processVAppRestCall(VcloudClient vCloudClient, ReferenceType ref)
	 * Vdc processVdcRestCall(VcloudClient vCloudClient, ReferenceType ref)
	 * Organization processOrgRestCall(VcloudClient vCloudClient, ReferenceType ref)
	 * ReferenceResult processAdminOrgRestCall(VcloudClient vCloudClient, ReferenceType ref)
	 * 
	 */
	
	
    @Before
    public void setUp()
    {
    	restUtil = new RestCallUtil(3,3);
    	vAppMock = mock(Vapp.class);
    	controlAccessParamsMock = mock(ControlAccessParamsType.class);
    	vdcMock = mock(Vdc .class);
    	composeVappParamsMock = mock(ComposeVAppParamsType.class);
    	networkParamsMock = mock(OrgVdcNetworkType.class);
    }
    
    @After
    public void tearDown()
    {
    	restUtil = null;
    	vAppMock = null;
    	controlAccessParamsMock = null;
    	vdcMock = null;
    	composeVappParamsMock = null;
    	networkParamsMock = null;
    }

    
    /**
     *  ########## vApp.updateControlAccess Tests Cases
     */    
    
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and no Exceptions are thrown.
     */
	@Test
	public void processVAppUpdateControlAccessNoErrors()
	{				
		// Mocked return object
		ControlAccessParamsType controlAccessParamsMockResult = mock(ControlAccessParamsType.class);
		
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(controlAccessParamsMockResult).
			when(vAppMock).updateControlAccess(controlAccessParamsMock);
			
			// Call the method under test
			ControlAccessParamsType result = restUtil.processVAppRestCall(vAppMock, controlAccessParamsMock);
			
			// Check out results - we should get back the result with no retries
			verify(vAppMock, times(1)).updateControlAccess(controlAccessParamsMock);
			Assert.assertEquals(controlAccessParamsMockResult, result);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
    
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and One Exceptions are thrown.
     */
	@Test
	public void processVAppUpdateControlAccessOneRuntimeException()
	{				
		// Mocked return object
		ControlAccessParamsType controlAccessParamsMockResult = mock(ControlAccessParamsType.class);
		
		try
		{
			// Set up our expected 1 Exception then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doReturn(controlAccessParamsMockResult).
			when(vAppMock).updateControlAccess(controlAccessParamsMock);
			
			// Call the method under test
			ControlAccessParamsType result = restUtil.processVAppRestCall(vAppMock, controlAccessParamsMock);
			
			// Check out results - we should get back the result after 1 retries
			verify(vAppMock, times(2)).updateControlAccess(controlAccessParamsMock);
			Assert.assertEquals(controlAccessParamsMockResult, result);

		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Two Exceptions are thrown.
     */
	@Test
	public void processVAppUpdateControlAccessTwoRuntimeExceptions()
	{				
		// Mocked return object
		ControlAccessParamsType controlAccessParamsMockResult = mock(ControlAccessParamsType.class);
		
		try
		{
			// Set up our expected 2 Exceptions then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(controlAccessParamsMockResult).
			when(vAppMock).updateControlAccess(controlAccessParamsMock);
			
			// Call the method under test
			ControlAccessParamsType result = restUtil.processVAppRestCall(vAppMock, controlAccessParamsMock);
			
			// Check out results - we should get back the result after 2 retries
			verify(vAppMock, times(3)).updateControlAccess(controlAccessParamsMock);
			Assert.assertEquals(controlAccessParamsMockResult, result);

		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVAppUpdateControlAccessThreeRuntimeExceptions()
	{				
		// Mocked return object
		ControlAccessParamsType controlAccessParamsMockResult = mock(ControlAccessParamsType.class);
		
		try
		{
			// Set up our expected 3 Exception then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(controlAccessParamsMockResult).
			when(vAppMock).updateControlAccess(controlAccessParamsMock);
			
			// Call the method under test
			ControlAccessParamsType result = restUtil.processVAppRestCall(vAppMock, controlAccessParamsMock);
			
			// Check out results - we should get back the result after 3 retries
			verify(vAppMock, times(4)).updateControlAccess(controlAccessParamsMock);
			Assert.assertEquals(controlAccessParamsMockResult, result);

		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}    
	
	/**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVAppUpdateControlAccessFourRuntimeExceptions()
	{				
		// Mocked return object
		ControlAccessParamsType controlAccessParamsMockResult = mock(ControlAccessParamsType.class);
		
		try
		{
			// Set up our expected 4 Exceptions then return from the mock when called by the invoked method. The last call should not be made.
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(controlAccessParamsMockResult).
			when(vAppMock).updateControlAccess(controlAccessParamsMock);
			
			// Call the method under test
			ControlAccessParamsType result = restUtil.processVAppRestCall(vAppMock, controlAccessParamsMock);
			
			fail("Failed due to expected Exception not being thrown.");
		}
		catch(RuntimeException ex)
		{
			// this is out success condition after exceeding max retries
			ex.printStackTrace();
			Assert.assertTrue(ex.getMessage().contains("Fatal Exception during processing of Vdc Rest call:"));
			try {
				verify(vAppMock, times(4)).updateControlAccess(controlAccessParamsMock);
			} catch (VCloudException e) {
				e.printStackTrace();
				fail("Failed due to Exception: " + ex.getMessage());
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}		
	}
	
	/**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVAppUpdateControlAccessThreeRuntimeOneVCloudExceptions()
	{				
		// Mocked return object
		ControlAccessParamsType controlAccessParamsMockResult = mock(ControlAccessParamsType.class);
		
		String vCloudErorMsg = "Unit Test Thrown Exception";
		
		try
		{
			// Set up our expected 4 Exceptions then return from the mock when called by the invoked method. The last call should not be made.
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new VCloudException(vCloudErorMsg)).
			doReturn(controlAccessParamsMockResult).
			when(vAppMock).updateControlAccess(controlAccessParamsMock);
			
			// Call the method under test
			ControlAccessParamsType result = restUtil.processVAppRestCall(vAppMock, controlAccessParamsMock);
			
			fail("Failed due to expected Exception not being thrown.");
		}
		catch(VCloudException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertTrue(vcex.getMessage().contains(vCloudErorMsg));
			try {
				verify(vAppMock, times(4)).updateControlAccess(controlAccessParamsMock);
			} catch (VCloudException e) {
				e.printStackTrace();
				fail("Failed due to Exception: " + vcex.getMessage());
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}		
	}
	
	/**
	 * ########## Vdc.composeVapp Test Cases - Vdc targetVdc, ComposeVAppParamsType vAppParams
	 */
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and no Exceptions are thrown.
     */
	@Test
	public void processVdcComposeVappNoErrors()
	{				
		// Mocked return object
		Vapp vAppMockResult = mock(Vapp.class);
		
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(vAppMockResult).
			when(vdcMock).composeVapp(composeVappParamsMock);
			
			// Call the method under test
			Vapp result = restUtil.processVdcRestCall(vdcMock, composeVappParamsMock);
			
			// Check out results - we should get back the result with no retries
			verify(vdcMock, times(1)).composeVapp(composeVappParamsMock);
			Assert.assertEquals(vAppMockResult, result);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
    
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and One Exceptions are thrown.
     */
	@Test
	public void processVdcComposeVappOneRuntimeException()
	{				
		// Mocked return object
		Vapp vAppMockResult = mock(Vapp.class);
		
		try
		{
			// Set up our expected 1 Exception then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doReturn(vAppMockResult).
			when(vdcMock).composeVapp(composeVappParamsMock);
			
			// Call the method under test
			Vapp result = restUtil.processVdcRestCall(vdcMock, composeVappParamsMock);
						
			// Check out results - we should get back the result after 1 retries
			verify(vdcMock, times(2)).composeVapp(composeVappParamsMock);
			Assert.assertEquals(vAppMockResult, result);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Two Exceptions are thrown.
     */
	@Test
	public void processVdcComposeVappTwoRuntimeExceptions()
	{				
		// Mocked return object
		Vapp vAppMockResult = mock(Vapp.class);
		
		try
		{
			// Set up our expected 2 Exceptions then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(vAppMockResult).
			when(vdcMock).composeVapp(composeVappParamsMock);
			
			// Call the method under test
			Vapp result = restUtil.processVdcRestCall(vdcMock, composeVappParamsMock);
						
			// Check out results - we should get back the result after 2 retries
			verify(vdcMock, times(3)).composeVapp(composeVappParamsMock);
			Assert.assertEquals(vAppMockResult, result);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVdcComposeVappThreeRuntimeExceptions()
	{				
		// Mocked return object
		Vapp vAppMockResult = mock(Vapp.class);
		
		try
		{
			// Set up our expected 3 Exception then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(vAppMockResult).
			when(vdcMock).composeVapp(composeVappParamsMock);
			
			// Call the method under test
			Vapp result = restUtil.processVdcRestCall(vdcMock, composeVappParamsMock);
						
			// Check out results - we should get back the result after 3 retries
			verify(vdcMock, times(4)).composeVapp(composeVappParamsMock);
			Assert.assertEquals(vAppMockResult, result);

		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}    
	
	/**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVdcComposeVappFourRuntimeExceptions()
	{				
		// Mocked return object
		Vapp vAppMockResult = mock(Vapp.class);
		
		try
		{
			// Set up our expected 4 Exceptions then return from the mock when called by the invoked method. The last call should not be made.
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(vAppMockResult).
			when(vdcMock).composeVapp(composeVappParamsMock);
			
			// Call the method under test
			Vapp result = restUtil.processVdcRestCall(vdcMock, composeVappParamsMock);
						
			fail("Failed due to expected Exception not being thrown.");
		}
		catch(RuntimeException ex)
		{
			// this is out success condition after exceeding max retries
			ex.printStackTrace();
			Assert.assertTrue(ex.getMessage().contains("Fatal Exception during processing of Vdc Rest call:"));
			try {
				verify(vdcMock, times(4)).composeVapp(composeVappParamsMock);
			} catch (VCloudException e) {
				e.printStackTrace();
				fail("Failed due to Exception: " + ex.getMessage());
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}		
	}
	
	/**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVdcComposeVappThreeRuntimeOneVCloudExceptions()
	{				
		// Mocked return object
		Vapp vAppMockResult = mock(Vapp.class);
		
		String vCloudErorMsg = "Unit Test Thrown Exception";
		
		try
		{
			// Set up our expected 4 Exceptions then return from the mock when called by the invoked method. The last call should not be made.
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new VCloudException(vCloudErorMsg)).
			doReturn(vAppMockResult).
			when(vdcMock).composeVapp(composeVappParamsMock);
			
			// Call the method under test
			Vapp result = restUtil.processVdcRestCall(vdcMock, composeVappParamsMock);
						
			fail("Failed due to expected Exception not being thrown.");
		}
		catch(VCloudException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertTrue(vcex.getMessage().contains(vCloudErorMsg));
			try {
				verify(vdcMock, times(4)).composeVapp(composeVappParamsMock);
				} catch (VCloudException e) {
				e.printStackTrace();
				fail("Failed due to Exception: " + vcex.getMessage());
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}		
	}
	
	/**
	 * ########## Vdc.createOrgVdcNetwork Test Cases - processVdcRestCall(Vdc targetVdc, OrgVdcNetworkType networkParams)
	 * ########## targetVdc.createOrgVdcNetwork(networkParams);
	 */
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and no Exceptions are thrown.
     */
	@Test
	public void processVdcCreateOrgVdcNetworkNoErrors()
	{				
		// Mocked return object
		OrgVdcNetwork orgNetworkMockResult = mock(OrgVdcNetwork.class);
				
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(orgNetworkMockResult).
			when(vdcMock).createOrgVdcNetwork(networkParamsMock);
			
			// Call the method under test
			OrgVdcNetwork result = restUtil.processVdcRestCall(vdcMock, networkParamsMock);
			
			// Check out results - we should get back the result with no retries
			verify(vdcMock, times(1)).createOrgVdcNetwork(networkParamsMock);
			Assert.assertEquals(orgNetworkMockResult, result);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
    
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and One Exceptions are thrown.
     */
	@Test
	public void processVdcCreateOrgVdcNetworkOneRuntimeException()
	{				
		// Mocked return object
		OrgVdcNetwork orgNetworkMockResult = mock(OrgVdcNetwork.class);
		
		try
		{
			// Set up our expected 1 Exception then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doReturn(orgNetworkMockResult).
			when(vdcMock).createOrgVdcNetwork(networkParamsMock);
			
			// Call the method under test
			OrgVdcNetwork result = restUtil.processVdcRestCall(vdcMock, networkParamsMock);
			
			// Check out results - we should get back the result with 1 retry
			verify(vdcMock, times(2)).createOrgVdcNetwork(networkParamsMock);
			Assert.assertEquals(orgNetworkMockResult, result);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Two Exceptions are thrown.
     */
	@Test
	public void processVdcCreateOrgVdcNetworkTwoRuntimeExceptions()
	{				
		// Mocked return object
		OrgVdcNetwork orgNetworkMockResult = mock(OrgVdcNetwork.class);
		
		try
		{
			// Set up our expected 2 Exceptions then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(orgNetworkMockResult).
			when(vdcMock).createOrgVdcNetwork(networkParamsMock);
			
			// Call the method under test
			OrgVdcNetwork result = restUtil.processVdcRestCall(vdcMock, networkParamsMock);
			
			// Check out results - we should get back the result with 2 retries
			verify(vdcMock, times(3)).createOrgVdcNetwork(networkParamsMock);
			Assert.assertEquals(orgNetworkMockResult, result);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVdcCreateOrgVdcNetworkThreeRuntimeExceptions()
	{				
		// Mocked return object
		OrgVdcNetwork orgNetworkMockResult = mock(OrgVdcNetwork.class);
		
		try
		{
			// Set up our expected 3 Exception then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(orgNetworkMockResult).
			when(vdcMock).createOrgVdcNetwork(networkParamsMock);
			
			// Call the method under test
			OrgVdcNetwork result = restUtil.processVdcRestCall(vdcMock, networkParamsMock);
			
			// Check out results - we should get back the result with 3 retries
			verify(vdcMock, times(4)).createOrgVdcNetwork(networkParamsMock);
			Assert.assertEquals(orgNetworkMockResult, result);

		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}    
	
	/**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVdcCreateOrgVdcNetworkFourRuntimeExceptions()
	{				
		// Mocked return object
		OrgVdcNetwork orgNetworkMockResult = mock(OrgVdcNetwork.class);
		
		try
		{
			// Set up our expected 4 Exceptions then return from the mock when called by the invoked method. The last call should not be made.
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(orgNetworkMockResult).
			when(vdcMock).createOrgVdcNetwork(networkParamsMock);
			
			// Call the method under test
			OrgVdcNetwork result = restUtil.processVdcRestCall(vdcMock, networkParamsMock);
						
			fail("Failed due to expected Exception not being thrown.");
		}
		catch(RuntimeException ex)
		{
			// this is out success condition after exceeding max retries
			ex.printStackTrace();
			Assert.assertTrue(ex.getMessage().contains("Fatal Exception during processing of Vdc Rest call:"));
			try {
				verify(vdcMock, times(4)).createOrgVdcNetwork(networkParamsMock);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Failed due to Exception: " + ex.getMessage());
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}		
	}
	
	/**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVdcCreateOrgVdcNetworkThreeRuntimeOneVCloudExceptions()
	{				
		// Mocked return object
		OrgVdcNetwork orgNetworkMockResult = mock(OrgVdcNetwork.class);
		
		String vCloudErorMsg = "Unit Test Thrown Exception";
		
		try
		{
			// Set up our expected 4 Exceptions then return from the mock when called by the invoked method. The last call should not be made.
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new VCloudException(vCloudErorMsg)).
			doReturn(orgNetworkMockResult).
			when(vdcMock).createOrgVdcNetwork(networkParamsMock);
			
			// Call the method under test
			OrgVdcNetwork result = restUtil.processVdcRestCall(vdcMock, networkParamsMock);
						
			fail("Failed due to expected Exception not being thrown.");
		}
		catch(VCloudException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertTrue(vcex.getMessage().contains(vCloudErorMsg));
			try {
				verify(vdcMock, times(4)).createOrgVdcNetwork(networkParamsMock);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Failed due to Exception: " + vcex.getMessage());
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}		
	}
	
	
	/**
	 * ########## Vdc.getEdgeGatewayRefs() Test Cases - processVdcRestCall(Vdc targetVdc)
	 * ########## targetVdc.getEdgeGatewayRefs();
	 */
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and no Exceptions are thrown.
     */
	@Test
	public void processVdcGetEdgeGatewayRefsNoErrors()
	{				
		// Mocked return object
		ReferenceResult refsMockResult = mock(ReferenceResult.class);
				
		try
		{
			// Set up our expected return from the mock when called by the invoked method
			doReturn(refsMockResult).
			when(vdcMock).getEdgeGatewayRefs();
			
			// Call the method under test
			ReferenceResult result = restUtil.processVdcRestCall(vdcMock);
			
			// Check out results - we should get back the result with no retries
			verify(vdcMock, times(1)).getEdgeGatewayRefs();
			Assert.assertEquals(refsMockResult, result);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
    
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and One Exceptions are thrown.
     */
	@Test
	public void processVdcGetEdgeGatewayRefsOneRuntimeException()
	{				
		// Mocked return object
		ReferenceResult refsMockResult = mock(ReferenceResult.class);
		
		try
		{
			// Set up our expected 1 Exception then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doReturn(refsMockResult).
			when(vdcMock).getEdgeGatewayRefs();
			
			// Call the method under test
			ReferenceResult result = restUtil.processVdcRestCall(vdcMock);
			
			// Check out results - we should get back the result with 1 retry
			verify(vdcMock, times(2)).getEdgeGatewayRefs();
			Assert.assertEquals(refsMockResult, result);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Two Exceptions are thrown.
     */
	@Test
	public void processVdcGetEdgeGatewayRefsTwoRuntimeExceptions()
	{				
		// Mocked return object
		ReferenceResult refsMockResult = mock(ReferenceResult.class);
		
		try
		{
			// Set up our expected 2 Exceptions then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(refsMockResult).
			when(vdcMock).getEdgeGatewayRefs();
			
			// Call the method under test
			ReferenceResult result = restUtil.processVdcRestCall(vdcMock);
			
			// Check out results - we should get back the result with 2 retries
			verify(vdcMock, times(3)).getEdgeGatewayRefs();
			Assert.assertEquals(refsMockResult, result);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}
	
    /**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVdcGetEdgeGatewayRefsThreeRuntimeExceptions()
	{				
		// Mocked return object
		ReferenceResult refsMockResult = mock(ReferenceResult.class);
		
		try
		{
			// Set up our expected 3 Exception then return from the mock when called by the invoked method
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(refsMockResult).
			when(vdcMock).getEdgeGatewayRefs();
			
			// Call the method under test
			ReferenceResult result = restUtil.processVdcRestCall(vdcMock);
			
			// Check out results - we should get back the result with 3 retries
			verify(vdcMock, times(4)).getEdgeGatewayRefs();
			Assert.assertEquals(refsMockResult, result);

		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}
	}    
	
	/**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVdcGetEdgeGatewayRefsFourRuntimeExceptions()
	{				
		// Mocked return object
		ReferenceResult refsMockResult = mock(ReferenceResult.class);
		
		try
		{
			// Set up our expected 4 Exceptions then return from the mock when called by the invoked method. The last call should not be made.
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doReturn(refsMockResult).
			when(vdcMock).getEdgeGatewayRefs();
			
			// Call the method under test
			ReferenceResult result = restUtil.processVdcRestCall(vdcMock);
						
			fail("Failed due to expected Exception not being thrown.");
		}
		catch(RuntimeException ex)
		{
			// this is out success condition after exceeding max retries
			ex.printStackTrace();
			Assert.assertTrue(ex.getMessage().contains("Fatal Exception during processing of Vdc Rest call:"));
			try {
				verify(vdcMock, times(4)).getEdgeGatewayRefs();
			} catch (Exception e) {
				e.printStackTrace();
				fail("Failed due to Exception: " + ex.getMessage());
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}		
	}
	
	/**
     * Test method to ensure that the correct collaborator method signature is called when calling
     * the method under test and Three Exceptions are thrown.
     */
	@Test
	public void processVdcGetEdgeGatewayRefsThreeRuntimeOneVCloudExceptions()
	{				
		// Mocked return object
		ReferenceResult refsMockResult = mock(ReferenceResult.class);
		
		String vCloudErorMsg = "Unit Test Thrown Exception";
		
		try
		{
			// Set up our expected 4 Exceptions then return from the mock when called by the invoked method. The last call should not be made.
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new RuntimeException()).
			doThrow(new VCloudException(vCloudErorMsg)).
			doReturn(refsMockResult).
			when(vdcMock).getEdgeGatewayRefs();
			
			// Call the method under test
			ReferenceResult result = restUtil.processVdcRestCall(vdcMock);
						
			fail("Failed due to expected Exception not being thrown.");
		}
		catch(VCloudException vcex)
		{
			// this is out success condition after exceeding max retries
			vcex.printStackTrace();
			Assert.assertTrue(vcex.getMessage().contains(vCloudErorMsg));
			try {
				verify(vdcMock, times(4)).getEdgeGatewayRefs();
			} catch (Exception e) {
				e.printStackTrace();
				fail("Failed due to Exception: " + vcex.getMessage());
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			fail("Failed due to Exception: " + ex.getMessage());
		}		
	}
	
	
}
