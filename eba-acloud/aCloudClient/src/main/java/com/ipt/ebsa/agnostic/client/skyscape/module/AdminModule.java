package com.ipt.ebsa.agnostic.client.skyscape.module;

import java.util.Collection;

import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.Expression;
import com.vmware.vcloud.sdk.Filter;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.QueryParams;
import com.vmware.vcloud.sdk.ReferenceResult;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.admin.AdminOrganization;
import com.vmware.vcloud.sdk.admin.AdminVdc;
import com.vmware.vcloud.sdk.constants.query.ExpressionType;
import com.vmware.vcloud.sdk.constants.query.QueryReferenceField;
import com.vmware.vcloud.sdk.constants.query.QueryReferenceType;

/**
 * Provides generally useful, stateless method calls for working with VDC, Org, AdminVDC and AdminOrg objects
 * IN some cases you need elevated access for these to work.
 * 
 *
 */
public class AdminModule {
	
	/**
     * Creates a reference to the AdminOrganisation and returns it
     * 
     * @param vcloudClient
     * @param organisationName
     * @return
     * @throws VCloudException
     */
	public AdminOrganization getAdminOrganisation(VcloudClient vcloudClient, String organisationName) throws VCloudException {
		QueryParams<QueryReferenceField> params = new QueryParams<QueryReferenceField>();
		Filter filter = new Filter(new Expression(QueryReferenceField.NAME, organisationName, ExpressionType.EQUALS));
		params.setFilter(filter);
      
		ReferenceResult result  = vcloudClient.getQueryService().queryReferences(QueryReferenceType.ORGANIZATION, params);
      
		AdminOrganization adminOrg = AdminOrganization.getAdminOrgById(vcloudClient, result.getReferences().get(0).getId());
		return adminOrg;
	}
	
	/**
	 * This works ONLY IF YOUR USER HAS VDC ADMIN privileges (which pretty much no-one has so just forget about it).  You ony really need this for
	 * changing the vdc or adding edge gateways.
	 * @param vcloudClient
	 * @param adminOrg
	 * @return
	 * @throws VCloudException 
	 */
	public AdminVdc getAdminVdc(VcloudClient vcloudClient, AdminOrganization adminOrg, String vdcName) throws VCloudException {
		ReferenceType adminVDCRef =  adminOrg.getAdminVdcRefByName(vdcName);
        try {
			return AdminVdc.getAdminVdcByReference(vcloudClient, adminVDCRef);
		} catch (ClassCastException e) {
			throw new RuntimeException("If you are getting a classcast exception because a regular VDC cannot be cast into an AdminVDC then it is lilely you do not have VDCAdmin privileges. https://communities.vmware.com/thread/422847", e);
		}
	}
	

	/**
	 *  Look up and return a reference to a VDC
	 * @param vcloudClient
	 * @param organisation
	 * @param vdcName
	 * @return
	 * @throws VCloudException
	 */
	public Vdc getVDC(VcloudClient vcloudClient, Organization organisation, String vdcName) throws VCloudException {
		ReferenceType vdcRef = organisation.getVdcRefByName(vdcName);
		if(vdcName == null || vdcRef == null) {
			
			throw new IllegalArgumentException("VDC reference not found for VDC=["+vdcName+"] This is a configuration error. Possible VDC's =["+toStringBuilder(organisation.getVdcRefs())+"]");
		}
		//  - route all REST calls through a retry mechanism
		return new RestCallUtil().processVdcRestCall(vcloudClient, vdcRef);
	}
	
	private String toStringBuilder (Collection<ReferenceType> vdc) {
		
		StringBuilder sb = new StringBuilder();
		for(ReferenceType ref:vdc) {
			sb.append(ref.getName());
			sb.append(", ");
		}
		
		return sb.toString();
	}

	/**
	 * Get an organisation
	 * @param vcloudClient
	 * @param orgName
	 * @return
	 * @throws VCloudException
	 */
	public Organization getOrganisation(VcloudClient vcloudClient, String orgName) throws VCloudException {
		ReferenceType orgRef = vcloudClient.getOrgRefByName(orgName);
		//  Route VCLoud REST calls through a retry mechanism
		Organization org = new RestCallUtil().processOrgRestCall(vcloudClient, orgRef);
		return org;
	}


}