package com.ipt.ebsa.buildtools.release.entities;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
	@NamedQuery(
		name="findAllApplicationVersionsUsingApplication",
	    query="select x from ApplicationVersion x where x.application = :application order by x.id desc"
	),
	@NamedQuery(
		name="findAllApplicationVersions",
	    query="select x from ApplicationVersion x order by x.application.id desc, x.dateOfRelease desc"
	),
	@NamedQuery(
		name="findAllUsingApplicationVersionIds",
	    query="select x from ApplicationVersion x where x.id in :applicationVersionIds order by x.name asc, x.version desc"
	),
	@NamedQuery(
		name="countApplicationVersionsWithNameAndVersion",
	    query="select count(x) from ApplicationVersion x where x.name = :name and x.version = :version"
	),
	@NamedQuery(
		name="countApplicationVersionsWithAppAndVersion",
	    query="select count(x) from ApplicationVersion x where x.application.id = :appId and x.version = :version"
	)
})
public class ApplicationVersion implements ReleaseEntity {
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

    private String relatedBrpIssue;
	
	private Date dateOfRelease;
	
	private Date dateCreated;
		
	@ManyToOne
	private Application application;
	
	@ManyToMany
	private List<ComponentVersion> components;
	
	@ManyToMany(mappedBy = "applicationVersions")
	private Set<ReleaseVersion> releaseVersions;
	
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append(String.format("id='%s', ",id));
		b.append(String.format("name='%s', ",name));
		b.append(String.format("dateCreated='%s', ",dateCreated));
		b.append(String.format("dateOfRelease='%s', ",dateOfRelease));
		b.append(String.format("application='%s, '",application != null ? application.getName() : "null"));
		b.append(String.format("version='%s', ",version));
		b.append(String.format("relatedJiraIssue='%s', ", relatedJiraIssue));
        b.append(String.format("relatedBrpIssue='%s', ",relatedBrpIssue));
		b.append(String.format("notes='%s'", notes));
		return b.toString();
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

	public List<ComponentVersion> getComponents() {
		return components;
	}

	public void setComponents(List<ComponentVersion> components) {
		this.components = components;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
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

    public String getRelatedBrpIssue() {
        return relatedBrpIssue;
    }

    public void setRelatedBrpIssue(String relatedBrpIssue) {
        this.relatedBrpIssue = relatedBrpIssue;
    }

	public Set<ReleaseVersion> getReleaseVersions() {
		return releaseVersions;
	}

	public void setReleaseVersions(Set<ReleaseVersion> releaseVersions) {
		this.releaseVersions = releaseVersions;
	}
    
}
