package com.ipt.ebsa.manage.deploy.comprelease;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.buildtools.release.entities.ReleaseVersion;
import com.ipt.ebsa.buildtools.release.manager.CrudServiceImpl;
import com.ipt.ebsa.manage.Command;
import com.ipt.ebsa.manage.deploy.Deployer;
import com.ipt.ebsa.manage.deploy.database.DatabaseManager;

/**
 * This is the top level entry point for composite release (SS3) deployments
 * @author scowx
 *
 */
public class CompositeReleaseDeploymentManager implements Command {

	private static final Logger LOG = LogManager.getLogger(CompositeReleaseDeploymentManager.class);
	private final DatabaseManager dbm = new DatabaseManager();
	private final String releaseVersion;
	private final String environmentName;
	private final String deploymentDescriptorFile;
	
	public CompositeReleaseDeploymentManager(String releaseVersion, String environmentName, String deploymentDescriptorFile) {
		this.releaseVersion = releaseVersion;
		this.environmentName = environmentName;
		this.deploymentDescriptorFile = deploymentDescriptorFile;
	}

	/**
	 * Execute the deployment
	 */
	public void execute() throws Exception {
		LOG.info(String.format("Started processing ReleaseVersion with id '%s' against environment '%s'", releaseVersion, environmentName));
		dbm.initialise();
		Deployer deployer = new Deployer();
		ReleaseVersion relVer = new CrudServiceImpl<ReleaseVersion>(dbm.getEntityManager()).find(ReleaseVersion.class, new Long(releaseVersion));
		deployer.deploy(relVer, environmentName, deploymentDescriptorFile);
		LOG.info(String.format("Finished processing ApplicationVersion with id '%s' against environment '%s'", releaseVersion, environmentName));
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
