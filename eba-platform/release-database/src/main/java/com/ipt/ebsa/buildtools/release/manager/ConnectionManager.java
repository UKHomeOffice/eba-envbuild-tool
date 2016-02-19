package com.ipt.ebsa.buildtools.release.manager;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static com.ipt.ebsa.buildtools.release.util.ClassLoaderUtils.*;

public class ConnectionManager {

	private EntityManager manager;

	public EntityManager getManager() {
		return manager;
	}

	public void setManager(EntityManager manager) {
		this.manager = manager;
	}
	
	public void closeConnection(PrintStream log ) {
		if (manager != null) {
			manager.close();
			manager.getEntityManagerFactory().close();
     	    log.println("Entity manager closed");
     	}	
	}

	/**
	 * Initialises the connection by calling out to the GlobalConfig.ReleasePluginGlobalConfigDescriptor to fetch some values
	 * @param printStream
	 * @throws Exception
	 */
	public void initialiseConnection(PrintStream printStream, ConnectionData descriptor) throws Exception {
		printStream.println("Driver class: "+descriptor.getDriverClass());
		printStream.println("JDBC Url: "+descriptor.getUrl());
		printStream.println("Username: "+descriptor.getUsername());
		printStream.println("Schema: "+descriptor.getDriverClass());
		printStream.println("AutoDll: "+descriptor.getAutodll());
		printStream.println("Dialect: "+descriptor.getDialect());
               
        initialiseConnection(printStream, descriptor.getDriverClass(), descriptor.getUrl(), descriptor.getUsername(), descriptor.getPassword(), descriptor.getSchema(), descriptor.getAutodll(),descriptor.getDialect());
	}
	
	/**
	 * Initialises the Entity Manager from the properties passed in by the user.
	 */
	public void initialiseConnection(PrintStream log, String driverClass, String connectionUrl, String username, String password, String schema, String hbm2ddl, String dialect) throws Exception {
		
		/* 
		 * The Jenkins class loader does not play ball nicely.  It does not seem to be able to pick up our persistence XML
		 * because the PersistenceResolver cannot see it on the class path.  It is definitely on a classpath because we 
		 * I have checked in the HPI it just seems that the classpath is being governed from "above" inside Jenkins and so the 
		 * persistence.xml is not on the searchpath of the PersistenceResolver.
		 * In order for the Persistence.xml file to be found at runtime I had to set the ContextClassloader to the one which loaded
		 * all of my plugin classes and then everything works sweetly.  I put it back again when I have made the connection as the persistence.XML 
		 * has already been loaded and does not need to be used again and I do not want undesirable side effects from swapping out the original
		 * ContextClassLoader for my own. 
		 *  */
		
		try {
		    log.println("Connecting to database");
			log.println("connection.driver_class: " + driverClass);
			log.println("hibernate.connection.url: " + connectionUrl);
			log.println("hibernate.connection.password: " + ((password != null && password.length() > 0) ? password.replaceAll(".*", "*") : " password is empty or null"));
			log.println("hibernate.default_schema: " + schema);
			log.println("hibernate.connection.username: " + username);
			log.println("hibernate.hbm2ddl.auto: " + hbm2ddl);
			log.println("hibernate.dialect: " + dialect);
	
			final Map<String, String> properties = new HashMap<String, String>();            
			properties.put("connection.driver_class", driverClass);
			properties.put("hibernate.connection.url", connectionUrl);
			properties.put("hibernate.connection.password", password);
			properties.put("hibernate.default_schema", schema);
			properties.put("hibernate.connection.username", username);
			properties.put("hibernate.hbm2ddl.auto", hbm2ddl);
			properties.put("hibernate.dialect", dialect);
			
			manager = doInAppContext(this, new TypedRunnable<EntityManager>() {

				@Override
				public EntityManager run() throws Exception {
					EntityManagerFactory emf = Persistence.createEntityManagerFactory("ebsa-release-persistence-unit", properties);
					return emf.createEntityManager();
				}
				
			});	
			
			log.println("Finished connecting, entity manager is " + manager);
			
		}
		catch (Exception e) {
			log.println("Error connecting");
			e.printStackTrace(log);
			throw new Exception("Unable to connect", e);
		}
	}
}
