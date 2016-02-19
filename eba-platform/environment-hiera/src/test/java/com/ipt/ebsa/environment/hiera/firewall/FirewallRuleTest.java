package com.ipt.ebsa.environment.hiera.firewall;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

public class FirewallRuleTest {
	private static final Logger LOG = Logger.getLogger(FirewallRuleTest.class);
	
	@Test
	public void testParseHost() {
		assertEquals(Arrays.asList(new String[]{"mega01"}), FirewallUtil.parseHostname("mega01"));
		assertEquals(Arrays.asList(new String[]{"mega01","mega02"}), FirewallUtil.parseHostname(" mega01 / 02"));
		
		try {
			FirewallUtil.parseHostname("Checkout my new weapon, weapon of choice.");
			fail("not a hostname");
		} catch (Exception e) {
			assertEquals("Failed to parse hostname [Checkout my new weapon, weapon of choice.]", e.getMessage());
		}
	}
	
	@Test
	public void testParseVyatta() {
		ForwardFirewallDetails fwd = new ForwardFirewallDetails();
		fwd.setComments("");
		fwd.setDesc("test");
		fwd.setDest("127.0.0.1,2,3");
		fwd.setForumSupport("");
		fwd.setHost("mfwam01/02");
		fwd.setPort("25");
		fwd.setProtocol("tcp");
		fwd.setRelatedZones("CTL1_CTZO");
		fwd.setRuleNumber("10,11,12");
		fwd.setSource("192.168.0.0/16");
		fwd.setType("Firewall");
		fwd.setZone("pr-ctl1-inbc");
		fwd.setVersion("0.1");
		
		ForwardFirewallRule[] rulesActual = ForwardFirewallRule.parse(fwd).toArray(new ForwardFirewallRule[]{});
		
		ForwardFirewallRule rule0 = new ForwardFirewallRule();
		rule0.setDesc("test");
		rule0.setDest("127.0.0.1");
		rule0.setHost("mfwam01");
		rule0.setPort("25");
		rule0.setProtocol("tcp");
		rule0.setSource("192.168.0.0/16");
		rule0.setZone("pr-ctl1-inbc");
		rule0.setRuleNumber("10");
		
		ForwardFirewallRule rule1 = new ForwardFirewallRule();
		rule1.setDesc("test");
		rule1.setDest("127.0.0.2");
		rule1.setHost("mfwam01");
		rule1.setPort("25");
		rule1.setProtocol("tcp");
		rule1.setSource("192.168.0.0/16");
		rule1.setZone("pr-ctl1-inbc");
		rule1.setRuleNumber("11");
		
		ForwardFirewallRule rule2 = new ForwardFirewallRule();
		rule2.setDesc("test");
		rule2.setDest("127.0.0.3");
		rule2.setHost("mfwam01");
		rule2.setPort("25");
		rule2.setProtocol("tcp");
		rule2.setSource("192.168.0.0/16");
		rule2.setZone("pr-ctl1-inbc");
		rule2.setRuleNumber("12");
		
		ForwardFirewallRule rule3 = new ForwardFirewallRule();
		rule3.setDesc("test");
		rule3.setDest("127.0.0.1");
		rule3.setHost("mfwam02");
		rule3.setPort("25");
		rule3.setProtocol("tcp");
		rule3.setSource("192.168.0.0/16");
		rule3.setZone("pr-ctl1-inbc");
		rule3.setRuleNumber("10");
		
		ForwardFirewallRule rule4 = new ForwardFirewallRule();
		rule4.setDesc("test");
		rule4.setDest("127.0.0.2");
		rule4.setHost("mfwam02");
		rule4.setPort("25");
		rule4.setProtocol("tcp");
		rule4.setSource("192.168.0.0/16");
		rule4.setZone("pr-ctl1-inbc");
		rule4.setRuleNumber("11");
		
		ForwardFirewallRule rule5 = new ForwardFirewallRule();
		rule5.setDesc("test");
		rule5.setDest("127.0.0.3");
		rule5.setHost("mfwam02");
		rule5.setPort("25");
		rule5.setProtocol("tcp");
		rule5.setSource("192.168.0.0/16");
		rule5.setZone("pr-ctl1-inbc");
		rule5.setRuleNumber("12");
		
		ForwardFirewallRule[] rulesExpected = new ForwardFirewallRule[] { rule0, rule1, rule2, rule3, rule4, rule5 };
		
		assertArrayEquals(rulesExpected, rulesActual);
	}
	
	@Test
	public void testInputCommon() {
		InputFirewallDetails fwd = new InputFirewallDetails();
		fwd.setHosts("Common");
		fwd.setPorts("22");
		fwd.setProtocol("tcp");
		fwd.setDesc("ssh");
		fwd.setVersion("1.0");
		fwd.setComments("test test");
		
		List<InputFirewallRule> rules = InputFirewallRule.parse(fwd);
		
		assertEquals(1, rules.size());
		InputFirewallRule rule = new InputFirewallRule();
		rule.setHost("Common");
		rule.setDomain(null);
		rule.setSource(null);
		rule.setPort("22");
		rule.setProtocol("tcp");
		rule.setDesc("ssh");
		rule.setVersion("1.0");
		rule.setComments("test test");
		
		assertEquals(rule, rules.get(0));
	}
	
	@Test
	public void testInputSimple() {
		InputFirewallDetails fwd = new InputFirewallDetails();
		fwd.setHosts("mpxdm01");
		fwd.setZone("pr-prd1-cobc.ipt.ho.local");
		fwd.setPorts("10514");
		fwd.setDesc("syslog");
		fwd.setVersion("1.0");
		fwd.setComments("test test");
		
		List<InputFirewallRule> rules = InputFirewallRule.parse(fwd);
		
		assertEquals(1, rules.size());
		InputFirewallRule rule = new InputFirewallRule();
		rule.setHost("mpxdm01");
		rule.setDomain("pr-prd1-cobc.ipt.ho.local");
		rule.setSource(null);
		rule.setPort("10514");
		rule.setProtocol(null);
		rule.setDesc("syslog");
		rule.setVersion("1.0");
		rule.setComments("test test");
		
		assertEquals(rule, rules.get(0));
	}
	
	@Test
	public void testComplexInput() {
		InputFirewallDetails fwd = new InputFirewallDetails();
		fwd.setHosts("dbsem21/22");
		fwd.setZone("pr-prd1-dazo.ipt.ho.local");
		fwd.setSources("etlga01/02 \n 192.168.32/24");
		fwd.setPorts("1534, 7916");
		fwd.setDesc("BRP");
		fwd.setVersion("1.0");
		fwd.setComments(null);
		
		List<InputFirewallRule> rules = InputFirewallRule.parse(fwd);
		LOG.debug(rules);
		
		assertEquals(12, rules.size());
		
		InputFirewallRule rule = new InputFirewallRule();
		rule.setHost("dbsem21");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("etlga01");
		rule.setPort("1534");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(0));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem21");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("etlga02");
		rule.setPort("1534");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(1));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem21");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("192.168.32.0/24");
		rule.setPort("1534");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(2));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem21");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("etlga01");
		rule.setPort("7916");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(3));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem21");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("etlga02");
		rule.setPort("7916");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(4));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem21");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("192.168.32.0/24");
		rule.setPort("7916");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(5));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem22");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("etlga01");
		rule.setPort("1534");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(6));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem22");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("etlga02");
		rule.setPort("1534");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(7));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem22");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("192.168.32.0/24");
		rule.setPort("1534");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(8));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem22");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("etlga01");
		rule.setPort("7916");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(9));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem22");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("etlga02");
		rule.setPort("7916");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(10));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem22");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("192.168.32.0/24");
		rule.setPort("7916");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(11));
	}
	
	@Test
	public void testComplexInputIP() {
		InputFirewallDetails fwd = new InputFirewallDetails();
		fwd.setHosts("dbsem21");
		fwd.setZone("pr-prd1-dazo.ipt.ho.local");
		fwd.setSources("192.168.0.32,33");
		fwd.setPorts("1534");
		fwd.setDesc("BRP");
		fwd.setVersion("1.0");
		fwd.setComments(null);
		
		List<InputFirewallRule> rules = InputFirewallRule.parse(fwd);
		LOG.debug(rules);
		
		assertEquals(2, rules.size());
		
		InputFirewallRule rule = new InputFirewallRule();
		rule.setHost("dbsem21");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("192.168.0.32");
		rule.setPort("1534");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(0));
		
		rule = new InputFirewallRule();
		rule.setHost("dbsem21");
		rule.setDomain("pr-prd1-dazo.ipt.ho.local");
		rule.setSource("192.168.0.33");
		rule.setPort("1534");
		rule.setProtocol(null);
		rule.setDesc("BRP");
		rule.setVersion("1.0");
		rule.setComments(null);
		assertEquals(rule, rules.get(1));
	}
}
