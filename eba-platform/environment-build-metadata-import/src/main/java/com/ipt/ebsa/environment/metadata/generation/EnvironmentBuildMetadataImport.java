package com.ipt.ebsa.environment.metadata.generation;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLApplicationNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLDNATType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLDataCenterType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentDefinitionType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLMetaDataType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNATType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLStorageType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.database.manager.ConnectionData;
import com.ipt.ebsa.database.manager.GlobalConfig;
import com.ipt.ebsa.environment.build.entities.ApplicationNetwork;
import com.ipt.ebsa.environment.build.entities.ApplicationNetworkMetaData;
import com.ipt.ebsa.environment.build.entities.DNat;
import com.ipt.ebsa.environment.build.entities.DataCentre;
import com.ipt.ebsa.environment.build.entities.Environment;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinitionMetaData;
import com.ipt.ebsa.environment.build.entities.Gateway;
import com.ipt.ebsa.environment.build.entities.GeographicContainer;
import com.ipt.ebsa.environment.build.entities.HardwareProfile;
import com.ipt.ebsa.environment.build.entities.Interface;
import com.ipt.ebsa.environment.build.entities.Nat;
import com.ipt.ebsa.environment.build.entities.Nic;
import com.ipt.ebsa.environment.build.entities.OrganisationNetwork;
import com.ipt.ebsa.environment.build.entities.OrganisationNetworkMetaData;
import com.ipt.ebsa.environment.build.entities.Storage;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;
import com.ipt.ebsa.environment.build.entities.VirtualMachineMetaData;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.environment.build.manager.UpdateManager;

/**
 * Imports the Configuration and Environment data from the Visio XML extract into the ENVIRONMENT_BUILD database 
 *
 */
public class EnvironmentBuildMetadataImport {
	
	/** Logger */
	private static final Logger LOG = LogManager.getLogger(EnvironmentBuildMetadataImport.class);
	
	/** The 2 types of XML document that can be imported */
	protected enum ExportType {
		configuration, environment
	}
	
	/** Name of XML file to import */
	private String importXMLFilename;

	/** Map of XML Gateway -> DB Gateway */
	private Map<XMLGatewayType, Gateway> gateways = new HashMap<XMLGatewayType, Gateway>();
	/** Map of DataCenter.name -> DB DataCenter */
	private Map<String, DataCentre> dataCenters = new HashMap<String, DataCentre>();
	
	/** Update Manager instance used to insert into the ENVIRONMENT_BUILD database */
	private UpdateManager updateManager = new UpdateManager();
	
	/** Read Manager instance used to read from the ENVIRONMENT_BUILD database */
	private ReadManager readManager = new ReadManager();

	/**
	 * Constructor 
	 * @param importXMLFilename
	 */
	public EnvironmentBuildMetadataImport(String importXMLFilename) {
		this.importXMLFilename = importXMLFilename;
		// Set database connection details from properties
		GlobalConfig.getInstance().setSharedConnectionData(new ConnectionData() {
			@Override
			public String getUsername() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_USERNAME);
			}
			@Override
			public String getUrl() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_URL);
			}
			@Override
			public String getSchema() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_SCHEMA);
			}
			@Override
			public String getPassword() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_PASSWORD);
			}
			@Override
			public String getDriverClass() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_DRIVER);
			}
			@Override
			public String getDialect() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_HIBERNATE_DIALECT);
			}
			@Override
			public String getAutodll() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_HBM2DDL_AUTO);
			}
		});
	}

	/**
	 * Import data from the importXMLFilename into the ENVIRONMENT_BUILD database
	 * @throws Exception
	 */
	public void importEnvironmentMetadata() throws Exception {
		XMLGeographicContainerType xmlGeographicContainer = loadXML();
		GeographicContainer dbGeographicContainer = new GeographicContainer();
		dbGeographicContainer.setAccount(xmlGeographicContainer.getAccount());
		dbGeographicContainer.setRegion(xmlGeographicContainer.getRegion());
		XMLEnvironmentContainerType xmlEnvContainer = xmlGeographicContainer.getEnvironmentContainer();
		String provider = xmlEnvContainer.getProvider().value();
		EnvironmentContainer dbEnvContainer = new EnvironmentContainer();
		dbEnvContainer.setName(xmlEnvContainer.getName());
		dbEnvContainer.setProvider(provider);
		dbEnvContainer.addGeographiccontainer(dbGeographicContainer);
		
		// Import EnvironmentContainerDefinitions
		importEnvironmentContainerDefinitions(xmlEnvContainer, dbEnvContainer);
		// Import EnvironmentDefinitions
		importEnvironmentDefinitions(xmlEnvContainer, dbEnvContainer);

		// Add EnvironmentContainerDefinitions to the database
		LOG.debug("Adding " + dbEnvContainer.getEnvironmentcontainerdefinitions().size() + " EnvironmentContainerDefinition(s) to the database");
		updateManager.createEnvironmentContainerDefinitions(dbEnvContainer);
		
		// Add EnvironmentDefinitions to the database
		LOG.debug("Adding " + dbEnvContainer.getEnvironments().size() + " Environment(s) to the database");
		for (Environment environment : dbEnvContainer.getEnvironments()) {
			updateManager.createEnvironmentDefinitions(environment);
		}
		LOG.info("Import completed successfully");
	}

	/**
	 * Import Environment Container Definitions
	 * @param xmlEnvContainer
	 * @param dbEnvContainer
	 * @throws Exception 
	 */
	private void importEnvironmentContainerDefinitions(XMLEnvironmentContainerType xmlEnvContainer, EnvironmentContainer dbEnvContainer) throws Exception {
		LOG.debug("Importing " + xmlEnvContainer.getEnvironmentContainerDefinition().size() + " EnvironmentContainerDefinition(s) for " + dbEnvContainer);
		for (XMLEnvironmentContainerType.XMLEnvironmentContainerDefinition xmlEnvConDef : xmlEnvContainer.getEnvironmentContainerDefinition()) {
			EnvironmentContainerDefinition dbEnvConDef = new EnvironmentContainerDefinition();
			dbEnvConDef.setName(xmlEnvConDef.getName());
			dbEnvConDef.setVersion(xmlEnvConDef.getVersion());
			// Data Centers
			importDataCenters(xmlEnvConDef, dbEnvConDef);
			// Gateways
			importGateways(xmlEnvConDef, dbEnvConDef);
			// Organisation Networks
			importOrganisationNetworks(xmlEnvConDef, dbEnvConDef);

			dbEnvContainer.addEnvironmentcontainerdefinition(dbEnvConDef);
		}
	}

	/**
	 * Import Organisation Networks
	 * @param xmlEnvConDef
	 * @param dbEnvConDef
	 */
	private void importOrganisationNetworks(XMLEnvironmentContainerType.XMLEnvironmentContainerDefinition xmlEnvConDef, EnvironmentContainerDefinition dbEnvConDef) {
		List<XMLOrganisationalNetworkType> orgNetworks = xmlEnvConDef.getNetwork();
		LOG.debug("Importing " + orgNetworks.size() + " OrganisationNetwork(s) for " + dbEnvConDef);
		for (XMLOrganisationalNetworkType xmlNetwork : orgNetworks) {
			OrganisationNetwork dbNetwork = new OrganisationNetwork();
			dbNetwork.setName(xmlNetwork.getName());
			dbNetwork.setDescription(xmlNetwork.getDescription());
			dbNetwork.setFenceMode(xmlNetwork.getFenceMode());
			dbNetwork.setNetworkMask(xmlNetwork.getNetworkMask());
			dbNetwork.setGatewayAddress(xmlNetwork.getGatewayAddress());
			dbNetwork.setPrimaryDns(xmlNetwork.getPrimaryDns());
			dbNetwork.setSecondaryDns(xmlNetwork.getSecondaryDns());
			dbNetwork.setDnsSuffix(xmlNetwork.getDnsSuffix());
			dbNetwork.setStaticIpPool(xmlNetwork.getStaticIpPool());
			dbNetwork.setIpRangeStart(xmlNetwork.getIpRangeStart());
			dbNetwork.setIpRangeEnd(xmlNetwork.getIpRangeEnd());
			dbNetwork.setCidr(xmlNetwork.getCIDR());
			dbNetwork.setShared(xmlNetwork.isShared());
			dbNetwork.setPeerEnvironmentName(xmlNetwork.getPeerEnvironmentName());
			dbNetwork.setPeerNetworkName(xmlNetwork.getPeerNetworkName());
			for (XMLMetaDataType xmlNetMetaData : xmlNetwork.getMetaData()) {
				OrganisationNetworkMetaData dbNetMetaData = new OrganisationNetworkMetaData();
				dbNetMetaData.setName(xmlNetMetaData.getName());
				dbNetMetaData.setValue(xmlNetMetaData.getValue());
				dbNetwork.addMetadata(dbNetMetaData);
			}
			dbNetwork.setGateway(gateways.get(xmlNetwork.getGatewayId()));
			dbNetwork.setDataCentreName(xmlNetwork.getDataCenterName());
			dbNetwork.setDataCentre(dataCenters.get(xmlNetwork.getDataCenterName()));
			dbEnvConDef.addNetwork(dbNetwork);
		}
	}

	/**
	 * Import Gateways
	 * @param xmlEnvConDef
	 * @param dbEnvConDef
	 */
	private void importGateways(XMLEnvironmentContainerType.XMLEnvironmentContainerDefinition xmlEnvConDef, EnvironmentContainerDefinition dbEnvConDef) {
		LOG.debug("Importing " + xmlEnvConDef.getGateway().size() + " Gateway(s) for " + dbEnvConDef);
		for (XMLGatewayType xmlGateway : xmlEnvConDef.getGateway()) {
			Gateway dbGateway = new Gateway();
			dbGateway.setName(xmlGateway.getName());
			for (XMLNATType xmlNat : xmlGateway.getNAT()) {
				Nat dbNat;
				XMLDNATType xmlDnat = xmlNat.getDNAT();
				if (xmlDnat == null) {
					dbNat = new Nat();
				} else {
					dbNat = new DNat();
					((DNat) dbNat).setTranslatedPort(xmlDnat.getTranslatedPort());
					((DNat) dbNat).setProtocolType(xmlDnat.getProtocolType());
					((DNat) dbNat).setProtocolOriginalPort(xmlDnat.getProtocolOriginalPort());
					((DNat) dbNat).setProtocolIcmpType(xmlDnat.getProtocolIcmpType());
				}
				dbNat.setAppliedOn(xmlNat.getAppliedOn());
				dbNat.setOriginalSourceIpOrRange(xmlNat.getOriginalSourceIpOrRange());
				dbNat.setTranslatedSourceIpOrRange(xmlNat.getTranslatedSourceIpOrRange());
				dbNat.setEnabled(xmlNat.isEnabled());
				dbGateway.addNat(dbNat);
			}
			dbEnvConDef.addGateway(dbGateway);
			gateways.put(xmlGateway, dbGateway);
		}
	}
	
	/**
	 * Import Data Centers
	 * @param xmlEnvConDef
	 * @param dbEnvConDef
	 */
	private void importDataCenters(XMLEnvironmentContainerType.XMLEnvironmentContainerDefinition xmlEnvConDef, EnvironmentContainerDefinition dbEnvConDef) {
		LOG.debug("Importing " + xmlEnvConDef.getDataCenter().size() + " Data Center(s) for " + dbEnvConDef);
		for (XMLDataCenterType xmlDataCenter : xmlEnvConDef.getDataCenter()) {
			DataCentre dbDataCenter = new DataCentre();
			dbDataCenter.setName(xmlDataCenter.getName());
			dbEnvConDef.addDataCentre(dbDataCenter);
			dataCenters.put(xmlDataCenter.getName(), dbDataCenter);
		}
	}

	/**
	 * Import Environment Definitions
	 * @param networks
	 * @param nics
	 * @param xmlEnvContainer
	 * @param dbEnvContainer
	 * @throws Exception 
	 */
	private void importEnvironmentDefinitions(XMLEnvironmentContainerType xmlEnvContainer, EnvironmentContainer dbEnvContainer) throws Exception {
		List<XMLEnvironmentType> environments = xmlEnvContainer.getEnvironment();
		LOG.debug("Importing " + environments.size() + " EnvironmentDefinition(s) for " + dbEnvContainer);
		
		for (XMLEnvironmentType xmlEnv : environments) {
			// Environment
			Environment dbEnv = new Environment();
			dbEnv.setEnvironmentGroupName(xmlEnv.getName());
			//As per Adrians design, we now need to assume only 1 environment, 1 vmc and take info out of the VMC and spray into the database.
			//This makes creates a 1:1 dependency of environment to VMC to support VISIO import. EBSAD-23152
			String aEnvironmentName = "";

			if(xmlEnv.getEnvironmentDefinition() != null && xmlEnv.getEnvironmentDefinition().size() >= 1 ) {
				XMLEnvironmentDefinitionType enDf =  xmlEnv.getEnvironmentDefinition().get(0);
				if(enDf.getVirtualMachineContainer() != null && enDf.getVirtualMachineContainer().size() >=1) {
					XMLVirtualMachineContainerType fvmc = enDf.getVirtualMachineContainer().get(0);
					aEnvironmentName = fvmc.getName();
				}
				else {
					aEnvironmentName = xmlEnv.getName();
				}
			} else {
				aEnvironmentName = xmlEnv.getName();
			}

			dbEnv.setName(aEnvironmentName);
			dbEnv.setNotes(xmlEnv.getNotes());
			dbEnv.setValidated(false);
			dbEnvContainer.addEnvironment(dbEnv);
			for (XMLEnvironmentDefinitionType xmlEnvDef : xmlEnv.getEnvironmentDefinition()) {
				// EnvironmentDefinition
				EnvironmentDefinition dbEnvDef = new EnvironmentDefinition();
				dbEnv.addEnvironmentdefinition(dbEnvDef);
				dbEnvDef.setName(aEnvironmentName);
				dbEnvDef.setVersion(xmlEnvDef.getVersion());
				dbEnvDef.setCidr(xmlEnvDef.getCidr());
				dbEnvDef.setDefinitionType(xmlEnvDef.getEnvironmentDefinitionType().value());
				for (XMLMetaDataType xmlEnvMetaData : xmlEnvDef.getMetaData()) {
					EnvironmentDefinitionMetaData dbEnvDefMetaData = new EnvironmentDefinitionMetaData();
					dbEnvDefMetaData.setName(xmlEnvMetaData.getName());
					dbEnvDefMetaData.setValue(xmlEnvMetaData.getValue());
					dbEnvDef.addMetadata(dbEnvDefMetaData);
				}
				// VirtualMachineContainers
				importVirtualMachineContainers(xmlEnvDef, dbEnvDef);
			}
		}
	}

	/**
	 * Import Virtual Machine Containers
	 * @param xmlEnvDef
	 * @param dbEnvDef
	 * @throws Exception 
	 */
	private void importVirtualMachineContainers(XMLEnvironmentDefinitionType xmlEnvDef, EnvironmentDefinition dbEnvDef) throws Exception {
		LOG.debug("Importing " + xmlEnvDef.getVirtualMachineContainer().size() + " VirtualMachineContainer(s) for " + dbEnvDef);
		for (XMLVirtualMachineContainerType xmlVmc : xmlEnvDef.getVirtualMachineContainer()) {
			VirtualMachineContainer dbVmc = new VirtualMachineContainer();
			dbEnvDef.addVirtualmachinecontainer(dbVmc);
			dbVmc.setName(xmlVmc.getName());
			dbVmc.setDescription(xmlVmc.getDescription());
			dbVmc.setRuntimeLease(xmlVmc.getRuntimeLease());
			dbVmc.setStorageLease(xmlVmc.getStorageLease());
			dbVmc.setServiceLevel(xmlVmc.getServiceLevel());
			dbVmc.setPowerOn(xmlVmc.isPowerOn());
			dbVmc.setDeploy(xmlVmc.isDeploy());
			dbVmc.setDomain(xmlVmc.getDomain());
			dbVmc.setDataCentreName(xmlVmc.getDataCenterName());

			// VirtualMachines
			importVirtualMachines(xmlVmc, dbVmc);
			// ApplicationNetworks
			importApplicationNetworks(xmlVmc, dbVmc);
		}
	}

	/**
	 * Import Virtual Machines
	 * @param xmlVmc
	 * @param dbVmc
	 * @throws Exception 
	 */
	private void importVirtualMachines(XMLVirtualMachineContainerType xmlVmc, VirtualMachineContainer dbVmc) throws Exception {
		LOG.debug("Importing " + xmlVmc.getVirtualMachine().size() + " VirtualMachine(s) for " + dbVmc);
		for (XMLVirtualMachineType xmlVm : xmlVmc.getVirtualMachine()) {
			VirtualMachine dbVm = new VirtualMachine();
			dbVmc.addVirtualmachine(dbVm);
			dbVm.setVmName(xmlVm.getVmName());
			dbVm.setComputerName(xmlVm.getComputerName());
			dbVm.setDescription(xmlVm.getDescription());
			dbVm.setTemplateName(xmlVm.getTemplateName());
			dbVm.setTemplateServiceLevel(xmlVm.getTemplateServiceLevel());
			dbVm.setStorageProfile(xmlVm.getStorageProfile());
			dbVm.setCustomisationScript(xmlVm.getCustomisationScript());
			dbVm.setCpuCount(xmlVm.getCpuCount().intValue());
			dbVm.setMemory(xmlVm.getMemory().intValue());
			dbVm.setMemoryUnit(xmlVm.getMemoryUnit());
			dbVm.setHardwareProfile(xmlVm.getHardwareProfile());
			dbVm.setHaType(xmlVm.getHatype());
			for (XMLMetaDataType xmlVmMetaData : xmlVm.getMetaData()) {
				VirtualMachineMetaData dbVmMetaData = new VirtualMachineMetaData();
				dbVmMetaData.setName(xmlVmMetaData.getName());
				dbVmMetaData.setValue(xmlVmMetaData.getValue());
				dbVm.addMetadata(dbVmMetaData);
			}
			// Storages
			importStorages(xmlVm, dbVm);
			// Nics
			importNics(xmlVm, dbVm);
			
			// Determining the Hardware profile requires the number of Nics so must be done after importing the Nics
			setHardwareProfile(dbVm);
		}
	}

	/**
	 * Import Storages
	 * @param xmlVm
	 * @param dbVm
	 */
	private void importStorages(XMLVirtualMachineType xmlVm, VirtualMachine dbVm) {
		LOG.debug("Importing " + xmlVm.getStorage().size() + " Storage(s) for " + dbVm);
		for (XMLStorageType xmlStorage : xmlVm.getStorage()) {
			Storage dbStorage = new Storage();
			dbStorage.setIndexNumber(xmlStorage.getIndexNumber().intValue());
			dbStorage.setSize(xmlStorage.getSize().intValue());
			dbStorage.setSizeUnit(xmlStorage.getSizeUnit());
			dbStorage.setBusType(xmlStorage.getBusType());
			dbStorage.setBusSubType(xmlStorage.getBusSubType());
			dbStorage.setDeviceMount(xmlStorage.getDeviceMount());
			dbVm.addStorage(dbStorage);
		}
	}

	/**
	 * Import Nics
	 * @param xmlVm
	 * @param dbVm
	 */
	private void importNics(XMLVirtualMachineType xmlVm, VirtualMachine dbVm) {
		LOG.debug("Importing " + xmlVm.getNIC().size() + " Nic(s) for " + dbVm);
		for (XMLNICType xmlNic : xmlVm.getNIC()) {
			Nic dbNic = new Nic();
			dbNic.setIndexNumber(xmlNic.getIndexNumber().intValue());
			dbNic.setNetworkName(xmlNic.getNetworkName());
			dbNic.setPrimaryNic(xmlNic.isPrimary());
			dbNic.setIpAssignment(xmlNic.getIpAssignment());
			
			importInterfaces(xmlNic, dbNic);

			dbVm.addNic(dbNic);
		}
	}

	/**
	 * Import Interfaces
	 * @param xmlNic
	 * @param dbNic
	 */
	private void importInterfaces(XMLNICType xmlNic, Nic dbNic) {
		LOG.debug("Importing " + xmlNic.getInterface().size() + " Interface(s) for " + dbNic);
		for (XMLInterfaceType xmlInterface : xmlNic.getInterface()) {
			Interface dbInterface = new Interface();
			dbInterface.setInterfaceNumber(xmlInterface.getInterfaceNumber() == null ? null : xmlInterface.getInterfaceNumber().intValue());
			dbInterface.setName(xmlInterface.getName());
			dbInterface.setNetworkMask(xmlInterface.getNetworkMask());
			dbInterface.setStaticIpAddress(xmlInterface.getStaticIpAddress());
			dbInterface.setStaticIpPool(xmlInterface.getStaticIpPool());
			dbInterface.setVip(xmlInterface.isIsVip());
			dbInterface.setVrrp(xmlInterface.getVRRP() == null ? null : xmlInterface.getVRRP().intValue());
			dbNic.addInterface(dbInterface);
		}
	}
	
	/**
	 * Import Application Networks
	 * @param xmlVmc
	 * @param dbVmc
	 */
	private void importApplicationNetworks(XMLVirtualMachineContainerType xmlVmc, VirtualMachineContainer dbVmc) {
		LOG.debug("Importing " + xmlVmc.getNetwork().size() + " ApplicationNetwork(s) for " + dbVmc);
		for (XMLApplicationNetworkType xmlNetwork : xmlVmc.getNetwork()) {
			ApplicationNetwork dbNetwork = new ApplicationNetwork();
			dbNetwork.setName(xmlNetwork.getName());
			dbNetwork.setDescription(xmlNetwork.getDescription());
			dbNetwork.setFenceMode(xmlNetwork.getFenceMode());
			dbNetwork.setNetworkMask(xmlNetwork.getNetworkMask());
			dbNetwork.setGatewayAddress(xmlNetwork.getGatewayAddress());
			dbNetwork.setPrimaryDns(xmlNetwork.getPrimaryDns());
			dbNetwork.setSecondaryDns(xmlNetwork.getSecondaryDns());
			dbNetwork.setDnsSuffix(xmlNetwork.getDnsSuffix());
			dbNetwork.setStaticIpPool(xmlNetwork.getStaticIpPool());
			dbNetwork.setIpRangeStart(xmlNetwork.getIpRangeStart());
			dbNetwork.setIpRangeEnd(xmlNetwork.getIpRangeEnd());
			dbNetwork.setCidr(xmlNetwork.getCIDR());
			dbNetwork.setShared(xmlNetwork.isShared());
			for (XMLMetaDataType xmlNetMetaData : xmlNetwork.getMetaData()) {
				ApplicationNetworkMetaData dbNetMetaData = new ApplicationNetworkMetaData();
				dbNetMetaData.setName(xmlNetMetaData.getName());
				dbNetMetaData.setValue(xmlNetMetaData.getValue());
				dbNetwork.addMetadata(dbNetMetaData);
			}
			dbNetwork.setDataCentreName(xmlNetwork.getDataCenterName());
			dbVmc.addNetwork(dbNetwork);
		}
	}

	/**
	 * Loads the XML from importXMLFilename
	 * @return
	 * @throws Exception
	 */
	private XMLGeographicContainerType loadXML() throws Exception {
		File xmlFile = new File(importXMLFilename);
		try {
			LOG.info("Loading XML from: " + xmlFile.getAbsolutePath());
			if (!xmlFile.exists()) {
				throw new FileNotFoundException("Unable to load file '" + xmlFile.getAbsolutePath() + "' because it does not exist or cannot be read.");
			}
			
			URL schemaURL = getClass().getResource("/AgnosticCloudConfig-1.0.xsd");
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = sf.newSchema(schemaURL);
			
			JAXBContext context = JAXBContext.newInstance(XMLGeographicContainerType.class);
			StreamSource streamSource = new StreamSource(xmlFile);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(schema);
			unmarshaller.setEventHandler(new ValidationEventHandler(){
				public boolean handleEvent(ValidationEvent event) {
					return false;
				}
			});
			XMLGeographicContainerType geographicContainer = (XMLGeographicContainerType) unmarshaller.unmarshal(streamSource, XMLGeographicContainerType.class).getValue();
			return geographicContainer;
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to load XML file: " + xmlFile.getAbsolutePath(), e);
		}
	}
	
	/**
	 * Determines and sets the VM hardware profile. Not relevant for Skyscape.
	 * @param dbVm
	 * @throws Exception
	 */
	private void setHardwareProfile(VirtualMachine dbVm) throws Exception {
		String hardwareProfile = null;
		String provider = dbVm.getVirtualmachinecontainer().getEnvironmentdefinition().getEnvironment().getEnvironmentcontainer().getProvider();
		if (!XMLProviderType.SKYSCAPE.toString().equals(provider)) {
			HardwareProfile profile = new HardwareProfile();
			profile.setCpuCount(dbVm.getCpuCount());
			profile.setMemory(dbVm.getMemory());
			profile.setInterfaceCount(dbVm.getNics().size());
			profile.setVmRole(StringUtils.substring(dbVm.getVmName(), 0, 3).toUpperCase());
			profile.setProvider(provider);
			hardwareProfile = readManager.getHardwareProfile(profile);
			if (hardwareProfile == null) {
				throw new RuntimeException("No Hardware Profile found for VM: " + dbVm + " matching: " + profile);
			} else {
				LOG.debug("Determined Hardware Profile: " + hardwareProfile + " for VM: " + dbVm);
				dbVm.setHardwareProfile(hardwareProfile);
			}
		}
	}
}
 