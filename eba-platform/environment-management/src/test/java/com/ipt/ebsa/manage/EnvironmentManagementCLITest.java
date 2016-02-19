package com.ipt.ebsa.manage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

import com.ipt.ebsa.config.ConfigurationFactory;

import static com.ipt.ebsa.manage.Configuration.*;

public class EnvironmentManagementCLITest {
	
	@After
	public void afterEachTest() {
	    ConfigurationFactory.getProperties().remove(DEPLOYMENT_ENVIRONMENTS);
	    File file = new File(DEFAULT_DEPLOYMENT_ENVIRONMENTS_FILE);
	    if (file.exists()) {
	    	file.delete();
	    }
	}
	
	@Test
	public void getEnvironmentsTest() throws Exception {
		final String command = "listEnvironments";
		ConfigurationFactory.getProperties().setProperty(DEPLOYMENT_ENVIRONMENTS, "  IPT_ST_SIT1   , HO_IPT_NP_PRP1  ");
		EnvironmentManagementCLI.main(new String[] {"-command=" + command});
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(DEFAULT_DEPLOYMENT_ENVIRONMENTS_FILE)));
			assertEquals("[\"IPT_ST_SIT1\",\"HO_IPT_NP_PRP1\"]", reader.readLine());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}	
}
