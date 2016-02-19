package com.ipt.ebsa.database.manager;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.ipt.ebsa.util.ClassLoaderUtils;
import com.ipt.ebsa.util.ClassLoaderUtils.TypedRunnable;

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
	 * @param printStreame 
	 * @throws Exception
	 */
	public void initialiseConnection(PrintStream printStream, ConnectionData descriptor, String persistenceUnitName) throws Exception {
		printStream.println("Driver class: "+descriptor.getDriverClass());
		printStream.println("JDBC Url: "+descriptor.getUrl());
		printStream.println("Username: "+descriptor.getUsername());
		printStream.println("Schema: "+descriptor.getSchema());
		printStream.println("AutoDll: "+descriptor.getAutodll());
		printStream.println("Dialect: "+descriptor.getDialect());
               
        initialiseConnection(printStream, descriptor.getDriverClass(), descriptor.getUrl(), descriptor.getUsername(), descriptor.getPassword(), descriptor.getSchema(), descriptor.getAutodll(),descriptor.getDialect(), persistenceUnitName);
	}
	
	/**
	 * Initialises the Entity Manager from the properties passed in by the user.
	 */
	public void initialiseConnection(final PrintStream log, final String driverClass, final String connectionUrl, final String username, final String password, final String schema, final String hbm2ddl, final String dialect, final String persistenceUnitName) throws Exception {

		/* 
		 * The Jenkins class loader does not play ball nicely.  It does not seem to be able to pick up our persistence XML
		 * because the PersistenceResolver cannot see it on the class path.  It is definitely on a classpath because we 
		 * I have checked in the HPI it just seems that the classpath is being governed from "above" inside Jenkins and so the 
		 * persistence.xml is not on the searchpath of the PersistenceResolver.
		 * In order for the Persistence.xml file to be found at runtime I had to set the ContextClassloader to the one which loaded
		 * all of my plugin classes and then everything works sweetly.  I put it back again when I have made the connection as the persistence.XML 
		 * has already been loaded and does not need to be used again and I do not want undesirable side effects from swapping out the original
		 * ContextClassLoader for my own. 
		 */
		ClassLoaderUtils.doInAppContext(this, new TypedRunnable<Void>() {
			@Override
			public Void run() throws Exception {
				log.println("Connecting to database");
				log.println("connection.driver_class: " + driverClass);
				log.println("hibernate.connection.url: " + connectionUrl);
				log.println("hibernate.connection.password: " + ((password != null && password.length() > 0) ? password.replaceAll(".*", "*") : " password is empty or null"));
				log.println("hibernate.default_schema: " + schema);
				log.println("hibernate.connection.username: " + username );
				log.println("hibernate.hbm2ddl.auto: " + hbm2ddl );
				log.println("hibernate.dialect: " + dialect );

				Map<String, String> properties = new HashMap<String, String>();            
				properties.put("connection.driver_class",driverClass);
				properties.put("hibernate.connection.url",connectionUrl);
				properties.put("hibernate.connection.password",password);
				properties.put("hibernate.default_schema",schema);
				properties.put("hibernate.connection.username",username );
				properties.put("hibernate.hbm2ddl.auto",hbm2ddl);
				properties.put("hibernate.dialect",dialect);
				properties.put("hibernate.show_sql", "true");

				EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnitName, properties);

				manager = emf.createEntityManager();

				log.println("Finished connecting, entity manager is " + manager);
				return null;
			}
		});
	}
}
