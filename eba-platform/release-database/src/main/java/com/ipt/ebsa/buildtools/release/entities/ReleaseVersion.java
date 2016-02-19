package com.ipt.ebsa.buildtools.release.entities;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
@NamedQueries({
	@NamedQuery(
		name="findAllReleaseVersions",
	    query="select x from ReleaseVersion x order by x.id desc"
	),
	@NamedQuery(
		name="countReleaseVersionsWithNameAndVersion",
	    query="select count(x) from ReleaseVersion x where x.name = :name and x.version = :version"
	)
})
public class ReleaseVersion implements ReleaseEntity {
	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private String version;

	private String notes;
	
	private String jenkinsJobName;
	
	private int jenkinsBuildNumber;

	private String jenkinsBuildId;
	
	private String relatedJiraIssue;

	private Date dateOfRelease;
	
	private Date dateCreated;
	
	@ManyToMany
	private List<ApplicationVersion> applicationVersions;

	@Override
	public String toString() {
		ToStringBuilder toStringBuilder = new ToStringBuilder(this)
			.append(String.format("id='%s', ",id))
			.append(String.format("name='%s', ",name))
			.append(String.format("dateCreated='%s', ",dateCreated))
			.append(String.format("dateOfRelease='%s', ",dateOfRelease))
			.append(String.format("version='%s', ",version))
			.append(String.format("relatedJiraIssue='%s', ", relatedJiraIssue))
			.append(String.format("notes='%s'", notes));
		return toStringBuilder.toString();
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Date getDateOfRelease() {
		return dateOfRelease;
	}

	public void setDateOfRelease(Date dateOfRelease) {
		this.dateOfRelease = dateOfRelease;
	}

	public List<ApplicationVersion> getApplicationVersions() {
		return applicationVersions;
	}

	public void setApplicationVersions(List<ApplicationVersion> applicationVersions) {
		this.applicationVersions = applicationVersions;
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

	public String getRelatedJiraIssue() {
		return relatedJiraIssue;
	}

	public void setRelatedJiraIssue(String relatedJiraIssue) {
		this.relatedJiraIssue = relatedJiraIssue;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
}
