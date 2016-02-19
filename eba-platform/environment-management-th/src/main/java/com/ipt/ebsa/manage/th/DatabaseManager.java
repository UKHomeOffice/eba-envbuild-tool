package com.ipt.ebsa.manage.th;

import java.util.Properties;

import javax.persistence.EntityManager;

import com.ipt.ebsa.buildtools.release.manager.ConnectionManager;

/**
 * Contains files for loading Release data into the model
 * @author scowx
 *
 */
public class DatabaseManager {

	private ConnectionManager connectionManager = null;
	private EntityManager entityManager = null;
	
	public static final String	DATABASE_JDBC_HBM2DDL_AUTO										= "database.jdbc.hbm2ddl.auto";
	public static final String	DATABASE_JDBC_HIBERNATE_DIALECT									= "database.jdbc.hibernateDialect";
	public static final String	DATABASE_JDBC_SCHEMA											= "database.jdbc.schema";
	public static final String	DATABASE_JDBC_PASSWORD											= "database.jdbc.password";
	public static final String	DATABASE_JDBC_USERNAME											= "database.jdbc.username";
	public static final String	DATABASE_JDBC_URL												= "database.jdbc.url";
	public static final String	DATABASE_JDBC_DRIVER											= "database.jdbc.driver";
	
    
    /**
     * Initialises the connection to the database
     * @throws Exception
     */
	public void initialise(Properties properties) throws Exception {
		/* Get a connection to the database */
		connectionManager = getConnectionManager(properties);
		entityManager = connectionManager.getManager();		
	}
	
	/**
	 * Closes the connection to the database
	 * @throws Exception
	 */
	public void finalise() throws Exception {
		if (connectionManager != null) {
			connectionManager.closeConnection(System.out);
		}
	}
	
	/**
	 * Gets a connection manager
	 * @return
	 * @throws Exception
	 */
	private ConnectionManager getConnectionManager(Properties properties) throws Exception {
		ConnectionManager connectionManager = new ConnectionManager();
		connectionManager.initialiseConnection(
				System.out,
				properties.getProperty(DATABASE_JDBC_DRIVER),
				properties.getProperty(DATABASE_JDBC_URL),
				properties.getProperty(DATABASE_JDBC_USERNAME), 
				properties.getProperty(DATABASE_JDBC_PASSWORD), 
				properties.getProperty(DATABASE_JDBC_SCHEMA),
				properties.getProperty(DATABASE_JDBC_HBM2DDL_AUTO), 
				properties.getProperty(DATABASE_JDBC_HIBERNATE_DIALECT));
		return connectionManager;
	}

	/**
	 * Returns the entity manager (or null if it has not been initialised)
	 * @return
	 */
	public EntityManager getEntityManager() {
		return entityManager;
	}
	
}
