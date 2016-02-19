package com.ipt.ebsa.manage.deploy.database;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import com.ipt.ebsa.buildtools.release.entities.Application;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.buildtools.release.manager.UpdateManager;

public class DBTestUtil {
	
	private static int COMP_NAME = 0;
	private static int GROUP_ID = 1;
	private static int ARTIFACTID = 2;
	private static int COMP_VERSION = 3;
	private static int RPM_PACKAGE_NAME = 4;
	private static int RPM_PACKAGE_VERSION = 5;
	
	public static void clearDownDB(EntityManager entityManager) throws Exception {
		try {
			
			entityManager.getTransaction().begin();
			entityManager.createNativeQuery("delete from RELEASE_MANAGEMENT.ApplicationVersion_ComponentVersion").executeUpdate();
			entityManager.createNativeQuery("delete from RELEASE_MANAGEMENT.ApplicationVersion").executeUpdate();
			entityManager.createNativeQuery("delete from RELEASE_MANAGEMENT.ComponentVersion").executeUpdate();
			entityManager.createNativeQuery("delete from RELEASE_MANAGEMENT.Application").executeUpdate();
			entityManager.getTransaction().commit();

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * Creates an application, related component versions and finally an ApplicationVersion to go with it, all based on the data passed in
	 * @param appShortNamme
	 * @param appLongName
	 * @param componentInfo
	 * @return
	 * @throws Exception
	 */
	public static ApplicationVersion createApplicationVersion(EntityManager entityManager, String appShortNamme, 
			String appLongName, String[][] componentInfo) throws Exception {
		entityManager.getTransaction().begin();
		
		Application application = new UpdateManager().createApplication(entityManager, appShortNamme, appLongName, "role");
    	
		List<Long> ids = new ArrayList<Long>();
    	
    	if (componentInfo != null && componentInfo.length > 0) {
	    	for (int i = 0; i < componentInfo.length; i++) {
	    		if (componentInfo[i] != null) {
	    		   ids.add(createCV(entityManager, appShortNamme, 
	    				   componentInfo[i][COMP_NAME], componentInfo[i][GROUP_ID], 
	    				   componentInfo[i][ARTIFACTID], componentInfo[i][COMP_VERSION],
	    				   componentInfo[i][RPM_PACKAGE_NAME], componentInfo[i][RPM_PACKAGE_VERSION]));
	    		}
			}
    	}
    	ApplicationVersion av = new UpdateManager().createApplicationVersion(entityManager,application.getId().toString(), "AVName", "notes", "relatedJiraIssue", "relatedBRPIssue", "1.1.0", 1, "JobName", "JenkinsBuildId",ids.size() < 1 ? null : ids);
    	entityManager.getTransaction().commit(); 
		return av;
	}
	
	public static Long createCV(EntityManager entityManager, String applicationShortName, String componentName, String groupId, String artifactId, String componentVersion, String rpmPackageName, String rpmPackageVersion) throws Exception  {
		ComponentVersion v = new UpdateManager().createComponentVersion(entityManager, applicationShortName, componentName, groupId, artifactId, componentVersion, "war", null, "rpm", rpmPackageName, rpmPackageVersion, null, 1, "projectName", "buildId");
		return v.getId();
	}
}
