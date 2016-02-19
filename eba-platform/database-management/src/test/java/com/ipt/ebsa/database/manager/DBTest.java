package com.ipt.ebsa.database.manager;

import java.io.File;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

public class DBTest {

	private static final String PATH_TO_TEST_DB = "target/h2db.mv.db";

	private static final String PATH_TO_EXAMPLE_DB = "src/test/resources/h2db/h2db.mv.db";

	private ConnectionManager connectionManager;

	private EntityManager entityManager;

	public EntityManager getEntityManager() {
		return entityManager;
	}
	
	/** Connection data for the test db. */
	private static final ConnectionData connectionData = new ConnectionData() {
		public String getUsername() {
			return "DB_MANAGEMENT";
		}
		public String getUrl() {
			return "jdbc:h2:./target/h2db";
		}
		public String getSchema() {
			return "DB_MANAGEMENT";
		}
		public String getPassword() {
			return "DB_MANAGEMENT";
		}
		public String getDriverClass() {
			return "org.h2.Driver";
		}
		public String getAutodll() {
			return "validate";
		}
		public String getDialect() {
			return "org.hibernate.dialect.H2Dialect";
		}
	};
	
	@Before
	public void setUp() throws Exception {
		System.out.println("Opening up database connection");
		
		try {
			//First copy the example over to the target folder (we are going to work with the copied one)
            try {
				FileUtils.copyFile(new File(PATH_TO_EXAMPLE_DB), new File(PATH_TO_TEST_DB));
			} catch (Exception e) {
				//Sometimes this gets called before the connection is completely down.  Lets wait a few seconds and try again
				System.err.println("Failed to copy first time. Trying again");
				Thread.sleep(5000);
				FileUtils.copyFile(new File(PATH_TO_EXAMPLE_DB), new File(PATH_TO_TEST_DB));
			}
            
            connectionManager = new ConnectionManager();
			connectionManager.initialiseConnection(System.out, connectionData, "ebsa-database-management-persistence-unit");
			
			entityManager = connectionManager.getManager();
		
			GlobalConfig.getInstance().setSharedConnectionData(connectionData);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	@After
	public void disconnect() {
		if (entityManager != null) {
			if (entityManager.isOpen()) {
				entityManager.close();
			}
			// EntityManagerFactory must be closed otherwise the target/h2db.mv.db file remains locked on Windows so it can't be overwritten for the next test
			EntityManagerFactory factory = entityManager.getEntityManagerFactory();
			if (factory != null && factory.isOpen()) {
				factory.close();
			}
		}
	}
}
