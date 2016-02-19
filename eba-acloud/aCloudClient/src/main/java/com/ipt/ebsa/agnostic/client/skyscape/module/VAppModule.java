package com.ipt.ebsa.agnostic.client.skyscape.module;

import java.util.concurrent.TimeoutException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.vmware.vcloud.api.rest.schema.ComposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.ControlAccessParamsType;
import com.vmware.vcloud.api.rest.schema.InstantiationParamsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.ovf.StartupSectionType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;

/**
 * Provides generally useful, stateless method calls for working with VApps
 *
 */
public class VAppModule {

	private Logger logger = LogManager.getLogger(VAppModule.class);

	/**
	 * Returns the Vapp after looking it up by reference
	 * @param vcloudClient
	 * @param containerVdc
	 * @param vappName
	 * @return
	 * @throws VCloudException
	 */
	public Vapp getVApp(VcloudClient vcloudClient, Vdc containerVdc, String vappName) throws VCloudException {
		ReferenceType vappReference = containerVdc.getVappRefByName(vappName);
		if (vappReference != null) {
			try {
				//  - route REST calls through a retry mechanism
				return new RestCallUtil().processVAppRestCall(vcloudClient, vappReference);
			} catch (com.vmware.vcloud.sdk.VCloudException e) {
				if (e.getMessage().contains("No access to entity")) {
					logger.error(e.getMessage() + " " + vappName + ".  It may be that you are using a stale VDC to look it up or it may be that it has been deleted and is still hanging around but is not accessible. ");
					return null;
				}
				else throw e;
			}
		} else {
			return null;
		}

	}
	
	/**
	 * Create a vApp with the parameters provided
	 * @param vcloudClient
	 * @param targetOrganisation
	 * @param targetVdc
	 * @param vAppName
	 * @param description
	 * @param deploy
	 * @param powerOn
	 * @return
	 * @throws VCloudException
	 */
	public Vapp createEmptyVApp(VcloudClient vcloudClient, Organization targetOrganisation, Vdc targetVdc, String vAppName, String description, boolean deploy, boolean powerOn) throws VCloudException {
		logger.debug("Creating empty vapp '" + vAppName + "' in organisation '"+targetOrganisation.getReference().getName()+"' and vdc '"+targetVdc.getReference().getName()+"'");
		ComposeVAppParamsType vappParams = getVAppParams(vAppName, description, deploy, powerOn);
		Vapp vApp = null;
		try {
			//  - Route all VCloud REST calls through a retry mechanism
			vApp = new RestCallUtil().processVdcRestCall(targetVdc, vappParams);
			
			/*
			 * this is a little tricky. Either we wait for the first one and it
			 * potentially isn't ours or we wait for them all. As it happens if
			 * you wait for all the tasks then this method never returns so we
			 * just wait for the first (like the examples) this could really do
			 * with being better
			 */
			vApp.getTasks().get(0).waitForTask(0);
						
			//  - Route all VCloud REST calls through a retry mechanism
			vApp = new RestCallUtil().processVAppRestCall(vcloudClient, vApp.getReference());
			
			// AA - share the vapp as read only with all users of the organisation
			ControlAccessParamsType controlAccessParams = new ControlAccessParamsType();
			controlAccessParams.setIsSharedToEveryone(true);
			controlAccessParams.setEveryoneAccessLevel("ReadOnly");
			
			//  - Route all VCloud REST calls through a retry mechanism
			new RestCallUtil().processVAppRestCall(vApp, controlAccessParams);
						
		} catch (TimeoutException e) {
			throw new RuntimeException("Timed out while waiting for vapp to be created");
		}
		logger.debug("Finished creating empty vapp '" + vAppName + "'");
		return vApp;
	}

	/**
	 * Deletes a VApp and checks to see that it has been removed from the VDC
	 * @param vcloudClient
	 * @param vdc
	 * @param vapp
	 * @throws VCloudException
	 */
	public void deleteVApp(VcloudClient vcloudClient, Vdc vdc, final Vapp vapp) throws VCloudException {
		String name = vapp.getReference().getName();
		
		logger.debug("Deleting vapp '" + name + "' from vdc '"+vdc.getReference().getName()+"'");
		new TaskUtil().waitForTask(vapp.delete());
		
		Vapp va = getVApp(vcloudClient, vdc, name);
		if (va != null) {
        	throw new RuntimeException("Expected the VApp '"+name+"' to have been deleted but it still exists within the VDC '"+vdc.getReference().getName()+"'");
        }
        logger.debug("Finished deleting vapp '" + name +"'");
	}

	/**
	 * Factory method for constructing an object
	 * @param vAppName
	 * @param deploy
	 * @return
	 */
	private ComposeVAppParamsType getVAppParams(String vAppName, String description, boolean deploy, boolean powerOn) {
		InstantiationParamsType instantiationParamsType = new InstantiationParamsType();

		ComposeVAppParamsType params = new ComposeVAppParamsType();
		params.setDeploy(deploy);
		params.setDescription(description);
		params.setName(vAppName);
		params.setInstantiationParams(instantiationParamsType);
		params.setPowerOn(powerOn);
		params.setAllEULAsAccepted(true);

		return params;
	}
	
	/**
	 *  - Allow update to Power On and Off order.
	 * 
	 * @throws VCloudException 
	 */
	public void updateVappStatupSection(Vapp vApp, StartupSectionType sectionType) throws VCloudException{
		new TaskUtil().waitForTask(vApp.updateSection(sectionType));
	}
}