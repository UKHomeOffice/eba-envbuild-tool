package com.ipt.ebsa.buildtools.release.manager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.buildtools.release.DBTest;
import com.ipt.ebsa.buildtools.release.entities.Application;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersionDeployment;
import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersionDeployment;

public class ReadManagerTest extends DBTest {
	
	private Application application1;
	private Application application2;
	private List<Long> componentIds1;
	private List<Long> componentIds2;
	private ApplicationVersion appVer1;
	private ApplicationVersion appVer2;
	private ApplicationVersionDeployment appVerDep;
	private ReleaseVersion relVer;
	private ReleaseVersionDeployment relVerDep;
	
	/**
	 * Before every test
	 * @throws Exception
	 */
	@Before
	public void setUpData() throws Exception {
		/*Start transaction*/
		getEntityManager().getTransaction().begin();
		
		/* Create test data */
		application1 = createApplication("Application Layer 1");
		application2 = createApplication("Application Layer 2");
		componentIds1 = createComponentVersions(application1);
		componentIds2 = createComponentVersions(application2);
		appVer1 = createApplicationVersion(application1, componentIds1);
		appVer2 = createApplicationVersion(application2, componentIds2);
		appVerDep = createApplicationVersionDeployment(appVer1);
		List<Long> appVersionIds = Arrays.asList(new Long[] {appVer1.getId(), appVer2.getId()});
		relVer = createReleaseVersion("Release 1", appVersionIds);
		relVerDep = createReleaseVersionDeployment(relVer);
		
		/* Commit so test data is visible to other connections so we can also test the methods that don't take an EntityManager */
		getEntityManager().getTransaction().commit();
		getEntityManager().getTransaction().begin();
	}
	
	@After
	public void tearDown() throws Exception {
		/*End transaction*/
		try {
			getEntityManager().getTransaction().commit();
		}
	    finally {
	    	if (getEntityManager() != null) {
	    		getEntityManager().close();
	    	}
	    }
	}
	
	@Test
	public void testGetApplications() throws Exception {
		List<Application> v = new ReadManager().getApplications(getEntityManager());
		assertNotNull(v);
		assertEquals(2, v.size());
	}
	
	@Test
	public void testGetApplicationsVersions() throws Exception {
		List<ApplicationVersion> apps = new ReadManager().getApplicationVersions(getEntityManager());
		assertNotNull(apps);
		assertEquals(2, apps.size());
	}
	
	@Test
	public void testGetApplicationVersionsForApplication() throws Exception {
		List<ApplicationVersion> apps = new ReadManager().getApplicationVersions(getEntityManager(), application1.getId().toString());
		assertNotNull(apps);
		assertTrue(apps.size() == 1);
	}
	
	@Test
	public void testGetComponentsByApplicationId() throws Exception {
		CrudService<ComponentVersion> cscv = new CrudServiceImpl<ComponentVersion>(getEntityManager());
		List<ComponentVersion> list1 = cscv.findWithNamedQuery(ComponentVersion.class, "findAllUsingApplication",
				ParamFactory.with("application", application1).parameters());
				
		assertNotNull(list1);
		
		List<String> list2 = new ReadManager().getUniqueComponentNames(getEntityManager(), application1.getId().toString());
		assertNotNull(list2);
		
		for (ComponentVersion componentVersion : list1) {
			System.out.println(componentVersion.getName());
			Assert.assertTrue(list2.contains(componentVersion.getName()));
			Assert.assertNotNull(componentVersion.getCiStatus());
			Assert.assertNotNull(componentVersion.getCbtStatus());
		}
	}
	
	@Test
	public void testGetComponentsVersionsForComponentName() throws Exception {
		List<ComponentVersion> list1 = new ReadManager().getComponentVersions(getEntityManager(),"componentNameA");
		assertNotNull(list1);
		assertEquals(4, list1.size());
		assertEquals("1.0.1",list1.get(0).getComponentVersion());
		assertEquals("1.0.0",list1.get(1).getComponentVersion());
	}
	
	@Test
	public void testGetComponentsByName() throws Exception {
		Map<String, Set<ComponentVersion>> map = new ReadManager().getComponentsByName(application1.getId().toString());
		System.out.println(map);
		assertEquals(3, map.size());
		for (String name : new String[]{"2:componentNameA","1:componentNameB", "1:componentNameC"}) {
			assertEquals(name, new Integer(name.substring(0,1)).intValue(), map.get(name.substring(2)).size());
		}
	}
	
	@Test
	public void testGetAllApplicationVersionDeployments() throws Exception {
		List<ApplicationVersionDeployment> list = new ReadManager().getAllApplicationVersionDeployments(getEntityManager());
		assertNotNull(list);
		assertEquals(1, list.size());
		assertTrue(EqualsBuilder.reflectionEquals(appVerDep, list.get(0)));
	}
	
	@Test
	public void testGetApplicationVersionDeploymentsUsingAppVer() throws Exception {
		List<ApplicationVersionDeployment> list = new ReadManager().getApplicationVersionDeployments(getEntityManager(), appVer1.getId().toString());
		assertNotNull(list);			
		assertEquals(1, list.size());
		assertTrue(EqualsBuilder.reflectionEquals(appVerDep, list.get(0)));
	}
	
	@Test
	public void testHasComponentVersionTrueNoClassifier() throws Exception {
		try {
			boolean answer = new ReadManager().hasComponentVersion(getEntityManager(), "groupIdA", "artifactId1", "1.0.0", null, "Application Layer 1");
			Assert.assertTrue(answer);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
	    }
	}
	
	@Test
	public void testHasComponentVersionTrueWithClassifier() throws Exception {
		try {
			boolean answer = new ReadManager().hasComponentVersion(getEntityManager(), "groupIdA", "artifactId1", "1.0.0", "classifier1", "Application Layer 1");
			Assert.assertTrue(answer);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
	    }
	}
	
	@Test
	public void testHasComponentVersionFalseNoClassifier() throws Exception {
		try {
			boolean answer = new ReadManager().hasComponentVersion(getEntityManager(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), null, "applicationShortName");
			Assert.assertFalse(answer);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
	    }
	}
	
	@Test
	public void testHasComponentVersionFalseWithClassifier() throws Exception {
		try {
			boolean answer = new ReadManager().hasComponentVersion(getEntityManager(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), "applicationShortName");
			Assert.assertFalse(answer);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
	    }
	}
	
	@Test
	public void testHasComponentVersionFalseNoClassifierDifferentApplication() throws Exception {
		try {
			boolean answer = new ReadManager().hasComponentVersion(getEntityManager(), "groupIdA", "artifactId1", "1.0.0", null, "applicationShortName2");
			Assert.assertFalse(answer);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
	    }
	}
	
	@Test
	public void testHasComponentVersionFalseWithClassifierDifferentApplication() throws Exception {
		try {
			boolean answer = new ReadManager().hasComponentVersion(getEntityManager(), "groupIdA", "artifactId1", "1.0.0", "classifier", "applicationShortName2");
			Assert.assertFalse(answer);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
	    }
	}
	
	@Test
	public void testHasApplicationVersionTrue() throws Exception {
		boolean answer = new ReadManager().hasApplicationVersion(getEntityManager(), "name", "val");
		assertTrue(answer);
	}
	
	@Test
	public void testHasApplicationVersionFalse() throws Exception {
		boolean answer = new ReadManager().hasApplicationVersion(getEntityManager(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
		assertFalse(answer);
	}
	
	@Test
	public void testHasApplicationVersionForAppTrue() throws Exception {
		boolean answer = new ReadManager().hasApplicationVersionForApp(getEntityManager(), appVer1.getApplication().getId(), appVer1.getVersion());
		assertTrue(answer);
	}
	
	@Test
	public void testHasApplicationVersionForAppFalse() throws Exception {
		boolean answer = new ReadManager().hasApplicationVersionForApp(getEntityManager(), appVer1.getApplication().getId(), UUID.randomUUID().toString());
		assertFalse(answer);
	}
	
	@Test
	public void testGetApplicationVersion() throws Exception {
    	// Re-read the ApplicationVersion
		getEntityManager().clear();
		ApplicationVersion appVersion = new ReadManager().getApplicationVersion(appVer1.getId().toString());
		assertNotNull(appVersion);
		assertTrue(EqualsBuilder.reflectionEquals(appVer1, appVersion, new String[] {"application", "components", "releaseVersions"}));
		assertEquals(appVersion.getComponents().size(), appVersion.getComponents().size());
	}
	
	@Test
	public void testGetReleaseVersions() throws Exception {
		List<ReleaseVersion> relVers = new ReadManager().getReleaseVersions(getEntityManager());
		assertNotNull(relVers);
		assertEquals(1, relVers.size());
	}
	
	@Test
	public void testGetAllReleaseVersionDeployments() throws Exception {
		List<ReleaseVersionDeployment> list = new ReadManager().getAllReleaseVersionDeployments(getEntityManager());
		assertNotNull(list);
		assertEquals(1, list.size());
		assertTrue(EqualsBuilder.reflectionEquals(relVerDep, list.get(0)));
	}
	
	@Test
	public void testGetReleaseVersionDeploymentsUsingRelVer() throws Exception {
		List<ReleaseVersionDeployment> list = new ReadManager().getReleaseVersionDeployments(getEntityManager(), relVer.getId().toString());
		assertNotNull(list);
		assertEquals(1, list.size());
	}
	
	@Test
	public void testGetReleaseVersionDeploymentsUsingRelVerNoneFound() throws Exception {
		List<ReleaseVersionDeployment> list = new ReadManager().getReleaseVersionDeployments(getEntityManager(), "-1");
		assertNotNull(list);
		assertEquals(0, list.size());
	}
	
	@Test
	public void testHasReleaseVersionTrue() throws Exception {
		boolean answer = new ReadManager().hasReleaseVersion(getEntityManager(), "Release 1", "val");
		assertTrue(answer);
	}
	
	@Test
	public void testHasReleaseVersionFalse() throws Exception {
		boolean answer = new ReadManager().hasReleaseVersion(getEntityManager(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
		assertFalse(answer);
	}
	
    private ApplicationVersionDeployment createApplicationVersionDeployment(ApplicationVersion av) throws Exception {
    	UpdateManager updateManager = new UpdateManager();
		ApplicationVersionDeployment d = updateManager.createApplicationVersionDeployment(getEntityManager(), av.getId().toString(), "test", new Date(), new Date(), "big log", "big plan", "good", Boolean.TRUE, 1, "JobName", "JenkinsBuildId");
		return d;
    }
	
    /**
     * Creates an app ver
     * @throws Exception
     */
	private ApplicationVersion createApplicationVersion(Application application, List<Long> ids) throws Exception {
		UpdateManager updateManager = new UpdateManager();
		ApplicationVersion av = updateManager.createApplicationVersion(getEntityManager(), application.getId().toString(), "name", "notes", "relatedJiraIssue","relatedBrpIssue", "val", 1, "JobName", "JenkinsBuildId", ids);
		assertNotNull(av);
		return av;
	}

	/**
	 * Create an application
	 * @return
	 */
	private Application createApplication(String name) {
		Application application = new Application();
		application.setName(name);
		application.setShortName(name);
		CrudService<Application> cs = new CrudServiceImpl<Application>(getEntityManager());
		application = cs.create(application);
		return application;
	}	

	/**
	 * Create component versions
	 * @param application
	 * @throws Exception
	 */
	private List<Long> createComponentVersions(Application application) throws Exception {
		UpdateManager updateManager = new UpdateManager();
		ComponentVersion v = updateManager.createComponentVersion(getEntityManager(), application.getShortName(), "componentNameA",
				"groupIdA", "artifactId1", "1.0.0", "war", "classifier" + application.getId(), "type", "rpmPackageName", "rpmPackageVersion", "notes", 1, "JobName", "JenkinsBuildId", Boolean.FALSE, Boolean.FALSE);
		ComponentVersion v1 = updateManager.createComponentVersion(getEntityManager(), application.getShortName(), "componentNameA",
				"groupIdA", "artifactId1", "1.0.1", "war", "classifier" + application.getId(), "type", "rpmPackageName", "rpmPackageVersion", "notes", 1, "JobName", "JenkinsBuildId", Boolean.FALSE, Boolean.TRUE);
		ComponentVersion v2 = updateManager.createComponentVersion(getEntityManager(), application.getShortName(), "componentNameB",
				"groupIdB", "artifactId2", "1.0.0", "war", "classifier" + application.getId(), "type", "rpmPackageName", "rpmPackageVersion", "notes", 1, "JobName", "JenkinsBuildId", Boolean.TRUE, Boolean.FALSE);
		ComponentVersion v3 = updateManager.createComponentVersion(getEntityManager(), application.getShortName(), "componentNameC",
				"groupIdC", "artifactId2", "1.0.0", "war", "classifier" + application.getId(), "type", "rpmPackageName", "rpmPackageVersion", "notes", 1, "JobName", "JenkinsBuildId", Boolean.TRUE, Boolean.TRUE);
					
		List<Long> ids = new ArrayList<Long>();
		ids.add(v.getId());
		ids.add(v1.getId());
		ids.add(v2.getId());
		ids.add(v3.getId());
		
		return ids;
	}
	
    /**
     * Creates a Release Version
     * @throws Exception
     */
	private ReleaseVersion createReleaseVersion(String name, List<Long> appVersionIds) throws Exception {
		UpdateManager updateManager = new UpdateManager();
		ReleaseVersion rv = updateManager.createReleaseVersion(getEntityManager(), name, "notes", "relatedJiraIssue", "val", 1, "JobName", "JenkinsBuildId", appVersionIds);
		assertNotNull(rv);
		return rv;
	}
	
    /**
     * Creates a Release Version Deployment
     * @throws Exception
     */
    private ReleaseVersionDeployment createReleaseVersionDeployment(ReleaseVersion releaseVersion) throws Exception {
    	UpdateManager updateManager = new UpdateManager();
		ReleaseVersionDeployment d = updateManager.createReleaseVersionDeployment(getEntityManager(), releaseVersion.getId().toString(), "test", new Date(), new Date(), "big log", "big plan", "good", Boolean.TRUE, 1, "JenkinsJobName", "JenkinsBuildId");
		return d;
    }
}
