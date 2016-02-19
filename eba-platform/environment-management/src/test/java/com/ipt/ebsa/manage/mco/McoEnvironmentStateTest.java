package com.ipt.ebsa.manage.mco;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.manage.puppet.EMPuppetManager;
import com.ipt.ebsa.ssh.ExecReturn;

public class McoEnvironmentStateTest {	
	
	@Test
	public void testEnvironmentLoad() throws IOException {
		String envName = "st-dev1-ebs1";
		String logFileName = envName + ".ipt.local" + ".txt";
			
		MCOEnvironmentStateManager esm = getTestEnvironmentStateManager(logFileName, envName);
		
		//Test fetching state for tstdzm01
		String serverName = "tstdzm01." + envName + ".ipt.local";
		MCOMachineState state = esm.getEnvironmentState(envName, serverName);
		assertNotNull("State should not be null", state);
		assertEquals("Server name incorrect", serverName, state.getRoleOrFQDN());
		assertEquals("Number of packages incorrect", 371, state.getState().size());
		assertEquals("Environment name incorrect", envName, state.getEnvironmentName());
		
		//Test fetching state for ssmtzm02
		serverName = "ssmtzm02." + envName + ".ipt.local";
		state = esm.getEnvironmentState(envName, serverName);
		assertNotNull("State should not be null", state);
		assertEquals("Server name incorrect", serverName, state.getRoleOrFQDN());
		assertEquals("Number of packages incorrect", 353, state.getState().size());
		assertEquals("Environment name incorrect", envName, state.getEnvironmentName());
		
		serverName = "rmatzm01." + envName + ".ipt.local";
		state = esm.getEnvironmentState(envName, serverName);
		assertNotNull("State should not be null", state);
		assertEquals("Server name incorrect", serverName, state.getRoleOrFQDN());
		assertEquals("Number of packages incorrect", 0, state.getState().size());
		
		//Test searching the environment for components
		List<StateSearchResult> result = esm.findComponent("postgresql92", envName);
		assertEquals("Incorrect number of component search results", 1, result.size());
		
		result = esm.findComponent("nmon", envName);
		assertEquals("Incorrect number of component search results", 9, result.size());
	}
	
	/**
	 * Loads a given log into a String.
	 * @param logFileName
	 * @return
	 * @throws IOException
	 */
	private String loadTestLog(String logFileName) throws IOException{
		String logsBaseFolder = "src/test/resources/mco/";
		byte[] fileContents = Files.readAllBytes(Paths.get(logsBaseFolder, logFileName));
		return new String(fileContents);
	}
	
	/**
	 * Sets up an MCOEnvironmentStateManager, with data read from a local log file
	 * @param logFileName
	 * @param env
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private MCOEnvironmentStateManager getTestEnvironmentStateManager(String logFileName, String zoneName) throws IOException {
		String logToTest = loadTestLog(logFileName);
		ExecReturn ret = new ExecReturn(0);
		ret.setStdOut(logToTest);
		
		// Mockito the PuppetManager and always return exit code 0 from updatePuppetMaster
		EMPuppetManager puppet = mock(EMPuppetManager.class);
			when(puppet.doMCollectiveOperationWithOutput(Matchers.any(Organisation.class), anySetOf(String.class), anyString(), anyBoolean(), anyInt()))
			.thenReturn(ret);
			
		HashSet<String> zone = new HashSet<String>();
		zone.add(zoneName);
		MCOEnvironmentStateManager esm = new MCOEnvironmentStateManager(puppet);
		esm.load(null, new Organisation(zoneName), zone, (Map<String, Collection<ResolvedHost>>)null);
		
		ArgumentCaptor<Organisation> orgCaptor = ArgumentCaptor.forClass(Organisation.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<Set<String>> zonesCaptor = ArgumentCaptor.forClass((Class)Set.class);
		ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Boolean> escapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		ArgumentCaptor<Integer> timeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		
		verify(puppet, times(1)).doMCollectiveOperationWithOutput(orgCaptor.capture(), zonesCaptor.capture(), commandCaptor.capture(), escapeCaptor.capture(), timeoutCaptor.capture());
		
		assertEquals("Unexpected command run", "runscript run_script scriptToRun=/usr/local/bin/list_installed_rpms.sh -j", commandCaptor.getValue());
		
		Set<String> zoneSet = new HashSet<>();
		zoneSet.add(zoneName);
		
		assertEquals("Unexpected zone set", zoneSet, zonesCaptor.getValue());
		
		return esm;
	}
}
