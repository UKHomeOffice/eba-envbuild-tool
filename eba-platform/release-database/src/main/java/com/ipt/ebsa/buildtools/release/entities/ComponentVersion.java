package com.ipt.ebsa.buildtools.release.entities;

import java.util.Date;
import java.util.Set;

import javax.persistence.*;

@Entity
@NamedQueries({
	@NamedQuery(
		name="findAllUsingApplication",
	    query="select x from ComponentVersion x where x.application = :application order by x.name asc"
	),
	@NamedQuery(
		name="findAllRpmsUsingApplication",
		query="select x from ComponentVersion x where x.application = :application"
	),
	@NamedQuery(
		name="findAllUsingComponentName",
	    query="select x from ComponentVersion x where x.name = :name order by x.id desc"
		),
	@NamedQuery(
		name="findAllUsingComponentIds",
	    query="select x from ComponentVersion x where x.id in :componentIds order by x.groupId asc, x.artifactId asc, x.componentVersion desc"
	),
    @NamedQuery(
		name="findAll",
	    query="select x from ComponentVersion x order by x.id asc"
    ),
	@NamedQuery(
		name="countComponentVersionsWithGroupArtifactVersion",
		query="select count(x) from ComponentVersion x where x.groupId = :groupId and x.artifactId = :artifactId and x.componentVersion = :version and x.application.shortName = :applicationShortName"
	),
	@NamedQuery(
		name="countComponentVersionsWithGroupArtifactVersionClassifier",
		query="select count(x) from ComponentVersion x where x.groupId = :groupId and x.artifactId = :artifactId and x.componentVersion = :version and x.classifier = :classifier and x.application.shortName = :applicationShortName"
	)
})

public class ComponentVersion implements ReleaseEntity {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	private String name;

	private String groupId;
	
	private String artifactId;
	
	private String componentVersion;

	private String classifier;
	
	private String packaging;

	private String type;
	
    private String rpmPackageName;
    
    private String rpmPackageVersion;

	private String notes;

	@ManyToMany(mappedBy = "components")
	private Set<ApplicationVersion> applications;
	
	private Date dateOfRelease;
	
	private String jenkinsJobName;
	
	private int jenkinsBuildNumber;
	
	private String jenkinsBuildId;

	@Column(nullable = true, columnDefinition = "TINYINT")
	private Boolean ciStatus;

	@Column(nullable = true, columnDefinition = "TINYINT")
	private Boolean cbtStatus;
	
	@ManyToOne
	private Application application;

	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append(String.format("id='%s', ",id));
		b.append(String.format("name='%s', ",name));
		b.append(String.format("dateOfRelease='%s', ",dateOfRelease));
		b.append(String.format("application='%s', ",application != null ? application.getName() : "null"));
		b.append(String.format("groupId='%s', ",groupId));
		b.append(String.format("artifactId='%s', ",artifactId));
		b.append(String.format("componentVersion='%s', ",componentVersion));
		b.append(String.format("classifier='%s', ",classifier));
		b.append(String.format("packaging='%s', ",packaging));
		b.append(String.format("rpmPackageName='%s', ",rpmPackageName));
		b.append(String.format("rpmPackageVersion='%s', ", rpmPackageVersion));
		b.append(String.format("type='%s', ",type));
		b.append(String.format("notes='%s', ",notes));
		b.append(String.format("ciStatus='%s', ",ciStatus));
		b.append(String.format("cbtStatus='%s', ",cbtStatus));
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

	public String getComponentVersion() {
		return componentVersion;
	}

	public void setComponentVersion(String componentVersion) {
		this.componentVersion = componentVersion;
	}

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public Set<ApplicationVersion> getApplications() {
		return applications;
	}

	public void setApplications(Set<ApplicationVersion> applications) {
		this.applications = applications;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String group) {
		this.groupId = group;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifact) {
		this.artifactId = artifact;
	}

	public String getPackaging() {
		return packaging;
	}

	public void setPackaging(String packaging) {
		this.packaging = packaging;
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

	public String getRpmPackageName() {
		return rpmPackageName;
	}

	public void setRpmPackageName(String rpmPackageName) {
		this.rpmPackageName = rpmPackageName;
	}
	
	public String getRpmPackageVersion() {
		return rpmPackageVersion;
	}

	public void setRpmPackageVersion(String rpmPackageVersion) {
		this.rpmPackageVersion = rpmPackageVersion;
	}

	public Boolean getCiStatus() { return ciStatus; }

	public void setCiStatus(Boolean ciStatus) { this.ciStatus = ciStatus; }

	public Boolean getCbtStatus() { return cbtStatus; }

	public void setCbtStatus(Boolean cbtStatus) { this.cbtStatus = cbtStatus; }
}
