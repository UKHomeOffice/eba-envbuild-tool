package com.ipt.ebsa.buildtools.release.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
@NamedQueries({
@NamedQuery(
		name="findAllReleaseVersionDeploymentsUsingReleaseVersion",
	    query="select x from ReleaseVersionDeployment x where x.releaseVersion = :releaseVersion order by x.dateStarted desc"
	),
@NamedQuery(
		name="findAllReleaseVersionDeployments",
	    query="select x from ReleaseVersionDeployment x order by x.dateStarted desc"
	)
})
public class ReleaseVersionDeployment {
	
	@Id
	@GeneratedValue
	private Long id;

	private String environment;

	private String status;

	@Lob 
	private String plan;
	
	@Lob 
	private String log;
	
	private Boolean succeeded;

    private String jenkinsJobName;
	
	private int jenkinsBuildNumber;

	private String jenkinsBuildId;
	
	private Date dateStarted;
	
	private Date dateCompleted;
		
	@ManyToOne
	private ReleaseVersion releaseVersion;

	@Override
	public String toString() {
		//NB: Log not included as it's HUGE.
		ToStringBuilder toStringBuilder = new ToStringBuilder(this)
			.append("id", this.id)
			.append("environment", this.environment)
			.append("status", this.status)
			.append("plan", this.plan)
			.append("succeeded", this.succeeded)
			.append("jenkinsJobName", this.jenkinsJobName)
			.append("jenkinsBuildNumber", this.jenkinsBuildNumber)
			.append("dateStarted", this.dateStarted)
			.append("dateCompleted", this.dateCompleted)
			.append("releaseVersion", this.releaseVersion);
		
		return toStringBuilder.toString();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPlan() {
		return plan;
	}

	public void setPlan(String plan) {
		this.plan = plan;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public Boolean getSucceeded() {
		return succeeded;
	}

	public void setSucceeded(Boolean succeeded) {
		this.succeeded = succeeded;
	}

	public String getJenkinsJobName() {
		return jenkinsJobName;
	}

	public void setJenkinsJobName(String jenkinsJobName) {
		this.jenkinsJobName = jenkinsJobName;
	}

	public int getJenkinsBuildNumber() {
		return jenkinsBuildNumber;
	}

	public void setJenkinsBuildNumber(int jenkinsBuildNumber) {
		this.jenkinsBuildNumber = jenkinsBuildNumber;
	}

	public String getJenkinsBuildId() {
		return jenkinsBuildId;
	}

	public void setJenkinsBuildId(String jenkinsBuildId) {
		this.jenkinsBuildId = jenkinsBuildId;
	}

	public Date getDateStarted() {
		return dateStarted;
	}

	public void setDateStarted(Date dateStarted) {
		this.dateStarted = dateStarted;
	}

	public Date getDateCompleted() {
		return dateCompleted;
	}

	public void setDateCompleted(Date dateCompleted) {
		this.dateCompleted = dateCompleted;
	}

	public ReleaseVersion getReleaseVersion() {
		return releaseVersion;
	}

	public void setReleaseVersion(ReleaseVersion releaseVersion) {
		this.releaseVersion = releaseVersion;
	}
}
