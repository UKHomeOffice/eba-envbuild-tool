package com.ipt.ebsa.manage.deploy.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.h2.store.fs.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.yaml.snakeyaml.Yaml;

import com.ipt.ebsa.buildtools.release.entities.Application;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Component;
import com.ipt.ebsa.deployment.descriptor.ObjectFactory;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.deployment.descriptor.XMLComponentType;
import com.ipt.ebsa.deployment.descriptor.XMLDeploymentDescriptorType;
import com.ipt.ebsa.environment.EnvironmentUpdate;
import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.hiera.HieraData;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.HieraStateSearchResult;
import com.ipt.ebsa.hiera.NodeMissingBehaviour;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.ApplicationDeployment;
import com.ipt.ebsa.manage.deploy.database.DBTest;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.hiera.HieraEnvironmentStateManager;
import com.ipt.ebsa.manage.puppet.EMPuppetManager;
import com.ipt.ebsa.manage.puppet.SshManager;
import com.ipt.ebsa.manage.transitions.MCOCommand;
import com.ipt.ebsa.manage.transitions.Transition;
import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.ssh.HostnameUsernamePort;

public class ApplicationDeploymentEngineTest extends DBTest {

	private static final String APP_SHORT_NAME = "APP";
	private static final String LS = System.getProperty("line.separator");
	private static final Logger LOG = Logger.getLogger(ApplicationDeploymentEngineTest.class);
	
	@Test
	public void testCollateHieraUpdates() {
		final String zone = "IPT_ST_CIT1_COR1";
		
		List<EnvironmentUpdate> uncollated = new ArrayList<>();
		
		HieraEnvironmentUpdate update = new HieraEnvironmentUpdate(APP_SHORT_NAME, zone);
		update.setSource(new HieraMachineState("", "", new File("soa.yaml"), new TreeMap<String, Object>()));
		uncollated.add(update);
		
		update = new HieraEnvironmentUpdate(APP_SHORT_NAME, zone);
		update.setSource(new HieraMachineState("", "", new File("soa.yaml"), new TreeMap<String, Object>()));
		uncollated.add(update);
		
		update = new HieraEnvironmentUpdate(APP_SHORT_NAME, zone);
		update.setSource(new HieraMachineState("", "", new File("dbs.yaml"), new TreeMap<String, Object>()));
		uncollated.add(update);
		
		update = new HieraEnvironmentUpdate(APP_SHORT_NAME, zone);
		update.setSource(new HieraMachineState("", "", new File("soa.yaml"), new TreeMap<String, Object>()));
		uncollated.add(update);
		
		update = new HieraEnvironmentUpdate(APP_SHORT_NAME, zone);
		update.setSource(new HieraMachineState("", "", new File("etl.yaml"), new TreeMap<String, Object>()));
		uncollated.add(update);
		
		update = new HieraEnvironmentUpdate(APP_SHORT_NAME, zone);
		update.setSource(new HieraMachineState("", "", new File("soatzm01.st-dev1-ebs1.ipt.local.yaml"), new TreeMap<String, Object>()));
		uncollated.add(update);
		
		update = new HieraEnvironmentUpdate(APP_SHORT_NAME, zone);
		update.setSource(new HieraMachineState("", "", new File("dbs.yaml"), new TreeMap<String, Object>()));
		uncollated.add(update);
		
		Map<String, List<EnvironmentUpdate>> collated = new ApplicationDeploymentEngine().collateUpdates(uncollated);
		assertEquals(4, collated.size());	
		
		Iterator<String> files = collated.keySet().iterator();
		
		String file = files.next();
		assertEquals("dbs.yaml", file);
		assertEquals(2, collated.get(file).size());
		assertEquals(uncollated.get(2), collated.get(file).get(0));
		assertEquals(uncollated.get(6), collated.get(file).get(1));
		
		file = files.next();
		assertEquals("etl.yaml", file);
		assertEquals(1, collated.get(file).size());
		assertEquals(uncollated.get(4), collated.get(file).get(0));
		
		file = files.next();
		assertEquals("soa.yaml", file);
		assertEquals(3, collated.get(file).size());
		assertEquals(uncollated.get(0), collated.get(file).get(0));
		assertEquals(uncollated.get(1), collated.get(file).get(1));
		assertEquals(uncollated.get(3), collated.get(file).get(2));
		
		file = files.next();
		assertEquals("soatzm01.st-dev1-ebs1.ipt.local.yaml", file);
		assertEquals(1, collated.get(file).size());
		assertEquals(uncollated.get(5), collated.get(file).get(0));
	}
	
	/**
	 * This test case undeploys 7 components across 4 transitions (3, 2, 1, 1) and a single YAML file
	 * @throws Exception 
	 * @throws GitAPIException 
	 * @throws NoFilepatternException 
	 */
	@Test
	public void testExecuteTransitions() throws NoFilepatternException, GitAPIException, Exception {		
		final String zone = "IPT_ST_SST1_SSB1";
		
		// Build the temp YAML file
		File file = new File(FileUtils.createTempFile("ssb-", "yaml", true, true));
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.write("system::packages:");
			writer.newLine();
			writer.write("    ssb-core-features-fuse-application:");
			writer.newLine();
			writer.write("        ensure: 2.1.119-1");
			writer.newLine();
			writer.write("    ssb-core-features-fuse-config:");
			writer.newLine();
			writer.write("        ensure: 2.1.119-1");
			writer.newLine();
			writer.write("    ssb-rpm-fuse-config:");
			writer.newLine();
			writer.write("        ensure: 2.0.294-1");
			writer.newLine();
			writer.write("    ssb-core-features-lib-nexus:");
			writer.newLine();
			writer.write("        ensure: 2.1.41-release_2.1.41_1");
			writer.newLine();
			writer.write("    ssb-rpm-nexus-baseline-config:");
			writer.newLine();
			writer.write("        ensure: 2.0.3-1");
			writer.newLine();
			writer.write("    ssb-ldap-schema:");
			writer.newLine();
			writer.write("        ensure: 1.143-1");
			writer.newLine();
			writer.write("    ssb-db-schema:");
			writer.newLine();
			writer.write("        ensure: 1.376.293-1");
			writer.newLine();
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
		
		List<Transition> transitions = new ArrayList<>();
		
		Transition t = new Transition();
		t.setSequenceNumber(0);		
		t.getUpdates().add(buildMockYamlUpdate("2.1.119-1", "system::packages/ssb-core-features-fuse-application/ensure", file, zone));		
		t.getUpdates().add(buildMockYamlUpdate("2.1.119-1", "system::packages/ssb-core-features-fuse-config/ensure", file, zone));		
		t.getUpdates().add(buildMockYamlUpdate("2.0.294-1", "system::packages/ssb-rpm-fuse-config/ensure", file, zone));
		transitions.add(t);
		
		t = new Transition();
		t.setSequenceNumber(1);
		t.getUpdates().add(buildMockYamlUpdate("2.1.41-release_2.1.41_1", "system::packages/ssb-core-features-lib-nexus/ensure", file, zone));
		t.getUpdates().add(buildMockYamlUpdate("2.0.3-1", "system::packages/ssb-rpm-nexus-baseline-config/ensure", file, zone));
		transitions.add(t);
		
		t = new Transition();
		t.setSequenceNumber(2);
		t.getUpdates().add(buildMockYamlUpdate("1.143-1", "system::packages/ssb-ldap-schema/ensure", file, zone));
		transitions.add(t);
			
		t = new Transition();
		t.setSequenceNumber(3);
		t.getUpdates().add(buildMockYamlUpdate("1.376.293-1", "system::packages/ssb-db-schema/ensure", file, zone));
		transitions.add(t);
		
		ApplicationVersion applicationVersion = new ApplicationVersion();
		Application application = new Application();
		application.setShortName("APP");
		applicationVersion.setApplication(application);
		applicationVersion.setName("TEST_APP_VERSION");
		applicationVersion.setVersion("1.0");
		
		// Build deployment object with the transitions and YAML updates
		ApplicationDeployment deployment = new ApplicationDeployment(applicationVersion);
		deployment.setTransitions(transitions);				
		ConfigurationFactory.getProperties().setProperty(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX, "st");
		
		// Mockito the PuppetManager and always return exit code 0 from updatePuppetMaster
		EMPuppetManager puppet = mock(EMPuppetManager.class);
		when(puppet.doPuppetRunWithRetry(Matchers.any(Organisation.class), anyString(), anyString(), anyInt(), anyInt()))
			.thenReturn(new ExecReturn(0));
		
		// Mockito the GITManager - the method we are mocking commitBranchMergeToMaster() is void so no need to specify here
		EMGitManager git = mock(EMGitManager.class);
		deployment.setGitManager(git);
		
		// Execute the transitions
		ApplicationDeploymentEngine defaultDeploymentEngine = new ApplicationDeploymentEngine();
		defaultDeploymentEngine.setDeployment(deployment);
		defaultDeploymentEngine.executeTransitions(deployment, puppet);
		
		// Assert that the Puppet method was called 4 times (once for each transition) with the environment name as the arg 
		ArgumentCaptor<Organisation> puppetUpdateArgCaptor = ArgumentCaptor.forClass(Organisation.class);
		verify(puppet, times(4)).updatePuppetMaster(puppetUpdateArgCaptor.capture());
		for (int i = 0; i < 4; i++) {
			Organisation puppetArg  = puppetUpdateArgCaptor.getAllValues().get(i);
			LOG.info(puppetArg);
			assertTrue(puppetArg.equals(ConfigurationFactory.getOrganisations().get("st")));
		}
		
		// Assert that the Git method was called 4 times (once for each transition) with a commit string starting with 'Transition [0|1|2|3]'
		ArgumentCaptor<String> commitBranchArgCaptor = ArgumentCaptor.forClass(String.class);
		verify(git, times(4)).commitBranchMergeToMaster(commitBranchArgCaptor.capture());	
		for (int i = 0; i < 4; i++) {
			String gitArg = commitBranchArgCaptor.getAllValues().get(i);
			LOG.info(gitArg);
			assertTrue(gitArg.startsWith("Transition " + i));
		}
		
		// Assert that the YAML file has been updated so that 7 components are now absent
		StringBuilder fileContent = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				LOG.info(line);
				fileContent.append(line);
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		assertEquals(7, StringUtils.countMatches(fileContent.toString(), "ensure: absent"));
	}
	
	/**
	 * This test case undeploys 7 components across 6 transitions with some commands thrown in for good measure
	 * @throws Exception 
	 * @throws GitAPIException 
	 * @throws NoFilepatternException 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testExecuteTransitionsWithCommands() throws NoFilepatternException, GitAPIException, Exception {		
		final String zone = "IPT_ST_SST1_SSB1";
		
		// Set some props before we start
		// Set Puppet Master port to empty so the default is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_PORT, "");
		// Set username otherwise 'localhost' is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_USERNAME, "peadmin");
		String puppetHost = "puppetmaster.host";
		ConfigurationFactory.getProperties().put("st.puppet.master.host", puppetHost);
		ConfigurationFactory.getProperties().put(Configuration.ENABLE_MCO, "true");
		
		// Build the temp YAML file
		File file = new File(FileUtils.createTempFile("ssb", "yaml", true, true));
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.write("system::packages:");
			writer.newLine();
			writer.write("    ssb-core-features-fuse-application:");
			writer.newLine();
			writer.write("        ensure: 2.1.119-1");
			writer.newLine();
			writer.write("    ssb-core-features-fuse-config:");
			writer.newLine();
			writer.write("        ensure: 2.1.119-1");
			writer.newLine();
			writer.write("    ssb-rpm-fuse-config:");
			writer.newLine();
			writer.write("        ensure: 2.0.294-1");
			writer.newLine();
			writer.write("    ssb-core-features-lib-nexus:");
			writer.newLine();
			writer.write("        ensure: 2.1.41-release_2.1.41_1");
			writer.newLine();
			writer.write("    ssb-rpm-nexus-baseline-config:");
			writer.newLine();
			writer.write("        ensure: 2.0.3-1");
			writer.newLine();
			writer.write("    ssb-ldap-schema:");
			writer.newLine();
			writer.write("        ensure: 1.143-1");
			writer.newLine();
			writer.write("    ssb-db-schema:");
			writer.newLine();
			writer.write("        ensure: 1.376.293-1");
			writer.newLine();
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
		
		List<Transition> transitions = new ArrayList<>();
		
		Transition t = new Transition();
		t.setSequenceNumber(0);		
		t.getCommands().add(buildMockCommand("prePlanCommand", Arrays.asList(new ResolvedHost("soatzm01", zone), new ResolvedHost("soatzm02", zone))));
		transitions.add(t);
		
		t = new Transition();
		t.setSequenceNumber(1);		
		t.getUpdates().add(buildMockYamlUpdate("2.1.119-1", "system::packages/ssb-core-features-fuse-application/ensure", file, zone));		
		t.getUpdates().add(buildMockYamlUpdate("2.1.119-1", "system::packages/ssb-core-features-fuse-config/ensure", file, zone));		
		t.getUpdates().add(buildMockYamlUpdate("2.0.294-1", "system::packages/ssb-rpm-fuse-config/ensure", file, zone));
		t.getCommands().add(buildMockCommand("transitionCommand1", Arrays.asList(new ResolvedHost("soatzm02", zone))));
		t.getCommands().add(buildMockCommand("transitionCommand2", Arrays.asList(new ResolvedHost("soatzm01", zone))));
		transitions.add(t);
		
		t = new Transition();
		t.setSequenceNumber(2);
		t.getCommands().add(buildMockCommand("transitionCommand3", Arrays.asList(new ResolvedHost("soa", zone))));
		t.getUpdates().add(buildMockYamlUpdate("2.1.41-release_2.1.41_1", "system::packages/ssb-core-features-lib-nexus/ensure", file, zone));
		t.getUpdates().add(buildMockYamlUpdate("2.0.3-1", "system::packages/ssb-rpm-nexus-baseline-config/ensure", file, zone));
		transitions.add(t);
		
		t = new Transition();
		t.setSequenceNumber(3);		
		t.getCommands().add(buildMockCommand("transitionCommand4", Arrays.asList(new ResolvedHost("soatzm01", zone), 
				new ResolvedHost("soatzm02", zone), new ResolvedHost("soatzm03", zone))));
		transitions.add(t);
		
		t = new Transition();
		t.setSequenceNumber(4);
		t.getUpdates().add(buildMockYamlUpdate("1.143-1", "system::packages/ssb-ldap-schema/ensure", file, zone));
		transitions.add(t);
			
		t = new Transition();
		t.setSequenceNumber(5);
		t.getUpdates().add(buildMockYamlUpdate("1.376.293-1", "system::packages/ssb-db-schema/ensure", file, zone));
		transitions.add(t);
		
		t = new Transition();
		t.setSequenceNumber(6);		
		t.getCommands().add(buildMockCommand("postPlanCommand", Arrays.asList(new ResolvedHost("soa", zone))));
		transitions.add(t);
		
		ApplicationVersion applicationVersion = new ApplicationVersion();
		Application application = new Application();
		application.setShortName("APP");
		applicationVersion.setApplication(application);
		applicationVersion.setName("TEST_APP_VERSION");
		applicationVersion.setVersion("1.0");
		
		// Build deployment object with the transitions, YAML updates & commands
//		ApplicationDeployment deployment = new ApplicationDeployment(applicationVersion, "IPT_SST1_SSB1");
		ApplicationDeployment deployment = new ApplicationDeployment(applicationVersion);
		deployment.setTransitions(transitions);
		
		// Mockito the JschManager and always return exit code 0 from runSSHExec
		SshManager ssh = mock(SshManager.class);
		when(ssh.runSSHExec(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class))).thenReturn(0);
		when(ssh.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean())).thenReturn(new ExecReturn(0));
				
		// Mockito the GITManager - the method we are mocking commitBranchMergeToMaster() is void so no need to specify here
		EMGitManager git = mock(EMGitManager.class);
		deployment.setGitManager(git);
		
		// Execute the transitions
		ApplicationDeploymentEngine defaultDeploymentEngine = new ApplicationDeploymentEngine();
		defaultDeploymentEngine.setDeployment(deployment);
		defaultDeploymentEngine.executeTransitions(deployment, new EMPuppetManager(ssh));
		
		// Assert that the Puppet method was called 4 times (once for each transition) with the environment name as the arg 
		ArgumentCaptor<Integer> jschTimeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> jschCommandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschUsernameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschHostCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> jschPortCaptor = ArgumentCaptor.forClass(Integer.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> jschJumpHostsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<String> commitBranchArgCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Boolean> jschUnescapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		
		InOrder inOrder = inOrder(ssh, git);
		// Assert that Jsch manager and git were called in the expected order.		
		inOrder.verify(ssh, times(3)).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		inOrder.verify(git, times(1)).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh, times(1)).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		
		inOrder.verify(ssh, times(2)).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		inOrder.verify(git, times(1)).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh, times(1)).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		
		inOrder.verify(ssh, times(2)).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		inOrder.verify(git, times(1)).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh, times(1)).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		
		inOrder.verify(ssh, times(1)).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		inOrder.verify(git, times(1)).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh, times(1)).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		
		inOrder.verify(ssh, times(2)).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		String[] commands = {
				// T1 - JschManager.runSSHExecWithOutput
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 prePlanCommand  -S \"(domain=st-sst1-ssb1.ipt.local and (fqdn=soatzm01.st-sst1-ssb1.ipt.local or fqdn=soatzm02.st-sst1-ssb1.ipt.local))\"",
				// T2 - JschManager.runSSHExecWithOutput x2
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 transitionCommand1  -S \"(domain=st-sst1-ssb1.ipt.local and (fqdn=soatzm02.st-sst1-ssb1.ipt.local))\"",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 transitionCommand2  -S \"(domain=st-sst1-ssb1.ipt.local and (fqdn=soatzm01.st-sst1-ssb1.ipt.local))\"",
				// T2 - JschManager.runSSHExec
				"./syncPuppetConfig.strategic.sh",
				// T2 - JschManager.runSSHExecWithOutput
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sst1-ssb1.ipt.local and (role=ssb))\"",
				// T3 - JschManager.runSSHExecWithOutput
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 transitionCommand3  -S \"(domain=st-sst1-ssb1.ipt.local and (role=soa))\"",
				// T3 - JschManager.runSSHExec
				"./syncPuppetConfig.strategic.sh",
				// T3 - JschManager.runSSHExecWithOutput
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sst1-ssb1.ipt.local and (role=ssb))\"",
				// T4 - JschManager.runSSHExecWithOutput
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 transitionCommand4  -S \"(domain=st-sst1-ssb1.ipt.local and (fqdn=soatzm01.st-sst1-ssb1.ipt.local or fqdn=soatzm02.st-sst1-ssb1.ipt.local or fqdn=soatzm03.st-sst1-ssb1.ipt.local))\"",
				// T4 - JschManager.runSSHExec
				"./syncPuppetConfig.strategic.sh",
				// T4 - JschManager.runSSHExecWithOutput
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sst1-ssb1.ipt.local and (role=ssb))\"",
				// T4 - JschManager.runSSHExec
				"./syncPuppetConfig.strategic.sh",
				// T4 - JschManager.runSSHExecWithOutput x2
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sst1-ssb1.ipt.local and (role=ssb))\"",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 postPlanCommand  -S \"(domain=st-sst1-ssb1.ipt.local and (role=soa))\""
				};
		// Assert that the Jsch manager was called 14 times in total		
		for (int i = 0; i < 14; i++) {
			String command = jschCommandCaptor.getAllValues().get(i);
			LOG.info(command);
			assertEquals(commands[i], command);
			
			int timeout = jschTimeoutCaptor.getAllValues().get(i);
			LOG.info(timeout);
			assertEquals(3000000, timeout);
			
			String host = jschHostCaptor.getAllValues().get(i);
			LOG.info(host);
			assertEquals(puppetHost, host);
			
			int port = jschPortCaptor.getAllValues().get(i);
			LOG.info(port);
			assertEquals(22, port);
			
			List<HostnameUsernamePort> jumpHosts = jschJumpHostsCaptor.getAllValues().get(i);
			LOG.info(jumpHosts);
			assertTrue(jumpHosts.isEmpty());
			
			String username = jschUsernameCaptor.getAllValues().get(i);
			LOG.info(username);
			assertEquals("peadmin", username);
		}
		
		// Assert that the Git method was called 4 times with a commit string starting with 'Transition [1|2|4|5]'	
		int[] gitTransitions = {1, 2, 4, 5};
		for (int i = 0; i < gitTransitions.length; i++) {
			String gitArg = commitBranchArgCaptor.getAllValues().get(i);
			LOG.info(gitArg);
			assertTrue(gitArg.startsWith("Transition " + gitTransitions[i]));
		}
		
		// Assert that the YAML file has been updated so that 7 components are now absent
		StringBuilder fileContent = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				LOG.info(line);
				fileContent.append(line);
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}		
		assertEquals(7, StringUtils.countMatches(fileContent.toString(), "ensure: absent"));
	}
	
	private HieraEnvironmentUpdate buildMockYamlUpdate(String version, String path, File file, String zone) {
		HieraEnvironmentUpdate u = new HieraEnvironmentUpdate(APP_SHORT_NAME, zone);
		u.setExistingValue(version);
		u.setExistingPath(null);
		u.setRequestedPath(path);
		u.setRequestedValue("absent");
		u.setPathElementsAdded("");
		u.setPathElementsRemoved("");	
		u.setNodeMissingBehaviour(NodeMissingBehaviour.INSERT_ALL);
		
		HieraMachineState ssbFile = new HieraMachineState("", "ssb", file, null);
		u.setSource(ssbFile);
		return u;
	}
	
	private MCOCommand buildMockCommand(String mcCommand, Collection<ResolvedHost> hostnames) {
		return new MCOCommand(mcCommand, hostnames, APP_SHORT_NAME);
	}

	@Test
	public void testPrepare() throws Exception {
		Map<ComponentId, ComponentDeploymentData> data = new TreeMap<>();
		
		//5. New Version is blank - FAIL (Component Version is invalid or corrupt)
		data.put(createComponentId("componentA"), createComponentDeploymentData("componentA", " ", "1.0.0-1", true));
		
		//6. New version is "Absent" and existing versions is anything - UNINSTALL
		data.put(createComponentId("componentB"), createComponentDeploymentData("componentB", "absent", "1.0.0-1", true));
		
		//4. New version specified, existing yaml specified but existing version is blank - FIX
		data.put(createComponentId("componentC"), createComponentDeploymentData("componentC", "1.0.0", null, true));	
		
		//7. New version is anything and existing version is "Absent" - INSTALL
		data.put(createComponentId("componentD"), createComponentDeploymentData("componentD", "1.0.0", "absent", true));
		
		//1. Versions are the same - NO_CHANGE
		data.put(createComponentId("componentE"), createComponentDeploymentData("componentE", "1.0.0", "1.0.0-1", true));
		
		//2. New version greater than existing version - UPGRADE
		data.put(createComponentId("componentF"), createComponentDeploymentData("componentF", "1.0.1", "1.0.0-1", true));
		
		//3. New version is less than existing version - DOWNGRADE
		data.put(createComponentId("componentG"), createComponentDeploymentData("componentG", "1.0.0", "1.0.1-1", true));
		
		//11. No existing YAML - INSTALL	
		data.put(createComponentId("componentH"), createComponentDeploymentData("componentH", "1.0.0", null, true));
		
		//10. New version not specified & existing version is "Absent" - NO_CHANGE
		data.put(createComponentId("componentI"), createComponentDeploymentData("componentI", null, "absent", true));
		
		//9. New Version not specified and existing version exists- FAIL
		data.put(createComponentId("componentJ"), createComponentDeploymentData("componentJ", null, "1.0.0-1", true));
		
		//9. New Version not specified - FAIL
		data.put(createComponentId("componentK"), createComponentDeploymentData("componentK", null, null, true));
		
		//8. This is a fail, there is nothing we can do without a deployment descriptor
		data.put(createComponentId("componentL"), createComponentDeploymentData("componentL", "1.0.0", "1.0.0-1", false));

		ApplicationDeployment deployment = new ApplicationDeployment(createApplicationVersion());
		deployment.getComponents().putAll(data);
		HieraEnvironmentStateManager hieraData = new HieraEnvironmentStateManager();
		File hieraFolder = new File("src/test/resources/scenarios/EBSAD-11654-scoped_deploy/hiera");
		hieraData.load(hieraFolder, ConfigurationFactory.getOrganisations().get("st"), null, null);
		deployment.setEnvironmentStateManager(hieraData);
		
		new DifferenceManager().calculateDifferencesAndAssignActions(deployment);
		
		//5. New Version is blank - FAIL (Component Version is invalid or corrupt)
		verifyPrepare(ChangeType.FAIL, " ", "1.0.0-1", data.get(createComponentId("componentA")));
		
		//6. New version is "Absent" and existing versions is anything - UNINSTALL
		verifyPrepare(ChangeType.UNDEPLOY, "absent", "1.0.0-1", data.get(createComponentId("componentB")));
		
		//4. New version specified, existing yaml specified but existing version is blank - DEPLOY
		verifyPrepare(ChangeType.DEPLOY, "1.0.0", null, data.get(createComponentId("componentC")));
		
		//7. New version is anything and existing version is "Absent" - INSTALL
		// NOT ANY MORE we also now check that the component is in the deployment descriptor
		//verifyPrepare(ChangeType.DEPLOY, "1.0.0", "absent", data.get("componentD"));
		
		//1. Versions are the same - NO_CHANGE
		verifyPrepare(ChangeType.NO_CHANGE, "1.0.0", "1.0.0-1", data.get(createComponentId("componentE")));
		
		//2. New version greater than existing version - UPGRADE
		verifyPrepare(ChangeType.UPGRADE, "1.0.1", "1.0.0-1", data.get(createComponentId("componentF")));
		
		//3. New version is less than existing version - DOWNGRADE
		verifyPrepare(ChangeType.DOWNGRADE, "1.0.0", "1.0.1-1", data.get(createComponentId("componentG")));
		
		//11. No existing YAML - INSTALL	
		verifyPrepare(ChangeType.DEPLOY, "1.0.0", null, data.get(createComponentId("componentH")));
		
		//10. New version not specified & existing version is "Absent" - NO_CHANGE
		verifyPrepare(ChangeType.NO_CHANGE, null, "absent", data.get(createComponentId("componentI")));
		
		//9. New Version not specified and existing version exists- FAIL
		verifyPrepare(ChangeType.UNDEPLOY, null, "1.0.0-1", data.get(createComponentId("componentJ")));
		
		//9. New Version not specified and no existing version either - NO_CHANGE
		verifyPrepare(ChangeType.NO_CHANGE, null, null, data.get(createComponentId("componentK")));
		
		//8. This is a fail, there is nothing we can do without a deployment descriptor
		verifyPrepare(ChangeType.FAIL, "1.0.0", "1.0.0-1", data.get(createComponentId("componentL")));
		
	}

	private ApplicationVersion createApplicationVersion() {
		ApplicationVersion applicationVersion = new ApplicationVersion();
		Application application = new Application();
		application.setShortName(APP_SHORT_NAME);
		applicationVersion.setApplication(application);
		return applicationVersion;
	}

	private ComponentId createComponentId(String componentName) {
		return new ComponentId(componentName, APP_SHORT_NAME);
	}

	/**
	 * Verify the ChangeType, and the existing and new component versions (shown on the Deployment plan report)
	 * @since EBSAD-9338
	 * @param expectedChangeType The expected ChangeType
	 * @param expectedNewVersion The expected new component version
	 * @param expectedExistingVersion The expected existing component version
	 * @param cdd The Component Deployment Data
	 */
	private void verifyPrepare(ChangeType expectedChangeType, String expectedNewVersion, String expectedExistingVersion, ComponentDeploymentData cdd) {
		// Check ChangeType
		Assert.assertEquals(expectedChangeType, cdd.getChangeSets().get(0).getPrimaryChange().getChangeType());
		// Check new version
		if (expectedNewVersion == null) {
			Assert.assertNull("New target version must be null", cdd.getTargetCmponentVersion());
		} else {
			ComponentVersion newTargetVersion = cdd.getTargetCmponentVersion();
			Assert.assertNotNull("New target version must not be null", newTargetVersion);
			Assert.assertEquals(expectedNewVersion, newTargetVersion.getComponentVersion());
		}
		// Check existing version
		List<StateSearchResult> existingYaml = cdd.getOriginalExistingComponentState();
		String existingVersion = null;
		if (existingYaml != null) {
			existingVersion = "";
			for (StateSearchResult searchResult : existingYaml) {
				String versionInYaml = searchResult.getComponentState().get(HieraData.ENSURE).toString();
				existingVersion += (existingVersion.equals("") ? versionInYaml : ", " + versionInYaml);
			}
		}
		Assert.assertEquals(expectedExistingVersion != null && StringUtils.isBlank(expectedExistingVersion) ? "null" : expectedExistingVersion, existingVersion);		
	}
	
	/**
	 * Generic method for creating sets which will feed the test
	 * @param componentName
	 * @param cvVersion
	 * @param yamlVersion
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ComponentDeploymentData createComponentDeploymentData(String componentName, String cvVersion, String yamlVersion, boolean makeXMLdd) {
		ComponentDeploymentData cdd = new ComponentDeploymentData(createComponentId(componentName));
		cdd.setHosts(new ArrayList<ResolvedHost>());
		if (yamlVersion != null) {
		    List<StateSearchResult> existingYaml = new ArrayList<StateSearchResult>();
		    StateSearchResult ex = new HieraStateSearchResult();
			XMLComponentType xmlCompType = createXMLComponentType(componentName, "");
			Map<String,Object> yaml = (Map<String,Object>) new Yaml().load(xmlCompType.getYaml());
			yaml = (Map<String,Object>) yaml.get(componentName);
			yaml.put("ensure", yamlVersion);
			ex.setComponentState(new TreeMap<String, Object>(yaml));
			HieraMachineState source = new HieraMachineState("IPT_ST_DEV1_EBS1", "hostname", new File("hostname.yaml"), yaml);
			ex.setSource(source);
			existingYaml.add(ex);
			cdd.setExistingState(existingYaml);
		}
		
		if (makeXMLdd) {
			XMLComponentType xmlCompType = createXMLComponentType(componentName, "");
			cdd.setDeploymentDescriptorDef(new Component(xmlCompType, new DeploymentDescriptor(new XMLDeploymentDescriptorType(), APP_SHORT_NAME), APP_SHORT_NAME));
			Map<String,Object> yaml = (Map<String,Object>) new Yaml().load(xmlCompType.getYaml());
			Map<String,Object> dd = (Map<String,Object>) yaml.get(componentName);
			cdd.setDeploymentDescriptorYaml(new TreeMap<String, Object>(dd));
		}
		
		if (cvVersion != null) {
		  cdd.setTargetComponentVersion(createComponentVersion(componentName, componentName, cvVersion));
		}
		return cdd;
	}

	/**
	 * Snippet of YAML
	 * @param componentName
	 * @param version
	 * @return
	 */
	private XMLComponentType createXMLComponentType(String componentName, String version) {
		XMLComponentType xmlComp = new ObjectFactory().createXMLComponentType();
		xmlComp.setMinimumPlan(new Integer(1));
		xmlComp.setYaml(componentName+":" + ApplicationDeploymentEngineTest.LS +
                         "    require:" + ApplicationDeploymentEngineTest.LS +
                         "      - 'Class[Profile::Wls::Ipt_custom]'" + ApplicationDeploymentEngineTest.LS +
                         "      - 'Class[Profile::Wls::Startwls_managed]'" + ApplicationDeploymentEngineTest.LS +
                         "    tag:    'appdeploy'");
		xmlComp.setHostnames("hostname");
		return xmlComp;
	}

	private ComponentVersion createComponentVersion(String componentName, String artifactId, String cptVersion) {
		ComponentVersion componentVersion = new ComponentVersion();
		componentVersion.setApplication(null);
		componentVersion.setName(componentName);
		componentVersion.setDateOfRelease(new Date());
		componentVersion.setJenkinsBuildNumber(1);
		componentVersion.setJenkinsJobName(null);
		componentVersion.setJenkinsBuildId(null);
		componentVersion.setGroupId("com.xyz");
		componentVersion.setArtifactId(artifactId);
		componentVersion.setComponentVersion(cptVersion);
		componentVersion.setPackaging("rpm");
		componentVersion.setClassifier(null);
		componentVersion.setType(null);
		componentVersion.setRpmPackageName(componentName);
		componentVersion.setRpmPackageVersion(cptVersion + "-1");
		componentVersion.setNotes(null);
		return componentVersion;
	}

	
}
