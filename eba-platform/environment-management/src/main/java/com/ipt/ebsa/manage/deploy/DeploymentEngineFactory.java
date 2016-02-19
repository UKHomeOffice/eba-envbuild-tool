package com.ipt.ebsa.manage.deploy;

import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeployment;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeploymentEngine;
import com.ipt.ebsa.manage.deploy.impl.ApplicationDeploymentEngine;

/**
 * Factory for creating deployment engines.  I imagine that we will need one which can run outside of Jenkins to enable us to deploy things "manually" and to do dryRuns
 * and one to run inside a container like Jenkins which hooks into Jenkins jobs and the like instead of doing things itself.  A better model might be to keep the central logic 
 * and just hand off to plugins, I guess we will see as things move on.
 * @author scowx
 *
 */
public class DeploymentEngineFactory {
	
	/**
	 * SS2 application deployment engine
	 * @param deployment
	 * @return
	 */
	public static DeploymentEngine buildDeploymentEngine(ApplicationDeployment deployment) {
		ApplicationDeploymentEngine engine = new ApplicationDeploymentEngine();
		engine.setDeployment(deployment);
		return engine;
	}

	/**
	 * SS3 composite release deployment engine
	 * @param deployment
	 * @return
	 */
	public static DeploymentEngine buildDeploymentEngine(CompositeReleaseDeployment deployment) {
		CompositeReleaseDeploymentEngine engine = new CompositeReleaseDeploymentEngine();
		engine.setDeployment(deployment);
		return engine;
	}
	
}
