package com.ipt.ebsa.manage.deploy;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.manager.CrudServiceImpl;
import com.ipt.ebsa.manage.Command;
import com.ipt.ebsa.manage.deploy.database.DatabaseManager;

/**
 * This is the top level entry point for application deployments
 * @author scowx
 *
 */
public class ApplicationDeploymentManager implements Command {

	private static final Logger LOG = LogManager.getLogger(ApplicationDeploymentManager.class);
	private final DatabaseManager dbm = new DatabaseManager();
	private final String applicationVersion;
	private final String zoneOrEnvName;
	private final boolean isEnv;
	private final String schemeName;
	
	public ApplicationDeploymentManager(String applicationVersion, String zoneOrEnvName, boolean isEnv, String schemeName) {
		this.applicationVersion = applicationVersion;
		this.zoneOrEnvName = zoneOrEnvName;
		this.isEnv = isEnv;
		this.schemeName = schemeName;
	}

	/**
	 * Execute the deployment
	 */
	public void execute() throws Exception {
		LOG.info(String.format("Started processing ApplicationVersion with id '%s' against zone/env '%s'", applicationVersion, zoneOrEnvName));
		dbm.initialise();
		Deployer deployer = new Deployer();
		ApplicationVersion appVer = new CrudServiceImpl<ApplicationVersion>(dbm.getEntityManager()).find(ApplicationVersion.class, new Long(applicationVersion));
		deployer.deploy(appVer, zoneOrEnvName, isEnv, schemeName);
		LOG.info(String.format("Finished processing ApplicationVersion with id '%s' against zone/env '%s'", applicationVersion, zoneOrEnvName));
	}
	
	public void cleanUp() {
		try {
			LOG.info("Cleaning up");
			dbm.finalise();
			LOG.info("Done");
		} catch (Exception e) {
			LOG.error("Error cleaning up", e);
		}
	}

}
