package com.ipt.ebsa.manage.deploy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.EnvironmentUpdate;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeployment;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeploymentEngine;
import com.ipt.ebsa.manage.deploy.data.ETD;
import com.ipt.ebsa.manage.deploy.impl.ApplicationDeploymentEngine;
import com.ipt.ebsa.manage.deploy.impl.JitYumUpdateManager;
import com.ipt.ebsa.manage.deploy.impl.report.ApplicationReport;
import com.ipt.ebsa.manage.transitions.MCOCommand;
import com.ipt.ebsa.manage.transitions.Transition;

public class ChainDeploymentVerification {
	
	private static Logger log = Logger.getLogger(ChainDeploymentVerification.class);

	/**
	 * Does some digging around to do validation
	 * @param dep
	 * @param numTrans Number of transitions expected
	 * @param transitionChangeCounts count of changes in each transition
	 * @param transitionValues what changes in each transition
	 * @param yumCsvFile The contents of the CSV file that is used for 
	 */
	public static void verify(ApplicationDeployment dep, String yumCsvFile, int numTrans, int[] transitionChangeCounts, ETD[]... transitionValues) {
		List<Transition> transitions = dep.getTransitions();
		
		if (numTrans == 0) {
           assertNull(null, transitions);
		   return;
		}
		assertNotNull("Dependency null", dep);
		assertNotNull("Dependency has not transitions", transitions);
		assertEquals("Incorrect number of transitions", numTrans, transitions.size());
		
		// check yum csv
		JitYumUpdateManager yumManager = ((ApplicationDeploymentEngine) dep.getDeploymentEngine()).getYumManager();
		assertEquals("yum deploy csv", yumCsvFile, yumManager.getCsv());
		
		int i=0;
		for (Transition transition : transitions) {
			log.info("Transition: " + ToStringBuilder.reflectionToString(transition));
		    
			List<EnvironmentUpdate> updates = transition.getUpdates();
		    assertNotNull("Updates undefined for transition", updates);
		    
		    List<MCOCommand> commands = transition.getCommands();
		    assertNotNull("Commands undefined for transition", commands);
		    
		    assertEquals("Invalid test data, there is a mismatch between the number of expected transitions and the transitionChangeCounts array", numTrans, transitionChangeCounts.length);
			assertEquals("Incorrect number of updates in transition " + i, transitionChangeCounts[i], updates.size() + commands.size());
			if (transitionValues != null && transitionValues.length>i) {
				ETD[] transitionIDValues = transitionValues[i];
				assertEquals("Number of changes does not match calculated changes", transitionIDValues.length, updates.size() + commands.size());
				
				for (int j = 0; j < transitionIDValues.length; j++) {
					ETD expected = transitionIDValues[j];
					
					if (j < commands.size()) {
						MCOCommand command = commands.get(j);
						
						assertEquals(dep.getId() + " Command (requested value) in transition " + i, expected.requestedValue, command.getCommand());
						assertEquals(dep.getId() + " Hostnames (requested path) in transition " + i, expected.requestedPath, command.getHosts().toString());
					} else if (j - commands.size() < updates.size()) {					
						EnvironmentUpdate performing = updates.get(j - commands.size());
						
						assertEquals(dep.getId() + " Change made in transition "+i, expected.changeMade, performing.changeMade());
						if (performing.getRequestedValue() instanceof Map || performing.getRequestedValue() instanceof List){
							assertEquals(dep.getId() + " Requested value in transition "+i, expected.requestedValue, ""+performing.getRequestedValue());
						} else {
							assertEquals(dep.getId() + " Requested value in transition "+i, expected.requestedValue, performing.getRequestedValue());
						}
						assertEquals(dep.getId() + " Requested path in transition "+i, expected.requestedPath, ((HieraEnvironmentUpdate)performing).getRequestedPath());
						if (expected.existingValue == null) {
							assertNull(dep.getId() + " Existing value in transition "+i, performing.getExistingValue());
						}
						else {
							assertEquals(dep.getId() + " Existing value in transition "+i, expected.existingValue, ""+performing.getExistingValue());	
						}
						
						assertEquals(dep.getId() + " File path in transition "+i, expected.filePath, ApplicationReport.trimPath(dep, ((HieraMachineState)performing.getSource()).getFile().getPath()));	
					}
				}
			}
		    i++;
		}
	}
	
	public static void verify(CompositeReleaseDeployment dep, String yumCsvFile, int numTrans, int[] transitionChangeCounts, ETD[]... transitionValues) {
		List<Transition> transitions = dep.getTransitions();
		
		if (numTrans == 0) {
           assertNull(null, transitions);
		   return;
		}
		assertNotNull("Dependency null", dep);
		assertNotNull("Dependency has not transitions", transitions);
		assertEquals("Incorrect number of transitions", numTrans, transitions.size());
		
		// check yum csv
		JitYumUpdateManager yumManager = ((CompositeReleaseDeploymentEngine) dep.getDeploymentEngine()).getYumManager();
		assertEquals("yum deploy csv", yumCsvFile, yumManager.getCsv());
		
		for (int i = 0; i < transitions.size(); i++) {
			Transition transition = transitions.get(i);
			log.info("Transition: " + ToStringBuilder.reflectionToString(transition));
		    
			List<MCOCommand> commands = transition.getCommands();
		    assertNotNull("Commands undefined for transition", commands);
		    
		    List<EnvironmentUpdate> updates = transition.getUpdates();
		    assertNotNull("Updates undefined for transition", updates);
		    
		    int waitSecs = transition.getWaitSeconds();
		    int waits = waitSecs > 0 ? 1 : 0;
		    
		    boolean stop = transition.isStopAfter();
		    int stops = stop ? 1 : 0;
		    
		    int transitionChanges = commands.size() + updates.size() + waits + stops;
		    
		    assertEquals("Invalid test data, there is a mismatch between the number of expected transitions and the transitionChangeCounts array", numTrans, transitionChangeCounts.length);
			assertEquals("Incorrect number of updates in transition " + i, transitionChangeCounts[i], transitionChanges);
			if (transitionValues != null && transitionValues.length>i) {
				ETD[] transitionIDValues = transitionValues[i];
				assertEquals("Number of changes does not match calculated changes for transition index [" + i + "]", transitionIDValues.length, transitionChanges);
				
				for (int j = 0; j < transitionIDValues.length; j++) {
					ETD expected = transitionIDValues[j];
					
					if (j < commands.size()) {
						// Any commands come first
						MCOCommand command = commands.get(j);
						
						assertEquals(dep.getId() + " Command (requested value) in transition " + i, expected.requestedValue, command.getCommand());
						assertEquals(dep.getId() + " Hostnames (requested path) in transition " + i, expected.requestedPath, command.getHosts().toString());
					} else if (j - commands.size() < updates.size()) {					
						// Any updates come second
						EnvironmentUpdate performing = updates.get(j - commands.size());
						
						assertEquals(dep.getId() + " Change made in transition "+i, expected.changeMade, performing.changeMade());
						if (performing.getRequestedValue() instanceof Map || performing.getRequestedValue() instanceof List){
							assertEquals(dep.getId() + " Requested value in transition "+i, expected.requestedValue, ""+performing.getRequestedValue().toString());
						} else {
							assertEquals(dep.getId() + " Requested value in transition "+i, expected.requestedValue, performing.getRequestedValue() != null ? performing.getRequestedValue().toString() : null);
						}
						HieraEnvironmentUpdate performingHiera = (HieraEnvironmentUpdate)performing;
						assertEquals(dep.getId() + " Requested path in transition "+i, expected.requestedPath, performingHiera.getRequestedValue() != null ? performingHiera.getRequestedPath().toString() : null);
						if (expected.existingValue == null) {
							assertNull(dep.getId() + " Existing value in transition "+i, performing.getExistingValue());
						} else {
							assertEquals(dep.getId() + " Existing value in transition "+i, expected.existingValue, ""+performing.getExistingValue());	
						}
						
						if (expected.existingPath != null) {
							assertEquals(dep.getId() + " Existing path in transition "+i, expected.existingPath, ((HieraEnvironmentUpdate) performing).getExistingPath());
						}
						
						assertEquals(dep.getId() + " File path in transition "+i, expected.filePath, ApplicationReport.trimPath(dep, ((HieraMachineState)performing.getSource()).getFile().getPath()));	
					} else if (j - commands.size() - updates.size() < waits) {
						assertEquals(dep.getId() + " Wait expected in transition "+i, expected.waitDuration, waitSecs);
					} else if (j - commands.size() - updates.size() - waits < stops) {
						assertEquals(dep.getId() + " Stop expected in transition "+i, expected.stop, stop);
						assertEquals(dep.getId() + " Stop message expected in transition "+i, expected.stopMessage, transition.getStopMessage());
					}
				}
			}
		}
	}
}
