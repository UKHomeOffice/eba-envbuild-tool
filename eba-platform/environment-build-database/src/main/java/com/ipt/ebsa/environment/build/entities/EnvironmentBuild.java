package com.ipt.ebsa.environment.build.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Type;


/**
 * The persistent class for the environmentbuild database table.
 * 
 */
@Entity
@NamedQueries({
	@NamedQuery(name="EnvironmentBuild.findAll", query="SELECT e FROM EnvironmentBuild e"),
	@NamedQuery(name="EnvironmentBuild.findLastSuccesful", query="SELECT e FROM EnvironmentBuild e , EnvironmentDefinition ed where ed.name = :name and e.succeeded = true order by e.datecompleted desc")
})

public class EnvironmentBuild implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private Date datecompleted;

	private Date datestarted;

	private String jenkinsbuildid;

	private Integer jenkinsbuildnumber;

	private String jenkinsjobname;
	
	@Lob
	// This type is needed for the CLOB to work properly on H2 and Postgres
	@Type(type = "org.hibernate.type.StringClobType")
	private String envxml;

	@Lob
	// This type is needed for the CLOB to work properly on H2 and Postgres
	@Type(type = "org.hibernate.type.StringClobType")
	private String log;

	@Lob
	// This type is needed for the CLOB to work properly on H2 and Postgres
	@Type(type = "org.hibernate.type.StringClobType")
	private String plan;
	
	@Lob
	// This type is needed for the CLOB to work properly on H2 and Postgres
	@Type(type = "org.hibernate.type.StringClobType")
	private String report;

	private Boolean succeeded;

	//bi-directional many-to-one association to Environmentdefinition
	@ManyToOne
	@JoinColumn(name="environmentdefinitionid")
	private EnvironmentDefinition environmentdefinition;

	public EnvironmentBuild() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Date getDateCompleted() {
		return this.datecompleted;
	}

	public void setDateCompleted(Date datecompleted) {
		this.datecompleted = datecompleted;
	}

	public Date getDateStarted() {
		return this.datestarted;
	}

	public void setDateStarted(Date datestarted) {
		this.datestarted = datestarted;
	}

	public String getJenkinsBuildId() {
		return this.jenkinsbuildid;
	}

	public void setJenkinsBuildId(String jenkinsbuildid) {
		this.jenkinsbuildid = jenkinsbuildid;
	}

	public Integer getJenkinsBuildNumber() {
		return this.jenkinsbuildnumber;
	}

	public void setJenkinsBuildNumber(int jenkinsbuildnumber) {
		this.jenkinsbuildnumber = jenkinsbuildnumber;
	}

	public String getJenkinsJobName() {
		return this.jenkinsjobname;
	}

	public void setJenkinsJobName(String jenkinsjobname) {
		this.jenkinsjobname = jenkinsjobname;
	}

	public String getLog() {
		return this.log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public String getPlan() {
		return this.plan;
	}

	public void setPlan(String plan) {
		this.plan = plan;
	}

	public String getReport() {
		return report;
	}

	public void setReport(String report) {
		this.report = report;
	}

	public String getEnvXml() {
		return envxml;
	}

	public void setEnvXml(String envxml) {
		this.envxml = envxml;
	}

	public Boolean getSucceeded() {
		return this.succeeded;
	}

	public void setSucceeded(Boolean succeeded) {
		this.succeeded = succeeded;
	}

	public EnvironmentDefinition getEnvironmentDefinition() {
		return this.environmentdefinition;
	}

	public void setEnvironmentDefinition(EnvironmentDefinition environmentdefinition) {
		this.environmentdefinition = environmentdefinition;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("id", getId()).append("datecompleted", datecompleted)
		.append("datestarted", datestarted)
		.append("jenkinsbuildid", jenkinsbuildid)
		.append("jenkinsbuildnumber", jenkinsbuildnumber)
		.append("jenkinsjobname", jenkinsjobname)
		.append("log (length)", null != log ? log.length() : 0)
		.append("plan (length)", null != plan ? plan.length() : 0)
		.append("report (length)", null != report ? report.length() : 0)
		.append("succeeded", succeeded)
		.append("environmentdefinitionid", environmentdefinition.getId()).toString();
	}
}