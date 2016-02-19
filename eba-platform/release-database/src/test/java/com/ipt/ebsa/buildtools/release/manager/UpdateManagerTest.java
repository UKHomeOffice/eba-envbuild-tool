package com.ipt.ebsa.buildtools.release.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.buildtools.release.DBTest;
import com.ipt.ebsa.buildtools.release.entities.Application;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersionDeployment;
import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersionDeployment;

public class UpdateManagerTest extends DBTest {
    @Test
    public void testCreateApplication() throws Exception {
        UpdateManager updateManager = new UpdateManager();
        Application a = updateManager.createApplication(System.out,"APP", "Application Layer");
        ComponentVersion v = updateManager.createComponentVersion(System.out, "APP", "cn", "groupId", "artifactId", 
        		"version", "packaging", "classifier", "type", "rpmPackageName", "rpmPackageVersion", "notes", 1, "projectName", "buildId");
        Assert.assertNotNull(a.getId());
        Assert.assertNotNull(v.getId());
    }

	@Test
	public void testCreateApplicationAndComponentVersion() throws Exception {
    	getEntityManager().getTransaction().begin();
    	
    	Application application = new Application();
    	application.setName("Application Layer");
    	application.setShortName("applicationShortName");
    	
    	CrudService<Application> cs = new CrudServiceImpl<Application>(getEntityManager());
    	cs.create(application);
    	 
		UpdateManager updateManager = new UpdateManager();
		ComponentVersion v = updateManager.createComponentVersion(getEntityManager(), "applicationShortName", "componentName", 
				"groupId", "artifactId", "version", "packaging", "classifier", "type", "rpmPackageName", "rpmPackageVersion", 
				"notes", 1, "JobName", "JenkinsBuildId");
		
		CrudService<ComponentVersion> cscv = new CrudServiceImpl<ComponentVersion>(getEntityManager());
		ComponentVersion cv = cscv.find(ComponentVersion.class, v.getId());
		
		Assert.assertNotNull(cv);
		
		getEntityManager().getTransaction().commit();
	}
	
	
	@Test
	public void testCreateApplicationVersion() throws Exception {
    	getEntityManager().getTransaction().begin();
    	
        UpdateManager updateManager = new UpdateManager();
    	ApplicationVersion av = createApplicationVersion(updateManager);
		Assert.assertEquals(3, av.getComponents().size());
		
		getEntityManager().getTransaction().commit();
	}
	
	@Test
	public void testCreateApplicationVersionDeployment() throws Exception {
    	getEntityManager().getTransaction().begin();
    	
    	UpdateManager updateManager = new UpdateManager();
    	ApplicationVersion av = createApplicationVersion(updateManager);
		Assert.assertEquals(3, av.getComponents().size());
		
		ApplicationVersionDeployment d = updateManager.createApplicationVersionDeployment(getEntityManager(), av.getId().toString(), "test", new Date(), new Date(), "big log", "big plan", "good", Boolean.TRUE, 1, "JobName", "JenkinsBuildId");
		Assert.assertNotNull(d);
		
		getEntityManager().getTransaction().commit();
	}

	/**
	 * Creates an Application Version
	 * @param updateManager
	 * @return
	 * @throws Exception
	 */
	private ApplicationVersion createApplicationVersion(UpdateManager updateManager) throws Exception {
		Application application = new Application();
		application.setName("Application Layer");
		application.setShortName("applicationShortName1");
		CrudService<Application> cs = new CrudServiceImpl<Application>(getEntityManager());
		application = cs.create(application);
		
		ComponentVersion v1 = updateManager.createComponentVersion(getEntityManager(), application.getShortName(), "componentNameA",
				"groupIdA", "artifactId1", "1.0.0", "war", "classifier", "type", "rpmPackageName", "rpmPackageVersion", "notes", 1, "JobName", "JenkinsBuildId");
		ComponentVersion v2 = updateManager.createComponentVersion(getEntityManager(), application.getShortName(), "componentNameB", "groupIdB",
				"artifactId2", "1.0.1", "war", "classifier", "type", "rpmPackageName", "rpmPackageVersion", "notes", 1, "JobName", "JenkinsBuildId");
		ComponentVersion v3 = updateManager.createComponentVersion(getEntityManager(), application.getShortName(), "componentNameC", "groupIdC",
				"artifactId2", "1.0.0", "war", "classifier", "type", "rpmPackageName", "rpmPackageVersion", "notes", 1, "JobName", "JenkinsBuildId");
		
		@SuppressWarnings("unused") // we're creating another component version but we're testing it doesn't affect the application version
		ComponentVersion v4 = updateManager.createComponentVersion(getEntityManager(), application.getShortName(), "componentNameD", "groupIdD",
				"artifactId1", "1.0.0", "war", "classifier", "type", "rpmPackageName", "rpmPackageVersion", "notes", 1, "JobName", "JenkinsBuildId");

		List<Long> ids = new ArrayList<Long>();
		ids.add(v1.getId());
		ids.add(v2.getId());
		ids.add(v3.getId());
		
		ApplicationVersion av = updateManager.createApplicationVersion(getEntityManager(), application.getId().toString(), "name", "notes", "relatedJiraIssue", "relatedBrpIssue", "val", 1, "JobName", "JenkinsBuildId", ids);
		Assert.assertNotNull(av);
		
		Assert.assertTrue(av.getComponents().size() == ids.size());
		return av;
	}
	
	/**
	 * Creates a Release Version
	 * @param updateManager
	 * @return
	 * @throws Exception
	 */
	private ReleaseVersion createReleaseVersion(UpdateManager updateManager) throws Exception {
    	ApplicationVersion av = createApplicationVersion(updateManager);
    	List<Long> ids = Collections.singletonList(av.getId());
    	ReleaseVersion rv = updateManager.createReleaseVersion(getEntityManager(), "Rel 1", "notes", "relatedJiraIssue", "val", 1, "JenkinsJobName", "JenkinsBuildId", ids);
    	return rv;
	}
	@Test
	public void testCreateReleaseVersion() throws Exception {
	    try {
	    	getEntityManager().getTransaction().begin();
	    	
            UpdateManager updateManager = new UpdateManager();
            ReleaseVersion rv = createReleaseVersion(updateManager);
            Assert.assertNotNull(rv);
            Assert.assertNotNull(rv.getId());
            Assert.assertTrue(rv.getId() > 0L);
            
			getEntityManager().getTransaction().commit();
			
	    } catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	    finally {
	    	if (getEntityManager() != null) {
	    		getEntityManager().close();
	    	}
	    }
	}
	
	@Test
	public void testCreateReleaseVersionDeployment() throws Exception {
	    try {
	    	getEntityManager().getTransaction().begin();
	    	
	    	UpdateManager updateManager = new UpdateManager();
	    	ReleaseVersion rv = createReleaseVersion(updateManager);
			
			ReleaseVersionDeployment d = updateManager.createReleaseVersionDeployment(getEntityManager(), rv.getId().toString(), "test", new Date(), new Date(), "big log", "big plan", "good", Boolean.TRUE, 1, "JenkinsJobName", "JenkinsBuildId");
			Assert.assertNotNull(d);
			Assert.assertNotNull(d.getId());
			Assert.assertTrue(d.getId() > 0L);
			
			getEntityManager().getTransaction().commit();
			
	    } catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	    finally {
	    	if (getEntityManager() != null) {
	    		getEntityManager().close();
	    	}
	    }
	}
	
	/**
	 * EBSAD-22317
	 * @throws Exception 
	 */
	@Test
	public void testCreateApplicationVersionNoComponents() throws Exception {
		 try {
	    	getEntityManager().getTransaction().begin();
	    	
	    	UpdateManager updateManager = new UpdateManager();
	
			createApplicationVersion(updateManager);
			
			String applicationId = "1";
			String name = "";
			String notes = "";
			String relatedJiraIssue = "";
			String relatedBrpIssue = "";
			String version = "";
			String jenkinsJobName = "";
			String jenkinsBuildId = "";
			int jenkinsBuildNumber = 0;
			Object componentIds = new ArrayList();
			ApplicationVersion empty = updateManager.createApplicationVersion(getEntityManager(), applicationId, name, notes, relatedJiraIssue, relatedBrpIssue, version, jenkinsBuildNumber, jenkinsJobName, jenkinsBuildId, componentIds);
			
			ApplicationVersion retrieved = new ReadManager().getApplicationVersion(getEntityManager(), empty.getId().toString());
			Assert.assertTrue(retrieved.getComponents().isEmpty());
			
			getEntityManager().getTransaction().commit();
	
	    } catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
	    	if (getEntityManager() != null) {
	    		getEntityManager().close();
	    	}
	    }
	}
}
