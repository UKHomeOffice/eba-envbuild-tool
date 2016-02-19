package com.ipt.ebsa.buildtools.release.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.buildtools.release.DBTest;

public class ReleaseVersionTest extends DBTest {
	
	/**
	 * End-to-end test which creates and loads db rows relating to the addition entities added
	 * for self-service 3.
	 */
	@Test
	public void endToEnd() {
	    try {
	    	getEntityManager().getTransaction().begin();
	    	
	    	// Create Application
	    	Application application = new Application();
	    	application.setName("Application Layer");
	    	application.setShortName("APP");
	    	getEntityManager().persist(application);
	    	
	    	// Create ApplicationVersion linked to Application
			ApplicationVersion applicationVersion = new ApplicationVersion();
			applicationVersion.setName("ApplicationRelease");
			applicationVersion.setDateOfRelease(new Date());
			applicationVersion.setNotes("These are the application notes that we are making");
			applicationVersion.setApplication(application);
			applicationVersion.setVersion("2.0.1");
			
			// Create ComponentVersion linked to Application
			ComponentVersion componentVersion = new ComponentVersion();
			componentVersion.setApplication(application);
			componentVersion.setName("ComponentRelease");
			componentVersion.setClassifier("ConfigRPM");
			componentVersion.setDateOfRelease(new Date());
			componentVersion.setNotes("These are the component notes that we are making");
			componentVersion.setType("Type1");
			componentVersion.setComponentVersion("1.0.1");
			getEntityManager().persist(componentVersion);
			
			// Add ComponentVersion to ApplicationVersion 
			List<ComponentVersion> components = new ArrayList<ComponentVersion>();
			components.add(componentVersion);
			applicationVersion.setComponents(components);
			getEntityManager().persist(applicationVersion);

			// Create ReleaseVersion
			ReleaseVersion releaseVersion = new ReleaseVersion();
			releaseVersion.setName("ReleaseVersion test");
			releaseVersion.setDateOfRelease(new Date());
			releaseVersion.setNotes("These are the release version notes that we are making");
			releaseVersion.setVersion("1.0.0");
			
			// Add ApplicationVersion to ReleaseVersion
			List<ApplicationVersion> applicationVersions = new ArrayList<ApplicationVersion>();
			applicationVersions.add(applicationVersion);
			releaseVersion.setApplicationVersions(applicationVersions);
			getEntityManager().persist(releaseVersion);

			getEntityManager().flush();
			getEntityManager().getTransaction().commit();

			// Clear EntityManager to force reload from DB
			getEntityManager().clear();
			getEntityManager().getTransaction().begin();
			
			// Check objects loaded from DB have the same contents as those we created
			Application savedApplication = getEntityManager().find(Application.class, 1L);
			Assert.assertTrue(EqualsBuilder.reflectionEquals(application, savedApplication));
			
			ApplicationVersion savedApplicationVersion = getEntityManager().find(ApplicationVersion.class, 1L);
			Assert.assertTrue(EqualsBuilder.reflectionEquals(applicationVersion, savedApplicationVersion, new String[] {"application", "components", "releaseVersions"}));
			Assert.assertTrue(EqualsBuilder.reflectionEquals(applicationVersion.getApplication(), savedApplicationVersion.getApplication()));
			Assert.assertEquals(1, savedApplicationVersion.getComponents().size());
			Assert.assertEquals(1, savedApplicationVersion.getReleaseVersions().size());
			
			ComponentVersion savedComponentVersion = getEntityManager().find(ComponentVersion.class, 1L);
			Assert.assertTrue(EqualsBuilder.reflectionEquals(componentVersion, savedComponentVersion, new String[] {"application", "applications"}));
			Assert.assertTrue(EqualsBuilder.reflectionEquals(componentVersion.getApplication(), savedComponentVersion.getApplication()));
			Assert.assertEquals(1, savedComponentVersion.getApplications().size());

			ReleaseVersion savedReleaseVersion = getEntityManager().find(ReleaseVersion.class, 1L);
			Assert.assertTrue(EqualsBuilder.reflectionEquals(releaseVersion, savedReleaseVersion, new String[] {"applicationVersions"}));
			Assert.assertEquals(1, savedReleaseVersion.getApplicationVersions().size());
			
			getEntityManager().getTransaction().rollback();
		} finally {
    		getEntityManager().close();
	    }
	}
}
