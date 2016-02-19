/**
 * 
 */
package com.ipt.ebsa.manage.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.git.GitManager;
import com.ipt.ebsa.hiera.HieraData;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.ExternalScenarioUnitTest;
import com.ipt.ebsa.manage.deploy.database.DBTestUtil;
import com.ipt.ebsa.manage.git.EMGitManagerTest;

/**
 * @author James Shepherd
 *
 */
public class TestHelper {

	public static GitManager setupGitRepo(String baseFolder) throws IOException, FileNotFoundException, NoFilepatternException, GitAPIException {
		Properties properties = new Properties();
		String hieraDataFolder = baseFolder + "/st";
		String propertiesFile = baseFolder + "/config.properties";
		String reportFolder = "target/reports/" + baseFolder.replaceAll(".*/", "");
		String reportFile = "report.html";
		
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR, baseFolder + "/DeploymentDescriptor.xml");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER, reportFolder);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FILENAME, reportFile);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX, "st");
		ConfigurationFactory.getProperties().put("st.yum.repo.update.enabled", "false");
	
		properties.load(new FileInputStream(new File(propertiesFile)));
		Set<Entry<Object, Object>> entrySet = properties.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			ConfigurationFactory.getProperties().put(entry.getKey(), entry.getValue());
		}
		
		GitManager repo = EMGitManagerTest.createGitRepo(new File(hieraDataFolder));
		ConfigurationFactory.getProperties().put(Configuration.GIT_REMOTE_CHECKOUT_REPO_URL, "file://garbage");
		ConfigurationFactory.getProperties().put("st.hiera.ext.repo.enabled", "true");
		ConfigurationFactory.getProperties().put("st.hiera.ext.repo", "file://" + repo.getGitMetadataDir());
		return repo;
	}

	public static ApplicationVersion getApplicationVersion(EntityManager entityManager) throws Exception {
		/* Set everything up for this test */
		String newReleaseDetails = ConfigurationFactory.getConfiguration(ExternalScenarioUnitTest.CONFIG_PARAM_NEW_PACKAGE_NAMES);
		String[][] data = null;
		if (newReleaseDetails != null) {
			String[] packages = newReleaseDetails.split(",");
			
			/* Set up database data */
			String group = "groupid";
			data = new String[packages.length][6];
			for (int i = 0; i < packages.length; i++) {
				String name = packages[i];
				String componentVersion = ConfigurationFactory.getConfiguration(name + ExternalScenarioUnitTest.CONFIG_PARAM_NEW_VERSION);
				if (componentVersion == null || componentVersion.equals(HieraData.ABSENT)) {
					data[i] = null;
					continue;
				}
				String rpmPackageName = name;
				String rpmPackageVersion = componentVersion.contains("-") ? componentVersion : componentVersion + "-1"; //arbitary hardcoding to use -1 as the rpm, if hyphen not already present
				
				data[i] = new String[]{name.toUpperCase(), group, name, componentVersion, rpmPackageName, rpmPackageVersion };
			}
		}
		ApplicationVersion appVersion = DBTestUtil.createApplicationVersion(entityManager, "APP", "Application being tested", data);
		return appVersion;
	}

	/**
	 * Any test that calls ConfigurationFactory anywhere must call this in @Before.
	 * Note: if you don't, even if the property you require is set in the default properties
	 * file in the classpath, you will break the other tests, as the test properties file
	 * below will never be loaded.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void setupTestConfig() throws FileNotFoundException, IOException {
		mergeProperties("src/test/resources/testconfig.properties");
	}

	public static void mergeProperties(String propertiesFile) throws IOException, FileNotFoundException {
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File(propertiesFile)));
		Set<Entry<Object, Object>> entrySet = properties.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			ConfigurationFactory.getProperties().put(entry.getKey(), entry.getValue());
		}
	}
}
