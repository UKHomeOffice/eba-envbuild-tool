package com.ipt.ebsa.manage.deploy.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.ipt.ebsa.buildtools.release.entities.ReleaseVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.deployment.descriptor.release.XMLPhaseType;
import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.hiera.NodeMissingBehaviour;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeployment;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleasePhaseBuilderTest;
import com.ipt.ebsa.manage.deploy.impl.TransitionManager;
import com.ipt.ebsa.manage.environment.EnvironmentStateManager;
import com.ipt.ebsa.manage.transitions.MCOCommand;
import com.ipt.ebsa.manage.transitions.Transition;

public class TransitionManagerTest {
	
	@After
	public void afterEveryTest() {
		// Reset max wait to default
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_MAX_WAIT_SECS);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void buildPhasesWaitTooLong() throws Exception {
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARE_ONLY, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX, "st");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_COMPOSITE_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.GIT_LOCAL_CHECKOUT_DIR, "src/test/resources/ss3");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER, "src/test/resources/ss3");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_CREATE_HIERA, "false");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER, "target/reports/ss3/sits");
		ConfigurationFactory.getProperties().put("st.rpm.failfile.report.enabled", "false");
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_MAX_WAIT_SECS, "5"); // Set max wait to override default value for this test
		
		ReleaseVersion releaseVersion = CompositeReleasePhaseBuilderTest.buildReleaseFromAppShortNames();
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(releaseVersion, "", "SITS.xml");
		TransitionManager transitionManager = new TransitionManager();
		
		for (XMLPhaseType phase : deployment.getReleaseDeploymentDescriptor().getPhase()) {
			transitionManager.createPhasePreTransitions(phase.getBefore(), deployment);
			transitionManager.createPhasePostTransitions(phase.getAfter(), phase.getStop(), deployment);
		}
	}
	
	/**
	 * Tests the building of phases with before and after transitions that include inject/remove/command/wait/stop
	 * 
	 * @throws Exception
	 */
	@Test
	public void buildPhasesBeforesAndAfters() throws Exception {
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARE_ONLY, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX, "st");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_COMPOSITE_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.GIT_LOCAL_CHECKOUT_DIR, "src/test/resources/ss3");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER, "src/test/resources/ss3");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_CREATE_HIERA, "false");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER, "target/reports/ss3/ssb");
		ConfigurationFactory.getProperties().put("st.rpm.failfile.report.enabled", "false");
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR);
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_MAX_WAIT_SECS);
		
		ReleaseVersion releaseVersion = CompositeReleasePhaseBuilderTest.buildReleaseFromAppShortNames("SSB", "SSBSIM");
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(releaseVersion, "", "SSB inject remove command wait stop.xml");
		
		TransitionManager transitionManager = new TransitionManager();
		
		// Mock the environment state manager to always return a valid YAML file
		EnvironmentStateManager hieraData = mock(EnvironmentStateManager.class);
		when(hieraData.getEnvironmentState(anyString(), anyString())).thenAnswer(new Answer<MachineState>() {

			@Override
			public MachineState answer(InvocationOnMock i) throws Throwable {
				String zone = i.getArgumentAt(0, String.class);
				String hostOrRole = i.getArgumentAt(1, String.class);
				return new HieraMachineState(zone, hostOrRole, new File(hostOrRole + ".yaml"), new HashMap<String, Object>());
			}
			
		});
				
		deployment.setHieraData(hieraData);
		
		// PHASE 1
		Iterator<XMLPhaseType> iterator = deployment.getReleaseDeploymentDescriptor().getPhase().iterator();
		XMLPhaseType phase = iterator.next();
		
		// BEFORE TRANSITION 1
		List<Transition> beforeTransitions = transitionManager.createPhasePreTransitions(phase.getBefore(), deployment);
		assertEquals(2, beforeTransitions.size());
		
		Transition t = beforeTransitions.get(0);
		assertEquals(0, t.getCommands().size());
		assertEquals(2, t.getUpdates().size());
		
		HieraEnvironmentUpdate update = (HieraEnvironmentUpdate) t.getUpdates().get(0);
		assertEquals("soatzm01", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services/weblogic-cdp/ensure", update.getRequestedPath());
		assertEquals("stopped", update.getRequestedValue());
		assertEquals(NodeMissingBehaviour.INSERT_ALL, update.getNodeMissingBehaviour());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(1);
		assertEquals("soatzm02", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services/weblogic-cdp/ensure", update.getRequestedPath());
		assertEquals("stopped", update.getRequestedValue());
		assertEquals(NodeMissingBehaviour.INSERT_ALL, update.getNodeMissingBehaviour());
		
		assertFalse(t.isStopAfter());
		assertNull(t.getStopMessage());
		assertEquals(-1, t.getWaitSeconds());
		
		// BEFORE TRANSITION 2
		t = beforeTransitions.get(1);
		assertEquals(0, t.getCommands().size());
		assertEquals(0, t.getUpdates().size());
		assertFalse(t.isStopAfter());
		assertNull(t.getStopMessage());
		assertEquals(600, t.getWaitSeconds());
		
		// AFTER TRANSITION 1
		List<Transition> afterTransitions = transitionManager.createPhasePostTransitions(phase.getAfter(), phase.getStop(), deployment);
		assertEquals(2, afterTransitions.size());
		
		t = afterTransitions.get(0);
		assertEquals(0, t.getCommands().size());
		assertEquals(4, t.getUpdates().size());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(0);
		assertEquals("soatzm01", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services/weblogic-cdp/ensure", update.getRequestedPath());
		assertEquals("running", update.getRequestedValue());
		assertEquals(NodeMissingBehaviour.INSERT_ALL, update.getNodeMissingBehaviour());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(1);
		assertEquals("soatzm02", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services/weblogic-cdp/ensure", update.getRequestedPath());
		assertEquals("running", update.getRequestedValue());
		assertEquals(NodeMissingBehaviour.INSERT_ALL, update.getNodeMissingBehaviour());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(2);
		assertEquals("soatzm01", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services/weblogic-cdp/enable", update.getRequestedPath());
		assertEquals(true, update.getRequestedValue());
		assertEquals(NodeMissingBehaviour.INSERT_ALL, update.getNodeMissingBehaviour());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(3);
		assertEquals("soatzm02", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services/weblogic-cdp/enable", update.getRequestedPath());
		assertEquals(true, update.getRequestedValue());
		assertEquals(NodeMissingBehaviour.INSERT_ALL, update.getNodeMissingBehaviour());
		
		assertFalse(t.isStopAfter());
		assertNull(t.getStopMessage());
		assertEquals(-1, t.getWaitSeconds());
		
		// AFTER TRANSITION 2 
		t = afterTransitions.get(1);
		assertEquals(0, t.getCommands().size());
		assertEquals(0, t.getUpdates().size());
		assertTrue(t.isStopAfter());
		assertEquals("Stop before SIMS", t.getStopMessage());
		assertEquals(-1, t.getWaitSeconds());
		
		// PHASE 2
		phase = iterator.next();
		
		// BEFORE TRANSITION 1
		beforeTransitions = transitionManager.createPhasePreTransitions(phase.getBefore(), deployment);
		assertEquals(2, beforeTransitions.size());
		
		t = beforeTransitions.get(0);
		assertEquals(1, t.getCommands().size());
		MCOCommand command = t.getCommands().get(0);
		assertEquals("command-to-be-executed", command.getCommand());
		assertEquals(2, command.getHosts().size());
		Iterator<ResolvedHost> hosts = command.getHosts().iterator();
		ResolvedHost host = hosts.next();
		assertEquals("dbs", host.getHostOrRole());
		assertEquals("IPT_ST_SIT1_COR1", host.getZone());
		host = hosts.next();
		assertEquals("soatzm01", host.getHostOrRole());
		assertEquals("IPT_ST_SIT1_COR1", host.getZone());
		assertEquals(0, t.getUpdates().size());
		assertFalse(t.isStopAfter());
		assertNull(t.getStopMessage());
		assertEquals(-1, t.getWaitSeconds());
		
		// BEFORE TRANSITION 2
		t = beforeTransitions.get(1);
		assertEquals(0, t.getCommands().size());
		assertEquals(2, t.getUpdates().size());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(0);
		assertEquals("soatzm01", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services", update.getPathElementsRemoved());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(1);
		assertEquals("soatzm02", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services", update.getPathElementsRemoved());
		
		assertFalse(t.isStopAfter());
		assertNull(t.getStopMessage());
		assertEquals(-1, t.getWaitSeconds());
		
		// AFTER TRANSITION 1
		afterTransitions = transitionManager.createPhasePostTransitions(phase.getAfter(), phase.getStop(), deployment);
		assertEquals(2, afterTransitions.size());
		
		t = afterTransitions.get(0);
		assertEquals(0, t.getCommands().size());
		assertEquals(2, t.getUpdates().size());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(0);
		assertEquals("soatzm01", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services", update.getPathElementsRemoved());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(1);
		assertEquals("soatzm02", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services", update.getPathElementsRemoved());
		
		assertFalse(t.isStopAfter());
		assertNull(t.getStopMessage());
		assertEquals(-1, t.getWaitSeconds());
		
		// AFTER TRANSITION 2
		t = afterTransitions.get(1);
		assertEquals(0, t.getCommands().size());
		assertEquals(2, t.getUpdates().size());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(0);
		assertEquals("soatzm01", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services", update.getPathElementsRemoved());
		
		update = (HieraEnvironmentUpdate) t.getUpdates().get(1);
		assertEquals("soatzm02", update.getSource().getRoleOrFQDN());
		assertEquals("IPT_ST_SIT1_COR1", update.getSource().getEnvironmentName());
		assertEquals("system::services", update.getPathElementsRemoved());
		
		assertFalse(t.isStopAfter());
		assertNull(t.getStopMessage());
		assertEquals(-1, t.getWaitSeconds());
	}


}
