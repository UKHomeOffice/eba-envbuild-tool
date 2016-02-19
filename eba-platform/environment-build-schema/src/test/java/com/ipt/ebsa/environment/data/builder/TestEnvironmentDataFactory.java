package com.ipt.ebsa.environment.data.builder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.ipt.ebsa.environment.data.factory.EnvironmentDataFactory;
import com.ipt.ebsa.environment.data.model.Build;
import com.ipt.ebsa.environment.data.model.EnvironmentData;
import com.ipt.ebsa.environment.data.model.FirewallHieraAction;
import com.ipt.ebsa.environment.data.model.InfraAction;
import com.ipt.ebsa.environment.data.model.InternalHieraAction;
import com.ipt.ebsa.environment.data.model.ParameterisedNode;
import com.ipt.ebsa.environment.data.model.Sequence;
import com.ipt.ebsa.environment.data.model.SshAction;
import com.ipt.ebsa.environment.data.model.Step;
import com.ipt.ebsa.environment.v1.build.XMLUpdateBehaviour;

/**
 * Tests for {@link EnvironmentDataFactory}, surprise surprise.
 *
 * @author David Manning
 */
public class TestEnvironmentDataFactory {

	/**
	 * Tests loading of a basic build with nested sequences and steps for an environment.
	 */
	@Test
	public void testLoadEnvironmentData() throws URISyntaxException, IOException {
		EnvironmentDataFactory builder = new EnvironmentDataFactory();
		URI uri = ClassLoader.getSystemResource("TestLoadEnvironmentData_resources").toURI();
		EnvironmentData data = builder.getEnvironmentDataInstance(new File(uri));
		
		assertNotNull(data);
		
		// Check the build
		assertEquals("Incorrect number of builds", 1, data.getBuilds().size());
		Build build = data.getBuildForEnvironmentAndBuildId("hostile", "UniqueIdentifityForThisBuildRef");
		assertEquals("Incorrect number of user parameters", 2, build.getUserParameters().size());
		assertEquals("Incorrect user parameter #1 value", "Input something useful please, guv", build.getUserParameters().get("something useful"));
		assertEquals("Incorrect user parameter #2 value", "Input something better please, guv", build.getUserParameters().get("something better"));
		assertEquals("Incorrect parameter #1 value", "Glenda", build.getParameters().get("build_4"));
		assertEquals("Incorrect parameter #2 value", "Joseph", build.getParameters().get("build_5"));

		assertBuild(data, build);
	}
	
	/**
	 * Tests loading of a basic build with nested sequences and steps for an environment container.
	 */
	@Test
	public void testLoadEnvironmentContainerData() throws URISyntaxException, IOException {
		EnvironmentDataFactory builder = new EnvironmentDataFactory();
		URI uri = ClassLoader.getSystemResource("TestLoadEnvironmentData_resources").toURI();
		EnvironmentData data = builder.getEnvironmentDataInstance(new File(uri));
		
		assertNotNull(data);
		
		// Check the build
		assertEquals("Incorrect number of builds", 1, data.getBuilds().size());
		
		Build build = data.getBuildForEnvironmentContainerAndBuildId("base_container", "S1");
		assertEquals("Incorrect number of user parameters", 2, build.getUserParameters().size());
		assertEquals("Incorrect user parameter #1 value", "Something for a container", build.getUserParameters().get("up_cont_1"));
		assertEquals("Incorrect user parameter #2 value", "Something else", build.getUserParameters().get("up_cont_2"));
		assertEquals("Incorrect parameter #1 value", "Hey", build.getParameters().get("env_1"));
		assertEquals("Incorrect parameter #2 value", "Ya", build.getParameters().get("env_2"));
		assertBuild(data, build);
	}

	private void assertBuild(EnvironmentData data, Build build)	throws IOException {
		assertEquals("Incorrect build name", "prod_like", build.getId());
		assertEquals("Incorrect number of build parameters", 5, build.getParameters().size());
		assertEquals("Incorrect build parameter #1 value", "Rod", build.getParameters().get("build_1"));
		assertEquals("Incorrect build parameter #2 value", "Jane", build.getParameters().get("build_2"));
		assertEquals("Incorrect build parameter #3 value", "Freddy", build.getParameters().get("build_3"));
		
		// And a top-level sequence utilised by the build
		List<ParameterisedNode> sequences = build.getChildren();
		assertEquals("Incorrect number of sequences discovered", 1, sequences.size());
		Sequence sequence = (Sequence) sequences.iterator().next();
		assertEquals("Incorrect sequence id", "S1", sequence.getId());
		assertEquals("Incorrect number of parameters on S1", 3, sequence.getParameters().size());
		assertEquals("Incorrect S1 parameter #1 value", "Zippy", sequence.getParameters().get("sequence_1a"));
		assertEquals("Incorrect S1 parameter #2 value", "George", sequence.getParameters().get("sequence_1b"));
		assertEquals("Incorrect S1 parameter #3 value", "Bungle", sequence.getParameters().get("sequence_1c"));
		
		// And a sub-sequence
		assertEquals("Incorrect number of sub-sequences and steps discovered", 2, sequence.getChildren().size());
		Iterator<ParameterisedNode> iterator = sequence.getChildren().iterator();
		ParameterisedNode step = iterator.next();
		assertEquals("Incorrect number of actions on step S1", 1, step.getChildren().size());
		assertTrue("Incorrect action for S1 step 1", step.getChildren().get(0) instanceof InfraAction);
		
		assertEquals("Incorrect number of parameters of S1 Step", 1, step.getParameters().size());
		assertEquals("Incorrect value for s1 step parameter", "Rosie", step.getParameters().get("step_context_param_1"));
		Sequence subSequence = (Sequence) iterator.next();
		assertEquals("Incorrect sub-sequence id", "S2", subSequence.getId());
		assertEquals("Incorrect number of parameters on S2", 4, subSequence.getParameters().size());
		assertEquals("Incorrect S2 parameter #1 value", "Sooty", subSequence.getParameters().get("sequence_2a"));
		assertEquals("Incorrect S2 parameter #2 value", "Sweep", subSequence.getParameters().get("sequence_2b"));
		assertEquals("Incorrect S2 parameter #3 value", "Sue", subSequence.getParameters().get("sequence_2c"));
		assertEquals("Incorrect S2 context parameter", "duckula", subSequence.getParameters().get("count"));
		
		assertEquals("Incorrect number of steps on sub-sequence S2", 4, subSequence.getChildren().size());
		Step step1 = (Step) subSequence.getChildren().get(0);
		assertEquals("Incorrect number of actions of S2 step 1", 1, step1.getChildren().size());
		assertTrue("Incorrect action for S2 step 1", step1.getChildren().get(0) instanceof InfraAction);
		assertEquals("Incorrect number of parameters on sub sequence S2 step 1", 2, step1.getParameters().size());
		assertEquals("Incorrect S2 Step 1 parameter #1", "Noddy", step1.getParameters().get("step_context_param_2")); 
		assertEquals("Incorrect S2 Step 1 parameter #2", "Big Ears", step1.getParameters().get("step_context_param_3"));
		
		Step step2 = (Step) subSequence.getChildren().get(1);
		assertTrue("Incorrect action for S2 step 1", step2.getChildren().get(0) instanceof SshAction);
		assertEquals("Incorrect machine name on SSH action", "${machine}", ((SshAction)step2.getChildren().get(0)).getMachine());
		assertEquals("Incorrect jump host name on SSH action", "Gloria Estefan", ((SshAction)step2.getChildren().get(0)).getJumpHosts());
		assertEquals("Incorrect sshopts on SSH action", "${sshopts}", ((SshAction)step2.getChildren().get(0)).getSshOptsFile());
		assertEquals("Incorrect command on SSH action", "command x", ((SshAction)step2.getChildren().get(0)).getCommand());
		assertEquals("Incorrect number of parameters on sub sequence S2 step 1", 1, step2.getParameters().size());
		assertEquals("Incorrect S2 Step 2 parameter #1", "Rosie", step2.getParameters().get("step_context_param_4"));
		
		Step step3 = (Step) subSequence.getChildren().get(2);
		InternalHieraAction internalHieraAction = (InternalHieraAction) step3.getChildren().get(0);
		assertTrue("Incorrect action for S2 step 2", internalHieraAction instanceof InternalHieraAction);
		assertEquals("Incorrect hierarepourl InternalHieraAction", "git://testhiera.git", internalHieraAction.getHieraRepoUrl());
		assertEquals("Incorrect routesrepourl on InternalHieraAction", "git://testroutes.git", internalHieraAction.getRoutesRepoUrl());
		assertEquals("Incorrect routespath on InternalHieraAction", "path/to/xls", internalHieraAction.getRoutesPath());
		assertArrayEquals(new String[]{"someother/path", "somepath/going/here"}, internalHieraAction.getScope().toArray(new String[]{}));
		assertArrayEquals(new String[]{"HO_IPT_ZONE1", "HO_IPT_ZONE2", "zone1", "zone2"}, internalHieraAction.getZones().toArray(new String[]{}));
		assertEquals(XMLUpdateBehaviour.OVERWRITE_ALL, internalHieraAction.getUpdateBehaviour());
		
		Step step4 = (Step) subSequence.getChildren().get(3);
		FirewallHieraAction firewallHieraAction = (FirewallHieraAction) step4.getChildren().get(0);
		assertTrue("Incorrect action for S2 step 3", firewallHieraAction instanceof FirewallHieraAction);
		assertEquals("Incorrect hierarepourl FirewallHieraAction", "git://hiera.git", firewallHieraAction.getHieraRepoUrl());
		assertEquals("Incorrect firewallrepourl on FirewallHieraAction", "git://firewall.git", firewallHieraAction.getFirewallRepoUrl());
		assertEquals("Incorrect firewallpath on FirewallHieraAction", "path/to/firewall/xls", firewallHieraAction.getFirewallPath());
		assertArrayEquals(new String[]{"someother/path", "somepath/going/here"}, firewallHieraAction.getScope().toArray(new String[]{}));
		assertArrayEquals(new String[]{"HO_IPT_ZONE1", "HO_IPT_ZONE2", "zoneA", "zoneB"}, firewallHieraAction.getZones().toArray(new String[]{}));
		assertEquals(XMLUpdateBehaviour.ADD_AND_UPDATE_ONLY, firewallHieraAction.getUpdateBehaviour());
		
		// test collecting it all into one XML file
		assertEquals(FileUtils.readFileToString(new File("src/test/resources/TestLoadEnvironmentData_resources-all.xml")).replace("\r", ""), data.getAllXmlAsXml().replace("\r", ""));
	}
	
	/**
	 * Tests loading of global config.
	 */
	@Test
	public void testLoadGlobalConfig() throws URISyntaxException {
		EnvironmentDataFactory builder = new EnvironmentDataFactory();
		URI uri = ClassLoader.getSystemResource("TestLoadGlobalConfig_resources").toURI();
		EnvironmentData data = builder.getEnvironmentDataInstance(new File(uri));
		
		assertNotNull(data);
		assertEquals("Incorrect number of global params", 3, data.getGlobalParameters().size());
		assertEquals("Incorrect global params #1", "Bruce", data.getGlobalParameters().get("global_1"));
	}
	
	
	/**
	 * Tests handling of a typo in an action
	 */
	@Test
	public void testActionTypo() throws URISyntaxException {
		EnvironmentDataFactory builder = new EnvironmentDataFactory();
		URI uri = ClassLoader.getSystemResource("TestActionTypo_resources").toURI();
		EnvironmentData data = builder.getEnvironmentDataInstance(new File(uri));
		
		assertNotNull(data);
		Build build = data.getBuildForEnvironmentAndBuildId("hostile", "UniqueIdentifityForThisBuildRef");
		Sequence s1 = (Sequence) build.getChildren().get(0);
		Step duffStep = null;
		for (ParameterisedNode child : s1.getChildren()) {
			if ("action-which-does-not-exist".equals(((Step)child).getActionId())) {
				duffStep = (Step) child;
				break;
			}
		}
		
		try {
			duffStep.getChildren();
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("action-which-does-not-exist"));
		}
	}
	
	/**
	 * Tests loading of environment data with a duplicate build ref id
	 * @since EBSAD-19298
	 */
	@Test(expected=IllegalStateException.class)
	public void testLoadEnvironmentDataDuplicateBuildRef() throws URISyntaxException, IOException {
		EnvironmentDataFactory builder = new EnvironmentDataFactory();
		URI uri = ClassLoader.getSystemResource("TestLoadEnvironmentDataDuplicateBuildRef_resources").toURI();
		try {
			builder.getEnvironmentDataInstance(new File(uri));
			fail("Expected an IllegalStateException due to a duplicate build ref id");
		} catch (IllegalStateException e) {
			assertTrue(StringUtils.defaultString(e.getMessage()).contains("contains a duplicate buildref id"));
			throw e;
		}
	}
}
