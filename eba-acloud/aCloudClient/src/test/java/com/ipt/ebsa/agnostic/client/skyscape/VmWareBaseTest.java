package com.ipt.ebsa.agnostic.client.skyscape;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.ipt.ebsa.agnostic.client.BaseTest;
import com.ipt.ebsa.agnostic.client.cli.AgnosticCliController;
import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.skyscape.connection.SkyscapeCloudValues;
import com.ipt.ebsa.agnostic.client.skyscape.manager.NetworkManager;
import com.ipt.ebsa.agnostic.client.skyscape.manager.SkyscapeCloudManager;
import com.ipt.ebsa.agnostic.client.skyscape.manager.VAppManager;
import com.ipt.ebsa.agnostic.client.skyscape.manager.VMManager;
import com.ipt.ebsa.agnostic.client.skyscape.module.VAppModule;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLApplicationNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLDataCenterType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType.XMLEnvironmentContainerDefinition;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentDefinitionType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentDefinitionTypeType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLStorageType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.vmware.vcloud.sdk.Vapp;

public class VmWareBaseTest extends BaseTest {

	private static Logger logger = LogManager.getLogger(VmWareBaseTest.class);

	public static String configFile = "src/test/resources/skyscape/vmware-test-config.properties";
	public static String jenkinsOverridesFile = "src/test/resources/skyscape/vmware-test-jenkins-overrides.properties";

	protected static XMLEnvironmentContainerType env;
	protected static XMLEnvironmentContainerType env2;
	protected static XMLGeographicContainerType geographic;
	protected static XMLGeographicContainerType geographic2;
	protected static XMLEnvironmentType environment;
	protected static XMLEnvironmentType environmentAdditionalVpc;
	protected static XMLEnvironmentDefinitionType environmentDefinition;
	protected static XMLEnvironmentDefinitionType environmentDefinition2;
	protected static XMLEnvironmentContainerDefinition environmentContainerDefinition;
	protected static XMLEnvironmentContainerDefinition environmentContainerDefinition2;
	protected static XMLApplicationNetworkType appNet1;
	protected static XMLApplicationNetworkType appNet2;
	protected static XMLOrganisationalNetworkType orgNetwork1;
	protected static XMLOrganisationalNetworkType orgNetwork2;
	protected static XMLOrganisationalNetworkType orgNetwork3;
	protected static XMLVirtualMachineContainerType vmc;
	protected static XMLVirtualMachineContainerType vmcAdditional;
	protected static XMLVirtualMachineType vm1;
	protected static XMLVirtualMachineType vm2;
	protected static XMLVirtualMachineType vm3;
	protected static XMLVirtualMachineType vm4;
	protected static XMLVirtualMachineType vm5;
	protected static XMLGatewayType gateway;
	protected static XMLDataCenterType datacenter;
	protected static XMLStorageType hdd1a;
	protected static XMLStorageType hdd2a;
	protected static XMLStorageType hdd3a;
	protected static XMLStorageType hdd1b;
	protected static XMLStorageType hdd2b;
	protected static XMLStorageType hdd3b;
	protected static XMLStorageType hdd1c;
	protected static XMLStorageType hdd2c;
	protected static XMLStorageType hdd3c;
	protected static XMLStorageType hdd1d;
	protected static XMLStorageType hdd2d;
	protected static XMLStorageType hdd3d;
	protected static XMLStorageType hdd1e;
	protected static XMLNICType nic1a;
	protected static XMLNICType nic2a;
	protected static XMLNICType nic3a;
	protected static XMLNICType nic1b;
	protected static XMLNICType nic2b;
	protected static XMLNICType nic3b;
	protected static XMLNICType nic1c;
	protected static XMLNICType nic2c;
	protected static XMLNICType nic3c;
	protected static XMLNICType nic1d;
	protected static XMLNICType nic2d;
	protected static XMLNICType nic3d;
	protected static XMLNICType nic1e;
	protected static String testPrefixIdent;
	protected static String testPrefixIdentAdditionalVpc;
	protected static String testVpcId;
	protected static String testAdditionalVpcId;

	public static final String CENTOS_IMAGE = "IPT-TEMPLATES-CENTOS-6.5-64";
	public static final String VYOS_IMAGE = "ami-f5306a82";
	public static final String RHEL_IMAGE = "IPT-TEMPLATES-RHEL-6.5-64";
	public static final String STORAGE_PROFILE_BASIC = "9-76-3-BASIC-Any";
	public static final String TEST_CUSTOMISATION_SCRIPT = "customisationScript.sh";
	public static final String TEST_TEMPLATE_SERVICE_LEVEL = "Home Office IPT-DEPLOYED (IL2-DEVTEST-BASIC)";
	public static final String VDC = "Home Office IPT (IL2-DEVTEST-BASIC)";
	public static final String VDC_GATEWAY = "Home Office IPT-DEPLOYED (IL2-DEVTEST-BASIC)";
	public static final String TEST_DOMAIN_NAME = "test.domain.local";

	protected static SkyscapeCloudManager cloudManager;
	static VAppManager vappManager;
	static NetworkManager networkManager;
	static VMManager vmManager;
	static VAppModule vappModule;
	static SkyscapeCloudValues cv;
	protected static AgnosticCliController controller;

	@BeforeClass
	public static void setUpBeforeVmWareBaseTestClass() throws InterruptedException {
		ConfigurationFactory.resetProperties();
		if (System.getProperty("user.name").contains("jenkins")) {
			Properties overrides = new Properties();
			try {
				overrides.load(new FileInputStream(jenkinsOverridesFile));
			} catch (Exception e) {
				logger.error("Unable to load overrides from file " + jenkinsOverridesFile, e);
				fail("Unable to load Jenkins overrides");
			}
			for (Entry<Object, Object> override : overrides.entrySet()) {
				ConfigurationFactory.getProperties().setProperty((String) override.getKey(), (String) override.getValue());
			}
		} else {
			ConfigurationFactory.setConfigFile(new File(configFile));
		}

		container = weld.initialize();
		cloudManager = container.instance().select(SkyscapeCloudManager.class).get();
		vappManager = container.instance().select(VAppManager.class).get();
		networkManager = container.instance().select(NetworkManager.class).get();
		vmManager = container.instance().select(VMManager.class).get();
		vappModule = container.instance().select(VAppModule.class).get();
		cv = container.instance().select(SkyscapeCloudValues.class).get();
		controller = container.instance().select(AgnosticCliController.class).get();

		testPrefixIdent = getTestPrefixIdent();
		testPrefixIdentAdditionalVpc = getTestPrefixIdent();
		getBaseTestGeographicContainer(true, testPrefixIdent);
	}

	@AfterClass
	public static void tearDownAfterVmWareBaseTestClass() throws ToManyResultsException, InterruptedException, UnSafeOperationException {
		// envModule.deleteVpcById(testVpcId, "UNITTEST");
		// if(environmentAdditionalVpc != null &&
		// StringUtils.isNoneBlank(testAdditionalVpcId)) {
		// envModule.deleteVpcById(testAdditionalVpcId, "UNITTEST");
		// }
	}

	/**
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDownVmWareBaseTest() throws Exception {

	}

	/**
	 * 
	 * @param testIsolationPrefix
	 */
	public static void resetBaseTestConfig(String testIsolationPrefix, String testIsolationPrefix2) {
		getBaseTestGeographicContainer(true, testIsolationPrefix);
		getBaseTestGeographicContainer2(true, testIsolationPrefix2);
	}

	@Before
	public void setUpVmWareBaseTest() throws Exception {
		if (System.getProperty("user.name").equals("jenkins")) {
			ConfigurationFactory.setConfigFile(new File(jenkinsOverridesFile));
		} else {
			ConfigurationFactory.setConfigFile(new File(configFile));
		}
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLGatewayType getBaseTestGateway(boolean reset, String testIsolationPrefix) {
		if (gateway == null || reset) {
			gateway = new XMLGatewayType();
			gateway.setName("nft000c2i2-1");
			gateway.setId("A_47");
		}
		return gateway;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLGeographicContainerType getBaseTestGeographicContainer(boolean reset, String testIsolationPrefix) {
		if (geographic == null || reset) {
			geographic = new XMLGeographicContainerType();
			geographic.setEnvironmentContainer(getBaseTestEnvironmentContainer(reset, testIsolationPrefix));
			geographic.setRegion("eu-west-1");
			geographic.setAccount("ebsa");
			geographic.setId("A_1");
		}
		return geographic;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLGeographicContainerType getBaseTestGeographicContainer2(boolean reset, String testIsolationPrefix) {
		if (geographic2 == null || reset) {
			geographic2 = new XMLGeographicContainerType();
			geographic2.setEnvironmentContainer(getBaseTestEnvironmentContainer2(reset, testIsolationPrefix));
			geographic2.setRegion("eu-west-1");
			geographic2.setAccount("ebsa");
			geographic2.setId("A_2");
		}
		return geographic2;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLEnvironmentContainerType getBaseTestEnvironmentContainer(boolean reset, String testIsolationPrefix) {
		if (env == null || reset) {
			env = new XMLEnvironmentContainerType();
			env.getEnvironment().add(getBaseTestEnvironment(reset, testIsolationPrefix));
			env.getEnvironmentContainerDefinition().add(getBaseTestEnvironmentContainerDefinition(reset, testIsolationPrefix));
			env.setName(testIsolationPrefix + "SkyscapeTestContainer");
			env.setProvider(XMLProviderType.SKYSCAPE);
			env.setId("A_3");
		}
		return env;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLEnvironmentContainerType getBaseTestEnvironmentContainer2(boolean reset, String testIsolationPrefix) {
		if (env2 == null || reset) {
			env2 = new XMLEnvironmentContainerType();
			env2.getEnvironment().add(getBaseTestEnvironment2(reset, testIsolationPrefix));
			env2.getEnvironmentContainerDefinition().add(getBaseTestEnvironmentContainerDefinition2(reset, testIsolationPrefix));
			env2.setName(testIsolationPrefix + "SkyscapeTestContainer");
			env2.setProvider(XMLProviderType.SKYSCAPE);
			env2.setId("A_4");
		}
		return env2;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLEnvironmentType getBaseTestEnvironment(boolean reset, String testIsolationPrefix) {
		if (environment == null || reset) {
			environment = new XMLEnvironmentType();
			environment.getEnvironmentDefinition().add(getBaseTestEnvironmentDefinition(reset, testIsolationPrefix));
			environment.setName(testIsolationPrefix + "SkyscapeTest_IPT_HO_ipt_ho");
			environment.setEnvironmentContainerDefinitionId(environmentContainerDefinition);
			environment.setId("A_5");
			environment.setNotes("notes");
		}
		return environment;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLEnvironmentType getBaseTestEnvironment2(boolean reset, String testIsolationPrefix) {
		if (environmentAdditionalVpc == null || reset) {
			environmentAdditionalVpc = new XMLEnvironmentType();
			environmentAdditionalVpc.getEnvironmentDefinition().add(getBaseTestEnvironmentDefinition2(reset, testIsolationPrefix));
			environmentAdditionalVpc.setName(testIsolationPrefix + "SkyscapeTest");
			environmentAdditionalVpc.setEnvironmentContainerDefinitionId(environmentContainerDefinition2);
			environmentAdditionalVpc.setId("A_6");
			environmentAdditionalVpc.setNotes("notes");
		}
		return environmentAdditionalVpc;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLEnvironmentContainerDefinition getBaseTestEnvironmentContainerDefinition(boolean reset, String testIsolationPrefix) {
		if (environmentContainerDefinition == null || reset) {
			environmentContainerDefinition = new XMLEnvironmentContainerDefinition();
			environmentContainerDefinition.getGateway().add(getBaseTestGateway(reset, testIsolationPrefix));
			environmentContainerDefinition.getDataCenter().add(getBaseTestDataCenter(reset, testIsolationPrefix));
			environmentContainerDefinition.getNetwork().add(getBaseTestOrgNetwork1(reset, testIsolationPrefix));
			environmentContainerDefinition.setName(testIsolationPrefix + "Skyscape_Environment_Definition_1");
			environmentContainerDefinition.setId("A_7");
			environmentContainerDefinition.setVersion("1");
		}
		return environmentContainerDefinition;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLEnvironmentContainerDefinition getBaseTestEnvironmentContainerDefinition2(boolean reset, String testIsolationPrefix) {
		if (environmentContainerDefinition2 == null || reset) {
			environmentContainerDefinition2 = new XMLEnvironmentContainerDefinition();
			environmentContainerDefinition2.getDataCenter().add(getBaseTestDataCenter(reset, testIsolationPrefix));
			environmentContainerDefinition2.getNetwork().add(getBaseTestOrgNetwork2(reset, testIsolationPrefix));
			environmentContainerDefinition2.setName(testIsolationPrefix + "Skyscape_Environment_Definition_2");
			environmentContainerDefinition2.setId("A_8");
		}
		return environmentContainerDefinition2;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLDataCenterType getBaseTestDataCenter(boolean reset, String testIsolationPrefix) {
		if (datacenter == null || reset) {
			datacenter = new XMLDataCenterType();
			datacenter.setName(VDC);
			datacenter.setId("A_9");
		}
		return datacenter;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLOrganisationalNetworkType getBaseTestOrgNetwork1(boolean reset, String testIsolationPrefix) {
		if (orgNetwork1 == null || reset) {
			orgNetwork1 = new XMLOrganisationalNetworkType();
			//This is because we cannot delete from skyscape as they do no allow us! Keep the same name so we can at least test other things.
			orgNetwork1.setName("UNITTEST-ORGNETWORK");
			orgNetwork1.setShared(true);
			orgNetwork1.setFenceMode("BRIDGED");
			orgNetwork1.setGatewayAddress("192.168.1.1");
			orgNetwork1.setNetworkMask("255.255.255.0");
			orgNetwork1.setPrimaryDns("11.0.0.1");
			orgNetwork1.setSecondaryDns("10.9.1.1");
			orgNetwork1.setDnsSuffix("np-mebc-mtl1-ho.ipt.local");
			orgNetwork1.setIpRangeStart("192.168.1.2");
			orgNetwork1.setIpRangeEnd("192.168.1.100");
			orgNetwork1.setStaticIpPool("string");
			orgNetwork1.setCIDR("192.168.1.1/24");
			orgNetwork1.setDataCenterId(getBaseTestDataCenter(reset, testIsolationPrefix));
			orgNetwork1.setGatewayId(getBaseTestGateway(reset, testIsolationPrefix));
			orgNetwork1.setDescription("test org network 1");
			orgNetwork1.setId("A_10");
			orgNetwork1.setDataCenterName("");
		}
		return orgNetwork1;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLOrganisationalNetworkType getBaseTestOrgNetwork2(boolean reset, String testIsolationPrefix) {
		throw new NotImplementedException();
//		if (orgNetwork2 == null || reset) {
//			orgNetwork2 = new XMLOrganisationalNetworkType();
//			orgNetwork2.setName(testIsolationPrefix + "TEST_ORG_NET_2");
//			orgNetwork2.setShared(true);
//			orgNetwork2.setFenceMode("BRIDGED");
//			orgNetwork2.setGatewayAddress("11.0.1.1");
//			orgNetwork2.setNetworkMask("255.255.255.0");
//			orgNetwork2.setPrimaryDns("11.16.7.9");
//			orgNetwork2.setDnsSuffix("np-mebc-mtl1-ho.ipt.local");
//			orgNetwork2.setIpRangeStart("11.0.1.2");
//			orgNetwork2.setIpRangeEnd("11.0.1.254");
//			orgNetwork2.setCIDR("11.0.1.0/24");
//			orgNetwork2.setDataCenterId(getBaseTestDataCenter(reset, testIsolationPrefix));
//			orgNetwork2.setGatewayId(getBaseTestGateway(reset, testIsolationPrefix));
//			orgNetwork2.setDescription("test org network 2");
//		}
//		return orgNetwork2;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLOrganisationalNetworkType getBaseTestOrgNetwork3(boolean reset, String testIsolationPrefix) {
		throw new NotImplementedException();
		// if (orgNetwork3 == null || reset) {
		// orgNetwork3 = new XMLOrganisationalNetworkType();
		// orgNetwork3.setName(testIsolationPrefix + "TEST_NET_3");
		// orgNetwork3.setCIDR("10.16.101.0/24");
		// orgNetwork3.setDataCenterId(getBaseTestDataCenter(reset,
		// testIsolationPrefix));
		// orgNetwork3.setGatewayId(getBaseTestGateway(reset,
		// testIsolationPrefix));
		// orgNetwork3.setDescription("test org network 3");
		// orgNetwork3.setPrimaryDns("10.0.0.0");
		// orgNetwork3.setSecondaryDns("10.0.0.0");
		// }
		// return orgNetwork3;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLEnvironmentDefinitionType getBaseTestEnvironmentDefinition(boolean reset, String testIsolationPrefix) {
		if (environmentDefinition == null || reset) {
			environmentDefinition = new XMLEnvironmentDefinitionType();
			environmentDefinition.getVirtualMachineContainer().add(getBaseTestVirtualMachineContainer(reset, testIsolationPrefix));
			environmentDefinition.setName(testIsolationPrefix + "Test1");
			environmentDefinition.setId("A_11");
			environmentDefinition.setEnvironmentDefinitionType(XMLEnvironmentDefinitionTypeType.LOGICAL);
			environmentDefinition.setVersion("1");
			environmentDefinition.setCidr("10.16.0.0/16");
		}
		return environmentDefinition;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLEnvironmentDefinitionType getBaseTestEnvironmentDefinition2(boolean reset, String testIsolationPrefix) {
		if (environmentDefinition2 == null || reset) {
			environmentDefinition2 = new XMLEnvironmentDefinitionType();
			// environmentDefinition.getVirtualMachineContainer().add(getBaseTestVirtualMachineContainer(reset,
			// testIsolationPrefix));
			environmentDefinition2.setName(testIsolationPrefix + "Test1");
			environmentDefinition2.setCidr("10.17.0.0/16");
			environmentDefinition2.setId("A_12");
			environmentDefinition2.setEnvironmentDefinitionType(XMLEnvironmentDefinitionTypeType.LOGICAL);
		}
		return environmentDefinition2;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLVirtualMachineContainerType getBaseTestVirtualMachineContainer(boolean reset, String testIsolationPrefix) {
		if (vmc == null || reset) {
			vmc = new XMLVirtualMachineContainerType();
			vmc.setServiceLevel(VDC);
			vmc.getNetwork().add(getBaseTestAppNetwork1(reset, testIsolationPrefix));
			vmc.getNetwork().add(getBaseTestAppNetwork2(reset, testIsolationPrefix));
			vmc.getVirtualMachine().add(getBaseTestVirtualMachine1(reset, testIsolationPrefix));
			vmc.getVirtualMachine().add(getBaseTestVirtualMachine2(reset, testIsolationPrefix));
			// vmc.getVirtualMachine().add(getBaseTestVirtualMachine3(reset,
			// testIsolationPrefix));
			// vmc.getVirtualMachine().add(getBaseTestVirtualMachine4(reset,
			// testIsolationPrefix));
			// vmc.getVirtualMachine().add(getBaseTestVirtualMachine5(reset,
			// testIsolationPrefix));
			vmc.setDomain(TEST_DOMAIN_NAME);
			vmc.setName(testIsolationPrefix + "TEST_VMC_IPT_HO_ipt_ho");
			vmc.setDescription("TEST_VMC container");
			vmc.setId("A_13");
			vmc.setDataCenterName("testblar");
			vmc.setRuntimeLease("notused");
			vmc.setStorageLease("notused");
		}
		return vmc;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLApplicationNetworkType getBaseTestAppNetwork1(boolean reset, String testIsolationPrefix) {

		if (appNet1 == null || reset) {
			appNet1 = new XMLApplicationNetworkType();
			appNet1.setFenceMode("NATROUTED");
			appNet1.setNetworkMask("255.255.255.0");
			appNet1.setGatewayAddress("10.9.1.1");
			appNet1.setPrimaryDns("10.9.1.1");
			appNet1.setSecondaryDns("10.9.1.1");
			appNet1.setDnsSuffix("dns-suffix");
			appNet1.setStaticIpPool("string");
			appNet1.setIpRangeStart("10.9.1.2");
			appNet1.setIpRangeEnd("10.9.1.254");
			appNet1.setDescription("App net 1 description");
			appNet1.setName(testIsolationPrefix + "TEST_APP_NET_1");
			appNet1.setDataCenterId(getBaseTestDataCenter(reset, testIsolationPrefix));
			appNet1.setId("A_14");
			appNet1.setDataCenterName("");
			appNet1.setCIDR("");

		}
		return appNet1;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLApplicationNetworkType getBaseTestAppNetwork2(boolean reset, String testIsolationPrefix) {
		if (appNet2 == null || reset) {
			appNet2 = new XMLApplicationNetworkType();
			appNet2.setFenceMode("NATROUTED");
			appNet2.setNetworkMask("255.255.255.0");
			appNet2.setGatewayAddress("10.10.1.1");
			appNet2.setPrimaryDns("10.10.1.1");
			appNet2.setSecondaryDns("10.10.1.1");
			appNet2.setDnsSuffix("dns-suffix");
			appNet2.setIpRangeStart("10.10.1.2");
			appNet2.setStaticIpPool("string");
			appNet2.setIpRangeEnd("10.10.1.254");
			appNet2.setDescription("App net 2 description");
			appNet2.setName(testIsolationPrefix + "TEST_APP_NET_2");
			appNet2.setDataCenterId(getBaseTestDataCenter(reset, testIsolationPrefix));
			appNet2.setId("A_15");
			appNet2.setDataCenterName("");
			appNet2.setCIDR("");
		}
		return appNet2;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLVirtualMachineType getBaseTestVirtualMachine1(boolean reset, String testIsolationPrefix) {
		if (vm1 == null || reset) {
			vm1 = new XMLVirtualMachineType();
			vm1.setDescription("Test VM 1");
			vm1.setComputerName(testIsolationPrefix + "testvm1");
			vm1.setVmName(testIsolationPrefix + "testvm1");
			vm1.setTemplateName(CENTOS_IMAGE);
			vm1.setTemplateServiceLevel(TEST_TEMPLATE_SERVICE_LEVEL);
			vm1.setCustomisationScript(TEST_CUSTOMISATION_SCRIPT);
			vm1.setCpuCount(new BigInteger("2"));
			vm1.setCustomisationScript("");
			vm1.setMemory(new BigInteger("4096"));
			vm1.setMemoryUnit("MB");
			vm1.setStorageProfile(STORAGE_PROFILE_BASIC);
			vm1.getStorage().add(getBaseTestVirtualMachineStorage1a(reset, testIsolationPrefix));
			// vm1.getStorage().add(getBaseTestVirtualMachineStorage2a(reset,
			// testIsolationPrefix));
			// vm1.getStorage().add(getBaseTestVirtualMachineStorage3a(reset,
			// testIsolationPrefix));
			vm1.getNIC().add(getBaseTestVirtualMachineNIC1a(reset, testIsolationPrefix));
			// vm1.getNIC().add(getBaseTestVirtualMachineNIC2a(reset,
			// testIsolationPrefix));
			// vm1.getNIC().add(getBaseTestVirtualMachineNIC3a(reset,
			// testIsolationPrefix));
			vm1.setId("A_16");
			vm1.setHardwareProfile("notused");
			vm1.setVMOrder(1);
			vm1.setVMStartAction("powerOn");
			vm1.setVMStopAction("powerOff");
			vm1.setVMStopDelay(0);
			vm1.setVMStartDelay(0);
			
			//making data match what used to happen in the legacy database export
			vm1.setComputerName(vm1.getComputerName()+"."+TEST_DOMAIN_NAME);
			vm1.setVmName(vm1.getVmName()+"."+TEST_DOMAIN_NAME);
			
		}
		return vm1;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLVirtualMachineType getBaseTestVirtualMachine2(boolean reset, String testIsolationPrefix) {
		if (vm2 == null || reset) {
			vm2 = new XMLVirtualMachineType();
			vm2.setDescription("Test VM 2");
			vm2.setComputerName(testIsolationPrefix + "testvm2");
			vm2.setVmName(testIsolationPrefix + "testvm2");
			vm2.setTemplateName(CENTOS_IMAGE);
			vm2.setTemplateServiceLevel(TEST_TEMPLATE_SERVICE_LEVEL);
			vm2.setCustomisationScript("");
			// vm2.setCustomisationScript(TEST_CUSTOMISATION_SCRIPT);
			vm2.setCpuCount(new BigInteger("2"));
			vm2.setMemory(new BigInteger("4096"));
			vm2.setMemoryUnit("MB");
			vm2.setStorageProfile(STORAGE_PROFILE_BASIC);
			vm2.getStorage().add(getBaseTestVirtualMachineStorage1b(reset, testIsolationPrefix));
			// vm2.getStorage().add(getBaseTestVirtualMachineStorage2b(reset,
			// testIsolationPrefix));
			// vm2.getStorage().add(getBaseTestVirtualMachineStorage3b(reset,
			// testIsolationPrefix));
			vm2.getNIC().add(getBaseTestVirtualMachineNIC1b(reset, testIsolationPrefix));
			vm2.getNIC().add(getBaseTestVirtualMachineNIC2b(reset, testIsolationPrefix));
			// vm2.getNIC().add(getBaseTestVirtualMachineNIC3b(reset,
			// testIsolationPrefix));
			vm2.setId("A_17");
			vm2.setHardwareProfile("notused");
			vm2.setVMOrder(1);
			vm2.setVMStartAction("powerOn");
			vm2.setVMStopAction("powerOff");
			vm2.setVMStopDelay(0);
			vm2.setVMStartDelay(0);
			
			//making data match what used to happen in the legacy database export
			vm2.setComputerName(vm2.getComputerName()+"."+TEST_DOMAIN_NAME);
			vm2.setVmName(vm2.getVmName()+"."+TEST_DOMAIN_NAME);
		}
		return vm2;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLVirtualMachineType getBaseTestVirtualMachine3(boolean reset, String testIsolationPrefix) {
		if (vm3 == null || reset) {
			vm3 = new XMLVirtualMachineType();
			vm3.setDescription("Test VM 3");
			vm3.setComputerName(testIsolationPrefix + "testvm3");
			vm3.setVmName(testIsolationPrefix + "testvm3");
			vm3.setTemplateName(CENTOS_IMAGE);
			vm3.setTemplateServiceLevel(TEST_TEMPLATE_SERVICE_LEVEL);
			vm3.setCustomisationScript(TEST_CUSTOMISATION_SCRIPT);
			vm3.setCpuCount(new BigInteger("2"));
			vm3.setMemory(new BigInteger("4096"));
			vm3.setMemoryUnit("MB");
			vm3.setStorageProfile(STORAGE_PROFILE_BASIC);
			vm3.getStorage().add(getBaseTestVirtualMachineStorage1c(reset, testIsolationPrefix));
			vm3.getStorage().add(getBaseTestVirtualMachineStorage2c(reset, testIsolationPrefix));
			vm3.getStorage().add(getBaseTestVirtualMachineStorage3c(reset, testIsolationPrefix));
			vm3.getNIC().add(getBaseTestVirtualMachineNIC1c(reset, testIsolationPrefix));
			vm3.getNIC().add(getBaseTestVirtualMachineNIC2c(reset, testIsolationPrefix));
			vm3.getNIC().add(getBaseTestVirtualMachineNIC3c(reset, testIsolationPrefix));
			vm3.setId("A_18");
			vm3.setHardwareProfile("notused");
			
			//making data match what used to happen in the legacy database export
			vm3.setComputerName(vm3.getComputerName()+"."+TEST_DOMAIN_NAME);
			vm3.setVmName(vm3.getVmName()+"."+TEST_DOMAIN_NAME);
		}
		return vm3;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLVirtualMachineType getBaseTestVirtualMachine4(boolean reset, String testIsolationPrefix) {
		if (vm4 == null || reset) {
			vm4 = new XMLVirtualMachineType();
			vm4.setDescription("Test VM 4");
			vm4.setComputerName(testIsolationPrefix + "testvm4");
			vm4.setVmName(testIsolationPrefix + "testvm4");
			vm4.setTemplateName(CENTOS_IMAGE);
			vm4.setTemplateServiceLevel(TEST_TEMPLATE_SERVICE_LEVEL);
			vm4.setCustomisationScript(TEST_CUSTOMISATION_SCRIPT);
			vm4.setCpuCount(new BigInteger("2"));
			vm4.setMemory(new BigInteger("4096"));
			vm4.setMemoryUnit("MB");
			vm4.setStorageProfile(STORAGE_PROFILE_BASIC);
			vm4.getStorage().add(getBaseTestVirtualMachineStorage1d(reset, testIsolationPrefix));
			vm4.getNIC().add(getBaseTestVirtualMachineNIC1d(reset, testIsolationPrefix));
			vm4.setId("A_19");
			vm4.setHardwareProfile("notused");
			
			//making data match what used to happen in the legacy database export
			vm4.setComputerName(vm4.getComputerName()+"."+TEST_DOMAIN_NAME);
			vm4.setVmName(vm4.getVmName()+"."+TEST_DOMAIN_NAME);
		}
		return vm4;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLVirtualMachineType getBaseTestVirtualMachine5(boolean reset, String testIsolationPrefix) {
		if (vm5 == null || reset) {
			vm5 = new XMLVirtualMachineType();
			vm5.setDescription("Test VM 5");
			vm5.setComputerName(testIsolationPrefix + "testvm5");
			vm5.setVmName(testIsolationPrefix + "testvm5");
			vm5.setTemplateName(CENTOS_IMAGE);
			vm5.setTemplateServiceLevel(TEST_TEMPLATE_SERVICE_LEVEL);
			vm5.setCustomisationScript(TEST_CUSTOMISATION_SCRIPT);
			vm5.setCpuCount(new BigInteger("2"));
			vm5.setMemory(new BigInteger("4096"));
			vm5.setMemoryUnit("MB");
			vm5.setStorageProfile(STORAGE_PROFILE_BASIC);
			vm5.getStorage().add(getBaseTestVirtualMachineStorage1e(reset, testIsolationPrefix));
			vm5.getNIC().add(getBaseTestVirtualMachineNIC1e(reset, testIsolationPrefix));
			vm5.setId("A_20");
			
			//making data match what used to happen in the legacy database export
			vm5.setComputerName(vm5.getComputerName()+"."+TEST_DOMAIN_NAME);
			vm5.setVmName(vm5.getVmName()+"."+TEST_DOMAIN_NAME);
		}
		return vm5;
	}

	/**
	 * 
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage1a(boolean reset, String testIsolationPrefix) {
		if (hdd1a == null || reset) {
			hdd1a = new XMLStorageType();
			hdd1a.setBusSubType("");
			hdd1a.setBusType("");
			hdd1a.setDeviceMount("");
			hdd1a.setIndexNumber(new BigInteger("1"));
			hdd1a.setSize(new BigInteger("50"));
			hdd1a.setSizeUnit("GB");
			hdd1a.setId("A_21");
		}
		return hdd1a;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage2a(boolean reset, String testIsolationPrefix) {
		if (hdd2a == null || reset) {
			hdd2a = new XMLStorageType();
			hdd2a.setBusSubType("");
			hdd2a.setBusType("");
			hdd2a.setDeviceMount("");
			hdd2a.setIndexNumber(new BigInteger("2"));
			hdd2a.setSize(new BigInteger("50"));
			hdd2a.setSizeUnit("GB");
			hdd2a.setId("A_22");
		}
		return hdd2a;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage3a(boolean reset, String testIsolationPrefix) {
		if (hdd3a == null || reset) {
			hdd3a = new XMLStorageType();
			hdd3a.setBusSubType("");
			hdd3a.setBusType("");
			hdd3a.setDeviceMount("");
			hdd3a.setIndexNumber(new BigInteger("3"));
			hdd3a.setSize(new BigInteger("50"));
			hdd3a.setSizeUnit("GB");
			hdd3a.setId("A_23");
		}
		return hdd3a;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage1b(boolean reset, String testIsolationPrefix) {
		if (hdd1b == null || reset) {
			hdd1b = new XMLStorageType();
			hdd1b.setBusSubType("");
			hdd1b.setBusType("");
			hdd1b.setDeviceMount("");
			hdd1b.setIndexNumber(new BigInteger("1"));
			hdd1b.setSize(new BigInteger("50"));
			hdd1b.setSizeUnit("GB");
			hdd1b.setId("A_24");
		}
		return hdd1b;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage2b(boolean reset, String testIsolationPrefix) {
		if (hdd2b == null || reset) {
			hdd2b = new XMLStorageType();
			hdd2b.setBusSubType("");
			hdd2b.setBusType("");
			hdd2b.setDeviceMount("");
			hdd2b.setIndexNumber(new BigInteger("2"));
			hdd2b.setSize(new BigInteger("50"));
			hdd2b.setSizeUnit("GB");
			hdd2b.setId("A_25");
		}
		return hdd2b;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage3b(boolean reset, String testIsolationPrefix) {
		if (hdd3b == null || reset) {
			hdd3b = new XMLStorageType();
			hdd3b.setBusSubType("");
			hdd3b.setBusType("");
			hdd3b.setDeviceMount("");
			hdd3b.setIndexNumber(new BigInteger("3"));
			hdd3b.setSize(new BigInteger("50"));
			hdd3b.setSizeUnit("GB");
			hdd3b.setId("A_26");
		}
		return hdd3b;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage1c(boolean reset, String testIsolationPrefix) {
		if (hdd1c == null || reset) {
			hdd1c = new XMLStorageType();
			hdd1c.setBusSubType("");
			hdd1c.setBusType("");
			hdd1c.setDeviceMount("");
			hdd1c.setIndexNumber(new BigInteger("1"));
			hdd1c.setSize(new BigInteger("50"));
			hdd1c.setSizeUnit("GB");
			hdd1c.setId("A_27");
		}
		return hdd1c;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage2c(boolean reset, String testIsolationPrefix) {
		if (hdd2c == null || reset) {
			hdd2c = new XMLStorageType();
			hdd2c.setBusSubType("");
			hdd2c.setBusType("");
			hdd2c.setDeviceMount("");
			hdd2c.setIndexNumber(new BigInteger("2"));
			hdd2c.setSize(new BigInteger("50"));
			hdd2c.setSizeUnit("GB");
			hdd2c.setId("A_28");
		}
		return hdd2c;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage3c(boolean reset, String testIsolationPrefix) {
		if (hdd3c == null || reset) {
			hdd3c = new XMLStorageType();
			hdd3c.setBusSubType("");
			hdd3c.setBusType("");
			hdd3c.setDeviceMount("");
			hdd3c.setIndexNumber(new BigInteger("3"));
			hdd3c.setSize(new BigInteger("50"));
			hdd3c.setSizeUnit("GB");
			hdd3c.setId("A_29");
		}
		return hdd3c;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage1d(boolean reset, String testIsolationPrefix) {
		if (hdd1d == null || reset) {
			hdd1d = new XMLStorageType();
			hdd1d.setBusSubType("");
			hdd1d.setBusType("");
			hdd1d.setDeviceMount("");
			hdd1d.setIndexNumber(new BigInteger("1"));
			hdd1d.setSize(new BigInteger("50"));
			hdd1d.setSizeUnit("GB");
			hdd1d.setId("A_30");
		}
		return hdd1d;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage2d(boolean reset, String testIsolationPrefix) {
		if (hdd2d == null || reset) {
			hdd2d = new XMLStorageType();
			hdd2d.setBusSubType("");
			hdd2d.setBusType("");
			hdd2d.setDeviceMount("");
			hdd2d.setIndexNumber(new BigInteger("2"));
			hdd2d.setSize(new BigInteger("50"));
			hdd2d.setSizeUnit("GB");
			hdd2d.setId("A_31");
		}
		return hdd2d;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage3d(boolean reset, String testIsolationPrefix) {
		if (hdd3d == null || reset) {
			hdd3d = new XMLStorageType();
			hdd3d.setBusSubType("");
			hdd3d.setBusType("");
			hdd3d.setDeviceMount("");
			hdd3d.setIndexNumber(new BigInteger("3"));
			hdd3d.setSize(new BigInteger("50"));
			hdd3d.setSizeUnit("GB");
			hdd3d.setId("A_32");
		}
		return hdd3d;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage1e(boolean reset, String testIsolationPrefix) {
		if (hdd1e == null || reset) {
			hdd1e = new XMLStorageType();
			hdd1e.setBusSubType("");
			hdd1e.setBusType("");
			hdd1e.setDeviceMount("");
			hdd1e.setIndexNumber(new BigInteger("1"));
			hdd1e.setSize(new BigInteger("50"));
			hdd1e.setSizeUnit("GB");
			hdd1e.setId("A_33");
		}
		return hdd1e;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC1a(boolean reset, String testIsolationPrefix) {
		if (nic1a == null || reset) {
			nic1a = new XMLNICType();
			nic1a.setIndexNumber(new BigInteger("0"));
			nic1a.setPrimary(true);
			nic1a.setIpAssignment("MANUAL");
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setInterfaceNumber(new BigInteger("1"));
			ip1.setName("IPT_UNITTEST_ROLE");
			ip1.setStaticIpAddress("10.9.1.2");
			ip1.setStaticIpPool("usless");
			ip1.setNetworkMask("255.255.255.0");
			ip1.setIsVip(false);
			nic1a.getInterface().add(ip1);
			nic1a.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
			nic1a.setId("A_34");
			nic1a.setNetworkName("notused");
		}
		return nic1a;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC2a(boolean reset, String testIsolationPrefix) {
		if (nic2a == null || reset) {
			nic2a = new XMLNICType();
			nic2a.setIpAssignment("MANUAL");
			nic2a.setIndexNumber(new BigInteger("1"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1a");
			ip1.setStaticIpPool("usless");
			ip1.setNetworkMask("255.255.255.0");
			ip1.setStaticIpAddress("10.9.1.3");
			nic2a.getInterface().add(ip1);
			nic2a.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
			nic2a.setId("A_35");
			nic2a.setNetworkName("notused");
		}
		return nic2a;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC3a(boolean reset, String testIsolationPrefix) {
		if (nic3a == null || reset) {
			nic3a = new XMLNICType();
			nic3a.setIpAssignment("MANUAL");
			nic3a.setIndexNumber(new BigInteger("2"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic2a");
			ip1.setStaticIpPool("usless");
			ip1.setNetworkMask("255.255.255.0");
			nic3a.setPrimary(false);
			ip1.setStaticIpAddress("192.168.240.4");
			nic3a.getInterface().add(ip1);
			nic3a.setNetworkID(getBaseTestOrgNetwork1(reset, testIsolationPrefix));
			nic3a.setId("A_36");
			nic3a.setNetworkName("notused");
		}
		return nic3a;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC1b(boolean reset, String testIsolationPrefix) {
		if (nic1b == null || reset) {
			nic1b = new XMLNICType();
			nic1b.setIpAssignment("MANUAL");
			nic1b.setIndexNumber(new BigInteger("0"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setInterfaceNumber(new BigInteger("1"));
			ip1.setStaticIpPool("usless");
			ip1.setNetworkMask("255.255.255.0");
			ip1.setName(testIsolationPrefix + "testvm1nic0b");
			nic1b.setPrimary(true);
			ip1.setStaticIpAddress("10.9.1.10");
			nic1b.getInterface().add(ip1);
			nic1b.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
			nic1b.setId("A_37");
			nic1b.setNetworkName("notused");
		}
		return nic1b;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC2b(boolean reset, String testIsolationPrefix) {
		if (nic2b == null || reset) {
			nic2b = new XMLNICType();
			nic2b.setIpAssignment("MANUAL");
			nic2b.setIndexNumber(new BigInteger("1"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setInterfaceNumber(new BigInteger("1"));
			ip1.setName(testIsolationPrefix + "testvm1nic1b");
			ip1.setStaticIpPool("usless");
			ip1.setNetworkMask("255.255.255.0");
			nic2b.setPrimary(false);
			ip1.setStaticIpAddress("10.9.1.11");
			nic2b.getInterface().add(ip1);
			nic2b.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
			nic2b.setId("A_38");
			nic2b.setNetworkName("notused");
		}
		return nic2b;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC3b(boolean reset, String testIsolationPrefix) {
		if (nic3b == null || reset) {
			nic3b = new XMLNICType();
			nic3b.setIpAssignment("MANUAL");
			nic3b.setIndexNumber(new BigInteger("2"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic2b");
			ip1.setStaticIpPool("usless");
			ip1.setNetworkMask("255.255.255.0");
			ip1.setInterfaceNumber(new BigInteger("1"));
			nic3b.setPrimary(false);
			ip1.setStaticIpAddress("10.9.1.12");
			nic3b.getInterface().add(ip1);
			nic3b.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
			nic3b.setId("A_39");
			nic3b.setNetworkName("notused");
		}
		return nic3b;
	}

	protected static XMLInterfaceType nic1c_ip1 = new XMLInterfaceType();

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC1c(boolean reset, String testIsolationPrefix) {
		if (nic1c == null || reset) {
			nic1c = new XMLNICType();
			nic1c.setIpAssignment("MANUAL");
			nic1c.setIndexNumber(new BigInteger("0"));
			nic1c_ip1.setNetworkMask("255.255.255.0");
			nic1c_ip1.setName(testIsolationPrefix + "testvm1nic0c");
			nic1c_ip1.setInterfaceNumber(new BigInteger("1"));
			nic1c_ip1.setStaticIpPool("usless");
			nic1c.setPrimary(true);
			nic1c_ip1.setStaticIpAddress("10.16.100.6");
			nic1c.getInterface().add(nic1c_ip1);
			nic1c.setNetworkID(getBaseTestOrgNetwork1(reset, testIsolationPrefix));
			nic1c.setId("A_40");
			nic1c.setNetworkName("notused");
		}
		return nic1c;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC2c(boolean reset, String testIsolationPrefix) {
		if (nic2c == null || reset) {
			nic2c = new XMLNICType();
			nic2c.setIpAssignment("MANUAL");
			nic2c.setIndexNumber(new BigInteger("1"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1c");
			ip1.setStaticIpPool("usless");
			ip1.setInterfaceNumber(new BigInteger("1"));
			ip1.setNetworkMask("255.255.255.0");
			nic2c.setPrimary(false);
			ip1.setStaticIpAddress("10.16.1.6");
			nic2c.getInterface().add(ip1);
			nic2c.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
			nic2c.setId("A_41");
			nic2c.setNetworkName("notused");
		}
		return nic2c;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC3c(boolean reset, String testIsolationPrefix) {
		if (nic3c == null || reset) {
			nic3c = new XMLNICType();
			nic3c.setIpAssignment("MANUAL");
			nic3c.setIndexNumber(new BigInteger("2"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic2c");
			ip1.setStaticIpPool("usless");
			ip1.setInterfaceNumber(new BigInteger("1"));
			ip1.setNetworkMask("255.255.255.0");
			nic3c.setPrimary(false);
			ip1.setStaticIpAddress("10.16.2.6");
			nic3c.getInterface().add(ip1);
			nic3c.setNetworkID(getBaseTestAppNetwork2(reset, testIsolationPrefix));
			nic3c.setId("A_42");
			nic3c.setNetworkName("notused");
		}
		return nic3c;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC1d(boolean reset, String testIsolationPrefix) {
		if (nic1d == null || reset) {
			nic1d = new XMLNICType();
			nic1d.setIpAssignment("MANUAL");
			nic1d.setIndexNumber(new BigInteger("0"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic0d");
			ip1.setStaticIpPool("usless");
			ip1.setInterfaceNumber(new BigInteger("1"));
			ip1.setNetworkMask("255.255.255.0");
			nic1d.setPrimary(true);
			ip1.setStaticIpAddress("10.16.100.7");
			nic1d.getInterface().add(ip1);
			nic1d.setNetworkID(getBaseTestOrgNetwork1(reset, testIsolationPrefix));
			nic1d.setId("A_43");
			nic1d.setNetworkName("notused");
		}
		return nic1d;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC2d(boolean reset, String testIsolationPrefix) {
		if (nic2d == null || reset) {
			nic2d = new XMLNICType();
			nic2d.setIpAssignment("MANUAL");
			nic2d.setIndexNumber(new BigInteger("1"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1d");
			ip1.setStaticIpPool("usless");
			ip1.setInterfaceNumber(new BigInteger("1"));
			ip1.setNetworkMask("255.255.255.0");
			nic2d.setPrimary(false);
			ip1.setStaticIpAddress("10.16.1.8");
			nic2d.getInterface().add(ip1);
			nic2d.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
			nic2d.setId("A_44");
			nic2d.setNetworkName("notused");
		}
		return nic2d;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC3d(boolean reset, String testIsolationPrefix) {
		if (nic3d == null || reset) {
			nic3d = new XMLNICType();
			nic3d.setIpAssignment("MANUAL");
			nic3d.setIndexNumber(new BigInteger("2"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic2d1");
			ip1.setStaticIpPool("usless");
			ip1.setNetworkMask("255.255.255.0");
			ip1.setInterfaceNumber(new BigInteger("1"));
			nic3d.setPrimary(false);
			ip1.setStaticIpAddress("10.16.2.7");
			nic3d.getInterface().add(ip1);
			nic3d.setNetworkID(getBaseTestAppNetwork2(reset, testIsolationPrefix));
			nic3d.setId("A_45");
			nic3d.setNetworkName("notused");
		}
		return nic3d;
	}

	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC1e(boolean reset, String testIsolationPrefix) {
		if (nic1e == null || reset) {
			nic1e = new XMLNICType();
			nic1e.setIpAssignment("MANUAL");
			nic1e.setIndexNumber(new BigInteger("0"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1e");
			ip1.setStaticIpPool("usless");
			ip1.setNetworkMask("255.255.255.0");
			ip1.setInterfaceNumber(new BigInteger("1"));
			nic1e.setPrimary(false);
			ip1.setStaticIpAddress("10.16.101.4");
			nic1e.getInterface().add(ip1);
			nic1e.setNetworkID(getBaseTestOrgNetwork2(reset, testIsolationPrefix));
			nic1e.setId("A_46");
			nic1e.setNetworkName("notused");
		}
		return nic1e;
	}

	public static boolean confirmVAppExists(XMLVirtualMachineContainerType vappConfig) throws Exception {
		boolean exists = false;
		try {
			cloudManager.confirmVirtualMachineContainer(CmdStrategy.EXISTS, null, vappConfig, null);
			Vapp vApp = vappManager.confirmVApp(CmdStrategy.EXISTS, vappConfig);
			// / What is the result of this? Presumably it returns the VApp if
			// it exists
			logger.debug("Veapp exists result is: " + vApp.getMetadata());
			if (vApp != null) {
				exists = true;
			}
		} catch (Exception e) {
			logger.debug("confirmVappExists - Exception caught, normally indicates it doesn't exist. " + "NB this needs firming up...", e);
		}
		return exists;
	}

	public static void stopAndDelete(XMLVirtualMachineContainerType vappConfig) {
		// delete
		try {
			cloudManager.stopVirtualMachineContainer(vappConfig);
		} catch (Exception e) {
			logger.debug("Cannot stop - may not exist");
		}
		try {
			cloudManager.shutdownVirtualMachineContainer(vappConfig);
		} catch (Exception e) {
			logger.debug("Cannot shutdown - may not exist");
		}
		try {
			cloudManager.undeployVirtualMachineContainer(vappConfig);
		} catch (Exception ex) {
			logger.debug("Cannot Undeploy - may not exist");
		}
		try {
			cloudManager.deleteVirtualMachineContainer(null, vappConfig, null);
		} catch (Exception ex) {
			logger.debug("Cannot delete - may not exist",ex);
		}
	}
	
	public static void stopVmc(XMLVirtualMachineContainerType vappConfig) {
		// delete
		try {
			cloudManager.stopVirtualMachineContainer(vappConfig);
		} catch (Exception e) {
			logger.debug("Cannot stop - may not exist",e);
		}
		try {
			cloudManager.shutdownVirtualMachineContainer(vappConfig);
		} catch (Exception e) {
			logger.debug("Cannot shutdown - may not exist",e);
		}
		try {
			cloudManager.undeployVirtualMachineContainer(vappConfig);
		} catch (Exception ex) {
			logger.debug("Cannot Undeploy - may not exist",ex);
		}
	}

	public Vapp getVApp(XMLVirtualMachineContainerType vappConfig) throws Exception {
		Vapp vApp = null;
		try {
			vApp = vappManager.confirmVApp(CmdStrategy.EXISTS, vappConfig);
			// / What is the result of this? Presumably it returns the VApp if
			// it exists
			logger.debug("getVApp() :: Vapp is: " + vApp);
		} catch (Exception e) {
			logger.debug("confirmVappExists - Exception caught, normally indicates it doesn't exist. " + "NB this needs firming up...");
			e.printStackTrace();
		}
		return vApp;
	}

	/**
	 * Method for starting a vapp
	 * 
	 * @param cloudManager
	 * @param applications
	 * @param vappConfig
	 * @throws Exception
	 */
	public void powerOnVApp(XMLVirtualMachineContainerType vappConfig) throws Exception {
		cloudManager.startVirtualMachineContainer(vappConfig);
	}

	/**
	 * Method for stopping a vapp
	 * 
	 * @param cloudManager
	 * @param applications
	 * @param vappConfig
	 * @throws Exception
	 */
	public void powerOffVApp(XMLVirtualMachineContainerType vappConfig) throws Exception {
		cloudManager.stopVirtualMachineContainer(vappConfig);
	}

	/**
	 * Method for shutting down a vapp
	 * 
	 * @param cloudManager
	 * @param applications
	 * @param vappConfig
	 * @throws Exception
	 */
	public void shutdownVApp(XMLVirtualMachineContainerType vappConfig) throws Exception {
		cloudManager.shutdownVirtualMachineContainer(vappConfig);
	}

	/**
	 * Method for rebooting down a vapp
	 * 
	 * @param cloudManager
	 * @param applications
	 * @param vappConfig
	 * @throws Exception
	 */
	public void rebootVApp(XMLVirtualMachineContainerType vappConfig) throws Exception {

		cloudManager.rebootVirtualMachineContainer(vappConfig);
	}

	/**
	 * Method for starting a vapp
	 * 
	 * @param cloudManager
	 * @param applications
	 * @param vappConfig
	 * @throws Exception
	 */
	public void suspendVApp(XMLVirtualMachineContainerType vappConfig) throws Exception {

		cloudManager.suspendVirtualMachineContainer(vappConfig);

	}

	/**
	 * Method for stopping a vapp
	 * 
	 * @param cloudManager
	 * @param applications
	 * @param vappConfig
	 * @throws Exception
	 */
	public void discardSuspendVApp(XMLVirtualMachineContainerType vappConfig) throws Exception {
		cloudManager.resumeVirtualMachineContainer(vappConfig);
	}

	/**
	 * Method for deploying a vapp
	 * 
	 * @param cloudManager
	 * @param applications
	 * @param vappConfig
	 * @throws Exception
	 */
	public void deployVApp(XMLVirtualMachineContainerType vappConfig) throws Exception {
		cloudManager.deployVirtualMachineContainer(vappConfig);
	}

	/**
	 * Method for deleting a vapp
	 * 
	 * @param cloudManager
	 * @param applications
	 * @param vappConfig
	 * @throws Exception
	 */
	public void undeployVApp(XMLVirtualMachineContainerType vappConfig) throws Exception {
		cloudManager.undeployVirtualMachineContainer(vappConfig);
	}

	/**
	 * Method for deleting a vapp
	 * 
	 * @param cloudManager
	 * @param applications
	 * @param vappConfig
	 * @throws Exception
	 */
	public void deleteVApp(XMLVirtualMachineContainerType vappConfig) throws Exception {
		cloudManager.deleteVirtualMachineContainer(null, vappConfig, null);
	}

}
