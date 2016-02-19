package com.ipt.ebsa.buildtools.release.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.buildtools.release.entities.Application;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersionDeployment;
import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersionDeployment;

/**
 * Provides set of stateless methods for reading data values from the store.  Note that each method makes
 * a new connection.  This is because I am not sure how to manage request scoped attributes in Jenkins
 * and I need to be sure the connection gets closed.  It is safer to make sure I get a new connection every time
 * until a method becomes known for how to open, share and close a connection per request.
 * @author scowx
 *
 */
public class ReadManager {

	private Logger logger = LogManager.getLogger(ReadManager.class);
		
	/**
     * Returns the application related to the application short name provided
     * @return		
     * @throws IOException
     */
    public Application getApplication(String applicationId) throws IOException {
        
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	if (applicationId != null) {
        		connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
	        	Application application = new CrudServiceImpl<Application>(connectionManager.getManager()).find(Application.class, Long.parseLong(applicationId));
	    		if (application == null) {
	    			throw new Exception("Application with an ID name of '"+applicationId+"' cannot be found");
	    		}
	    		return application;
        	}
        	else {
        		logger.error("ApplicationId is null, cannot fetch application. ");
        		return null;
        	}
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        } 
    }  
    
    /**
     * Returns a list of Applications which the user can choose to make a release out of
     * @return		
     * @throws IOException
     */
    public List<Application> getApplications() throws IOException {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
        	return getApplications(connectionManager.getManager());
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        } 
    }
    
    /**
     * Returns a unique list of component names for the selected application
     * @param applicationId the applicationId to limit the search to
     * @return a list of component names for this application
     * @throws IOException
     */
    public List<String> getComponents(String applicationId) throws IOException {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
        	return getUniqueComponentNames(connectionManager.getManager(), applicationId);        	
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
    }
    
    /**
     * Returns a list of unique RPM package names and versions for the selected application
     * @param applicationId the applicationId to limit the search to
     * @return a list of components, which each have a unique rpmPackageName and rpmPackageVersion
     * @throws IOException
     */
	public List<ComponentVersion> getUniqueRpmPackages(String applicationId) throws IOException {
		ConnectionManager connectionManager = new ConnectionManager();
		try {
			connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());

			EntityManager em = connectionManager.getManager();

			Application application = new CrudServiceImpl<Application>(em).find(Application.class, Long.parseLong(applicationId));
			CrudService<ComponentVersion> cscv = new CrudServiceImpl<ComponentVersion>(em);

			List<ComponentVersion> orderedByRPM = cscv.findWithNamedQuery(ComponentVersion.class, "findAllRpmsUsingApplication",
					ParamFactory.with("application", application).parameters());

			// Remove components with duplicate RPM package name + version by
			// adding to a set
			Set<ComponentVersion> filtered = new TreeSet<ComponentVersion>(new Comparator<ComponentVersion>() {

				@Override
				public int compare(ComponentVersion o1, ComponentVersion o2) {
					int result = o1.getRpmPackageName().compareTo(o2.getRpmPackageName());
					if (result != 0) {
						return result;
					}
					return o1.getRpmPackageVersion().compareTo(o2.getRpmPackageVersion());
				}

			});
			filtered.addAll(orderedByRPM);

			// Return a list populated from the set
			return new ArrayList<ComponentVersion>(filtered);
		} catch (Exception e) {
			logger.error("Exception while getting data", e);
			return null;
		} finally {
			logger.debug("Closing entity manager");
			if (connectionManager.getManager() != null) {
				connectionManager.closeConnection(System.out);
			}
		}
	}
    
	 /**
     * Returns a map that groups components by name
     * @param applicationId the applicationId to limit the search to
     * @return a map of components, grouped by name
     * @throws IOException
     */
    public Map<String, Set<ComponentVersion>> getComponentsByName(String applicationId) throws IOException {
    	ConnectionManager connectionManager = new ConnectionManager();
		try {
			connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());
		
			EntityManager em = connectionManager.getManager();
			
			return getComponentsByName(em, applicationId);
		} catch (Exception e) {
			logger.error("Exception while getting data", e);
			return null;
		}
		finally {
			logger.debug("Closing entity manager");
			if (connectionManager.getManager() != null) {
				connectionManager.closeConnection(System.out);
			}
		}
    }
    
	/**
     * Returns a map that groups components by name
     * @param applicationId the applicationId to limit the search to
     * @param em
     * @return a map of components, grouped by name
     */
    public Map<String, Set<ComponentVersion>> getComponentsByName(EntityManager em, String applicationId) throws IOException {
		Application application = new CrudServiceImpl<Application>(em).find(Application.class, Long.parseLong(applicationId));
		CrudService<ComponentVersion> cscv = new CrudServiceImpl<ComponentVersion>(em);
		
		List<ComponentVersion> components = cscv.findWithNamedQuery(ComponentVersion.class, "findAllRpmsUsingApplication", ParamFactory.with("application", application).parameters());
		
		Map<String, Set<ComponentVersion>> groupedByName = new TreeMap<>();
		for (ComponentVersion component : components) {
			if (groupedByName.containsKey(component.getName())) {
				groupedByName.get(component.getName()).add(component);
			} else {
				// Components are stored in REVERESE id order
				Set<ComponentVersion> componentsWithName = new TreeSet<>(new Comparator<ComponentVersion>(){

					@Override
					public int compare(ComponentVersion o1,
							ComponentVersion o2) {
						return o2.getId().compareTo(o1.getId());
					}
					
				});
				componentsWithName.add(component);
				groupedByName.put(component.getName(), componentsWithName);
			}
		}
		return groupedByName;
	}
    
    /**
     * Returns the requested ComponentVersion
     * @param componentVersionId
     * @return
     * @throws IOException
     */
    public ComponentVersion getComponentVersion(String componentVersionId) throws IOException {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
    		if (componentVersionId != null) {
        		connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
	        	ComponentVersion componentVersion = new CrudServiceImpl<ComponentVersion>(connectionManager.getManager()).find(ComponentVersion.class, Long.parseLong(componentVersionId));
	    		if (componentVersion == null) {
	    			throw new Exception("ComponentVersion with an ID name of '"+componentVersionId+"' cannot be found");
	    		}
	    		return componentVersion;
        	}
        	else {
        		logger.error("ComponentVersionId is null, cannot fetch application. ");
        		return null;
        	}      	
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
    }

    /**
     * Returns all the component versions for a particular component 
     * @param componentName
     * @return
     * @throws IOException
     */
    public List<ComponentVersion> getComponentVersions(final String componentName) throws IOException {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
        	return getComponentVersions(connectionManager.getManager(), componentName);
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
    }
    
    /**
     * Returns all the ApplicationVersion 
     * @return
     * @throws IOException
     */
    public List<ApplicationVersion> getApplicationVersions() throws IOException {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
        	List<ApplicationVersion> ver =  getApplicationVersions(connectionManager.getManager());
        	//Force lazy initialisation as after this method the connection will be closed
        	for (ApplicationVersion applicationVersion : ver) {
				List<ComponentVersion> components = applicationVersion.getComponents();
				for (ComponentVersion componentVersion : components) {
					logger.debug(componentVersion.toString());
				}
			}
        	return ver;
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
    }    
    
    /**
     * Returns ApplicationVersions for a specific Application 
     * @return
     * @throws IOException
     */
    public List<ApplicationVersion> getApplicationVersions(String applicationId) throws IOException {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
        	List<ApplicationVersion> ver =  getApplicationVersions(connectionManager.getManager(), applicationId);
        	//Force lazy initialisation as after this method the connection will be closed
        	for (ApplicationVersion applicationVersion : ver) {
				List<ComponentVersion> components = applicationVersion.getComponents();
				for (ComponentVersion componentVersion : components) {
					logger.debug(componentVersion.toString());
				}
			}
        	return ver;
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
    } 


    /**
     * Returns the applicationVersionDeployment record related to the id provided
     * @return		
     * @throws IOException
     */
    public ApplicationVersionDeployment getApplicationVersionDeployment(String applicationVersionDeploymentId) throws IOException {
        
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	if (applicationVersionDeploymentId != null) {
        		connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
        		ApplicationVersionDeployment appverDep = new CrudServiceImpl<ApplicationVersionDeployment>(connectionManager.getManager()).find(ApplicationVersionDeployment.class, Long.parseLong(applicationVersionDeploymentId));
	    		if (appverDep == null) {
	    			throw new Exception("ApplicationVersionDeployment with an ID of '"+applicationVersionDeploymentId+"' cannot be found");
	    		}
	    		return appverDep;
        	}
        	else {
        		logger.error("applicationVersionDeploymentId is null, cannot fetch ApplicationVersionDeployment. ");
        		return null;
        	}
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        } 
    } 
    
    /**
	 * Returns the list of ApplicationVersionDeployments
	 * @param em
	 * @return
	 */
	public List<ApplicationVersionDeployment> getApplicationVersionDeployments() {
		ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
        	List<ApplicationVersionDeployment> ver =  getAllApplicationVersionDeployments(connectionManager.getManager());
        	//Force lazy initialisation as after this method the connection will be closed
        	for (ApplicationVersionDeployment item : ver) {
        		ApplicationVersion applicationVersion = item.getApplicationVersion();
				List<ComponentVersion> components = applicationVersion.getComponents();
				for (ComponentVersion componentVersion : components) {
					logger.debug(componentVersion.toString());
				}
			}
        	return ver;
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        } finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
	}
	
	/**
	 * Returns all ApplicationVersionDeployments for this application versions
	 * @param em
	 * @return
	 */
	public List<ApplicationVersionDeployment> getApplicationVersionDeployments(String applicationVersionId) {
		ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
        	List<ApplicationVersionDeployment> ver =  getApplicationVersionDeployments(connectionManager.getManager(), applicationVersionId);
        	//Force lazy initialisation as after this method the connection will be closed
        	for (ApplicationVersionDeployment item : ver) {
        		ApplicationVersion applicationVersion = item.getApplicationVersion();
				List<ComponentVersion> components = applicationVersion.getComponents();
				for (ComponentVersion componentVersion : components) {
					logger.debug(componentVersion.toString());
				}
			}
        	return ver;
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        } 
	}	
    
    
	/**
	 * Returns a unique list of component names associated with the applicationId passed in as a parameter
	 * @param em
	 * @param applicationId
	 * @return
	 */
	public List<String> getUniqueComponentNames(EntityManager em, String applicationId) {
		Application application = new CrudServiceImpl<Application>(em).find(Application.class, Long.parseLong(applicationId));
		List<String> componentNames = new ArrayList<String>(); 
		CrudService<ComponentVersion> cscv = new CrudServiceImpl<ComponentVersion>(em);
		List<ComponentVersion> list = cscv.findWithNamedQuery(ComponentVersion.class, "findAllUsingApplication", ParamFactory.with("application", application).parameters());
		if (list != null && list.size() > 0) {
			for (ComponentVersion componentVersion : list) {
				if (!componentNames.contains(componentVersion.getName())){
					componentNames.add(componentVersion.getName());
				}
			}
		}
		return componentNames;
	}
		
	/**
	 * Returns all component versions which have a particular component name
	 * @param em
	 * @param componentName
	 * @return
	 */
	public List<ComponentVersion> getComponentVersions(EntityManager em, String componentName) {
		CrudService<ComponentVersion> cscv = new CrudServiceImpl<ComponentVersion>(em);
		List<ComponentVersion> list = cscv.findWithNamedQuery(ComponentVersion.class, "findAllUsingComponentName", ParamFactory.with("name", componentName).parameters());
		return list;
	}
	
	/**
	 * Returns all application versions
	 * @param em
	 * @return
	 */
	public List<ApplicationVersion> getApplicationVersions(EntityManager em) {
		CrudService<ApplicationVersion> cscv = new CrudServiceImpl<ApplicationVersion>(em);
		List<ApplicationVersion> list = cscv.findWithNamedQuery(ApplicationVersion.class, "findAllApplicationVersions");
		return list;
	}	
	
	/**
	 * Returns all application versions
	 * @param em
	 * @return
	 */
	public List<ApplicationVersion> getApplicationVersions(EntityManager em, String applicationId) {
		Application application = new CrudServiceImpl<Application>(em).find(Application.class, Long.parseLong(applicationId));
		CrudService<ApplicationVersion> cscv = new CrudServiceImpl<ApplicationVersion>(em);
		List<ApplicationVersion> list = cscv.findWithNamedQuery(ApplicationVersion.class, "findAllApplicationVersionsUsingApplication", ParamFactory.with("application", application).parameters());
		return list;
	}
	
	/**
	 * Returns the list of applications
	 * @param em
	 * @return
	 */
	public List<Application> getApplications(EntityManager em) {
		CrudService<Application> cscv = new CrudServiceImpl<Application>(em);
		return cscv.findWithNamedQuery(Application.class, "list");		
	}
	
	/**
	 * Returns the list of ApplicationVersionDeployments
	 * @param em
	 * @return
	 */
	public List<ApplicationVersionDeployment> getAllApplicationVersionDeployments(EntityManager em) {
		return new CrudServiceImpl<ApplicationVersionDeployment>(em).findWithNamedQuery(ApplicationVersionDeployment.class, "findAllApplicationVersionDeployments");		
	}
	
	/**
	 * Returns all ApplicationVersionDeployments for this application versions
	 * @param em
	 * @return
	 */
	public List<ApplicationVersionDeployment> getApplicationVersionDeployments(EntityManager em, String applicationVersionId) {
		ApplicationVersion applicationVersion = new CrudServiceImpl<ApplicationVersion>(em).find(ApplicationVersion.class, Long.parseLong(applicationVersionId));
		List<ApplicationVersionDeployment> list = new CrudServiceImpl<ApplicationVersionDeployment>(em).findWithNamedQuery(ApplicationVersionDeployment.class, "findAllApplicationVersionDeploymentsUsingApplicationVersion", ParamFactory.with("applicationVersion", applicationVersion).parameters());
		return list;
	}
	
    /**
     * Returns whether a ComponentVersion with the given groupId, artifactId, version, classifier, applicationShortName exists 
     * @param groupId
     * @param artifactId
     * @param version
     * @param classifier
     * @param applicationShortName
     * @return
     * @throws Exception
     */
    public boolean hasComponentVersion(final String groupId, final String artifactId, final String version, final String classifier, final String applicationShortName) throws Exception {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());
        	return hasComponentVersion(connectionManager.getManager(), groupId, artifactId, version, classifier, applicationShortName);
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	throw e;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
    }
    
	/**
	 * Returns whether a ComponentVersion with the given groupId, artifactId, version, classifier, applicationShortName exists
	 * @param em
     * @param groupId
     * @param artifactId
     * @param version
     * @param classifier
     * @param applicationShortName
	 * @return
	 */
	public boolean hasComponentVersion(final EntityManager em, final String groupId, final String artifactId, final String version, final String classifier, final String applicationShortName) {
		Long count;
		ParamFactory params = ParamFactory.with("groupId", groupId).and("artifactId", artifactId).and("version", version).and("applicationShortName", applicationShortName);
		if (StringUtils.isBlank(classifier)) {
			count = new CrudServiceImpl<Long>(em).findOnlyResultWithNamedQuery(Long.class, "countComponentVersionsWithGroupArtifactVersion", params.parameters());
		} else {
			params.and("classifier", classifier);
			count = new CrudServiceImpl<Long>(em).findOnlyResultWithNamedQuery(Long.class, "countComponentVersionsWithGroupArtifactVersionClassifier", params.parameters());
		}
		return count != null && count.longValue() > 0;
	}

    /**
     * Returns whether an ApplicationVersion with the given name and version exists
     * @param name
     * @param version
     * @return
     * @throws Exception
     */
    public boolean hasApplicationVersion(final String name, final String version) throws Exception {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());
        	return hasApplicationVersion(connectionManager.getManager(), name, version);
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	throw e;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
    }
    
	/**
     * Returns whether an ApplicationVersion with the given name and version exists
	 * @param em
     * @param groupId
     * @param artifactId
     * @param version
     * @param classifier
	 * @return
	 */
	public boolean hasApplicationVersion(final EntityManager em, final String name, final String version) {
		ParamFactory params = ParamFactory.with("name", name).and("version", version);
		Long count = new CrudServiceImpl<Long>(em).findOnlyResultWithNamedQuery(Long.class, "countApplicationVersionsWithNameAndVersion", params.parameters());
		return count != null && count.longValue() > 0;
	}
	
	/**
     * Returns whether an ApplicationVersion with the given application id and version exists
     * @param name
     * @param version
     * @return
     * @throws Exception
     */
    public boolean hasApplicationVersionForApp(final Long applicationId, final String version) throws Exception {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());
        	return hasApplicationVersionForApp(connectionManager.getManager(), applicationId, version);
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	throw e;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
    }
    
    /**
     * Returns whether an ApplicationVersion with the given application id and version exists
	 * @param em
     * @param groupId
     * @param artifactId
     * @param version
     * @param classifier
	 * @return
	 */
	public boolean hasApplicationVersionForApp(final EntityManager em, final Long applicationId, final String version) {
		ParamFactory params = ParamFactory.with("appId", applicationId).and("version", version);
		Long count = new CrudServiceImpl<Long>(em).findOnlyResultWithNamedQuery(Long.class, "countApplicationVersionsWithAppAndVersion", params.parameters());
		return count != null && count.longValue() > 0;
	}
	
	/**
     * Returns the ApplicationVersion related to the application version id provided
     * @return		
     * @throws IOException
     */
    public ApplicationVersion getApplicationVersion(String applicationVersionId) throws IOException {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	if (applicationVersionId != null) {
        		connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());
	        	ApplicationVersion applicationVersion = getApplicationVersion(connectionManager.getManager(), applicationVersionId);
	    		if (applicationVersion == null) {
	    			throw new Exception("ApplicationVersion with an ID of '" + applicationVersionId + "' cannot be found");
	    		} else {
	    			// Force lazy initialisation as after this method the connection will be closed
	    			for (ComponentVersion componentVersion : applicationVersion.getComponents()) {
	    				logger.debug(componentVersion.toString());
	    			}
	    		}
	    		return applicationVersion;
        	}
        	else {
        		logger.error("ApplicationVersionId is null, cannot fetch ApplicationVersion. ");
        		return null;
        	}
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        } 
    }
    
	/**
     * Returns the application related to the application short name provided
     * @return		
     * @throws IOException
     */
    public ApplicationVersion getApplicationVersion(EntityManager em, String applicationVersionId) throws IOException {
    	return new CrudServiceImpl<ApplicationVersion>(em).find(ApplicationVersion.class, Long.parseLong(applicationVersionId));
    }
	
	/**
	 * Returns the list of ReleaseVersionDeployments
	 * @param em
	 * @return
	 */
	public List<ReleaseVersionDeployment> getAllReleaseVersionDeployments(EntityManager em) {
		return new CrudServiceImpl<ReleaseVersionDeployment>(em).findWithNamedQuery(ReleaseVersionDeployment.class, "findAllReleaseVersionDeployments");		
	}
	
    /**
	 * Returns the list of ReleaseVersionDeployments
	 * @param em
	 * @return
	 */
	public List<ReleaseVersionDeployment> getReleaseVersionDeployments() {
		ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());
        	List<ReleaseVersionDeployment> ver = getAllReleaseVersionDeployments(connectionManager.getManager());
        	// Force lazy initialisation as after this method the connection will be closed
        	for (ReleaseVersionDeployment item : ver) {
        		ReleaseVersion releaseVersion = item.getReleaseVersion();
				List<ApplicationVersion> applicationVersions = releaseVersion.getApplicationVersions();
				for (ApplicationVersion applicationVersion : applicationVersions) {
					logger.debug(applicationVersion.toString());
				}
			}
        	return ver;
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        } finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
	}
	
    /**
     * Returns the ReleaseVersionDeployment record related to the id provided
     * @return		
     * @throws IOException
     */
    public ReleaseVersionDeployment getReleaseVersionDeployment(String releaseVersionDeploymentId) throws IOException {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	if (releaseVersionDeploymentId != null) {
        		connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());
        		ReleaseVersionDeployment relVerDep = new CrudServiceImpl<ReleaseVersionDeployment>(connectionManager.getManager()).find(ReleaseVersionDeployment.class, Long.parseLong(releaseVersionDeploymentId));
	    		if (relVerDep == null) {
	    			throw new Exception("ReleaseVersionDeployment with an ID of '" + releaseVersionDeploymentId + "' cannot be found");
	    		}
	    		return relVerDep;
        	}
        	else {
        		logger.error("releaseVersionDeploymentId is null, cannot fetch ReleaseVersionDeployment. ");
        		return null;
        	}
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        } 
    }
    
	/**
	 * Returns all ReleaseVersionDeployments for a release version
	 * @param em
	 * @return
	 */
	public List<ReleaseVersionDeployment> getReleaseVersionDeployments(String releaseVersionId) {
		ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());
        	List<ReleaseVersionDeployment> ver = getReleaseVersionDeployments(connectionManager.getManager(), releaseVersionId);
        	// Force lazy initialisation as after this method the connection will be closed
        	for (ReleaseVersionDeployment item : ver) {
        		ReleaseVersion releaseVersion = item.getReleaseVersion();
				List<ApplicationVersion> applicationVersions = releaseVersion.getApplicationVersions();
				for (ApplicationVersion applicationVersion : applicationVersions) {
					logger.debug(applicationVersion.toString());
				}
			}
        	return ver;
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        } 
	}
	
	/**
	 * Returns all ReleaseVersionDeployments of the given release version
	 * @param em
	 * @return
	 */
	public List<ReleaseVersionDeployment> getReleaseVersionDeployments(EntityManager em, String releaseVersionId) {
		ReleaseVersion releaseVersion = new CrudServiceImpl<ReleaseVersion>(em).find(ReleaseVersion.class, Long.parseLong(releaseVersionId));
		List<ReleaseVersionDeployment> list = new CrudServiceImpl<ReleaseVersionDeployment>(em).findWithNamedQuery(ReleaseVersionDeployment.class, "findAllReleaseVersionDeploymentsUsingReleaseVersion", ParamFactory.with("releaseVersion", releaseVersion).parameters());
		return list;
	}
	
    /**
     * Returns all the ReleaseVersions 
     * @return
     * @throws IOException
     */
    public List<ReleaseVersion> getReleaseVersions() throws IOException {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());
        	List<ReleaseVersion> ver = getReleaseVersions(connectionManager.getManager());
        	// Force lazy initialisation as after this method the connection will be closed
        	for (ReleaseVersion releaseVersion : ver) {
				List<ApplicationVersion> applicationVersions = releaseVersion.getApplicationVersions();
				// Sort by application name
				Collections.sort(applicationVersions, new Comparator<ApplicationVersion>() {
					public int compare(ApplicationVersion o1, ApplicationVersion o2) {
						return o1.getApplication().getName().compareTo(o2.getApplication().getName());
					}
				});
			}
        	return ver;
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
    }
    
	/**
	 * Returns all release versions
	 * @param em
	 * @return
	 */
	public List<ReleaseVersion> getReleaseVersions(EntityManager em) {
		return new CrudServiceImpl<ReleaseVersion>(em).findWithNamedQuery(ReleaseVersion.class, "findAllReleaseVersions");
	}
	
	/**
	 * Returns a release version
	 * @param releaseVersionId
	 * @return
	 */
	public ReleaseVersion getReleaseVersion(String releaseVersionId) {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	if (releaseVersionId != null) {
        		connectionManager.initialiseConnection(System.out, (ConnectionData)GlobalConfig.getConfig());
	        	ReleaseVersion releaseVersion = new CrudServiceImpl<ReleaseVersion>(connectionManager.getManager()).find(ReleaseVersion.class, Long.parseLong(releaseVersionId));
	    		if (releaseVersion == null) {
	    			throw new Exception("Release Version with an ID of '"+releaseVersionId+"' cannot be found");
	    		}
	        	// Force lazy initialisation as after this method the connection will be closed
				List<ApplicationVersion> applicationVersions = releaseVersion.getApplicationVersions();
				// Sort by application name
				Collections.sort(applicationVersions, new Comparator<ApplicationVersion>() {
					public int compare(ApplicationVersion o1, ApplicationVersion o2) {
						return o1.getApplication().getName().compareTo(o2.getApplication().getName());
					}
				});
				
				// We're interested in the component versions applicable in each app. version. Force them
				// to be loaded now instead of breaking when we attempt to load them lazily later on outside
				// of a transaction
				for (ApplicationVersion applicationVersion : releaseVersion.getApplicationVersions()) {
					applicationVersion.getComponents().size();
				}
	    		return releaseVersion;
        	}
        	else {
        		logger.error("ReleaseVersionId is null, cannot fetch application. ");
        		return null;
        	}
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	return null;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        } 

	}
	
    /**
     * Returns whether a ReleaseVersion with the given name and version exists
     * @param name
     * @param version
     * @return
     * @throws Exception
     */
    public boolean hasReleaseVersion(final String name, final String version) throws Exception {
    	ConnectionManager connectionManager = new ConnectionManager();
    	try {
        	connectionManager.initialiseConnection(System.out, (ConnectionData) GlobalConfig.getConfig());
        	return hasReleaseVersion(connectionManager.getManager(), name, version);
        } catch (Exception e) {
        	logger.error("Exception while getting data", e);
        	throw e;
        }
        finally {
        	logger.debug("Closing entity manager");
        	if (connectionManager.getManager() != null) {
        		connectionManager.closeConnection(System.out);
        	}	
        }   
    }
    
	/**
     * Returns whether an ReleaseVersion with the given name and version exists
	 * @param em
     * @param groupId
     * @param artifactId
     * @param version
     * @param classifier
	 * @return
	 */
	public boolean hasReleaseVersion(final EntityManager em, final String name, final String version) {
		ParamFactory params = ParamFactory.with("name", name).and("version", version);
		Long count = new CrudServiceImpl<Long>(em).findOnlyResultWithNamedQuery(Long.class, "countReleaseVersionsWithNameAndVersion", params.parameters());
		return count != null && count.longValue() > 0;
	}
}
