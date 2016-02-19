package com.ipt.ebsa.environment.hiera.route;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.build.entities.VirtualMachineContainer;

public class RouteManagerTest {

	@Test
	public void testHost() {
		RouteManager rm = new RouteManager(new File("src/test/resources/xl/IPTRoutesNP.xls"), null);
		VirtualMachine vm = buildVM("artam01");
		
		Map<String, List<Route>> applicationRoutes = rm.getRoutes(vm);
		
		assertEquals("no of routes", 3, applicationRoutes.get("eth1").size());
		assertEquals("10.48.15.0/24", applicationRoutes.get("eth1").get(0).getCidr());
		assertEquals("10.48.15.1", applicationRoutes.get("eth1").get(0).getVia());
		assertEquals("10.16.33.0/24", applicationRoutes.get("eth1").get(1).getCidr());
		assertEquals("10.48.15.1", applicationRoutes.get("eth1").get(1).getVia());
		assertEquals("10.54.0.0/24", applicationRoutes.get("eth1").get(2).getCidr());
		assertEquals("10.48.15.37", applicationRoutes.get("eth1").get(2).getVia());
	}

	@Test
	public void testCompartment() {
		RouteManager rm = new RouteManager(new File("src/test/resources/xl/IPTRoutesNP.xls"), null);
		VirtualMachine vm = buildVM("docam01");
		
		Map<String, List<Route>> applicationRoutes = rm.getRoutes(vm);
		
		assertEquals("no of routes", 3, applicationRoutes.get("eth1").size());
		assertEquals("10.48.15.0/24", applicationRoutes.get("eth1").get(0).getCidr());
		assertEquals("10.43.0.1", applicationRoutes.get("eth1").get(0).getVia());
		assertEquals("10.16.33.0/24", applicationRoutes.get("eth1").get(1).getCidr());
		assertEquals("10.43.0.1", applicationRoutes.get("eth1").get(1).getVia());
		assertEquals("10.54.0.0/24", applicationRoutes.get("eth1").get(2).getCidr());
		assertEquals("10.43.0.1", applicationRoutes.get("eth1").get(2).getVia());
	}
	
	private VirtualMachine buildVM(String computerName) {
		VirtualMachine vm = new VirtualMachine();
		vm.setComputerName(computerName);
		VirtualMachineContainer vmc = new VirtualMachineContainer();
		vmc.setName("HO_IPT_NP_II_PJT3_DEV1");
		vm.setVirtualmachinecontainer(vmc);
		return vm;
	}
}
