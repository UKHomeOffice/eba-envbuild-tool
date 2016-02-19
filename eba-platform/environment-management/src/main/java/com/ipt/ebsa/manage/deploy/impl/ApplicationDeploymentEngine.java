package com.ipt.ebsa.manage.deploy.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.EnvironmentUpdate;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.BaseDeploymentEngine;
import com.ipt.ebsa.manage.deploy.ApplicationDeployment;
import com.ipt.ebsa.manage.deploy.impl.report.ApplicationReport;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.puppet.EMPuppetManager;
import com.ipt.ebsa.manage.puppet.SshManager;
import com.ipt.ebsa.manage.transitions.MCOCommand;
import com.ipt.ebsa.manage.transitions.Transition;
import com.ipt.ebsa.yaml.YamlUtil;

/**
 * This class figures out all of the transitions which need to be made to bring
 * the system to the final state by processing all of the source data.
 * ASSUMPTIONS: 1) A component can only have one version in an zone at
 * any one time. Only a single version of a component can be deployed. We do not
 * support more than one version of a component deployed to two different boxes
 * in an zone at the same time. Limitations are that an
 * ApplicationVersion only ever has one version of each component at the same
 * time and the DeploymentDescriptor only allows each component to be defined
 * once.
 * 
 * @author scowx
 */
public class ApplicationDeploymentEngine extends BaseDeploymentEngine {

	private static Logger					log						= LogManager.getLogger(ApplicationDeploymentEngine.class);

	private ComponentDeploymentDataManager	deploymentDataManager	= new ComponentDeploymentDataManager();
	private YamlManager						yamlManager				= new YamlManager();
	private DependencyManager				dependencyManager		= new DependencyManager();
	private DifferenceManager				differenceManager		= new DifferenceManager();
	private PlanManager						planManager				= new PlanManager();
	private TransitionManager				transitionManager		= new TransitionManager();
	private JitYumUpdateManager				yumManager				= new JitYumUpdateManager();
	private HostnameResolver				hostnameResolver		= new HostnameResolver();
	//private RPMFailFileManager				failFileManager			= new RPMFailFileManager();
	private ApplicationDeployment			deployment;

	public void setDeployment(ApplicationDeployment deployment) {
		this.deployment = deployment;
	}

	/**
	 * This method figures out a deployment plan.
	 * We need a three way comparison between the components in the deployment
	 * descriptor, the components in the application version and the components
	 * in the existing YAML files. The deployment descriptor should be the base
	 * of the comparison because it defines everything we know about deploying
	 * at this moment. The comparison will allow us to determine the difference
	 * between what has been deployed and what we need to deploy. When we know
	 * what needs to be deployed (upgraded, installed, downgraded or whatever)
	 * we can then go on to see what the impact will be of changes to these
	 * components. The impact will then allow us to choose a plan which we can
	 * use to deploy the differences. The plan will define a series of transitions.
	 * Components will be updated within one of these transitions
	 * 
	 * @return 	true: if the deployment is ready to go ahead. 
	 * 			false: if the deployment should not go ahead, but no error was found
	 * (e.g. if there is nothing to change)
	 * @throws Exception if an error occurred (e.g. a failure in the deployment plan 
	 * comparison)
	 */
	@Override
	public boolean prepare() throws Exception {
		try {
			String applicationShortName = deployment.getApplicationShortName();			
			return doPrepare(deployment, applicationShortName) == PrepareStatus.OK;
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Caught an exception", e);
			throw e;
		} finally {
			new ApplicationReport().generateReport(deployment);
		}
	}

	@Override
	public void validate() {

	}

	@Override
	public boolean execute() {
		if (StringUtils.isBlank(deployment.getId())) {
			throw new IllegalArgumentException("Job Id must not be blank");
		}

		try {
			yumManager.doJitYumRepoUpdate(deployment.getOrganisation());
		} catch (Exception e2) {
			throw new RuntimeException("Failed to doJitYumRepoUpdate()", e2);
		}

		EMPuppetManager puppet = null;
		try {
			puppet = new EMPuppetManager(new SshManager());
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate PuppetManager", e);
		}

		executeTransitions(deployment, puppet);

		// Finished transition
		return true;
	}

	/**
	 * Collates hiera updates by grouping them by filename
	 * @param updates
	 * @return
	 */
	protected Map<String, List<EnvironmentUpdate>> collateUpdates(List<EnvironmentUpdate> updates) {
		Map<String, List<EnvironmentUpdate>> updatesForEnvironment = new TreeMap<>();
		for (EnvironmentUpdate update : updates) {
			if (update == null || update.getSource() == null || update.getSource().getSourceName() == null) {
				log.warn(String.format("Attempting to collate update [%s] with non-existant or incorrect source", update));
				continue;
			}

			String key = update.getSource().getSourceName();
			if (updatesForEnvironment.containsKey(key)) {
				updatesForEnvironment.get(key).add(update);
			} else {
				List<EnvironmentUpdate> updatesForFile = new ArrayList<>();
				updatesForEnvironment.put(key, updatesForFile);
				updatesForFile.add(update);
			}
		}
		return updatesForEnvironment;
	}

	/**
	 * Self contained logic for executing some transitions given a puppet and git manager
	 * The logic is that for each transition, commands are executed then the updates are made YAML file, puppet, YAML file, puppet, ...
	 */
	protected void executeTransitions(ApplicationDeployment deployment, EMPuppetManager puppet) {
		String applicationShortName = deployment.getApplicationShortName();
		
		EMGitManager git = deployment.getGitManager();
		
		for (Transition t : deployment.getTransitions()) {
			// Build base commit string
			String baseCommitString = String.format("Transition %s, deploying %s version %s:%s", t.getSequenceNumber(), applicationShortName, deployment.getApplicationVersion(applicationShortName).getName(), deployment.getApplicationVersion().getVersion());

			log.info(String.format("Performing workload task for %s", baseCommitString));

			// Execute the commands for the transition first.  
			// In all cases if there are commands there will be no yaml updates (based on the way actions always result in a fresh transition ).
			// In the unlikely event that we do get transitions as well then the commands are ordered to run before the yamlupdates and th ecorrsponding puppet run
			for (MCOCommand command : t.getCommands()) {
				invokeMCollectiveCommand(puppet, command);
			}

			// Collate YAML updates against each file
			Map<String, List<EnvironmentUpdate>> updatesForEnvironment = collateUpdates(t.getUpdates());

			// Apply the YAML updates, one Hiera file at a time
			for (String hieraSource : updatesForEnvironment.keySet()) {
				String roleFQDN = YamlUtil.getRoleOrHostFromYaml(hieraSource);
				
				// Get zone
				String zoneName = null;
				
				// Apply the updates, one by one 
				try {
					for (EnvironmentUpdate update : updatesForEnvironment.get(hieraSource)) {
						String updateZoneName = update.getZoneName();
						if (zoneName == null) {
							zoneName = updateZoneName;
						}
						update.doUpdate(updateZoneName, baseCommitString);
					}
				} catch (Exception e1) {
					throw new RuntimeException(String.format("Failed during Update, fatal for %s", baseCommitString), e1);
				}

				// Commit all YAML updates for current Hiera file
				try {
					git.commitBranchMergeToMaster(baseCommitString);
				} catch (Exception e) {
					throw new RuntimeException(String.format("Failed during merge and commit phase, fatal for %s", baseCommitString), e);
				}

				//Invoke a puppet run for the current file
				if (zoneName != null) {
					invokePuppetRun(puppet, zoneName, baseCommitString, hieraSource, roleFQDN);
				}
			}
		}
	}

	/**
	 * Invokes a puppet run
	 * @param puppet
	 * @param zoneName
	 * @param baseCommitString
	 * @param hieraFile
	 * @param roleFQDN
	 * @throws IOException
	 */
	private void invokePuppetRun(EMPuppetManager puppet, String zoneName, String baseCommitString, String hieraFile, String roleFQDN) {
		// Update the Puppet Master so that it has all the YAML changes for this Hiera file
		String startPuppetUpdateMessage = String.format("Updating Puppet Master against file %s for %s", hieraFile, baseCommitString);
		log.info(startPuppetUpdateMessage);
		int exitCode = puppet.updatePuppetMaster(deployment.getOrganisation());

		if (exitCode != 0) {
			throw new RuntimeException(String.format("Did not successfully update the Puppet Master against file %s, fatal for %s", hieraFile, baseCommitString));
		}
		
		int retryCount = Configuration.getPuppetRetryCount();
		int retryDelaySeconds = Configuration.getPuppetRetryDelaySeconds();
		
		// Trigger a Puppet run for changes made to this Hiera file
		String startPuppetRunMessage = String.format("Starting Puppet run against file %s for %s (%s)", hieraFile, baseCommitString, roleFQDN);
		log.info(startPuppetRunMessage);
		exitCode = puppet.doPuppetRunWithRetry(deployment.getOrganisation(), zoneName, roleFQDN, retryCount, retryDelaySeconds).getReturnCode();
		String finishPuppetRunMessage = String.format("Finished Puppet run against file %s for %s (%s)", hieraFile, baseCommitString, roleFQDN);
		log.info(finishPuppetRunMessage);
		
		if (exitCode != 0) {
			throw new RuntimeException(String.format("Did not successfully perform the Puppet run, exit code was %s for %s", exitCode, baseCommitString));
		}
	}
	
	/**
	 * This method handles executing a command.
	 * @param puppet
	 * @param zoneName
	 * @param command
	 */
	private void invokeMCollectiveCommand(EMPuppetManager puppet, MCOCommand command) {
		String mcoCommand = command.getCommand();
		
		log.info(String.format("Executing command '%s' on hosts (%s)", mcoCommand, command.getHosts()));
		
		int exitCode = puppet.doMCollectiveOperation(deployment.getOrganisation(), command.getHosts(), mcoCommand);
		if (exitCode != 0) {
			throw new RuntimeException(String.format("None-zero return code from command '%s' on hosts (%s).  Return code was '%s'.", mcoCommand, command.getHosts(), exitCode));		
		}
		log.info(String.format("Successfully executed command '%s' on hosts (%s)", mcoCommand, command.getHosts()));
	}

	public JitYumUpdateManager getYumManager() {
		return yumManager;
	}

	@Override
	public boolean cleanup() throws Exception {
		if (null != deployment && null != deployment.getGitManager()) {
			deployment.getGitManager().close();
		}
		
		return true;
	}

	@Override
	public ComponentDeploymentDataManager getDeploymentDataManager() {
		return deploymentDataManager;
	}

	@Override
	public HostnameResolver getHostnameResolver() {
		return hostnameResolver;
	}

	@Override
	public YamlManager getYamlManager() {
		return yamlManager;
	}

	@Override
	public DifferenceManager getDifferenceManager() {
		return differenceManager;
	}

	@Override
	public DependencyManager getDependencyManager() {
		return dependencyManager;
	}

	@Override
	public PlanManager getPlanManager() {
		return planManager;
	}

	@Override
	public TransitionManager getTransitionManager() {
		return transitionManager;
	}

//	@Override
//	public RPMFailFileManager getFailFileManager() {
//		return failFileManager;
//	}
}
