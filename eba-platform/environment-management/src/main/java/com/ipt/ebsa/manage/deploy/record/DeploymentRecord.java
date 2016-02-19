package com.ipt.ebsa.manage.deploy.record;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class DeploymentRecord {

	private Logger logger = LogManager.getLogger("DeploymentRecord");
	private Date startTime;
	private Date endTime;
	private String status;
	private List<LogItem> logs = new ArrayList<LogItem>();
	
	static class LogItem {
		Date date;
		String message;
		Throwable exception;
		public LogItem(Date date, String message) {
			this.date = date;
			this.message = message;
		}
		public LogItem(Date date, Throwable exception, String message) {
			this.date = date;
			this.message = message;
			this.exception = exception;
		}
	}
	
	public void log(String message) {
		logs.add(new LogItem(new Date(), message));
		logger.info(message);
	}
	
	public void log(Throwable e, String message) {
		logs.add(new LogItem(new Date(), e, message));
		logger.error(message, e);
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		log("STATUS: " + status);
		this.status = status;
	}

	public List<LogItem> getLogs() {
		return logs;
	}

	public void setLogs(List<LogItem> logs) {
		this.logs = logs;
	}
	
	
}
