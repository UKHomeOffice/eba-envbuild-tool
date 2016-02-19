package com.ipt.ebsa.manage.deploy;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersion;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeployment;


public class Deployer {
	
	public ApplicationDeployment deploy(ApplicationVersion applicationVersion, String zoneOrEnvName, String schemeName) throws Exception {
		return deploy(applicationVersion, zoneOrEnvName, false, schemeName);
	}
	
	/**
	 * The deployer puts together the inputs, creates a deployment instance, a deployment engine and calls the deploy method on the deployment instance. 
	 * @param applicationVersion
	 * @param zoneOrEnvName
	 * @param isEnv true if zoneOrEnvName is an environment
	 * @param schemeName
	 */
	public ApplicationDeployment deploy(ApplicationVersion applicationVersion, String zoneOrEnvName, boolean isEnv, String schemeName) throws Exception {
		// Zone name will be of the form 'HO_IPT_NP_PRP2_DAZO' or 'IPT_ST_SIT1_COR1'
		// Env name will be of the form 'HO_IPT_NP_PRP2' or 'IPT_ST_SIT1'
		// We must be prepared for both cases
		ApplicationDeployment deployment = new ApplicationDeployment(applicationVersion);
		DeploymentEngine deploymentEngine = DeploymentEngineFactory.buildDeploymentEngine(deployment);
		deployment.setDeploymentEngine(deploymentEngine);
		// Likely to be null
		deployment.setSchemeName(schemeName);
		deployment.run(zoneOrEnvName, isEnv);
		return deployment;
	}
	
	public CompositeReleaseDeployment deploy(ReleaseVersion releaseVersion, String environmentName, String deploymentDescriptorFile) throws Exception {
		// Env name will be of the form 'HO_IPT_NP_PRP2' or 'IPT_ST_SIT1'
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(releaseVersion, environmentName, deploymentDescriptorFile);
		DeploymentEngine deploymentEngine = DeploymentEngineFactory.buildDeploymentEngine(deployment);
		deployment.setDeploymentEngine(deploymentEngine);
		deployment.run();
		return deployment;
	}
}
