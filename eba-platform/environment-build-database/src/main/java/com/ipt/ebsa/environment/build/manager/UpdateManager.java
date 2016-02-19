package com.ipt.ebsa.environment.build.manager;

import javax.persistence.EntityManager;

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
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerBuild;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
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
 * This class contains methods for creating/updating ENVIRONMENT_BUILD database entities
 *
 */
public class UpdateManager extends DatabaseManager {
	
	/**
	 * Creates an Environment Definition and its associated objects
	 * @param manager
	 * @param environmentDefinition
	 * @return
	 * @throws Exception 
	 */
	private EnvironmentDefinition createEnvironmentDefinition(EntityManager manager, EnvironmentDefinition environmentDefinition) throws Exception {
		manager.persist(environmentDefinition);
		for (EnvironmentDefinitionMetaData metadata : environmentDefinition.getMetadata()) {
			manager.persist(metadata);
		}
		for (VirtualMachineContainer vmContainer : environmentDefinition.getVirtualmachinecontainers()) {
			manager.persist(vmContainer);
			for (VirtualMachineContainerMetaData metadata : vmContainer.getMetadata()) {
				manager.persist(metadata);
			}
			for (ApplicationNetwork network : vmContainer.getNetworks()) {
				manager.persist(network);
				for (ApplicationNetworkMetaData metadata : network.getMetadata()) {
					manager.persist(metadata);
				}
			}
			for (VirtualMachine vm : vmContainer.getVirtualmachines()) {
				manager.persist(vm);
				for (Storage storage : vm.getStorages()) {
					manager.persist(storage);
				}
				for (VirtualMachineMetaData metaData : vm.getMetadata()) {
					manager.persist(metaData);
				}
				for (Nic nic : vm.getNics()) {
					manager.persist(nic);
					for (Interface anInterface : nic.getInterfaces()) {
						manager.persist(anInterface);
					}
				}
			}
		}
		return environmentDefinition;
	}
	
	/**
	 * Creates an Organisation Network and its associated objects
	 * @param manager
	 * @param network
	 * @return
	 * @throws Exception
	 */
	private OrganisationNetwork createOrganisationNetwork(EntityManager manager, OrganisationNetwork network) throws Exception {
		manager.persist(network);
		for (OrganisationNetworkMetaData metaData : network.getMetadata()) {
			manager.persist(metaData);
		}
		return network;
	}
	
	/**
	 * Creates a Gateway and its associated objects
	 * @param manager
	 * @param gateway
	 * @return
	 * @throws Exception
	 */
	private Gateway createGateway(EntityManager manager, Gateway gateway) throws Exception {
		manager.persist(gateway);
		for (Nat nat : gateway.getNats()) {
			manager.persist(nat);
		}
		return gateway;
	}
	
	/**
	 * Creates Gateways, OrganisationNetworks and DataCenters
	 * @param envConDef
	 * @return
	 * @throws Exception
	 */
	public EnvironmentContainerDefinition createGatewaysAndOrganisationNetworksAndDataCenters(EnvironmentContainerDefinition envConDef) throws Exception {
		ConnectionManager connMgr = initialiseConnection();
		EntityManager manager = connMgr.getManager();
		try {
			manager.getTransaction().begin();

			// Add EnvironmentContainerDefinition to existing EnvironmentContainer
			CrudService<EnvironmentContainer> envConService = new CrudServiceImpl<EnvironmentContainer>(manager);
			EnvironmentContainer existingEnvCon = envConService.findOnlyResultWithNamedQuery(
					EnvironmentContainer.class, "EnvironmentContainer.findByNameAndProvider", 
					ParamFactory.with("name", envConDef.getEnvironmentcontainer().getName()).and("provider", envConDef.getEnvironmentcontainer().getProvider()).parameters());
			if (existingEnvCon == null) {
				throw new RuntimeException("No EnvironmentContainer found with name: " + envConDef.getEnvironmentcontainer().getName());
			} else {
				existingEnvCon.addEnvironmentcontainerdefinition(envConDef);
				manager.persist(envConDef);
			}

			// Add DataCenters
			for (DataCentre dataCentre : envConDef.getDataCentres()) {
				manager.persist(dataCentre);
			}
			// Add Gateways
			for (Gateway gateway : envConDef.getGateways()) {
				createGateway(manager, gateway);
			}
			// Add Networks
			for (OrganisationNetwork network : envConDef.getNetworks()) {
				createOrganisationNetwork(manager, network);
			}
			
			manager.flush();

			/* EBSAD-15259: isCurrent will be used to indicate which version is currently deployed so should not be set here
			if (envConDef.getIsCurrent()) {
				// Ensure no other EnvironmentContainerDefinition is current in this EnvironmentContainer
				Query query = manager.createNamedQuery("EnvironmentContainerDefinition.setOthersNotCurrent");
				query.setParameter("id", envConDef.getId());
				query.setParameter("environmentcontainerid", existingEnvCon.getId());
				query.executeUpdate();
			}
			*/
			
			manager.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(logger);
			rollback(manager);
			logger.println("Exception caught while creating Gateways and Organisation Networks. " + e.getMessage());
			throw e;
		}
        finally {
       		connMgr.closeConnection(logger);
        }
		return envConDef;
	}
	
	/**
	 * Creates Environment Definitions
	 * @param environment
	 * @return
	 * @throws Exception
	 */
	public Environment createEnvironmentDefinitions(Environment environment) throws Exception {
		ConnectionManager connMgr = initialiseConnection();
		EntityManager manager = connMgr.getManager();
		try {
			manager.getTransaction().begin();

			// Check if Environment already exists
			CrudService<Environment> envService = new CrudServiceImpl<Environment>(manager);
			Environment existingEnv = envService.findOnlyResultWithNamedQuery(Environment.class, "Environment.findByNameAndProvider", 
					ParamFactory.with("name", environment.getName()).and("provider", environment.getEnvironmentcontainer().getProvider()).parameters());
			if (existingEnv == null) {
				// Check that EnvironmentContainer already exists
				CrudService<EnvironmentContainer> envConService = new CrudServiceImpl<EnvironmentContainer>(manager);
				EnvironmentContainer existingEnvCon = envConService.findOnlyResultWithNamedQuery(
						EnvironmentContainer.class, "EnvironmentContainer.findByNameAndProvider", 
						ParamFactory.with("name", environment.getEnvironmentcontainer().getName()).and("provider", environment.getEnvironmentcontainer().getProvider()).parameters());
				if (existingEnvCon == null) {
					throw new RuntimeException("No EnvironmentContainer found with name: " + environment.getEnvironmentcontainer().getName());
				} else {
					// Add new Environment to existing EnvironmentContainer
					existingEnvCon.addEnvironment(environment);
					manager.persist(environment);
					existingEnv = environment;
				}
			} else {
				// Attach new Environment Definitions to existing Environment
				for (EnvironmentDefinition environmentDef : environment.getEnvironmentdefinitions()) {
					existingEnv.addEnvironmentdefinition(environmentDef);
				}
			}

			// Insert EnvironmentDefinitions
			for (EnvironmentDefinition environmentDef : environment.getEnvironmentdefinitions()) {
				createEnvironmentDefinition(manager, environmentDef);
			}
			
			manager.flush();
			
			manager.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(logger);
			rollback(manager);
			logger.println("Exception caught while creating EnvironmentDefintions. " + e.getMessage());
			throw e;
		}
        finally {
       		connMgr.closeConnection(logger);
        }
	
		return environment;
	}
	
	/**
	 * Save environment build log
	 * @param environmentBuild entity
	 * @return same entity
	 * @throws Exception
	 */
	public EnvironmentBuild saveEnvironmentBuild(EnvironmentBuild environmentBuild) throws Exception {
		ConnectionManager connMgr = initialiseConnection();
		EntityManager manager = connMgr.getManager();
		try {
			manager.getTransaction().begin();
			manager.persist(environmentBuild);
			manager.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(logger);
			rollback(manager);
			logger.println("Exception caught while saving EnvironmentBuild. " + e.getMessage());
			throw e;
		}
        finally {
       		connMgr.closeConnection(logger);
        }
	
		return environmentBuild;
	}
	
	/**
	 * Update environment build log
	 * @param environmentBuild entity
	 * @return same entity
	 * @throws Exception
	 */
	public EnvironmentBuild updateEnvironmentBuild(EnvironmentBuild environmentBuild) throws Exception {
		ConnectionManager connMgr = initialiseConnection();
		EntityManager manager = connMgr.getManager();
		try {
			manager.getTransaction().begin();
			manager.merge(environmentBuild);
			manager.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(logger);
			rollback(manager);
			logger.println("Exception caught while updating EnvironmentBuild. " + e.getMessage());
			throw e;
		}
        finally {
       		connMgr.closeConnection(logger);
        }
	
		return environmentBuild;
	}

	/**
	 * Save environment container build log
	 * @param environmentContainerBuild entity
	 * @return same entity
	 * @throws Exception
	 */
	public EnvironmentContainerBuild saveEnvironmentContainerBuild(EnvironmentContainerBuild environmentContainerBuild) throws Exception {
		ConnectionManager connMgr = initialiseConnection();
		EntityManager manager = connMgr.getManager();
		try {
			manager.getTransaction().begin();
			manager.persist(environmentContainerBuild);
			manager.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(logger);
			rollback(manager);
			logger.println("Exception caught while saving EnvironmentContainerBuild. " + e.getMessage());
			throw e;
		}
        finally {
       		connMgr.closeConnection(logger);
        }
	
		return environmentContainerBuild;
	}
	
	/**
	 * Update environment container build log
	 * @param environmentContainerBuild entity
	 * @return same entity
	 * @throws Exception
	 */
	public EnvironmentContainerBuild updateEnvironmentContainerBuild(EnvironmentContainerBuild environmentContainerBuild) throws Exception {
		ConnectionManager connMgr = initialiseConnection();
		EntityManager manager = connMgr.getManager();
		try {
			manager.getTransaction().begin();
			manager.merge(environmentContainerBuild);
			manager.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(logger);
			rollback(manager);
			logger.println("Exception caught while updating EnvironmentContainerBuild. " + e.getMessage());
			throw e;
		}
        finally {
       		connMgr.closeConnection(logger);
        }
	
		return environmentContainerBuild;
	}
	
	/**
	 * Creates Environment Container Definitions
	 * @param environmentContainer
	 * @return
	 * @throws Exception
	 */
	public EnvironmentContainer createEnvironmentContainerDefinitions(EnvironmentContainer environmentContainer) throws Exception {
		ConnectionManager connMgr = initialiseConnection();
		EntityManager manager = connMgr.getManager();
		EnvironmentContainer existingEnvCon = null;
		try {
			manager.getTransaction().begin();
	
			// Find existing EnvironmentContainer
			CrudService<EnvironmentContainer> envConService = new CrudServiceImpl<EnvironmentContainer>(manager);
			existingEnvCon = envConService.findOnlyResultWithNamedQuery(
					EnvironmentContainer.class, "EnvironmentContainer.findByNameAndProvider", 
					ParamFactory.with("name", environmentContainer.getName()).and("provider", environmentContainer.getProvider()).parameters());
			if (existingEnvCon == null) {
				throw new RuntimeException("No EnvironmentContainer found with name: " + environmentContainer.getName() + " for provider: " + environmentContainer.getProvider());
			} else {
				// Add EnvironmentContainerDefinitions to existing EnvironmentContainer
				for (EnvironmentContainerDefinition envConDef : environmentContainer.getEnvironmentcontainerdefinitions()) {
					existingEnvCon.addEnvironmentcontainerdefinition(envConDef);
					manager.persist(envConDef);
					
					// Add DataCenters
					for (DataCentre dataCentre : envConDef.getDataCentres()) {
						manager.persist(dataCentre);
					}
					// Add Gateways
					for (Gateway gateway : envConDef.getGateways()) {
						createGateway(manager, gateway);
					}
					// Add Networks
					for (OrganisationNetwork network : envConDef.getNetworks()) {
						createOrganisationNetwork(manager, network);
					}
				}
			}
			
			manager.flush();
			manager.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(logger);
			rollback(manager);
			logger.println("Exception caught while creating Gateways and Organisation Networks. " + e.getMessage());
			throw e;
		}
	    finally {
	   		connMgr.closeConnection(logger);
	    }
		return existingEnvCon;
	}
}
