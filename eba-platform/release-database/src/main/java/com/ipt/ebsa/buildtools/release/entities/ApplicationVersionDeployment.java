package com.ipt.ebsa.buildtools.release.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Type;

@Entity
@NamedQueries({
@NamedQuery(
		name="findAllApplicationVersionDeploymentsUsingApplicationVersion",
	    query="select x from ApplicationVersionDeployment x where x.applicationVersion = :applicationVersion order by x.dateStarted desc"
	),
@NamedQuery(
		name="findAllApplicationVersionDeployments",
	    query="select x from ApplicationVersionDeployment x order by x.dateStarted desc"
	)
})
public class ApplicationVersionDeployment {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	private String environment;

	private String status;

	@Lob
	// This type is needed for the CLOB to work properly on Postgres
	@Type(type = "org.hibernate.type.StringClobType")
	private String plan;
	
	@Lob
	// This type is needed for the CLOB to work properly on Postgres
	@Type(type = "org.hibernate.type.StringClobType")
	private String log;
	
	private Boolean succeeded;

    private String jenkinsJobName;
	
	private int jenkinsBuildNumber;

	private String jenkinsBuildId;
	
	private Date dateStarted;
	
	private Date dateCompleted;
		
	@ManyToOne
	private ApplicationVersion applicationVersion;

	@Override
	public String toString() {
		//NB: Log not included as it's HUGE.
		ToStringBuilder toStringBuilder = new ToStringBuilder(this)
			.append("id", this.id)
			.append("environment", this.environment)
			.append("status", this.status)
			.append("plan (length)", this.plan == null ? 0 : this.plan.length())
			.append("plan", this.plan)
			.append("log (length)", this.log == null ? 0 : this.log.length())
			.append("succeeded", this.succeeded)
			.append("jenkinsJobName", this.jenkinsJobName)
			.append("jenkinsBuildNumber", this.jenkinsBuildNumber)
			.append("dateStarted", this.dateStarted)
			.append("dateCompleted", this.dateCompleted)
			.append("applicationVersion", this.applicationVersion);
		
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

	public ApplicationVersion getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(ApplicationVersion applicationVersion) {
		this.applicationVersion = applicationVersion;
	}
    
}
