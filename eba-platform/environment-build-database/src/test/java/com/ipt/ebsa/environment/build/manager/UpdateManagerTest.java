package com.ipt.ebsa.environment.build.manager;

import org.junit.Test;

import com.ipt.ebsa.environment.build.DBTest;
import com.ipt.ebsa.environment.build.entities.ApplicationNetwork;
import com.ipt.ebsa.environment.build.entities.DNat;
import com.ipt.ebsa.environment.build.entities.Environment;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.Gateway;
import com.ipt.ebsa.environment.build.entities.Nat;
import com.ipt.ebsa.environment.build.entities.Nic;
import com.ipt.ebsa.environment.build.entities.OrganisationNetwork;
import com.ipt.ebsa.environment.build.entities.Storage;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;
import com.ipt.ebsa.environment.build.entities.VirtualMachineMetaData;

/**
 * Unit tests for the UpdateManager
 *
 */
public class UpdateManagerTest extends DBTest {
	
	@Test
	public void testCreateEnvironmentDefinition() throws Exception {
		UpdateManager manager = new UpdateManager();
	
		EnvironmentContainer envContainer = getEnvironmentContainer();
		
		Environment environment = getEnvironment();
		envContainer.addEnvironment(environment);
		
		EnvironmentDefinition environmentDefinition = getEnvironmentDefinition();
		environment.addEnvironmentdefinition(environmentDefinition);
		
		entityManager.getTransaction().begin();
		entityManager.persist(envContainer);
		entityManager.flush();
		entityManager.getTransaction().commit();
		
		VirtualMachineContainer vmContainer = getVirtualMachineContainer();
		VirtualMachine vm = getVirtualMachine();
		Storage storage = getStorage();
		vm.addStorage(storage);
		VirtualMachineMetaData metadata = getVirtualMachineMetaData();
		vm.addMetadata(metadata);
		Nic nic = getNic();
		vm.addNic(nic);
		vmContainer.addVirtualmachine(vm);
		ApplicationNetwork network = getApplicationNetwork();
		vmContainer.addNetwork(network);
		environmentDefinition.addVirtualmachinecontainer(vmContainer);
		
		VirtualMachineContainer vmContainer2 = getVirtualMachineContainer();
		VirtualMachine vm2 = getVirtualMachine();
		Storage storage2 = getStorage();
		vm2.addStorage(storage2);
		VirtualMachineMetaData metadata2 = getVirtualMachineMetaData();
		vm2.addMetadata(metadata2);
		Nic nic2 = getNic();
		vm2.addNic(nic2);
		vmContainer2.addVirtualmachine(vm2);
		ApplicationNetwork network2 = getApplicationNetwork();
		vmContainer2.addNetwork(network2);
		environmentDefinition.addVirtualmachinecontainer(vmContainer2);
		
		OrganisationNetwork orgNetwork = getOrganisationNetwork();
		Gateway gateway = getGateway();
		gateway.addNetwork(orgNetwork);
		Nat nat = getNat();
		DNat dnat = getDNat();
		gateway.addNat(nat);
		gateway.addNat(dnat);
		
		manager.createEnvironmentDefinitions(environment);
	}

}
