cd /home/scowx/Documents/IPT/Dev/ebsa-ci-components/Function/environment-management
mvn clean install -DskipTests
#mvn clean install 
cd /home/scowx/Documents/IPT/Dev/ebsa-ci-components/ServiceDescription/environment-management-rpm
mvn clean install -Denvironment.management.version=1.0.25-SNAPSHOT
