package com.ipt.ebsa.agnostic.client.aws;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.xml.sax.SAXException;

import com.amazonaws.services.ec2.model.Vpc;
import com.ipt.ebsa.agnostic.client.BaseTest;
import com.ipt.ebsa.agnostic.client.aws.connection.AwsCloudValues;
import com.ipt.ebsa.agnostic.client.aws.module.AwsEnvironmentModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsGatewayModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsNetworkModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsRoleModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsSecurityGroupModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsVmContainerModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsVmModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsVolumeModule;
import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;
import com.ipt.ebsa.agnostic.client.controller.Controller;
import com.ipt.ebsa.agnostic.client.exception.InvalidConfigurationException;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.EnvironmentOverrideException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLApplicationNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLDataCenterType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType.XMLEnvironmentContainerDefinition;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentDefinitionType;
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
import com.jcabi.aspects.Loggable;
import com.vmware.vcloud.sdk.VCloudException;

/**
 * Amazon Web Services base test for testing the api used for creating aws
 * components.
 * 
 * This class contains test data management by default to isolate tests from
 * each other.
 * 
 * Each time this class is loaded the test data is rekeyed to allow automation
 * of test to be more reliable and consistent.
 * 
 *
 */
@Loggable(prepend=true)
public class AwsBaseTest extends BaseTest {

	private static Logger logger = LogManager.getLogger(AwsBaseTest.class);
	
	public static String configFile = "src/test/resources/aws/aws-test-config.properties";
	public static String jenkinsOverridesFile = "src/test/resources/aws/aws-test-jenkins-overrides.properties";

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
	protected static XMLOrganisationalNetworkType orgNetwork4;
	protected static XMLVirtualMachineContainerType vmc;
	protected static XMLVirtualMachineType vm1;
	protected static XMLVirtualMachineType vm2;
	protected static XMLVirtualMachineType vm3;
	protected static XMLVirtualMachineType vm4;
	protected static XMLVirtualMachineType vm5;
	protected static XMLVirtualMachineType vm6;
	protected static XMLVirtualMachineType vm7;
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
	protected static XMLNICType nic1f;
	protected static XMLNICType nic1g;
	protected static String testPrefixIdent;
	protected static String testPrefixIdentAdditionalVpc;
	protected static String testVpcId;
	protected static String testAdditionalVpcId;
	
	public static final String CENTOS_AMI = "ami-08acfa7f";
	public static final String VYOS_AMI = "ami-9feddae8";
	
	static AwsEnvironmentModule envModule;
	static AwsVmContainerModule vmcModule;
	static AwsSecurityGroupModule securityGroupModule;
	static AwsNetworkModule networkModule;
	static AwsGatewayModule gatewayManager;
	static AwsVmModule vmModule;
	static AwsVolumeModule volumeModule;
	static AwsRoleModule roleManager;
	static AwsCloudValues cv;
	static Vpc baseVpc;
	
	@BeforeClass
	public static void setUpBeforeAwsBaseTestClass() throws InterruptedException {
		//ConfigurationFactory.setConfigFile(new File(configFile));
		if (System.getProperty("user.name").contains("jenkins")) {
			Properties overrides = new Properties();
			try {
				overrides.load(new FileInputStream(jenkinsOverridesFile));
			} catch (Exception e) {
				logger.error("Unable to load overrides from file " + jenkinsOverridesFile, e);
				fail("Unable to load Jenkins overrides");
			}
			for (Entry<Object, Object> override : overrides.entrySet()) {
				ConfigurationFactory.getProperties().setProperty((String)override.getKey(), (String)override.getValue());
			}
		} else {
			ConfigurationFactory.setConfigFile(new File(configFile));
		}
		container = weld.initialize();
		envModule = container.instance().select(AwsEnvironmentModule.class).get();
		vmcModule = container.instance().select(AwsVmContainerModule.class).get();
		networkModule = container.instance().select(AwsNetworkModule.class).get();
		gatewayManager = container.instance().select(AwsGatewayModule.class).get();
		vmModule = container.instance().select(AwsVmModule.class).get();
		volumeModule = container.instance().select(AwsVolumeModule.class).get();
		roleManager = container.instance().select(AwsRoleModule.class).get();
		cv = container.instance().select(AwsCloudValues.class).get();
		securityGroupModule = container.instance().select(AwsSecurityGroupModule.class).get();
		
		testPrefixIdent = getTestPrefixIdent();
		testPrefixIdentAdditionalVpc = getTestPrefixIdent();
		resetBaseTestConfig(testPrefixIdent,testPrefixIdentAdditionalVpc);
		baseVpc = envModule.createVpc(environment);
		testVpcId = baseVpc.getVpcId();
	}

	@AfterClass
	public static void tearDownAfterAwsBaseTestClass() throws ToManyResultsException, InterruptedException, UnSafeOperationException {
		envModule.deleteVpcById(testVpcId, "UNITTEST");
		if(environmentAdditionalVpc != null && StringUtils.isNoneBlank(testAdditionalVpcId)) {
			envModule.deleteVpcById(testAdditionalVpcId, "UNITTEST");
		}
	}

	@Before
	public void setUpAwsBaseTest() throws Exception {
		if (System.getProperty("user.name").equals("jenkins")) {
			ConfigurationFactory.setConfigFile(new File("src/test/resources/test-config.properties"));
		} else {
			ConfigurationFactory.setConfigFile(new File("src/test/resources/aws/aws-test-config.properties"));
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
			gateway.setName(testIsolationPrefix + "AwsTestGatway_1");
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
			env.setName(testIsolationPrefix + "AmazonTestContainer");
			env.setProvider(XMLProviderType.AWS);
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
			env2.setName(testIsolationPrefix + "AmazonTestContainer");
			env2.setProvider(XMLProviderType.AWS);
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
			environment.setName(testIsolationPrefix + "AmazonTest_IPT_HO_ipt_ho");
			environment.setEnvironmentContainerDefinitionId(environmentContainerDefinition);
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
			environmentAdditionalVpc.setName(testIsolationPrefix + "AmazonTest");
			environmentAdditionalVpc.setEnvironmentContainerDefinitionId(environmentContainerDefinition2);
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
			environmentContainerDefinition.setName(testIsolationPrefix + "AmazonPoC_Environment_Definition");
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
			getBaseTestOrgNetwork4(reset, testIsolationPrefix);
			//environmentContainerDefinition2.getNetwork().add(getBaseTestOrgNetwork4(reset, testIsolationPrefix));
			environmentContainerDefinition2.setName(testIsolationPrefix + "AmazonPoC_Environment_Definition");
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
			datacenter.setName("eu-west-1a");
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
			orgNetwork1.setName(testIsolationPrefix + "TEST_NET_1_IPT_HO_ipt_ho");
			orgNetwork1.setCIDR("10.16.100.0/24");
			orgNetwork1.setDataCenterId(getBaseTestDataCenter(reset, testIsolationPrefix));
			//orgNetwork1.setGatewayId(getBaseTestGateway(reset, testIsolationPrefix));
			orgNetwork1.setDescription("test org network 1");
			orgNetwork1.setPrimaryDns("10.0.0.0");
			orgNetwork1.setSecondaryDns("10.0.0.0");
			
			
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
		if (orgNetwork2 == null || reset) {
			orgNetwork2 = new XMLOrganisationalNetworkType();
			orgNetwork2.setName(testIsolationPrefix + "TEST_NET_2");
			orgNetwork2.setCIDR("10.17.1.0/24");
			orgNetwork2.setDataCenterId(getBaseTestDataCenter(reset, testIsolationPrefix));
			orgNetwork2.setDescription("test org network 2");
			orgNetwork2.setPrimaryDns("10.0.0.0");
			orgNetwork2.setSecondaryDns("10.0.0.0");
		}
		return orgNetwork2;
	}
	
	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLOrganisationalNetworkType getBaseTestOrgNetwork3(boolean reset, String testIsolationPrefix) {
		if (orgNetwork3 == null || reset) {
			orgNetwork3 = new XMLOrganisationalNetworkType();
			orgNetwork3.setName(testIsolationPrefix + "TEST_ORG_NET_3");
			orgNetwork3.setCIDR("10.16.101.0/24");
			orgNetwork3.setDataCenterId(getBaseTestDataCenter(reset, testIsolationPrefix));
			//orgNetwork3.setGatewayId(getBaseTestGateway(reset, testIsolationPrefix));
			orgNetwork3.setDescription("test org network 3");
			orgNetwork3.setPrimaryDns("10.0.0.0");
			orgNetwork3.setSecondaryDns("10.0.0.0");
		}
		return orgNetwork3;
	}
	
	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLOrganisationalNetworkType getBaseTestOrgNetwork4(boolean reset, String testIsolationPrefix) {
		if (orgNetwork4 == null || reset) {
			orgNetwork4 = new XMLOrganisationalNetworkType();
			orgNetwork4.setName(testIsolationPrefix + "TEST_ORG_NET_4");
			orgNetwork4.setCIDR("10.17.2.0/24");
			orgNetwork4.setDataCenterId(getBaseTestDataCenter(reset, testIsolationPrefix));
			orgNetwork4.setDescription("test org network 4");
			orgNetwork4.setPrimaryDns("10.0.0.0");
			orgNetwork4.setSecondaryDns("10.0.0.0");
		}
		return orgNetwork4;
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
			//environmentDefinition.getVirtualMachineContainer().add(getBaseTestVirtualMachineContainer(reset, testIsolationPrefix));
			environmentDefinition2.setName(testIsolationPrefix + "Test1");
			environmentDefinition2.setCidr("10.17.0.0/16");
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
			vmc.getNetwork().add(getBaseTestAppNetwork1(reset, testIsolationPrefix));
			vmc.getNetwork().add(getBaseTestAppNetwork2(reset, testIsolationPrefix));
			vmc.getVirtualMachine().add(getBaseTestVirtualMachine1(reset, testIsolationPrefix));
			vmc.getVirtualMachine().add(getBaseTestVirtualMachine2(reset, testIsolationPrefix));
			vmc.getVirtualMachine().add(getBaseTestVirtualMachine3(reset, testIsolationPrefix));
			vmc.getVirtualMachine().add(getBaseTestVirtualMachine4(reset, testIsolationPrefix));
			vmc.getVirtualMachine().add(getBaseTestVirtualMachine5(reset, testIsolationPrefix));
			vmc.getVirtualMachine().add(getBaseTestVirtualMachine6(reset, testIsolationPrefix));
			vmc.getVirtualMachine().add(getBaseTestVirtualMachine7(reset, testIsolationPrefix));
			vmc.setDomain("test.domain.local.IPT.HO.ipt.ho");
			vmc.setName(testIsolationPrefix + "TEST_VMC_IPT_HO_ipt_ho");
			vmc.setDescription("TEST_VMC container");
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
			appNet1.setCIDR("10.16.1.0/24");
			appNet1.setGatewayAddress("10.16.1.1");
			appNet1.setDescription("App net 1 descrption");
			appNet1.setName(testIsolationPrefix + "TEST_APP_NET_1_IPT_HO_ipt_ho");
			appNet1.setDataCenterId(getBaseTestDataCenter(reset, testIsolationPrefix));
			appNet1.setPrimaryDns("10.0.0.0");
			appNet1.setSecondaryDns("10.0.0.0");

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
			appNet2.setCIDR("10.16.2.0/24");
			appNet2.setGatewayAddress("10.16.2.1");
			appNet2.setDescription("App net 1 descrption");
			appNet2.setName(testIsolationPrefix + "TEST_APP_NET_2");
			appNet2.setDataCenterId(getBaseTestDataCenter(reset, testIsolationPrefix));
			appNet2.setPrimaryDns("10.0.0.0");
			appNet2.setSecondaryDns("10.0.0.0");
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
			vm1.setComputerName("testvm1");
			vm1.setVmName(testIsolationPrefix + "testvm1");
			vm1.setTemplateName(VYOS_AMI);
			vm1.setCustomisationScript("");
			vm1.setDescription("Test VM 1");
			vm1.setHardwareProfile("m3.large");
			vm1.getStorage().add(getBaseTestVirtualMachineStorage1a(reset, testIsolationPrefix));
			vm1.getStorage().add(getBaseTestVirtualMachineStorage2a(reset, testIsolationPrefix));
			vm1.getStorage().add(getBaseTestVirtualMachineStorage3a(reset, testIsolationPrefix));
			vm1.getNIC().add(getBaseTestVirtualMachineNIC1a(reset, testIsolationPrefix));
			vm1.getNIC().add(getBaseTestVirtualMachineNIC2a(reset, testIsolationPrefix));
			vm1.getNIC().add(getBaseTestVirtualMachineNIC3a(reset, testIsolationPrefix));
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
			vm2.setComputerName("testvm2");
			vm2.setVmName(testIsolationPrefix + "testvm2");
			vm2.setStorageProfile("EBS–Optimized=true");
			vm2.setTemplateName(CENTOS_AMI);
			vm2.setCustomisationScript("");
			vm2.setDescription("Test VM 2");
			vm2.setHardwareProfile("m3.large");
			vm2.setCustomisationScript("awsUserDataScript.sh");
			vm2.getStorage().add(getBaseTestVirtualMachineStorage1b(reset, testIsolationPrefix));
			vm2.getStorage().add(getBaseTestVirtualMachineStorage2b(reset, testIsolationPrefix));
			vm2.getStorage().add(getBaseTestVirtualMachineStorage3b(reset, testIsolationPrefix));
			vm2.getNIC().add(getBaseTestVirtualMachineNIC1b(reset, testIsolationPrefix));
			vm2.getNIC().add(getBaseTestVirtualMachineNIC2b(reset, testIsolationPrefix));
			vm2.getNIC().add(getBaseTestVirtualMachineNIC3b(reset, testIsolationPrefix));
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
			vm3.setComputerName("testvm3");
			vm3.setVmName(testIsolationPrefix + "testvm3");
			vm3.setStorageProfile("EBS–Optimized=true");
			vm3.setTemplateName(CENTOS_AMI);
			vm3.setCustomisationScript("");
			vm3.setDescription("Test VM 3");
			vm3.setHardwareProfile("m3.large");
			vm3.setCustomisationScript("awsUserDataScript.sh");
			vm3.getStorage().add(getBaseTestVirtualMachineStorage1c(reset, testIsolationPrefix));
			vm3.getStorage().add(getBaseTestVirtualMachineStorage2c(reset, testIsolationPrefix));
			vm3.getStorage().add(getBaseTestVirtualMachineStorage3c(reset, testIsolationPrefix));
			vm3.getNIC().add(getBaseTestVirtualMachineNIC1c(reset, testIsolationPrefix));
			vm3.getNIC().add(getBaseTestVirtualMachineNIC2c(reset, testIsolationPrefix));
			vm3.getNIC().add(getBaseTestVirtualMachineNIC3c(reset, testIsolationPrefix));
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
			vm4.setComputerName("testvm4");
			vm4.setVmName(testIsolationPrefix + "testvm4");
			vm4.setStorageProfile("EBS–Optimized=true");
			vm4.setTemplateName(CENTOS_AMI);
			vm4.setCustomisationScript("");
			vm4.setDescription("Test VM 4");
			vm4.setHardwareProfile("m3.large");
			vm4.setCustomisationScript("awsUserDataScript.sh");
			vm4.getStorage().add(getBaseTestVirtualMachineStorage1d(reset, testIsolationPrefix));
			vm4.getNIC().add(getBaseTestVirtualMachineNIC1d(reset, testIsolationPrefix));
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
			vm5.setComputerName("testvm5");
			vm5.setVmName(testIsolationPrefix + "testvm5");
			vm5.setStorageProfile("");
			vm5.setTemplateName(CENTOS_AMI);
			vm5.setCustomisationScript("");
			vm5.setDescription("Test VM 5");
			vm5.setHardwareProfile("m3.large");
			vm5.getStorage().add(getBaseTestVirtualMachineStorage1e(reset, testIsolationPrefix));
			vm5.getNIC().add(getBaseTestVirtualMachineNIC1e(reset, testIsolationPrefix));
		}
		return vm5;
	}
	
	protected static XMLVirtualMachineType getBaseTestVirtualMachine6(boolean reset, String testIsolationPrefix) {
		if (vm6 == null || reset) {
			vm6 = new XMLVirtualMachineType();
			vm6.setComputerName("testvm6");
			vm6.setVmName(testIsolationPrefix + "testvm6");
			vm6.setStorageProfile("");
			vm6.setTemplateName(CENTOS_AMI);
			vm6.setCustomisationScript("");
			vm6.setDescription("Test VM 6");
			vm6.setHardwareProfile("m3.large");
			vm6.getStorage().add(getBaseTestVirtualMachineStorage1e(reset, testIsolationPrefix));
			vm6.getNIC().add(getBaseTestVirtualMachineNIC1f(reset, testIsolationPrefix));
		}
		return vm6;
	}
	protected static XMLVirtualMachineType getBaseTestVirtualMachine7(boolean reset, String testIsolationPrefix) {
		if (vm7 == null || reset) {
			vm7 = new XMLVirtualMachineType();
			vm7.setComputerName("testvm7");
			vm7.setVmName(testIsolationPrefix + "testvm7");
			vm7.setStorageProfile("");
			vm7.setTemplateName(CENTOS_AMI);
			vm7.setCustomisationScript("");
			vm7.setDescription("Test VM 7");
			vm7.setHardwareProfile("m3.large");
			vm7.getStorage().add(getBaseTestVirtualMachineStorage1e(reset, testIsolationPrefix));
			vm7.getNIC().add(getBaseTestVirtualMachineNIC1g(reset, testIsolationPrefix));
		}
		return vm7;
	}

	/**
	 * 
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLStorageType getBaseTestVirtualMachineStorage1a(boolean reset, String testIsolationPrefix) {
		if (hdd1a == null || reset) {
			hdd1a = new XMLStorageType();
			hdd1a.setBusSubType("iops");
			hdd1a.setBusType("gp2");
			hdd1a.setDeviceMount("/dev/sda1");
			hdd1a.setIndexNumber(new BigInteger("0"));
			hdd1a.setSize(new BigInteger("50"));
			hdd1a.setSizeUnit("GB");
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
			hdd2a.setBusSubType("iops");
			hdd2a.setBusType("gp2");
			hdd2a.setDeviceMount("/dev/sdh");
			hdd2a.setIndexNumber(new BigInteger("1"));
			hdd2a.setSize(new BigInteger("50"));
			hdd2a.setSizeUnit("GB");
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
			hdd3a.setBusSubType("iops");
			hdd3a.setBusType("gp2");
			hdd3a.setDeviceMount("/dev/sdi");
			hdd3a.setIndexNumber(new BigInteger("2"));
			hdd3a.setSize(new BigInteger("50"));
			hdd3a.setSizeUnit("GB");
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
			hdd1b.setBusSubType("iops");
			hdd1b.setBusType("gp2");
			hdd1b.setDeviceMount("/dev/sda1");
			hdd1b.setIndexNumber(new BigInteger("0"));
			hdd1b.setSize(new BigInteger("50"));
			hdd1b.setSizeUnit("GB");
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
			hdd2b.setBusSubType("iops");
			hdd2b.setBusType("gp2");
			hdd2b.setDeviceMount("/dev/sdh");
			hdd2b.setIndexNumber(new BigInteger("1"));
			hdd2b.setSize(new BigInteger("50"));
			hdd2b.setSizeUnit("GB");
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
			hdd3b.setBusSubType("iops");
			hdd3b.setBusType("gp2");
			hdd3b.setDeviceMount("/dev/sdi");
			hdd3b.setIndexNumber(new BigInteger("2"));
			hdd3b.setSize(new BigInteger("50"));
			hdd3b.setSizeUnit("GB");
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
			hdd1c.setBusSubType("iops");
			hdd1c.setBusType("gp2");
			hdd1c.setDeviceMount("/dev/sda1");
			hdd1c.setIndexNumber(new BigInteger("0"));
			hdd1c.setSize(new BigInteger("50"));
			hdd1c.setSizeUnit("GB");
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
			hdd2c.setBusSubType("iops");
			hdd2c.setBusType("gp2");
			hdd2c.setDeviceMount("/dev/sdh");
			hdd2c.setIndexNumber(new BigInteger("1"));
			hdd2c.setSize(new BigInteger("50"));
			hdd2c.setSizeUnit("GB");
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
			hdd3c.setBusSubType("iops");
			hdd3c.setBusType("gp2");
			hdd3c.setDeviceMount("/dev/sdi");
			hdd3c.setIndexNumber(new BigInteger("2"));
			hdd3c.setSize(new BigInteger("50"));
			hdd3c.setSizeUnit("GB");
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
			hdd1d.setBusSubType("iops");
			hdd1d.setBusType("gp2");
			hdd1d.setDeviceMount("/dev/sda1");
			hdd1d.setIndexNumber(new BigInteger("0"));
			hdd1d.setSize(new BigInteger("50"));
			hdd1d.setSizeUnit("GB");
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
			hdd2d.setBusSubType("iops");
			hdd2d.setBusType("gp2");
			hdd2d.setDeviceMount("/dev/sdh");
			hdd2d.setIndexNumber(new BigInteger("1"));
			hdd2d.setSize(new BigInteger("50"));
			hdd2d.setSizeUnit("GB");
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
			hdd3d.setBusSubType("iops");
			hdd3d.setBusType("gp2");
			hdd3d.setDeviceMount("/dev/sdi");
			hdd3d.setIndexNumber(new BigInteger("2"));
			hdd3d.setSize(new BigInteger("50"));
			hdd3d.setSizeUnit("GB");
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
			hdd1e.setBusSubType("iops");
			hdd1e.setBusType("gp2");
			hdd1e.setDeviceMount("/dev/sda1");
			hdd1e.setIndexNumber(new BigInteger("0"));
			hdd1e.setSize(new BigInteger("50"));
			hdd1e.setSizeUnit("GB");
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
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName("IPT_UNITTEST_ROLE");
			ip1.setStaticIpAddress("10.16.100.4");
			ip1.setIsVip(false);
			nic1a.getInterface().add(ip1);
			nic1a.setNetworkID(getBaseTestOrgNetwork1(reset, testIsolationPrefix));
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
			nic2a.setIndexNumber(new BigInteger("1"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1a");
			ip1.setStaticIpAddress("10.16.1.4");
			ip1.setIsVip(false);
			nic2a.getInterface().add(ip1);
			nic2a.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
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
			nic3a.setIndexNumber(new BigInteger("2"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic2a");
			nic3a.setPrimary(false);
			ip1.setStaticIpAddress("10.16.2.4");
			ip1.setIsVip(false);
			nic3a.getInterface().add(ip1);
			nic3a.setNetworkID(getBaseTestAppNetwork2(reset, testIsolationPrefix));
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
			nic1b.setIndexNumber(new BigInteger("0"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic0b");
			nic1b.setPrimary(true);
			ip1.setStaticIpAddress("10.16.100.5");
			nic1b.getInterface().add(ip1);
			nic1b.setNetworkID(getBaseTestOrgNetwork1(reset, testIsolationPrefix));
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
			nic2b.setIndexNumber(new BigInteger("1"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1b");
			nic2b.setPrimary(false);
			ip1.setStaticIpAddress("10.16.1.5");
			nic2b.getInterface().add(ip1);
			nic2b.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
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
			nic3b.setIndexNumber(new BigInteger("2"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic2b");
			nic3b.setPrimary(false);
			ip1.setStaticIpAddress("10.16.2.5");
			nic3b.getInterface().add(ip1);
			nic3b.setNetworkID(getBaseTestAppNetwork2(reset, testIsolationPrefix));
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
			nic1c.setIndexNumber(new BigInteger("0"));
			
			nic1c_ip1.setName(testIsolationPrefix + "testvm1nic0c");
			nic1c.setPrimary(true);
			nic1c_ip1.setStaticIpAddress("10.16.100.6");
			nic1c.getInterface().add(nic1c_ip1);
			nic1c.setNetworkID(getBaseTestOrgNetwork1(reset, testIsolationPrefix));
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
			nic2c.setIndexNumber(new BigInteger("1"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1c");
			nic2c.setPrimary(false);
			ip1.setStaticIpAddress("10.16.1.6");
			nic2c.getInterface().add(ip1);
			nic2c.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
		}
		return nic2c;
	}
	
	protected static XMLInterfaceType nic1c_ip2 = new XMLInterfaceType();
	
	protected static XMLInterfaceType getBaseTestVirtualMachineNIC1cIp2(boolean reset, String testIsolationPrefix) {
		
		nic1c_ip2.setName(testIsolationPrefix + "testvm1nic1cip2");
		nic1c_ip2.setStaticIpAddress("10.16.100.70");
		return nic1c_ip2;
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
			nic3c.setIndexNumber(new BigInteger("2"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic2c");
			nic3c.setPrimary(false);
			ip1.setStaticIpAddress("10.16.2.6");
			nic3c.getInterface().add(ip1);
			nic3c.setNetworkID(getBaseTestAppNetwork2(reset, testIsolationPrefix));
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
			nic1d.setIndexNumber(new BigInteger("0"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic0d");
			nic1d.setPrimary(true);
			ip1.setStaticIpAddress("10.16.100.7");
			nic1d.getInterface().add(ip1);
			nic1d.setNetworkID(getBaseTestOrgNetwork1(reset, testIsolationPrefix));
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
			nic2d.setIndexNumber(new BigInteger("1"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1d");
			nic2d.setPrimary(false);
			ip1.setStaticIpAddress("10.16.1.8");
			nic2d.getInterface().add(ip1);
			nic2d.setNetworkID(getBaseTestAppNetwork1(reset, testIsolationPrefix));
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
			nic3d.setIndexNumber(new BigInteger("2"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic2d1");
			nic3d.setPrimary(false);
			ip1.setStaticIpAddress("10.16.2.7");
			nic3d.getInterface().add(ip1);
			//add secondary ip
			XMLInterfaceType ip2 = new XMLInterfaceType();
			ip2.setName(testIsolationPrefix + "testvm1nic2d2");
			ip2.setStaticIpAddress("10.16.2.8");
			nic3d.getInterface().add(ip2);
			nic3d.setNetworkID(getBaseTestAppNetwork2(reset, testIsolationPrefix));
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
			nic1e.setIndexNumber(new BigInteger("0"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1e");
			nic1e.setPrimary(false);
			ip1.setStaticIpAddress("10.16.101.4");
			nic1e.getInterface().add(ip1);
			nic1e.setNetworkID(getBaseTestAppNetwork2(reset, testIsolationPrefix));
		}
		return nic1e;
	}
	
	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC1f(boolean reset, String testIsolationPrefix) {
		if (nic1f == null || reset) {
			nic1f = new XMLNICType();
			nic1f.setIndexNumber(new BigInteger("0"));
			nic1f.setPrimary(true);
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1fip1");
			ip1.setStaticIpAddress("10.16.2.40");
			XMLInterfaceType ip2 = new XMLInterfaceType();
			ip2.setName(testIsolationPrefix + "testvm1nic1fip1ip2");
			ip2.setIsVip(true);
			ip2.setStaticIpAddress("10.16.2.42");
			XMLInterfaceType ip3 = new XMLInterfaceType();
			ip3.setName(testIsolationPrefix + "testvm1nic1fip3");
			ip3.setStaticIpAddress("10.16.2.43");
			XMLInterfaceType ip4 = new XMLInterfaceType();
			ip4.setName("testvm1nic1fip4");
			ip4.setIsVip(true);
			ip4.setStaticIpAddress("10.16.2.44");
			nic1f.getInterface().add(ip1);
			nic1f.getInterface().add(ip2);
			nic1f.getInterface().add(ip3);
			nic1f.getInterface().add(ip4);
			nic1f.setNetworkID(getBaseTestAppNetwork2(reset, testIsolationPrefix));
		}
		return nic1f;
	}
	
	/**
	 * 
	 * @param reset
	 * @param testIsolationPrefix
	 * @return
	 */
	protected static XMLNICType getBaseTestVirtualMachineNIC1g(boolean reset, String testIsolationPrefix) {
		if (nic1g == null || reset) {
			nic1g = new XMLNICType();
			nic1g.setPrimary(true);
			nic1g.setIndexNumber(new BigInteger("0"));
			XMLInterfaceType ip1 = new XMLInterfaceType();
			ip1.setName(testIsolationPrefix + "testvm1nic1eip1");
			ip1.setStaticIpAddress("10.16.2.41");
			XMLInterfaceType ip2 = new XMLInterfaceType();
			ip2.setName(testIsolationPrefix + "testvm1nic1eip2");
			ip2.setIsVip(true);
			ip2.setStaticIpAddress("10.16.2.42");
			nic1g.getInterface().add(ip1);
			nic1g.getInterface().add(ip2);
			nic1g.setNetworkID(getBaseTestAppNetwork2(reset, testIsolationPrefix));
		}
		return nic1g;
	}

	/**
	 * 
	 * @param testIsolationPrefix
	 */
	public static void resetBaseTestConfig(String testIsolationPrefix, String testIsolationPrefix2) {
		getBaseTestGeographicContainer(true, testIsolationPrefix);
		getBaseTestGeographicContainer2(true, testIsolationPrefix2);
	}

	/**
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDownAwsBaseTest() throws Exception {
		
	}

	/**
	 * This is a utility method for creating the AWS POC environment easily.
	 * It's not a test, it's an install.
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws UnresolvedDependencyException
	 * @throws ConnectionException
	 * @throws InvalidConfigurationException
	 * @throws EnvironmentOverrideException 
	 */
	// @Test
	public void createPocEnvironment() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException, StrategyFailureException, VCloudException, InvalidStrategyException, UnresolvedDependencyException,
			ConnectionException, InvalidConfigurationException, EnvironmentOverrideException {
		try {
			Weld weld = new Weld();
			WeldContainer container = weld.initialize();
			ConfigurationFactory.getProperties().put("executionplan", "src/test/resources/aws/testCommandv2.xml");
			ConfigurationFactory.getProperties().put("definition", "src/test/resources/aws/awsTestEnvironment.xml");
			Controller controller = container.instance().select(Controller.class).get();
			controller.execute();
		} catch (SAXException e) {
			logger.error("SAXException parsing the config", e);
		} catch (IOException e) {
			logger.error("IOException running the config", e);
		}
	}

}
