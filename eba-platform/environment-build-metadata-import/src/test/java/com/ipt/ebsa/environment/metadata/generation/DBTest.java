package com.ipt.ebsa.environment.metadata.generation;


import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.database.manager.ConnectionData;
import com.ipt.ebsa.database.manager.ConnectionManager;
import com.ipt.ebsa.database.manager.GlobalConfig;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.GeographicContainer;
import com.ipt.ebsa.environment.build.entities.HardwareProfile;

import difflib.DiffUtils;
import difflib.Patch;

/**
 * Superclass for unit tests for shared database functions
 *
 */
public class DBTest {
	
	/** Directory where database tables are exported as CSV files */
	private static final File CSV_OUTPUT_DIR = new File("actual");
	
	/** Directory containing expected import CSV files for a Skyscape environment import */
	protected static final File CSV_EXPECTED_ENVIRONMENT_DIR_SKYSCAPE = new File("src/test/resources/expected/environment-import-skyscape");

	/** Directory containing expected import CSV files for an AWS environment import */
	protected static final File CSV_EXPECTED_ENVIRONMENT_DIR_AWS = new File("src/test/resources/expected/environment-import-aws");

	
	/** Directory containing expected import CSV files for a Skyscape environmentContainerDefinition import */
	protected static final File CSV_EXPECTED_ENVIRONMENT_CONTAINER_DEFINITION_DIR_SKYSCAPE = new File("src/test/resources/expected/environmentContainerDefinition-import-skyscape");

	
	/** Directory containing expected import CSV files for an AWS environmentContainerDefinition import */
	protected static final File CSV_EXPECTED_ENVIRONMENT_CONTAINER_DEFINITION_DIR_AWS = new File("src/test/resources/expected/environmentContainerDefinition-import-aws");

	@BeforeClass
	public static void setUpOnce() throws Exception {
		// Use DB connection data from src/test/resources/config.properties
		GlobalConfig.getInstance().setSharedConnectionData(new ConnectionData() {
			public String getUsername() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_USERNAME);
			}
			public String getUrl() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_URL);
			}
			public String getSchema() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_SCHEMA);
			}
			public String getPassword() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_PASSWORD);
			}
			public String getDriverClass() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_DRIVER);
			}
			public String getAutodll() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_HBM2DDL_AUTO);
			}
			public String getDialect() {
				return ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_HIBERNATE_DIALECT);
			}
		});
	}

	@Before
	public void setUp() throws Exception {
		// Clear the Configuration so it is reloaded for each test
		Method clearConfig = ConfigurationFactory.class.getDeclaredMethod("reset");
		clearConfig.setAccessible(true);
		clearConfig.invoke(null, (Object[]) null);		

		// Delete all data from the unit test database
		emptyDatabase();
		// Create a test GeographicContainer and EnvironmentContainer
		createTestData();
	}
	
	/**
	 * Ensure no data in the unit test database
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private static void emptyDatabase() throws Exception {
		String schemaName = ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_SCHEMA);
		ConnectionManager connectionManager = new ConnectionManager();
		connectionManager.initialiseConnection(System.out, GlobalConfig.getInstance().getConfig(), "ebsa-environment-build-database-persistence-unit");
		EntityManager manager = connectionManager.getManager();
		try {
			if (ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_URL).startsWith("jdbc:h2:")) {
				// H2 database
				manager.getTransaction().begin();
				// Disable referential integrity
				manager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
				// List tables
				List<Object[]> results = (List<Object[]>) manager.createNativeQuery("SHOW TABLES FROM " + schemaName).getResultList();
				for (Object[] result : results) {
					// Empty tables
					String tableName = (String) result[0];
					manager.createNativeQuery("TRUNCATE TABLE " + schemaName + "." + tableName).executeUpdate();
				}
				// Reset sequences
				results = (List<Object[]>) manager.createNativeQuery("SELECT TABLE_NAME, COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS WHERE SEQUENCE_NAME IS NOT NULL AND TABLE_SCHEMA = '" + schemaName.toUpperCase() + "'").getResultList();
				for (Object[] result : results) {
					// Empty tables
					String tableName = (String) result[0];
					String columnName = (String) result[1];
					manager.createNativeQuery("ALTER TABLE " + schemaName + "." + tableName + " ALTER COLUMN " + columnName + " RESTART WITH 1").executeUpdate();
				}
				// Enable referential integrity
				manager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
				manager.getTransaction().commit();
			} else if (ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_URL).startsWith("jdbc:postgresql:")) {
				schemaName = schemaName.toLowerCase();
				// Postgres database
				manager.getTransaction().begin();
				// List tables
				List<Object> results = (List<Object>) manager.createNativeQuery("select tablename from pg_tables where schemaname = '" + schemaName + "'").getResultList();
				for (Object tableName : results) {
					// Empty tables and reset sequences
					manager.createNativeQuery("TRUNCATE TABLE " + schemaName + "." + tableName + " RESTART IDENTITY CASCADE").executeUpdate();
				}
				manager.getTransaction().commit();
			}
		} catch (Exception e) {
			if (manager.getTransaction().isActive()) {
				manager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			connectionManager.closeConnection(System.out);
		}
	}
	
	/**
	 * Creates a GeographicContainer, EnvironmentContainer and HardwareProfile for unit testing
	 * @throws Exception
	 */
	private static void createTestData() throws Exception {
		ConnectionManager connectionManager = new ConnectionManager();
		connectionManager.initialiseConnection(System.out, GlobalConfig.getInstance().getConfig(), "ebsa-environment-build-database-persistence-unit");
		EntityManager manager = connectionManager.getManager();
		try {
			manager.getTransaction().begin();
			GeographicContainer geoContainer = new GeographicContainer();
			geoContainer.setAccount("Home Office");
			geoContainer.setRegion("IPT");
			EnvironmentContainer envContainer = new EnvironmentContainer();
			envContainer.setName("np");
			envContainer.setProvider("SKYSCAPE");
			envContainer.addGeographiccontainer(geoContainer);
			manager.persist(geoContainer);
			manager.persist(envContainer);
			geoContainer = new GeographicContainer();
			geoContainer.setAccount("Home Office");
			geoContainer.setRegion("IPT");
			envContainer = new EnvironmentContainer();
			envContainer.setName("np");
			envContainer.setProvider("AWS");
			envContainer.addGeographiccontainer(geoContainer);
			manager.persist(geoContainer);
			manager.persist(envContainer);
			// Dummy hardware profile for AWS
			HardwareProfile hardwareProfile = new HardwareProfile();
			hardwareProfile.setCpuCount(2);
			hardwareProfile.setMemory(99);
			hardwareProfile.setInterfaceCount(99);
			hardwareProfile.setVmRole("Default");
			hardwareProfile.setProvider("AWS");
			hardwareProfile.setProfile("m3.large");
			hardwareProfile.setEnabled(true);
			manager.persist(hardwareProfile);
			manager.flush();
			manager.getTransaction().commit();
		} catch (Exception e) {
			if (manager.getTransaction().isActive()) {
				manager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			connectionManager.closeConnection(System.out);
		}
	}
	
	/**
	 * Exports data from the database tables to CSV files
	 * @throws Exception
	 */
	protected static void exportToCSV() throws Exception {
		String schemaName = ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_SCHEMA);
		ConnectionManager connectionManager = new ConnectionManager();
		connectionManager.initialiseConnection(System.out, GlobalConfig.getInstance().getConfig(), "ebsa-environment-build-database-persistence-unit");
		EntityManager manager = connectionManager.getManager();
		if (CSV_OUTPUT_DIR.exists()) {
			FileUtils.cleanDirectory(CSV_OUTPUT_DIR);
		} else {
			CSV_OUTPUT_DIR.mkdirs();
		}
		try {
			if (ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_URL).startsWith("jdbc:h2:")) {
				// H2 database
				manager.getTransaction().begin();
				@SuppressWarnings("unchecked")
				List<Object[]> results = (List<Object[]>) manager.createNativeQuery("SHOW TABLES FROM " + schemaName).getResultList();
				for (Object[] result : results) {
					String tableName = schemaName + "." + result[0];
					manager.createNativeQuery("call CSVWRITE ('" + CSV_OUTPUT_DIR.getAbsolutePath() + "/" + tableName + ".csv', 'SELECT * FROM " + tableName + "' )").executeUpdate();
				}
				manager.getTransaction().commit();
			} else if (ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_URL).startsWith("jdbc:postgresql:")) {
				schemaName = schemaName.toLowerCase();
				// Postgres database
				manager.getTransaction().begin();
				@SuppressWarnings("unchecked")
				List<Object> results = (List<Object>) manager.createNativeQuery("select tablename from pg_tables where schemaname = '" + schemaName + "'").getResultList();
				for (Object tableName : results) {
					tableName = schemaName + "." + tableName.toString().toUpperCase();
					manager.createNativeQuery("COPY " + tableName + " TO '" + CSV_OUTPUT_DIR.getAbsolutePath() + "/" + tableName + ".csv' CSV HEADER FORCE QUOTE *").executeUpdate();
				}
				manager.getTransaction().commit();
			}
		} catch (Exception e) {
			if (manager.getTransaction().isActive()) {
				manager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			connectionManager.closeConnection(System.out);
		}
	}
	
	/**
	 * Verifies the data exported from the database tables
	 * @param expectedDir
	 * @throws Exception
	 */
	protected static void verifyCSV(File expectedDir) throws Exception {
		List<String> expectedFiles = Arrays.asList(expectedDir.list());
		List<String> actualFiles = Arrays.asList(CSV_OUTPUT_DIR.list());
		
		List<String> missingFiles = new ArrayList<String>(expectedFiles);
		missingFiles.removeAll(actualFiles);
		if (missingFiles.size() > 0) {
			Collections.sort(missingFiles);
			Assert.fail("Missing expected CSV files: " + missingFiles);
		}
		
		List<String> extraFiles = new ArrayList<String>(actualFiles);
		extraFiles.removeAll(expectedFiles);
		if (extraFiles.size() > 0) {
			Collections.sort(extraFiles);
			Assert.fail("Unexpected CSV files: " + extraFiles);
		}
		
		for (String expectedFilename : expectedFiles) {
			File expectedFile = new File(expectedDir, expectedFilename);
			File actualFile = new File(CSV_OUTPUT_DIR, expectedFilename);
			
			Patch patch = DiffUtils.diff(FileUtils.readLines(expectedFile), FileUtils.readLines(actualFile));
			if (patch.getDeltas().size() > 0) {
				String errorMessage = actualFile.getAbsolutePath() + " does not match " + expectedFile.getAbsolutePath() + "\n" + patch.getDeltas();
				System.err.println(errorMessage);
				Assert.fail(errorMessage);
			}
		}
		
		FileUtils.forceDelete(CSV_OUTPUT_DIR);
	}

}
