package com.ipt.ebsa.buildtools.release.entities;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.buildtools.release.DBTest;

/**
 * Unit tests for the new foreign keys implemented in EBSAD-10679
 * @author Mark Kendall
 *
 */
public class ForeignKeyTest extends DBTest {
	
	@Test
	public void testApplicationVersion() {
		// Attempt to insert an ApplicationVersion without the existing Application
		String table = connectionData.getSchema() + ".ApplicationVersion";
		insert("insert into " + table + " (name, application_id) values('test', 1)", "FK_APPLICATIONVERSION_APPLICATION");
	}

	@Test
	public void testComponentVersion() {
		// Attempt to insert a ComponentVersion without the existing Application
		String table = connectionData.getSchema() + ".ComponentVersion";
		insert("insert into " + table + " (name, application_id) values('test', 1)", "FK_COMPONENTVERSION_APPLICATION");
	}
	
	@Test
	public void testApplicationVersion_ComponentVersion__NoAppVersion() {
		// Insert a ComponentVersion
		ComponentVersion componentVersion = new ComponentVersion();
		componentVersion.setName("test");
		EntityManager manager = getEntityManager();
		manager.getTransaction().begin();
		manager.persist(componentVersion);
		manager.getTransaction().commit();
		
		// Attempt to insert an ApplicationVersion_ComponentVersion without the existing ApplicationVersion
		String table = connectionData.getSchema() + ".ApplicationVersion_ComponentVersion";
		insert("insert into " + table + " (applications_id, components_id) values(1, " + componentVersion.getId() + ")", "FK_APPLICATIONVERSIONCOMPONENTVERSION_APPLICATIONVERSION");
	}
	
	@Test
	public void testApplicationVersion_ComponentVersion__NoCompVersion() {
		// Insert an ApplicationVersion
		ApplicationVersion appVersion = new ApplicationVersion();
		appVersion.setName("test");
		EntityManager manager = getEntityManager();
		manager.getTransaction().begin();
		manager.persist(appVersion);
		manager.getTransaction().commit();
		
		// Attempt to insert an ApplicationVersion_ComponentVersion without the existing ComponentVersion
		String table = connectionData.getSchema() + ".ApplicationVersion_ComponentVersion";
		insert("insert into " + table + " (applications_id, components_id) values(" + appVersion.getId() + ", 1)", "FK_APPLICATIONVERSIONCOMPONENTVERSION_COMPONENTVERSION");
	}
	
	@Test
	public void testApplicationVersionDeployment() {
		// Attempt to insert an ApplicationVersionDeployment without the existing ApplicationVersion
		String table = connectionData.getSchema() + ".ApplicationVersionDeployment";
		insert("insert into " + table + " (applicationversion_id) values(1)", "FK_APPLICATIONVERSIONDEPLOYMENT_APPLICATIONVERSION");
	}
	
	@Test
	public void testReleaseVersion_ApplicationVersion__NoRelVersion() {
		// Insert an ApplicationVersion
		ApplicationVersion appVersion = new ApplicationVersion();
		appVersion.setName("test");
		EntityManager manager = getEntityManager();
		manager.getTransaction().begin();
		manager.persist(appVersion);
		manager.getTransaction().commit();
		
		// Attempt to insert a ReleaseVersion_ApplicationVersion without the existing ReleaseVersion
		String table = connectionData.getSchema() + ".ReleaseVersion_ApplicationVersion";
		insert("insert into " + table + " (releaseVersions_id, applicationVersions_id) values(1, " + appVersion.getId() + ")", "FK_RELEASEVERSIONAPPLICATIONVERSION_RELEASEVERSION");
	}
	
	@Test
	public void testReleaseVersion_ApplicationVersion__NoAppVersion() {
		// Insert a ReleaseVersion
		ReleaseVersion relVersion = new ReleaseVersion();
		relVersion.setName("test");
		EntityManager manager = getEntityManager();
		manager.getTransaction().begin();
		manager.persist(relVersion);
		manager.getTransaction().commit();
		
		// Attempt to insert an ApplicationVersion_ComponentVersion without the existing ApplicationVersion
		String table = connectionData.getSchema() + ".ReleaseVersion_ApplicationVersion";
		insert("insert into " + table + " (releaseVersions_id, applicationVersions_id) values(" + relVersion.getId() + ", 1)", "FK_RELEASEVERSIONAPPLICATIONVERSION_APPLICATIONVERSION");
	}
	
	@Test
	public void testReleaseVersionDeployment() {
		// Attempt to insert an ReleaseVersionDeployment without the existing ReleaseVersion
		String table = connectionData.getSchema() + ".ReleaseVersionDeployment";
		insert("insert into " + table + " (releaseVersion_id) values(1)", "FK_RELEASEVERSIONDEPLOYMENT_RELEASEVERSION");
	}
	
	/**
	 * Run the given insert SQL and check a ConstraintViolationException is thrown for the violatedConstraintName
	 * @param sql
	 * @param violatedConstraintName
	 */
	private void insert(String sql, String violatedConstraintName) {
		EntityManager manager = getEntityManager();
		manager.getTransaction().begin();
		try {
			manager.createNativeQuery(sql).executeUpdate();
			Assert.fail("Expected a constraint violation exception for constraint: " + violatedConstraintName);
		} catch (PersistenceException pe) {
			Throwable cause = pe.getCause();
			Assert.assertNotNull(cause);
			Assert.assertTrue(cause instanceof ConstraintViolationException);
			Assert.assertTrue(((ConstraintViolationException)cause).getConstraintName().contains(violatedConstraintName));
		} finally {
			manager.getTransaction().rollback();
		}
	}
}
