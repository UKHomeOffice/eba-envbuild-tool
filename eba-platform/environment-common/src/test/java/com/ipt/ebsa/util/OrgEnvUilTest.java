package com.ipt.ebsa.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import static com.ipt.ebsa.util.OrgEnvUtil.*;

public class OrgEnvUilTest {
	
	@Test
	public void getDomainForPuppetTest() {
		// All domains are lowercase and have underscores replaced with hyphens just before being returned
		// Env starts with HO_IPT_ so has that removed and .ipt.ho.local appended
		assertEquals("np-prp1.ipt.ho.local", getDomainForPuppet("HO_IPT_NP_PRP1"));
		// Env starts with IPT_ so has that removed and .ipt.local appended
		assertEquals("st-dev1.ipt.local", getDomainForPuppet("IPT_ST_DEV1"));
		// Env starts with IPT_ so has that removed and .ipt.local appended 
		assertEquals("st-dev1-ebs2.ipt.local", getDomainForPuppet("IPT_ST_DEV1_EBS2"));
	}
	
	@Test
	public void getOrganisationNameTest() {
		assertEquals("np", getOrganisationName("HO_IPT_NP_PRP1"));
		assertEquals("st", getOrganisationName("IPT_ST_DEV1"));
		assertEquals("st", getOrganisationName("IPT_ST_DEV1_EBS2"));
	}
	
	@Test
	public void getEnvironmentNameTest() {
		assertEquals("np-prp1-dazo", getEnvironmentName("HO_IPT_NP_PRP1_DAZO"));
		assertEquals("st-dev1", getEnvironmentName("IPT_ST_DEV1"));
		assertEquals("st-dev1-ebs2", getEnvironmentName("IPT_ST_DEV1_EBS2"));
	}
	
	@Test(expected=RuntimeException.class)
	public void getDomainForPuppetTestException() {
		// Invalid environment
		OrgEnvUtil.getDomainForPuppet("blahblah");
	}
	
	@Test
	public void getZoneOrEnvLCNoOrgPrefixTest() {
		assertEquals("st-sit1", getZoneOrEnvLCNoOrgPrefix("IPT_ST_SIT1"));
		assertEquals("st-sit1-cor1", getZoneOrEnvLCNoOrgPrefix("IPT_ST_SIT1_COR1"));
		assertEquals("np-prp1", getZoneOrEnvLCNoOrgPrefix("HO_IPT_NP_PRP1"));
		assertEquals("np-prp1-eszo", getZoneOrEnvLCNoOrgPrefix("HO_IPT_NP_PRP1_ESZO"));
		assertEquals("pr-prd1", getZoneOrEnvLCNoOrgPrefix("HO_IPT_PR_PRD1"));
		assertEquals("pr-prd1-eszo", getZoneOrEnvLCNoOrgPrefix("HO_IPT_PR_PRD1_ESZO"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void getZoneOrEnvLCNoOrgPrefixFail() {
		getZoneOrEnvLCNoOrgPrefix("blah");
	}
	
	@Test
	public void getZoneOrEnvUCWithOrgPrefixTest() {
		assertEquals("IPT_ST_SIT1", getZoneOrEnvUCWithOrgPrefix("st-sit1"));
		assertEquals("IPT_ST_SIT1_COR1", getZoneOrEnvUCWithOrgPrefix("st-sit1-cor1"));
		assertEquals("HO_IPT_NP_PRP1", getZoneOrEnvUCWithOrgPrefix("np-prp1"));
		assertEquals("HO_IPT_NP_PRP1_ESZO", getZoneOrEnvUCWithOrgPrefix("np-prp1-eszo"));
		assertEquals("HO_IPT_PR_PRD1", getZoneOrEnvUCWithOrgPrefix("pr-prd1"));
		assertEquals("HO_IPT_PR_PRD1_ESZO", getZoneOrEnvUCWithOrgPrefix("pr-prd1-eszo"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void getZoneOrEnvUCWithOrgPrefixFail() {
		getZoneOrEnvUCWithOrgPrefix("blah");
	}
}
