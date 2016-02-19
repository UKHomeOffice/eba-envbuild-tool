package com.ipt.ebsa.manage.deploy.database;

import javax.persistence.EntityManager;

import com.ipt.ebsa.buildtools.release.manager.ConnectionManager;
import com.ipt.ebsa.manage.Configuration;

/**
 * Contains files for loading Release data into the model
 * @author scowx
 *
 */
public class DatabaseManager {

	private ConnectionManager connectionManager = null;
	private EntityManager entityManager = null;
    
    /**
     * Initialises the connection to the database
     * @throws Exception
     */
	public void initialise() throws Exception {
		/* Get a connection to the database */
		connectionManager = getConnectionManager();
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
	private ConnectionManager getConnectionManager() throws Exception {
		ConnectionManager connectionManager = new ConnectionManager();
		connectionManager.initialiseConnection(
				System.out,
				Configuration.getJdbcDriver(),
				Configuration.getJdbcUrl(),
				Configuration.getJdbcUsername(), 
				Configuration.getJdbcPassword(), 
				Configuration.getJdbcSchema(), 
				Configuration.getJdbcHbm2ddlAuto(), 
				Configuration.getJdbcDialect());
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
