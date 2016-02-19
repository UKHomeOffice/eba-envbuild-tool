package com.ipt.ebsa.environment.hiera.route;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class RouteTest {

	@Test
	public void testGetRoute1() {
		RouteDetails rd = new RouteDetails();
		rd.setDest("1.22.33/1");
		rd.setRoute("192.168.2.10");
		Route r = Route.parse(rd);
		assertEquals("1.22.33.0/1", r.getCidr());
		assertEquals("192.168.2.10", r.getVia());
	}
	
	@Test
	public void testGetRoute2() {
		RouteDetails rd = new RouteDetails();
		rd.setDest("123.22/24");
		rd.setRoute("1.168.24.10 (text)");
		Route r = Route.parse(rd);
		assertEquals("123.22.0.0/24", r.getCidr());
		assertEquals("1.168.24.10", r.getVia());
	}
	
	@Test
	public void testGetRoute3() {
		RouteDetails rd = new RouteDetails();
		rd.setDest("123.22X/24");
		rd.setRoute("1.168.24.10 (text)");
		try {
			Route.parse(rd);
			fail("bad format of dest");
		} catch (Exception e) {
			assertEquals("Couldn't convert [123.22X/24] to x.x.x.x/y notation", e.getMessage());
		}
	}
	
	@Test
	public void testGetRoute4() {
		RouteDetails rd = new RouteDetails();
		rd.setDest("123.22/24");
		rd.setRoute("1.168.2455.10 (text)");
		try {
			Route.parse(rd);
			fail("bad format of route");
		} catch (Exception e) {
			assertEquals("Failed to get via from [null:null:123.22/24:1.168.2455.10 (text):null]", e.getMessage());
		}
	}
}
