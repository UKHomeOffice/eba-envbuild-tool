package com.ipt.ebsa.environment.metadata.export;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Validator;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.database.manager.ConnectionData;
import com.ipt.ebsa.database.manager.ConnectionManager;
import com.ipt.ebsa.database.manager.GlobalConfig;
import com.ipt.ebsa.environment.build.entities.Environment;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerBuild;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.GeographicContainer;
import com.ipt.ebsa.environment.build.entities.HardwareProfile;
import com.ipt.ebsa.environment.build.manager.UpdateManager;

/**
 * Superclass for Export unit tests
 *
 */
public class ExportTest {
	
	/** Output file names (match those in src/test/resources/config.properties) */
	protected static final File EXPORT_ENVIRONMENT_CONTAINER_FILE = new File("exportEnvironmentContainer.xml");
	protected static final File EXPORT_ENVIRONMENT_FILE = new File("exportEnvironment.xml");

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
		// Create test data
		createTestData();
		// Delete any existing export files
		FileUtils.deleteQuietly(EXPORT_ENVIRONMENT_FILE);
		FileUtils.deleteQuietly(EXPORT_ENVIRONMENT_CONTAINER_FILE);
	}
	
	/**
	 * Ensure no data in the unit test database
	 * @throws Exception
	 */
	private static void emptyDatabase() throws Exception {
		String schemaName = ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_SCHEMA);
		ConnectionManager connectionManager = new ConnectionManager();
		connectionManager.initialiseConnection(System.out, GlobalConfig.getInstance().getConfig(), "ebsa-environment-build-database-persistence-unit");
		EntityManager manager = connectionManager.getManager();
		try {
			if (ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_URL).startsWith("jdbc:h2:")) {
				// H2 database
				manager.getTransaction().begin();
				manager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
				@SuppressWarnings("unchecked")
				List<Object[]> results = (List<Object[]>) manager.createNativeQuery("SHOW TABLES FROM " + schemaName).getResultList();
				for (Object[] result : results) {
					String tableName = (String) result[0];
					manager.createNativeQuery("TRUNCATE TABLE " + schemaName + "." + tableName).executeUpdate();
				}
				manager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
				manager.getTransaction().commit();
			} else if (ConfigurationFactory.getConfiguration(Configuration.DATABASE_JDBC_URL).startsWith("jdbc:postgresql:")) {
				schemaName = schemaName.toLowerCase();
				// Postgres database
				manager.getTransaction().begin();
				@SuppressWarnings("unchecked")
				List<Object> results = (List<Object>) manager.createNativeQuery("select tablename from pg_tables where schemaname = '" + schemaName + "'").getResultList();
				for (Object tableName : results) {
					manager.createNativeQuery("TRUNCATE TABLE " + schemaName + "." + tableName + " CASCADE").executeUpdate();
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
			Environment environment = new Environment();
			environment.setEnvironmentGroupName("HO_IPT_NP_PRP1");
			environment.setName("HO_IPT_NP_PRP1");
			environment.setNotes("HO_IPT_NP_PRP1 environment");
			environment.setValidated(false);
			envContainer.addEnvironment(environment);
			manager.persist(geoContainer);
			manager.persist(envContainer);
			manager.persist(environment);
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
	
	
	public void resetForAws() throws Exception {
		emptyDatabase();
		createTestDataAws();
	}
	
	/**
	 * Creates a GeographicContainer, EnvironmentContainer and HardwareProfile for unit testing
	 * @throws Exception
	 */
	private static void createTestDataAws() throws Exception {
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
			envContainer.setProvider("AWS");
			envContainer.addGeographiccontainer(geoContainer);
			Environment environment = new Environment();
			environment.setEnvironmentGroupName("HO_IPT_NP_PRP1");
			environment.setName("HO_IPT_NP_PRP1");
			environment.setNotes("HO_IPT_NP_PRP1 environment");
			environment.setValidated(false);
			envContainer.addEnvironment(environment);
			manager.persist(geoContainer);
			manager.persist(envContainer);
			manager.persist(environment);
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
	 * Validates the given xml against the schema with the given name
	 * @param xml
	 * @param schemaName
	 * @throws Exception
	 */
	protected void assertXMLValid(File xmlFile, String schemaName) throws Exception {
		FileReader reader = new FileReader(xmlFile);
		try {
			Validator validator = new Validator(reader);
			InputStream stream = this.getClass().getResourceAsStream("/" + schemaName);
			validator.useXMLSchema(true);
			validator.setJAXP12SchemaSource(stream);
			validator.assertIsValid();
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}
	
	/**
	 * Asserts that the XML in the given files matches
	 * 
	 * @param expected
	 * @param actual
	 * @throws Exception
	 */
	protected void assertXMLEqual(File expected, File actual) throws Exception {
		FileReader expectedReader = null;
		FileReader actualReader = null;
		try {
			expectedReader = new FileReader(expected);
			actualReader = new FileReader(actual);
			XMLUnit.setIgnoreWhitespace(true);
			XMLUnit.setIgnoreComments(true);
			
			Diff diff = new Diff(expectedReader, actualReader);
			
			DetailedDiff detailedDiff = new DetailedDiff(diff);
			if (!detailedDiff.similar()) {
				File differencesFile = new File("differences-" + System.currentTimeMillis() + ".txt");
				FileUtils.writeStringToFile(differencesFile, detailedDiff.getAllDifferences().toString());
				Assert.fail("Actual XML from " + actual.getAbsolutePath() + " did not match expected XML from " + expected.getAbsolutePath() + ". See " + differencesFile.getAbsolutePath());
			}
		} finally {
			IOUtils.closeQuietly(expectedReader);
			IOUtils.closeQuietly(actualReader);
		}
	}
	
	/**
	 * Writes a successful deployment record to the database for the given Environment Container Definition
	 * @param environmentContainerDefinition
	 * @throws Exception
	 */
	protected void recordSuccessfulDeployment(EnvironmentContainerDefinition environmentContainerDefinition) throws Exception {
		EnvironmentContainerBuild envConBuild = new EnvironmentContainerBuild();
		envConBuild.setDateStarted(new Date());
		envConBuild.setDateCompleted(new Date());
		envConBuild.setSucceeded(true);
		envConBuild.setJenkinsBuildId("test");
		envConBuild.setJenkinsBuildNumber(1);
		envConBuild.setJenkinsJobName("test");
		envConBuild.setEnvironmentContainerDefinition(environmentContainerDefinition);
		new UpdateManager().saveEnvironmentContainerBuild(envConBuild);
	}
}
