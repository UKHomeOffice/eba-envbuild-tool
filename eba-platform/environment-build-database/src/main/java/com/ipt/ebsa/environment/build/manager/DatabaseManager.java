package com.ipt.ebsa.environment.build.manager;

import java.io.PrintStream;

import javax.persistence.EntityManager;

import com.ipt.ebsa.database.manager.ConnectionManager;
import com.ipt.ebsa.database.manager.GlobalConfig;

/**
 * Superclass containing common methods for accessing the database
 *
 */
public class DatabaseManager {

	/** Default logger */
	protected PrintStream logger = System.out;
	
	/** Persistence unit name (from persistence.xml) */
	private static final String PERSISTENCE_UNIT = "ebsa-environment-build-database-persistence-unit";
	
	/**
	 * Constructor
	 */
	public DatabaseManager() {
		super();
	}
	
	/**
	 * Constructor
	 * @param logger
	 */
	public DatabaseManager(PrintStream logger) {
		this.logger = logger;
	}
	
	/**
	 * Initialises a connection to the database
	 * @return
	 * @throws Exception
	 */
	protected ConnectionManager initialiseConnection() throws Exception {
		ConnectionManager connMgr = new ConnectionManager();
		try {
			connMgr.initialiseConnection(logger, GlobalConfig.getInstance().getConfig(), PERSISTENCE_UNIT);
		} catch (Exception e) {
			connMgr.closeConnection(logger);
			throw e;
		}
		return connMgr;
	}

	/**
	 * Rollback any active transaction
	 * @param manager
	 */
	protected void rollback(EntityManager manager) {
		if (manager.getTransaction().isActive()) {
			manager.getTransaction().rollback();
		}
	}
}
