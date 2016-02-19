-- These statements can be used to initialise an empty H2 database
-- the test database is initialised to this level.
-- log in as the admin user using the web ui for H2, connect to the file database.
-- Copy and paste all the statements below.

-- The following three statents will
DROP SCHEMA IF EXISTS RELEASE_MANAGEMENT;
DROP USER IF EXISTS RELEASE_MANAGEMENT;
CREATE USER IF NOT EXISTS RELEASE_MANAGEMENT password 'RELEASE_MANAGEMENT';
ALTER USER RELEASE_MANAGEMENT ADMIN TRUE;
CREATE SCHEMA IF NOT EXISTS RELEASE_MANAGEMENT AUTHORIZATION RELEASE_MANAGEMENT;

DROP TABLE IF EXISTS RELEASE_MANAGEMENT.Application;
CREATE TABLE RELEASE_MANAGEMENT.Application (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) DEFAULT NULL,
  shortName VARCHAR(255) DEFAULT NULL,
  role VARCHAR(250) DEFAULT NULL,
  PRIMARY KEY (id)
) ;

DROP TABLE IF EXISTS RELEASE_MANAGEMENT.ApplicationVersion;
CREATE TABLE RELEASE_MANAGEMENT.ApplicationVersion (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  dateOfRelease DATETIME DEFAULT NULL,
  name VARCHAR(255) DEFAULT NULL,
  notes VARCHAR(10000) DEFAULT NULL,
  version VARCHAR(255) DEFAULT NULL,
  application_id BIGINT DEFAULT NULL,
  jenkinsBuildNumber INT DEFAULT NULL,
  jenkinsJobName VARCHAR(255) DEFAULT NULL,
  jenkinsBuildId VARCHAR(255) DEFAULT NULL,
  dateCreated DATETIME DEFAULT NULL,
  relatedJiraIssue VARCHAR(50) DEFAULT NULL,
  relatedBRPBTDIssue VARCHAR(50) DEFAULT NULL,
  relatedBrpIssue VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (id)
) ;

DROP TABLE IF EXISTS RELEASE_MANAGEMENT.ComponentVersion;
CREATE TABLE RELEASE_MANAGEMENT.ComponentVersion (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  artifactId VARCHAR(255) DEFAULT NULL,
  classifier VARCHAR(255) DEFAULT NULL,
  dateOfRelease DATETIME DEFAULT NULL,
  groupId VARCHAR(255) DEFAULT NULL,
  name VARCHAR(255) DEFAULT NULL,
  notes VARCHAR(10000) DEFAULT NULL,
  packaging VARCHAR(255) DEFAULT NULL,
  type VARCHAR(255) DEFAULT NULL,
  componentVersion VARCHAR(255) DEFAULT NULL,
  application_id BIGINT DEFAULT NULL,
  jenkinsBuildNumber INT DEFAULT NULL,
  jenkinsJobName VARCHAR(255) DEFAULT NULL,
  jenkinsBuildId VARCHAR(255) DEFAULT NULL,
  rpmPackageName VARCHAR(500) DEFAULT NULL,
  rpmPackageVersion VARCHAR(255) DEFAULT NULL,
  ciStatus TINYINT DEFAULT NULL,
  cbtStatus TINYINT DEFAULT NULL,
  PRIMARY KEY (id)
) ;

DROP TABLE IF EXISTS RELEASE_MANAGEMENT.ApplicationVersion_ComponentVersion;
CREATE TABLE RELEASE_MANAGEMENT.ApplicationVersion_ComponentVersion (
  applications_id BIGINT NOT NULL,
  components_id BIGINT NOT NULL,
  PRIMARY KEY (applications_id, components_id)
) ;

DROP TABLE IF EXISTS RELEASE_MANAGEMENT.ApplicationVersionDeployment;
CREATE TABLE RELEASE_MANAGEMENT.ApplicationVersionDeployment (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  applicationversion_id BIGINT DEFAULT NULL,
  dateStarted DATETIME DEFAULT NULL,
  dateCompleted DATETIME DEFAULT NULL,
  environment VARCHAR(255) DEFAULT NULL,
  succeeded bit(1) DEFAULT NULL,
  status VARCHAR(255) DEFAULT NULL,
  plan MEDIUMTEXT DEFAULT NULL,
  log MEDIUMTEXT DEFAULT NULL,
  jenkinsBuildNumber INT DEFAULT NULL,
  jenkinsJobName VARCHAR(255) DEFAULT NULL,
  jenkinsBuildId VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS RELEASE_MANAGEMENT.ReleaseVersion;
CREATE TABLE RELEASE_MANAGEMENT.ReleaseVersion (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  dateOfRelease DATETIME DEFAULT NULL,
  name VARCHAR(255) DEFAULT NULL,
  notes VARCHAR(10000) DEFAULT NULL,
  version VARCHAR(255) DEFAULT NULL,
  jenkinsBuildNumber INT DEFAULT NULL,
  jenkinsJobName VARCHAR(255) DEFAULT NULL,
  jenkinsBuildId VARCHAR(255) DEFAULT NULL,
  dateCreated DATETIME DEFAULT NULL,
  relatedJiraIssue VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (id)
) ;

DROP TABLE IF EXISTS RELEASE_MANAGEMENT.ReleaseVersion_ApplicationVersion;
CREATE TABLE RELEASE_MANAGEMENT.ReleaseVersion_ApplicationVersion (
  releaseVersions_id BIGINT NOT NULL,
  applicationVersions_id BIGINT NOT NULL,
  PRIMARY KEY (releaseVersions_id, applicationVersions_id)
) ;

DROP TABLE IF EXISTS RELEASE_MANAGEMENT.ReleaseVersionDeployment;
CREATE TABLE RELEASE_MANAGEMENT.ReleaseVersionDeployment (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  releaseVersion_id BIGINT DEFAULT NULL,
  dateStarted DATETIME DEFAULT NULL,
  dateCompleted DATETIME DEFAULT NULL,
  environment VARCHAR(255) DEFAULT NULL,
  succeeded bit(1) DEFAULT NULL,
  status VARCHAR(255) DEFAULT NULL,
  plan MEDIUMTEXT DEFAULT NULL,
  log MEDIUMTEXT DEFAULT NULL,
  jenkinsBuildNumber INT DEFAULT NULL,
  jenkinsJobName VARCHAR(255) DEFAULT NULL,
  jenkinsBuildId VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- Add foreign key constraints

-- ApplicationVersion.application_id is a foreign key to Application.id
ALTER TABLE RELEASE_MANAGEMENT.ApplicationVersion 
	ADD CONSTRAINT FK_ApplicationVersion_Application
	FOREIGN KEY (application_id) REFERENCES RELEASE_MANAGEMENT.Application(id);

-- ComponentVersion.application_id is a foreign key to Application.id
ALTER TABLE RELEASE_MANAGEMENT.ComponentVersion 
	ADD CONSTRAINT FK_ComponentVersion_Application
	FOREIGN KEY (application_id) REFERENCES RELEASE_MANAGEMENT.Application(id);

-- ApplicationVersion_ComponentVersion.applications_id is a foreign key to ApplicationVersion.id
ALTER TABLE RELEASE_MANAGEMENT.ApplicationVersion_ComponentVersion 
	ADD CONSTRAINT FK_ApplicationVersionComponentVersion_ApplicationVersion
	FOREIGN KEY (applications_id) REFERENCES RELEASE_MANAGEMENT.ApplicationVersion(id) ON DELETE CASCADE;
	
-- ApplicationVersion_ComponentVersion.components_id is a foreign key to ComponentVersion.id
ALTER TABLE RELEASE_MANAGEMENT.ApplicationVersion_ComponentVersion 
	ADD CONSTRAINT FK_ApplicationVersionComponentVersion_ComponentVersion
	FOREIGN KEY (components_id) REFERENCES RELEASE_MANAGEMENT.ComponentVersion(id) ON DELETE CASCADE;
	
-- ApplicationVersionDeployment.applicationversion_id is a foreign key to ApplicationVersion.id
ALTER TABLE RELEASE_MANAGEMENT.ApplicationVersionDeployment 
	ADD CONSTRAINT FK_ApplicationVersionDeployment_ApplicationVersion
	FOREIGN KEY (applicationversion_id) REFERENCES RELEASE_MANAGEMENT.ApplicationVersion(id);
	
-- ReleaseVersion_ApplicationVersion.releaseVersions_id is a foreign key to ReleaseVersion.id
ALTER TABLE RELEASE_MANAGEMENT.ReleaseVersion_ApplicationVersion 
	ADD CONSTRAINT FK_ReleaseVersionApplicationVersion_ReleaseVersion
	FOREIGN KEY (releaseVersions_id) REFERENCES RELEASE_MANAGEMENT.ReleaseVersion(id) ON DELETE CASCADE;
	
-- ReleaseVersion_ApplicationVersion.applicationVersions_id is a foreign key to ApplicationVersion.id
ALTER TABLE RELEASE_MANAGEMENT.ReleaseVersion_ApplicationVersion 
	ADD CONSTRAINT FK_ReleaseVersionApplicationVersion_ApplicationVersion
	FOREIGN KEY (applicationVersions_id) REFERENCES RELEASE_MANAGEMENT.ApplicationVersion(id) ON DELETE CASCADE;
	
-- ReleaseVersionDeployment.releaseVersion_id is a foreign key to ReleaseVersion.id
ALTER TABLE RELEASE_MANAGEMENT.ReleaseVersionDeployment 
	ADD CONSTRAINT FK_ReleaseVersionDeployment_ReleaseVersion
	FOREIGN KEY (releaseVersion_id) REFERENCES RELEASE_MANAGEMENT.ReleaseVersion(id);
