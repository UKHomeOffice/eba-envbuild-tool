
package com.ipt.ebsa.manage.deploy.comprelease;

import static com.ipt.ebsa.manage.deploy.BaseDeploymentEngine.PrepareStatus.FAIL;
import static com.ipt.ebsa.manage.deploy.BaseDeploymentEngine.PrepareStatus.OK;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.deployment.descriptor.release.XMLPhaseType;
import com.ipt.ebsa.environment.EnvironmentUpdate;
import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.BaseDeploymentEngine;
import com.ipt.ebsa.manage.deploy.DeploymentStatus;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentDataManager;
import com.ipt.ebsa.manage.deploy.impl.DependencyManager;
import com.ipt.ebsa.manage.deploy.impl.DifferenceManager;
import com.ipt.ebsa.manage.deploy.impl.HostnameResolver;
import com.ipt.ebsa.manage.deploy.impl.JitYumUpdateManager;
import com.ipt.ebsa.manage.deploy.impl.PlanManager;
import com.ipt.ebsa.manage.deploy.impl.TransitionManager;
import com.ipt.ebsa.manage.deploy.impl.YamlManager;
import com.ipt.ebsa.manage.deploy.impl.report.CompositeReleaseReport;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.puppet.EMPuppetManager;
import com.ipt.ebsa.manage.puppet.SshManager;
import com.ipt.ebsa.manage.transitions.MCOCommand;
import com.ipt.ebsa.manage.transitions.Transition;
import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.yaml.YamlUtil;

/**
 * This class loops over each phase and figures out all of the transitions which need to be made to bring
 * the system to the final state by processing all of the source data.
 * 
 * @author Dan McCarthy
 */
public class CompositeReleaseDeploymentEngine extends BaseDeploymentEngine {

	private static Logger					log						= LogManager.getLogger(CompositeReleaseDeploymentEngine.class);

	private ComponentDeploymentDataManager	deploymentDataManager	= new ComponentDeploymentDataManager();
	private YamlManager						yamlManager				= new YamlManager();
	private DependencyManager				dependencyManager		= new DependencyManager();
	private DifferenceManager				differenceManager		= new DifferenceManager();
	private PlanManager						planManager				= new PlanManager();
	private TransitionManager				transitionManager		= new TransitionManager();
	private JitYumUpdateManager				yumManager				= new JitYumUpdateManager();
	private HostnameResolver				hostnameResolver		= new HostnameResolver();
	private CompositeReleasePhaseBuilder	phaseBuilder			= new CompositeReleasePhaseBuilder();
	private CompositeReleaseDeployment		deployment;
	
	public void setDeployment(CompositeReleaseDeployment deployment) {
		this.deployment = deployment;
	}

	/**
	 * This method figures out a deployment plan for each phase.
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
			Set<CompositeReleasePhase> phases = phaseBuilder.buildPhases(deployment);
			Iterator<CompositeReleasePhase> iterator = phases.iterator();
			
			List<PrepareStatus> statuses = new ArrayList<>(phases.size());
			for (int i = 0; i < phases.size(); i++) {
				CompositeReleasePhase phase = iterator.next();
				XMLPhaseType xmlPhase = phase.getXmlPhase();
				
				// Before phase steps
				List<Transition> beforeTransitions = transitionManager.createPhasePreTransitions(xmlPhase.getBefore(), phase);
				phase.setBeforeTransitions(beforeTransitions);
				log.info(String.format("Phase %d: Added %d before transitions", i, beforeTransitions.size()));
				
				// Prepare
				PrepareStatus status = doPrepare(phase, phase.getApplicationShortNames());
				statuses.add(status);
				
				// After phase steps
				List<Transition> afterTransitions = transitionManager.createPhasePostTransitions(xmlPhase.getAfter(), xmlPhase.getStop(), phase);
				phase.setAfterTransitions(afterTransitions);
				log.info(String.format("Phase %d: Added %d after transitions", i, afterTransitions.size()));
				
				deployment.addPhase(phase);
			}
			
			return (!statuses.contains(FAIL) && statuses.contains(OK));
		} catch (Exception e) {
			log.error("Caught an exception", e);
			throw e;
		} finally {
			new CompositeReleaseReport(false, !Configuration.isPrepareOnly()).generateReport(deployment);
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
	 * Collates hiera updates by grouping them by machine state
	 * @param updates
	 * @return
	 */
	protected Map<MachineState, List<EnvironmentUpdate>> collateUpdates(List<EnvironmentUpdate> updates) {
		Map<MachineState, List<EnvironmentUpdate>> updatesForEnvironment = new TreeMap<>();
		for (EnvironmentUpdate update : updates) {
			if (update == null || update.getSource() == null || update.getSource().getSourceName() == null) {
				log.warn(String.format("Attempting to collate update [%s] with non-existant or incorrect source", update));
				continue;
			}

			MachineState state = update.getSource();
			if (updatesForEnvironment.containsKey(state)) {
				updatesForEnvironment.get(state).add(update);
			} else {
				List<EnvironmentUpdate> updatesForFile = new ArrayList<>();
				updatesForEnvironment.put(state, updatesForFile);
				updatesForFile.add(update);
			}
		}
		return updatesForEnvironment;
	}

	/**
	 * Self contained logic for executing some transitions given a puppet and git manager
	 * The logic is that for each transition, commands are executed then the updates are made YAML file, puppet, YAML file, puppet, ...
	 */
	protected void executeTransitions(CompositeReleaseDeployment deployment, EMPuppetManager puppet) {
		EMGitManager git = deployment.getGitManager();
		
		for (Transition t : deployment.getTransitions()) {
			// TODO: Build better base commit string
			String baseCommitString = String.format("Transition %s, deploying into %s", t.getSequenceNumber(), deployment.getOrganisation());

			log.info(String.format("Performing workload task for %s", baseCommitString));
			
			updateStatus(deployment, t, DeploymentStatus.STARTED, null, null);
			
			// Execute the commands for the transition first.  
			// In all cases if there are commands there will be no yaml updates (based on the way actions always result in a fresh transition ).
			// In the unlikely event that we do get transitions as well then the commands are ordered to run before the yaml updates and the corresponding puppet run
			for (MCOCommand command : t.getCommands()) {
				invokeMCollectiveCommand(puppet, command);
			}

			// Collate YAML updates against each file
			Map<MachineState, List<EnvironmentUpdate>> updatesForEnvironment = collateUpdates(t.getUpdates());
			
			if (!updatesForEnvironment.isEmpty()) {
				// Maintain insertion order as we gather unique zones for hiera files
				Map<String, Set<String>> zonesRolesAndFQDNs = new LinkedHashMap<>();
				
				// Apply the YAML updates, for all Hiera files in one go
				for (MachineState key : updatesForEnvironment.keySet()) {
					String roleFQDN = YamlUtil.getRoleOrHostFromYaml(key.getSourceName());
	
					// Apply the YAML updates for current Hiera file, one by one 
					try {
						for (EnvironmentUpdate update : updatesForEnvironment.get(key)) {
							String zoneName = update.getZoneName();
							update.doUpdate(zoneName, baseCommitString);
							
							if (zonesRolesAndFQDNs.containsKey(zoneName)) {
								zonesRolesAndFQDNs.get(zoneName).add(roleFQDN);
							} else {
								Set<String> rolesAndFQDNs = new TreeSet<>();
								rolesAndFQDNs.add(roleFQDN);
								zonesRolesAndFQDNs.put(zoneName, rolesAndFQDNs);
							}
						}
					} catch (Exception e1) {
						String message = String.format("Failed during YAML Update, fatal for %s", baseCommitString);
						updateStatus(deployment, t, DeploymentStatus.ERRORED, e1, message);
						throw new RuntimeException(message, e1);
					}
				}
	
				// Commit all YAML updates
				try {
					git.commitBranchMergeToMaster(baseCommitString);
				} catch (Exception e) {
					String message = String.format("Failed during merge and commit phase, fatal for %s", baseCommitString);
					updateStatus(deployment, t, DeploymentStatus.ERRORED, e, message);
					throw new RuntimeException(message, e);
				}

				try {
				//Invoke a puppet run
				invokePuppetRun(puppet, zonesRolesAndFQDNs, baseCommitString, t);
				} catch (Exception e) {
					updateStatus(deployment, t, DeploymentStatus.ERRORED, e, e.getMessage());
					throw e;
				}
			}
			
			// Execute the wait for the transition last.  
			// In all cases if there is a wait there will be no yaml updates (based on the way actions always result in a fresh transition ).
			// In the unlikely event that we do get transitions as well then the waits are ordered to run after the yaml updates and the corresponding puppet run
			int waitSeconds = t.getWaitSeconds();
			if (waitSeconds > 0) {
				long waitMillis = waitSeconds * 1000L;
				log.info(String.format("Sleeping now for %d seconds until %s", waitSeconds, new Date(System.currentTimeMillis() + waitMillis)));
				try {
					Thread.sleep(waitMillis);
				} catch (InterruptedException e) {
					log.error("Failed to sleep during transition", e);
				}
				log.info(String.format("Awoken from sleep after %d seconds", waitSeconds));
			}
			
			// This will only get called if the puppet run was successful
			updateStatus(deployment, t, DeploymentStatus.COMPLETED, null, null);
			
			if (t.isStopAfter()) {
				log.info(String.format("STOPPING DEPLOYMENT NOW, AS REQUESTED WITH MESSAGE: %s", t.getStopMessage() != null ? t.getStopMessage() : ""));
				break;
			}
		}
	}
	
	/**
	 * Updates the transition status and writes the report
	 * @param deployment
	 * @param transition
	 * @param status
	 * @param Exception null if not set
	 * @param message null if not set
	 */
	private void updateStatus(CompositeReleaseDeployment deployment, Transition transition, DeploymentStatus status, Exception exception, String message) {
		transition.setStatus(status);
		transition.setException(exception);
		transition.setStatusMessage(message);
		try {
			new CompositeReleaseReport(true, true).generateReport(deployment);
		} catch (Exception e) {
			log.error("Failed to dump the report", e);
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
	private void invokePuppetRun(EMPuppetManager puppet, Map<String, Set<String>> zoneRolesFQDNs, String baseCommitString, Transition transition) {
		// Update the Puppet Master so that it has all the YAML changes for this Hiera file
		String startPuppetUpdateMessage = String.format("Updating Puppet Master for %s", baseCommitString);
		log.info(startPuppetUpdateMessage);
		int exitCode = puppet.updatePuppetMaster(deployment.getOrganisation());

		if (exitCode != 0) {
			throw new RuntimeException(String.format("Did not successfully update the Puppet Master, fatal for %s", baseCommitString));
		}
		
		int retryCount = Configuration.getPuppetRetryCount();
		int retryDelaySeconds = Configuration.getPuppetRetryDelaySeconds();
		
		// Trigger a Puppet run for changes made to this Hiera file
		String startPuppetRunMessage = String.format("Starting Puppet run for %s", baseCommitString);
		log.info(startPuppetRunMessage);
		ExecReturn exitStatus = puppet.doPuppetRunWithRetry(deployment.getOrganisation(), zoneRolesFQDNs, retryCount, retryDelaySeconds);
		String finishPuppetRunMessage = String.format("Finished Puppet run for %s", baseCommitString);
		log.info(finishPuppetRunMessage);
		
		if (exitStatus.getReturnCode() != 0) {
			transition.setLog(exitStatus.getStdOut());
			throw new RuntimeException(String.format("Did not successfully perform the Puppet run, exit code was %s for %s", exitStatus.getReturnCode(), baseCommitString));
		}
	}
	
	/**
	 * This method handles executing a command.
	 * @param puppet
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
