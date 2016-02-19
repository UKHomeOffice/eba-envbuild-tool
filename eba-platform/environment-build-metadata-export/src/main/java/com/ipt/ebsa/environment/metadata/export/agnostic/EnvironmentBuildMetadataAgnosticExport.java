package com.ipt.ebsa.environment.metadata.export.agnostic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.cloud.config.v1.ObjectFactory;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLApplicationNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLDNATType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLDataCenterType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType.XMLEnvironmentContainerDefinition;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentDefinitionType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentDefinitionTypeType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLMetaDataType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNATType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLStorageType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.ipt.ebsa.environment.build.entities.ApplicationNetwork;
import com.ipt.ebsa.environment.build.entities.ApplicationNetworkMetaData;
import com.ipt.ebsa.environment.build.entities.DNat;
import com.ipt.ebsa.environment.build.entities.DataCentre;
import com.ipt.ebsa.environment.build.entities.Environment;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition.DefinitionType;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinitionMetaData;
import com.ipt.ebsa.environment.build.entities.Gateway;
import com.ipt.ebsa.environment.build.entities.GeographicContainer;
import com.ipt.ebsa.environment.build.entities.Interface;
import com.ipt.ebsa.environment.build.entities.MetaData;
import com.ipt.ebsa.environment.build.entities.Nat;
import com.ipt.ebsa.environment.build.entities.Nic;
import com.ipt.ebsa.environment.build.entities.OrganisationNetwork;
import com.ipt.ebsa.environment.build.entities.OrganisationNetworkMetaData;
import com.ipt.ebsa.environment.build.entities.Storage;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;
import com.ipt.ebsa.environment.build.entities.VirtualMachineMetaData;
import com.ipt.ebsa.environment.metadata.export.EnvironmentBuildMetadataExport;

/**
 * Exports the Environment and EnvironmentContainer data from the ENVIRONMENT_BUILD database for input to the agnostic Cloud client 
 *
 */
public class EnvironmentBuildMetadataAgnosticExport extends EnvironmentBuildMetadataExport {
	
	/** Logger */
	private static final Logger LOG = LogManager.getLogger(EnvironmentBuildMetadataAgnosticExport.class);
	
	/** Unique id used to populate id attribute of XML elements */
	private int id = 1;
	
	/** Map of DB Gateway -> XML Gateway */
	private Map<Gateway, XMLGatewayType> gateways = new HashMap<Gateway, XMLGatewayType>();
	/** Map of Network name -> XML Network */
	private Map<String, XMLNetworkType> networks = new HashMap<String, XMLNetworkType>();
	/** Map of DataCenter name -> XML DataCenter */
	private Map<String, XMLDataCenterType> dataCenters = new HashMap<String, XMLDataCenterType>();
	/** Map of DB Environment id -> XML Environment */
	private Map<Integer, XMLEnvironmentType> environments = new HashMap<Integer, XMLEnvironmentType>();
	
	/**
	 * Extract an aCloud Environment version (includes the EnvironmentContainer)
	 * @param environmentName
	 * @param version
	 * @param provider
	 * @return
	 * @throws Exception
	 */
	public XMLGeographicContainerType extractEnvironment(String environmentName, String version, String provider) throws Exception {
		LOG.debug("Extracting Environment Definition data in agnostic format, environmentName=" + environmentName + ", version=" + version + ", provider=" + provider);
		// Fetch the Physical Environment Definition for the specified version of the named environment
		EnvironmentDefinition dbEnvDef = readManager.getEnvironmentDefinition(environmentName, version, DefinitionType.Physical, provider);
		if (dbEnvDef == null) {
			throw new RuntimeException("No Physical Environment Definition found for environmentName: " + environmentName + ", version: " + version + ", provider: " + provider);
		}
		GeographicContainer dbGeographicContainer =  dbEnvDef.getEnvironment().getEnvironmentcontainer().getGeographicContainer();
		XMLGeographicContainerType xmlGeographicContainer = new XMLGeographicContainerType();
		exportGeographicContainer(dbGeographicContainer, xmlGeographicContainer);
		EnvironmentContainer dbEnvironmentContainer = dbGeographicContainer.getEnvironmentcontainer();
		XMLEnvironmentContainerType xmlEnvironmentContainer = xmlGeographicContainer.getEnvironmentContainer();
		
		// Add currently deployed EnvironmentContainerDefinition to EnvironmentContainer
		EnvironmentContainerDefinition dbEnvironmentContainerDefinition = readManager.getEnvironmentContainerDefinitonCurrentlyDeployed(dbEnvironmentContainer.getName(), provider);
		if (dbEnvironmentContainerDefinition == null) {
			throw new RuntimeException("No successfully deployed Environment Container Definition for " + dbEnvironmentContainer);
		} else {
			LOG.info("Currently deployed Environment Container Definition is: " + dbEnvironmentContainerDefinition);
		}
		exportEnvironmentContainerDefinition(dbEnvironmentContainerDefinition, xmlEnvironmentContainer);
		
		// Add Environment to EnvironmentContainer
		exportEnvironment(dbEnvDef.getEnvironment(), xmlEnvironmentContainer);
		
		// Add EnvironmentDefinition to Environment
		exportEnvironmentDefinition(dbEnvDef, xmlEnvironmentContainer.getEnvironment().get(0));
		
		return xmlGeographicContainer;
	}
	
	/**
	 * Extract an aCloud Environment version (includes the EnvironmentContainer)
	 * @param environmentName
	 * @param version
	 * @param provider
	 * @return
	 * @throws Exception
	 */
	public XMLGeographicContainerType extractEnvironmentsDeployed(String environmentName, String provider) throws Exception {
		LOG.debug("Extracting Environment Definition data in agnostic format, environmentName=" + environmentName + ", provider=" + provider);

		ArrayList<String> envNames = new ArrayList<String>();
		envNames.add(environmentName);

		Map<String, List<EnvironmentDefinition>> definitions = readManager.getEnvironmentDefinitions(envNames,DefinitionType.Physical, provider);
		
		List<EnvironmentDefinition> defList = definitions.get(environmentName);
		BigDecimal latestVerion = new BigDecimal(0);
		EnvironmentDefinition latestDef = new EnvironmentDefinition();
		for(EnvironmentDefinition currentDef: defList) {
			BigDecimal currentVersion = new BigDecimal(currentDef.getVersion());
			if(currentVersion.compareTo(latestVerion) > 0) {
				latestVerion = currentVersion;
				latestDef = currentDef;
			}
		}
		EnvironmentDefinition dbEnvDef = readManager.getEnvironmentDefinition(environmentName , latestDef.getVersion(), DefinitionType.Physical, provider);
		if(dbEnvDef == null) {
			throw new RuntimeException("No Physical Environment Definition found for environmentName: " + environmentName + ", provider: " + provider);
		}
		
		GeographicContainer dbGeographicContainer =  dbEnvDef.getEnvironment().getEnvironmentcontainer().getGeographicContainer();
		XMLGeographicContainerType xmlGeographicContainer = new XMLGeographicContainerType();
		exportGeographicContainer(dbGeographicContainer, xmlGeographicContainer);
		EnvironmentContainer dbEnvironmentContainer = dbGeographicContainer.getEnvironmentcontainer();
		XMLEnvironmentContainerType xmlEnvironmentContainer = xmlGeographicContainer.getEnvironmentContainer();
		
		// Add currently deployed EnvironmentContainerDefinition to EnvironmentContainer
		EnvironmentContainerDefinition dbEnvironmentContainerDefinition = readManager.getEnvironmentContainerDefinitonCurrentlyDeployed(dbEnvironmentContainer.getName(), provider);
		if (dbEnvironmentContainerDefinition == null) {
			throw new RuntimeException("No successfully deployed Environment Container Definition for " + dbEnvironmentContainer);
		} else {
			LOG.info("Currently deployed Environment Container Definition is: " + dbEnvironmentContainerDefinition);
		}
		exportEnvironmentContainerDefinition(dbEnvironmentContainerDefinition, xmlEnvironmentContainer);
		
		// Add Environment to EnvironmentContainer
		exportEnvironment(dbEnvDef.getEnvironment(), xmlEnvironmentContainer);
		
		// Add EnvironmentDefinition to Environment
		exportEnvironmentDefinition(dbEnvDef, xmlEnvironmentContainer.getEnvironment().get(0));
		
		return xmlGeographicContainer;
		

	}
	
	/**
	 * Extract an aCloud Environment Container (Organisation) version
	 * @param environmentName
	 * @param version
	 * @return
	 * @throws Exception
	 */
	public XMLGeographicContainerType extractEnvironmentContainer(String environmentContainerName, String version, String provider) throws Exception {
		LOG.debug("Extracting Environment Container Definition data in agnostic format, environmentContainerName=" + environmentContainerName + ", version=" + version + ", provider=" + provider);
		// Fetch EnvironmentContainerDefinition for the specified version of the named environment container
		EnvironmentContainerDefinition dbEnvironmentContainerDefinition = readManager.getEnvironmentContainerDefinition(environmentContainerName, version, provider);
		if (dbEnvironmentContainerDefinition == null) {
			throw new RuntimeException("No Environment Container Definition found for environment container name: " + environmentContainerName + ", environment container definition version: " + version + ", provider: " + provider);
		}
		LOG.debug("Exporting " + dbEnvironmentContainerDefinition);
		GeographicContainer dbGeographicContainer =  dbEnvironmentContainerDefinition.getEnvironmentcontainer().getGeographicContainer();
		XMLGeographicContainerType xmlGeographicContainer = new XMLGeographicContainerType();
		exportGeographicContainer(dbGeographicContainer, xmlGeographicContainer);

		// Add EnvironmentContainerDefinition to EnvironmentContainer
		XMLEnvironmentContainerType xmlEnvironmentContainer = xmlGeographicContainer.getEnvironmentContainer();
		exportEnvironmentContainerDefinition(dbEnvironmentContainerDefinition, xmlEnvironmentContainer);
		
		return xmlGeographicContainer;
	}
	
	/**
	 * Extract an aCloud Environment Container (Organisation) version
	 * @param environmentName
	 * @param version
	 * @return
	 * @throws Exception
	 */
	public XMLGeographicContainerType extractEnvironmentContainerDeployed(String environmentContainerName, String provider) throws Exception {
		LOG.debug("Extracting Environment Container Definition data in agnostic format, environmentContainerName=" + environmentContainerName +", provider=" + provider);
		// Fetch EnvironmentContainerDefinition for the specified version of the named environment container
		EnvironmentContainerDefinition dbEnvironmentContainerDefinition = readManager.getEnvironmentContainerDefinitonCurrentlyDeployed(environmentContainerName, provider);
		if (dbEnvironmentContainerDefinition == null) {
			throw new RuntimeException("No Environment Container Definition found for environment container name: " + environmentContainerName + ", provider: " + provider);
		}
		LOG.debug("Exporting " + dbEnvironmentContainerDefinition);
		GeographicContainer dbGeographicContainer =  dbEnvironmentContainerDefinition.getEnvironmentcontainer().getGeographicContainer();
		XMLGeographicContainerType xmlGeographicContainer = new XMLGeographicContainerType();
		exportGeographicContainer(dbGeographicContainer, xmlGeographicContainer);

		// Add EnvironmentContainerDefinition to EnvironmentContainer
		XMLEnvironmentContainerType xmlEnvironmentContainer = xmlGeographicContainer.getEnvironmentContainer();
		exportEnvironmentContainerDefinition(dbEnvironmentContainerDefinition, xmlEnvironmentContainer);
		
		return xmlGeographicContainer;
	}
	
	/**
	 * Export GeographicContainer 
	 * @param dbGeographicContainer
	 * @param xmlGeographicContainer
	 */
	private void exportGeographicContainer(GeographicContainer dbGeographicContainer, XMLGeographicContainerType xmlGeographicContainer) {
		LOG.debug("Exporting " + dbGeographicContainer);
		xmlGeographicContainer.setId(nextId());
		xmlGeographicContainer.setAccount(dbGeographicContainer.getAccount());
		xmlGeographicContainer.setRegion(dbGeographicContainer.getRegion());
		exportEnvironmentContainer(dbGeographicContainer, xmlGeographicContainer);
	}	
	
	/**
	 * Export EnvironmentContainer
	 * @param dbGeographicContainer
	 * @param xmlGeographicContainer
	 */
	private void exportEnvironmentContainer(GeographicContainer dbGeographicContainer, XMLGeographicContainerType xmlGeographicContainer) {
		EnvironmentContainer dbEnvironmentContainer = dbGeographicContainer.getEnvironmentcontainer();
		LOG.debug("Exporting " + dbEnvironmentContainer);
		XMLEnvironmentContainerType xmlEnvironmentContainer = new XMLEnvironmentContainerType();
		xmlEnvironmentContainer.setId(nextId());
		xmlEnvironmentContainer.setName(dbEnvironmentContainer.getName());
		xmlEnvironmentContainer.setProvider(XMLProviderType.fromValue(dbEnvironmentContainer.getProvider()));
		xmlGeographicContainer.setEnvironmentContainer(xmlEnvironmentContainer);
	}
	
	/**
	 * Export Environment
	 * @param dbEnvironment
	 * @param xmlEnvironmentContainer
	 */
	private XMLEnvironmentType exportEnvironment(Environment dbEnvironment, XMLEnvironmentContainerType xmlEnvironmentContainer) {
		XMLEnvironmentType xmlEnvironment = environments.get(dbEnvironment.getId());
		if (xmlEnvironment == null) {
			LOG.debug("Exporting " + dbEnvironment);
			xmlEnvironment = new XMLEnvironmentType();
			xmlEnvironment.setId(nextId());
			xmlEnvironment.setName(dbEnvironment.getEnvironmentGroupName());
			xmlEnvironment.setNotes(dbEnvironment.getNotes());
			xmlEnvironment.setEnvironmentContainerDefinitionId(xmlEnvironmentContainer.getEnvironmentContainerDefinition().get(0));
			xmlEnvironmentContainer.getEnvironment().add(xmlEnvironment);
			environments.put(dbEnvironment.getId(), xmlEnvironment);
		}
		return xmlEnvironment;
	}
	
	/**
	 * Export EnvironmentDefinition
	 * @param dbEnvironmentDefinition
	 * @param xmlEnvironment
	 */
	private void exportEnvironmentDefinition(EnvironmentDefinition dbEnvironmentDefinition, XMLEnvironmentType xmlEnvironment) {
		LOG.debug("Exporting " + dbEnvironmentDefinition);
		XMLEnvironmentDefinitionType xmlEnvironmentDefinition = new XMLEnvironmentDefinitionType();
		xmlEnvironmentDefinition.setId(nextId());
		xmlEnvironmentDefinition.setEnvironmentDefinitionType(XMLEnvironmentDefinitionTypeType.fromValue(dbEnvironmentDefinition.getDefinitionType()));
		xmlEnvironmentDefinition.setName(dbEnvironmentDefinition.getEnvironment().getEnvironmentGroupName());
		xmlEnvironmentDefinition.setVersion(dbEnvironmentDefinition.getVersion());
		xmlEnvironmentDefinition.setCidr(dbEnvironmentDefinition.getCidr());
	
		exportVirtualMachineContainers(dbEnvironmentDefinition, xmlEnvironmentDefinition);
		
		for (EnvironmentDefinitionMetaData dbMetaData : dbEnvironmentDefinition.getMetadata()) {
			XMLMetaDataType xmlMetaData = new XMLMetaDataType();
			exportMetaData(dbMetaData, xmlMetaData);
			xmlEnvironmentDefinition.getMetaData().add(xmlMetaData);
		}
		
		xmlEnvironment.getEnvironmentDefinition().add(xmlEnvironmentDefinition);
	}

	/**
	 * Export VirtualMachineContainers
	 * @param dbEnvironmentDefinition
	 * @param xmlEnvironmentDefinition
	 */
	private void exportVirtualMachineContainers(EnvironmentDefinition dbEnvironmentDefinition, XMLEnvironmentDefinitionType xmlEnvironmentDefinition) {
		LOG.debug("Exporting " + dbEnvironmentDefinition.getVirtualmachinecontainers().size() + " VirtualMachineContainer(s) from " + dbEnvironmentDefinition);
		for (VirtualMachineContainer dbVmc :  dbEnvironmentDefinition.getVirtualmachinecontainers()) {
			XMLVirtualMachineContainerType xmlVmc = new XMLVirtualMachineContainerType();
			xmlVmc.setId(nextId());
			xmlVmc.setDeploy(dbVmc.getDeploy());
			xmlVmc.setDescription(dbVmc.getDescription());
			xmlVmc.setDomain(dbVmc.getDomain());
			xmlVmc.setName(dbVmc.getName());
			xmlVmc.setPowerOn(dbVmc.getPowerOn());
			xmlVmc.setRuntimeLease(dbVmc.getRuntimeLease());
			xmlVmc.setServiceLevel(dbVmc.getServiceLevel());
			xmlVmc.setStorageLease(dbVmc.getStorageLease());
			
			String dataCenterName = dbVmc.getDataCentreName();
			xmlVmc.setDataCenterName(dataCenterName);
			if (StringUtils.isNotBlank(dataCenterName)) {
				XMLDataCenterType xmlDataCenter = dataCenters.get(dataCenterName);
				if (xmlDataCenter == null) {
					throw new RuntimeException("DataCenter: " + dataCenterName + " not found in currently deployed Environment Container Definition for: " + dbVmc);
				}
				xmlVmc.setDataCenterId(xmlDataCenter);
			}
			
			exportNetworks(dbVmc, xmlVmc);
			exportVirtualMachines(dbVmc, xmlVmc);
			
			xmlEnvironmentDefinition.getVirtualMachineContainer().add(xmlVmc);
		}
	}
	
	/**
	 * Export DataCenter
	 * @param dbDataCenter
	 * @param xmlDataCenter
	 */
	private void exportDataCenter(DataCentre dbDataCenter, XMLDataCenterType xmlDataCenter) {
		LOG.debug("Exporting " + dbDataCenter);
		xmlDataCenter.setId(nextId());
		xmlDataCenter.setName(dbDataCenter.getName());
		dataCenters.put(dbDataCenter.getName(), xmlDataCenter);
	}
	
	/**
	 * Export ApplicationNetworks
	 * @param dbVmc
	 * @param xmlVmc
	 */
	private void exportNetworks(VirtualMachineContainer dbVmc, XMLVirtualMachineContainerType xmlVmc) {
		LOG.debug("Exporting " + dbVmc.getNetworks() + " Application Network(s) from " + dbVmc);
		for (ApplicationNetwork dbNetwork : dbVmc.getNetworks()) {
			XMLNetworkType xmlNetwork = networks.get(dbNetwork.getName());
			if (xmlNetwork == null) {
				xmlNetwork = new XMLApplicationNetworkType();
				exportNetwork(dbNetwork, xmlNetwork);
			}
			xmlVmc.getNetwork().add((XMLApplicationNetworkType) xmlNetwork);
		}
	}
	
	/**
	 * Export OrganisationNetworks
	 * @param dbEnvironmentContainerDefinition
	 * @param xmlEnvironmentContainerDefinition
	 */
	private void exportNetworks(EnvironmentContainerDefinition dbEnvironmentContainerDefinition, XMLEnvironmentContainerDefinition xmlEnvironmentContainerDefinition) {
		LOG.debug("Exporting " + dbEnvironmentContainerDefinition.getNetworks().size() + " Organisation Network(s) from " + dbEnvironmentContainerDefinition);
		for (OrganisationNetwork dbNetwork : dbEnvironmentContainerDefinition.getNetworks()) {
			XMLNetworkType xmlNetwork = networks.get(dbNetwork.getName());
			if (xmlNetwork == null) {
				xmlNetwork = new XMLOrganisationalNetworkType();
				exportNetwork(dbNetwork, (XMLOrganisationalNetworkType) xmlNetwork);
			}
			xmlEnvironmentContainerDefinition.getNetwork().add((XMLOrganisationalNetworkType) xmlNetwork);
		}
	}

	/**
	 * Export Application Network
	 * @param dbNetwork
	 * @param xmlNetwork
	 */
	private void exportNetwork(ApplicationNetwork dbNetwork, XMLNetworkType xmlNetwork) {
		if (networks.get(dbNetwork.getName()) == null) {
			xmlNetwork.setId(nextId());
			xmlNetwork.setCIDR(dbNetwork.getCidr());
			xmlNetwork.setDescription(dbNetwork.getDescription());
			xmlNetwork.setDnsSuffix(dbNetwork.getDnsSuffix());
			xmlNetwork.setFenceMode(dbNetwork.getFenceMode());
			xmlNetwork.setGatewayAddress(dbNetwork.getGatewayAddress());
			xmlNetwork.setIpRangeStart(dbNetwork.getIpRangeStart());
			xmlNetwork.setIpRangeEnd(dbNetwork.getIpRangeEnd());
			xmlNetwork.setName(dbNetwork.getName());
			xmlNetwork.setNetworkMask(dbNetwork.getNetworkMask());
			xmlNetwork.setPrimaryDns(dbNetwork.getPrimaryDns());
			xmlNetwork.setSecondaryDns(dbNetwork.getSecondaryDns());
			xmlNetwork.setShared(dbNetwork.getShared());
			xmlNetwork.setStaticIpPool(dbNetwork.getStaticIpPool());
			
			for (ApplicationNetworkMetaData dbMetaData : dbNetwork.getMetadata()) {
				XMLMetaDataType xmlMetaData = new XMLMetaDataType();
				exportMetaData(dbMetaData, xmlMetaData);
				xmlNetwork.getMetaData().add(xmlMetaData);
			}
			
			String dataCenterName = dbNetwork.getDataCentreName();
			xmlNetwork.setDataCenterName(dataCenterName);
			if (StringUtils.isNotBlank(dataCenterName)) {
				XMLDataCenterType xmlDataCenter = dataCenters.get(dataCenterName);
				if (xmlDataCenter == null) {
					throw new RuntimeException("DataCenter: " + dataCenterName + " not found in currently deployed Environment Container Definition for: " + dbNetwork);
				}
				xmlNetwork.setDataCenterId(xmlDataCenter);
			}
	
			networks.put(dbNetwork.getName(), xmlNetwork);
		}
	}
	
	
	/**
	 * Export Organisation Network
	 * @param dbNetwork
	 * @param xmlNetwork
	 */
	private void exportNetwork(OrganisationNetwork dbNetwork, XMLOrganisationalNetworkType xmlNetwork) {
		if (networks.get(dbNetwork.getName()) == null) {
			xmlNetwork.setId(nextId());
			xmlNetwork.setCIDR(dbNetwork.getCidr());
			xmlNetwork.setDataCenterName(dbNetwork.getDataCentreName());
			xmlNetwork.setDescription(dbNetwork.getDescription());
			xmlNetwork.setDnsSuffix(dbNetwork.getDnsSuffix());
			xmlNetwork.setFenceMode(dbNetwork.getFenceMode());
			xmlNetwork.setGatewayAddress(dbNetwork.getGatewayAddress());
			xmlNetwork.setIpRangeStart(dbNetwork.getIpRangeStart());
			xmlNetwork.setIpRangeEnd(dbNetwork.getIpRangeEnd());
			xmlNetwork.setName(dbNetwork.getName());
			xmlNetwork.setNetworkMask(dbNetwork.getNetworkMask());
			xmlNetwork.setPrimaryDns(dbNetwork.getPrimaryDns());
			xmlNetwork.setSecondaryDns(dbNetwork.getSecondaryDns());
			xmlNetwork.setShared(dbNetwork.getShared());
			xmlNetwork.setStaticIpPool(dbNetwork.getStaticIpPool());
			
			for (OrganisationNetworkMetaData dbMetaData : dbNetwork.getMetadata()) {
				XMLMetaDataType xmlMetaData = new XMLMetaDataType();
				exportMetaData(dbMetaData, xmlMetaData);
				xmlNetwork.getMetaData().add(xmlMetaData);
			}
	
			String dataCenterName = dbNetwork.getDataCentreName();
			xmlNetwork.setDataCenterName(dataCenterName);
			if (StringUtils.isNotBlank(dataCenterName)) {
				XMLDataCenterType xmlDataCenter = dataCenters.get(dataCenterName);
				if (xmlDataCenter == null) {
					throw new RuntimeException("DataCenter: " + dataCenterName + " not found in currently deployed Environment Container Definition for: " + dbNetwork);
				}
				xmlNetwork.setDataCenterId(xmlDataCenter);
			}
			
			xmlNetwork.setGatewayId(gateways.get(dbNetwork.getGateway()));
			xmlNetwork.setPeerEnvironmentName(dbNetwork.getPeerEnvironmentName());
			xmlNetwork.setPeerNetworkName(dbNetwork.getPeerNetworkName());
			
			networks.put(dbNetwork.getName(), xmlNetwork);
		}
	}
	
	/**
	 * Export VirtualMachines
	 * @param dbVmc
	 * @param xmlVmc
	 */
	private void exportVirtualMachines(VirtualMachineContainer dbVmc, XMLVirtualMachineContainerType xmlVmc) {
		LOG.debug("Exporting " + dbVmc.getVirtualmachines().size() + " VirtualMachine(s) from " + dbVmc);
		for (VirtualMachine dbVm : dbVmc.getVirtualmachines()) {
			XMLVirtualMachineType xmlVm = new XMLVirtualMachineType();
			xmlVm.setId(nextId());
			xmlVm.setComputerName(dbVm.getComputerName());
			xmlVm.setCpuCount(BigInteger.valueOf(dbVm.getCpuCount()));
			xmlVm.setCustomisationScript(dbVm.getCustomisationScript());
			xmlVm.setDescription(dbVm.getDescription());
			xmlVm.setHardwareProfile(dbVm.getHardwareProfile());
			xmlVm.setHatype(dbVm.getHaType());
			xmlVm.setMemory(BigInteger.valueOf(dbVm.getMemory()));
			xmlVm.setMemoryUnit(dbVm.getMemoryUnit());
			xmlVm.setStorageProfile(dbVm.getStorageProfile());
			xmlVm.setTemplateName(dbVm.getTemplateName());
			xmlVm.setTemplateServiceLevel(dbVm.getTemplateServiceLevel());
			xmlVm.setVmName(dbVm.getVmName());
			
			for (VirtualMachineMetaData dbMetaData : dbVm.getMetadata()) {
				XMLMetaDataType xmlMetaData = new XMLMetaDataType();
				exportMetaData(dbMetaData, xmlMetaData);
				xmlVm.getMetaData().add(xmlMetaData);
			}
			
			exportNics(dbVm, xmlVm);
			exportStorages(dbVm, xmlVm);

			xmlVmc.getVirtualMachine().add(xmlVm);
		}
	}
	
	/**
	 * Export NICs
	 * @param dbVm
	 * @param xmlVm
	 */
	private void exportNics(VirtualMachine dbVm, XMLVirtualMachineType xmlVm) {
		LOG.debug("Exporting " + dbVm.getNics().size() + " Nic(s) from " + dbVm);
		for (Nic dbNic : dbVm.getNics()) {
			XMLNICType xmlNic = new XMLNICType();
			xmlNic.setId(nextId());
			xmlNic.setIndexNumber(BigInteger.valueOf(dbNic.getIndexNumber()));
			xmlNic.setIpAssignment(dbNic.getIpAssignment());
			xmlNic.setPrimary(dbNic.getPrimaryNic());
			
			exportInterfaces(dbNic, xmlNic);
			
			String networkName = dbNic.getNetworkName();
			xmlNic.setNetworkName(networkName);
			XMLNetworkType xmlNetwork = networks.get(networkName);
			if (xmlNetwork == null) {
				throw new RuntimeException("Network: " + networkName + " not found in the Virtual Machine Container or the currently deployed Environment Container Definition for: " + dbNic);
			}
			xmlNic.setNetworkID(xmlNetwork);
			
			xmlVm.getNIC().add(xmlNic);
		}
	}
	
	/**
	 * Export Interfaces
	 * @param dbNic
	 * @param xmlNic
	 */
	private void exportInterfaces(Nic dbNic, XMLNICType xmlNic) {
		LOG.debug("Exporting " + dbNic.getInterfaces().size() + " Interface(s) from " + dbNic);
		for (Interface dbInterface : dbNic.getInterfaces()) {
			XMLInterfaceType xmlInterface = new XMLInterfaceType();
			xmlInterface.setInterfaceNumber(dbInterface.getInterfaceNumber() == null ?  null : BigInteger.valueOf(dbInterface.getInterfaceNumber()));
			xmlInterface.setIsVip(dbInterface.isVip());
			xmlInterface.setName(dbInterface.getName());
			xmlInterface.setNetworkMask(dbInterface.getNetworkMask());
			xmlInterface.setStaticIpAddress(dbInterface.getStaticIpAddress());
			xmlInterface.setStaticIpPool(dbInterface.getStaticIpPool());
			xmlInterface.setVRRP(dbInterface.getVrrp() == null ? null : BigInteger.valueOf(dbInterface.getVrrp()));
			xmlNic.getInterface().add(xmlInterface);
		}
	}
	
	/**
	 * Export Storages
	 * @param dbVm
	 * @param xmlVm
	 */
	private void exportStorages(VirtualMachine dbVm, XMLVirtualMachineType xmlVm) {
		LOG.debug("Exporting " + dbVm.getStorages().size() + " Storage(s) from " + dbVm);
		for (Storage dbStorage : dbVm.getStorages()) {
			XMLStorageType xmlStorage = new XMLStorageType();
			xmlStorage.setId(nextId());
			xmlStorage.setBusSubType(dbStorage.getBusSubType());
			xmlStorage.setBusType(dbStorage.getBusType());
			xmlStorage.setDeviceMount(dbStorage.getDeviceMount());
			xmlStorage.setIndexNumber(BigInteger.valueOf(dbStorage.getIndexNumber()));
			xmlStorage.setSize(BigInteger.valueOf(dbStorage.getSize()));
			xmlStorage.setSizeUnit(dbStorage.getSizeUnit());
			xmlVm.getStorage().add(xmlStorage);
		}
	}
	
	/**
	 * Export MetaData
	 * @param dbMetaData
	 * @param xmlMetaData
	 */
	private void exportMetaData(MetaData dbMetaData, XMLMetaDataType xmlMetaData) {
		LOG.debug("Exporting " + dbMetaData);
		xmlMetaData.setId(nextId());
		xmlMetaData.setName(dbMetaData.getName());
		xmlMetaData.setValue(dbMetaData.getValue());
	}
	
	/**
	 * Export EnvironmentContainerDefinition
	 * @param dbEnvironmentContainerDefinition
	 * @param xmlEnvironmentContainerDefinition
	 */
	private void exportEnvironmentContainerDefinition(EnvironmentContainerDefinition dbEnvironmentContainerDefinition, XMLEnvironmentContainerType xmlEnvironmentContainer) {
		LOG.debug("Exporting " + dbEnvironmentContainerDefinition);
		XMLEnvironmentContainerDefinition xmlEnvironmentContainerDefinition = new XMLEnvironmentContainerDefinition();
		xmlEnvironmentContainerDefinition.setId(nextId());
		xmlEnvironmentContainerDefinition.setName(dbEnvironmentContainerDefinition.getName());
		xmlEnvironmentContainerDefinition.setVersion(dbEnvironmentContainerDefinition.getVersion());
		
		xmlEnvironmentContainer.getEnvironmentContainerDefinition().add(xmlEnvironmentContainerDefinition);
		
		for (DataCentre dbDataCenter : dbEnvironmentContainerDefinition.getDataCentres()) {
			XMLDataCenterType xmlDataCenter = new XMLDataCenterType();
			exportDataCenter(dbDataCenter, xmlDataCenter);
			xmlEnvironmentContainerDefinition.getDataCenter().add(xmlDataCenter);
		}

		for (Gateway dbGateway : dbEnvironmentContainerDefinition.getGateways()) {
			XMLGatewayType xmlGateway = new XMLGatewayType();
			exportGateway(dbGateway, xmlGateway);
			xmlEnvironmentContainerDefinition.getGateway().add(xmlGateway);
		}
		
		exportNetworks(dbEnvironmentContainerDefinition, xmlEnvironmentContainerDefinition);
	}
	
	/**
	 * Export Gateway
	 * @param dbGateway
	 * @param xmlGateway
	 */
	private void exportGateway(Gateway dbGateway, XMLGatewayType xmlGateway) {
		LOG.debug("Exporting " + dbGateway);
		xmlGateway.setId(nextId());
		xmlGateway.setName(dbGateway.getName());
		
		exportNats(dbGateway, xmlGateway);
		
		gateways.put(dbGateway, xmlGateway);
	}
	
	/**
	 * Export Nats
	 * @param dbNat
	 * @param xmlNat
	 */
	private void exportNats(Gateway dbGateway, XMLGatewayType xmlGateway) {
		LOG.debug("Exporting " + dbGateway.getNats().size() + " Nat(s) from " + dbGateway);
		for (Nat dbNat : dbGateway.getNats()) {
			XMLNATType xmlNat = new XMLNATType();
			xmlNat.setId(nextId());
			xmlNat.setAppliedOn(dbNat.getAppliedOn());
			xmlNat.setEnabled(dbNat.getEnabled());
			xmlNat.setOriginalSourceIpOrRange(dbNat.getOriginalSourceIpOrRange());
			xmlNat.setTranslatedSourceIpOrRange(dbNat.getTranslatedSourceIpOrRange());
			if (dbNat instanceof DNat) {
				XMLDNATType xmlDNat = new XMLDNATType();
				xmlDNat.setId(nextId());
				xmlDNat.setProtocolIcmpType(((DNat) dbNat).getProtocolIcmpType());
				xmlDNat.setProtocolOriginalPort(((DNat) dbNat).getProtocolOriginalPort());
				xmlDNat.setProtocolType(((DNat) dbNat).getProtocolType());
				xmlDNat.setTranslatedPort(((DNat) dbNat).getTranslatedPort());
				xmlNat.setDNAT(xmlDNat);
			}
			xmlGateway.getNAT().add(xmlNat);
		}
	}
	
	/**
	 * Export an aCloud Environment Container (Organisation) version as XML
	 * @param environmentContainerName the name of the Environment Container
	 * @param version the required version of the Environment Container Definition or null if there is only one version
	 * @param provider the cloud provider
	 * @throws Exception
	 */
	protected void exportEnvironmentContainer(String environmentContainerName, String version, String provider) throws Exception {
		// Output XML to the file system
		writeExportFile(ExportType.environmentContainer, environmentContainerName, extractEnvironmentContainer(environmentContainerName, version, provider));
	}
	
	/**
	 * Export an aCloud Environment version as XML
	 * @param environmentName the name of the Environment
	 * @param version the required version of the Environment
	 * @param provider the cloud provider
	 * @return
	 * @throws Exception
	 */
	protected void exportEnvironment(String environmentName, String version, String provider) throws Exception {
		writeExportFile(ExportType.environment, environmentName, extractEnvironment(environmentName, version, provider));
	}
	
	/**
	 * Write the XML of the given exportType to the file system
	 * @param exportType
	 * @param name the name of the Environment / Environment Container
	 * @param xmlOrgType
	 * @throws IOException
	 */
	private void writeExportFile(ExportType exportType, String name, XMLGeographicContainerType xmlgeographicContainer) throws IOException {
		File exportFile = getExportFile(name, exportType);
		LOG.info("Writing " + exportType + " output XML to " + exportFile.getAbsolutePath());
		FileWriter writer = new FileWriter(exportFile);
		try {
			writeTo(new ObjectFactory().createGeographicContainer(xmlgeographicContainer), writer);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
	
	/**
	 * Returns a unique id
	 * @return
	 */
	private String nextId() {
		return "_" + id++;
	}
}