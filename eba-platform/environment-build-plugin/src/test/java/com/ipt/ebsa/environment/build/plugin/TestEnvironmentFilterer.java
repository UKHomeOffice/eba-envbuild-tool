package com.ipt.ebsa.environment.build.plugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.User;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import jenkins.security.LastGrantedAuthoritiesProperty;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

import org.acegisecurity.GrantedAuthority;
import org.junit.Before;
import org.junit.Test;

public class TestEnvironmentFilterer {

	private JSONObject rawJson;

	@Before
	public void readJSON() throws URISyntaxException, IOException {
		File rawJsonfile = new File(ClassLoader.getSystemResource("envandorgdata.json").toURI());
		rawJson = (JSONObject) new JsonSlurper().parse(rawJsonfile);
	}
	 
	/**
	 * Tests that only environments which *start* with one of the role names are allowed through. The file contains:
	 * - HO_IPT_NP_AS_DAT1_CIZ1 (removed)
	 * - HO_IPT_PR_AS_DAT1_SSZ1 (permitted)
	 * - HO_IPT_NP_PRP1_CIT1 (removed) (even though there's a PR in it)
	 * - HO_IPT_PR_AS_PJT3_CIT1 (permitted)
	 */
	@Test
	public void successfulMatch() {
		User user = mock(User.class);
		LastGrantedAuthoritiesProperty authoritiesProperty = mock(LastGrantedAuthoritiesProperty.class);
		when(user.getProperty(LastGrantedAuthoritiesProperty.class)).thenReturn(authoritiesProperty);
		GrantedAuthority authority = mock(GrantedAuthority.class);
		when(authority.getAuthority()).thenReturn("env_pr_crud");
		when(authoritiesProperty.getAuthorities()).thenReturn(new GrantedAuthority[] {authority});
		
		// Sanity check there's 4 to start with
		JSONArray jsonArray = (JSONArray)rawJson.get("envs");
		assertEquals("Number of Environments in raw JSON", 4, jsonArray.size());
		
		new EnvironmentFilterer().filterEnvironments(rawJson, user);
		
		assertEquals("Number of Environments in raw JSON", 2, jsonArray.size());
		assertEquals("Environment #1", "HO_IPT_PR_AS_DAT1_SSZ1", ((JSONObject)jsonArray.get(0)).get("environment"));
		assertEquals("Environment #2", "HO_IPT_PR_AS_PJT3_CIT1", ((JSONObject)jsonArray.get(1)).get("environment"));
	}
}
