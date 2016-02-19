package com.ipt.ebsa.environment.metadata.export.vCloud;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.environment.build.entities.ApplicationNetwork;
import com.ipt.ebsa.environment.build.entities.DNat;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition.DefinitionType;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.Gateway;
import com.ipt.ebsa.environment.build.entities.GeographicContainer;
import com.ipt.ebsa.environment.build.entities.Interface;
import com.ipt.ebsa.environment.build.entities.MetaData;
import com.ipt.ebsa.environment.build.entities.Nat;
import com.ipt.ebsa.environment.build.entities.Nic;
import com.ipt.ebsa.environment.build.entities.OrganisationNetwork;
import com.ipt.ebsa.environment.build.entities.Storage;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;
import com.ipt.ebsa.environment.metadata.export.EnvironmentBuildMetadataExport;
import com.ipt.ebsa.skyscape.config.v2.ObjectFactory;
import com.ipt.ebsa.skyscape.config.v2.XMLApplicationNetworkType;
import com.ipt.ebsa.skyscape.config.v2.XMLAssignOrganisationNetworkType;
import com.ipt.ebsa.skyscape.config.v2.XMLConfigurationType;
import com.ipt.ebsa.skyscape.config.v2.XMLEdgeGatewayServicesType;
import com.ipt.ebsa.skyscape.config.v2.XMLEdgeGatewayServicesType.XMLNAT;
import com.ipt.ebsa.skyscape.config.v2.XMLEdgeGatewayServicesType.XMLNAT.XMLDNAT;
import com.ipt.ebsa.skyscape.config.v2.XMLEdgeGatewayServicesType.XMLNAT.XMLDNAT.XMLProtocol;
import com.ipt.ebsa.skyscape.config.v2.XMLEdgeGatewayServicesType.XMLNAT.XMLSNAT;
import com.ipt.ebsa.skyscape.config.v2.XMLEdgeGatewayType;
import com.ipt.ebsa.skyscape.config.v2.XMLEnvironmentType;
import com.ipt.ebsa.skyscape.config.v2.XMLHardDiskType;
import com.ipt.ebsa.skyscape.config.v2.XMLNetworkSettingsType;
import com.ipt.ebsa.skyscape.config.v2.XMLNetworkSettingsType.XMLIPRange;
import com.ipt.ebsa.skyscape.config.v2.XMLNetworkType;
import com.ipt.ebsa.skyscape.config.v2.XMLOrganisationNetworkType;
import com.ipt.ebsa.skyscape.config.v2.XMLOrganisationType;
import com.ipt.ebsa.skyscape.config.v2.XMLSizeType;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualApplicationType;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualMachineType;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualMachineType.XMLMetaData;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualMachineType.XMLTemplate;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualMachineType.XMLVirtualHardware;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualNetworkCardType;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualNetworkCardType.XMLIPAssignment;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualNetworkCardType.XMLIPAssignment.XMLStaticManual;

/**
 * Exports the Configuration and Environment data from the ENVIRONMENT_BUILD database for input to the vCloud client 
 *
 */
public class EnvironmentBuildMetadataVCloudExport extends EnvironmentBuildMetadataExport {
	
	/** Logger */
	private static final Logger LOG = LogManager.getLogger(EnvironmentBuildMetadataVCloudExport.class);
	
	/* Map of Network name to DB ApplicationNetwork */
	private Map<String, ApplicationNetwork> applicationNetworks = new HashMap<String, ApplicationNetwork>();

	/* Map of Network name to DB OrganisationNetwork */
	private Map<String, OrganisationNetwork> organisationNetworks = new HashMap<String, OrganisationNetwork>();

	/* Set of exported Organisation Networks */
	Set<String> exportedOrganisationNetworks = new HashSet<String>();
	
	/**
	 * Export the vCloud Configuration XML (Organisation Networks)
	 * @param environmentContainerName the name of the Environment Container
	 * @param version the required version of the Environment Container Definition or null if there is only one version
	 * @throws Exception
	 */
	protected void exportConfiguration(String environmentContainerName, String version) throws Exception {
		// Output Configuration XML to the file system
		writeExportFile(ExportType.environmentContainer, environmentContainerName, extractConfiguration(environmentContainerName, version));
	}

	/**
	 * Export a vCloud Configuration (Organisation Networks/Gateways)
	 * @param environmentContainerName the name of the Environment Container
	 * @param version the required version of the Environment Container Definition or null if there is only one version
	 * @throws Exception
	 * @return In memory XML object
	 */
	public XMLOrganisationType extractConfiguration(String environmentContainerName, String version) throws Exception {
		LOG.debug("Extracting Environment Container Definition data in vCloud format, environmentContainerName=" + environmentContainerName + ", version=" + version);
		// Fetch EnvironmentContainerDefinition for the specified version of the named environment container
		EnvironmentContainerDefinition envConDef = readManager.getEnvironmentContainerDefinition(environmentContainerName, version, XMLProviderType.SKYSCAPE.toString());
		if (envConDef == null) {
			throw new RuntimeException("No Environment Container Definition found for environment container name: " + environmentContainerName + ", environment container definition version: " + version);
		}
		List<OrganisationNetwork> dbOrgNetworks = envConDef.getNetworks();
		Set<Integer> gatewayIds = new HashSet<Integer>();
		XMLOrganisationType xmlOrgType = new XMLOrganisationType();
		XMLConfigurationType xmlConfigType = new XMLConfigurationType();
		xmlOrgType.setConfiguration(xmlConfigType);
		organisationNetworks.clear();
		for (OrganisationNetwork dbOrgNetwork : dbOrgNetworks) {
			XMLOrganisationNetworkType xmlOrgNetwork = new XMLOrganisationNetworkType();
			xmlOrgNetwork.setVirtualDataCenter(dbOrgNetwork.getDataCentreName());
			if (dbOrgNetwork.getGateway() != null) {
				xmlOrgNetwork.setEdgeGateway(dbOrgNetwork.getGateway().getName());
			}
			xmlOrgNetwork.setName(dbOrgNetwork.getName());
			xmlOrgNetwork.setDescription(dbOrgNetwork.getDescription());
			xmlOrgNetwork.setShared(dbOrgNetwork.getShared());
			XMLNetworkSettingsType xmlNetSettings = new XMLNetworkSettingsType();
			xmlNetSettings.setFenceMode(dbOrgNetwork.getFenceMode());
			xmlNetSettings.setGatewayAddress(dbOrgNetwork.getGatewayAddress());
			xmlNetSettings.setNetworkMask(dbOrgNetwork.getNetworkMask());
			xmlNetSettings.setPrimaryDNS(dbOrgNetwork.getPrimaryDns());
			xmlNetSettings.setSecondaryDNS(dbOrgNetwork.getSecondaryDns());
			xmlNetSettings.setDNSSuffix(dbOrgNetwork.getDnsSuffix());
			if (StringUtils.isNotBlank(dbOrgNetwork.getIpRangeStart())) {
				XMLIPRange xmlIpRange = new XMLIPRange();
				xmlIpRange.setStartAddress(dbOrgNetwork.getIpRangeStart());
				xmlIpRange.setEndAddress(dbOrgNetwork.getIpRangeEnd());
				xmlNetSettings.getIPRange().add(xmlIpRange);
			}
			xmlOrgNetwork.setNetworkSettings(xmlNetSettings);
			xmlConfigType.getOrganisationNetwork().add(xmlOrgNetwork);
			exportGateway(gatewayIds, xmlConfigType, dbOrgNetwork);
			organisationNetworks.put(dbOrgNetwork.getName(), dbOrgNetwork);
		}
		return xmlOrgType;
	}

	/**
	 * Export Gateways
	 * @param gatewayIds
	 * @param xmlConfigType
	 * @param dbOrgNetwork
	 */
	private void exportGateway(Set<Integer> gatewayIds, XMLConfigurationType xmlConfigType, OrganisationNetwork dbOrgNetwork) {
		if (dbOrgNetwork.getGateway() != null && !gatewayIds.contains(dbOrgNetwork.getGateway().getId())) {
			Gateway dbGateway = dbOrgNetwork.getGateway();
			XMLEdgeGatewayType xmlGateway = new XMLEdgeGatewayType();
			xmlGateway.setName(dbGateway.getName());
			xmlGateway.setVirtualDataCenter(dbOrgNetwork.getDataCentreName());
			XMLEdgeGatewayServicesType xmlGatewayServices = new XMLEdgeGatewayServicesType();
			exportNats(dbGateway, xmlGatewayServices);
			xmlGateway.setEdgeGatewayServices(xmlGatewayServices);
			xmlConfigType.getEdgeGateway().add(xmlGateway);
			gatewayIds.add(dbGateway.getId());
		}
	}

	/**
	 * Export Nats
	 * @param dbGateway
	 * @param xmlGatewayServices
	 */
	private void exportNats(Gateway dbGateway,
			XMLEdgeGatewayServicesType xmlGatewayServices) {
		for (Nat dbNat : dbGateway.getNats()) {
			XMLNAT xmlNat = new XMLNAT();
			if (dbNat instanceof DNat) {
				XMLDNAT xmlDNat = new XMLDNAT();
				xmlDNat.setAppliedOn(dbNat.getAppliedOn());
				xmlDNat.setOriginalSourceIPOrRange(dbNat.getOriginalSourceIpOrRange());
				XMLProtocol xmlProtocol = new XMLProtocol();
				xmlProtocol.setType(((DNat) dbNat).getProtocolType());
				xmlProtocol.setOriginalPort(((DNat) dbNat).getProtocolOriginalPort());
				xmlProtocol.setICMPType(((DNat) dbNat).getProtocolIcmpType());
				xmlDNat.setProtocol(xmlProtocol);
				xmlDNat.setTranslatedSourceIPOrRange(dbNat.getTranslatedSourceIpOrRange());
				xmlDNat.setTranslatedPort(((DNat) dbNat).getTranslatedPort());
				xmlDNat.setEnabled(dbNat.getEnabled());
				xmlNat.setDNAT(xmlDNat);
			} else {
				XMLSNAT xmlSNat = new XMLSNAT();
				xmlSNat.setAppliedOn(dbNat.getAppliedOn());
				xmlSNat.setOriginalSourceIPOrRange(dbNat.getOriginalSourceIpOrRange());
				xmlSNat.setTranslatedSourceIPOrRange(dbNat.getTranslatedSourceIpOrRange());
				xmlSNat.setEnabled(dbNat.getEnabled());
				xmlNat.setSNAT(xmlSNat);
			}
			xmlGatewayServices.getNAT().add(xmlNat);
		}
	}

	/**
	 * Export the vCloud Environment XML
	 * @param environmentName the name of the Environment
	 * @param version the required version of the Environment Definition
	 * @throws Exception
	 */
	protected void exportEnvironment(String environmentName, String version) throws Exception {
		// Output Environment XML to the file system
		writeExportFile(ExportType.environment, environmentName, extractEnvironment(environmentName, version));
	}
	
	/**
	 * Extract a vCloud Environment
	 * @param environmentName the name of the Environment
	 * @param version the required version of the Environment Definition
	 * @throws Exception
	 * @return In memory XML object
	 */
	public XMLOrganisationType extractEnvironment(String environmentName, String version) throws Exception {
		LOG.debug("Extracting Environment Definition data in vCloud format, environmentName=" + environmentName + ", version=" + version);
		// Fetch the Physical Environment Definition for the specified version of the named environment
		EnvironmentDefinition dbEnvDef = readManager.getEnvironmentDefinition(environmentName, version, DefinitionType.Physical, XMLProviderType.SKYSCAPE.toString());
		if (dbEnvDef == null) {
			throw new RuntimeException("No Physical Environment Definition found for environmentName: " + environmentName + ", version: " + version);
		}
		// Fetch the currently deployed Environment Container Definition		
		GeographicContainer dbGeographicContainer =  dbEnvDef.getEnvironment().getEnvironmentcontainer().getGeographicContainer();
		EnvironmentContainer dbEnvironmentContainer = dbGeographicContainer.getEnvironmentcontainer();
		EnvironmentContainerDefinition dbEnvironmentContainerDefinition = readManager.getEnvironmentContainerDefinitonCurrentlyDeployed(dbEnvironmentContainer.getName(), XMLProviderType.SKYSCAPE.toString());
		if (dbEnvironmentContainerDefinition == null) {
			throw new RuntimeException("No successfully deployed Environment Container Definition for " + dbEnvironmentContainer);
		} else {
			LOG.info("Currently deployed Environment Container Definition is: " + dbEnvironmentContainerDefinition);
		}
		// Populate the organisationNetworks Map so they are available for Nics that reference an Organisation Network
		extractConfiguration(dbEnvironmentContainer.getName(), dbEnvironmentContainerDefinition.getVersion());
		
		XMLOrganisationType xmlOrgType = new XMLOrganisationType();
		XMLEnvironmentType xmlEnvType = new XMLEnvironmentType();
		xmlEnvType.setName(dbEnvDef.getName());
		exportVirtualMachineContainers(dbEnvDef, xmlEnvType);
		xmlOrgType.setEnvironment(xmlEnvType);
		return xmlOrgType;
	}

	/**
	 * Export Virtual Machine Containers (vApps) 
	 * @param dbEnvDef
	 * @param xmlEnvType
	 */
	private void exportVirtualMachineContainers(EnvironmentDefinition dbEnvDef, XMLEnvironmentType xmlEnvType) {
		for (VirtualMachineContainer dbVmc : dbEnvDef.getVirtualmachinecontainers()) {
			XMLVirtualApplicationType xmlVApp = new XMLVirtualApplicationType();
			xmlVApp.setName(dbVmc.getName());
			xmlVApp.setDescription(dbVmc.getDescription());
			if (StringUtils.isNotEmpty(dbVmc.getRuntimeLease())) {
				xmlVApp.setRuntimeLease(dbVmc.getRuntimeLease());
			}
			if (StringUtils.isNotEmpty(dbVmc.getStorageLease())) {
				xmlVApp.setStorageLease(dbVmc.getStorageLease());
			}
			xmlVApp.setServiceLevel(dbVmc.getServiceLevel());
			xmlVApp.setPowerOn(dbVmc.getPowerOn());
			xmlVApp.setDeploy(dbVmc.getDeploy());

			exportedOrganisationNetworks.clear();
			applicationNetworks.clear();
			for (ApplicationNetwork dbNetwork : dbVmc.getNetworks()) {
				exportNetwork(xmlVApp, dbNetwork);
			}
			
			exportVirtualMachines(dbVmc, xmlVApp);
			
			xmlEnvType.getVirtualApplication().add(xmlVApp);
		}
	}

	/**
	 * Export Virtual Machines
	 * @param dbVmc
	 * @param xmlVApp
	 * @param vAppNetworks
	 */
	protected void exportVirtualMachines(VirtualMachineContainer dbVmc, XMLVirtualApplicationType xmlVApp) {
		for (VirtualMachine dbVm : dbVmc.getVirtualmachines()) {
			XMLVirtualMachineType xmlVm = new XMLVirtualMachineType();
			// EBSAD-16602: Append domain to vmName and computerName
			String domain = StringUtils.defaultIfBlank(dbVmc.getDomain(), "");
			if (StringUtils.isNotEmpty(domain) && !domain.startsWith(".")) {
				domain = "." + domain;
			}
			if(!dbVm.getVmName().contains(domain)) {
				xmlVm.setVMName(dbVm.getVmName() + domain);
			} else {
				xmlVm.setVMName(dbVm.getVmName());
			}
			xmlVm.setComputerName(dbVm.getComputerName() + domain);
			if (StringUtils.isNotEmpty(dbVm.getDescription())) {
				xmlVm.setDescription(dbVm.getDescription());
			}
			XMLTemplate xmlTemplate = new XMLTemplate();
			xmlTemplate.setName(dbVm.getTemplateName());
			xmlTemplate.setServiceLevel(dbVm.getTemplateServiceLevel());
			xmlVm.setTemplate(xmlTemplate);
			xmlVm.setStorageProfile(dbVm.getStorageProfile());
			xmlVm.setGuestCustomisationScript(dbVm.getCustomisationScript());;
			XMLVirtualHardware xmlVh = new XMLVirtualHardware();
			xmlVh.setCPU(dbVm.getCpuCount());
			XMLSizeType xmlMemSize = new XMLSizeType();
			if ("GB".equals(dbVm.getMemoryUnit())) {
				xmlMemSize.setSizeGB(dbVm.getMemory());
			} else {
				xmlMemSize.setSizeMB(dbVm.getMemory());
			}
			xmlVh.setMemorySize(xmlMemSize);
			exportStorages(dbVm, xmlVh);
			exportNics(xmlVApp, dbVm, xmlVh);
			xmlVm.setVirtualHardware(xmlVh);
			exportMetadata(dbVm, xmlVm);
			xmlVApp.getVirtualMachine().add(xmlVm);
		}
	}

	/**
	 * Export Virtual Machine Meta Data
	 * @param dbVm
	 * @param xmlVm
	 */
	private void exportMetadata(VirtualMachine dbVm, XMLVirtualMachineType xmlVm) {
		for (MetaData dbMetaData : dbVm.getMetadata()) {
			XMLMetaData xmlMetaData = new XMLMetaData();
			xmlMetaData.setName(dbMetaData.getName());
			xmlMetaData.setValue(dbMetaData.getValue());
			xmlVm.getMetaData().add(xmlMetaData);
		}
	}

	/**
	 * Export Nics (Virtual Network Cards)
	 * @param xmlVApp
	 * @param vAppNetworks
	 * @param dbVm
	 * @param xmlVh
	 */
	private void exportNics(XMLVirtualApplicationType xmlVApp, VirtualMachine dbVm, XMLVirtualHardware xmlVh) {
		for (Nic dbNic : dbVm.getNics()) {
			XMLVirtualNetworkCardType xmlNic = new XMLVirtualNetworkCardType();
			xmlNic.setNICNumber(dbNic.getIndexNumber());
			xmlNic.setIsPrimaryNIC(dbNic.getPrimaryNic());
			xmlNic.setNetworkName(dbNic.getNetworkName());
			XMLIPAssignment xmlIpAssign = new XMLIPAssignment();
			xmlNic.setIPAssignment(xmlIpAssign);
			if ("StaticIPPool".equals(dbNic.getIpAssignment())) {
				xmlIpAssign.setStaticIPPool(true);
			} else if ("DHCP".equals(dbNic.getIpAssignment())) {
				xmlIpAssign.setDHCP(true);
			} else {
				XMLStaticManual xmlStaticManual = new XMLStaticManual();
				// IP address is the staticIpAddress of the 1st non-vip interface which is not a sub-interface
				for (Interface dbInterface : dbNic.getInterfaces()) {
					if (!dbInterface.isVip() && dbInterface.getInterfaceNumber() == null) {
						xmlStaticManual.setIPAddress(dbInterface.getStaticIpAddress());
						xmlIpAssign.setStaticManual(xmlStaticManual);
						break;
					}
				}

			}
			// Export Organisation Networks (Application Networks for the VMC have already been exported)
			String networkName = dbNic.getNetworkName();
			if (applicationNetworks.get(networkName) == null) {
				OrganisationNetwork dbNetwork = organisationNetworks.get(networkName);
				if (dbNetwork == null) {
					throw new RuntimeException("Network: " + networkName + " not found in the Virtual Machine Container or the currently deployed Environment Container Definition for: " + dbNic);
				} else if (exportedOrganisationNetworks.add(networkName)) {
					// Only export each Org Network once per vApp
					XMLNetworkType xmlNetwork = new XMLNetworkType();
					XMLAssignOrganisationNetworkType xmlOrgNet = new XMLAssignOrganisationNetworkType();
					xmlOrgNet.setName(dbNetwork.getName());
					xmlOrgNet.setFenceMode(dbNetwork.getFenceMode());
					xmlNetwork.setOrganisationNetwork(xmlOrgNet);
					xmlVApp.getNetwork().add(xmlNetwork);
				}
			}
			xmlVh.getNetworkCard().add(xmlNic);
		}
	}
	
	/**
	 * Export Application Network
	 * @param xmlVApp
	 * @param vAppNetworks
	 * @param dbNetwork
	 */
	private void exportNetwork(XMLVirtualApplicationType xmlVApp, ApplicationNetwork dbNetwork) {
		if (applicationNetworks.get(dbNetwork.getName()) == null) {
			// Only export each Network once per vApp
			XMLNetworkType xmlNetwork = new XMLNetworkType();
			XMLApplicationNetworkType xmlAppNet = new XMLApplicationNetworkType();
			xmlAppNet.setName(dbNetwork.getName());
			xmlAppNet.setFenceMode(dbNetwork.getFenceMode());
			xmlAppNet.setNetworkMask(dbNetwork.getNetworkMask());
			xmlAppNet.setGatewayAddress(dbNetwork.getGatewayAddress());
			if (StringUtils.isNotEmpty(dbNetwork.getPrimaryDns())) {
				xmlAppNet.setPrimaryDNS(dbNetwork.getPrimaryDns());
			}
			if (StringUtils.isNotEmpty(dbNetwork.getSecondaryDns())) {
				xmlAppNet.setSecondaryDNS(dbNetwork.getSecondaryDns());
			}
			xmlAppNet.setDNSSuffix(dbNetwork.getDnsSuffix());
			if (StringUtils.isNotEmpty(dbNetwork.getStaticIpPool())) {
				xmlAppNet.setStaticIPPool(dbNetwork.getStaticIpPool());
			}
			if (StringUtils.isNotBlank(dbNetwork.getIpRangeStart())) {
				com.ipt.ebsa.skyscape.config.v2.XMLApplicationNetworkType.XMLIPRange xmlIpRange = new com.ipt.ebsa.skyscape.config.v2.XMLApplicationNetworkType.XMLIPRange();
				xmlIpRange.setStartAddress(dbNetwork.getIpRangeStart());
				xmlIpRange.setEndAddress(dbNetwork.getIpRangeEnd());
				xmlAppNet.getIPRange().add(xmlIpRange);
			}
			xmlNetwork.setApplicationNetwork(xmlAppNet);
			applicationNetworks.put(dbNetwork.getName(), dbNetwork);
			xmlVApp.getNetwork().add(xmlNetwork);
		}
	}

	/**
	 * Export Storages (Hard Disks)
	 * @param dbVm
	 * @param xmlVh
	 */
	private void exportStorages(VirtualMachine dbVm, XMLVirtualHardware xmlVh) {
		for (Storage dbStorage : dbVm.getStorages()) {
			XMLHardDiskType xmlDisk = new XMLHardDiskType();
			xmlDisk.setDiskNumber(dbStorage.getIndexNumber());
			XMLSizeType xmlDiskSize = new XMLSizeType();
			if ("GB".equals(dbStorage.getSizeUnit())) {
				xmlDiskSize.setSizeGB(dbStorage.getSize());
			} else {
				xmlDiskSize.setSizeMB(dbStorage.getSize());
			}
			xmlDisk.setDiskSize(xmlDiskSize);
			if (StringUtils.isNotEmpty(dbStorage.getBusType())) {
				xmlDisk.setBusType(dbStorage.getBusType());
			}
			if (StringUtils.isNotEmpty(dbStorage.getBusSubType())) {
				xmlDisk.setBusSubType(dbStorage.getBusSubType());
			}
			xmlVh.getHardDisk().add(xmlDisk);
		}
	}
	
	/**
	 * Write the XML of the given exportType to the file system
	 * @param exportType
	 * @param name the name of the Environment / Environment Container
	 * @param xmlOrgType
	 * @throws IOException
	 */
	private void writeExportFile(ExportType exportType, String name, XMLOrganisationType xmlOrgType) throws IOException {
		File exportFile = getExportFile(name, exportType);
		LOG.info("Writing " + exportType + " output XML to " + exportFile.getAbsolutePath());
		FileWriter writer = new FileWriter(exportFile);
		try {
			writeTo(new ObjectFactory().createOrganisation(xmlOrgType), writer);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
}
