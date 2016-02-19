package com.ipt.ebsa.environment.build.manager;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import com.ipt.ebsa.environment.build.DBTest;
import com.ipt.ebsa.environment.build.entities.Environment;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerBuild;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition.DefinitionType;
import com.ipt.ebsa.environment.build.entities.HardwareProfile;

/**
 * Unit tests for ReadManager
 *
 */
public class ReadManagerTest extends DBTest {
	
	@Test
	public void testGetEnvironmentContainerForEnvironmentName() throws Exception {
		Environment env = getEnvironment();
		String environmentName = env.getName();
		EnvironmentContainer envContainer = getEnvironmentContainer();
		String expectedContainerName = envContainer.getName();
		String expectedContainerProvider = envContainer.getProvider();
		envContainer.addEnvironment(env);
		getEntityManager().getTransaction().begin();
		getEntityManager().persist(envContainer);
		getEntityManager().persist(env);
		getEntityManager().flush();
		getEntityManager().getTransaction().commit();
		getEntityManager().close();
		EnvironmentContainer actualEnvContainer = new ReadManager().getEnvironmentContainerForEnvironmentName(environmentName, "provider");
		Assert.assertNotNull(actualEnvContainer);
		Assert.assertEquals(expectedContainerName, actualEnvContainer.getName());
		Assert.assertEquals(expectedContainerProvider, actualEnvContainer.getProvider());
	}
	
	@Test
	public void testGetEnvironmentContainerForContainerName() throws Exception {
		EnvironmentContainer envContainer = getEnvironmentContainer();
		String expectedContainerName = envContainer.getName();
		String expectedContainerProvider = envContainer.getProvider();
		getEntityManager().getTransaction().begin();
		getEntityManager().persist(envContainer);
		getEntityManager().flush();
		getEntityManager().getTransaction().commit();
		getEntityManager().close();
		EnvironmentContainer actualEnvContainer = new ReadManager().getEnvironmentContainerForContainerName(expectedContainerName, "provider");
		Assert.assertNotNull(actualEnvContainer);
		Assert.assertEquals(expectedContainerName, actualEnvContainer.getName());
		Assert.assertEquals(expectedContainerProvider, actualEnvContainer.getProvider());
	}
	
	@Test
	public void testGetEnvironment() throws Exception {
		EnvironmentContainer envContainer = getEnvironmentContainer();
		Environment expectedEnv = getEnvironment();
		envContainer.addEnvironment(expectedEnv);
		String environmentName = expectedEnv.getName();
		getEntityManager().getTransaction().begin();
		getEntityManager().persist(envContainer);
		getEntityManager().persist(expectedEnv);
		getEntityManager().flush();
		getEntityManager().getTransaction().commit();
		getEntityManager().close();
		Environment actualEnv = new ReadManager().getEnvironment(environmentName, "provider");
		Assert.assertNotNull(actualEnv);
		Assert.assertEquals(expectedEnv.getName(), actualEnv.getName());
		Assert.assertEquals(expectedEnv.getNotes(), actualEnv.getNotes());
	}
	
	@Test
	public void testGetEnvironmentDefinition() throws Exception {
		EnvironmentContainer envContainer = getEnvironmentContainer();
		Environment env = getEnvironment();
		envContainer.addEnvironment(env);
		String environmentName = env.getName();
		EnvironmentDefinition expectedEnvDef = getEnvironmentDefinition();
		env.addEnvironmentdefinition(expectedEnvDef);
		String version = expectedEnvDef.getVersion();
		getEntityManager().getTransaction().begin();
		getEntityManager().persist(envContainer);
		getEntityManager().persist(env);
		getEntityManager().persist(expectedEnvDef);
		getEntityManager().flush();
		getEntityManager().getTransaction().commit();
		getEntityManager().close();
		// Disconnect Environment since only interested in comparing the child object graphs
		expectedEnvDef.setEnvironment(null);
		EnvironmentDefinition actualEnvDef = new ReadManager().getEnvironmentDefinition(environmentName, version, DefinitionType.Physical, "provider");
		Assert.assertNotNull(actualEnvDef);
		ReflectionAssert.assertReflectionEquals(expectedEnvDef, actualEnvDef, ReflectionComparatorMode.IGNORE_DEFAULTS);
	}
	
	@Test
	public void testGetEnvironmentDefinitionsMap() throws Exception {
		EnvironmentContainer envContainer = getEnvironmentContainer();
		Environment env = getEnvironment();
		envContainer.addEnvironment(env);
		String environmentName = env.getName();
		EnvironmentDefinition expectedEnvDef1 = getEnvironmentDefinition();
		env.addEnvironmentdefinition(expectedEnvDef1);
		String expectedVersion1 = expectedEnvDef1.getVersion();
		EnvironmentDefinition expectedEnvDef2 = getEnvironmentDefinition();
		String expectedVersion2 = "2.0";
		expectedEnvDef2.setVersion(expectedVersion2);
		env.addEnvironmentdefinition(expectedEnvDef2);
		List<EnvironmentDefinition> expectedEnvDefs = env.getEnvironmentdefinitions();
		getEntityManager().getTransaction().begin();
		getEntityManager().persist(envContainer);
		getEntityManager().persist(env);
		getEntityManager().persist(expectedEnvDef1);
		getEntityManager().persist(expectedEnvDef2);
		getEntityManager().flush();
		getEntityManager().getTransaction().commit();
		getEntityManager().close();
		Map<String, List<EnvironmentDefinition>> actualEnvDefsMap = new ReadManager().getEnvironmentDefinitions(Arrays.asList(environmentName), DefinitionType.Physical, "provider");
		List<EnvironmentDefinition> actualEnvDefs = actualEnvDefsMap.get(environmentName);
		Assert.assertEquals(expectedEnvDefs.size(), actualEnvDefs.size());
		// Ordered by descending version
		Assert.assertEquals(expectedVersion2, actualEnvDefs.get(0).getVersion());
		Assert.assertEquals(expectedVersion1, actualEnvDefs.get(1).getVersion());
	}
	
	@Test
	public void testGetEnvironmentContainerDefinition() throws Exception {
		EnvironmentContainer envContainer = getEnvironmentContainer();
		String envContainerName = envContainer.getName();
		EnvironmentContainerDefinition expectedEnvConDef = getEnvironmentContainerDefinition();
		String version = "1.0";
		expectedEnvConDef.setVersion(version);
		expectedEnvConDef.setEnvironmentcontainer(envContainer);
		entityManager.getTransaction().begin();
		entityManager.persist(envContainer);
		entityManager.persist(expectedEnvConDef);
		entityManager.getTransaction().commit();
		EnvironmentContainerDefinition actualEnvConDef = new ReadManager().getEnvironmentContainerDefinition(envContainerName, version, "provider");
		Assert.assertNotNull(actualEnvConDef);
		Assert.assertEquals(expectedEnvConDef.getVersion(), actualEnvConDef.getVersion());
	}
	
	@Test
	public void testGetEnvironmentContainerDefinitionsMap() throws Exception {
		EnvironmentContainer envContainer = getEnvironmentContainer();
		EnvironmentContainerDefinition definition = getEnvironmentContainerDefinition();
		definition.setVersion("1.2.5");
		EnvironmentContainerDefinition definition2 = getEnvironmentContainerDefinition();
		definition2.setVersion("1.3.8");
		definition.setEnvironmentcontainer(envContainer);
		definition2.setEnvironmentcontainer(envContainer);
		entityManager.getTransaction().begin();
		entityManager.persist(envContainer);
		entityManager.persist(definition);
		entityManager.persist(definition2);
		entityManager.getTransaction().commit();
		
		Map<String, List<EnvironmentContainerDefinition>> environmentContainerDefinitionsMap = new ReadManager().getEnvironmentContainerDefinitions(Arrays.asList(envContainer.getName()), "provider");
		List<EnvironmentContainerDefinition> environmentContainerDefinitions = environmentContainerDefinitionsMap.get(envContainer.getName());
		assertEquals("Number of definitions", 2, environmentContainerDefinitions.size());
		assertEquals("1.3.8", environmentContainerDefinitions.get(0).getVersion());
		assertEquals("1.2.5", environmentContainerDefinitions.get(1).getVersion());
	}
	
	@Test
	public void testGetHardwareProfile() throws Exception {
		ReadManager readManager = new ReadManager();
		HardwareProfile profile = new HardwareProfile();
		profile.setCpuCount(1);
		profile.setMemory(2);
		profile.setInterfaceCount(3);
		profile.setVmRole("Doesn'tExist");
		profile.setProvider("AWS");

		Assert.assertEquals("t2.medium", readManager.getHardwareProfile(profile));
		
		profile.setMemory(4);
		Assert.assertNull(readManager.getHardwareProfile(profile));
		
		profile.setCpuCount(2);
		Assert.assertEquals("c3.large", readManager.getHardwareProfile(profile));
		
		profile.setInterfaceCount(4);
		Assert.assertEquals("c3.xlarge", readManager.getHardwareProfile(profile));
		
		profile.setInterfaceCount(6);
		Assert.assertEquals("c3.4xlarge", readManager.getHardwareProfile(profile));
		
		profile.setMemory(8);
		Assert.assertNull(readManager.getHardwareProfile(profile));
		
		profile.setVmRole("DBS");
		Assert.assertNull(readManager.getHardwareProfile(profile));
		
		profile.setInterfaceCount(3);
		Assert.assertEquals("m3.large", readManager.getHardwareProfile(profile));
		
		profile.setCpuCount(4);
		Assert.assertEquals("c3.xlarge", readManager.getHardwareProfile(profile));
		
		profile.setVmRole("Default");
		Assert.assertEquals("c3.xlarge", readManager.getHardwareProfile(profile));
		
		profile.setMemory(24);
		Assert.assertEquals("r3.xlarge", readManager.getHardwareProfile(profile));
		
		profile.setCpuCount(6);
		profile.setInterfaceCount(2);
		Assert.assertNull(readManager.getHardwareProfile(profile));
		
		profile.setCpuCount(8);
		Assert.assertEquals("m3.2xlarge", readManager.getHardwareProfile(profile));
		
		profile.setProvider("SKYSCAPE");
		Assert.assertNull(readManager.getHardwareProfile(profile));
	}
	
	@Test
	public void testGetEnvironments() throws Exception {
		getEntityManager().getTransaction().begin();
		getEntityManager().createNativeQuery("insert into envBuild.environment (name, notes, validated, environmentContainerId) values ('ENV 1', 'Some notes 1', true, 1)").executeUpdate();
		getEntityManager().createNativeQuery("insert into envBuild.environment (name, notes, validated, environmentContainerId) values ('ENV 2', 'Some notes 2', true, 2);").executeUpdate();
		getEntityManager().createNativeQuery("insert into envBuild.environment (name, notes, validated, environmentContainerId) values ('ENV 3', 'Some notes 3', true, 3);").executeUpdate();
		getEntityManager().getTransaction().commit();
		
		ReadManager readManager = new ReadManager();
		List<Environment> envs = readManager.getEnvironments();
		Assert.assertEquals(3, envs.size());
	}
	
	@Test
	public void testGetCurrentEnvironmentContainerDefinition_NothingDeployed() throws Exception {
		ReadManager readManager = new ReadManager();
		EnvironmentContainerDefinition envConDef = readManager.getEnvironmentContainerDefinitonCurrentlyDeployed("npa", "AWS");
		Assert.assertNull(envConDef);
	}
	
	@Test
	public void testGetCurrentEnvironmentContainerDefinition_DeployFailed() throws Exception {
		// Create EnvironmentContainer
		EnvironmentContainer ec = getEnvironmentContainer();
		// Create EnvironmentContainerDefinition
		EnvironmentContainerDefinition ecd = getEnvironmentContainerDefinition();
		ec.addEnvironmentcontainerdefinition(ecd);
		// Create EnvironmentContainerBuild
		EnvironmentContainerBuild ecb = getEnvironmentContainerBuild();
		ecb.setDateCompleted(new Date());
		ecb.setSucceeded(false);
		ecd.addEnvironmentContainerBuild(ecb);
		
		entityManager.getTransaction().begin();
		entityManager.persist(ec);
		entityManager.persist(ecd);
		entityManager.persist(ecb);
		entityManager.getTransaction().commit();
		
		ReadManager readManager = new ReadManager();
		EnvironmentContainerDefinition envConDef = readManager.getEnvironmentContainerDefinitonCurrentlyDeployed(ec.getName(), ec.getProvider());
		Assert.assertNull(envConDef);
	}
	
	@Test
	public void testGetCurrentEnvironmentContainerDefinition_DeployIncomplete() throws Exception {
		// Create EnvironmentContainer
		EnvironmentContainer ec = getEnvironmentContainer();
		// Create EnvironmentContainerDefinition
		EnvironmentContainerDefinition ecd = getEnvironmentContainerDefinition();
		ec.addEnvironmentcontainerdefinition(ecd);
		// Create EnvironmentContainerBuild
		EnvironmentContainerBuild ecb = getEnvironmentContainerBuild();
		ecd.addEnvironmentContainerBuild(ecb);
		
		entityManager.getTransaction().begin();
		entityManager.persist(ec);
		entityManager.persist(ecd);
		entityManager.persist(ecb);
		entityManager.getTransaction().commit();
		
		ReadManager readManager = new ReadManager();
		EnvironmentContainerDefinition envConDef = readManager.getEnvironmentContainerDefinitonCurrentlyDeployed(ec.getName(), ec.getProvider());
		Assert.assertNull(envConDef);
	}
	
	@Test
	public void testGetCurrentEnvironmentContainerDefinition_DeployComplete() throws Exception {
		// Create EnvironmentContainer
		EnvironmentContainer ec = getEnvironmentContainer();
		// Create EnvironmentContainerDefinition
		EnvironmentContainerDefinition ecd = getEnvironmentContainerDefinition();
		ec.addEnvironmentcontainerdefinition(ecd);
		// Create EnvironmentContainerBuild
		EnvironmentContainerBuild ecb = getEnvironmentContainerBuild();
		ecb.setDateCompleted(new Date());
		ecb.setSucceeded(true);
		ecd.addEnvironmentContainerBuild(ecb);
		
		entityManager.getTransaction().begin();
		entityManager.persist(ec);
		entityManager.persist(ecd);
		entityManager.persist(ecb);
		entityManager.getTransaction().commit();
		
		ReadManager readManager = new ReadManager();
		EnvironmentContainerDefinition envConDef = readManager.getEnvironmentContainerDefinitonCurrentlyDeployed(ec.getName(), ec.getProvider());
		Assert.assertNotNull(envConDef);
		Assert.assertEquals(ecd.getId(), envConDef.getId());
	}
	
	@Test
	public void testGetCurrentEnvironmentContainerDefinition_MultipleDeploysComplete() throws Exception {
		// Create EnvironmentContainer
		EnvironmentContainer ec = getEnvironmentContainer();
		
		// Create EnvironmentContainerDefinitions
		EnvironmentContainerDefinition ecd1 = getEnvironmentContainerDefinition();
		ecd1.setVersion("1.0");
		ec.addEnvironmentcontainerdefinition(ecd1);
		EnvironmentContainerDefinition ecd2 = getEnvironmentContainerDefinition();
		ecd2.setVersion("2.0");
		ec.addEnvironmentcontainerdefinition(ecd2);
		
		// Create EnvironmentContainerBuilds: ecb1_2 is the most recent successfully completed build and it's attached to ecd1
		Long completed = System.currentTimeMillis();
		EnvironmentContainerBuild ecb1_1 = getEnvironmentContainerBuild();
		ecb1_1.setDateCompleted(new Date(completed - 30));
		ecb1_1.setSucceeded(true);
		ecd1.addEnvironmentContainerBuild(ecb1_1);
		EnvironmentContainerBuild ecb1_2 = getEnvironmentContainerBuild();
		ecb1_2.setDateCompleted(new Date(completed - 20));
		ecb1_2.setSucceeded(true);
		ecd1.addEnvironmentContainerBuild(ecb1_2);
		EnvironmentContainerBuild ecb2_1 = getEnvironmentContainerBuild();
		ecb2_1.setDateCompleted(new Date(completed - 50));
		ecb2_1.setSucceeded(true);
		ecd2.addEnvironmentContainerBuild(ecb2_1);
		EnvironmentContainerBuild ecb2_2 = getEnvironmentContainerBuild();
		ecb2_2.setDateCompleted(new Date(completed - 10));
		ecb2_2.setSucceeded(false);
		ecd2.addEnvironmentContainerBuild(ecb2_2);
		
		entityManager.getTransaction().begin();
		entityManager.persist(ec);
		entityManager.persist(ecd1);
		entityManager.persist(ecd2);
		entityManager.persist(ecb1_1);
		entityManager.persist(ecb1_2);
		entityManager.persist(ecb2_1);
		entityManager.getTransaction().commit();
		
		ReadManager readManager = new ReadManager();
		EnvironmentContainerDefinition envConDef = readManager.getEnvironmentContainerDefinitonCurrentlyDeployed(ec.getName(), ec.getProvider());
		Assert.assertNotNull(envConDef);
		Assert.assertEquals(ecd1.getId(), envConDef.getId());
	}
}
