package com.ipt.ebsa.environment.build;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import com.ipt.ebsa.database.manager.ConnectionData;
import com.ipt.ebsa.database.manager.ConnectionManager;
import com.ipt.ebsa.database.manager.GlobalConfig;
import com.ipt.ebsa.environment.build.entities.ApplicationNetwork;
import com.ipt.ebsa.environment.build.entities.ApplicationNetworkMetaData;
import com.ipt.ebsa.environment.build.entities.DBEntity;
import com.ipt.ebsa.environment.build.entities.DNat;
import com.ipt.ebsa.environment.build.entities.DataCentre;
import com.ipt.ebsa.environment.build.entities.Environment;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerBuild;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition.DefinitionType;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinitionMetaData;
import com.ipt.ebsa.environment.build.entities.Gateway;
import com.ipt.ebsa.environment.build.entities.Interface;
import com.ipt.ebsa.environment.build.entities.Nat;
import com.ipt.ebsa.environment.build.entities.Nic;
import com.ipt.ebsa.environment.build.entities.OrganisationNetwork;
import com.ipt.ebsa.environment.build.entities.OrganisationNetworkMetaData;
import com.ipt.ebsa.environment.build.entities.Storage;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainerMetaData;
import com.ipt.ebsa.environment.build.entities.VirtualMachineMetaData;

/**
 * Common unit test methods and functions 
 *
 */
public class DBTest {

	private static final String PATH_TO_TEST_DB = "target/h2db.mv.db";

	private static final String PATH_TO_EXAMPLE_DB = "src/test/resources/h2db/h2db.mv.db";

	private ConnectionManager connectionManager;

	protected EntityManager entityManager;

	public EntityManager getEntityManager() {
		return entityManager;
	}
	
	/** Connection data for the test db. */
	private static final ConnectionData connectionData = new ConnectionData() {
		public String getUsername() {
            return "ENVIRONMENT_BUILD";
        }

		public String getUrl() {
			return "jdbc:h2:./target/h2db";
        }
		
        public String getSchema() {
            return "envBuild";
        }
        public String getPassword() {
            return "ENVIRONMENT_BUILD";
        }

        public String getDriverClass() {
        	return "org.h2.Driver";
        }
        
        public String getAutodll() {
            return "validate";
        }
        public String getDialect() {
            return "org.hibernate.dialect.H2Dialect";
        }
	};
	
	/** Connection data for a local postgreSQL test db. */
	/*
	private static final ConnectionData localConnectionData = new ConnectionData() {
		public String getUsername() {
			return "postgres";
		}
		public String getUrl() {
			return "jdbc:postgresql://localhost:5432/ENVIRONMENT_BUILD";
		}
		public String getSchema() {
			return "envBuild";
		}
		public String getPassword() {
			return "**********";
		}
		public String getDriverClass() {
			return "org.postgresql.Driver";
		}
		public String getAutodll() {
			return "validate";
		}
		public String getDialect() {
			return "org.hibernate.dialect.PostgreSQLDialect";
		}
	};
	*/	
	
	@Before
	public void setUp() throws Exception {
		System.out.println("Opening up database connection");
		
		try {
			//First copy the example over to the target folder (we are going to work with the copied one)
            try {
				FileUtils.copyFile(new File(PATH_TO_EXAMPLE_DB), new File(PATH_TO_TEST_DB));
			} catch (Exception e) {
				//Sometimes this gets called before the connection is completely down.  Lets wait a few seconds and try again
				System.err.println("Failed to copy first time. Trying again");
				Thread.sleep(5000);
				FileUtils.copyFile(new File(PATH_TO_EXAMPLE_DB), new File(PATH_TO_TEST_DB));
			}
            
            connectionManager = new ConnectionManager();
			connectionManager.initialiseConnection(System.out, connectionData, "ebsa-environment-build-database-persistence-unit");
            //connectionManager.initialiseConnection(System.out, localConnectionData, "ebsa-environment-build-database-persistence-unit");
			
			entityManager = connectionManager.getManager();
		
			GlobalConfig.getInstance().setSharedConnectionData(connectionData);
			//GlobalConfig.getInstance().setSharedConnectionData(localConnectionData);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	@After
	public void disconnect() {
		if (entityManager != null) {
			if (entityManager.isOpen()) {
				entityManager.close();
			}
			// EntityManagerFactory must be closed otherwise the target/h2db.mv.db file remains locked on Windows so it can't be overwritten for the next test
			EntityManagerFactory factory = entityManager.getEntityManagerFactory();
			if (factory != null && factory.isOpen()) {
				factory.close();
			}
		}
	}
	
	protected EnvironmentContainer getEnvironmentContainer() {
		EnvironmentContainer environmentContainer = new EnvironmentContainer();
		environmentContainer.setName("env container " + UUID.randomUUID());
		environmentContainer.setProvider("provider");
		return environmentContainer;
	}

	protected EnvironmentContainerDefinition getEnvironmentContainerDefinition() {
		EnvironmentContainerDefinition environmentContainerDefinition = new EnvironmentContainerDefinition();
		environmentContainerDefinition.setName("env container def " + UUID.randomUUID());
		environmentContainerDefinition.setVersion("1.0");
		return environmentContainerDefinition;
	}
	
	protected Environment getEnvironment() {
		Environment environment = new Environment();
		environment.setName("env " + UUID.randomUUID());
		environment.setNotes("env notes #1");
		environment.setValidated(false);
		return environment;
	}

	protected EnvironmentDefinition getEnvironmentDefinition() {
		EnvironmentDefinition environmentDefinition = new EnvironmentDefinition();
		environmentDefinition.setName("envDef " + UUID.randomUUID());
		environmentDefinition.setVersion("1.0");
		environmentDefinition.setCidr("ed cidr");
		environmentDefinition.setDefinitionType(DefinitionType.Physical);
		return environmentDefinition;
	}

	protected VirtualMachineContainer getVirtualMachineContainer() {
		VirtualMachineContainer vmc = new VirtualMachineContainer();
		vmc.setName("vmc " + UUID.randomUUID());
		vmc.setDescription("vm description");
		vmc.setRuntimeLease("runtime lease");
		vmc.setStorageLease("storage lease");
		vmc.setServiceLevel("service level");
		vmc.setPowerOn(true);
		vmc.setDeploy(false);
		vmc.setDomain("domain");
		return vmc;
	}
	
	protected VirtualMachine getVirtualMachine() {
		VirtualMachine vm = new VirtualMachine();
		vm.setMemory(10);
		vm.setMemoryUnit("MB");
		vm.setCpuCount(2);
		vm.setDescription("vm description");
		vm.setVmName("vm " + UUID.randomUUID());
		vm.setComputerName("computer name " + vm.getVmName());
		vm.setTemplateName("template name");
		vm.setTemplateServiceLevel("template service level");
		vm.setStorageProfile("storage profile");
		vm.setCustomisationScript("cust script");
		vm.setHardwareProfile("hardware profile");
		vm.setHaType("ha type");
		return vm;
	}

	protected ApplicationNetwork getApplicationNetwork() {
		ApplicationNetwork network = new ApplicationNetwork();
		network.setName("network " + UUID.randomUUID());
		network.setDescription("app net desc");
		network.setFenceMode("fence");
		network.setNetworkMask("mask");
		network.setGatewayAddress("gateway addr");
		network.setPrimaryDns("prim dns");
		network.setSecondaryDns("sec dns");
		network.setDnsSuffix("dns suffix");
		network.setStaticIpPool("static ip pool");
		network.setIpRangeStart("ip start");
		network.setIpRangeEnd("ip end");
		network.setCidr("cidr");
		network.setShared(false);
		network.setDataCentreName("dataCentreName");
		return network;
	}
	
	protected OrganisationNetwork getOrganisationNetwork() {
		OrganisationNetwork network = new OrganisationNetwork();
		network.setName("network " + UUID.randomUUID());
		network.setDescription("org net desc");
		network.setFenceMode("fence");
		network.setNetworkMask("mask");
		network.setGatewayAddress("gateway addr");
		network.setPrimaryDns("prim dns");
		network.setSecondaryDns("sec dns");
		network.setDnsSuffix("dns suffix");
		network.setStaticIpPool("static ip pool");
		network.setIpRangeStart("ip start");
		network.setIpRangeEnd("ip end");
		network.setCidr("cidr");
		network.setShared(false);
		network.setPeerNetworkName("peer net name");
		network.setPeerEnvironmentName("peer env name");
		network.setDataCentreName("dataCentreName");
		return network;
	}
	
	protected Nic getNic() {
		Nic nic = new Nic();
		nic.setPrimaryNic(true);
		nic.setIndexNumber(0);
		nic.setIpAssignment("ip assign");
		return nic;
	}
	
	protected Interface getInterface() {
		Interface inter = new Interface();
		inter.setName("interface name " + UUID.randomUUID());
		inter.setNetworkMask("net mask");
		inter.setStaticIpAddress("static ip");
		inter.setStaticIpPool("ip pool");
		inter.setVip(false);
		inter.setVrrp(123);
		return inter;
	}
	
	protected Storage getStorage() {
		Storage storage = new Storage();
		storage.setIndexNumber(0);
		storage.setSize(100);
		storage.setSizeUnit("GB");
		storage.setBusType("bus type");
		storage.setBusSubType("bus sub type");
		storage.setDeviceMount("device mount");
		return storage;
	}
	
	protected EnvironmentDefinitionMetaData getEnvironmentDefinitionMetaData() {
		EnvironmentDefinitionMetaData metadata = new EnvironmentDefinitionMetaData();
		metadata.setName("name1");
		metadata.setValue("value1");
		return metadata;
	}
	
	protected ApplicationNetworkMetaData getApplicationNetworkMetaData() {
		ApplicationNetworkMetaData metadata = new ApplicationNetworkMetaData();
		metadata.setName("name2");
		metadata.setValue("value2");
		return metadata;
	}
	
	protected OrganisationNetworkMetaData getOrganisationNetworkMetaData() {
		OrganisationNetworkMetaData metadata = new OrganisationNetworkMetaData();
		metadata.setName("name2");
		metadata.setValue("value2");
		return metadata;
	}
	
	protected VirtualMachineMetaData getVirtualMachineMetaData() {
		VirtualMachineMetaData metadata = new VirtualMachineMetaData();
		metadata.setName("name3");
		metadata.setValue("value3");
		return metadata;
	}
	
	protected VirtualMachineContainerMetaData getVirtualMachineContainerMetaData() {
		VirtualMachineContainerMetaData metadata = new VirtualMachineContainerMetaData();
		metadata.setName("name4");
		metadata.setValue("value4");
		return metadata;
	}
	
//	protected OrganisationNetwork getOrganisationNetwork() {
//		OrganisationNetwork organisationNetwork = new OrganisationNetwork();
//		organisationNetwork.setName("org network " + UUID.randomUUID());
//		organisationNetwork.setFenceMode("fence");
//		organisationNetwork.setNetworkMask("mask");
//		organisationNetwork.setGatewayAddress("gateway addr");
//		organisationNetwork.setPrimaryDns("prim dns");
//		organisationNetwork.setSecondaryDns("sec dns");
//		organisationNetwork.setDnsSuffix("dns suffix");
//		organisationNetwork.setStaticIpPool("static ip pool");
//		organisationNetwork.setDescription("desc " + UUID.randomUUID());
//		organisationNetwork.setEdgeGateway("edge gateway");
//		organisationNetwork.setShared(true);
//		organisationNetwork.setVirtualDataCenter("vdc");
//		organisationNetwork.setOrgNetType("assign");
//		return organisationNetwork;
//	}
	
	protected Gateway getGateway() {
		Gateway gateway = new Gateway();
		gateway.setName("gateway " + UUID.randomUUID());
		//gateway.setVirtualDataCenter("vdc");
		return gateway;
	}
	
	protected Nat getNat() {
		Nat nat = new Nat();
		nat.setAppliedOn("applied on");
		nat.setEnabled(true);
		nat.setOriginalSourceIpOrRange("orig");
		nat.setTranslatedSourceIpOrRange("trans");
		return nat;
	}
	
	protected DNat getDNat() {
		DNat dnat = new DNat();
		dnat.setAppliedOn("applied on");
		dnat.setEnabled(true);
		dnat.setOriginalSourceIpOrRange("orig");
		dnat.setTranslatedSourceIpOrRange("trans");
		dnat.setProtocolIcmpType("proto icmp");
		dnat.setProtocolOriginalPort("proto orig port");
		dnat.setProtocolType("proto type");
		dnat.setTranslatedPort("trans port");
		return dnat;
	}
	
	protected DataCentre getDataCentre() {
		DataCentre dataCentre = new DataCentre();
		dataCentre.setName("datacentre " + UUID.randomUUID());
		return dataCentre;
	}
	
	protected EnvironmentContainerBuild getEnvironmentContainerBuild() {
		EnvironmentContainerBuild ecb = new EnvironmentContainerBuild();
		ecb.setDateStarted(new Date());
		ecb.setJenkinsBuildId("jenkinsbuildid");
		ecb.setJenkinsBuildNumber(1);
		ecb.setJenkinsJobName("jenkinsjobname");
		return ecb;
	}
	
	protected void verify(DBEntity... expected) {
		for (int i = 0; i < expected.length; i++) {
			DBEntity actual = getEntityManager().find(expected[i].getClass(), expected[i].getId());
			ReflectionAssert.assertReflectionEquals(expected[i].getClass().getName() + " mismatch", expected[i], actual, ReflectionComparatorMode.IGNORE_DEFAULTS);
		}
	}
}
