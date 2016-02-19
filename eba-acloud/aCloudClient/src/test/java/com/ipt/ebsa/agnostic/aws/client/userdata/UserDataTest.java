package com.ipt.ebsa.agnostic.aws.client.userdata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.agnostic.client.aws.extensions.IptNetworkInterface;
import com.ipt.ebsa.agnostic.client.aws.module.handler.UserDataHandler;
import com.ipt.ebsa.agnostic.client.controller.Controller;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;

/**
 * 
 *
 */
public class UserDataTest {
	
	XMLVirtualMachineContainerType vmc;
	XMLVirtualMachineType virtualMachine1;
	XMLVirtualMachineType virtualMachine2;
	Collection<IptNetworkInterface> nics;
	File baseDir = new File("src/test/resources/aws/");
	
	String mac = "06:10:9f:f3:80:0";
	
	@Before
	public void setUp() throws Exception {
		
		Controller controllerInstance = new Controller();
		XMLGeographicContainerType geographic = controllerInstance.loadConfiguration(new File("src/test/resources/aws/awsGenericEnvironment.xml"));
		XMLEnvironmentContainerType envContainer = geographic.getEnvironmentContainer();
		
		vmc = envContainer.getEnvironment().get(0).getEnvironmentDefinition().get(0).getVirtualMachineContainer().get(0);
		virtualMachine1 = vmc.getVirtualMachine().get(0);
		List<XMLNICType> vmNics = virtualMachine1.getNIC();
		
		nics = new ArrayList<IptNetworkInterface>();
		for(XMLNICType nic : vmNics) {
			IptNetworkInterface iptNic = new IptNetworkInterface();
			iptNic.setPrimary(nic.isPrimary());
			iptNic.setDeviceIndex(nic.getIndexNumber().intValue());
			iptNic.setPrivateIpAddress(nic.getInterface().get(0).getStaticIpAddress());
			iptNic.setMacAddress(mac+nic.getIndexNumber().intValue());
			nics.add(iptNic);
		}
		
		virtualMachine2 = vmc.getVirtualMachine().get(1);
	}
	
	@After
	public void tareDown() {
		vmc = null;
		virtualMachine1 = null;
		virtualMachine2 = null;
		nics = null;
	}
	
	@Test
	public void createMultiPartDataWithCustomisationScript() throws UnresolvedDependencyException, IOException {	
		UserDataHandler usrDataHandler = new UserDataHandler(baseDir.getAbsolutePath(),"toolingDomain.local", "192.168.1.1");
		String userData = usrDataHandler.getUserDataMultiPartMime(false,vmc, virtualMachine1, nics);
		Assert.assertTrue("Should have found the mac in the script",userData.contains(mac+"0"));
	}
	
	@Test
	public void createMultiPartDataWithoutCustomisationScript() throws UnresolvedDependencyException, IOException {
		UserDataHandler usrDataHandler = new UserDataHandler(baseDir.getAbsolutePath(),"toolingDomain.local", "192.168.1.1");
		String userData = usrDataHandler.getUserDataMultiPartMime(false,vmc, virtualMachine2, nics);
		Assert.assertTrue("Should have found the mac in the script",userData.contains("hostname: afwam02"));
		Assert.assertFalse("Should not have found the ip still present as we have not processed the customisation file",userData.contains("192.168.1.1"));
		
	}

}
