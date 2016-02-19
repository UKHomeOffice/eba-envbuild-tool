# EBA - Platform
EBA - Platform is a set of components that ties into the Visio designs, stores them in the Environment Build Repository, and provides the Environment Action Repository with valid options for users to build.

## Requirements ##
- **JDK 7**:
	JDK 7 is required 

- **Maven 3**:
Maven is used to resolve the external dependencies during the build process, which usually comes pre-packaged with IDEs. However if you intend to do build outside your IDE or continuous integration tool, you might download it directly from the relevant Apache website.

## Dependencies: ##
In order to build the code, you will need to add the global Jenkins repository to the Maven settings.xml file in order to get the correct jenkins jar version.

Below is a code snippet you can add to your settings.xml file:

	   <repository>
	    <id>jenkins-releases</id>
	    <url>http://repo.jenkins-ci.org/releases/</url>
	   </repository>

The rest of the dependencies can be found in the global Maven repository.



## Build ##
Run a Maven clean install in this order:

		programme-super-parent
		programme-parent
		environment-common
		database-management
		environment-build-database 
		environment-build-metadata-import
		vCloud
		environment-build-metadata-export
		plugin-common
		environment-build-schema
		environment-hiera
		environment-build
		environment-build-plugin
		release-database
		deployment-descriptor-schema
		environment-management
		environment-management-th

## Documentation ##
A very brief description of each module is listed below. More details can be found in the documentation in ./docs/index.html and in the ./javadoc folder under each package. 

**environment-build**	This is the core of the Environment Build Java code behind the environment-build Jenkins "Generate Plan" button

**environment-build-database**		This is the JPA code to read and update objects in the Environment Build Repository (a Postgres liquibase-built database). This is where Environment Definitions and Container Definitions are stored in relational database format

**environment-build-metadata-export**		This is the code that exports Environment Definitions and Container Definitions out of the Environment Build Repository and into XML to be parsed by ACloud

**environment-build-metadata-import**		This is the code that is kicked off by Jenkins when new Environment Definition or Container files are uploaded to Git, unmarshalls the XML into environment-build-database JPA objects and persists them in that database.

**environment-build-plugin**		This is the code behind the Jenkins build plugin, i.e. the screens below the environment build Jenkins "Build with parameters" option
 
**environment-build-schema**		This is the code that executes the sequences of steps coming out of a build plan
 
**environment-common**		Common code to help deal with Git, Hiera, Puppet, YAML and various utility methods

**environment-hiera**		Used to manage access to hieradata files

**environment-management**		Self service

**environment-management-th** 	The Environment Management Test Harness- This is a small application that users can run on their machine to test their deployment descriptors before executing them on Jenkins.

**vcloud** - this is the legacy pre-cursor to ACloud; most of it has been removed except for the vCloudConfiguration project which has the Skyscape xsd file and associated JAXB bindings

**programme-super-parent and programme-parent** these contain super-POMs




