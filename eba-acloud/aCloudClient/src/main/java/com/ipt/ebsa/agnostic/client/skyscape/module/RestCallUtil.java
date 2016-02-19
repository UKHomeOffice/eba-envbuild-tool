package com.ipt.ebsa.agnostic.client.skyscape.module;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.vmware.vcloud.api.rest.schema.ComposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.ControlAccessParamsType;
import com.vmware.vcloud.api.rest.schema.EntityType;
import com.vmware.vcloud.api.rest.schema.OrgVdcNetworkType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.OrgVdcNetwork;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.ReferenceResult;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.VcloudEntity;
import com.vmware.vcloud.sdk.Vdc;

/**
 * Generic logic for handling retry and waiting for REST calls made direct to Skyscape from VCloudEntities.
 * 
 * Current implementation is to have a separate method for each type of VCloudEntity call, as making the method 
 * generic introduces a lot of unnecessary complexity in terms of type conversion, method determination etc. 
 * 
 * This can be revised in the future if required.
 * (If all the VCloudEntity.getByReference() methods has the same signature, then all would be gravy...)
 * 
 *
 */
public class RestCallUtil
{

	Logger logger = LogManager.getLogger(RestCallUtil.class);
	Logger performanceLogger = LogManager.getLogger("performancelogger");
	
	private int retryCount = 0;
	private static int SLEEP_TIME = 5000;
	private static int MAX_RETRIES = 3;
	private static final String FATAL_ERROR_MSG = "Fatal Exception during processing of Vdc Rest call: ";
	
	public RestCallUtil() {
		
	}
	
	public RestCallUtil(int sleep, int retry) {
		SLEEP_TIME = sleep;
		MAX_RETRIES = retry;
	}
	
	/**
	 * Method to process VApp Rest calls to getVappByReference(). If we get an exception from the call it is often down to 
	 * a temporary issue either connecting to, or an internal processing issue with, Skyscape.
	 * If we retry 3 times to no avail, we throw the exception back to the caller to handle appropriately.
	 * 
	 * @param vCloudClient
	 * @param ref
	 * @return
	 * @throws VCloudException
	 */
	public Vapp processVAppRestCall(VcloudClient vCloudClient, ReferenceType ref) throws VCloudException
	{			
		logger.debug("processVAppRestCall() for getVappByReference() >> IN");
		logger.debug("processVAppRestCall() :: ReferenceType is: " + ref.getName());
		logger.debug("processVAppRestCall() :: retryCount = " + retryCount);
		
		try
		{
			logger.debug("processVAppRestCall() :: making the Vapp method call.");
			return Vapp.getVappByReference(vCloudClient, ref);
		}		
		catch (Exception e)
		{
			logger.debug("processVAppRestCall() :: Exception caught: " + e.getMessage() + ", retryCount is: " + retryCount);
			
			if (retryCount < MAX_RETRIES)
			{
				retryCount++;
				logger.debug("processVAppRestCall() :: about the sleep for 5 seconds then retry the call.");
				
				// We will wait and then retry the Rest call to Skyscape, in case it was a temporary issue
				sleepAWhile();			
				return processVAppRestCall(vCloudClient, ref);
			}
			else
			{
				// We are out of retry attempts, we need to throw the Exception
				handleException(e);
				return null;
			}		
		}	
	}
	
	
	/**
	 * Method to process VApp Rest calls to updateControlAccess(). If we get an exception from the call it is often down to 
	 * a temporary issue either connecting to, or an internal processing issue with, Skyscape.
	 * If we retry 3 times to no avail, we throw the exception back to the caller to handle appropriately.
	 * 
	 * @param vApp
	 * @param controlAccessParams
	 * @return
	 * @throws VCloudException
	 */
	public ControlAccessParamsType processVAppRestCall(Vapp vApp, ControlAccessParamsType controlAccessParams) throws VCloudException
	{			
		logger.debug("processVAppRestCall() for updateControlAccess() >> IN");
		logger.debug("processVAppRestCall() :: retryCount = " + retryCount);
				
		try
		{
			logger.debug("processVAppRestCall() :: making the Vapp method call.");
			return vApp.updateControlAccess(controlAccessParams);
		}		
		catch (Exception e)
		{
			logger.debug("processVAppRestCall() :: Exception caught: " + e.getMessage() + ", retryCount is: " + retryCount);
			
			if (retryCount < MAX_RETRIES)
			{
				retryCount++;
				logger.debug("processVAppRestCall() :: about the sleep for 5 seconds then retry the call.");
				
				// We will wait and then retry the Rest call to Skyscape, in case it was a temporary issue
				sleepAWhile();			
				return processVAppRestCall(vApp, controlAccessParams);
			}
			else
			{
				// We are out of retry attempts, we need to throw the Exception
				handleException(e);
				return null;
			}
		}
	}
	
	
	/**
	 * Method to process Vdc Rest calls to getVdcByReference(). If we get an exception from the call it is often down to 
	 * a temporary issue either connecting to, or an internal processing issue with, Skyscape.
	 * If we retry 3 times to no avail, we throw the exception back to the caller to handle appropriately.
	 * 
	 * @param vCloudClient
	 * @param ref
	 * @return
	 * @throws VCloudException
	 */
	public Vdc processVdcRestCall(VcloudClient vCloudClient, ReferenceType ref) throws VCloudException
	{
		logger.debug("processVdcRestCall() for getVdcByReference() >> IN");
		logger.debug("processVdcRestCall() :: ReferenceType is: " + ref.getName());
		logger.debug("processVdcRestCall() :: retryCount = " + retryCount);
		
		try
		{
			logger.debug("processVdcRestCall() :: making the Vdc method call.");
			return Vdc.getVdcByReference(vCloudClient, ref);
		}
		catch (Exception e)
		{
			logger.debug("processVdcRestCall() :: Exception caught: " + e.getMessage() + ", retryCount is: " + retryCount);
						
			if (retryCount < MAX_RETRIES)
			{
				retryCount++;
				logger.debug("processVdcRestCall() :: about the sleep for 5 seconds then retry the call.");
				
				// We will wait and then retry the Rest call to Skyscape, in case it was a temporary issue
				sleepAWhile();
				return processVdcRestCall(vCloudClient, ref);
			}
			else
			{
				// We are out of retry attempts, we need to throw the Exception
				handleException(e);
				return null;
			}			
		}			
	}
	
	
	/**
	 * Method to process Organization Rest calls to getOrganizationByReference(). If we get an exception from the call it is often down to 
	 * a temporary issue either connecting to, or an internal processing issue with, Skyscape.
	 * If we retry 3 times to no avail, we throw the exception back to the caller to handle appropriately.
	 * 
	 * @param vCloudClient
	 * @param ref
	 * @return
	 * @throws VCloudException
	 */
	public Organization processOrgRestCall(VcloudClient vCloudClient, ReferenceType ref) throws VCloudException
	{
		logger.debug("processOrgRestCall() for getOrganizationByReference() >> IN");
		logger.debug("processOrgRestCall() :: ReferenceType is: " + ref.getName());
		logger.debug("processOrgRestCall() :: retryCount = " + retryCount);
				
		try
		{
			logger.debug("processOrgRestCall() :: making the Org method call.");
			return Organization.getOrganizationByReference(vCloudClient, ref);
		}
		catch (Exception e)
		{
			logger.debug("processOrgRestCall() :: Exception caught: " + e.getMessage() + ", retryCount is: " + retryCount);
						
			if (retryCount < MAX_RETRIES)
			{
				retryCount++;
				logger.debug("processOrgRestCall() :: about the sleep for 5 seconds then retry the call.");
				
				// We will wait and then retry the Rest call to Skyscape, in case it was a temporary issue
				sleepAWhile();
				return processOrgRestCall(vCloudClient, ref);
			}
			else
			{
				// We are out of retry attempts, we need to throw the Exception
				handleException(e);
				return null;
			}			
		}				
	}
	
	
	/**
	 * Method to process Vdc Rest calls to composeVapp(). If we get an exception from the call it is often down to 
	 * a temporary issue either connecting to, or an internal processing issue with, Skyscape.
	 * If we retry 3 times to no avail, we throw the exception back to the caller to handle appropriately.
	 * 
	 * @param targetVdc
	 * @param vAppParams
	 * @return
	 * @throws VCloudException
	 */
	public Vapp processVdcRestCall(Vdc targetVdc, ComposeVAppParamsType vAppParams) throws VCloudException
	{
		logger.debug("processVdcRestCall() for composeVapp() >> IN");
		logger.debug("processVdcRestCall() :: VApp Name is: " + vAppParams.getName());
		logger.debug("processVdcRestCall() :: retryCount = " + retryCount);
		
		try
		{
			logger.debug("processVdcRestCall() :: making the Vdc method call.");
			return targetVdc.composeVapp(vAppParams);
		}
		catch (Exception e)
		{
			logger.debug("processVdcRestCall() :: Exception caught: " + e.getMessage() + ", retryCount is: " + retryCount);
						
			if (retryCount < MAX_RETRIES)
			{
				retryCount++;
				logger.debug("processVdcRestCall() :: about the sleep for 5 seconds then retry the call.");
				
				// We will wait and then retry the Rest call to Skyscape, in case it was a temporary issue
				sleepAWhile();
				return processVdcRestCall(targetVdc, vAppParams);
			}
			else
			{
				// We are out of retry attempts, we need to throw the Exception
				handleException(e);
				return null;
			}			
		}			
	}
		
	
	/**
	 * Method to process Vdc Rest calls to createOrgVdcNetwork(). If we get an exception from the call it is often down to 
	 * a temporary issue either connecting to, or an internal processing issue with, Skyscape.
	 * If we retry 3 times to no avail, we throw the exception back to the caller to handle appropriately.
	 * 
	 * @param targetVdc
	 * @param networkParams
	 * @return
	 * @throws VCloudException
	 */
	public OrgVdcNetwork processVdcRestCall(Vdc targetVdc, OrgVdcNetworkType networkParams) throws VCloudException
	{
		logger.debug("processVdcRestCall() for createOrgVdcNetwork() >> IN");
		logger.debug("processVdcRestCall() :: Network Name is: " + networkParams.getName());
		logger.debug("processVdcRestCall() :: retryCount = " + retryCount);
				
		try
		{
			logger.debug("processVdcRestCall() :: making the Vdc method call.");
			return targetVdc.createOrgVdcNetwork(networkParams);
		}
		catch (Exception e)
		{
			logger.debug("processVdcRestCall() :: Exception caught: " + e.getMessage() + ", retryCount is: " + retryCount);
						
			if (retryCount < MAX_RETRIES)
			{
				retryCount++;
				logger.debug("processVdcRestCall() :: about the sleep for 5 seconds then retry the call.");
				
				// We will wait and then retry the Rest call to Skyscape, in case it was a temporary issue
				sleepAWhile();
				return processVdcRestCall(targetVdc, networkParams);
			}
			else
			{
				// We are out of retry attempts, we need to throw the Exception
				handleException(e);
				return null;
			}		
		}			
	}
	
	
	/**
	 * Method to process Vdc Rest calls to getEdgeGatewayRefs(). If we get an exception from the call it is often down to 
	 * a temporary issue either connecting to, or an internal processing issue with, Skyscape.
	 * If we retry 3 times to no avail, we throw the exception back to the caller to handle appropriately.
	 * 
	 * @param targetVdc
	 * @return
	 * @throws VCloudException
	 */
	public ReferenceResult processVdcRestCall(Vdc targetVdc) throws VCloudException
	{
		logger.debug("processVdcRestCall() for getEdgeGatewayRefs() >> IN");
		logger.debug("processVdcRestCall() :: retryCount = " + retryCount);
				
		try
		{
			logger.debug("processVdcRestCall() :: making the Vdc method call.");
			return targetVdc.getEdgeGatewayRefs();
		}
		catch (Exception e)
		{
			logger.debug("processVdcRestCall() :: Exception caught: " + e.getMessage() + ", retryCount is: " + retryCount);
						
			if (retryCount < MAX_RETRIES)
			{
				retryCount++;
				logger.debug("processVdcRestCall() :: about the sleep for 5 seconds then retry the call.");
				
				// We will wait and then retry the Rest call to Skyscape, in case it was a temporary issue
				sleepAWhile();
				return processVdcRestCall(targetVdc);
			}
			else
			{
				// We are out of retry attempts, we need to throw the Exception
				handleException(e);
				return null;
			}						
		}
	}		
	
	
	/**
	 * Method to allow a pause in the current thread before continuing processing.
	 */
	private void sleepAWhile()
	{
		try {
			logger.debug("About to sleep for " +  SLEEP_TIME + " ms");
			Thread.sleep(SLEEP_TIME);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	
	/**
	 * Method to process which type of exception to throw once failure has occurred.
	 * 
	 * @param e
	 * @throws VCloudException
	 */
	private void handleException(Exception e) throws VCloudException
	{
		// We are out of retry attempts, we need to throw the Exception
		logger.error("Rethrowing the caught Exception as we are out of retry attempts.");
		
		// We want to throw VCloudExceptions directly as the calling process should know what to do with them contextually
		if(e instanceof VCloudException)
		{
			throw (VCloudException)e;
		}
		
		throw new RuntimeException(FATAL_ERROR_MSG + e.getMessage(), e);
	}


	/**
	 * Prototype untested method to process generic ByReference VCloud Client Rest calls. If we get an exception from the call it is often down to 
	 * a temporary issue either connecting to, or an internal processing issue with, Skyscape.
	 * If we retry 3 times to no avail, we throw the exception back to the caller to handle appropriately.
	 * 
	 * @TODO - make work with both Class params for static methods and instance params.
	 * @TODO - implement fully if required.
	 * 
	 * @param vCloudClient
	 * @param ref
	 * @param entity Class of our generic VCloudEntity with wild-carded EntityType
	 * @param methodName
	 * @return 
	 * @throws VCloudException
	 */
	@Deprecated
	private VcloudEntity<? extends EntityType> prototypeProcessGenericRestCall(VcloudClient vCloudClient, ReferenceType ref, 
			VcloudEntity<? extends EntityType> entity, String methodName) throws VCloudException
	{
		logger.debug("processGenericRestCall() >> IN");
		logger.debug("processGenericRestCall() :: retryCount = " + retryCount);
		logger.debug("Entity Type is: " + entity.getClass());
				
		VcloudEntity<? extends EntityType> result = null;
				
		try
		{
			// We get the method we are going to invoke
			Method method = entity.getClass().getMethod(methodName, VcloudClient.class, ReferenceType.class);
			
			// Invoke the method directly
			Object rawResult = method.invoke(entity, vCloudClient, ref);

			// Check we can cast, then cast to the appropriate return type
			if(rawResult instanceof VcloudEntity<?>)
			{
				result = (VcloudEntity<?>)rawResult;
			}
		}
		catch(NoSuchMethodException nsme)
		{
			logger.error("We have an incorrect method name " + methodName + " for this class: " + entity.getClass() + nsme.getMessage(), nsme);
			throw new RuntimeException("Unable to process REST API call", nsme);
		}
		catch(SecurityException se)
		{
			logger.error("We cannot access the method name " + methodName + " for this class: " + entity.getClass() + se.getMessage(), se);
			throw new RuntimeException("Unable to process REST API call", se);
		}
		catch(IllegalAccessException iae)
		{
			logger.error("We cannot access the method name " + methodName + " for this class: " + entity.getClass() + iae.getMessage(), iae);
			throw new RuntimeException("Unable to process REST API call", iae);
		}
		catch(IllegalArgumentException iarge)
		{
			logger.error("We have illegal arguments for the name " + methodName + " for this class: " + entity.getClass() + iarge.getMessage(), iarge);
			throw new RuntimeException("Unable to process REST API call", iarge);
		}
		catch(InvocationTargetException ite)
		{
			logger.error("We cannot invoke the method name " + methodName + " for this class: " + entity.getClass() + ite.getMessage(), ite);
			throw new RuntimeException("Unable to process REST API call", ite);
		}
		catch (Exception e)
		{
			// We have a genuine exception from the method invocation
			if (retryCount < MAX_RETRIES)
			{
				retryCount++;
				
				// We will wait and then retry the Rest call to Skyscape, in case it was a temporary issue
				sleepAWhile();				
				prototypeProcessGenericRestCall(vCloudClient, ref, entity, methodName);
			}
			handleException(e);						
		}
		
		logger.debug("processGenericRestCall() >> OUT");
		return result;
	}
	
}
