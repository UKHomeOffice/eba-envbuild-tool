package com.ipt.ebsa.environment.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.ipt.ebsa.environment.build.entities.ApplicationNetwork;
import com.ipt.ebsa.environment.build.entities.ApplicationNetworkMetaData;
import com.ipt.ebsa.environment.build.entities.DNat;
import com.ipt.ebsa.environment.build.entities.DataCentre;
import com.ipt.ebsa.environment.build.entities.Environment;
import com.ipt.ebsa.environment.build.entities.EnvironmentBuild;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinitionMetaData;
import com.ipt.ebsa.environment.build.entities.Gateway;
import com.ipt.ebsa.environment.build.entities.Interface;
import com.ipt.ebsa.environment.build.entities.Nat;
import com.ipt.ebsa.environment.build.entities.Nic;
import com.ipt.ebsa.environment.build.entities.OrganisationNetwork;
import com.ipt.ebsa.environment.build.entities.Storage;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainerMetaData;
import com.ipt.ebsa.environment.build.entities.VirtualMachineMetaData;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.environment.build.manager.UpdateManager;

/**
 * Unit test for all the entities
 *
 */
public class AllEntityTest extends DBTest {
	
	@Test
	public void testGetEnvironmentBuild() throws Exception {

		Calendar created = Calendar.getInstance();
		created.set(1976, 3, 25, 12, 1, 2);
		created.set(Calendar.MILLISECOND, 0);
		Calendar completed = Calendar.getInstance();
		completed.set(1977, 2, 8, 18, 3, 4);
		completed.set(Calendar.MILLISECOND, 0);
		
		EnvironmentDefinition envDef = getEnvironmentDefinition();
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		em.persist(envDef);
		em.getTransaction().commit();
		
		EnvironmentBuild eb = new EnvironmentBuild();
		eb.setEnvironmentDefinition(envDef);
		eb.setDateStarted(created.getTime());
		eb.setJenkinsBuildId("MyBuildID");
		eb.setJenkinsBuildNumber(666);
		eb.setJenkinsJobName("MyJenkinsJob");
		eb.setPlan(StringUtils.repeat("plan", 1024));
		eb.setEnvXml(StringUtils.repeat("envXML", 1945));
		eb.setSucceeded(null);
		
		// create
		UpdateManager up = new UpdateManager();
		Integer id = up.saveEnvironmentBuild(eb).getId();
		assertNotNull(id);
		
		// fetch
		ReadManager rm = new ReadManager();
		EnvironmentBuild eb2 = rm.getEnvironmentBuild(id);
		assertEquals(eb.getEnvironmentDefinition().getId(), eb2.getEnvironmentDefinition().getId());
		assertEquals(eb.getDateStarted(), eb2.getDateStarted());
		assertNull(eb2.getDateCompleted());
		assertEquals(eb.getJenkinsBuildId(), eb2.getJenkinsBuildId());
		assertEquals(eb.getJenkinsBuildNumber(), eb2.getJenkinsBuildNumber());
		assertEquals(eb.getJenkinsJobName(), eb2.getJenkinsJobName());
		assertNull(eb2.getLog());
		assertEquals(eb.getPlan(), eb2.getPlan());
		assertEquals(eb.getEnvXml(), eb2.getEnvXml());
		assertNull(eb2.getReport());
		assertNull(eb2.getSucceeded());
		
		// update
		eb2.setLog(StringUtils.repeat("log", 123));
		eb2.setReport(StringUtils.repeat("report", 2048));
		eb2.setSucceeded(true);
		eb2.setDateCompleted(completed.getTime());
		up.updateEnvironmentBuild(eb2);
		
		// check
		EnvironmentBuild eb3 = rm.getEnvironmentBuild(id);
		assertEquals(eb.getEnvironmentDefinition().getId(), eb3.getEnvironmentDefinition().getId());
		assertEquals(eb.getDateStarted(), eb3.getDateStarted());
		assertEquals(completed.getTime(), eb3.getDateCompleted());
		assertEquals(eb.getJenkinsBuildId(), eb3.getJenkinsBuildId());
		assertEquals(eb.getJenkinsBuildNumber(), eb3.getJenkinsBuildNumber());
		assertEquals(eb.getJenkinsJobName(), eb3.getJenkinsJobName());
		assertEquals(eb2.getLog(), eb3.getLog());
		assertEquals(eb.getPlan(), eb3.getPlan());
		assertEquals(eb2.getReport(), eb3.getReport());
		assertTrue(eb3.getSucceeded());
	}
	
	@Test
	public void test() {
		doTest(getEntityManager());
	}

	private void doTest(EntityManager manager) {
		
		EnvironmentContainer environmentContainer = getEnvironmentContainer();
		EnvironmentContainerDefinition environmentContainerDefinition = getEnvironmentContainerDefinition();
		environmentContainer.addEnvironmentcontainerdefinition(environmentContainerDefinition);
		Environment environment = getEnvironment();
		environmentContainer.addEnvironment(environment);
		EnvironmentDefinition environmentDefinition = getEnvironmentDefinition();
		EnvironmentDefinitionMetaData envDefMeta = getEnvironmentDefinitionMetaData();
		environmentDefinition.addMetadata(envDefMeta);
		environment.addEnvironmentdefinition(environmentDefinition);
		
		DataCentre datacentre = getDataCentre();
		
		VirtualMachineContainer vmContainer = getVirtualMachineContainer();
		VirtualMachineContainerMetaData vmcMetadata = getVirtualMachineContainerMetaData();
		vmContainer.addMetadata(vmcMetadata);
		VirtualMachine vm = getVirtualMachine();
		Storage storage = getStorage();
		vm.addStorage(storage);
		VirtualMachineMetaData vmMetadata = getVirtualMachineMetaData();
		vm.addMetadata(vmMetadata);
		Nic nic = getNic();
		Interface interface1 = getInterface();
		nic.addInterface(interface1);
		Interface interface2 = getInterface();
		interface2.setInterfaceNumber(1);
		nic.addInterface(interface2);
		vm.addNic(nic);
		vmContainer.addVirtualmachine(vm);
		ApplicationNetwork network = getApplicationNetwork();
		ApplicationNetworkMetaData networkMetadata = getApplicationNetworkMetaData();
		network.addMetadata(networkMetadata);
		vmContainer.addNetwork(network);
		vmContainer.setDataCentreName(datacentre.getName());
		environmentDefinition.addVirtualmachinecontainer(vmContainer);
		
		OrganisationNetwork orgNetwork = getOrganisationNetwork();
		orgNetwork.setDataCentre(datacentre);
		Gateway gateway = getGateway();
		gateway.addNetwork(orgNetwork);
		environmentContainerDefinition.addNetwork(orgNetwork);

		Nat nat = getNat();
		DNat dnat = getDNat();
		gateway.addNat(nat);
		gateway.addNat(dnat);
		
		manager.getTransaction().begin();
		
		manager.persist(environmentContainer);
		manager.persist(environmentContainerDefinition);
		manager.persist(environment);
		manager.persist(environmentDefinition);
		
		manager.persist(vmContainer);
		manager.persist(vm);
		manager.persist(network);
		manager.persist(nic);
		manager.persist(interface1);
		manager.persist(interface2);
		manager.persist(storage);
		manager.persist(envDefMeta);
		manager.persist(vmcMetadata);
		manager.persist(vmMetadata);
		manager.persist(networkMetadata);
		manager.persist(datacentre);
		
		manager.persist(gateway);
		manager.persist(orgNetwork);
		manager.persist(nat);
		manager.persist(dnat);
		
		manager.flush();
		manager.getTransaction().commit();
		
		manager.clear();
	
		// Transaction needed to read the Environment.notes LOB in PostgreSQL 
		manager.getTransaction().begin();
		verify(
				environmentContainer,
				environmentContainerDefinition,
				environment,
				environmentDefinition,
				vmContainer,
				vm,
				network,
				nic,
				interface1,
				interface2,
				storage,
				envDefMeta,
				vmcMetadata,
				vmMetadata,
				networkMetadata,
				datacentre,
				orgNetwork,
				gateway,
				nat,
				dnat
		);
		manager.getTransaction().rollback();
	}


	/*
	public static void main(String[] args) {
		Map<String, String> properties = new HashMap<String, String>();     
		properties.put("connection.driver_class", "org.postgresql.Driver");
		properties.put("hibernate.connection.url", "jdbc:postgresql://localhost:5432/ENVIRONMENT_BUILD");
		properties.put("hibernate.connection.password", "******************");
		properties.put("hibernate.default_schema", "envBuild");
		properties.put("hibernate.connection.username", "postgres");
		properties.put("hibernate.hbm2ddl.auto", "validate");
		properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
				
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("ebsa-environment-build-database-persistence-unit", properties);
		
		EntityManager manager = emf.createEntityManager();
		
		AllEntityTest test = new AllEntityTest();
		test.entityManager = manager;
		test.doTest(manager);
		System.out.println("Done");
	}
	*/

}
