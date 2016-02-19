package com.ipt.ebsa.manage.deploy.comprelease;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

import com.ipt.ebsa.buildtools.release.entities.Application;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.entities.ComponentVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.ChainDeploymentVerification;
import com.ipt.ebsa.manage.deploy.DeploymentStatus;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeployment;
import com.ipt.ebsa.manage.deploy.comprelease.CompositeReleaseDeploymentEngine;
import com.ipt.ebsa.manage.deploy.data.ETD;
import com.ipt.ebsa.manage.git.EMGitManager;
import com.ipt.ebsa.manage.puppet.EMPuppetManager;
import com.ipt.ebsa.manage.puppet.SshManager;
import com.ipt.ebsa.manage.transitions.Transition;
import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.ssh.HostnameUsernamePort;

public class CompositeReleaseDeploymentTest {
	
	private static final Logger LOG = Logger.getLogger(CompositeReleaseDeploymentTest.class);
	
	// Temp directory holds hiera files that are updated
	private static final String TEMP_TEST_FILES = "target/ss3";
	
	@Before
	public void beforeEveryTest() {
		try {
			FileUtils.copyDirectory(new File("src/test/resources/ss3"), new File(TEMP_TEST_FILES));
		} catch (IOException e) {
			LOG.error(e);
		}
	}
	
	@After
	public void afterEveryTest() {
		try {
			FileUtils.deleteDirectory(new File(TEMP_TEST_FILES));
		} catch (IOException e) {
			LOG.error(e);
		}
	}

	@Test
	public void EBSAD26163() throws Exception {
		String scenarioFolder = "EBSAD26163";

		// Set some props before we start
		// Set Puppet Master port to empty so the default is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_PORT, "");
		// Set username otherwise 'localhost' is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_USERNAME, "peadmin");
		String puppetHost = "puppetmaster.host";
		ConfigurationFactory.getProperties().put("st.puppet.master.host", puppetHost);
		ConfigurationFactory.getProperties().put(Configuration.ENABLE_MCO, "true");

		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARE_ONLY, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX, "st");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_COMPOSITE_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.GIT_LOCAL_CHECKOUT_DIR, TEMP_TEST_FILES + File.separator + scenarioFolder);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER, TEMP_TEST_FILES + File.separator + scenarioFolder);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_CREATE_HIERA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER, "target/reports/ss3/" + scenarioFolder);
		ConfigurationFactory.getProperties().put("st.rpm.failfile.report.enabled", "false");
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR);

		List<ApplicationVersion> applicationVersions = new ArrayList<>();
		ApplicationVersion applicationVersion = buildApplicationVersion("SSB-MID1", "3.0.3", new String[][]{
				new String[]{"ssb-baseline-rpm", "5.0.24"},
				new String[]{"gen-ins-fuse-service-wrapper", "5.0.17"},
				new String[]{"gen-ins-fuse-fabric-ensemble", "5.0.16"},
				new String[]{"gen-ins-fuse-fabric", "5.0.16"},
				new String[]{"gen-ssb-rpm-scripts", "5.0.56"},
				new String[]{"gen-ssb-common-scripts", "5.0.32"},
				new String[]{"gen-bin-jboss-fuse", "5.0.17"},
				new String[]{"gen-ins-jboss-fuse", "5.0.19"}});
		applicationVersions.add(applicationVersion);

		applicationVersion = buildApplicationVersion("SSB-MID2", "3.0.3", new String[][]{
				new String[]{"gen-ssb-rpm-scripts", "5.0.56"},
				new String[]{"gen-bin-jboss-fuse", "5.0.17"},
				new String[]{"gen-ins-fuse-fabric", "5.0.16"},
				new String[]{"gen-ins-fuse-fabric-ensemble", "5.0.16"},
				new String[]{"ssb-baseline-rpm", "5.0.24"},
				new String[]{"gen-ins-fuse-service-wrapper", "5.0.17"},
				new String[]{"gen-ins-jboss-fuse", "5.0.19"},
				new String[]{"gen-ssb-common-scripts", "5.0.32"}});
		applicationVersions.add(applicationVersion);

		ReleaseVersion release = new ReleaseVersion();
		release.setApplicationVersions(applicationVersions);

		String releaseDeploymentDescriptor = "Deployment Descriptor_SIT1.xml";

		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(release, "IPT_ST_SIT1", releaseDeploymentDescriptor);

		CompositeReleaseDeploymentEngine engine = new CompositeReleaseDeploymentEngine();
		engine.setDeployment(deployment);

		deployment.setDeploymentEngine(engine);

		deployment.run();

		ETD[] t0 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "5.0.24-1", "system::packages/ssb-baseline-rpm/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/dbstzm01.st-sit1-ssb1.ipt.local.yaml"),
		};

		ETD[] t1 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "absent", "system::packages/ssb-baseline-rpm/ensure", "5.0.24-1", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "absent", "system::packages/ssb-baseline-rpm/ensure", "5.0.24-1", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "absent", "system::packages/gen-ins-fuse-fabric/ensure", "5.0.16-1", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "absent", "system::packages/gen-ins-fuse-fabric/ensure", "5.0.16-1", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "absent", "system::packages/gen-ins-fuse-fabric-ensemble/ensure", "5.0.16-1", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "absent", "system::packages/gen-ins-fuse-fabric-ensemble/ensure", "5.0.16-1", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "absent", "system::packages/gen-ins-fuse-service-wrapper/ensure", "5.0.17-1", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "absent", "system::packages/gen-ins-fuse-service-wrapper/ensure", "5.0.17-1", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "absent", "system::packages/gen-ins-jboss-fuse/ensure", "5.0.19-1", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "absent", "system::packages/gen-ins-jboss-fuse/ensure", "5.0.19-1", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "absent", "system::packages/gen-bin-jboss-fuse/ensure", "5.0.17-1", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "absent", "system::packages/gen-bin-jboss-fuse/ensure", "5.0.17-1", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
		};

		ETD[] t2 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "absent", "system::packages/gen-ssb-rpm-scripts/ensure", "5.0.56-1", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "absent", "system::packages/gen-ssb-rpm-scripts/ensure", "5.0.56-1", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
		};

		ETD[] t3 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "5.0.32-1", "system::packages/gen-ssb-common-scripts/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "5.0.32-1", "system::packages/gen-ssb-common-scripts/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "5.0.56-1", "system::packages/gen-ssb-rpm-scripts/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "5.0.56-1", "system::packages/gen-ssb-rpm-scripts/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "5.0.17-1", "system::packages/gen-bin-jboss-fuse/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "5.0.17-1", "system::packages/gen-bin-jboss-fuse/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "5.0.19-1", "system::packages/gen-ins-jboss-fuse/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "5.0.19-1", "system::packages/gen-ins-jboss-fuse/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "5.0.17-1", "system::packages/gen-ins-fuse-service-wrapper/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "5.0.17-1", "system::packages/gen-ins-fuse-service-wrapper/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "5.0.16-1", "system::packages/gen-ins-fuse-fabric/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "5.0.16-1", "system::packages/gen-ins-fuse-fabric/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "5.0.16-1", "system::packages/gen-ins-fuse-fabric-ensemble/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "5.0.16-1", "system::packages/gen-ins-fuse-fabric-ensemble/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
				new ETD(true, "5.0.24-1", "system::packages/ssb-baseline-rpm/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
				new ETD(true, "5.0.24-1", "system::packages/ssb-baseline-rpm/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
		};

		String yumCsv = "ssb-baseline-rpm,5.0.24-1,,,5.0.24,,\n" +
				"gen-bin-jboss-fuse,5.0.17-1,,,5.0.17,,\n" +
				"gen-ins-fuse-fabric,5.0.16-1,,,5.0.16,,\n" +
				"gen-ins-fuse-fabric-ensemble,5.0.16-1,,,5.0.16,,\n" +
				"gen-ins-fuse-service-wrapper,5.0.17-1,,,5.0.17,,\n" +
				"gen-ins-jboss-fuse,5.0.19-1,,,5.0.19,,\n" +
				"gen-ssb-common-scripts,5.0.32-1,,,5.0.32,,\n" +
				"gen-ssb-rpm-scripts,5.0.56-1,,,5.0.56,,\n" +
				"ssb-baseline-rpm,5.0.24-1,,,5.0.24,,\n";

		ChainDeploymentVerification.verify(deployment, yumCsv, 4, new int[]{1,12,2,16}, t0,t1,t2,t3);
	}
	
	@Test
	public void testEBSAD21646() throws Exception {
		String scenarioFolder = "EBSAD-21646";
		
		// Set some props before we start
		// Set Puppet Master port to empty so the default is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_PORT, "");
		// Set username otherwise 'localhost' is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_USERNAME, "peadmin");
		String puppetHost = "puppetmaster.host";
		ConfigurationFactory.getProperties().put("st.puppet.master.host", puppetHost);
		ConfigurationFactory.getProperties().put(Configuration.ENABLE_MCO, "true");

		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARE_ONLY, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX, "st");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_COMPOSITE_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.GIT_LOCAL_CHECKOUT_DIR, TEMP_TEST_FILES + File.separator + scenarioFolder);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER, TEMP_TEST_FILES + File.separator + scenarioFolder);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_CREATE_HIERA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER, "target/reports/ss3/" + scenarioFolder);
		ConfigurationFactory.getProperties().put("st.rpm.failfile.report.enabled", "false");
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR);
		
		List<ApplicationVersion> applicationVersions = new ArrayList<>();
		ApplicationVersion applicationVersion = buildApplicationVersion("EBSA_TEST_RPMS_BLUE", "1.0.0", new String[][] {
				new String[]{"ebsa-test-rpm-j", "1.0.4"}});				
		applicationVersions.add(applicationVersion);
		
		applicationVersion = buildApplicationVersion("EBSA_TEST_RPMS_RED", "1.0.0", new String[][] {
				new String[]{"ebsa-test-rpm-e", "1.0.1"}
		});		 
		applicationVersions.add(applicationVersion);
		
		applicationVersion = buildApplicationVersion("EBSA_TEST_RPMS_RED", "1.0.0", new String[][] {
				new String[]{"ebsa-test-rpm-m", "1.0.1"}
		});		
		applicationVersions.add(applicationVersion);
		
		applicationVersion = buildApplicationVersion("EBSA_TEST_RPMS_GREEN", "1.0.0", new String[][] {
				new String[]{"ebsa-test-rpm-a", "1.0.4"}
		});		
		applicationVersions.add(applicationVersion);
		
		applicationVersion = buildApplicationVersion("EBSA_TEST_RPMS_YELLOW", "1.0.0", new String[][] {
				new String[]{"ebsa-test-rpm-s", "1.0.7"}
		});		
		applicationVersions.add(applicationVersion);
		
		ReleaseVersion release = new ReleaseVersion();
		release.setApplicationVersions(applicationVersions);
		
		String releaseDeploymentDescriptor = "DeploymentDescriptorEBSA-TEST-RPMS-3phase.xml";
		
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(release, "IPT_ST_DEV1_EBS1", releaseDeploymentDescriptor);
		
		CompositeReleaseDeploymentEngine engine = new CompositeReleaseDeploymentEngine();
		engine.setDeployment(deployment);
		
		deployment.setDeploymentEngine(engine);
		
		deployment.run();
		
		ETD[] t0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{tag=appdeploy, ensure=1.0.4-1}", "system::packages/ebsa-test-rpm-j", "{tag=dangermouse, ensure=1.0.4-1}", "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
		
		ETD[] t1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "dangermouse", "system::packages/ebsa-test-rpm-j/tag", "appdeploy", "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
		
		ETD[] t2 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(60),
		};
		
		ETD[] t3 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "absent", "system::packages/ebsa-test-rpm-e/ensure", "1.0.1-1", "/hiera-ext/st/st-dev1-ebs1/ext/ldptzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
		
		ETD[] t4 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "Frankly my dear, I don't give a damn"),
		};
		
		ETD[] t5 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, existingPath, filePath
			new ETD(true, "penfold", "system::packages/ebsa-test-rpm-j/tag", "dangermouse", "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
			
		ETD[] t6 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "1.0.7-1", "system::packages/ebsa-test-rpm-s/ensure", "1.0.6-1", "/hiera-ext/st/st-dev1-ebs1/ext/rmatzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
		
		ETD[] t7 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, existingPath, filePath
			// YAML remove 
			new ETD(true, null, null, "penfold", "system::packages/ebsa-test-rpm-j/tag", "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
			
		String csv = "ebsa-test-rpm-s,1.0.7-1,,,1.0.7,,\n";
		
		ChainDeploymentVerification.verify(deployment, csv, 8, new int[]{1,1,1,1,1,1,1,1}, t0,t1,t2,t3,t4,t5,t6,t7);
	}
	
	@Test
	public void testEBSAD21648() throws Exception {
		CompositeReleaseDeployment deployment = setupScenario("1.0.0", "EBSAD-21648");
		
		deployment.run();
		
		ETD[] t0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{tag=appdeploy, ensure=1.0.4-1}", "system::packages/ebsa-test-rpm-j", "{tag=dangermouse, ensure=1.0.4-1}", "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm01.st-dev1-ebs1.ipt.local.yaml"),
			
		};
		
		ETD[] t1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "dangermouse", "system::packages/ebsa-test-rpm-j/tag", "appdeploy", "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
		
		ETD[] t2 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(60),
		};
		
		ETD[] t3 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "absent", "system::packages/ebsa-test-rpm-e/ensure", "1.0.1-1", "/hiera-ext/st/st-dev1-ebs1/ext/ldptzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
		
		ETD[] t4 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "Frankly my dear, I don't give a damn"),
		};
		
		ETD[] t5 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "1.0.7-1", "system::packages/ebsa-test-rpm-s/ensure", "1.0.6-1", "/hiera-ext/st/st-dev1-ebs1/ext/rmatzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
		
		ETD[] t6 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, existingPath, filePath
			// YAML remove 
			new ETD(true, null, null, "dangermouse", "system::packages/ebsa-test-rpm-j/tag", "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
			
		String csv = "ebsa-test-rpm-s,1.0.7-1,,,1.0.7,,\n";
		
		ChainDeploymentVerification.verify(deployment, csv, 7, new int[]{1,1,1,1,1,1,1}, t0,t1,t2,t3,t4,t5,t6);
	}
	
	/**
	 * Checks that the scope set-up for a given application only applies to components under that application and
	 * doesn't 'leak' across to other applications. 
	 */
	@Test
	public void testEBSAD22548() throws Exception {
		CompositeReleaseDeployment deployment = setupScenario("1.0.0", "EBSAD-22548");
		
		deployment.run();
		
		ETD[] t0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "1.0.4-1", "system::packages/ebsa-test-rpm-j/ensure", "1.0.3-1", "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm01.st-dev1-ebs1.ipt.local.yaml"),
			
		};
		
		ETD[] t1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "dangermouse", "system::packages/ebsa-test-rpm-j/tag", "appdeploy", "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
		
		ETD[] t2 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(60),
		};
		
		ETD[] t3 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "absent", "system::packages/ebsa-test-rpm-e/ensure", "1.0.0-1", "/hiera-ext/st/st-dev1-ebs1/ext/ldptzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
		
		ETD[] t4 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "{ensure=1.0.4-1, tag=appdeploy}", "system::packages/ebsa-test-rpm-a", null, "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm02.st-dev1-ebs1.ipt.local.yaml"),
				new ETD(true, "1.0.1-1", "system::packages/ebsa-test-rpm-m/ensure", "1.0.5-1", "/hiera-ext/st/st-dev1-ebs1/ext/ldptzm01.st-dev1-ebs1.ipt.local.yaml")
			};
		
		ETD[] t5 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "Frankly my dear, I don't give a damn"),
		};
		
		ETD[] t6 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "absent", "system::packages/ebsa-test-rpm-s/ensure", "1.0.8-1", "/hiera-ext/st/st-dev1-ebs1/ext/ssb.yaml"),
		};
		
		ETD[] t7 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "1.0.7-1", "system::packages/ebsa-test-rpm-s/ensure", "1.0.6-1", "/hiera-ext/st/st-dev1-ebs1/ext/rmatzm01.st-dev1-ebs1.ipt.local.yaml"),
			};
		
		ETD[] t8 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, existingPath, filePath
			// YAML remove 
			new ETD(true, null, null, "dangermouse", "system::packages/ebsa-test-rpm-j/tag", "/hiera-ext/st/st-dev1-ebs1/ext/ssbtzm01.st-dev1-ebs1.ipt.local.yaml"),
		};
			
		String csv = "ebsa-test-rpm-j,1.0.4-1,,,1.0.4,,\n" + 
					 "ebsa-test-rpm-a,1.0.4-1,,,1.0.4,,\n" +
					 "ebsa-test-rpm-m,1.0.1-1,,,1.0.1,,\n" +
					 "ebsa-test-rpm-s,1.0.7-1,,,1.0.7,,\n";
		
		ChainDeploymentVerification.verify(deployment, csv, 9, new int[]{1,1,1,1,2,1,1,1,1}, t0,t1,t2,t3,t4,t5,t6,t7,t8);
	}

	@Test
	public void testEBSAD21504() throws Exception {
		CompositeReleaseDeployment deployment = setupScenario("1.0.1", "EBSAD-21648");
		try {
			deployment.run();
			fail("Should throw exception");
		} catch (RuntimeException e) {
			assertEquals("Unable to resolve hosts and/or roles: [ssb1]", e.getCause().getMessage());
		}
		
	}
	
	protected CompositeReleaseDeployment setupScenario(String yellowVersion, String testDataSubDir) throws Exception {
		// Set some props before we start
		// Set Puppet Master port to empty so the default is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_PORT, "");
		// Set username otherwise 'localhost' is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_USERNAME, "peadmin");
		String puppetHost = "puppetmaster.host";
		ConfigurationFactory.getProperties().put("st.puppet.master.host", puppetHost);
		ConfigurationFactory.getProperties().put(Configuration.ENABLE_MCO, "true");

		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARE_ONLY, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX, "st");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_COMPOSITE_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.GIT_LOCAL_CHECKOUT_DIR, TEMP_TEST_FILES + File.separator + testDataSubDir);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER, TEMP_TEST_FILES + File.separator + testDataSubDir);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_CREATE_HIERA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER, "target/reports/ss3/" + testDataSubDir);
		ConfigurationFactory.getProperties().put("st.rpm.failfile.report.enabled", "false");
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR);
		
		List<ApplicationVersion> applicationVersions = new ArrayList<>();
		ApplicationVersion applicationVersion = buildApplicationVersion("EBSA_TEST_RPMS_BLUE", "1.0.0", new String[][] {
				new String[]{"ebsa-test-rpm-j", "1.0.4"}});				
		applicationVersions.add(applicationVersion);
		
		applicationVersion = buildApplicationVersion("EBSA_TEST_RPMS_RED", "1.0.0", new String[][] {
				new String[]{"ebsa-test-rpm-e", "1.0.0"}
		});		 
		applicationVersions.add(applicationVersion);
		
		applicationVersion = buildApplicationVersion("EBSA_TEST_RPMS_RED", "1.0.0", new String[][] {
				new String[]{"ebsa-test-rpm-m", "1.0.1"}
		});		
		applicationVersions.add(applicationVersion);
		
		applicationVersion = buildApplicationVersion("EBSA_TEST_RPMS_GREEN", "1.0.0", new String[][] {
				new String[]{"ebsa-test-rpm-a", "1.0.4"}
		});		
		applicationVersions.add(applicationVersion);
		
		applicationVersion = buildApplicationVersion("EBSA_TEST_RPMS_YELLOW", yellowVersion, new String[][] {
				new String[]{"ebsa-test-rpm-s", "1.0.7"}
		});		
		applicationVersions.add(applicationVersion);
		
		ReleaseVersion release = new ReleaseVersion();
		release.setApplicationVersions(applicationVersions);
		
		String releaseDeploymentDescriptor = "DeploymentDescriptorEBSA-TEST-RPMS-3phase.xml";
		
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(release, "IPT_ST_DEV1_EBS1", releaseDeploymentDescriptor);
		
		CompositeReleaseDeploymentEngine engine = new CompositeReleaseDeploymentEngine();
		engine.setDeployment(deployment);
		
		deployment.setDeploymentEngine(engine);
		return deployment;
	}
	
	/**
	 * This is the mother of all unit tests.
	 * Test that builds some transitions for SSB and SSBSIM deployed together into NP.
	 * Will fail if an exception is thrown.
	 * The transitions are asserted.
	 * The transitions are executed with mocked Puppet + Git and the calls are asserted. 
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testNP() throws Exception {
		// Set some props before we start
		String puppetHost = "puppetmaster.host";
		CompositeReleaseDeployment deployment = createNPDeployment(puppetHost);
		
		CompositeReleaseDeploymentEngine engine = new CompositeReleaseDeploymentEngine();
		engine.setDeployment(deployment);
		
		deployment.setDeploymentEngine(engine);
		
//		Map<String, String> zoneNames = deployment.getZoneNames();
//		zoneNames.put("SSB", "HO_IPT_NP_PRP2_PRZO");
//		zoneNames.put("SSBSIM", "HO_IPT_NP_PRP2_ESZO");
		
		deployment.run();
		
		ETD[] t0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "1.7.1-3.el6_4.1", "system::packages/git/ensure", "1.7.1-3.el6_4.1", "/hiera-ext/np/np-prp2-przo/ext/ssm.yaml"),
				new ETD(true, "mwdeploy", "system::packages/git/tag", "mwdeploy", "/hiera-ext/np/np-prp2-przo/ext/ssm.yaml"),
		};
			
		ETD[] t1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "2.0.0-12.el6", "system::packages/opencv/ensure", "2.0.0-12.el6", "/hiera-ext/np/np-prp2-przo/ext/ssb.yaml"),
				new ETD(true, "mwdeploy", "system::packages/opencv/tag", "mwdeploy", "/hiera-ext/np/np-prp2-przo/ext/ssb.yaml"),
		};
				
		ETD[] t2 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "0.10.29-2.el6", "system::packages/gstreamer-plugins-base/ensure", "0.10.29-2.el6", "/hiera-ext/np/np-prp2-przo/ext/ssb.yaml"),
				new ETD(true, "mwdeploy", "system::packages/gstreamer-plugins-base/tag", "mwdeploy", "/hiera-ext/np/np-prp2-przo/ext/ssb.yaml"),
		};
				
		ETD[] t3 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "absent", "system::packages/ssb-core-features-fuse-application/ensure", "2.1.646-1", "/hiera-ext/np/np-prp2-przo/ext/ssm.yaml"),
				new ETD(true, "absent", "system::packages/ssb-core-features-fuse-config/ensure", "2.1.646-1", "/hiera-ext/np/np-prp2-przo/ext/ssm.yaml"),
		};
		
		ETD[] t4 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "absent", "system::packages/iabs-simulator-rpm/ensure", "0.0.757-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem01.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/iabs-simulator-rpm/ensure", "0.0.757-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem02.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/iabs-simulator-rpm/ensure", "0.0.757-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem03.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/ssb-simulator-rpm/ensure", "0.0.803-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem01.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/ssb-simulator-rpm/ensure", "0.0.803-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem02.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/ssb-simulator-rpm/ensure", "0.0.803-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem03.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/ocf-simulator-rpm/ensure", "0.0.285-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem01.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/ocf-simulator-rpm/ensure", "0.0.285-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem02.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/ocf-simulator-rpm/ensure", "0.0.285-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem03.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/pkiss-simulator-rpm/ensure", "0.0.818-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem01.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/pkiss-simulator-rpm/ensure", "0.0.818-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem02.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/pkiss-simulator-rpm/ensure", "0.0.818-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem03.np-prp2-eszo.ipt.ho.local.yaml"),
		};
		
		ETD[] t5 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "absent", "system::packages/ssb-core-features-lib-nexus/ensure", "2.1.646-1", "/hiera-ext/np/np-prp2-przo/ext/rma.yaml"),
		};
		
		ETD[] t6 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "absent", "system::packages/ssb-common-config-rpm/ensure", "1.0.1116-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem01.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/ssb-common-config-rpm/ensure", "1.0.1116-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem02.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "absent", "system::packages/ssb-common-config-rpm/ensure", "1.0.1116-1", "/hiera-ext/np/np-prp2-eszo/ext/tstem03.np-prp2-eszo.ipt.ho.local.yaml"),
		};
		
		ETD[] t7 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "absent", "system::packages/ssb-rpm-nexus-baseline-config/ensure", "2.0.44-1", "/hiera-ext/np/np-prp2-przo/ext/rma.yaml"),
		};
		
		ETD[] t8 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "2.0.44-1", "system::packages/ssb-rpm-nexus-baseline-config/ensure", "absent", "/hiera-ext/np/np-prp2-przo/ext/rma.yaml"),
				new ETD(true, "1.0.1114-1", "system::packages/ssb-common-config-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem01.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "1.0.1114-1", "system::packages/ssb-common-config-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem02.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "1.0.1114-1", "system::packages/ssb-common-config-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem03.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "2.1.641-1", "system::packages/ssb-core-features-lib-nexus/ensure", "absent", "/hiera-ext/np/np-prp2-przo/ext/rma.yaml"),
				new ETD(true, "0.0.753-1", "system::packages/iabs-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem01.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.753-1", "system::packages/iabs-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem02.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.753-1", "system::packages/iabs-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem03.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.799-1", "system::packages/ssb-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem01.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.799-1", "system::packages/ssb-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem02.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.799-1", "system::packages/ssb-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem03.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.281-1", "system::packages/ocf-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem01.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.281-1", "system::packages/ocf-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem02.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.281-1", "system::packages/ocf-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem03.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.814-1", "system::packages/pkiss-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem01.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.814-1", "system::packages/pkiss-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem02.np-prp2-eszo.ipt.ho.local.yaml"),
				new ETD(true, "0.0.814-1", "system::packages/pkiss-simulator-rpm/ensure", "absent", "/hiera-ext/np/np-prp2-eszo/ext/tstem03.np-prp2-eszo.ipt.ho.local.yaml"),
		};
		
		ETD[] t9 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "2.1.641-1", "system::packages/ssb-core-features-fuse-config/ensure", "absent", "/hiera-ext/np/np-prp2-przo/ext/ssm.yaml"),
		};
		
		ETD[] t10 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "2.1.641-1", "system::packages/ssb-core-features-fuse-application/ensure", "absent", "/hiera-ext/np/np-prp2-przo/ext/ssm.yaml"),
		};
		
		ETD[] t11 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "true", "system::services/fuse-service/hasstatus", "true", "/hiera-ext/np/np-prp2-przo/ext/ssb.yaml"),
				new ETD(true, "false", "system::services/fuse-service/enable", "true", "/hiera-ext/np/np-prp2-przo/ext/ssb.yaml"),
				new ETD(true, "stopped", "system::services/fuse-service/ensure", "running", "/hiera-ext/np/np-prp2-przo/ext/ssb.yaml"),
		};
		
		ETD[] t12 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "true", "system::services/fuse-service/enable", "false", "/hiera-ext/np/np-prp2-przo/ext/ssb.yaml"),
				new ETD(true, "running", "system::services/fuse-service/ensure", "stopped", "/hiera-ext/np/np-prp2-przo/ext/ssb.yaml"),
		};
			
		String csv = "iabs-simulator-rpm,0.0.753-1,,,0.0.753,,\n"
		+ "ocf-simulator-rpm,0.0.281-1,,,0.0.281,,\n"
		+ "pkiss-simulator-rpm,0.0.814-1,,,0.0.814,,\n"
		+ "ssb-common-config-rpm,1.0.1114-1,,,1.0.1114,,\n"
		+ "ssb-core-features-fuse-application,2.1.641-1,,,2.1.641,,\n"
		+ "ssb-core-features-fuse-config,2.1.641-1,,,2.1.641,,\n"
		+ "ssb-core-features-lib-nexus,2.1.641-1,,,2.1.641,,\n"
		+ "ssb-rpm-nexus-baseline-config,2.0.44-1,,,2.0.44,,\n"
		+ "ssb-simulator-rpm,0.0.799-1,,,0.0.799,,\n";

		ChainDeploymentVerification.verify(deployment, csv, 13, new int[]{2,2,2,2,12,1,3,1,17,1,1,3,2}, t0,t1,t2,t3,t4,t5,t6,t7,t8,t9,t10,t11,t12);

		// Mockito the JschManager and always return exit code 0 from runSSHExec
		SshManager ssh = mock(SshManager.class);
		when(ssh.runSSHExec(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class))).thenReturn(0);
		when(ssh.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean())).thenReturn(new ExecReturn(0));
				
		// Mockito the GITManager - the method we are mocking commitBranchMergeToMaster() is void so no need to specify here
		EMGitManager git = mock(EMGitManager.class);
		deployment.setGitManager(git);
		
		engine.executeTransitions(deployment, new EMPuppetManager(ssh));
		
		// Assert that the Puppet method was called 26 times (twice for each transition) with the environment name as the arg 
		ArgumentCaptor<Integer> jschTimeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> jschCommandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschUsernameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschHostCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> jschPortCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<List> jschJumpHostsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<String> commitBranchArgCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Boolean> jschUnescapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		
		InOrder inOrder = inOrder(ssh, git);
		// Assert that Jsch manager and git were called in the expected order.		
		// T0
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T1
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T2
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T3
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T4
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T5
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T6
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T7
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T8
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T9
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T10
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T11
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T12
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		String[] commands = {
				// T0
				// JschManager.runSSHExec
				"./syncPuppetConfig.strategic.sh",
				// JschManager.runSSHExecWithOutput
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-przo.ipt.ho.local and (role=ssm))\"",
				
				// T1
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-przo.ipt.ho.local and (role=ssb))\"",
				
				// T2
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-przo.ipt.ho.local and (role=ssb))\"",
				
				// T3
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-przo.ipt.ho.local and (role=ssm))\"",
				
				// T4
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-eszo.ipt.ho.local and (fqdn=tstem01.np-prp2-eszo.ipt.ho.local or fqdn=tstem02.np-prp2-eszo.ipt.ho.local or fqdn=tstem03.np-prp2-eszo.ipt.ho.local))\"",
				
				// T5
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-przo.ipt.ho.local and (role=rma))\"",
				
				// T6
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-eszo.ipt.ho.local and (fqdn=tstem01.np-prp2-eszo.ipt.ho.local or fqdn=tstem02.np-prp2-eszo.ipt.ho.local or fqdn=tstem03.np-prp2-eszo.ipt.ho.local))\"",
				
				// T7
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-przo.ipt.ho.local and (role=rma))\"",
				
				// T8
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-eszo.ipt.ho.local and (fqdn=tstem01.np-prp2-eszo.ipt.ho.local or fqdn=tstem02.np-prp2-eszo.ipt.ho.local or fqdn=tstem03.np-prp2-eszo.ipt.ho.local)) or (domain=np-prp2-przo.ipt.ho.local and (role=rma))\"",
				
				// T9
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-przo.ipt.ho.local and (role=ssm))\"",
				
				// T10
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-przo.ipt.ho.local and (role=ssm))\"",
				
				// T11
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-przo.ipt.ho.local and (role=ssb))\"",
				
				// T12
				"./syncPuppetConfig.strategic.sh",
				"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=np-prp2-przo.ipt.ho.local and (role=ssb))\"",
				};
		
		// Assert that the Jsch manager was called 26 times in total (twice for each transition)	
		List<String> commandValues = jschCommandCaptor.getAllValues();
		assertEquals(commands.length, commandValues.size());
		List<Integer> timeoutValues = jschTimeoutCaptor.getAllValues();
		assertEquals(commands.length, timeoutValues.size());
		List<String> hostValues = jschHostCaptor.getAllValues();
		assertEquals(commands.length, hostValues.size());
		List<Integer> portValues = jschPortCaptor.getAllValues();
		assertEquals(commands.length, portValues.size());
		List<List> jumpHostsValues = jschJumpHostsCaptor.getAllValues();
		assertEquals(commands.length, jumpHostsValues.size());
		List<String> usernameValues = jschUsernameCaptor.getAllValues();
		assertEquals(commands.length, usernameValues.size());
		for (int i = 0; i < commands.length; i++) {
			String command = commandValues.get(i);
			LOG.info(command);
			assertEquals(commands[i], command);
			
			int timeout = timeoutValues.get(i);
			LOG.info(timeout);
			assertEquals(3000000, timeout);
			
			String host = hostValues.get(i);
			LOG.info(host);
			assertEquals(puppetHost, host);
			
			int port = portValues.get(i);
			LOG.info(port);
			assertEquals(22, port);
			
			List<HostnameUsernamePort> jumpHosts = jumpHostsValues.get(i);
			LOG.info(jumpHosts);
			assertTrue(jumpHosts.isEmpty());
			
			String username = usernameValues.get(i);
			LOG.info(username);
			assertEquals("peadmin", username);
		}
		
		// Assert that the Git method was called 13 times with a commit string starting with 'Transition [0-12]'	
		int[] gitTransitions = {0,1,2,3,4,5,6,7,8,9,10,11,12};
		List<String> gitValues = commitBranchArgCaptor.getAllValues();
		assertEquals(gitTransitions.length, gitValues.size());
		for (int i = 0; i < gitTransitions.length; i++) {
			String gitArg = gitValues.get(i);
			LOG.info(gitArg);
			assertTrue(gitArg.startsWith("Transition " + gitTransitions[i]));
		}
	}
	
	@Test
	public void testNPReport() throws Exception {
		// Set some props before we start
		String puppetHost = "puppetmaster.host";
		CompositeReleaseDeployment deployment = createNPDeployment(puppetHost);
		
		CompositeReleaseDeploymentEngine engine = new CompositeReleaseDeploymentEngine();
		engine.setDeployment(deployment);
		
		deployment.setDeploymentEngine(engine);
		
		deployment.run();
		
		List<Transition> transitions = deployment.getTransitions();
		assertEquals(13, transitions.size());
		
		// Mockito the JschManager and always return exit code 0 from runSSHExec
		SshManager ssh = mock(SshManager.class);
		when(ssh.runSSHExec(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class))).thenReturn(0);
		when(ssh.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean())).thenReturn(new ExecReturn(0));
		
		// Mockito the GITManager - the method we are mocking commitBranchMergeToMaster() is void so no need to specify here
		EMGitManager git = mock(EMGitManager.class);
		deployment.setGitManager(git);
		
		final int transitionToFail = 3;
		//Mockito the EMPuppetManager - we want to make the doPuppetRun method fail!
		EMPuppetManager puppetManager = new EMPuppetManager(ssh) {
			private int callCount = 0;
			@Override
			public ExecReturn doPuppetRunWithRetry(Organisation organisation, Map<String, Set<String>> zoneRolesOrHosts, int maxRetryCount, int retryDelaySeconds) {
				callCount++;
				if (callCount == transitionToFail+1) {
					//Fail transition
					ExecReturn ret = new ExecReturn(1);
					ret.setStdOut("Log for error in the puppet run");
					return ret;
				}
				return new ExecReturn(0);
			}
		};
		
		Exception e = null;
		
		try {
			engine.executeTransitions(deployment, puppetManager);
		} catch (Exception e1) {
			e = e1;
		}
		
		assertTrue(e != null);
		for (int i = 0; i < deployment.getTransitions().size(); i++){
			Transition t = deployment.getTransitions().get(i);
			if (i < transitionToFail) {
				assertNull(t.getException());
				assertNull(t.getStatusMessage());
				assertEquals(DeploymentStatus.COMPLETED, t.getStatus());
			} else if (i == transitionToFail) {
				assertNotNull(t.getException());
				assertNotNull(t.getStatusMessage());
				assertEquals(DeploymentStatus.ERRORED, t.getStatus());
			} else {
				assertNull(t.getException());
				assertNull(t.getStatusMessage());
				assertEquals(DeploymentStatus.NOT_STARTED, t.getStatus());
			}
		}
	}

	private CompositeReleaseDeployment createNPDeployment(String puppetHost)
			throws Exception {
		// Set Puppet Master port to empty so the default is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_PORT, "");
		// Set username otherwise 'localhost' is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_USERNAME, "peadmin");
		ConfigurationFactory.getProperties().put("np.puppet.master.host", puppetHost);
		ConfigurationFactory.getProperties().put(Configuration.ENABLE_MCO, "true");

		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARE_ONLY, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX, "np");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_COMPOSITE_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.GIT_LOCAL_CHECKOUT_DIR, TEMP_TEST_FILES);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER, TEMP_TEST_FILES);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_CREATE_HIERA, "false");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER, "target/reports/ss3/np");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_AUTO_REFRESH_ENABLED, "true");
		ConfigurationFactory.getProperties().put("np.rpm.failfile.report.enabled", "false");
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR);
		
		List<ApplicationVersion> applicationVersions = new ArrayList<>();
		ApplicationVersion applicationVersion = buildApplicationVersion("SSB", "1.0.0", new String[][] {
				new String[]{"ssb-db-schema", "1.376.355"},
				new String[]{"ssb-ldap-schema", "1.166"},
				new String[]{"ssb-rpm-nexus-baseline-config", "2.0.44"},
				new String[]{"ssb-core-features-lib-nexus", "2.1.641"},
				new String[]{"ssb-core-features-fuse-config", "2.1.641"}, 
				new String[]{"ssb-core-features-fuse-application", "2.1.641"}});				
		applicationVersions.add(applicationVersion);
		
		applicationVersion = buildApplicationVersion("SSBSIM", "1.0.0", new String[][] {
				new String[]{"ocf-simulator-rpm", "0.0.281"},
				new String[]{"ssb-simulator-rpm", "0.0.799"},
				new String[]{"pkiss-simulator-rpm", "0.0.814"},
				new String[]{"iabs-simulator-rpm", "0.0.753"},
				new String[]{"ssb-sim-master-data-ta-rpm", "1.0.48"},
				new String[]{"ssb-common-config-rpm", "1.0.1114"}
		});		
		applicationVersions.add(applicationVersion);
		
		ReleaseVersion release = new ReleaseVersion();
		release.setApplicationVersions(applicationVersions);
		
		String releaseDeploymentDescriptor = "SSB DD.xml";
		
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(release, "HO_IPT_NP_PRP2", releaseDeploymentDescriptor);
		return deployment;
	}
	
	/**
	 * OK, now THIS is the mother of all unit tests!
	 * Test that builds some transitions for SIT Small deployment into ST.
	 * See: https://confluenceurl/display/EN/Typical+Deployment+Steps
	 * Will fail if an exception is thrown.
	 * Transitions are asserted.
	 * The transitions are executed with mocked Puppet + Git and the calls are asserted. 
	 * The deployment descriptor contains a stop mid-way through.
	 * After the stop the deployment is re-run through to completion.
	 * Transitions are asserted for the second half.
	 * The transitions are executed with mocked Puppet + Git and the calls are asserted for the second half.	 
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testSITS() throws Exception {
		// Set some props before we start
		String puppetHost = "puppetmaster.host";
		ReleaseVersion release = new ReleaseVersion();
		String releaseDeploymentDescriptor = "SITS.xml";
		
		CompositeReleaseDeployment deployment = createSITSDeployment(
				puppetHost, release, releaseDeploymentDescriptor);
		
		CompositeReleaseDeploymentEngine engine = new CompositeReleaseDeploymentEngine();
		engine.setDeployment(deployment);
		
		deployment.setDeploymentEngine(engine);
				
		deployment.run();
		
		// Phase 1
		ETD[] t0 = new ETD[]{
			new ETD(true, "absent", "system::packages/gg_lr_sis/ensure", "1.45-1", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
			new ETD(true, "absent", "system::packages/lr_crs_sw/ensure", "1.3-1", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
		};
		ETD[] t1 = new ETD[]{	
			new ETD(true, "absent", "system::packages/lr_sis_etl_4.0.27.0.0/ensure", "1.19-1", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
			new ETD(true, "absent", "system::packages/lr_sis_etl_cdc_4.0.28.0.0/ensure", "1.3-1", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
		};
		ETD[] t2 = new ETD[]{	
			new ETD(true, "absent", "system::packages/lr_cid_sw_4.0/ensure", "1.54-1", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
			new ETD(true, "absent", "system::packages/lr_sis_conf/ensure", "1.66-1", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
			new ETD(true, "absent", "system::packages/lr_crs_conf/ensure", "1.15-1", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
		};
		ETD[] t3 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "1.81-1", "system::packages/lr_cid_sw_4.0/ensure", "absent", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
			new ETD(true, "0.0.799-1", "system::packages/lr_sis_conf/ensure", "absent", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
			new ETD(true, "1.22-1", "system::packages/lr_crs_conf/ensure", "absent", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
			new ETD(true, "31.0.0-1", "system::packages/CommonConfigRPM/ensure", "29.0.0-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{ensure=1.8-1, require=[Package[lr_crs_conf], Package[lr_crs_db_2.0.27.0.0]], tag=appdeploy}", "system::packages/lr_crs_db_2.0.30.1.0", null, "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
		};
		ETD[] t4 = new ETD[]{
			new ETD(true, "{require=Package[lr_sis_conf], tag=appdeploy, ensure=1.3-1}", "system::packages/lr_sis_etl_cdc_4.0.28.0.0", "{ensure=absent, require=Package[lr_sis_etl_4.0.27.0.0], tag=appdeploy}", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
		};
		ETD[] t5 = new ETD[]{
			new ETD(true, "1.7.1-3.el6_4.1", "system::packages/git/ensure", "1.7.1-3.el6_4.1", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
			new ETD(true, "mwdeploy", "system::packages/git/tag", "mwdeploy", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
		};
		ETD[] t6 = new ETD[]{
			new ETD(true, "2.0.0-12.el6", "system::packages/opencv/ensure", "2.0.0-12.el6", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
			new ETD(true, "mwdeploy", "system::packages/opencv/tag", "mwdeploy", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
		};
		ETD[] t7 = new ETD[]{
			new ETD(true, "0.10.29-2.el6", "system::packages/gstreamer-plugins-base/ensure", "0.10.29-2.el6", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
			new ETD(true, "mwdeploy", "system::packages/gstreamer-plugins-base/tag", "mwdeploy", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
		};
		ETD[] t8 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "2.1.646-1", "system::packages/ssb-core-features-lib-nexus/ensure", "2.1.641-1", "/hiera-ext/st/st-sit1-ssb1/ext/rma.yaml"),
			new ETD(true, "32.0.4-1", "system::packages/IPTSOACommon/ensure", "30.0.1-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "31.0.6-1", "system::packages/ipt/ensure", "30.0.1-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "31.0.0-1", "system::packages/AppProxyWar/ensure", "28.0.2-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.45-1", "system::packages/gg_lr_sis/ensure", "absent", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
			new ETD(true, "{require=Package[lr_crs_db_2.0.30.1.0], tag=appdeploy, ensure=1.5-1}", "system::packages/lr_crs_sw", "{ensure=absent, require=Package[lr_crs_db_2.0.27.0.0], tag=appdeploy}", "/hiera-ext/st/st-sit1-cor1/ext/rep.yaml"),
			new ETD(true, "28.0.2-1", "system::packages/IPTProductPresentationService/ensure", "28.0.0-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "31.0.1-1", "system::packages/IPTPresentationProductionManagementService/ensure", "30.0.0-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "31.0.1-1", "system::packages/IPTPresentationProductQueryFacade/ensure", "27.0.1-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "32.0.0-1", "system::packages/IPTPresentationRulesService/ensure", "29.0.0-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
		};
		ETD[] t9 = new ETD[]{
			new ETD(true, "2.1.646-1", "system::packages/ssb-core-features-fuse-config/ensure", "2.1.641-1", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
		};
		ETD[] t10 = new ETD[]{
			new ETD(true, "2.1.646-1", "system::packages/ssb-core-features-fuse-application/ensure", "2.1.641-1", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
			new ETD(true, "31.0.1-1", "system::packages/IPTPresentationDocumentManagementService/ensure", "25.0.1-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "31.0.0-1", "system::packages/IPTPresentationPostProductionFacade/ensure", "25.0.0-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "31.0.1-1", "system::packages/IPTPKISSManagement/ensure", "30.0.4-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "31.0.2-1", "system::packages/IPTIABSManagement/ensure", "27.0.1-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "31.0.10-1", "system::packages/IPTBRPFulfilmentManagement/ensure", "29.1.0-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "32.0.1-1", "system::packages/IPTFulfilmentManagement/ensure", "28.0.0-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "32.0.2-1", "system::packages/IPTBatchHandler/ensure", "26.2.1-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "31.0.2-1", "system::packages/IPTBTDFulfilmentManagement/ensure", "28.0.2-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "32.0.2-1", "system::packages/IPTOCFManagement/ensure", "30.0.0-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "32.0.1-1", "system::packages/CIDUpdate/ensure", "30.0.0-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
		};
		ETD[] t11 = new ETD[]{
			new ETD(true, "true", "system::services/fuse-service/hasstatus", "true", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
			new ETD(true, "false", "system::services/fuse-service/enable", "true", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
			new ETD(true, "stopped", "system::services/fuse-service/ensure", "running", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
		};
		ETD[] t12 = new ETD[]{
			new ETD(true, "true", "system::services/fuse-service/enable", "false", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
			new ETD(true, "running", "system::services/fuse-service/ensure", "stopped", "/hiera-ext/st/st-sit1-ssb1/ext/ssb.yaml"),
		};
		ETD[] t13 = new ETD[]{
			new ETD(true, "service restart service=weblogic-app", "[soatzm01 [IPT_ST_SIT1_COR1], soatzm02 [IPT_ST_SIT1_COR1]]", null, null)		
		};
		ETD[] t14 = new ETD[]{
			new ETD(10)
		};
		ETD[] t15 = new ETD[]{
			new ETD(5)
		};
		// Phase 2
		//ETD[] t0 = new ETD[]{
		ETD[] t16 = new ETD[]{
			new ETD(true, "absent", "system::packages/iabs-simulator-rpm/ensure", "0.0.753-1", "/hiera-ext/st/st-sit1-ssb1/ext/tst.yaml"),
			new ETD(true, "absent", "system::packages/ssb-simulator-rpm/ensure", "0.0.799-1", "/hiera-ext/st/st-sit1-ssb1/ext/tst.yaml"),
			new ETD(true, "absent", "system::packages/ocf-simulator-rpm/ensure", "0.0.281-1", "/hiera-ext/st/st-sit1-ssb1/ext/tst.yaml"),
			new ETD(true, "absent", "system::packages/pkiss-simulator-rpm/ensure", "0.0.814-1", "/hiera-ext/st/st-sit1-ssb1/ext/tst.yaml"),
		};
		//ETD[] t1 = new ETD[]{
		ETD[] t17 = new ETD[]{
			new ETD(true, "absent", "system::packages/ssb-common-config-rpm/ensure", "1.0.1114-1", "/hiera-ext/st/st-sit1-ssb1/ext/tst.yaml"),
		};
		//ETD[] t2 = new ETD[]{
		ETD[] t18 = new ETD[]{
			new ETD(true, "stopped", "system::services/weblogic-cdp/ensure", "running", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "stopped", "system::services/weblogic-cdp/ensure", "running", "/hiera-ext/st/st-sit1-cor1/ext/soatzm02.st-sit1-cor1.ipt.local.yaml"),
		};
		//ETD[] t3 = new ETD[]{
		ETD[] t19 = new ETD[]{
			new ETD(true, "absent", "system::packages/oer_live_sw/ensure", "1.925-1", "/hiera-ext/st/st-sit1-cor1/ext/dbstzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "absent", "system::packages/oer_stg_crs_sw/ensure", "1.90-1", "/hiera-ext/st/st-sit1-cor1/ext/dbstzm01.st-sit1-cor1.ipt.local.yaml"),
		};
		//ETD[] t4 = new ETD[]{
		ETD[] t20 = new ETD[]{
			new ETD(true, "stopped", "system::services/weblogic-cdp/ensure", "stopped", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "stopped", "system::services/weblogic-cdp/ensure", "stopped", "/hiera-ext/st/st-sit1-cor1/ext/soatzm02.st-sit1-cor1.ipt.local.yaml"),
		};
		//ETD[] t5 = new ETD[]{
		ETD[] t21 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{require=[Package[oer_live_db_2.0.31.0.1]], tag=appdeploy, ensure=1.976-1}", "system::packages/oer_live_sw", "{require=[Package[oer_live_db_2.0.30.0.0]], tag=appdeploy, ensure=absent}", "/hiera-ext/st/st-sit1-cor1/ext/dbstzm01.st-sit1-cor1.ipt.local.yaml")
		};
		//ETD[] t6 = new ETD[]{
		ETD[] t22 = new ETD[]{
			new ETD(true, "running", "system::services/weblogic-cdp/ensure", "stopped", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "running", "system::services/weblogic-cdp/ensure", "stopped", "/hiera-ext/st/st-sit1-cor1/ext/soatzm02.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "true", "system::services/weblogic-cdp/enable", "true", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "true", "system::services/weblogic-cdp/enable", "true", "/hiera-ext/st/st-sit1-cor1/ext/soatzm02.st-sit1-cor1.ipt.local.yaml"),
		};
		//ETD[] t7 = new ETD[]{
		ETD[] t23 = new ETD[]{
			new ETD(true, "{require=[Package[oer_stg_core_sw]], tag=appdeploy, ensure=1.100-1}", "system::packages/oer_stg_crs_sw", "{require=Package[oer_stg_core_sw], tag=appdeploy, ensure=absent}", "/hiera-ext/st/st-sit1-cor1/ext/dbstzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.1116-1", "system::packages/ssb-common-config-rpm/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/tst.yaml"),
			new ETD(true, "0.0.757-1", "system::packages/iabs-simulator-rpm/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/tst.yaml"),
			new ETD(true, "0.0.803-1", "system::packages/ssb-simulator-rpm/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/tst.yaml"),
			new ETD(true, "0.0.285-1", "system::packages/ocf-simulator-rpm/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/tst.yaml"),
			new ETD(true, "0.0.818-1", "system::packages/pkiss-simulator-rpm/ensure", "absent", "/hiera-ext/st/st-sit1-ssb1/ext/tst.yaml"),
		};
		ETD[] t24 = new ETD[]{
			new ETD(true, "shtop, thish deployment ish not ready yet")
		};
		// Phase 3
		//ETD[] t0 = new ETD[]{
		ETD[] t25 = new ETD[]{
			new ETD(true, "absent", "system::packages/ipt-cdp-doc-alfresco-rpm/ensure", "1.0.144-1", "/hiera-ext/st/st-sit1-cor1/ext/doc.yaml"),
		};
		//ETD[] t1 = new ETD[]{
		ETD[] t26 = new ETD[]{
			new ETD(true, "1.0.145-1", "system::packages/ipt-cdp-doc-alfresco-rpm/ensure", "absent", "/hiera-ext/st/st-sit1-cor1/ext/doc.yaml"),
		};
		//ETD[] t2 = new ETD[]{
		ETD[] t27 = new ETD[]{
			new ETD(true, "stopped", "system::services/tomcat/ensure", "running", "/hiera-ext/st/st-sit1-cor1/ext/doc.yaml"),
		};
		//ETD[] t3 = new ETD[]{
		ETD[] t28 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath	
			new ETD(true, "true", "system::services/tomcat/enable", "true", "/hiera-ext/st/st-sit1-cor1/ext/doc.yaml"),
			new ETD(true, "running", "system::services/tomcat/ensure", "stopped", "/hiera-ext/st/st-sit1-cor1/ext/doc.yaml"),
			new ETD(true, "false", "system::services/tomcat/hasstatus", "false", "/hiera-ext/st/st-sit1-cor1/ext/doc.yaml"),
			new ETD(true, "false", "system::services/tomcat/hasrestart", "false", "/hiera-ext/st/st-sit1-cor1/ext/doc.yaml"),
			new ETD(true, "su tomcat -c /opt/tomcat/bin/startup.sh", "system::services/tomcat/start", "su tomcat -c /opt/tomcat/bin/startup.sh", "/hiera-ext/st/st-sit1-cor1/ext/doc.yaml"),
			new ETD(true, "su tomcat -c /opt/tomcat/bin/shutdown.sh", "system::services/tomcat/stop", "su tomcat -c /opt/tomcat/bin/shutdown.sh", "/hiera-ext/st/st-sit1-cor1/ext/doc.yaml")
		};
		// Phase 4
		//ETD[] t0 = new ETD[]{
		ETD[] t29 = new ETD[]{
			new ETD(true, "absent", "system::packages/SOAMETADATA-Services/ensure", "1.1.37-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
		};
		//ETD[] t1 = new ETD[]{
		ETD[] t30 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath	
			new ETD(true, "1.0.49-1", "system::packages/cdp_tal_ipt_mappingdata/ensure", "1.0.43-1", "/hiera-ext/st/st-sit1-cor1/ext/etltzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.79-1", "system::packages/cdp_tal_ipt_referencedata/ensure", "1.0.73-1", "/hiera-ext/st/st-sit1-cor1/ext/etltzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.16-1", "system::packages/cdp_tal_ipt_xrfdata/ensure", "1.0.13-1", "/hiera-ext/st/st-sit1-cor1/ext/etltzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{ensure=1.1.12-1, tag=appdeploy}", "system::packages/cdp-talend-database-aud-housekeeping-rpm", null, "/hiera-ext/st/st-sit1-cor1/ext/dbstzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.1.61-1", "system::packages/EnvironmentConfiguration-Services/ensure", "1.1.57-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.1.61-1", "system::packages/EnvironmentConfiguration-Services/ensure", "1.1.57-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm02.st-sit1-cor1.ipt.local.yaml"),
		};
		//ETD[] t2 = new ETD[]{
		ETD[] t31 = new ETD[]{
			new ETD(true, "1.5.12-1", "system::packages/cdp-talend-tac-config-rpm/ensure", "1.5.9-1", "/hiera-ext/st/st-sit1-cor1/ext/etctzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.1.25-1", "system::packages/cdp-tal-midaXref-rpm/ensure", "1.1.21-1", "/hiera-ext/st/st-sit1-cor1/ext/etctzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.1.18-1", "system::packages/cdp-tal-crsdoc-rpm/ensure", "1.1.15-1", "/hiera-ext/st/st-sit1-cor1/ext/etctzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.1.17-1", "system::packages/cdp-tal-iptmap-rpm/ensure", "1.1.13-1", "/hiera-ext/st/st-sit1-cor1/ext/etctzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.1.38-1", "system::packages/cdp-tal-crsxref-rpm/ensure", "1.1.34-1", "/hiera-ext/st/st-sit1-cor1/ext/etctzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.1.4-1", "system::packages/cdp-tal-iptxrf-rpm/ensure", "1.1.3-1", "/hiera-ext/st/st-sit1-cor1/ext/etctzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.1.19-1", "system::packages/cdp-tal-iptref-rpm/ensure", "1.1.17-1", "/hiera-ext/st/st-sit1-cor1/ext/etctzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.3.11-1", "system::packages/cdp-tal-cidbtdxref-rpm/ensure", "1.3.4-1", "/hiera-ext/st/st-sit1-cor1/ext/etctzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{ensure=1.1.7-1, require=Package[cdp-talend-tac-config-rpm], tag=appdeploy}", "system::packages/cdp-tal-iptxref-rpm", null, "/hiera-ext/st/st-sit1-cor1/ext/etctzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.1.47-1", "system::packages/SOAMETADATA-Services/ensure", "absent", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.8-1, require=Package[SOAMETADATA-Services], tag=appdeploy}", "system::packages/iptbrpfulfilment-rpm", null, "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.202-1", "system::packages/addressmanagement-rpm/ensure", "1.0.197-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.168-1", "system::packages/servicedeliverymanagement-rpm/ensure", "1.0.166-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.69-1", "system::packages/referencedatamanagement-rpm/ensure", "1.0.67-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.168-1", "system::packages/systemmanagement-rpm/ensure", "1.0.165-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.121-1", "system::packages/sourcedataimportmanagement-rpm/ensure", "1.0.102-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.137-1", "system::packages/eventmanagement-rpm/ensure", "1.0.124-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.107-1", "system::packages/documentmanagement-rpm/ensure", "1.0.102-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.163-1", "system::packages/utilitymanagement-rpm/ensure", "1.0.150-1", "/hiera-ext/st/st-sit1-cor1/ext/soatzm01.st-sit1-cor1.ipt.local.yaml"),
		};
		// Phase 5
		//ETD[] t0 = new ETD[]{
		ETD[] t32 = new ETD[]{
			new ETD(true, "1.67-1", "system::packages/cdp_activeeon_scripts/ensure", "1.45-1", "/hiera-ext/st/st-sit1-cor1/ext/sch.yaml"),
			new ETD(true, "1.110-1", "system::packages/cdp_activeeon_workflows/ensure", "1.95-1", "/hiera-ext/st/st-sit1-cor1/ext/sch.yaml"),
		};
		
		String csv = "AppProxyWar,31.0.0-1,,,31.0.0,,\n"
			+ "CIDUpdate,32.0.1-1,,,32.0.1,,\n"
			+ "CommonConfigRPM,31.0.0-1,,,31.0.0,,\n"
			+ "IPTBRPFulfilmentManagement,31.0.10-1,,,31.0.10,,\n"
			+ "IPTBTDFulfilmentManagement,31.0.2-1,,,31.0.2,,\n"
			+ "IPTBatchHandler,32.0.2-1,,,32.0.2,,\n"
			+ "IPTFulfilmentManagement,32.0.1-1,,,32.0.1,,\n"
			+ "IPTIABSManagement,31.0.2-1,,,31.0.2,,\n"
			+ "IPTOCFManagement,32.0.2-1,,,32.0.2,,\n"
			+ "IPTPKISSManagement,31.0.1-1,,,31.0.1,,\n"
			+ "IPTPresentationDocumentManagementService,31.0.1-1,,,31.0.1,,\n"
			+ "IPTPresentationPostProductionFacade,31.0.0-1,,,31.0.0,,\n"
			+ "IPTPresentationProductQueryFacade,31.0.1-1,,,31.0.1,,\n"
			+ "IPTPresentationProductionManagementService,31.0.1-1,,,31.0.1,,\n"
			+ "IPTPresentationRulesService,32.0.0-1,,,32.0.0,,\n"
			+ "IPTProductPresentationService,28.0.2-1,,,28.0.2,,\n"
			+ "IPTSOACommon,32.0.4-1,,,32.0.4,,\n"
			+ "gg_lr_sis,1.45-1,,,1.45,,\n"
			+ "ipt,31.0.6-1,,,31.0.6,,\n"
			+ "lr_cid_sw_4.0,1.81-1,,,1.81,,\n"
			+ "lr_crs_conf,1.22-1,,,1.22,,\n"
			+ "lr_crs_db_2.0.30.1.0,1.8-1,,,1.8,,\n"
			+ "lr_crs_sw,1.5-1,,,1.5,,\n"
			+ "lr_sis_conf,0.0.799-1,,,0.0.799,,\n"
			+ "lr_sis_etl_cdc_4.0.28.0.0,1.3-1,,,1.3,,\n"
			+ "ssb-core-features-fuse-application,2.1.646-1,,,2.1.646,,\n"
			+ "ssb-core-features-fuse-config,2.1.646-1,,,2.1.646,,\n"
			+ "ssb-core-features-lib-nexus,2.1.646-1,,,2.1.646,,\n"
			+ "iabs-simulator-rpm,0.0.757-1,,,0.0.757,,\n"
			+ "ocf-simulator-rpm,0.0.285-1,,,0.0.285,,\n"
			+ "oer_live_sw,1.976-1,,,1.976,,\n"
			+ "oer_stg_crs_sw,1.100-1,,,1.100,,\n"
			+ "pkiss-simulator-rpm,0.0.818-1,,,0.0.818,,\n"
			+ "ssb-common-config-rpm,1.0.1116-1,,,1.0.1116,,\n"
			+ "ssb-simulator-rpm,0.0.803-1,,,0.0.803,,\n"
			+ "ipt-cdp-doc-alfresco-rpm,1.0.145-1,,,1.0.145,,\n"
			+ "EnvironmentConfiguration-Services,1.1.61-1,,,1.1.61,,\n"
			+ "SOAMETADATA-Services,1.1.47-1,,,1.1.47,,\n"
			+ "addressmanagement-rpm,1.0.202-1,,,1.0.202,,\n"
			+ "cdp-tal-cidbtdxref-rpm,1.3.11-1,,,1.3.11,,\n"
			+ "cdp-tal-crsdoc-rpm,1.1.18-1,,,1.1.18,,\n"
			+ "cdp-tal-crsxref-rpm,1.1.38-1,,,1.1.38,,\n"
			+ "cdp-tal-iptmap-rpm,1.1.17-1,,,1.1.17,,\n"
			+ "cdp-tal-iptref-rpm,1.1.19-1,,,1.1.19,,\n"
			+ "cdp-tal-iptxref-rpm,1.1.7-1,,,1.1.7,,\n"
			+ "cdp-tal-iptxrf-rpm,1.1.4-1,,,1.1.4,,\n"
			+ "cdp-tal-midaXref-rpm,1.1.25-1,,,1.1.25,,\n"
			+ "cdp-talend-database-aud-housekeeping-rpm,1.1.12-1,,,1.1.12,,\n"
			+ "cdp-talend-tac-config-rpm,1.5.12-1,,,1.5.12,,\n"
			+ "cdp_tal_ipt_mappingdata,1.0.49-1,,,1.0.49,,\n"
			+ "cdp_tal_ipt_referencedata,1.0.79-1,,,1.0.79,,\n"
			+ "cdp_tal_ipt_xrfdata,1.0.16-1,,,1.0.16,,\n"
			+ "documentmanagement-rpm,1.0.107-1,,,1.0.107,,\n"
			+ "eventmanagement-rpm,1.0.137-1,,,1.0.137,,\n"
			+ "iptbrpfulfilment-rpm,1.0.8-1,,,1.0.8,,\n"
			+ "referencedatamanagement-rpm,1.0.69-1,,,1.0.69,,\n"
			+ "servicedeliverymanagement-rpm,1.0.168-1,,,1.0.168,,\n"
			+ "sourcedataimportmanagement-rpm,1.0.121-1,,,1.0.121,,\n"
			+ "systemmanagement-rpm,1.0.168-1,,,1.0.168,,\n"
			+ "utilitymanagement-rpm,1.0.163-1,,,1.0.163,,\n"
			+ "cdp_activeeon_scripts,1.67-1,,,1.67,,\n"
			+ "cdp_activeeon_workflows,1.110-1,,,1.110,,\n";

		ChainDeploymentVerification.verify(deployment, csv, 33, new int[]{
				// Phase 1
				2,2,3,5,1,2,2,2,10,1,
				11,3,2,1,1,1,
				// Phase 2
				4,1,2,2,2,1,4,6,1,
				// Phase 3
				1,1,1,6,
				// Phase 4
				1,6,19,
				// Phase 5
				2
		}, t0,t1,t2,t3,t4,t5,t6,t7,t8,t9,t10,t11,t12,t13,t14,t15,t16,t17,t18,t19,t20,t21,t22,t23,t24,t25,t26,t27,t28,t29,t30,t31,t32);
		
		// Mockito the JschManager and always return exit code 0 from runSSHExec
		SshManager ssh = mock(SshManager.class);
		when(ssh.runSSHExec(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class))).thenReturn(0);
		when(ssh.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean())).thenReturn(new ExecReturn(0));
				
		// Mockito the GITManager - the method we are mocking commitBranchMergeToMaster() is void so no need to specify here
		EMGitManager git = mock(EMGitManager.class);
		deployment.setGitManager(git);
		
		engine.executeTransitions(deployment, new EMPuppetManager(ssh));
		
		ArgumentCaptor<Integer> jschTimeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> jschCommandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschUsernameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jschHostCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> jschPortCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<List> jschJumpHostsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<String> commitBranchArgCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Boolean> jschUnescapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		
		InOrder inOrder = inOrder(ssh, git);
		// Assert that Jsch manager and git were called in the expected order.		
		// T0
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T1
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T2
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T3
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T4
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T5
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T6
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T7
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T8
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T9
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T10
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T11
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T12
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		// Once each for T12 & T13
		inOrder.verify(ssh, times(2)).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T14
		// WAIT
		
		// T15
		// WAIT
		
		// T16
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T17
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T18
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T19
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T20
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T21
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T22
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T23
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T24
		// STOP
		
		String[] deploymentOneCommands = {
			// T0
			// JschManager.runSSHExec
			"./syncPuppetConfig.strategic.sh",
			// JschManager.runSSHExecWithOutput
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=rep))\"",
			
			// T1
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=rep))\"",
			
			// T2
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=rep))\"",
			
			// T3
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=rep or fqdn=soatzm01.st-sit1-cor1.ipt.local))\"",
			
			// T4
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=rep))\"",
			
			// T5
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-ssb1.ipt.local and (role=ssb))\"",
			
			// T6
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-ssb1.ipt.local and (role=ssb))\"",
			
			// T7
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-ssb1.ipt.local and (role=ssb))\"",
			
			// T8
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=rep or fqdn=soatzm01.st-sit1-cor1.ipt.local)) or (domain=st-sit1-ssb1.ipt.local and (role=rma))\"",
			
			// T9
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-ssb1.ipt.local and (role=ssb))\"",
			
			// T10
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=soatzm01.st-sit1-cor1.ipt.local)) or (domain=st-sit1-ssb1.ipt.local and (role=ssb))\"",
			
			// T11
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-ssb1.ipt.local and (role=ssb))\"",
			
			// T12
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-ssb1.ipt.local and (role=ssb))\"",
			
			// T13
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 service restart service=weblogic-app  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=soatzm01.st-sit1-cor1.ipt.local or fqdn=soatzm02.st-sit1-cor1.ipt.local))\"",
			
			// T14
			// WAIT
			
			// T15
			// WAIT
			
			// T16
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-ssb1.ipt.local and (role=tst))\"",
			
			// T17
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-ssb1.ipt.local and (role=tst))\"",
			
			// T18
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=soatzm01.st-sit1-cor1.ipt.local or fqdn=soatzm02.st-sit1-cor1.ipt.local))\"",
			
			// T19
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=dbstzm01.st-sit1-cor1.ipt.local))\"",
			
			// T20
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=soatzm01.st-sit1-cor1.ipt.local or fqdn=soatzm02.st-sit1-cor1.ipt.local))\"",
			
			// T21
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=dbstzm01.st-sit1-cor1.ipt.local))\"",
			
			// T22
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=soatzm01.st-sit1-cor1.ipt.local or fqdn=soatzm02.st-sit1-cor1.ipt.local))\"",
			
			// T23
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=dbstzm01.st-sit1-cor1.ipt.local)) or (domain=st-sit1-ssb1.ipt.local and (role=tst))\"",
			
			// T24
			// STOP
		};
		
		// Assert that the Jsch manager was called for each of the above commands	
		List<String> commandValues = jschCommandCaptor.getAllValues();
		assertEquals(deploymentOneCommands.length, commandValues.size());
		List<Integer> timeoutValues = jschTimeoutCaptor.getAllValues();
		assertEquals(deploymentOneCommands.length, timeoutValues.size());
		List<String> hostValues = jschHostCaptor.getAllValues();
		assertEquals(deploymentOneCommands.length, hostValues.size());
		List<Integer> portValues = jschPortCaptor.getAllValues();
		assertEquals(deploymentOneCommands.length, portValues.size());
		List<List> jumpHostsValues = jschJumpHostsCaptor.getAllValues();
		assertEquals(deploymentOneCommands.length, jumpHostsValues.size());
		List<String> usernameValues = jschUsernameCaptor.getAllValues();
		assertEquals(deploymentOneCommands.length, usernameValues.size());
		for (int i = 0; i < deploymentOneCommands.length; i++) {
			String command = commandValues.get(i);
			LOG.info(command);
			assertEquals("Command " + i, deploymentOneCommands[i], command);
			
			int timeout = timeoutValues.get(i);
			LOG.info(timeout);
			assertEquals(3000000, timeout);
			
			String host = hostValues.get(i);
			LOG.info(host);
			assertEquals(puppetHost, host);
			
			int port = portValues.get(i);
			LOG.info(port);
			assertEquals(22, port);
			
			List<HostnameUsernamePort> jumpHosts = jumpHostsValues.get(i);
			LOG.info(jumpHosts);
			assertTrue(jumpHosts.isEmpty());
			
			String username = usernameValues.get(i);
			LOG.info(username);
			assertEquals("peadmin", username);
		}
		
		// Assert that the Git method was called for the appropriate transitions with a commit string starting with 'Transition n'	
		int[] deploymentOneGitTransitions = {0,1,2,3,4,5,6,7,8,9,10,11,12,0,1,2,3,4,5,6,7};
		List<String> gitValues = commitBranchArgCaptor.getAllValues();
		assertEquals(deploymentOneGitTransitions.length, gitValues.size());
		for (int i = 0; i < deploymentOneGitTransitions.length; i++) {
			String gitArg = gitValues.get(i);
			LOG.info(gitArg);
			assertTrue(gitArg.startsWith("Transition " + deploymentOneGitTransitions[i]));
		}
		
		
		
		/**
		 * Now re-run to do the second half (after the stop)
		 */
		
		deployment = new CompositeReleaseDeployment(release, "IPT_ST_SIT1", releaseDeploymentDescriptor);
		
		engine = new CompositeReleaseDeploymentEngine();
		engine.setDeployment(deployment);
		
		deployment.setDeploymentEngine(engine);
		
		deployment.run();
		
		csv = "ipt-cdp-doc-alfresco-rpm,1.0.145-1,,,1.0.145,,\n"
			+ "EnvironmentConfiguration-Services,1.1.61-1,,,1.1.61,,\n"
			+ "SOAMETADATA-Services,1.1.47-1,,,1.1.47,,\n"
			+ "addressmanagement-rpm,1.0.202-1,,,1.0.202,,\n"
			+ "cdp-tal-cidbtdxref-rpm,1.3.11-1,,,1.3.11,,\n"
			+ "cdp-tal-crsdoc-rpm,1.1.18-1,,,1.1.18,,\n"
			+ "cdp-tal-crsxref-rpm,1.1.38-1,,,1.1.38,,\n"
			+ "cdp-tal-iptmap-rpm,1.1.17-1,,,1.1.17,,\n"
			+ "cdp-tal-iptref-rpm,1.1.19-1,,,1.1.19,,\n"
			+ "cdp-tal-iptxref-rpm,1.1.7-1,,,1.1.7,,\n"
			+ "cdp-tal-iptxrf-rpm,1.1.4-1,,,1.1.4,,\n"
			+ "cdp-tal-midaXref-rpm,1.1.25-1,,,1.1.25,,\n"
			+ "cdp-talend-database-aud-housekeeping-rpm,1.1.12-1,,,1.1.12,,\n"
			+ "cdp-talend-tac-config-rpm,1.5.12-1,,,1.5.12,,\n"
			+ "cdp_tal_ipt_mappingdata,1.0.49-1,,,1.0.49,,\n"
			+ "cdp_tal_ipt_referencedata,1.0.79-1,,,1.0.79,,\n"
			+ "cdp_tal_ipt_xrfdata,1.0.16-1,,,1.0.16,,\n"
			+ "documentmanagement-rpm,1.0.107-1,,,1.0.107,,\n"
			+ "eventmanagement-rpm,1.0.137-1,,,1.0.137,,\n"
			+ "iptbrpfulfilment-rpm,1.0.8-1,,,1.0.8,,\n"
			+ "referencedatamanagement-rpm,1.0.69-1,,,1.0.69,,\n"
			+ "servicedeliverymanagement-rpm,1.0.168-1,,,1.0.168,,\n"
			+ "sourcedataimportmanagement-rpm,1.0.121-1,,,1.0.121,,\n"
			+ "systemmanagement-rpm,1.0.168-1,,,1.0.168,,\n"
			+ "utilitymanagement-rpm,1.0.163-1,,,1.0.163,,\n"
			+ "cdp_activeeon_scripts,1.67-1,,,1.67,,\n"
			+ "cdp_activeeon_workflows,1.110-1,,,1.110,,\n";

		ChainDeploymentVerification.verify(deployment, csv, 8, new int[]{
			// Phase 3
			1,1,1,6,
			// Phase 4
			1,6,19,
			// Phase 5
			2
		}, t25,t26,t27,t28,t29,t30,t31,t32);
		
		// Mockito the JschManager and always return exit code 0 from runSSHExec
		ssh = mock(SshManager.class);
		when(ssh.runSSHExec(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class))).thenReturn(0);
		when(ssh.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean())).thenReturn(new ExecReturn(0));
				
		// Mockito the GITManager - the method we are mocking commitBranchMergeToMaster() is void so no need to specify here
		git = mock(EMGitManager.class);
		deployment.setGitManager(git);
		
		engine.executeTransitions(deployment, new EMPuppetManager(ssh));
		
		jschTimeoutCaptor = ArgumentCaptor.forClass(Integer.class);
		jschCommandCaptor = ArgumentCaptor.forClass(String.class);
		jschUsernameCaptor = ArgumentCaptor.forClass(String.class);
		jschHostCaptor = ArgumentCaptor.forClass(String.class);
		jschPortCaptor = ArgumentCaptor.forClass(Integer.class);
		jschJumpHostsCaptor = ArgumentCaptor.forClass(List.class);
		commitBranchArgCaptor = ArgumentCaptor.forClass(String.class);
		jschUnescapeCaptor = ArgumentCaptor.forClass(Boolean.class);
		
		inOrder = inOrder(ssh, git);
		
		// Assert that Jsch manager and git were called in the expected order.		
		// T25
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T26
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T27
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T28
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		// T29
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T30
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T31
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		// T32
		inOrder.verify(git).commitBranchMergeToMaster(commitBranchArgCaptor.capture());
		inOrder.verify(ssh).runSSHExec(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture());
		inOrder.verify(ssh).runSSHExecWithOutput(jschTimeoutCaptor.capture(), jschCommandCaptor.capture(), jschUsernameCaptor.capture(), jschHostCaptor.capture(), jschPortCaptor.capture(), 
				(List<HostnameUsernamePort>) jschJumpHostsCaptor.capture(), jschUnescapeCaptor.capture());
		
		String[] deploymentTwoCommands = {
			// T25
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=doc))\"",
			
			// T26
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=doc))\"",
			
			// T27
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=doc))\"",
			
			// T28
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=doc))\"",
			
			// T29
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=soatzm01.st-sit1-cor1.ipt.local))\"",
			
			// T30
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=dbstzm01.st-sit1-cor1.ipt.local or fqdn=etltzm01.st-sit1-cor1.ipt.local or fqdn=soatzm01.st-sit1-cor1.ipt.local or fqdn=soatzm02.st-sit1-cor1.ipt.local))\"",
			
			// T31
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (fqdn=etctzm01.st-sit1-cor1.ipt.local or fqdn=soatzm01.st-sit1-cor1.ipt.local))\"",
			
			// T32
			"./syncPuppetConfig.strategic.sh",
			"sudo -u peadmin /opt/puppet/bin/mco rpc -t 3000000 gonzo check  -S \"(domain=st-sit1-cor1.ipt.local and (role=sch))\"",
		};
		
		// Assert that the Jsch manager was called for each of the above commands	
		commandValues = jschCommandCaptor.getAllValues();
		assertEquals(deploymentTwoCommands.length, commandValues.size());
		timeoutValues = jschTimeoutCaptor.getAllValues();
		assertEquals(deploymentTwoCommands.length, timeoutValues.size());
		hostValues = jschHostCaptor.getAllValues();
		assertEquals(deploymentTwoCommands.length, hostValues.size());
		portValues = jschPortCaptor.getAllValues();
		assertEquals(deploymentTwoCommands.length, portValues.size());
		jumpHostsValues = jschJumpHostsCaptor.getAllValues();
		assertEquals(deploymentTwoCommands.length, jumpHostsValues.size());
		usernameValues = jschUsernameCaptor.getAllValues();
		assertEquals(deploymentTwoCommands.length, usernameValues.size());
		for (int i = 0; i < deploymentTwoCommands.length; i++) {
			String command = commandValues.get(i);
			LOG.info(command);
			assertEquals("Command " + i, deploymentTwoCommands[i], command);
			
			int timeout = timeoutValues.get(i);
			LOG.info(timeout);
			assertEquals(3000000, timeout);
			
			String host = hostValues.get(i);
			LOG.info(host);
			assertEquals(puppetHost, host);
			
			int port = portValues.get(i);
			LOG.info(port);
			assertEquals(22, port);
			
			List<HostnameUsernamePort> jumpHosts = jumpHostsValues.get(i);
			LOG.info(jumpHosts);
			assertTrue(jumpHosts.isEmpty());
			
			String username = usernameValues.get(i);
			LOG.info(username);
			assertEquals("peadmin", username);
		}
		
		// Assert that the Git method was called for the appropriate transitions with a commit string starting with 'Transition n'	
		int[] deploymentTwoGitTransitions = {0,1,2,3,0,1,2,0};
		gitValues = commitBranchArgCaptor.getAllValues();
		assertEquals(deploymentTwoGitTransitions.length, gitValues.size());
		for (int i = 0; i < deploymentTwoGitTransitions.length; i++) {
			String gitArg = gitValues.get(i);
			LOG.info(gitArg);
			assertTrue(gitArg.startsWith("Transition " + deploymentTwoGitTransitions[i]));
		}
	}
	
	@Test
	public void testSITSReport() throws Exception {
		// Set some props before we start
		String puppetHost = "puppetmaster.host";
		ReleaseVersion release = new ReleaseVersion();
		release.setName("Release name");
		release.setVersion("1.0.1");
		String releaseDeploymentDescriptor = "SITS.xml";
		CompositeReleaseDeployment deployment = createSITSDeployment(puppetHost, release, releaseDeploymentDescriptor);
		
		CompositeReleaseDeploymentEngine engine = new CompositeReleaseDeploymentEngine();
		engine.setDeployment(deployment);
		
		deployment.setDeploymentEngine(engine);
		
		deployment.run();
		
		List<Transition> transitions = deployment.getTransitions();
		assertEquals(33, transitions.size());
		
		// Mockito the JschManager and always return exit code 0 from runSSHExec
		SshManager ssh = mock(SshManager.class);
		when(ssh.runSSHExec(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class))).thenReturn(0);
		when(ssh.runSSHExecWithOutput(anyInt(), anyString(), anyString(), anyString(), anyInt(), anyListOf(HostnameUsernamePort.class), anyBoolean())).thenReturn(new ExecReturn(0));
		
		// Mockito the GITManager - the method we are mocking commitBranchMergeToMaster() is void so no need to specify here
		EMGitManager git = mock(EMGitManager.class);
		deployment.setGitManager(git);
		
		final int transitionToFail = 15;
		//Mockito the EMPuppetManager - we want to make the doPuppetRun method fail!
		EMPuppetManager puppetManager = new EMPuppetManager(ssh) {
			private int callCount = 0;
			@Override
			public ExecReturn doPuppetRunWithRetry(Organisation organisation, Map<String, Set<String>> zoneRolesOrHosts, int maxRetryCount, int retryDelaySeconds) {
				callCount++;
				if (callCount == transitionToFail+1) {
					//Fail transition
					ExecReturn ret = new ExecReturn(1);
					ret.setStdOut("Log for error in the puppet run\r\nThis should be a new line.\r\nAs should this.");
					return ret;
				}
				return new ExecReturn(0);
			}
		};
		
		Exception e = null;
		
		try {
			engine.executeTransitions(deployment, puppetManager);
		} catch (Exception e1) {
			e = e1;
		}
		
		assertTrue(e != null);
		int numTransitionsWithUpdates = 0;
		for (int i = 0; i < deployment.getTransitions().size(); i++){
			String message = String.format("Transition %s (or %s with updates)", i, numTransitionsWithUpdates);
			Transition t = deployment.getTransitions().get(i);
			
			if (numTransitionsWithUpdates < transitionToFail) {
				assertNull(message, t.getException());
				assertNull(message, t.getStatusMessage());
				assertEquals(message, DeploymentStatus.COMPLETED, t.getStatus());
			} else if (numTransitionsWithUpdates == transitionToFail) {
				assertNotNull(message, t.getException());
				assertNotNull(message, t.getStatusMessage());
				assertEquals(message, DeploymentStatus.ERRORED, t.getStatus());
			} else {
				assertNull(message, t.getException());
				assertNull(message, t.getStatusMessage());
				assertEquals(message, DeploymentStatus.NOT_STARTED, t.getStatus());
			}
			
			if (t.getUpdates() != null && t.getUpdates().size() > 0) {
				numTransitionsWithUpdates++;
			}
		}
		
		
		/**
		 * Now re-run to do the second half (after the stop)
		 */
		/*
		deployment = new CompositeReleaseDeployment(release, "IPT_ST_SIT1", releaseDeploymentDescriptor);
		
		engine = new CompositeReleaseDeploymentEngine();
		engine.setDeployment(deployment);
		
		deployment.setDeploymentEngine(engine);
		
		deployment.run();*/
	}

	private CompositeReleaseDeployment createSITSDeployment(String puppetHost,
			ReleaseVersion release, String releaseDeploymentDescriptor)
			throws Exception {
		// Set Puppet Master port to empty so the default is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_PORT, "");
		// Set username otherwise 'localhost' is used
		ConfigurationFactory.getProperties().put(Configuration.PUPPET_MASTER_UPDATE_LOGIN_USERNAME, "peadmin");
		
		ConfigurationFactory.getProperties().put("st.puppet.master.host", puppetHost);
		ConfigurationFactory.getProperties().put(Configuration.ENABLE_MCO, "true");
		
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARE_ONLY, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX, "st");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_COMPOSITE_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.GIT_LOCAL_CHECKOUT_DIR, TEMP_TEST_FILES);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER, TEMP_TEST_FILES);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_CREATE_HIERA, "false");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER, "target/reports/ss3/sits");
		ConfigurationFactory.getProperties().put("st.rpm.failfile.report.enabled", "false");
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR);
		
		// CDPLR_CID 1494
		List<ApplicationVersion> applicationVersions = new ArrayList<>();
		ApplicationVersion applicationVersion = buildApplicationVersion("CDPLR_CID", "31.0.0", new String[][] {
				new String[]{"lr_cid_sw_4.0", "1.81"}
				});				
		applicationVersions.add(applicationVersion);
		
		// CDPLR_SIS 1487
		applicationVersion = buildApplicationVersion("CDPLR_SIS", "29.2.4", new String[][] {
				new String[]{"lr_sis_etl_cdc_4.0.28.0.0", "1.3"},
				new String[]{"lr_sis_conf", "0.0.799"},
				new String[]{"gg_lr_sis", "1.45"},
				});		
		applicationVersions.add(applicationVersion);
		
		// APP 1485
		applicationVersion = buildApplicationVersion("APP", "32.0.4", new String[][] {
				new String[]{"IPTImageTransformation", "19.0.2"},
				new String[]{"IPTCommonLibraries", "19.0.2"},
				new String[]{"IPTCommonExceptionHandlerUI", "21.0.0"},
				new String[]{"IPTAPPSOASecurityConfigRPM", "1.0.16"},
				new String[]{"IPTXPATHLogger", "24.0.1"},
				new String[]{"IPTPresentationPartyManagementService", "25.0.1"},
				new String[]{"IPTCommonExceptionHandler", "25.0.1"},
				new String[]{"IPTRules", "25.1.0"},
				new String[]{"IPTAppHealthCheck", "26.2.0"},
				new String[]{"APPLoadbalnacer", "27.0.6"},
				new String[]{"IABSServiceManager", "27.0.1"},
				new String[]{"IPTPolicyConfig", "28.0.0"},
				new String[]{"IPTCDPServiceBroker", "29.0.1"},
				new String[]{"PKISSServices", "29.0.0"},
				new String[]{"IPTOCFBroker", "30.0.0"},
				new String[]{"CIDServiceManager", "30.0.0"},
				new String[]{"IPTPresentationPreProductionFacade", "30.0.0"},
				new String[]{"CommonConfigRPM", "31.0.0"},
				new String[]{"IPTPresentationPostProductionFacade", "31.0.0"},
				new String[]{"IPTPresentationProductQueryFacade", "31.0.1"},
				new String[]{"IPTPresentationDocumentManagementService", "31.0.1"},
				new String[]{"IPTPresentationProductionManagementService", "31.0.1"},
				new String[]{"AppProxyWar", "31.0.0"},
				new String[]{"IPTPKISSManagement", "31.0.1"},
				new String[]{"CIDUpdate", "32.0.1"},
				new String[]{"IPTBRPFulfilmentManagement", "31.0.10"},
				new String[]{"IPTPresentationRulesService", "32.0.0"},
				new String[]{"IPTProductPresentationService", "28.0.2"},
				new String[]{"IPTFulfilmentManagement", "32.0.1"},
				new String[]{"IPTBatchHandler", "32.0.2"},
				new String[]{"IPTOCFManagement", "32.0.2"},
				new String[]{"IPTSOACommon", "32.0.4"},
				new String[]{"ipt", "31.0.6"},
				new String[]{"IPTIABSManagement", "31.0.2"},
				new String[]{"IPTBTDFulfilmentManagement", "31.0.2"}
				});		
		applicationVersions.add(applicationVersion);
		
		// SSB 1347
		applicationVersion = buildApplicationVersion("SSB", "1.0.0", new String[][] {
				new String[]{"ssb-db-schema", "1.376.355"},
				new String[]{"ssb-ldap-schema", "1.166"},
				new String[]{"ssb-rpm-nexus-baseline-config", "2.0.44"},
				new String[]{"ssb-core-features-lib-nexus", "2.1.646"},
				new String[]{"ssb-core-features-fuse-config", "2.1.646"},
				new String[]{"ssb-core-features-fuse-application", "2.1.646"},
		});		
		applicationVersions.add(applicationVersion);
		
		// CDPSYSOBJ 354
		applicationVersion = buildApplicationVersion("CDPSYSOBJ", "1.0.1", new String[][] {
				new String[]{"oer_live_sys_obj", "1.12"},
				new String[]{"oer_live_sys_cfg", "1.16"},
		});		
		applicationVersions.add(applicationVersion);
		
		// CDPLR_CRS 1272
		applicationVersion = buildApplicationVersion("CDPLR_CRS", "30.1.0", new String[][] {
				new String[]{"lr_crs_conf", "1.22"},
				new String[]{"lr_crs_sw", "1.5"},
				new String[]{"lr_crs_db_2.0.30.1.0", "1.8"},
		});		
		applicationVersions.add(applicationVersion);
		
		// CDPOER 1501
		applicationVersion = buildApplicationVersion("CDPOER", "31.4.1", new String[][] {
				new String[]{"oer_live_sw", "1.976"},
				new String[]{"oer_stg_crs_sw", "1.100"},
		});		
		applicationVersions.add(applicationVersion);
		
		// SSBSIM 1350
		applicationVersion = buildApplicationVersion("SSBSIM", "1.0.1", new String[][] {
				new String[]{"ssb-sim-master-data-ta-rpm", "1.0.48"},
				new String[]{"ocf-simulator-rpm", "0.0.285"},
				new String[]{"pkiss-simulator-rpm", "0.0.818"},
				new String[]{"ssb-simulator-rpm", "0.0.803"},
				new String[]{"iabs-simulator-rpm", "0.0.757"},
				new String[]{"ssb-common-config-rpm", "1.0.1116"},
		});		
		applicationVersions.add(applicationVersion);
		
		// DOC 1400
		applicationVersion = buildApplicationVersion("DOC", "22.0.0", new String[][] {
				new String[]{"alfresco_conf", "1.13"},
				new String[]{"ipt-cdp-doc-alfresco-rpm", "1.0.145"},
		});		
		applicationVersions.add(applicationVersion);
		
		// CDPSOA 1464
		applicationVersion = buildApplicationVersion("CDPSOA", "6.31.0", new String[][] {
				new String[]{"cdp-soa-classpath-rpm", "1.0.46"},
				new String[]{"cdp-soa-security-config-rpm", "1.0.43"},
				new String[]{"cdp-soa-local-optimization-rpm", "1.0.9"},
				new String[]{"processmanagement-rpm", "1.0.87"},
				new String[]{"partymanagement-rpm", "1.0.157"},
				new String[]{"biometricmanagement-rpm", "1.0.36"},
				new String[]{"searchmanagement-rpm", "1.0.82"},
				new String[]{"cdp-soa-audit-logging-config-rpm", "1.0.19"},
				new String[]{"etlbatchmanagement-rpm", "1.0.67"},
				new String[]{"imagemanagement-rpm", "1.0.67"},
				new String[]{"systemmanagement-rpm", "1.0.168"},
				new String[]{"addressmanagement-rpm", "1.0.202"},
				new String[]{"EnvironmentConfiguration-Services", "1.1.61"},
				new String[]{"iptbrpfulfilment-rpm", "1.0.8"},
				new String[]{"referencedatamanagement-rpm", "1.0.69"},
				new String[]{"eventmanagement-rpm", "1.0.137"},
				new String[]{"servicedeliverymanagement-rpm", "1.0.168"},
				new String[]{"sourcedataimportmanagement-rpm", "1.0.121"},
				new String[]{"documentmanagement-rpm", "1.0.107"},
				new String[]{"SOAMETADATA-Services", "1.1.47"},
				new String[]{"utilitymanagement-rpm", "1.0.163"},
		});		
		applicationVersions.add(applicationVersion);
		
		// CDPTAL 1471
		applicationVersion = buildApplicationVersion("CDPTAL", "31.2.1", new String[][] {
				new String[]{"cdp-talend-context-dbschema-rpm", "1.1.0"},
				new String[]{"cdp-tal-crssd-rpm", "1.1.7"},
				new String[]{"cdp-tal-crsaddr-rpm", "1.1.24"},
				new String[]{"cdp-talend-jobserver-config-rpm", "1.3.3"},
				new String[]{"cdp-tal-crspty-rpm", "1.1.12"},
				new String[]{"cdp-tal-crsevt-rpm", "1.1.32"},
				new String[]{"cdp-tal-btdmiXref-rpm", "1.1.23"},
				new String[]{"cdp-tal-bidmuXref-rpm", "1.1.16"},
				new String[]{"cdp-talend-database-aud-housekeeping-rpm", "1.1.12"},
				new String[]{"cdp-talend-tac-config-rpm", "1.5.12"},
				new String[]{"cdp-tal-midaXref-rpm", "1.1.25"},
				new String[]{"cdp-tal-crsxref-rpm", "1.1.38"},
				new String[]{"cdp-tal-cidbtdxref-rpm", "1.3.11"},
				new String[]{"cdp-tal-iptref-rpm", "1.1.19"},
				new String[]{"cdp-tal-iptxref-rpm", "1.1.7"},
				new String[]{"cdp-tal-iptxrf-rpm", "1.1.4"},
				new String[]{"cdp-tal-crsdoc-rpm", "1.1.18"},
				new String[]{"cdp-tal-iptmap-rpm", "1.1.17"},
		});		
		applicationVersions.add(applicationVersion);
		
		// CDPTAL_DATA 1473
		applicationVersion = buildApplicationVersion("CDPTAL_DATA", "31.2.1", new String[][] {
				new String[]{"cdp_tal_ipt_mappingdata", "1.0.49"},
				new String[]{"cdp_tal_ipt_referencedata", "1.0.79"},
		});		
		applicationVersions.add(applicationVersion);
		
		// CDPTAL_XRFDATA 1434
		applicationVersion = buildApplicationVersion("CDPTAL_XRFDATA", "31.1.1", new String[][] {
				new String[]{"cdp_tal_ipt_xrfdata", "1.0.16"},
		});		
		applicationVersions.add(applicationVersion);
		
		// CDPETL_SERVICEMGMT 1121
		applicationVersion = buildApplicationVersion("CDPETL_SERVICEMGMT", "29.2.0", new String[][] {
				new String[]{"gen-ins-servicemanagement-cdpetloer", "1.0.49"},
		});		
		applicationVersions.add(applicationVersion);
		
		// CDPACT 1483
		applicationVersion = buildApplicationVersion("CDPACT", "31.3.0", new String[][] {
				new String[]{"cdp_activeeon_scripts", "1.67"},
				new String[]{"cdp_activeeon_workflows", "1.110"},
		});		
		applicationVersions.add(applicationVersion);
		
		release.setApplicationVersions(applicationVersions);
		
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(release, "IPT_ST_SIT1", releaseDeploymentDescriptor);
		return deployment;
	}

	private ApplicationVersion buildApplicationVersion(String shortName, String version, String[][] componentNamesVersions) {
		ApplicationVersion applicationVersion = new ApplicationVersion();
		
		Application application = new Application();
		application.setShortName(shortName);
		applicationVersion.setApplication(application);
		applicationVersion.setVersion(version);
		application.setName("Long version of:" + shortName);
		
		List<ComponentVersion> components = new ArrayList<>();
		for (int i = 0; i < componentNamesVersions.length; i++) {
			ComponentVersion componentVersion = buildComponentVersion(i, componentNamesVersions[i][0], componentNamesVersions[i][1], application);
			components.add(componentVersion);
		}
		applicationVersion.setComponents(components);
		return applicationVersion;
	}

	private ComponentVersion buildComponentVersion(long id, String name, String version, Application application) {
		ComponentVersion componentVersion = new ComponentVersion();
		componentVersion.setId(id);
		componentVersion.setName(name);
		componentVersion.setDateOfRelease(new Date());
		componentVersion.setApplication(application);
		componentVersion.setGroupId("");
		componentVersion.setArtifactId("");
		componentVersion.setComponentVersion(version);
		componentVersion.setClassifier("");
		componentVersion.setPackaging("");
		componentVersion.setRpmPackageName(name);
		componentVersion.setRpmPackageVersion(version + "-1");
		componentVersion.setType("");
		componentVersion.setNotes("");
		return componentVersion;
	}
	
}
