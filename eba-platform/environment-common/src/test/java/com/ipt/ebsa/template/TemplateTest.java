package com.ipt.ebsa.template;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TemplateTest {
	@Test
	public void simpleTest() {
		TemplateManager tm = new TemplateManager("src/test/resources/template");
		tm.put("placeholder", "maniacs");
		assertEquals("So, what would you little maniacs like to do first?", tm.render("more/lisa.vm"));
	}
}
