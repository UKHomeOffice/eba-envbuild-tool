package com.ipt.ebsa.agnostic.client.skyscape.module;

import javax.xml.bind.JAXBElement;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLDNATType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNATType;
import com.vmware.vcloud.api.rest.schema.GatewayFeaturesType;
import com.vmware.vcloud.api.rest.schema.GatewayNatRuleType;
import com.vmware.vcloud.api.rest.schema.NatRuleType;
import com.vmware.vcloud.api.rest.schema.NatServiceType;
import com.vmware.vcloud.api.rest.schema.NetworkServiceType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.admin.EdgeGateway;
import com.vmware.vcloud.sdk.constants.NatTypeType;

public class EdgeGatewayModule {
	
	private Logger logger = LogManager.getLogger(EdgeGatewayModule.class);
	
	public EdgeGateway getEdgeGatewayByReference(VcloudClient client, ReferenceType edgeGatewayTypeRef) {
		
		
		EdgeGateway edgeGateway = null;
		
		try {
			edgeGateway =  EdgeGateway.getEdgeGatewayByReference(client, edgeGatewayTypeRef);
		} catch (VCloudException e) {
			throw new IllegalArgumentException("The requested edge gateway reference ["+edgeGatewayTypeRef+"] did not exist");
		}
		return edgeGateway;
	}
	
	/**
	 * Creates an organisation Network
	 * @param xmlEdgeGatewayServicesType 
	 * @param vdc
	 * @param vapp
	 * @param networkName
	 * @param type
	 * @throws VCloudException
	 */
	public void updateEdgeGateway(EdgeGateway edgeGateway, Vdc externalNetwork, String name, XMLGatewayType xmlEdgeGatewayServicesType) throws VCloudException {
		logger.debug(String.format("Updating edge gateway '%s' with reference '%s' ", name, edgeGateway.getReference().getName()));

		GatewayFeaturesType createGatewayFeatures = createGatewayFeatures(externalNetwork, xmlEdgeGatewayServicesType);
		new TaskUtil().waitForTask(edgeGateway.configureServices(createGatewayFeatures));
		logger.debug(String.format("Finished creating organisation network '%s'", name));
	}
	
	
	/**
	    * Create params for Edge Gateway
	    *
	    * @param externalNetwork
	    *           {@link ReferenceType}
	 * @param xmlEdgeGatewayServicesType 
	    * @return GatewayType
	    * @throws VCloudException
	    */
	   private GatewayFeaturesType createGatewayFeatures(Vdc externalNetwork, XMLGatewayType xmlEdgeGatewayServicesType)
	         throws VCloudException {
		  logger.debug("Building gateway features for edge gateway");

	      GatewayFeaturesType gatewayFeatures = new GatewayFeaturesType();

	      ObjectFactory objectFactory = new ObjectFactory();
	      
	      logger.debug("Building nat rules");
	      
	      NatServiceType natService = new NatServiceType();
	      natService.setNatType(NatTypeType.IPTRANSLATION.name());
	      natService.setIsEnabled(true);
	      
	      for(XMLNATType natRule : xmlEdgeGatewayServicesType.getNAT()) {
		      //Edge Gateway NAT service configuration
	    	  
	    	  NatRuleType natRuleType = new NatRuleType();
	    	  if(natRule.getDNAT() != null){
	    		  XMLDNATType dnat = natRule.getDNAT();
	    		  natRuleType.setIsEnabled(natRule.isEnabled());
	    		  natRuleType.setRuleType("DNAT");
	    		  GatewayNatRuleType gatewayNatRule = new GatewayNatRuleType();
	    		  gatewayNatRule.setOriginalIp(natRule.getOriginalSourceIpOrRange());
	    		  gatewayNatRule.setOriginalPort(dnat.getProtocolOriginalPort());
	    		  gatewayNatRule.setProtocol(dnat.getProtocolType());
	    		  gatewayNatRule.setTranslatedIp(natRule.getTranslatedSourceIpOrRange());
	    		  gatewayNatRule.setTranslatedPort(dnat.getTranslatedPort());
	    		  ReferenceType externalInterface = externalNetwork.getAvailableNetworkRefByName(natRule.getAppliedOn());
	    		  gatewayNatRule.setInterface(externalInterface);
	    		  natRuleType.setGatewayNatRule(gatewayNatRule);
	    		  
	    	  } 
//	    	  else if (natRule.getSNAT() != null) {
//	    		  XMLSNAT snat = natRule.getSNAT();
//	    		  natRuleType.setIsEnabled(snat.isEnabled());
//	    		  natRuleType.setRuleType("SNAT");
//	    		  GatewayNatRuleType gatewayNatRule = new GatewayNatRuleType();
//	    		  ReferenceType externalInterface = externalNetwork.getAvailableNetworkRefByName(snat.getAppliedOn());
//	    		  gatewayNatRule.setInterface(externalInterface);
//	    		  gatewayNatRule.setOriginalIp(snat.getOriginalSourceIPOrRange());
//	    		  gatewayNatRule.setTranslatedIp(snat.getTranslatedSourceIPOrRange());
//	    		  gatewayNatRule.setProtocol("any");
//	    		  natRuleType.setGatewayNatRule(gatewayNatRule);
//	    	  }
	    	  natService.getNatRule().add(natRuleType);
		      
		      
	      }
	      
	      JAXBElement<NetworkServiceType> serviceType = objectFactory.createNetworkService(natService);
	      gatewayFeatures.getNetworkService().add(serviceType);
	      /*logger.debug("Building static routes");
	      //Edge Gateway Static Routing service configuration
	      StaticRoutingServiceType staticRouting = new StaticRoutingServiceType();
	      staticRouting.setIsEnabled(true);
	      StaticRouteType staticRoute = new StaticRouteType();
	      staticRoute.setName("RouteName");
	      staticRoute.setNetwork(".2.0/24");
	      
	      staticRoute.setNextHopIp("xxx");
	      staticRoute.setGatewayInterface(externalNetwork);
	      staticRoute.setInterface("External");
	      staticRouting.getStaticRoute().add(staticRoute);

	      JAXBElement<StaticRoutingServiceType> route =
	            objectFactory.createStaticRoutingService(staticRouting);
	      gatewayFeatures.getNetworkService().add(route);*/

	      return gatewayFeatures;
	   }

}
