# EBA - ACloud
ACloud stands for "Agnostic Cloud". Its purpose is to take configuration XML, created using network/VM designs created using "EBA - Visio Physical Design" or just hand crafted, and provision them as working, networked VM's in your cloud. It does not include Puppet scripts, middleware configuration or application deployments, it is purely the base OS and networking tier.  

## Requirements ##
- **JDK 7**:   
	JDK 7 is required to build and run ACloud.

- **Maven 3**:  
Maven is used to resolve the external dependencies during the build process. Usually comes pre-packaged with IDEs. However if you intend to do build outside your IDE or continuous integration tool, you might download it directly from the relevant Apache website.

- **Dependencies**:  
In order to build the code, you will need to obtain and install the following artifacts into your local Maven repository.

	- vcloud-java-sdk-5.5.0.jar
		- Instructions: 
			- [Download](https://my.vmware.com/group/vmware/get-download?downloadGroup=VCDSDKJAVA550) the resource named vCloud SDK for Java (filename is "VMware-vCloudDirector-JavaSDK-5.5.0-1294395.zip") which contains the relevant jar. 
			- Run the following command to install the 3rd party JAR to your local Maven repository:  
				`mvn install:install-file -Dfile=[PathToTheJAR]/vcloud-java-sdk-5.5.0.jar -DgroupId=com.vmware -DartifactId=vcloud-java-sdk -Dversion=5.5.0 -Dpackaging=jar -DgeneratePom=true`       

	- rest-api-schemas-5.5.0.jar
		- Instructions: 
			- Use the same zip file downloaded in Step-1, which contains this jar as well.
			- Run the following command to install the 3rd party JAR to your local Maven repository:  
				`mvn install:install-file -Dfile=[PathToTheJAR]/rest-api-schemas-5.5.0.jar -DgroupId=com.vmware -DartifactId=rest-api-schemas -Dversion=5.5.0 -Dpackaging=jar -DgeneratePom=true`


- **Cloud Provider Account**:   
Naturally, an AWS or SkyScape account is needed to provision environments via cloud. Please make sure the cloud provider profile that you will use in ACloud has relevant permissions for the actions you would like to perform, i.e. create/delete VMs etc.  


## Build ##
Use your IDE, continuous integration tool or java command line utilities to run a "Maven install". The process should generate executable files inside the directory "aCloudClient/target/appassembler/bin/"


## Run ##

*Prerequisites:*

- Make sure the cloud provider account (AWS or Skyscape) that you want to use in ACloud has relevant permissions for the actions you would like to perform, i.e. create/delete VMs etc. 

- For AWS, create a keypair named "mykeypair" through the EC2 Console at AWS website (https://console.aws.amazon.com/), in the same region as you would like to work on.  

There are three different ways to launch the ACloud tool:

- **Manual invocation**, via the commandline as follows:  
	AWS:

		cd {PROJECT_HOME}  

		Edit the file "aCloudClient\src\test\resources\aws\aws-test-config.properties" and enter your AWS "Access Key ID" and "Secret Access Key" values into the parameters named "user" and "password" 

		#Confirm that the test resources (VMs and VPCs) don't exist yet  
		aCloudClient\target\appassembler\bin\acloud-app -command execute -config aCloudClient\src\test\resources\aws\aws-test-config.properties -networkLayout aCloudClient\src\test\resources\aws\testNetworkLayoutAWS.xml -environments aCloudClient\src\test\resources\aws\testEnvironmentDefinitionAWS.xml -executionplan aCloudClient\src\test\resources\aws\confirmAllNotExistVapp.xml  
		
		#Create the test resources on AWS cloud  
		aCloudClient\target\appassembler\bin\acloud-app -command execute -config aCloudClient\src\test\resources\aws\aws-test-config.properties -networkLayout aCloudClient\src\test\resources\aws\testNetworkLayoutAWS.xml -environments aCloudClient\src\test\resources\aws\testEnvironmentDefinitionAWS.xml -executionplan aCloudClient\src\test\resources\aws\createAll.xml  
		
		
		#Confirm that the test resources exist  
		aCloudClient\target\appassembler\bin\acloud-app -command execute -config aCloudClient\src\test\resources\aws\aws-test-config.properties -networkLayout aCloudClient\src\test\resources\aws\testNetworkLayoutAWS.xml -environments aCloudClient\src\test\resources\aws\testEnvironmentDefinitionAWS.xml -executionplan aCloudClient\src\test\resources\aws\confirmAllVapp.xml  
		
		
		#Delete the test resources from AWS cloud  
		aCloudClient\target\appassembler\bin\acloud-app -command execute -config aCloudClient\src\test\resources\aws\aws-test-config.properties -networkLayout aCloudClient\src\test\resources\aws\testNetworkLayoutAWS.xml -environments aCloudClient\src\test\resources\aws\testEnvironmentDefinitionAWS.xml -executionplan aCloudClient\src\test\resources\aws\deleteEnvironment.xml  

	Skyscape:

		cd {PROJECT_HOME}  

		Edit the file "aCloudClient\src\test\resources\skyscape\vmware-test-config.properties" and enter all the relevant information

		#Create the test resources on Skyscape cloud
		aCloudClient\target\appassembler\bin\acloud-app -command execute -config aCloudClient\src\test\resources\skyscape\vmware-test-config.properties -networkLayout aCloudClient\src\test\resources\skyscape\testNetworkLayoutvCloud.xml -environments aCloudClient\src\test\resources\skyscape\testEnvironmentDefinitionvCloud.xml -executionplan aCloudClient\src\test\resources\skyscape\createAll.xml


		#Delete the test resources from Skyscape cloud
		aCloudClient\target\appassembler\bin\acloud-app -command execute -config aCloudClient\src\test\resources\skyscape\vmware-test-config.properties -networkLayout aCloudClient\src\test\resources\skyscape\testNetworkLayoutvCloud.xml -environments aCloudClient\src\test\resources\skyscape\testEnvironmentDefinitionvCloud.xml -executionplan aCloudClient\src\test\resources\skyscape\deleteEnvironment.xml



- **Programmatically**, by embedding the ACloud jar (and relevant dependencies) into your project and making calls to the relevant classes. The entry point could be the class "com.ipt.ebsa.AgnosticClientCLI" which has a main() method that handles the command line invocations.
	
- **Executing the tests** located in the class "com.ipt.ebsa.agnostic.client.CliMultifileImportTest" which will create a sample environment under your AWS account and then destroy it. Before running the tests, you would need to:  

		Edit the file "aCloudClient\src\test\resources\aws\aws-test-config.properties" and enter your AWS "Access Key ID" and "Secret Access Key" values into the parameters named "user" and "password", respectively.
		
		Run the tests in "CliMultifileImportTest.java"  


## Customisation ##

ACloud is designed in a modular way to allow the further customisations. One of the points you might be interested is to plug in your own credentials or password manager to keep the ACloud clean from any sensitive data. 

In order to integrate ACloud with your password store, you only need to add a piece of code to the relevant cloud provider connector classes to retrieve the password from your own password manager and overwrite the value derived from the ACloud context. 

For AWS, you need to inject your password manager integration code into the getPassword() method of the AwsConnector class. Similarly for Skyscape, you need to overwrite the value of the 'password' parameter in the login() method of the SkyscapeConnector class.

**Documentation**
-
Documentation can be found in ./docs/index.html 