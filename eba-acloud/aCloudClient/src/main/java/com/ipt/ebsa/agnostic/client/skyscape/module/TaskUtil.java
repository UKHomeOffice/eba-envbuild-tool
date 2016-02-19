package com.ipt.ebsa.agnostic.client.skyscape.module;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;

/**
 * Generic logic for handling waiting for tasks and attempting to cancel them properly if they fail.
 *
 */
public class TaskUtil {
	
	public TaskUtil() {
		
	}
	
	public TaskUtil(int retries, long millisecondsWait) {
		this.retries = retries;
		this.millisecondsWait = millisecondsWait;
	}


	int retries = 60;
	long millisecondsWait = 10000;

	Logger logger = LogManager.getLogger(TaskUtil.class);
	Logger performanceLogger = LogManager.getLogger("performancelogger");
	
	private int retryCount = 0;
		
	/**
	 * Waits for the task to complete, tries to cancel it if it fails or times out
	 * @param taskWrapper
	 */
	public void waitForTask(Task task) {
		boolean fatal = false;
		try {
			try {
				logTask(task, Level.DEBUG);
				task.waitForTask(0,5000);
				logTime(task);
			} catch (TimeoutException e) {
				e.printStackTrace();
				throw new VCloudException("Timeout while waiting for Task");
			}
		}
		catch (Throwable e) {
			logTask(task, Level.ERROR);
			// AA - Added in retry to cope with network glitches
			if (retryCount < retries) {
				logger.debug(String.format("ERROR RETRY = %s", retryCount));
				e.printStackTrace();
				try {
					Thread.sleep(millisecondsWait);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				logger.error("Finished sleeping");
				retryCount++;
				waitForTask(task);
				return;
			}
			if (e.getCause() != null && e.getCause() instanceof java.net.ConnectException) {
				logger.error("We have had a connect exception while waiting for the task to complete ..." + e.getMessage(),e);
				logger.error("Lets wait a bit and then just carry on and see if this resolves itself.  Waiting for " +(millisecondsWait/1000)+ " seconds...");
				try {
					Thread.sleep(millisecondsWait);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				logger.error("Finished sleeping");
				return;
			} else {
				fatal = true;
				logger.error("Error:",e);
				if (task != null) {
					try {
						logger.debug("Attempting to cancel task");
						task.cancelTask();
						logger.debug("Call to \"Cancel task\" completed (note that this does not neccessarily mean that the task has actually been cancelled, it just means that the call to cancel it has completed.)");
					} catch (Exception e1) {
						logger.error("There was an error while cancelling the task",e);
					}
				}
				logger.fatal("Error performing a vcloud command, the system must exit with an error state as this is a fatal error.",e);
				throw new RuntimeException("Error executing task",e);
			}
		} finally {
			if(fatal) {
				logger.fatal("Fatal Error performing a vcloud task, throwing new VCloudException.");
				//  - System.exit(1) changed to throwing a RuntimeException to propagate to calling process for clean termination
				throw new RuntimeException("Fatal Error when executing vCloud task");
			}
		}		
	}
	
	/*
	 * Logs out the relevant details of a task for debugging purposes
	 */
	private void logTask(Task task, Priority priority) {
		// AA - error logging for problem determination in errors thrown during waitForTask
		logger.log(priority, String.format("---------- Activity Task Detail ---------- Attempt = %s", retryCount));
		logger.log(priority, String.format("Get Task Type Operation - %s", task.getResource().getOperation()));
		logger.log(priority, String.format("Get Task Type Id - %s", task.getResource().getId()));
		logger.log(priority, String.format("Get Task Type Type - %s", task.getResource().getType()));
		logger.log(priority, String.format("Get Task Type Status - %s", task.getResource().getStatus()));
		logger.log(priority, String.format("Get Task Type Service Namespace - %s", task.getResource().getServiceNamespace()));
		logger.log(priority, String.format("Get Task Type Organisation Name - %s", task.getResource().getOrganization().getName()));
		logger.log(priority, String.format("Get Task Type Owner Name - %s", task.getResource().getOwner().getName()));
		logger.log(priority, String.format("Get Task Type Start Time - %s", task.getResource().getStartTime()));
		logger.log(priority, String.format("Get Task Type End Time - %s", task.getResource().getEndTime()));
		logger.log(priority, String.format("Get Task Type Details - %s", task.getResource().getDetails()));
		logger.log(priority, String.format("Get Task Type Name - %s", task.getResource().getName()));
		logger.log(priority, String.format("Get Task Type Description - %s", task.getResource().getDescription()));
		logger.log(priority, String.format("Get Task Type Error - %s", task.getResource().getError()));
		logger.log(priority, String.format("Get Task Type Href - %s", task.getResource().getHref()));
		logger.log(priority, String.format("Get Task Type Operation Key - %s", task.getResource().getOperationKey()));
		logger.log(priority, String.format("Get Task Type Operation Name - %s", task.getResource().getOperationName()));
		logger.log(priority, String.format("Get Task Progress - %s", task.getProgress()));
		logger.log(priority, String.format("Get Org Reference - %s", task.getOrgReference().getName()));
		logger.log(priority, String.format("Get User Reference - %s", task.getUserReference().getName()));
		logger.log(priority, "------------------------------------------");		
	}	
	
	/*
	 * Log time time it takes to wait for the task to complete.
	 * This will be slightly inaccurate but will 
	 */
	private void logTime(Task task) {
		XMLGregorianCalendar startTime = task.getResource().getStartTime();
		long millis = Calendar.getInstance().getTimeInMillis() - startTime.toGregorianCalendar().getTimeInMillis();
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
		logger.debug(String.format("%02d:%02d : %s", Math.round(seconds / 60), seconds % 60, task.getResource().getOperation()));
		performanceLogger.debug(String.format("%02d:%02d : %s - %s", Math.round(seconds / 60), seconds % 60, task.getResource().getOperationName(), task.getResource().getOperation()));
	}
}
