package com.ipt.ebsa.agnostic.client.skyscape.manager;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.skyscape.connection.SkyscapeCloudValues;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.skyscape.module.EdgeGatewayModule;
import com.ipt.ebsa.agnostic.client.skyscape.module.RestCallUtil;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.ReferenceResult;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.admin.EdgeGateway;

public class EdgeGatewayManager {
private Logger logger = LogManager.getLogger(NetworkManager.class);
	
	@Inject
	private EdgeGatewayModule edgeGatewayModule;
	
	@Inject
	private SkyscapeCloudValues cv;

	/**
	 * Assign an organisation network into a vApp given the strategy and VApp parameters
	 * 
	 * @param strategy
	 * @param vappConfig
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws ConnectionException 
	 */
	public void configureServices(CmdStrategy strategy, XMLGatewayType edgeGatewayConfig) throws StrategyFailureException,
			UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException {
		logger.debug("configureServices entry - strategy:" + strategy);
		
		String edgeGatewayName = edgeGatewayConfig.getName();
		
		Vdc vDC = cv.getVdc(null);//TODO this is broken
		
		EdgeGatewayModule egm = new EdgeGatewayModule();
		ReferenceType edgeGatewayReference = findEdgeGatewayReferenceByName(vDC, edgeGatewayName);
		EdgeGateway edgeGatewayInstance = egm.getEdgeGatewayByReference(cv.getClient(), edgeGatewayReference);
		edgeGatewayModule.updateEdgeGateway(edgeGatewayInstance, vDC, edgeGatewayConfig.getName(), edgeGatewayConfig);
		

		logger.debug("createOrgNetworkInVApp exit");
	}

	
	/**
	 * Find an Edge Gateway Reference Type given its name
	 * 
	 * @param vDC
	 * @param edgeGatewayName
	 * @return
	 * @throws VCloudException
	 */
	public ReferenceType findEdgeGatewayReferenceByName(Vdc vDC, String edgeGatewayName) throws VCloudException {
		// - Route all VCloud REST calls through a retry mechanism
		ReferenceResult edgeGatewayReferences = new RestCallUtil().processVdcRestCall(vDC);
		if (edgeGatewayReferences != null) {
			for (ReferenceType edgeGatewayReference : edgeGatewayReferences.getReferences()) {
				logger.debug(String.format("Found Edge Gateway '%s' within the organisation", edgeGatewayReference.getName()));
				if (edgeGatewayReference.getName().equals(edgeGatewayName)) {
					return edgeGatewayReference;
				}
			}
		}
		logger.error(String.format("Cannot find Edge Gateway '%s' within the organisation", edgeGatewayName));
		//throw new RuntimeException("Cannot find Edge Gateway within this organisation");
		return null;
	}
	
	
}
