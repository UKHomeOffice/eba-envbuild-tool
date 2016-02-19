package com.ipt.ebsa.environment.build.manager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.hibernate.Hibernate;

import com.ipt.ebsa.database.manager.ConnectionManager;
import com.ipt.ebsa.database.manager.CrudService;
import com.ipt.ebsa.database.manager.CrudServiceImpl;
import com.ipt.ebsa.database.manager.ParamFactory;
import com.ipt.ebsa.environment.build.entities.ApplicationNetwork;
import com.ipt.ebsa.environment.build.entities.ApplicationNetworkMetaData;
import com.ipt.ebsa.environment.build.entities.DataCentre;
import com.ipt.ebsa.environment.build.entities.Environment;
import com.ipt.ebsa.environment.build.entities.EnvironmentBuild;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition.DefinitionType;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinitionMetaData;
import com.ipt.ebsa.environment.build.entities.Gateway;
import com.ipt.ebsa.environment.build.entities.HardwareProfile;
import com.ipt.ebsa.environment.build.entities.Interface;
import com.ipt.ebsa.environment.build.entities.Nic;
import com.ipt.ebsa.environment.build.entities.OrganisationNetwork;
import com.ipt.ebsa.environment.build.entities.Storage;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainerMetaData;
import com.ipt.ebsa.environment.build.entities.VirtualMachineMetaData;

/**
 * This class contains methods for reading ENVIRONMENT_BUILD database entities
 *
 */
public class ReadManager extends DatabaseManager {
	
	/**
	 * Returns the Environment Definition of the given type for the environment with the given name and version, and provider
	 * @param environmentName
	 * @param version
	 * @param type
	 * @param provider
	 * @return
	 * @throws Exception
	 */
	public EnvironmentDefinition getEnvironmentDefinition(final String environmentName, final String version, final DefinitionType type, final String provider) throws Exception {
		return doInConnection(new Executor<EnvironmentDefinition>(){

			@Override
			public EnvironmentDefinition work(EntityManager manager) {
				EnvironmentDefinition environmentDefinition = null;
				// Find EnvironmentDefinition from environmentName, version, and type
				CrudService<EnvironmentDefinition> envDefService = new CrudServiceImpl<EnvironmentDefinition>(manager);
				environmentDefinition = envDefService.findOnlyResultWithNamedQuery(EnvironmentDefinition.class, "EnvironmentDefinition.findByEnvironmentNameTypeVersionAndProvider",
						ParamFactory.with("environmentName", environmentName).and("version", version).and("type", type.toString()).and("provider", provider).parameters());
				
				if (environmentDefinition != null) {
					// Force lazy initialisation
					for (VirtualMachineContainer vmc : environmentDefinition.getVirtualmachinecontainers()) {
						Hibernate.initialize(vmc);
						for (VirtualMachine vm : vmc.getVirtualmachines()) {
							Hibernate.initialize(vm);
							for (Storage storage : vm.getStorages()) {
								Hibernate.initialize(storage);
							}
							for (VirtualMachineMetaData metadata : vm.getMetadata()) {
								Hibernate.initialize(metadata);
							}
							for (Nic nic : vm.getNics()) {
								Hibernate.initialize(nic);
								for (Interface anInterface : nic.getInterfaces()) {
									Hibernate.initialize(anInterface);
								}
							}
						}
						for (ApplicationNetwork network : vmc.getNetworks()) {
							Hibernate.initialize(network);
							for (ApplicationNetworkMetaData metadata : network.getMetadata()) {
								Hibernate.initialize(metadata);
							}
						}
						for (VirtualMachineContainerMetaData metadata : vmc.getMetadata()) {
							Hibernate.initialize(metadata);
						}
					}
					for (EnvironmentDefinitionMetaData metadata : environmentDefinition.getMetadata()) {
						Hibernate.initialize(metadata);
					}
					// Initialise the GeographicContainer
					Environment environment = environmentDefinition.getEnvironment();
					Hibernate.initialize(environment.getEnvironmentcontainer().getGeographiccontainers());
					// Ensure the Environment Container only contains the Environment for this Environment Definition
					environment.getEnvironmentcontainer().setEnvironments(Collections.singletonList(environmentDefinition.getEnvironment()));
					// Ensure the Environment only contains this Environment Definition
					environment.setEnvironmentdefinitions(Collections.singletonList(environmentDefinition));
				}
				return environmentDefinition;
			}
		});
	}
	
	private List<EnvironmentDefinition> getEnvironmentDefinition(final String environmentName, final DefinitionType definitionType, final String provider,
			EntityManager manager) {
		List<EnvironmentDefinition> environmentDefinitions = Collections.emptyList();
		manager.getTransaction().begin();
		CrudService<EnvironmentDefinition> envDefService = new CrudServiceImpl<EnvironmentDefinition>(manager);
		environmentDefinitions = envDefService.findWithNamedQuery(EnvironmentDefinition.class, "EnvironmentDefinition.findByEnvironmentNameAndTypeAndProvider", 
				ParamFactory.with("environmentName", environmentName).and("type", definitionType.toString()).and("provider", provider).parameters());
		manager.getTransaction().commit();
		return environmentDefinitions;
	}
	
	/**
	 * 
	 * @param environments
	 * @param definitionType
	 * @param provider
	 * @return Map where key is the environment, value is all the environment definitions for that environment with the given provider.
	 * @throws Exception
	 */
	public Map<String, List<EnvironmentDefinition>> getEnvironmentDefinitions(final List<String> environments, final DefinitionType definitionType, final String provider) throws Exception {
		return doInConnection(new Executor<Map<String, List<EnvironmentDefinition>>>(){

			@Override
			public Map<String, List<EnvironmentDefinition>> work(EntityManager manager) {
				TreeMap<String, List<EnvironmentDefinition>> output = new TreeMap<>();
				for (String environmentName : environments) {
					output.put(environmentName, getEnvironmentDefinition(environmentName, definitionType, provider, manager));
				}
				return output;
			}
		});
	}
	
	public List<Environment> getEnvironments() throws Exception {
		return doInConnection(new Executor<List<Environment>>(){

			@Override
			public List<Environment> work(EntityManager manager) {
				CrudService<Environment> envService = new CrudServiceImpl<>(manager);
				return envService.findWithNamedQuery(Environment.class, "Environment.findAll");
			}
			
		});
	}
	
	/**
	 * Returns the Environment Container Definitions for the Environment Containers with the given names and provider
	 * @param environmentContainerNames
	 * @param provider
	 * @return
	 * @throws Exception
	 */
	public Map<String, List<EnvironmentContainerDefinition>> getEnvironmentContainerDefinitions(final List<String> environmentContainerNames, final String provider) throws Exception {
		return doInConnection(new Executor<Map<String, List<EnvironmentContainerDefinition>>>(){

			@Override
			public Map<String, List<EnvironmentContainerDefinition>> work(EntityManager manager) {
				TreeMap<String, List<EnvironmentContainerDefinition>> output = new TreeMap<>();
				for (String environmentContainerName : environmentContainerNames) {
					output.put(environmentContainerName, getEnvironmentContainerDefinitions(environmentContainerName, provider, manager));
				}
				return output;
			}

		});
	}
	
	private List<EnvironmentContainerDefinition> getEnvironmentContainerDefinitions(final String environmentContainerName, String provider, EntityManager manager) {
		List<EnvironmentContainerDefinition> ecds = Collections.emptyList();
		// Find Environment Container Definitions for the environmentContainerName and provider
		CrudService<EnvironmentContainerDefinition> ecdService = new CrudServiceImpl<EnvironmentContainerDefinition>(manager);
		ecds = ecdService.findWithNamedQuery(EnvironmentContainerDefinition.class, "EnvironmentContainerDefinition.findForEnvironmentContainerNameAndProvider", 
				ParamFactory.with("environmentContainerName", environmentContainerName).and("provider", provider).parameters());
		return ecds;
	}

	/**
	 * Returns the Environment Container for the given provider and environment name
	 * @param environmentName
	 * @param provider
	 * @return
	 * @throws Exception
	 */
	public EnvironmentContainer getEnvironmentContainerForEnvironmentName(final String environmentName, final String provider) throws Exception {
		return doInConnection(new Executor<EnvironmentContainer>(){

			@Override
			public EnvironmentContainer work(EntityManager manager) {
				// Find Environment with the environmentName
				EnvironmentContainer environmentContainer = null;
				CrudService<Environment> envService = new CrudServiceImpl<Environment>(manager);
				Environment environment = envService.findOnlyResultWithNamedQuery(Environment.class, "Environment.findByNameAndProvider", 
						ParamFactory.with("name", environmentName).and("provider", provider).parameters());
				if (environment != null) {
					// Get Environment Container for Environment
					environmentContainer = environment.getEnvironmentcontainer();
				}
				return environmentContainer;
			}	
		});
	}
	
	/**
	 * Returns the Environment Container for the given environment container name and provider
	 * @param environmentName
	 * @param provider
	 * @return
	 * @throws Exception
	 */
	public EnvironmentContainer getEnvironmentContainerForContainerName(final String environmentContainerName, final String provider) throws Exception {
		return doInConnection(new Executor<EnvironmentContainer>(){

			@Override
			public EnvironmentContainer work(EntityManager manager) {
				CrudService<EnvironmentContainer> envConService = new CrudServiceImpl<EnvironmentContainer>(manager);
				return envConService.findOnlyResultWithNamedQuery(EnvironmentContainer.class, "EnvironmentContainer.findByNameAndProvider", 
						ParamFactory.with("name", environmentContainerName).and("provider", provider).parameters());
			}	
		});
	}
	
	/**
	 * Returns the currently deployed Environment Container Definition for the given environmentContainerName and provider
	 * @param environmentContainerName the name of the container
	 * @param provider the cloud provider
	 * @return
	 * @throws Exception
	 */
	public EnvironmentContainerDefinition getEnvironmentContainerDefinitonCurrentlyDeployed(final String environmentContainerName, final String provider) throws Exception {
		return doInConnection(new Executor<EnvironmentContainerDefinition>(){

			@Override
			public EnvironmentContainerDefinition work(EntityManager manager) {
				EnvironmentContainerDefinition envConDef = null;
				// Find EnvironmentContainer
				CrudService<EnvironmentContainerDefinition> envContainerDefService = new CrudServiceImpl<EnvironmentContainerDefinition>(manager);
				List<EnvironmentContainerDefinition> envContainerDefs = envContainerDefService.findWithNamedQuery(EnvironmentContainerDefinition.class, "EnvironmentContainerDefinition.findCurrentlyDeployedForEnvironmentContainerNameAndProvider", 
						ParamFactory.with("environmentContainerName", environmentContainerName).and("provider", provider).parameters(), 1);
				if (envContainerDefs.size() > 0) {
					envConDef = initialise(envContainerDefs.get(0));
				}
				return envConDef;
			}
		});
	}
	
	/**
	 * Returns the Environment Container Definition for the environmentContainerName and provider with the given version
	 * @param environmentContainerName the name of the container
	 * @param version the version of the Environment Container Definition or null
	 * @return
	 * @throws Exception
	 */
	public EnvironmentContainerDefinition getEnvironmentContainerDefinition(final String environmentContainerName, final String version, final String provider) throws Exception {
		return doInConnection(new Executor<EnvironmentContainerDefinition>(){

			@Override
			public EnvironmentContainerDefinition work(EntityManager manager) {
				EnvironmentContainerDefinition envConDef = null;
				// Find EnvironmentContainer
				CrudService<EnvironmentContainer> envContainerService = new CrudServiceImpl<EnvironmentContainer>(manager);
				EnvironmentContainer envContainer = envContainerService.findOnlyResultWithNamedQuery(EnvironmentContainer.class, "EnvironmentContainer.findByNameAndProvider", 
						ParamFactory.with("name", environmentContainerName).and("provider", provider).parameters());
				if (envContainer != null) {
					// Fetch EnvironmentContainerDefinitions
					List<EnvironmentContainerDefinition> envContainerDefs = envContainer.getEnvironmentcontainerdefinitions();
					// Find the requested version
					// TODO Do this in the query
					for (EnvironmentContainerDefinition envContainerDef : envContainerDefs) {
						if (envContainerDef.getVersion().equals(version)) {
							// Exact match on version 
							envConDef = initialise(envContainerDef);
							break;
						}
					}
				}
				return envConDef;
			}
		});
	}

	/**
	 * Returns the Environment with the given name and provider
	 * @param environmentName
	 * @param provider
	 * @return
	 * @throws Exception
	 */
	public Environment getEnvironment(final String environmentName, final String provider) throws Exception {
		return doInConnection(new Executor<Environment>() {
			@Override
			public Environment work(EntityManager manager) {
				// Find Environment
				CrudService<Environment> envService = new CrudServiceImpl<Environment>(manager);
				Environment environment = envService.findOnlyResultWithNamedQuery(Environment.class, "Environment.findByNameAndProvider", 
						ParamFactory.with("name", environmentName).and("provider", provider).parameters());
				return environment;
			}
		});
	}
	
	/**
	 * Returns the name of the hardwareProfile matching the given parameters, or with the Default vmRole, or null if no matches
	 * @param hardwareProfile
	 * @return
	 * @throws Exception
	 */
	public String getHardwareProfile(final HardwareProfile hardwareProfile) throws Exception {
		return doInConnection(new Executor<String>() {
			@Override
			public String work(EntityManager manager) {
				CrudService<HardwareProfile> service = new CrudServiceImpl<HardwareProfile>(manager);
				ParamFactory params = ParamFactory.with("provider", hardwareProfile.getProvider())
						.and("cpuCount", hardwareProfile.getCpuCount())
						.and("memory", hardwareProfile.getMemory())
						.and("interfaceCount", hardwareProfile.getInterfaceCount())
						.and("vmRole", hardwareProfile.getVmRole());
				List<HardwareProfile> profiles = service.findWithNamedQuery(HardwareProfile.class, "HardwareProfile.find", params.parameters(), 1);
				if (profiles.size() == 0) {
					profiles = service.findWithNamedQuery(HardwareProfile.class, "HardwareProfile.find", params.and("vmRole", "Default").parameters(), 1);
				}
				return profiles.size() > 0 ? profiles.get(0).getProfile() : null; 
			}
		});
	}
	
	/**
	 * Get EnvironmentBuild by id.
	 * @param id
	 * @return entity
	 * @throws Exception
	 */
	public EnvironmentBuild getEnvironmentBuild(final int id) throws Exception {
		return doInConnection(new Executor<EnvironmentBuild>() {

			@Override
			public EnvironmentBuild work(EntityManager manager) {
				CrudService<EnvironmentBuild> envService = new CrudServiceImpl<EnvironmentBuild>(manager);
				return envService.find(EnvironmentBuild.class, id);
			}
		});
	}
	
	/**
	 * Lazy initialise the EnvironmentContainerDefinition
	 * @param envConDef
	 */
	private EnvironmentContainerDefinition initialise(EnvironmentContainerDefinition envConDef) {
		if (envConDef != null) {
			for (OrganisationNetwork network : envConDef.getNetworks()) {
				Hibernate.initialize(network);
				Hibernate.initialize(network.getMetadata());
				if (network.getGateway() != null) {
					Hibernate.initialize(network.getGateway());
					Hibernate.initialize(network.getGateway().getNats());
				}
			}
			for (Gateway gateway : envConDef.getGateways()) {
				Hibernate.initialize(gateway);
				Hibernate.initialize(gateway.getNetworks());
			}
			for (DataCentre dataCentre : envConDef.getDataCentres()) {
				Hibernate.initialize(dataCentre);
			}
			// Initialise the GeographicContainer
			EnvironmentContainer environmentContainer = envConDef.getEnvironmentcontainer();
			Hibernate.initialize(environmentContainer.getGeographiccontainers());
			// Ensure the Environment Container only contains this Environment Container Definition
			environmentContainer.setEnvironmentcontainerdefinitions(Collections.singletonList(envConDef));
		}
		return envConDef;
	}

	private <S> S doInConnection(Executor<S> executor) throws Exception {
		ConnectionManager connMgr = initialiseConnection();
		EntityManager manager = connMgr.getManager();
		try {
			return executor.work(manager);
		} catch (Exception e) {
			e.printStackTrace(logger);
			rollback(manager);
			logger.println("Exception caught while executing read: " + e.getMessage());
			throw e;
		}
        finally {
       		connMgr.closeConnection(logger);
        }
	}
	
	interface Executor<T> {
		public T work(EntityManager manager);
	}
}
