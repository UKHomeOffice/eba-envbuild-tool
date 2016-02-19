package com.ipt.ebsa.environment.metadata.export.vCloud;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import com.ipt.ebsa.environment.build.entities.ApplicationNetwork;
import com.ipt.ebsa.environment.build.entities.Environment;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition.DefinitionType;
import com.ipt.ebsa.environment.build.entities.Gateway;
import com.ipt.ebsa.environment.build.entities.Interface;
import com.ipt.ebsa.environment.build.entities.Network;
import com.ipt.ebsa.environment.build.entities.Nic;
import com.ipt.ebsa.environment.build.entities.OrganisationNetwork;
import com.ipt.ebsa.environment.build.entities.Storage;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;
import com.ipt.ebsa.environment.build.entities.VirtualMachineMetaData;
import com.ipt.ebsa.environment.build.manager.UpdateManager;
import com.ipt.ebsa.sdkclient.vcloudconfig.VCloudConfigurationLoader;
import com.ipt.ebsa.skyscape.config.v2.XMLApplicationNetworkType;
import com.ipt.ebsa.skyscape.config.v2.XMLApplicationNetworkType.XMLIPRange;
import com.ipt.ebsa.skyscape.config.v2.XMLAssignOrganisationNetworkType;
import com.ipt.ebsa.skyscape.config.v2.XMLConfigurationType;
import com.ipt.ebsa.skyscape.config.v2.XMLEnvironmentType;
import com.ipt.ebsa.skyscape.config.v2.XMLHardDiskType;
import com.ipt.ebsa.skyscape.config.v2.XMLNetworkSettingsType;
import com.ipt.ebsa.skyscape.config.v2.XMLNetworkType;
import com.ipt.ebsa.skyscape.config.v2.XMLOrganisationNetworkType;
import com.ipt.ebsa.skyscape.config.v2.XMLOrganisationType;
import com.ipt.ebsa.skyscape.config.v2.XMLSizeType;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualApplicationType;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualMachineType;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualMachineType.XMLMetaData;
import com.ipt.ebsa.skyscape.config.v2.XMLVirtualNetworkCardType;

/**
 * Import vCloud XML into the unit test database to use as test data for exporting
 *
 */
public class ImportVCloudXML {
	
	/**
	 * Import Environment XML into the unit test database
	 * @param importXMLFile
	 * @throws Exception
	 */
	protected static void importEnvironment(File importXMLFile) throws Exception {
		
		XMLOrganisationType organisation = loadConfiguration(importXMLFile);
		XMLEnvironmentType xmlEnvironment = organisation.getEnvironment();
		EnvironmentDefinition dbEnvDef = new EnvironmentDefinition();
		dbEnvDef.setName(xmlEnvironment.getName());
		dbEnvDef.setDefinitionType(DefinitionType.Physical);
		dbEnvDef.setVersion("1.0");
		
		for (XMLVirtualApplicationType xmlVApp : xmlEnvironment.getVirtualApplication()) {
			Map<String, Network> networkMap = new LinkedHashMap<String, Network>();
			VirtualMachineContainer dbVmc = new VirtualMachineContainer();
			dbVmc.setName(xmlVApp.getName());
			dbVmc.setDescription(xmlVApp.getDescription());
			dbVmc.setRuntimeLease(xmlVApp.getRuntimeLease());
			dbVmc.setStorageLease(xmlVApp.getStorageLease());
			dbVmc.setServiceLevel(xmlVApp.getServiceLevel());
			dbVmc.setPowerOn(xmlVApp.isPowerOn());
			dbVmc.setDeploy(xmlVApp.isDeploy());
			
			for (XMLNetworkType xmlNetwork : xmlVApp.getNetwork()) {
				Network dbNetwork;
				if (xmlNetwork.getApplicationNetwork() != null) {
					XMLApplicationNetworkType xmlAppNetwork = xmlNetwork.getApplicationNetwork();
					dbNetwork = new ApplicationNetwork();
					dbNetwork.setName(xmlAppNetwork.getName());
					dbNetwork.setFenceMode(xmlAppNetwork.getFenceMode());
					dbNetwork.setGatewayAddress(xmlAppNetwork.getGatewayAddress());
					dbNetwork.setPrimaryDns(xmlAppNetwork.getPrimaryDNS());
					dbNetwork.setSecondaryDns(xmlAppNetwork.getSecondaryDNS());
					dbNetwork.setStaticIpPool(xmlAppNetwork.getStaticIPPool());
					dbNetwork.setNetworkMask(xmlAppNetwork.getNetworkMask());
					dbNetwork.setDnsSuffix(xmlAppNetwork.getDNSSuffix());
					for (XMLIPRange xmlIpRange : xmlAppNetwork.getIPRange()) {
						dbNetwork.setIpRangeStart(xmlIpRange.getStartAddress());
						dbNetwork.setIpRangeEnd(xmlIpRange.getEndAddress());
					}
					if (xmlAppNetwork.getIPRange().size() > 1) {
						System.err.println("More than 1 IP Range in vCloud Application Network. Last IP Range used.");
					}
					dbVmc.addNetwork((ApplicationNetwork) dbNetwork);
				} else {
					dbNetwork = new OrganisationNetwork();
					XMLAssignOrganisationNetworkType xmlOrgNetwork = xmlNetwork.getOrganisationNetwork();
					dbNetwork.setName(xmlOrgNetwork.getName());
					dbNetwork.setFenceMode(xmlOrgNetwork.getFenceMode());
				}
				networkMap.put(dbNetwork.getName(), dbNetwork);
			}
			
			for (XMLVirtualMachineType xmlVm : xmlVApp.getVirtualMachine()) {
				VirtualMachine dbVm = new VirtualMachine();
				dbVm.setVmName(xmlVm.getVMName());
				dbVm.setComputerName(xmlVm.getComputerName());
				dbVm.setDescription(xmlVm.getDescription());
				dbVm.setCustomisationScript(xmlVm.getGuestCustomisationScript());
				dbVm.setStorageProfile(xmlVm.getStorageProfile());
				dbVm.setCpuCount(xmlVm.getVirtualHardware().getCPU());
				
				for (XMLMetaData xmlMetaData : xmlVm.getMetaData()) {
					VirtualMachineMetaData dbMetaData = new VirtualMachineMetaData();
					dbMetaData.setName(xmlMetaData.getName());
					dbMetaData.setValue(xmlMetaData.getValue());
					dbVm.addMetadata(dbMetaData);
				}
				
				dbVm.setTemplateName(xmlVm.getTemplate().getName());
				dbVm.setTemplateServiceLevel(xmlVm.getTemplate().getServiceLevel());
				
				XMLSizeType xmlMemorySize = xmlVm.getVirtualHardware().getMemorySize();
				if (xmlMemorySize.getSizeGB() != null) {
					dbVm.setMemory(xmlMemorySize.getSizeGB());
					dbVm.setMemoryUnit("GB");
				} else {
					dbVm.setMemory(xmlMemorySize.getSizeMB());
					dbVm.setMemoryUnit("MB");
				}
				
				for (XMLHardDiskType xmlHardDisk : xmlVm.getVirtualHardware().getHardDisk()) {
					Storage dbStorage = new Storage();
					dbStorage.setBusType(ObjectUtils.toString(xmlHardDisk.getBusType(), null));
					dbStorage.setBusSubType(ObjectUtils.toString(xmlHardDisk.getBusSubType(), null));
					dbStorage.setIndexNumber(xmlHardDisk.getDiskNumber());
					XMLSizeType xmlDiskSize = xmlHardDisk.getDiskSize();
					if (xmlDiskSize.getSizeGB() != null) {
						dbStorage.setSize(xmlDiskSize.getSizeGB());
						dbStorage.setSizeUnit("GB");
					} else {
						dbStorage.setSize(xmlDiskSize.getSizeMB());
						dbStorage.setSizeUnit("MB");
					}
					dbVm.addStorage(dbStorage);
				}
				
				for (XMLVirtualNetworkCardType xmlNic : xmlVm.getVirtualHardware().getNetworkCard()) {
					Nic dbNic = new Nic();
					dbNic.setIndexNumber(xmlNic.getNICNumber());
					dbNic.setPrimaryNic(xmlNic.isIsPrimaryNIC());
					if (xmlNic.getIPAssignment().getStaticIPPool() != null) {
						dbNic.setIpAssignment("StaticIPPool");
					} else if (xmlNic.getIPAssignment().getDHCP() != null) {
						dbNic.setIpAssignment("DHCP");
					} else {
						dbNic.setIpAssignment("StaticManual");
						Interface iface = new Interface();
						iface.setName(xmlNic.getNetworkName());
						iface.setStaticIpAddress(xmlNic.getIPAssignment().getStaticManual().getIPAddress());
						dbNic.addInterface(iface);
					}
					dbNic.setNetworkName(xmlNic.getNetworkName());
					if (networkMap.get(xmlNic.getNetworkName()) == null) {
						System.err.println("No network: " + xmlNic.getNetworkName() + " found for nic: " + dbNic);
					}
					dbVm.addNic(dbNic);
				}
				dbVmc.addVirtualmachine(dbVm);
			}
			dbEnvDef.addVirtualmachinecontainer(dbVmc);
		}
		
		UpdateManager manager = new UpdateManager();
		
		EnvironmentContainer environmentContainer = new EnvironmentContainer();
		environmentContainer.setName("np");
		environmentContainer.setProvider("SKYSCAPE");
		
		Environment environment = new Environment();
		environment.setEnvironmentGroupName(xmlEnvironment.getName());
		environment.setName(xmlEnvironment.getName());
		environment.setValidated(false);
		environment.addEnvironmentdefinition(dbEnvDef);
		
		environmentContainer.addEnvironment(environment);
		
		manager.createEnvironmentDefinitions(environment);
	}

	/**
	 * Import Configuration XML into the unit test database
	 * @param importXMLFile
	 * @throws Exception
	 */
	protected static void importConfiguration(File importXMLFile) throws Exception {
		
		XMLOrganisationType organisation = loadConfiguration(importXMLFile);
		
		UpdateManager manager = new UpdateManager();
		
		EnvironmentContainer environmentContainer = new EnvironmentContainer();
		environmentContainer.setName("np");
		environmentContainer.setProvider("SKYSCAPE");
		
		Environment environment = new Environment();
		environment.setEnvironmentGroupName("HO_IPT_NP_PRP1");
		environment.setName("HO_IPT_NP_PRP1");
		//environment.setName("VCloud Client Test Env");
		environment.setValidated(false);
		environmentContainer.addEnvironment(environment);
		
		EnvironmentContainerDefinition envConDef = new EnvironmentContainerDefinition(); 
		envConDef.setName("np env container for config import");
		envConDef.setVersion("1.0");
		environmentContainer.addEnvironmentcontainerdefinition(envConDef);
		
		Map<String, Gateway> gateways = new HashMap<String, Gateway>();
		
		XMLConfigurationType xmlConfigration = organisation.getConfiguration();
		for (XMLOrganisationNetworkType xmlOrgNetwork : xmlConfigration.getOrganisationNetwork()) {
			OrganisationNetwork dbOrgNetwork = new OrganisationNetwork();
			envConDef.addNetwork(dbOrgNetwork);
			dbOrgNetwork.setDescription(xmlOrgNetwork.getDescription());
			String gatewayName = xmlOrgNetwork.getEdgeGateway();
			if (StringUtils.isNotBlank(gatewayName)) {
				Gateway dbGateway = gateways.get(gatewayName);
				if (dbGateway == null) {
					dbGateway = new Gateway();
					dbGateway.setName(xmlOrgNetwork.getEdgeGateway());
					dbOrgNetwork.getEnvironmentcontainerdefinition().addGateway(dbGateway);
					gateways.put(dbGateway.getName(), dbGateway);
				}
				dbOrgNetwork.setGateway(dbGateway);
			}
			dbOrgNetwork.setDataCentreName(xmlOrgNetwork.getVirtualDataCenter());
			dbOrgNetwork.setShared(xmlOrgNetwork.isShared());
			dbOrgNetwork.setName(xmlOrgNetwork.getName());
			XMLNetworkSettingsType xmlNetSettings = xmlOrgNetwork.getNetworkSettings();
			dbOrgNetwork.setDnsSuffix(xmlNetSettings.getDNSSuffix());
			dbOrgNetwork.setFenceMode(xmlNetSettings.getFenceMode());
			dbOrgNetwork.setGatewayAddress(xmlNetSettings.getGatewayAddress());
			dbOrgNetwork.setNetworkMask(xmlNetSettings.getNetworkMask());
			dbOrgNetwork.setPrimaryDns(xmlNetSettings.getPrimaryDNS());
			dbOrgNetwork.setSecondaryDns(xmlNetSettings.getSecondaryDNS());
			for (com.ipt.ebsa.skyscape.config.v2.XMLNetworkSettingsType.XMLIPRange xmlIpRange : xmlNetSettings.getIPRange()) {
				dbOrgNetwork.setIpRangeStart(xmlIpRange.getStartAddress());
				dbOrgNetwork.setIpRangeEnd(xmlIpRange.getEndAddress());
			}
			if (xmlNetSettings.getIPRange().size() > 1) {
				System.err.println("More than 1 IP Range in vCloud Organisation Network. Last IP Range used.");
			}
			
		}
		manager.createGatewaysAndOrganisationNetworksAndDataCenters(envConDef);
	}
	
	/**
	 * Load the vCloud XML file into JAXB objectes
	 * @param file
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	private static XMLOrganisationType loadConfiguration(File file) throws SAXException, IOException {
		VCloudConfigurationLoader loader = new VCloudConfigurationLoader();
		XMLOrganisationType vCloud = loader.loadVC(file);
		return vCloud;
	}
}
