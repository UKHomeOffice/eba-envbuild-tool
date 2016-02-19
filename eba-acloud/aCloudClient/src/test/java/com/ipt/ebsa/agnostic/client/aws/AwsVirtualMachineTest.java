package com.ipt.ebsa.agnostic.client.aws;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.IamInstanceProfile;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstancePrivateIpAddress;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeState;
import com.google.code.tempusfugit.concurrency.ConcurrentTestRunner;
import com.ipt.ebsa.agnostic.client.aws.exception.PeeringChoreographyException;
import com.ipt.ebsa.agnostic.client.aws.exception.SubnetUnavailableException;
import com.ipt.ebsa.agnostic.client.aws.exception.VpcUnavailableException;
import com.ipt.ebsa.agnostic.client.exception.FatalException;
import com.ipt.ebsa.agnostic.client.exception.ResourceInUseException;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.jcabi.aspects.Loggable;

/**
 * 
 *
 */
@Loggable(prepend = true)
@RunWith(ConcurrentTestRunner.class)
public class AwsVirtualMachineTest extends AwsBaseTest {

	private static Logger logger = LogManager.getLogger(AwsVirtualMachineTest.class);

	private static AllocateAddressResult elasticIp = null;

	@BeforeClass
	public static void setUpAwsVirtualMachineTestClass() throws InterruptedException, ToManyResultsException, PeeringChoreographyException,
			UnSafeOperationException {
		vmcModule.createVirtualMachineContainer(environment, vmc);
		networkModule.createApplicationSubnet(environment, appNet1, vmc);
		networkModule.createApplicationSubnet(environment, appNet2, vmc);
		networkModule.createOrganisationSubnet(environment, orgNetwork1);
		//networkModule.createOrganisationSubnet(environment, orgNetwork3);
		//gatewayManager.createGateway(environment, gateway);
		resetBaseTestConfig(testPrefixIdent, testPrefixIdentAdditionalVpc);
	}

	@AfterClass
	public static void tearDownAfterAwsVirtualMachineTestClass() throws ToManyResultsException, InterruptedException, UnSafeOperationException {
		envModule.deleteVpc(environment, "UNITTEST");
		try {
			if (elasticIp != null) {
				gatewayManager.releaseAddress(elasticIp.getPublicIp());
			}
		} catch (Exception e) {

		}
	}

	@Before
	public void setUpAwsVirtualMachineTest() {
		logger.debug("I'm running on thread " + Thread.currentThread().getName());
	}

	private Instance getBaseCreateInstance(String suffix, XMLVirtualMachineType vm) throws InterruptedException, StrategyFailureException,
			InvalidStrategyException, ToManyResultsException, VpcUnavailableException, ResourceInUseException, UnSafeOperationException,
			UnresolvedDependencyException, IOException, SubnetUnavailableException, FatalException {
		vm.setVmName(vm.getVmName() + suffix);
		Instance vmInstanceCreated = vmModule.createVirtualMachine(environment, vmc, vm, geographic);
		Assert.assertNotNull(vmInstanceCreated);
		vmModule.waitForInstanceStatus(vmInstanceCreated.getInstanceId(), InstanceStateName.Running, false);
		Instance vmInstanceConfirm = vmModule.confirmVirtualMachine(CmdStrategy.EXISTS, environment, vmc, vm, geographic);
		Assert.assertEquals(vmInstanceConfirm.getImageId(), vm.getTemplateName());
		Assert.assertEquals(vmInstanceConfirm.getBlockDeviceMappings().size(), vm.getStorage().size());
		return vmInstanceConfirm;
	}

	@Test
	public void a_createConfirmDeleteVm() throws Exception {
		String roleName = "001_UNITTEST_ROLE";
		String instanceProfileName = "001_UNITTEST_ROLE.test.domain.local_IP";
		roleManager.removeRoleFromInstanceProfile(instanceProfileName, roleName, true);
		roleManager.deleteRolePolicyForHa(roleName);
		roleManager.deleteInstanceProfile(instanceProfileName);
		roleManager.waitForInstanceProfileDeleted(instanceProfileName);
		//Thread.sleep(30000);

		Instance vmInstanceCreated = getBaseCreateInstance("a", vm1);

		vmModule.waitForInstanceConsoleOutput(vmInstanceCreated.getInstanceId(), "a", true);
		// TODO add more asserts to make this test very comprehensive. Test all
		// the fields to make sure the VM config is loaded correctly.
		vmModule.deleteVirtualMachine(vmInstanceCreated);
		vmModule.waitForInstanceStatus(vmInstanceCreated.getInstanceId(), InstanceStateName.Terminated, true);
		Instance vmInstanceDelete = vmModule.confirmVirtualMachine(CmdStrategy.DOESNOTEXIST, environment, vmc, vm1, geographic);
		roleManager.removeRoleFromInstanceProfile(instanceProfileName, roleName, true);
		roleManager.deleteRolePolicyForHa(roleName);
		roleManager.deleteInstanceProfile(instanceProfileName);
		Assert.assertNull(vmInstanceDelete);
	}

	@Test
	public void b_updateHardwareProfileConfirmDeleteVm() throws Exception {
		Instance vmInstanceConfirm = getBaseCreateInstance("b", vm2);

		List<InstanceBlockDeviceMapping> hddsBefore = vmInstanceConfirm.getBlockDeviceMappings();
		List<InstanceNetworkInterface> nicsBefore = vmInstanceConfirm.getNetworkInterfaces();

		Assert.assertNotNull(vmInstanceConfirm);
		Assert.assertEquals(vmInstanceConfirm.getImageId(), vm2.getTemplateName());
		Assert.assertEquals(vmInstanceConfirm.getBlockDeviceMappings().size(), vm2.getStorage().size());
		vm2.setHardwareProfile("m3.xlarge");
		vmInstanceConfirm = vmModule.updateVirtualMachine(environment, vmc, vm2, geographic, vmInstanceConfirm);
		vmInstanceConfirm = vmModule.confirmVirtualMachine(CmdStrategy.EXISTS, environment, vmc, vm2, geographic);
		vmModule.waitForInstanceStatus(vmInstanceConfirm.getInstanceId(), InstanceStateName.Running, true);
		Assert.assertNotNull(vmInstanceConfirm);
		Assert.assertEquals(vmInstanceConfirm.getImageId(), vm2.getTemplateName());
		Assert.assertEquals(vmInstanceConfirm.getBlockDeviceMappings().size(), vm2.getStorage().size());
		Assert.assertEquals(vmInstanceConfirm.getBlockDeviceMappings().size(), hddsBefore.size());

		for (InstanceBlockDeviceMapping hddBefore : hddsBefore) {
			InstanceBlockDeviceMapping hddMatch = null;
			for (InstanceBlockDeviceMapping hddAfter : vmInstanceConfirm.getBlockDeviceMappings()) {
				if (hddBefore.getEbs().getVolumeId().equals(hddAfter.getEbs().getVolumeId())) {
					hddMatch = hddAfter;
				}
			}
			Assert.assertNotNull(hddMatch);
			Assert.assertEquals(hddBefore.getDeviceName(), hddMatch.getDeviceName());
			//Assert.assertNotEquals(hddBefore.getEbs().getAttachTime().getTime(), hddMatch.getEbs().getAttachTime().getTime());
		}

		Assert.assertEquals(vmInstanceConfirm.getNetworkInterfaces().size(), vm2.getNIC().size());
		Assert.assertEquals(vmInstanceConfirm.getNetworkInterfaces().size(), nicsBefore.size());
		for (InstanceNetworkInterface nicBefore : nicsBefore) {
			InstanceNetworkInterface nicMatch = null;
			for (InstanceNetworkInterface nicAfter : vmInstanceConfirm.getNetworkInterfaces()) {
				if (nicBefore.getNetworkInterfaceId().equals(nicAfter.getNetworkInterfaceId())) {
					nicMatch = nicAfter;
				}
			}
			Assert.assertNotNull(nicMatch);
			Assert.assertEquals(nicBefore.getPrivateIpAddress(), nicMatch.getPrivateIpAddress());
			Assert.assertEquals(nicBefore.getMacAddress(), nicMatch.getMacAddress());
			Assert.assertEquals(nicBefore.getAttachment().getDeviceIndex(), nicMatch.getAttachment().getDeviceIndex());
		}

		Assert.assertEquals(vmInstanceConfirm.getInstanceType(), vm2.getHardwareProfile());
		vmModule.deleteVirtualMachine(vmInstanceConfirm);
		vmModule.waitForInstanceStatus(vmInstanceConfirm.getInstanceId(), InstanceStateName.Terminated, true);
		Instance vmInstanceDelete = vmModule.confirmVirtualMachine(CmdStrategy.DOESNOTEXIST, environment, vmc, vm2, geographic);
		Assert.assertNull(vmInstanceDelete);
	}

	@Test
	public void c_updateHardwareRootDiskNicZeroConfirmDeleteVm() throws Exception {
		Instance vmInstanceConfirm = getBaseCreateInstance("c", vm3);
		nic1c.getInterface().add(getBaseTestVirtualMachineNIC1cIp2(false, testPrefixIdent));

		Instance updated1 = vmModule.updateVirtualMachine(environment, vmc, vm3, geographic, vmInstanceConfirm);
		boolean foundPNic0a = false;
		boolean foundPNic1a = false;
		for (InstanceNetworkInterface nic : updated1.getNetworkInterfaces()) {
			if (nic.getAttachment().getDeviceIndex().intValue() == nic1c.getIndexNumber().intValue()) {
				Set<String> ips = new HashSet<String>();
				for(InstancePrivateIpAddress iNic :nic.getPrivateIpAddresses()) {
					ips.add(iNic.getPrivateIpAddress());
				}
				Assert.assertTrue(ips.contains(nic1c_ip1.getStaticIpAddress()));
				foundPNic0a = true;
				Assert.assertTrue(ips.contains(nic1c_ip2.getStaticIpAddress()));
				foundPNic1a = true;
				
			} 
		}
		Assert.assertTrue(foundPNic0a);
		Assert.assertTrue(foundPNic1a);
		
		// Rebuild config with hdd2 having a larger size
		int doubleSize = hdd1c.getSize().intValue() * 2;
		hdd1c.setSize(new BigInteger(String.valueOf(doubleSize)));
		nic1c_ip1.setStaticIpAddress("10.16.100.60");
		Instance updated2 = vmModule.updateVirtualMachine(environment, vmc, vm3, geographic, vmInstanceConfirm);
		vmModule.waitForInstanceStatus(updated2.getInstanceId(), InstanceStateName.Running, true);

		boolean foundPNic0b = false;
		boolean foundPNic1b = false;
		for (InstanceNetworkInterface nic : updated2.getNetworkInterfaces()) {
			if (nic.getAttachment().getDeviceIndex().intValue() == nic1c.getIndexNumber().intValue()) {
				Set<String> ips = new HashSet<String>();
				for(InstancePrivateIpAddress iNic :nic.getPrivateIpAddresses()) {
					ips.add(iNic.getPrivateIpAddress());
				}
				Assert.assertTrue(ips.contains(nic1c_ip1.getStaticIpAddress()));
				foundPNic0b = true;
				Assert.assertTrue(ips.contains(nic1c_ip2.getStaticIpAddress()));
				foundPNic1b = true;
				
			} 
		}
		Assert.assertTrue(foundPNic0b);
		Assert.assertTrue(foundPNic1b);

		InstanceBlockDeviceMapping hddMatch = null;
		for (InstanceBlockDeviceMapping hddAfter : updated2.getBlockDeviceMappings()) {
			if (hddAfter.getDeviceName().equals(hdd1c.getDeviceMount())) {
				hddMatch = hddAfter;
				break;
			}
		}
		Assert.assertNotNull(hddMatch);
		Volume newVolume = volumeModule.getVolume(hddMatch.getEbs().getVolumeId());
		Assert.assertEquals(hdd1c.getSize().intValue(), newVolume.getSize().intValue());
		Assert.assertEquals(VolumeState.InUse.toString(), newVolume.getState());
		Assert.assertEquals(vm3.getStorage().size(), updated2.getBlockDeviceMappings().size());
		Assert.assertEquals(updated2.getInstanceType(), vm3.getHardwareProfile());
		Assert.assertEquals(InstanceStateName.Terminated.toString(), vmModule.getInstance(vmInstanceConfirm.getInstanceId()).getState().getName());

		for (InstanceNetworkInterface iNic : updated2.getNetworkInterfaces()) {
			switch (iNic.getAttachment().getDeviceIndex()) {
			case 0:
				List<XMLInterfaceType> configNic1 = getBaseTestVirtualMachineNIC1c(false, testPrefixIdent).getInterface();
				Assert.assertEquals(configNic1.get(0).getStaticIpAddress(), iNic.getPrivateIpAddress());
				Assert.assertEquals(configNic1.get(1).getStaticIpAddress(), iNic.getPrivateIpAddresses().get(1).getPrivateIpAddress());
				break;
			case 1:
				List<XMLInterfaceType> configNic2 = getBaseTestVirtualMachineNIC2c(false, testPrefixIdent).getInterface();
				Assert.assertEquals(configNic2.get(0).getStaticIpAddress(), iNic.getPrivateIpAddress());
				break;
			case 2:
				List<XMLInterfaceType> configNic3 = getBaseTestVirtualMachineNIC3c(false, testPrefixIdent).getInterface();
				Assert.assertEquals(configNic3.get(0).getStaticIpAddress(), iNic.getPrivateIpAddress());
				// Assert.assertEquals(configNic3.get(1).getStaticIpAddress(),
				// iNic.getPrivateIpAddresses().get(1).getPrivateIpAddress());
				break;
			}
		}

		vmModule.deleteVirtualMachine(updated2);
		vmModule.waitForInstanceStatus(updated2.getInstanceId(), InstanceStateName.Terminated, true);
		Instance vmInstanceDelete = vmModule.confirmVirtualMachine(CmdStrategy.DOESNOTEXIST, environment, vmc, vm3, geographic);
		Assert.assertNull(vmInstanceDelete);
	}

	@Test
	public void d_updateAddNicStorageConfirmDeleteVm() throws Exception {
		Instance vmInstanceConfirm = getBaseCreateInstance("d", vm4);
		vm4.getNIC().add(getBaseTestVirtualMachineNIC2d(false, testPrefixIdent));
		vm4.getNIC().add(getBaseTestVirtualMachineNIC3d(false, testPrefixIdent));
		vm4.getStorage().add(getBaseTestVirtualMachineStorage2d(false, testPrefixIdent));
		vm4.getStorage().add(getBaseTestVirtualMachineStorage3d(false, testPrefixIdent));
		vmInstanceConfirm = vmModule.updateVirtualMachine(environment, vmc, vm4, geographic, vmInstanceConfirm);
		vmInstanceConfirm = vmModule.confirmVirtualMachine(CmdStrategy.EXISTS, environment, vmc, vm4, geographic);
		vmModule.waitForInstanceStatus(vmInstanceConfirm.getInstanceId(), InstanceStateName.Running, true);

		Assert.assertEquals(vmInstanceConfirm.getInstanceType(), vm4.getHardwareProfile());
		Assert.assertEquals(vm4.getStorage().size(), vmInstanceConfirm.getBlockDeviceMappings().size());
		Assert.assertEquals(vm4.getNIC().size(), vmInstanceConfirm.getNetworkInterfaces().size());

		for (InstanceNetworkInterface iNic : vmInstanceConfirm.getNetworkInterfaces()) {
			switch (iNic.getAttachment().getDeviceIndex()) {
			case 0:
				List<XMLInterfaceType> configNic1 = getBaseTestVirtualMachineNIC1d(false, testPrefixIdent).getInterface();
				Assert.assertEquals(configNic1.get(0).getStaticIpAddress(), iNic.getPrivateIpAddress());
				break;
			case 1:
				List<XMLInterfaceType> configNic2 = getBaseTestVirtualMachineNIC2d(false, testPrefixIdent).getInterface();
				Assert.assertEquals(configNic2.get(0).getStaticIpAddress(), iNic.getPrivateIpAddress());
				break;
			case 2:
				List<XMLInterfaceType> configNic3 = getBaseTestVirtualMachineNIC3d(false, testPrefixIdent).getInterface();
				Assert.assertEquals(configNic3.get(0).getStaticIpAddress(), iNic.getPrivateIpAddress());
				Assert.assertEquals(configNic3.get(1).getStaticIpAddress(), iNic.getPrivateIpAddresses().get(1).getPrivateIpAddress());
				break;
			}
		}

		vmModule.deleteVirtualMachine(vmInstanceConfirm);
		vmModule.waitForInstanceStatus(vmInstanceConfirm.getInstanceId(), InstanceStateName.Terminated, true);
		Instance vmInstanceDelete = vmModule.confirmVirtualMachine(CmdStrategy.DOESNOTEXIST, environment, vmc, vm4, geographic);
		Assert.assertNull(vmInstanceDelete);
	}

	/**
	 * Don't use until in a seperate test account as it moght cause problems with required elastic ip addresses
	 * 
	 * @throws Exception
	 */
	//@Test
	public void e_ElasticIpAllocation() throws Exception {
		List<Address> unallocatedElastic = gatewayManager.getUnallocatedGatewayElasticIpAddress(baseVpc);

		if (unallocatedElastic.size() > 0) {
			// gatewayManager.releaseAddress(unallocatedElastic.get(0).getPublicIp());
			Assert.fail("Cannot test as its unsafe, we have unallocated elastic IP addresses, these may be genuine and so to be safe testing must stop in this account");
		}

		elasticIp = gatewayManager.createElasticIPAddress();
		List<Address> unallocatedElastic2 = gatewayManager.getUnallocatedGatewayElasticIpAddress(baseVpc);

		Assert.assertSame(1, unallocatedElastic2.size());

		Instance vmInstanceCreated = getBaseCreateInstance("e", vm5);
		vmModule.waitForInstanceStatus(vmInstanceCreated.getInstanceId(), InstanceStateName.Running, true);

		List<Address> unallocatedElastic3 = gatewayManager.getUnallocatedGatewayElasticIpAddress(baseVpc);
		Assert.assertSame(0, unallocatedElastic3.size());

		vmModule.deleteVirtualMachine(vmInstanceCreated);
		vmModule.waitForInstanceStatus(vmInstanceCreated.getInstanceId(), InstanceStateName.Terminated, true);
		Instance vmInstanceDelete = vmModule.confirmVirtualMachine(CmdStrategy.DOESNOTEXIST, environment, vmc, vm5, geographic);
		Assert.assertNull(vmInstanceDelete);
	}
	
	@Test
	public void f_updateAddNicStorageConfirmDeleteVm() throws Exception {
		Instance vmInstanceConfirm1 = getBaseCreateInstance("f", vm6);
		Assert.assertEquals(1, vmInstanceConfirm1.getNetworkInterfaces().size()); 
		Assert.assertEquals(4, vmInstanceConfirm1.getNetworkInterfaces().get(0).getPrivateIpAddresses().size()); 
		Assert.assertNotNull(vmInstanceConfirm1.getIamInstanceProfile());
		
		Instance vmInstanceConfirm2 = getBaseCreateInstance("g", vm7);
		Assert.assertEquals(1, vmInstanceConfirm2.getNetworkInterfaces().size()); 
		Assert.assertEquals(1, vmInstanceConfirm2.getNetworkInterfaces().get(0).getPrivateIpAddresses().size());
		Assert.assertNotNull(vmInstanceConfirm2.getIamInstanceProfile());

		XMLInterfaceType ip3 = new XMLInterfaceType();
		ip3.setName("testvm1nic1fip3");
		ip3.setStaticIpAddress("10.16.2.45");
		XMLInterfaceType ip4 = new XMLInterfaceType();
		ip4.setName("testvm1nic1fip4");
		ip4.setIsVip(true);
		ip4.setStaticIpAddress("10.16.2.44");
		XMLInterfaceType ip5 = new XMLInterfaceType();
		ip5.setName("testvm1nic1fip3");
		ip5.setStaticIpAddress("10.16.2.46");
		XMLInterfaceType ip6 = new XMLInterfaceType();
		ip6.setName("testvm1nic1fip4");
		ip6.setIsVip(true);
		ip6.setStaticIpAddress("10.16.2.47");
		XMLInterfaceType ip7 = new XMLInterfaceType();
		ip7.setName("testvm1nic1fip3");
		ip7.setStaticIpAddress("10.16.2.48");
		XMLInterfaceType ip8 = new XMLInterfaceType();
		ip8.setName("testvm1nic1fip4");
		ip8.setIsVip(true);
		ip8.setStaticIpAddress("10.16.2.49");
		XMLInterfaceType ip9 = new XMLInterfaceType();
		ip9.setName("testvm1nic1fip3");
		ip9.setStaticIpAddress("10.16.2.50");
		XMLInterfaceType ip10 = new XMLInterfaceType();
		ip10.setName("testvm1nic1fip4");
		ip10.setIsVip(true);
		ip10.setStaticIpAddress("10.16.2.51");
		
		nic1g.getInterface().add(ip3);
		nic1g.getInterface().add(ip4);
		nic1g.getInterface().add(ip5);
		nic1g.getInterface().add(ip6);
		nic1g.getInterface().add(ip7);
		nic1g.getInterface().add(ip8);
		nic1g.getInterface().add(ip9);
		nic1g.getInterface().add(ip10);
		//vm7.setVmName("a"+vm7.getVmName());
		
		Instance vmInstanceUpdate2 = vmModule.updateVirtualMachine(environment, vmc, vm7, geographic, vmInstanceConfirm2);
		
		Assert.assertEquals(1, vmInstanceUpdate2.getNetworkInterfaces().size()); 
		Assert.assertEquals(8, vmInstanceUpdate2.getNetworkInterfaces().get(0).getPrivateIpAddresses().size());
		Assert.assertNotNull(vmInstanceUpdate2.getIamInstanceProfile());
		
		vmModule.deleteVirtualMachine(vmInstanceConfirm1);
		vmModule.deleteVirtualMachine(vmInstanceUpdate2);
		vmModule.waitForInstanceStatus(vmInstanceConfirm1.getInstanceId(), InstanceStateName.Terminated, true);
		vmModule.waitForInstanceStatus(vmInstanceConfirm2.getInstanceId(), InstanceStateName.Terminated, true);
		Instance vmInstanceDelete1 = vmModule.confirmVirtualMachine(CmdStrategy.DOESNOTEXIST, environment, vmc, vm6, geographic);
		Instance vmInstanceDelete2 = vmModule.confirmVirtualMachine(CmdStrategy.DOESNOTEXIST, environment, vmc, vm7, geographic);
		
		Assert.assertNull(vmInstanceDelete1);
		Assert.assertNull(vmInstanceDelete2);
	}

}
