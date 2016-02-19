package com.ipt.ebsa.manage.th.data;

import java.io.IOException;
import java.util.Properties;

import javax.persistence.EntityManager;

import org.xml.sax.SAXException;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.manage.th.EnvironmentManagementTestHarness;
import com.ipt.ebsa.manage.th.database.DBUtil;

/**
 * Test data utility for the Deployer tests 
 * @author scowx
 *
 */
public class Data {
	
	private EntityManager entityManager;
	
	public Data(EntityManager entityManager) throws Exception {
		this.entityManager = entityManager;
	}

	/**
	 * Sets up data for a standard install
	 * @return
	 * @throws Exception
	 * @throws SAXException
	 * @throws IOException
	 */
	public ApplicationVersion setUpDataForTest(Properties properties) throws Exception, SAXException, IOException {
		
		String newReleaseDetails = properties.getProperty(EnvironmentManagementTestHarness.CONFIG_PARAM_NEW_PACKAGE_NAMES);
		String[] packages = newReleaseDetails.split(",");
		
		/* Set up database data */
		String group = "groupid";
		String[][] data = new String[packages.length][4];
		for (int i = 0; i < packages.length; i++) {
			String name = packages[i];
			String version = properties.getProperty(packages[i] + EnvironmentManagementTestHarness.CONFIG_PARAM_NEW_VERSION);
			data[i] = new String[]{name,group,name, version};
		}
		ApplicationVersion appVersion = DBUtil.createApplicationVersion(entityManager, "APP", "Application being tested", data);
		
		return appVersion;
	}	
}
