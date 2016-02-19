package com.ipt.ebsa.buildtools.release.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.ipt.ebsa.buildtools.release.DBTest;

public class ApplicationReleaseTest extends DBTest{
	@Test
	public void testCreateApplicationAndComponents() throws Exception {
	    try {
	    	getEntityManager().getTransaction().begin();
	    	
	    	Application application = new Application();
	    	application.setName("Application Layer");
	    	application.setShortName("APP");
	    	getEntityManager().persist(application);
	    	
			ApplicationVersion applicationRelease = new ApplicationVersion();
			applicationRelease.setName("ApplicationRelease");
			applicationRelease.setDateOfRelease(new Date());
			applicationRelease.setNotes("These are the application notes that we are making");
			applicationRelease.setApplication(application);
			applicationRelease.setVersion("2.0.1");
			
			ComponentVersion component = new ComponentVersion();
			component.setApplication(application);
			component.setName("ComponentRelease");
			component.setClassifier("ConfigRPM");
			component.setDateOfRelease(new Date());
			component.setNotes("These are the component notes that we are making");
			component.setType("Type1");
			component.setComponentVersion("1.0.1");
			
			List<ComponentVersion> components = new ArrayList<ComponentVersion>();
			components.add(component);
			applicationRelease.setComponents(components);
			
			getEntityManager().persist(component);
			getEntityManager().persist(applicationRelease);
			getEntityManager().flush();
						
			getEntityManager().getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
	    }
	}
}
