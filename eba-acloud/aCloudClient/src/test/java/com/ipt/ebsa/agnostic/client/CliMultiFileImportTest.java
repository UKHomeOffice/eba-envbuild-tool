package com.ipt.ebsa.agnostic.client;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.ipt.ebsa.AgnosticClientCLI;
import com.ipt.ebsa.agnostic.client.controller.Controller;
import com.ipt.ebsa.agnostic.client.skyscape.exception.EnvironmentOverrideException;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.ipt.ebsa.sdkclient.acloudconfig.ACloudConfigurationLoader;

public class CliMultiFileImportTest extends Controller{
	
	@Test
	public void loadNetworkAndEnvironment() throws SAXException, IOException, EnvironmentOverrideException {
		
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		CmdExecute job = loader.loadJob(new File("src/test/resources/aws/createAll.xml"));
		setNetworkPath("src/test/resources/aws/testNetworkLayoutAWS.xml");
		setEnvironmentPath("src/test/resources/aws/testEnvironmentDefinitionAWS.xml");
		XMLGeographicContainerType output = this.getConfigurationWithOverride(job);
		
		Assert.assertTrue(new Integer(1).equals(output.getEnvironmentContainer().getEnvironment().size()));
		Assert.assertTrue(output.getEnvironmentContainer().getEnvironment().get(0).getEnvironmentContainerDefinitionId() != null);
		Assert.assertTrue(output.getEnvironmentContainer().getEnvironment().get(0).getEnvironmentDefinition().size() == 1);
		Assert.assertTrue(output.getEnvironmentContainer().getEnvironment().get(0).getEnvironmentDefinition().get(0).getVirtualMachineContainer().size() == 1);
		Assert.assertTrue(output.getEnvironmentContainer().getEnvironment().get(0).getEnvironmentDefinition().get(0).getVirtualMachineContainer().get(0).getDataCenterId() != null);
		Assert.assertTrue(output.getEnvironmentContainer().getEnvironment().get(0).getEnvironmentDefinition().get(0).getVirtualMachineContainer().get(0).getDataCenterId().getName().equals("eu-west-1a"));
		Assert.assertTrue(output.getEnvironmentContainer().getEnvironment().get(0).getEnvironmentDefinition().get(0).getVirtualMachineContainer().get(0).getNetwork().size() == 2);
		Assert.assertTrue(output.getEnvironmentContainer().getEnvironment().get(0).getEnvironmentDefinition().get(0).getVirtualMachineContainer().get(0).getVirtualMachine().size() == 3);
		for(XMLVirtualMachineType vm:output.getEnvironmentContainer().getEnvironment().get(0).getEnvironmentDefinition().get(0).getVirtualMachineContainer().get(0).getVirtualMachine()) {
			for(XMLNICType nic:vm.getNIC()) {
				Assert.assertTrue(nic.getNetworkID() != null);
				Assert.assertTrue(nic.getNetworkID().getDataCenterId() != null);
				Assert.assertTrue(nic.getNetworkID().getDataCenterId().getName().equals("eu-west-1a"));
			}
		}

	}
	
	/**
	 * Test to make sure that the command line will read in the files and process the files withe options and then just test connection to aws. 
	 * Should not actually provision any VMS.
	 * @throws SAXException
	 * @throws IOException
	 * @throws EnvironmentOverrideException
	 */
	@Test
	public void cliCreateConfirmDeleteNetworkAndEnvironment() throws SAXException, IOException, EnvironmentOverrideException {
		
		String[] confirmDoesNotExistArgs = new String[] { "-command", "execute", "-config", "src/test/resources/aws/aws-test-config.properties", "-networkLayout",
				"src/test/resources/aws/testNetworkLayoutAWS.xml","-environments",
				"src/test/resources/aws/testEnvironmentDefinitionAWS.xml", "-executionplan", "src/test/resources/aws/confirmAllNotExistVapp.xml" };
		AgnosticClientCLI.main(confirmDoesNotExistArgs);
		
		String[] createArgs = new String[] { "-command", "execute", "-config", "src/test/resources/aws/aws-test-config.properties", "-networkLayout",
				"src/test/resources/aws/testNetworkLayoutAWS.xml","-environments",
				"src/test/resources/aws/testEnvironmentDefinitionAWS.xml", "-executionplan", "src/test/resources/aws/createAll.xml" };
		AgnosticClientCLI.main(createArgs);
		
		String[] confirmExistsArgs = new String[] { "-command", "execute", "-config", "src/test/resources/aws/aws-test-config.properties", "-networkLayout",
				"src/test/resources/aws/testNetworkLayoutAWS.xml","-environments",
				"src/test/resources/aws/testEnvironmentDefinitionAWS.xml", "-executionplan", "src/test/resources/aws/confirmAllVapp.xml" };
		AgnosticClientCLI.main(confirmExistsArgs);
		
		String[] deleteArgs = new String[] { "-command", "execute", "-config", "src/test/resources/aws/aws-test-config.properties", "-networkLayout",
				"src/test/resources/aws/testNetworkLayoutAWS.xml","-environments",
				"src/test/resources/aws/testEnvironmentDefinitionAWS.xml", "-executionplan", "src/test/resources/aws/deleteEnvironment.xml" };
		AgnosticClientCLI.main(deleteArgs);
		
		AgnosticClientCLI.main(confirmDoesNotExistArgs);

	}

}
