package com.ipt.ebsa.manage.deploy.impl;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.puppet.SshManager;
import com.ipt.ebsa.manage.util.Utils;
import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.ssh.HostnameUsernamePort;

/**
 * The purpose of this class is to perform the following behaviour:
 * 
 * <ul>
 * <li> Get a list of packages that need to be in the yum repo
 * <li> Create a CSV file with metadata for those packages
 * <li> SCP the CSV file to the correct yum repo box
 * <li> Poll a directory for output from the createrepo-q-tool
 * <li> If a success then print log and continue, if failure then print log and fail
 * </ul>
 * 
 * To use this class you must do the following in order:
 * <ul>
 * <li> {@link #setComponents(Map)} with the component map used elsewhere
 * <li> {@link #createRepoQCsv()} to create the CSV (can be done in the prepare step
 * <li> {@link #doJitYumRepoUpdate(String)} pass the env to we are deploying to, similar
 * to the other deployment classes. Called when we are executing the deployment.
 * </ul>
 * @author James Shepherd
 */
public class JitYumUpdateManager {
	/**
	 * suffix the createrepo-q-tool adds when it places a success log in the outbox
	 */
	private static final String SUCCEED_LOG_SUFFIX = ".log";
	/**
	 * suffix the createrepo-q-tool adds when it places a failure log in the outbox
	 */
	private static final String FAILED_LOG_SUFFIX = "_failed.log";
	private static final DateFormat YMDHMS_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
	private static final Logger	LOG = LogManager.getLogger(JitYumUpdateManager.class);
			
	private Map<ComponentId, ComponentDeploymentData> components;
	private Map<ComponentId, ComponentDeploymentData> componentsThatNeedPackagesInYum = new TreeMap<>();
	private String csvData = null;
	private String createrepoQToolLog = "";

	public JitYumUpdateManager() {
		// default constructor is the only one we use.
	}

	/*
	 * Needs to be called after {@link #setComponents()}
	 */
	public void createRepoQCsv() {
		
		if (null == getComponents()) {
			throw new IllegalArgumentException("must set components map before calling this method");
		}
		
		/* if no components, relax */
		if (components.size() > 0) {
			calculateComponentsThatNeedPackagesInYum();
			if (componentsThatNeedPackagesInYum.size() > 0) {
				makeCsvFile();
				LOG.debug("JIT YUM CSV:\n" + csvData);
			} else {
				LOG.debug("No JIT YUM CSV, as no DEPLOY, DOWNGRADE, UPGRADE");
			}
		}
	}

	/**
	 * Structure in csv is as follows:
	 * field1=rpmPackageName
	 * field2=rpmVersion
	 * field3=groupId
	 * field4=artifactId
	 * field5=version
	 * field6=packaging
	 * field7=classifier (optional)
	 * 
	 * @param toBeDeployed
	 *            components to put in csv file
	 */
	private void makeCsvFile() {
		StringBuilder sb = new StringBuilder();
		
		for (ComponentDeploymentData component : componentsThatNeedPackagesInYum.values()) {
			ComponentVersion cv = component.getTargetCmponentVersion();
			
			if (cv == null) {
				LOG.warn("Attempting to create a CSV entry for component [" + component.getComponentName() + "] with no target version");
				continue;
			}
			
			sb.append(StringUtils.trimToEmpty(cv.getRpmPackageName()))
				.append(',')
				.append(StringUtils.trimToEmpty(cv.getRpmPackageVersion()))
				.append(',')
				.append(StringUtils.trimToEmpty(cv.getGroupId()))
				.append(',')
				.append(StringUtils.trimToEmpty(cv.getArtifactId()))
				.append(',')
				.append(StringUtils.trimToEmpty(cv.getComponentVersion()))
				.append(',')
				.append(StringUtils.trimToEmpty(cv.getPackaging()))
				.append(',')
				.append(StringUtils.trimToEmpty(cv.getClassifier()));
			
			sb.append('\n');
		}
		
		csvData = (csvData == null) ? sb.toString() : String.format("%s%s", csvData, sb.toString());
	}

	private void calculateComponentsThatNeedPackagesInYum() {
		
		// in case for some reason we call this method a second time
		componentsThatNeedPackagesInYum.clear();
		
		LOG.debug("No of components: " + components.values().size());
		
		/* for each component see if it is being deployed */
		COMPONENT:
		for (ComponentDeploymentData component : components.values()) {
			for (ChangeSet changeSet : component.getChangeSets()) {
				if (changeSet.isComplexChange()) {
					for (Change change : changeSet.getSubTasks()) {
						if (isUpdateYumNeeded(change.getChangeType())) {
							componentsThatNeedPackagesInYum.put(component.getComponentId(), component);
							continue COMPONENT;
						}
					}
				} else {
					if (isUpdateYumNeeded(changeSet.getPrimaryChange().getChangeType())) {
						componentsThatNeedPackagesInYum.put(component.getComponentId(), component);
						continue COMPONENT;
					}
				}
			}
		}
		
		LOG.debug("No of rpms: " + componentsThatNeedPackagesInYum.size());
	}

	private boolean isUpdateYumNeeded(ChangeType changeType) {
		switch (changeType) {
		case DEPLOY:
		case DOWNGRADE:
		case UPGRADE:
			return true;
		case FIX:
		case UNDEPLOY:
		case NO_CHANGE:
		case FAIL:
		default:
			return false;
		}
	}

	/**
	 * Needs to be called after {@link #createRepoQCsv()}.
	 * @param zoneName
	 * @throws Exception 
	 */
	public void doJitYumRepoUpdate(Organisation organisation) throws Exception {
		if (Configuration.getYumRepoUpdateEnabled(organisation)) {
			LOG.info("yum repo update enabled");
			if (null != getCsv()) {
				// get all config data
				String username = Configuration.getYumRepoUsername(organisation);
				String host = Configuration.getYumRepoHost(organisation);
				int port = Configuration.getYumRepoUpdatePort(organisation);
				List<HostnameUsernamePort> jumphosts = Utils.getYumRepoJumpHost(organisation, username);
				File yumDir = Utils.getYumRepoUpdateDir(organisation);
				File destinationCsvFile = new File(yumDir, UUID.randomUUID().toString() + ".csv");
				int pollTimeoutMillis = Configuration.getYumRepoUpdatePollTimeoutSecs() * 1000;
				int betweenPollMillis = Configuration.getYumRepoBetweenPollSecs() * 1000;
				int timeout = Configuration.getYumRepoUpdateSshTimeoutMillis();
				final int maxJitExceptionCount = Configuration.getJitYumErrorCount();
				File createqrepoQToolDir = new File(Configuration.getCreaterepoQToolDir());
				File createrepoQToolInboxDir = new File(createqrepoQToolDir, "batch_inbox");
				File createrepoQToolOutboxDir = new File(createqrepoQToolDir, "batch_outbox");
				
				LOG.debug(String.format("Yum Repo Update: %s@%s:%s:%s jumphosts: %s", username, host, port, destinationCsvFile.getPath(), jumphosts));
				
				SshManager ssh = new SshManager();
				
				// upload CSV
				LOG.info(String.format("Uploading CSV file [%s] into folder [%s] on [%s]", destinationCsvFile.getName(), destinationCsvFile.getParent(), host));
				ssh.scpUploadFileContents(timeout, username, host, port, jumphosts, getCsv(), destinationCsvFile);
				
				// upload CSV to inbox of createrepo-q-tool
				File mailFile = new File(createrepoQToolInboxDir, destinationCsvFile.getName() + "." + YMDHMS_FORMAT.format(new Date()));
				File mailFileFailedLog = new File(createrepoQToolOutboxDir, mailFile.getName() + FAILED_LOG_SUFFIX);
				File mailFileLog = new File(createrepoQToolOutboxDir, mailFile.getName() + SUCCEED_LOG_SUFFIX);
				LOG.info(String.format("Uploading CSV file [%s] into batch_inbox [%s] on host [%s]", mailFile.getName(), mailFile.getParent(), host));
				ssh.scpUploadFileContents(timeout, username, host, port, jumphosts, getCsv(), mailFile);
				
				// and now we poll for answer
				long giveUpAt = System.currentTimeMillis() + pollTimeoutMillis;
				ExecReturn lsReturn = null;
				int exceptionCount = 0;
				
				do {
					try{
						lsReturn = ssh.runLsSSH(timeout, createrepoQToolOutboxDir, username, host, port, jumphosts);
						exceptionCount = 0;
					} catch (RuntimeException e){
						Throwable cause = e.getCause();
						if (cause != null && "Auth fail".equals(cause.getMessage())) {
							LOG.warn("Exception doing jitYumRepoUpdate: " + e.toString());
							lsReturn = new ExecReturn(-1);
							exceptionCount++;
						} else {
							throw e;
						}
					}
					
					if (0 != lsReturn.getReturnCode()) {
						if (System.currentTimeMillis() < giveUpAt) {
							LOG.warn("Non-zero return code from ls: " + lsReturn.getReturnCode());
							LOG.warn("Will retry in : " + Configuration.getYumRepoUpdatePollTimeoutSecs());
						} else {
							throw new RuntimeException("Non-zero return code from ls: " + lsReturn.getReturnCode());
						}
					} else {
					
						if (lsReturn.getStdOut().contains(mailFileFailedLog.getName())) {
							LOG.error("createrepo-q-tool failed, will fetch log");
							createrepoQToolLog = ssh.scpDownloadFileContents(timeout, username, host, port, jumphosts, mailFileFailedLog);
							LOG.error(createrepoQToolLog);
							throw new RuntimeException("createrpo-q-tool failed");
						}
						
						if (lsReturn.getStdOut().contains(mailFileLog.getName())) {
							LOG.info("createrepo-q-tool succeeded, will fetch log");
							createrepoQToolLog = ssh.scpDownloadFileContents(timeout, username, host, port, jumphosts, mailFileLog);
							LOG.info(createrepoQToolLog);
							return;
						}
					}
					
					try {
						Thread.sleep(betweenPollMillis);
					} catch (InterruptedException e) {
						// ignore
					}
				} while (System.currentTimeMillis() < giveUpAt && exceptionCount < maxJitExceptionCount);

				throw new RuntimeException("Failed to receive output from createrepo-q-tool - never received log");
			} else {
				LOG.info("No yum repo update to do");
			}
		} else {
			LOG.info("Yum Repo Update disabled");
		}
		
	}
	
	public Map<ComponentId, ComponentDeploymentData> getComponents() {
		return components;
	}

	public void setComponents(Map<ComponentId, ComponentDeploymentData> components) {
		this.components = components;
	}
	
	/**
	 * Needs to be called after {@link #createRepoQCsv()}
	 * @return
	 */
	public Map<ComponentId, ComponentDeploymentData> getComponentsThatNeedPackagesInYum() {
		return componentsThatNeedPackagesInYum;
	}
	
	/**
	 * Needs to be called after {@link #createRepoQCsv()}
	 * @return contents of CSV file, or null if there are no changes
	 */
	public String getCsv() {
		return csvData;
	}

	public String getCreaterepoQToolLog() {
		return createrepoQToolLog;
	}
}
