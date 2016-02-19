package com.ipt.ebsa.buildtools.release.manager;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import com.ipt.ebsa.buildtools.release.entities.Application;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersionDeployment;
import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersionDeployment;

import static com.ipt.ebsa.buildtools.release.util.ClassLoaderUtils.*;

/**
 * This class contains methods and functions for performing release oriented functions
 * @author scowx
 *
 */
public class UpdateManager {

	/**
	 * Creates a component version.  It does all of the connectivity
	 */
	public ComponentVersion createComponentVersion(PrintStream logger,
												   String applicationShortName, String componentName, String groupId,
												   String artifactId, String compVersion, String packaging,
												   String classifier, String type, String rpmPackageName,
												   String rpmPackageVersion, String notes, int buildNumber,
												   String projectName, String buildId) throws Exception {
		return createComponentVersion(logger,
				applicationShortName, componentName, groupId,
				artifactId, compVersion, packaging,
				classifier, type, rpmPackageName,
				rpmPackageVersion, notes, buildNumber,
				projectName, buildId, null, null);
	}

	/**
	 * Creates a component version.  It does all of the connectivity
	 */
	public ComponentVersion createComponentVersion(PrintStream logger,
			String applicationShortName, String componentName, String groupId,
			String artifactId, String compVersion, String packaging,
			String classifier, String type, String rpmPackageName, 
			String rpmPackageVersion, String notes, int buildNumber,
			String projectName, String buildId, Boolean ciStatus, Boolean cbtStatus) throws Exception {
		
		ConnectionManager connMgr = new ConnectionManager();
	    
		try {
        	connMgr.initialiseConnection(logger, (ConnectionData)GlobalConfig.getConfig());
			
			connMgr.getManager().getTransaction().begin();

			ComponentVersion cv = createComponentVersion(connMgr.getManager(), applicationShortName, componentName, groupId, artifactId, 
					compVersion, packaging, classifier, type, rpmPackageName, rpmPackageVersion, notes, buildNumber, projectName, buildId, ciStatus, cbtStatus);
						
			logger.println("Committing transaction");
			connMgr.getManager().getTransaction().commit();
			logger.println("Transaction committed");
			
			logger.println(String.format("Component version created %s",cv));

            return cv;
		} catch (Exception e) {
			e.printStackTrace(logger);
			EntityManager em = connMgr.getManager();
			if (em != null ){
			  EntityTransaction t = em.getTransaction();
			  if (t != null) {
			    t.rollback();
			  }
			}
			logger.println("Exception caught while creating component. " + e.getClass() + " " + e.getMessage());
			throw e;
		}
        finally {
        	logger.println("Closing entity manager");
        	if (connMgr.getManager() != null) {
        		connMgr.closeConnection(logger);
        	}	
        }
	}
	
	/**
	 * Creates an Application
	 * @param logger
	 * @param applicationShortName
	 * @param applicationName
	 * @return
	 * @throws Exception
	 */
	public Application createApplication(PrintStream logger, String applicationShortName, String applicationName, String role) throws Exception {
        ConnectionManager connMgr = new ConnectionManager();
	    
		try {
        	connMgr.initialiseConnection(logger, (ConnectionData)GlobalConfig.getConfig());
			
			connMgr.getManager().getTransaction().begin();

			Application application = createApplication(connMgr.getManager(), applicationShortName, applicationName, role);
						
			logger.println("Committing transaction");
			connMgr.getManager().getTransaction().commit();
			logger.println("Transaction committed");
			
			logger.println(String.format("Application created %s",application));

            return application;
		} catch (Exception e) {
			e.printStackTrace(logger);
			EntityManager em = connMgr.getManager();
			if (em != null ){
			  EntityTransaction t = em.getTransaction();
			  if (t != null) {
			    t.rollback();
			  }
			}
			logger.println("Exception caught while creating component. " + e.getClass() + " " + e.getMessage());
			throw e;
		}
        finally {
        	logger.println("Closing entity manager");
        	if (connMgr.getManager() != null) {
        		connMgr.closeConnection(logger);
        	}	
        }
	}

	/**
	 * Creates an Application
	 * @param logger
	 * @param applicationShortName
	 * @param applicationName
	 * @return
	 * @throws Exception
	 */
	public Application createApplication(PrintStream logger, String applicationShortName, String applicationName) throws Exception {
		return createApplication(logger, applicationShortName, applicationName, null);		
	}	

	/**
	 * Creates an application (entity manager needs to be provided)
	 * @param em
	 * @param applicationShortName
	 * @param applicationName
	 * @return
	 * @throws Exception
	 */
    public Application createApplication(EntityManager em, String applicationShortName, String applicationName, String role) throws Exception {
    	CrudService<Application> appService = new CrudServiceImpl<Application>(em);		
		Application application = appService.findOnlyResultWithNamedQuery(Application.class, "findApplicationByShortName",ParamFactory.with("shortName", applicationShortName).parameters());
		
		if (application != null) {
			throw new Exception("Application with a short name of '"+applicationShortName+"' already exists!");
		}
    	
    	application = new Application();
    	application.setShortName(applicationShortName);
    	application.setName(applicationName);
    	application.setRole(role);
		Application app = appService.create(application);
		
		return app;
	}

	/**
     * Creates an ApplicationVersion object with the parameters provided
     * @param stream
     * @param applicationId
     * @param name
     * @param notes
     * @param relatedJiraIssue
     * @param relatedBrpIssue
     * @param version
     * @param ids
     * @param buildNumber
     * @param projectName
     * @param buildId
     * @return
     * @throws Exception
     */
    public ApplicationVersion createApplicationVersion(PrintStream stream,
			String applicationId, String name, String notes, String relatedJiraIssue, String relatedBrpIssue, String version,
			List<Long> ids, int buildNumber, String projectName, String buildId) throws Exception {
		ConnectionManager connMgr = new ConnectionManager();
        
        try {
			/* Create the Application Version */
        	connMgr.initialiseConnection(stream, (ConnectionData)GlobalConfig.getConfig());
			
			connMgr.getManager().getTransaction().begin();
			
			ApplicationVersion applicationVersion = createApplicationVersion( connMgr.getManager(), applicationId, name, notes, relatedJiraIssue, relatedBrpIssue, version, buildNumber, projectName, buildId, ids);
			
			stream.println("Committing transaction");
			connMgr.getManager().getTransaction().commit();
			stream.println("Transaction committed");
			
			stream.println(String.format("Application Version created %s",applicationVersion));
			for (ComponentVersion cv : applicationVersion.getComponents()) {
				   stream.println(String.format("Includes: %s",cv));	
			}
			
			return applicationVersion;
		} catch (Exception e) {
			e.printStackTrace(stream);
			EntityManager em = connMgr.getManager();
			if (em != null ){
			  EntityTransaction t = em.getTransaction();
			  if (t != null) {
			    t.rollback();
			  }
			}
			stream.println("Exception caught while creating component. " + e.getClass() + " " + e.getMessage());
			throw e;
		}
        finally {
        	stream.println("Closing entity manager");
        	if (connMgr.getManager() != null) {
        		connMgr.closeConnection(stream);
        	}	
        }
	}
    
    /**
     * Creates an ApplicatioNVersionDeployment record
     */
    public ApplicationVersionDeployment createApplicationVersionDeployment(PrintStream stream, String applicationVersionId, String environment, Date dateStarted, Date dateCompleted, String log, String plan, String status, Boolean succeeded, int jenkinsBuildNumber, String jenkinsJobName, String jenkinsBuildId) throws Exception {

    	ConnectionManager connMgr = new ConnectionManager();
        
        try {
			/* Create the Application Version */
        	connMgr.initialiseConnection(stream, (ConnectionData)GlobalConfig.getConfig());
			
			connMgr.getManager().getTransaction().begin();
			
			ApplicationVersionDeployment appVerDep = createApplicationVersionDeployment(connMgr.getManager(), applicationVersionId, environment, dateStarted, dateCompleted, log, plan, status, succeeded, jenkinsBuildNumber, jenkinsJobName, jenkinsBuildId);
			
			stream.println("Committing transaction");
			connMgr.getManager().getTransaction().commit();
			stream.println("Transaction committed");
			
			stream.println(String.format("Application Version Deployment record created %s",appVerDep));
						
			return appVerDep;
			
		} catch (Exception e) {
			e.printStackTrace(stream);
			EntityManager em = connMgr.getManager();
			if (em != null ){
			  EntityTransaction t = em.getTransaction();
			  if (t != null) {
			    t.rollback();
			  }
			}
			stream.println("Exception caught while creating component. " + e.getClass() + " " + e.getMessage());
			throw e;
		}
        finally {
        	stream.println("Closing entity manager");
        	if (connMgr.getManager() != null) {
        		connMgr.closeConnection(stream);
        	}	
        }
	}

	/**
	 * Creates a ComponentVersion with the parameters which have been provided.
	 */
	public ComponentVersion createComponentVersion(EntityManager em, String applicationShortName, String name, String group, String artifact, String compVersion,
												   String packaging, String classifier, String type, String rpmPackageName, String rpmPackageVersion, String notes, int jenkinsBuildNumber, String jenkinsJobName, String jenkinsBuildId) throws Exception {
		return createComponentVersion(em, applicationShortName, name, group, artifact, compVersion,
				packaging, classifier, type, rpmPackageName, rpmPackageVersion, notes, jenkinsBuildNumber, jenkinsJobName, jenkinsBuildId, null, null);
	}
	
	/**
	 * Creates a ComponentVersion with the parameters which have been provided.
	 */
    public ComponentVersion createComponentVersion(EntityManager em, String applicationShortName, String name, String group, String artifact, String compVersion, 
    		         String packaging, String classifier, String type, String rpmPackageName, String rpmPackageVersion, String notes, int jenkinsBuildNumber, String jenkinsJobName, String jenkinsBuildId,
												   Boolean ciStatus, Boolean cbtStatus) throws Exception {
		
		CrudService<Application> appService = new CrudServiceImpl<Application>(em);
		CrudService<ComponentVersion> compService = new CrudServiceImpl<ComponentVersion>(em);
		
		Application application = appService.findOnlyResultWithNamedQuery(Application.class, "findApplicationByShortName",ParamFactory.with("shortName", applicationShortName).parameters());
		
		if (application == null) {
			throw new Exception("Application with a short name of '"+applicationShortName+"' cannot be found");
		}
		
		ComponentVersion componentVersion = new ComponentVersion();
		componentVersion.setApplication(application);
		componentVersion.setName(name);
		componentVersion.setDateOfRelease(new Date());
		componentVersion.setJenkinsBuildNumber(jenkinsBuildNumber);
		componentVersion.setJenkinsJobName(jenkinsJobName);
		componentVersion.setJenkinsBuildId(jenkinsBuildId);
		componentVersion.setGroupId(group);
		componentVersion.setArtifactId(artifact);
		componentVersion.setComponentVersion(compVersion);
		componentVersion.setPackaging(packaging);
		componentVersion.setClassifier(classifier);
		componentVersion.setType(type);
		componentVersion.setRpmPackageName(rpmPackageName);
		componentVersion.setRpmPackageVersion(rpmPackageVersion);
		componentVersion.setNotes(notes);
		componentVersion.setCiStatus(ciStatus);
		componentVersion.setCbtStatus(cbtStatus);
		ComponentVersion v = compService.create(componentVersion);
		
		return v;
	}
    
   /**
    * Creates an ApplicationVersionDeployment record
    */
    public ApplicationVersionDeployment createApplicationVersionDeployment(EntityManager em, String applicationVersionId, String environment, Date dateStarted, Date dateCompleted, String log, String plan, String status, Boolean succeeded, int jenkinsBuildNumber, String jenkinsJobName, String jenkinsBuildId) throws Exception {

    	if (applicationVersionId == null) {
    		throw new IllegalArgumentException("'applicationVersionId' may not be null");    		
    	}
    	
		CrudService<ApplicationVersion> appService = new CrudServiceImpl<ApplicationVersion>(em);
		Long long1 = Long.parseLong(applicationVersionId);
		ApplicationVersion applicationVersion = appService.find(ApplicationVersion.class, long1);
	
		if (applicationVersion == null) {
			throw new Exception("ApplicationVersion with id '"+applicationVersionId+"' cannot be found");
		}
	 
		CrudService<ApplicationVersionDeployment> appVerDepService = new CrudServiceImpl<ApplicationVersionDeployment>(em);
		
		ApplicationVersionDeployment dep = new ApplicationVersionDeployment();
		dep.setApplicationVersion(applicationVersion);
		dep.setDateStarted(dateStarted);
		dep.setDateCompleted(dateCompleted);
		dep.setEnvironment(environment);
		dep.setLog(log);
		dep.setPlan(plan);
		dep.setStatus(status);
		dep.setSucceeded(succeeded);
		dep.setJenkinsBuildNumber(jenkinsBuildNumber);
		dep.setJenkinsJobName(jenkinsJobName);
		dep.setJenkinsBuildId(jenkinsBuildId);
		
        ApplicationVersionDeployment newDep = appVerDepService.create(dep);
		
		return newDep;
	}
    

    /**
     * 
     * @param em
     * @param applicationId
     * @param name
     * @param notes
     * @param version
     * @param jenkinsBuildNumber
     * @param jenkinsJobName
     * @param jenkinsBuildId
     * @param componentIds
     * @return
     * @throws Exception
     */
    public ApplicationVersion createApplicationVersion(final EntityManager em, final String applicationId, final String name, final String notes, 
			final String relatedJiraIssue, final String relatedBrpIssue, final String version, final int jenkinsBuildNumber, final String jenkinsJobName, 
			final String jenkinsBuildId, final Object componentIds) throws Exception {
		
		
		return doInAppContext(this, new TypedRunnable<ApplicationVersion>() {
			@Override
			public ApplicationVersion run() throws Exception{
			    /* actual code for this method starts here */
				List<ComponentVersion> components = new ArrayList<>();
				if (componentIds instanceof List && ((List<?>)componentIds).size() > 0) {
					components = new CrudServiceImpl<ComponentVersion>(em).findWithNamedQuery(ComponentVersion.class, "findAllUsingComponentIds", ParamFactory.with("componentIds", componentIds).parameters());
				}
				
				Application application = new CrudServiceImpl<Application>(em).find(Application.class, Long.parseLong(applicationId));
				if (application == null) {
					throw new Exception("Application with an ID name of '"+applicationId+"' cannot be found");
				}
				
				CrudService<ApplicationVersion> appVerSer = new CrudServiceImpl<ApplicationVersion>(em);
				
				/* Create an Application Version */
				ApplicationVersion appver = new ApplicationVersion();
				appver.setDateCreated(new Date());
				appver.setJenkinsBuildNumber(jenkinsBuildNumber);
				appver.setJenkinsJobName(jenkinsJobName);
				appver.setJenkinsBuildId(jenkinsBuildId);
				appver.setName(name);
				appver.setNotes(notes);
				appver.setRelatedJiraIssue(relatedJiraIssue);
	            appver.setRelatedBrpIssue(relatedBrpIssue);
				appver.setVersion(version);
				
				appver.setApplication(application);
							
				/* Add the components to it */
				appver.setComponents(components);
				
				/* Save it */
				ApplicationVersion v = appVerSer.create(appver);
				
				return v;
				/* actual code for this method ends here */
			}
		});
	}
    
	/**
     * Creates a ReleaseVersion object with the parameters provided
     * @param stream
     * @param name
     * @param notes
     * @param relatedJiraIssue
     * @param version
     * @param ids
     * @param jenkinsBuildNumber
     * @param jenkinsJobName
     * @param jenkinsBuildId
     * @return
     * @throws Exception
     */
    public ReleaseVersion createReleaseVersion(PrintStream stream,
			String name, String notes, String relatedJiraIssue, String version,
			List<Long> applicationVersionIds, int jenkinsBuildNumber, String jenkinsJobName, String jenkinsBuildId) throws Exception {
		ConnectionManager connMgr = new ConnectionManager();
        
        try {
			/* Create the Release Version */
        	connMgr.initialiseConnection(stream, (ConnectionData) GlobalConfig.getConfig());
			
			connMgr.getManager().getTransaction().begin();
			
			ReleaseVersion releaseVersion = createReleaseVersion(connMgr.getManager(), name, notes, relatedJiraIssue, version, jenkinsBuildNumber, jenkinsJobName, jenkinsBuildId, applicationVersionIds);
			
			stream.println("Committing transaction");
			connMgr.getManager().getTransaction().commit();
			stream.println("Transaction committed");
			
			stream.println(String.format("Release Version created %s", releaseVersion));
			for (ApplicationVersion av : releaseVersion.getApplicationVersions()) {
				   stream.println(String.format("Includes: %s", av));	
			}
			
			return releaseVersion;
		} catch (Exception e) {
			e.printStackTrace(stream);
			EntityManager em = connMgr.getManager();
			if (em != null ){
			  EntityTransaction t = em.getTransaction();
			  if (t != null) {
			    t.rollback();
			  }
			}
			stream.println("Exception caught while creating component. " + e.getClass() + " " + e.getMessage());
			throw e;
		}
        finally {
        	stream.println("Closing entity manager");
        	if (connMgr.getManager() != null) {
        		connMgr.closeConnection(stream);
        	}	
        }
	}
    
    /**
     * Creates a ReleaseVersion
     * @param em
     * @param name
     * @param notes
     * @param relatedJiraIssue
     * @param version
     * @param jenkinsBuildNumber
     * @param jenkinsJobName
     * @param jenkinsBuildId
     * @param applicationVersionIds
     * @return
     * @throws Exception
     */
    public ReleaseVersion createReleaseVersion(final EntityManager em, final String name, final String notes, 
			final String relatedJiraIssue, final String version, final int jenkinsBuildNumber, final String jenkinsJobName, 
			final String jenkinsBuildId, final Object applicationVersionIds) throws Exception {
		/**
		 * See #createApplicationVersion for explanation of class loading shenanigans!
		 */
		return doInAppContext(this, new TypedRunnable<ReleaseVersion>(){

			@Override 
			public ReleaseVersion run() throws Exception {
			    /* actual code for this method starts here */
				List<ApplicationVersion> applicationVersions = new CrudServiceImpl<ApplicationVersion>(em).findWithNamedQuery(ApplicationVersion.class, "findAllUsingApplicationVersionIds", ParamFactory.with("applicationVersionIds", applicationVersionIds).parameters());
				
				/* Create a Release Version */
				ReleaseVersion relver = new ReleaseVersion();
				relver.setDateCreated(new Date());
				relver.setJenkinsBuildNumber(jenkinsBuildNumber);
				relver.setJenkinsJobName(jenkinsJobName);
				relver.setJenkinsBuildId(jenkinsBuildId);
				relver.setName(name);
				relver.setNotes(notes);
				relver.setRelatedJiraIssue(relatedJiraIssue);
				relver.setVersion(version);
				
				/* Add the ApplicationVersions to it */
				relver.setApplicationVersions(applicationVersions);
			
				/* Save it */
				return new CrudServiceImpl<ReleaseVersion>(em).create(relver);
			}
		
		});
	}

    /**
     * Creates a ReleaseVersionDeployment record
     * @param em
     * @param releaseVersionId
     * @param environment
     * @param dateStarted
     * @param dateCompleted
     * @param log
     * @param plan
     * @param status
     * @param succeeded
     * @param jenkinsBuildNumber
     * @param jenkinsJobName
     * @param jenkinsBuildId
     * @return
     * @throws Exception
     */
     public ReleaseVersionDeployment createReleaseVersionDeployment(EntityManager em, String releaseVersionId, 
    		 String environment, Date dateStarted, Date dateCompleted, String log, String plan, String status, 
    		 Boolean succeeded, int jenkinsBuildNumber, String jenkinsJobName, String jenkinsBuildId) throws Exception {

    	 if (releaseVersionId == null) {
    		 throw new IllegalArgumentException("'releaseVersionId' may not be null");    		
    	 }

    	 ReleaseVersion releaseVersion = new CrudServiceImpl<ReleaseVersion>(em).find(ReleaseVersion.class, Long.parseLong(releaseVersionId));
    	 if (releaseVersion == null) {
    		 throw new Exception("ReleaseVersion with id '" + releaseVersionId + "' cannot be found");
    	 }

    	 ReleaseVersionDeployment dep = new ReleaseVersionDeployment();
    	 dep.setReleaseVersion(releaseVersion);
    	 dep.setDateStarted(dateStarted);
    	 dep.setDateCompleted(dateCompleted);
    	 dep.setEnvironment(environment);
    	 dep.setLog(log);
    	 dep.setPlan(plan);
    	 dep.setStatus(status);
    	 dep.setSucceeded(succeeded);
    	 dep.setJenkinsBuildNumber(jenkinsBuildNumber);
    	 dep.setJenkinsJobName(jenkinsJobName);
    	 dep.setJenkinsBuildId(jenkinsBuildId);

    	 ReleaseVersionDeployment newDep = new CrudServiceImpl<ReleaseVersionDeployment>(em).create(dep);

    	 return newDep;
     }

     /**
      * Creates a ReleaseVersionDeployment record
      * @param stream
      * @param releaseVersionId
      * @param environment
      * @param dateStarted
      * @param dateCompleted
      * @param log
      * @param plan
      * @param status
      * @param succeeded
      * @param jenkinsBuildNumber
      * @param jenkinsJobName
      * @param jenkinsBuildId
      * @return
      * @throws Exception
      */
     public ReleaseVersionDeployment createReleaseVersionDeployment(PrintStream stream, String releaseVersionId, 
    		 String environment, Date dateStarted, Date dateCompleted, String log, String plan, String status, 
    		 Boolean succeeded, int jenkinsBuildNumber, String jenkinsJobName, String jenkinsBuildId) throws Exception {

    	 ConnectionManager connMgr = new ConnectionManager();

    	 try {
    		 connMgr.initialiseConnection(stream, (ConnectionData)GlobalConfig.getConfig());
    		 connMgr.getManager().getTransaction().begin();

    		 ReleaseVersionDeployment relVerDep = createReleaseVersionDeployment(connMgr.getManager(), releaseVersionId, 
    				 environment, dateStarted, dateCompleted, log, plan, status, succeeded, jenkinsBuildNumber, jenkinsJobName, jenkinsBuildId);

    		 stream.println("Committing transaction");
    		 connMgr.getManager().getTransaction().commit();
    		 stream.println("Transaction committed");

    		 stream.println(String.format("Release Version Deployment record created %s", relVerDep));

    		 return relVerDep;

    	 } catch (Exception e) {
    		 e.printStackTrace(stream);
    		 EntityManager em = connMgr.getManager();
    		 if (em != null ){
    			 EntityTransaction t = em.getTransaction();
    			 if (t != null) {
    				 t.rollback();
    			 }
    		 }
    		 stream.println("Exception caught while creating component. " + e.getClass() + " " + e.getMessage());
    		 throw e;
    	 }
    	 finally {
    		 stream.println("Closing entity manager");
    		 if (connMgr.getManager() != null) {
    			 connMgr.closeConnection(stream);
    		 }	
    	 }
     }
}
