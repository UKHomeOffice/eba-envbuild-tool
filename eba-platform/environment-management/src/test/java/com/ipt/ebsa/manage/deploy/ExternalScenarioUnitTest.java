package com.ipt.ebsa.manage.deploy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.hiera.HieraData;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.data.ETD;
import com.ipt.ebsa.manage.deploy.database.DBTest;
import com.ipt.ebsa.manage.deploy.impl.report.ApplicationReport;
import com.ipt.ebsa.manage.test.TestHelper;
import com.ipt.ebsa.util.OrgEnvUtil;

/**
 * This is added for testing specific scenarios which come up using the test harness. 
 * 
 * @author scowx
 */
public class ExternalScenarioUnitTest extends DBTest {

	public static final String CONFIG_PARAM_NEW_VERSION = ".newVersion";
	public static final String CONFIG_PARAM_NEW_PACKAGE_NAMES = "packageNames";
	public static final String SCENARIOS_BASE_FOLDER = "src/test/resources/scenarios/";

	@Before
	public void setup() {
		ConfigurationFactory.getProperties().setProperty("st.yum.repo.update.enabled", "true");
		ConfigurationFactory.getProperties().setProperty("np.yum.repo.update.enabled", "true");
		ConfigurationFactory.getProperties().setProperty("deployment.config.alternativeEnvironmentState.enabled", "false");
	}
	
	/**
	 * For some reason ipt-iias-tomcat-cfg-rpm was not deploying to tstem06 even though it
	 * was not present before the SS2 run. This caused puppet to fail, as something that
	 * depends on it could not be deployed.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	@Test
	public void ebsad18105MissingDeploy() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-18105-missing-deploy");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "1.0.12-1", "system::packages/gen-bin-tomcat8/ensure", "absent", "/np/np-prp1-eszo/tstem05.np-prp1-eszo.ipt.ho.local.yaml"),
			new ETD(true, "0.0.11-1", "system::packages/ipt-iias-tomcat-cfg-rpm/ensure", "absent", "/np/np-prp1-eszo/tstem05.np-prp1-eszo.ipt.ho.local.yaml"),
			new ETD(true, "{ensure=0.0.11-1, require=Package[gen-bin-tomcat8], tag=appdeploy}", "system::packages/ipt-iias-tomcat-cfg-rpm", null, "/np/np-prp1-eszo/tstem06.np-prp1-eszo.ipt.ho.local.yaml"),
			new ETD(true, "{ensure=0.0.3-CI, require=Package[ipt-iias-tomcat-cfg-rpm], tag=appdeploy}", "system::packages/ipt-brp-simulator-rpm", null, "/np/np-prp1-eszo/tstem06.np-prp1-eszo.ipt.ho.local.yaml"),
			new ETD(true, "0.0.14-CI", "system::packages/ipt-qmatic-simulator-rpm/ensure", "absent", "/np/np-prp1-eszo/tstem05.np-prp1-eszo.ipt.ho.local.yaml"),
		};
			
		String csv = "gen-bin-tomcat8,1.0.12-1,groupid,gen-bin-tomcat8,1.0.12,war,\n"
				+ "ipt-brp-simulator-rpm,0.0.3-CI,groupid,ipt-brp-simulator-rpm,0.0.3-CI,war,\n"
				+ "ipt-iias-tomcat-cfg-rpm,0.0.11-1,groupid,ipt-iias-tomcat-cfg-rpm,0.0.11,war,\n"
				+ "ipt-qmatic-simulator-rpm,0.0.14-CI,groupid,ipt-qmatic-simulator-rpm,0.0.14-CI,war,\n";

		ChainDeploymentVerification.verify(dep, csv, 1, new int[]{5}, t0);
			
	}
	
	/**
	 * This test is now for EBSAD-11919. See the hiera-working dir for heira data that does not
	 * trigger the NPE.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	@Test
	public void ebsad11883EnsureNull() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11883-ensure-null");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/gg_lr_crs/ensure", "1.34-1", "/np/np-prp2-dazo/dbsem31.np-prp2-dazo.ipt.ho.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/gg_lr_crs/ensure", "1.34-1", "/np/np-prp2-dazo/dbsem32.np-prp2-dazo.ipt.ho.local.yaml"),
		};
		ETD[] t1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/lr_crs_conf/ensure", "1.9-1", "/np/np-prp2-dazo/dbsem31.np-prp2-dazo.ipt.ho.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/lr_crs_conf/ensure", "1.9-1", "/np/np-prp2-dazo/dbsem32.np-prp2-dazo.ipt.ho.local.yaml"),
		};
		ETD[] t2 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{tag=appdeploy, ensure=1.13-1}", "system::packages/lr_crs_conf", "{ensure=absent, require=Package[cdp_cfg_db_CRSR], tag=mwconfig}", "/np/np-prp2-dazo/dbsem31.np-prp2-dazo.ipt.ho.local.yaml"),
			new ETD(true, "{ensure=1.3-1, require=Package[lr_crs_conf], tag=appdeploy}", "system::packages/lr_crs_21.0", null, "/np/np-prp2-dazo/dbsem31.np-prp2-dazo.ipt.ho.local.yaml"),
			new ETD(true, "{require=Package[lr_crs_21.0], tag=appdeploy, ensure=1.39-1}", "system::packages/gg_lr_crs", "{ensure=absent, require=Package[lr_crs_18.0], tag=mwconfig}", "/np/np-prp2-dazo/dbsem31.np-prp2-dazo.ipt.ho.local.yaml"),
		};
		
		String csv = "gg_lr_crs,1.39-1,groupid,gg_lr_crs,1.39,war,\n"
						+ "lr_crs_21.0,1.3-1,groupid,lr_crs_21.0,1.3,war,\n"
						+ "lr_crs_conf,1.13-1,groupid,lr_crs_conf,1.13,war,\n";

		ChainDeploymentVerification.verify(dep, csv, 3, new int[]{2, 2, 3}, t0, t1, t2);
	}
	
	/**
	 * This is to test the setting to absent of components that are already absent.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	@Test
	public void ebsad10970AbsentAbsent() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-10970-absent-absent");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/cdp-soa-classpath-rpm/ensure", "1.0.50-1", "/st/st-dev1-ebs2/soatzm02.st-dev1-ebs2.ipt.local.yaml"),
	    };
		ETD[] t1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/EnvironmentConfiguration-Services/ensure", "1.0.155-1", "/st/st-dev1-ebs2/soatzm01.st-dev1-ebs2.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/EnvironmentConfiguration-Services/ensure", "1.0.150-1", "/st/st-dev1-ebs2/soatzm02.st-dev1-ebs2.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/cdp-soa-audit-logging-config-rpm/ensure", "1.0.13-1", "/st/st-dev1-ebs2/soatzm01.st-dev1-ebs2.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/cdp-soa-audit-logging-config-rpm/ensure", "1.0.13-1", "/st/st-dev1-ebs2/soatzm02.st-dev1-ebs2.ipt.local.yaml"),
	    };
			
		ChainDeploymentVerification.verify(dep, null, 2, new int[]{1,4}, t0, t1);
	}
	
	/**
	 * Should not throw a null pointer exception.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	@Test
	public void ebsad12036Npe() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12036-NPE");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/iabs-simulator-rpm/ensure", "0.0.609-1", "/np/np-prp2-eszo/tst.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/pkiss-simulator-rpm/ensure", "0.0.649-1", "/np/np-prp2-eszo/tst.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/ssb-simulator-rpm/ensure", "0.0.638-1", "/np/np-prp2-eszo/tst.yaml"),
		};
		ETD[] t1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/ssb-common-config-rpm/ensure", "1.0.572-1", "/np/np-prp2-eszo/tst.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/ssb-sim-common-rpm/ensure", "1.0.1020-1", "/np/np-prp2-eszo/tst.yaml"),
		};
		ETD[] t2 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "1.0.708-1", "system::packages/ssb-common-config-rpm/ensure", HieraData.ABSENT, "/np/np-prp2-eszo/tst.yaml"),
			new ETD(true, "{ensure=0.0.139-1, require=Package[ssb-common-config-rpm]}", "system::packages/ocf-simulator-rpm", null, "/np/np-prp2-eszo/tst.yaml"),
			new ETD(true, "{require=Package[ssb-common-config-rpm], ensure=0.0.625-1}", "system::packages/iabs-simulator-rpm", "{ensure=absent, require=[Package[ssb_cfg_tomcat], Package[ssb-common-config-rpm], Package[ssb-common-ssl-rpm], Package[ssb-sim-common-rpm], Package[ssb-simulator-rpm]], tag=appdeploy}", "/np/np-prp2-eszo/tst.yaml"),
		};
		String csv = "iabs-simulator-rpm,0.0.625-1,groupid,iabs-simulator-rpm,0.0.625-1,war,\n"
				+ "ocf-simulator-rpm,0.0.139-1,groupid,ocf-simulator-rpm,0.0.139-1,war,\n"
				+ "ssb-common-config-rpm,1.0.708-1,groupid,ssb-common-config-rpm,1.0.708-1,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 3, new int[]{3, 2, 3}, t0, t1, t2);
	}
	
	/**
	 * When changing the version on a component, update any other yaml changes.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	@Test
	public void ebsad11646UpdateYaml() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11646-update-yaml");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] tran1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{tag=appdeploy, ensure=1.0.135-1}", "system::packages/forumsentry-common", "{ensure=1.0.135-1, tag=mwdeploy}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-common], tag=appdeploy, ensure=1.0.135-1}", "system::packages/forumsentry-installer", "{ensure=1.0.135-1, require=Package[forumsentry-common], tag=mwconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-installer], tag=appdeploy, ensure=1.0.128-1}", "system::packages/forumsentry-core", "{ensure=1.0.128-1, require=Package[forumsentry-installer], tag=mwconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "1.0.111-1", "system::packages/forumsentry-policy-ssb-brp/ensure", HieraData.ABSENT, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-brp], tag=appdeploy, ensure=1.0.87-1}", "system::packages/forumsentry-config-ssb-brp", "{ensure=absent}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-iabs], tag=appdeploy, ensure=1.0.90-1}", "system::packages/forumsentry-config-ssb-iabs", "{ensure=1.0.90-1, require=Package[forumsentry-policy-ssb-iabs], tag=appconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-pkiss], tag=appdeploy, ensure=1.0.92-1}", "system::packages/forumsentry-config-ssb-pkiss", "{ensure=1.0.92-1, require=Package[forumsentry-policy-ssb-pkiss], tag=appconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		String csv = "forumsentry-config-ssb-brp,1.0.87-1,groupid,forumsentry-config-ssb-brp,1.0.87,war,\n"
				+ "forumsentry-policy-ssb-brp,1.0.111-1,groupid,forumsentry-policy-ssb-brp,1.0.111,war,\n";

		ChainDeploymentVerification.verify(dep, csv, 1, new int[]{7}, tran1);
	}
	
	/**
	 * while fixing EBSAD-10113 not all cases were covered. If the change is a deploy, then the tool
	 * would still try to install on all roles/hosts that have yaml for the component. This tests that
	 * that is not happening.
	 * 
	 * @throws Exception 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Test
	public void ebsad11063HostnameDeploy() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD_11063_hostname_deploy");
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] tran0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{tag=appdeploy, ensure=1.0.140-1}", "system::packages/forumsentry-common", "{ensure=1.0.140-1, tag=mwdeploy}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-common], tag=appdeploy, ensure=1.0.140-1}", "system::packages/forumsentry-installer", "{ensure=1.0.140-1, require=Package[forumsentry-common], tag=mwconfig}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-installer], tag=appdeploy, ensure=1.0.131-1}", "system::packages/forumsentry-core", "{ensure=1.0.131-1, require=Package[forumsentry-installer], tag=mwconfig}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-brp], tag=appdeploy, ensure=1.0.90-1}", "system::packages/forumsentry-config-ssb-brp", "{ensure=absent, require=Package[forumsentry-policy-ssb-brp], tag=appconfig}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-iabs], tag=appdeploy, ensure=1.0.95-1}", "system::packages/forumsentry-config-ssb-iabs", "{ensure=1.0.95-1, require=Package[forumsentry-policy-ssb-iabs], tag=appconfig}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-pkiss], tag=appdeploy, ensure=1.0.87-1}", "system::packages/forumsentry-config-ssb-pkiss", "{ensure=1.0.87-1, require=Package[forumsentry-policy-ssb-pkiss], tag=appconfig}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
		};
		
		String csv = "forumsentry-config-ssb-brp,1.0.90-1,groupid,forumsentry-config-ssb-brp,1.0.90,war,\n";

		ChainDeploymentVerification.verify(dep, csv, 1, new int[]{6}, tran0);
	}
	
	/**
	 * Missing YAML is not inserted. This checks that YAML gets inserted where there is already YAML in hiera
	 * for the component, but in a different YAML file to the one mentioned in the deployment descriptor, and
	 * in the YAML
	 * 
	 * @throws Exception 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Test
	public void ebsad11820InsertYaml() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11820-insert-yaml");
		
		Deployer d = new Deployer();
		ApplicationDeployment deploy = d.deploy(appVersion, createEnvironmentName(), null);

		ETD[] t0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-config-ssb-brp/ensure", "1.0.90-1", "/st/st-cit1-cdp2/soa.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-config-ssb-iabs/ensure", "1.0.95-1", "/st/st-cit1-cdp2/soa.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-config-ssb-pkiss/ensure", "1.0.87-1", "/st/st-cit1-cdp2/soa.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-policy-ssb-brp/ensure", "1.0.117-1", "/st/st-cit1-cdp2/soa.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-policy-ssb-iabs/ensure", "1.0.108-1", "/st/st-cit1-cdp2/soa.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-policy-ssb-pkiss/ensure", "1.0.107-1", "/st/st-cit1-cdp2/soa.yaml"),
			};
		ETD[] t1 = new ETD[] {new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-core/ensure", "1.0.131-1", "/st/st-cit1-cdp2/soa.yaml")};
		ETD[] t2 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-installer/ensure", "1.0.140-1", "/st/st-cit1-cdp2/soa.yaml"),
			
		};
		ETD[] t3 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-common/ensure", "1.0.140-1", "/st/st-cit1-cdp2/soa.yaml"),
		};
		ETD[] t4 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{ensure=1.0.140-1, tag=appdeploy}", "system::packages/forumsentry-common", null, "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.140-1, require=Package[forumsentry-common], tag=appdeploy}", "system::packages/forumsentry-installer", null, "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.131-1, require=Package[forumsentry-installer], tag=appdeploy}", "system::packages/forumsentry-core", null, "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.117-1, require=Package[forumsentry-core], tag=appdeploy}", "system::packages/forumsentry-policy-ssb-brp", null, "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.109-1, require=Package[forumsentry-core], tag=appdeploy}", "system::packages/forumsentry-policy-ssb-iabs", null, "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.107-1, require=Package[forumsentry-core], tag=appdeploy}", "system::packages/forumsentry-policy-ssb-pkiss", null, "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.90-1, require=Package[forumsentry-policy-ssb-brp], tag=appdeploy}", "system::packages/forumsentry-config-ssb-brp", null, "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.95-1, require=Package[forumsentry-policy-ssb-iabs], tag=appdeploy}", "system::packages/forumsentry-config-ssb-iabs", null, "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.87-1, require=Package[forumsentry-policy-ssb-pkiss], tag=appdeploy}", "system::packages/forumsentry-config-ssb-pkiss", null, "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
		};
		String csv = "forumsentry-common,1.0.140-1,groupid,forumsentry-common,1.0.140,war,\n"
			+ "forumsentry-config-ssb-brp,1.0.90-1,groupid,forumsentry-config-ssb-brp,1.0.90,war,\n"
			+ "forumsentry-config-ssb-iabs,1.0.95-1,groupid,forumsentry-config-ssb-iabs,1.0.95,war,\n"
			+ "forumsentry-config-ssb-pkiss,1.0.87-1,groupid,forumsentry-config-ssb-pkiss,1.0.87,war,\n"
			+ "forumsentry-core,1.0.131-1,groupid,forumsentry-core,1.0.131,war,\n"
			+ "forumsentry-installer,1.0.140-1,groupid,forumsentry-installer,1.0.140,war,\n"
			+ "forumsentry-policy-ssb-brp,1.0.117-1,groupid,forumsentry-policy-ssb-brp,1.0.117,war,\n"
			+ "forumsentry-policy-ssb-iabs,1.0.109-1,groupid,forumsentry-policy-ssb-iabs,1.0.109,war,\n"
			+ "forumsentry-policy-ssb-pkiss,1.0.107-1,groupid,forumsentry-policy-ssb-pkiss,1.0.107,war,\n";

		ChainDeploymentVerification.verify(deploy, csv, 5, new int[]{6, 1, 1, 1, 9}, t0, t1, t2, t3, t4);
	}
	
	/**
	 * When a component already exists on a machine not included in the deployment descriptor, the component should be undeployed
	 * from that extraneous machine and not deployed to.
	 * 
	 * @throws Exception 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Test
	public void ebsad10113ExtraneousYaml() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-10113-extraneous-yaml");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] tran0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/cdp-soa-classpath-rpm/ensure", "1.0.46-1", "/st/st-sit1-cor1/soatzm03.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/cdp-soa-classpath-rpm/ensure", "1.0.46-1", "/st/st-sit1-cor1/soatzm04.st-sit1-cor1.ipt.local.yaml"),
		};
		
		ETD[] tran1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/EnvironmentConfiguration-Services/ensure", "1.0.150-1", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/EnvironmentConfiguration-Services/ensure", "1.0.150-1", "/st/st-sit1-cor1/soatzm02.st-sit1-cor1.ipt.local.yaml"),	
			new ETD(true, HieraData.ABSENT, "system::packages/EnvironmentConfiguration-Services/ensure", "1.0.115-1", "/st/st-sit1-cor1/soatzm03.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/EnvironmentConfiguration-Services/ensure", "1.0.115-1", "/st/st-sit1-cor1/soatzm04.st-sit1-cor1.ipt.local.yaml")
//			new ETD(true, HieraData.ABSENT, "system::packages/cdp-soa-audit-logging-config-rpm/ensure", "1.0.13-1", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
//			new ETD(true, HieraData.ABSENT, "system::packages/cdp-soa-audit-logging-config-rpm/ensure", "1.0.13-1", "/st/st-sit1-cor1/soatzm02.st-sit1-cor1.ipt.local.yaml"),
		};
		
		ETD[] tran2 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{tag=appdeploy, ensure=1.0.1-1}", "system::packages/EnvironmentConfiguration-Services", "{ensure=absent, require=[Class[Profile::Wls::Ipt_custom], Class[Profile::Wls::Startwls_managed]], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{tag=appdeploy, ensure=1.0.1-1}", "system::packages/EnvironmentConfiguration-Services", "{ensure=absent, require=[Class[Profile::Wls::Copydomain_nodes]], tag=appdeploy}", "/st/st-sit1-cor1/soatzm02.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.1-1}", "system::packages/cdp-soa-audit-logging-config-rpm", "{ensure=1.0.13-1, require=Package[SOAMETADATA-Services], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.1-1}", "system::packages/cdp-soa-audit-logging-config-rpm", "{ensure=1.0.13-1, require=Class[Profile::Wls::Copydomain_nodes], tag=mwconfig}", "/st/st-sit1-cor1/soatzm02.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.1-1", "system::packages/SOAMETADATA-Services/ensure", HieraData.ABSENT, "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.1-1", "system::packages/SOAMETADATA-Services/ensure", HieraData.ABSENT, "/st/st-sit1-cor1/soatzm02.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.1-1", "system::packages/cdp-soa-classpath-rpm/ensure", HieraData.ABSENT, "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.1-1", "system::packages/cdp-soa-security-config-rpm/ensure", HieraData.ABSENT, "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.1-1", "system::packages/addressmanagement-rpm/ensure", HieraData.ABSENT, "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{require=Package[SOAMETADATA-Services], tag=appdeploy, ensure=1.0.1-1}", "system::packages/biometricmanagement-rpm", "{ensure=absent, require=Package[eventmanagement-rpm], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{require=Package[SOAMETADATA-Services], tag=appdeploy, ensure=1.0.1-1}", "system::packages/documentmanagement-rpm", "{ensure=absent, require=Package[imagemanagement-rpm], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.1-1", "system::packages/etlbatchmanagement-rpm/ensure", HieraData.ABSENT, "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{require=Package[SOAMETADATA-Services], tag=appdeploy, ensure=1.0.1-1}", "system::packages/eventmanagement-rpm", "{ensure=absent, require=Package[processmanagement-rpm], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{require=Package[SOAMETADATA-Services], tag=appdeploy, ensure=1.0.1-1}", "system::packages/imagemanagement-rpm", "{ensure=absent, require=Package[servicedeliverymanagement-rpm], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{require=Package[SOAMETADATA-Services], tag=appdeploy, ensure=1.0.1-1}", "system::packages/partymanagement-rpm", "{ensure=absent, require=Package[biometricmanagement-rpm], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{require=Package[SOAMETADATA-Services], tag=appdeploy, ensure=1.0.1-1}", "system::packages/processmanagement-rpm", "{ensure=absent, require=Package[addressmanagement-rpm], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{require=Package[SOAMETADATA-Services], tag=appdeploy, ensure=1.0.1-1}", "system::packages/referencedatamanagement-rpm", "{ensure=absent, require=Package[imagemanagement-rpm], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{require=Package[SOAMETADATA-Services], tag=appdeploy, ensure=1.0.1-1}", "system::packages/searchmanagement-rpm", "{ensure=absent, require=Package[referencedatamanagement-rpm], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "{require=Package[SOAMETADATA-Services], tag=appdeploy, ensure=1.0.1-1}", "system::packages/servicedeliverymanagement-rpm", "{ensure=absent, require=Package[partymanagement-rpm], tag=appdeploy}", "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.1-1", "system::packages/systemmanagement-rpm/ensure", HieraData.ABSENT, "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
			new ETD(true, "1.0.1-1", "system::packages/utilitymanagement-rpm/ensure", HieraData.ABSENT, "/st/st-sit1-cor1/soatzm01.st-sit1-cor1.ipt.local.yaml"),
		};
		
		String csv = "EnvironmentConfiguration-Services,1.0.1-1,groupid,EnvironmentConfiguration-Services,1.0.1,war,\n"
						+ "SOAMETADATA-Services,1.0.1-1,groupid,SOAMETADATA-Services,1.0.1,war,\n"
						+ "addressmanagement-rpm,1.0.1-1,groupid,addressmanagement-rpm,1.0.1,war,\n"
						+ "biometricmanagement-rpm,1.0.1-1,groupid,biometricmanagement-rpm,1.0.1,war,\n"
						+ "cdp-soa-audit-logging-config-rpm,1.0.1-1,groupid,cdp-soa-audit-logging-config-rpm,1.0.1,war,\n"
						+ "cdp-soa-classpath-rpm,1.0.1-1,groupid,cdp-soa-classpath-rpm,1.0.1,war,\n"
						+ "cdp-soa-security-config-rpm,1.0.1-1,groupid,cdp-soa-security-config-rpm,1.0.1,war,\n"
						+ "documentmanagement-rpm,1.0.1-1,groupid,documentmanagement-rpm,1.0.1,war,\n"
						+ "etlbatchmanagement-rpm,1.0.1-1,groupid,etlbatchmanagement-rpm,1.0.1,war,\n"
						+ "eventmanagement-rpm,1.0.1-1,groupid,eventmanagement-rpm,1.0.1,war,\n"
						+ "imagemanagement-rpm,1.0.1-1,groupid,imagemanagement-rpm,1.0.1,war,\n"
						+ "partymanagement-rpm,1.0.1-1,groupid,partymanagement-rpm,1.0.1,war,\n"
						+ "processmanagement-rpm,1.0.1-1,groupid,processmanagement-rpm,1.0.1,war,\n"
						+ "referencedatamanagement-rpm,1.0.1-1,groupid,referencedatamanagement-rpm,1.0.1,war,\n"
						+ "searchmanagement-rpm,1.0.1-1,groupid,searchmanagement-rpm,1.0.1,war,\n"
						+ "servicedeliverymanagement-rpm,1.0.1-1,groupid,servicedeliverymanagement-rpm,1.0.1,war,\n"
						+ "systemmanagement-rpm,1.0.1-1,groupid,systemmanagement-rpm,1.0.1,war,\n"
						+ "utilitymanagement-rpm,1.0.1-1,groupid,utilitymanagement-rpm,1.0.1,war,\n";

		ChainDeploymentVerification.verify(dep, csv, 3, new int[]{2, 4, 21}, tran0, tran1, tran2);
	}
	
	/**
	 * If the Deployment Descriptor has a component (and YAML) and the component (and YAML) does not appear in the corresponding
	 * hiera data file, then we should insert the YAML in the hiera data.  
	 * @throws Exception 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Test
	public void ebsad11327InsertMissingYaml() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11327-insert-missing-yaml");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] tran1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{tag=appdeploy, ensure=1.0.135-1}", "system::packages/forumsentry-common", "{ensure=absent, tag=mwdeploy}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-common], tag=appdeploy, ensure=1.0.135-1}", "system::packages/forumsentry-installer", "{ensure=absent, require=Package[forumsentry-common], tag=mwconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-installer], tag=appdeploy, ensure=1.0.128-1}", "system::packages/forumsentry-core", "{ensure=absent, require=Package[forumsentry-installer], tag=mwconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "1.0.111-1", "system::packages/forumsentry-policy-ssb-brp/ensure", HieraData.ABSENT, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "1.0.103-1", "system::packages/forumsentry-policy-ssb-iabs/ensure", HieraData.ABSENT, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "1.0.102-1", "system::packages/forumsentry-policy-ssb-pkiss/ensure", HieraData.ABSENT, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{ensure=1.0.92-1, require=Package[forumsentry-policy-ssb-pkiss], tag=appdeploy}", "system::packages/forumsentry-config-ssb-pkiss", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-brp], tag=appdeploy, ensure=1.0.87-1}", "system::packages/forumsentry-config-ssb-brp", "{ensure=absent, require=Package[forumsentry-policy-ssb-brp], tag=appconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-iabs], tag=appdeploy, ensure=1.0.90-1}", "system::packages/forumsentry-config-ssb-iabs", "{ensure=absent, require=Package[forumsentry-policy-ssb-iabs], tag=appconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		String csv = "forumsentry-common,1.0.135-1,groupid,forumsentry-common,1.0.135,war,\n"
				+ "forumsentry-config-ssb-brp,1.0.87-1,groupid,forumsentry-config-ssb-brp,1.0.87,war,\n"
				+ "forumsentry-config-ssb-iabs,1.0.90-1,groupid,forumsentry-config-ssb-iabs,1.0.90,war,\n"
				+ "forumsentry-config-ssb-pkiss,1.0.92-1,groupid,forumsentry-config-ssb-pkiss,1.0.92,war,\n"
				+ "forumsentry-core,1.0.128-1,groupid,forumsentry-core,1.0.128,war,\n"
				+ "forumsentry-installer,1.0.135-1,groupid,forumsentry-installer,1.0.135,war,\n"
				+ "forumsentry-policy-ssb-brp,1.0.111-1,groupid,forumsentry-policy-ssb-brp,1.0.111,war,\n"
				+ "forumsentry-policy-ssb-iabs,1.0.103-1,groupid,forumsentry-policy-ssb-iabs,1.0.103,war,\n"
				+ "forumsentry-policy-ssb-pkiss,1.0.102-1,groupid,forumsentry-policy-ssb-pkiss,1.0.102,war,\n";

		ChainDeploymentVerification.verify(dep, csv, 1, new int[]{9}, tran1);
	}
	
    /**
	 * Since adding fix for undeploy transitions, deploy is not deploying in the required one transition. 
	 * @throws Exception 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Test
	public void ebsad9194FsbOneTran() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-9194-fsb-one-tran");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] tran1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{tag=appdeploy, ensure=1.0.135-1}", "system::packages/forumsentry-common", "{ensure=absent, tag=mwdeploy}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-common], tag=appdeploy, ensure=1.0.135-1}", "system::packages/forumsentry-installer", "{ensure=absent, require=Package[forumsentry-common], tag=mwconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-installer], tag=appdeploy, ensure=1.0.128-1}", "system::packages/forumsentry-core", "{ensure=absent, require=Package[forumsentry-installer], tag=mwconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "1.0.111-1", "system::packages/forumsentry-policy-ssb-brp/ensure", HieraData.ABSENT, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "1.0.103-1", "system::packages/forumsentry-policy-ssb-iabs/ensure", HieraData.ABSENT, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "1.0.102-1", "system::packages/forumsentry-policy-ssb-pkiss/ensure", HieraData.ABSENT, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-brp], tag=appdeploy, ensure=1.0.87-1}", "system::packages/forumsentry-config-ssb-brp", "{ensure=absent, require=Package[forumsentry-policy-ssb-brp], tag=appconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-iabs], tag=appdeploy, ensure=1.0.90-1}", "system::packages/forumsentry-config-ssb-iabs", "{ensure=absent, require=Package[forumsentry-policy-ssb-iabs], tag=appconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-pkiss], tag=appdeploy, ensure=1.0.92-1}", "system::packages/forumsentry-config-ssb-pkiss", "{ensure=absent, require=Package[forumsentry-policy-ssb-pkiss], tag=appconfig}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		String csv = "forumsentry-common,1.0.135-1,groupid,forumsentry-common,1.0.135,war,\n"
				+ "forumsentry-config-ssb-brp,1.0.87-1,groupid,forumsentry-config-ssb-brp,1.0.87,war,\n"
				+ "forumsentry-config-ssb-iabs,1.0.90-1,groupid,forumsentry-config-ssb-iabs,1.0.90,war,\n"
				+ "forumsentry-config-ssb-pkiss,1.0.92-1,groupid,forumsentry-config-ssb-pkiss,1.0.92,war,\n"
				+ "forumsentry-core,1.0.128-1,groupid,forumsentry-core,1.0.128,war,\n"
				+ "forumsentry-installer,1.0.135-1,groupid,forumsentry-installer,1.0.135,war,\n"
				+ "forumsentry-policy-ssb-brp,1.0.111-1,groupid,forumsentry-policy-ssb-brp,1.0.111,war,\n"
				+ "forumsentry-policy-ssb-iabs,1.0.103-1,groupid,forumsentry-policy-ssb-iabs,1.0.103,war,\n"
				+ "forumsentry-policy-ssb-pkiss,1.0.102-1,groupid,forumsentry-policy-ssb-pkiss,1.0.102,war,\n";

		ChainDeploymentVerification.verify(dep, csv, 1, new int[]{9}, tran1);
	}
	
    /**
	 * Dependency chains with branches in them result in components being deployed in the wrong order.
	 * @throws Exception 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Test
	public void ebsad9423OrderIncorrectOnMixedMultifileDeploy() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-9423-Order-incorrect-on-mixedmultifile-deploy");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] tran1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{tag=mwconfig, ensure=1.0.0-1}", "system::packages/gen-bin-jboss-fuse", "{ensure=absent, require=[Package[jdk], Package[gen_ebsa_common], File[/opt/fuse]], tag=mwdeploy}", "/np/np-prp1-przo/ssb.yaml"),
			new ETD(true, "{tag=mwconfig, ensure=1.0.0-1}", "system::packages/gen-bin-jboss-fuse", "{ensure=absent, require=Package[gen_ebsa_common], tag=mwdeploy}", "/np/np-prp1-przo/ssm.yaml"),
		};
		
		ETD[] tran2 = new ETD[]{
			new ETD(true, "{require=Package[gen-bin-jboss-fuse], tag=mwconfig, ensure=1.0.0-1}", "system::packages/gen-ins-jboss-fuse", "{ensure=absent, require=Package[gen-bin-jboss-fuse], tag=mwdeploy}", "/np/np-prp1-przo/ssb.yaml"),
			new ETD(true, "{require=Package[gen-bin-jboss-fuse], tag=mwconfig, ensure=1.0.0-1}", "system::packages/gen-ins-jboss-fuse", "{ensure=absent, require=Package[gen-bin-jboss-fuse], tag=mwdeploy}", "/np/np-prp1-przo/ssm.yaml"),
		};
                
		ETD[] tran3 = new ETD[]{
			new ETD(true, "{require=Package[gen-ins-jboss-fuse], tag=mwconfig, ensure=1.0.0-1}", "system::packages/ssb-fuse-config-ensemble", "{ensure=absent, require=[Package[gen-ins-jboss-fuse], File[/opt/fuse]], tag=mwconfig}", "/np/np-prp1-przo/ssm.yaml"),
		};
                
                ETD[] tran4 = new ETD[]{
			new ETD(true, "{tag=mwconfig, ensure=1.0.0-1}", "system::packages/ssb-fuse-config-managed-node", "{ensure=absent, require=[Package[gen-ins-jboss-fuse], File[/opt/fuse]], tag=mwconfig}", "/np/np-prp1-przo/ssb.yaml")
		};
                
                ETD[] tran5 = new ETD[]{
			new ETD(true, "{require=[Package[jdk], Service[postgresql-9.2]], tag=mwdeploy, ensure=1.0.0-1}", "system::packages/ssb-db-schema", "{ensure=absent, require=[Package[jdk], Service[postgresql-9.2]], tag=appdeploy}", "/np/np-prp1-przo/dbsgm11.np-prp1-przo.ipt.ho.local.yaml"),
			new ETD(true, "{require=[Package[jdk], Service[postgresql-9.2]], tag=mwdeploy, ensure=1.0.0-1}", "system::packages/ssb-db-schema", "{ensure=absent, require=[Package[jdk], Service[postgresql-9.2]], tag=appdeploy}", "/np/np-prp1-przo/dbsgm12.np-prp1-przo.ipt.ho.local.yaml"),
		};
                
                ETD[] tran6 = new ETD[]{
			new ETD(true, "{require=Package[ssb_cfg_openldap], tag=mwconfig, ensure=1.0.0-1}", "system::packages/ssb-ldap-schema", "{ensure=absent, require=Package[ssb_cfg_openldap], tag=appdeploy}", "/np/np-prp1-przo/ldp.yaml")
		};
                
                ETD[] tran7 = new ETD[]{
			new ETD(true, "1.0.0-1", "system::packages/ssb-rpm-nexus-baseline-config/ensure", HieraData.ABSENT, "/np/np-prp1-przo/rma.yaml")
		};
                
                ETD[] tran8 = new ETD[]{
			new ETD(true, "1.0.0-1", "system::packages/ssb-core-features-lib-nexus/ensure", HieraData.ABSENT, "/np/np-prp1-przo/rma.yaml")
		};
                ETD[] tran9 = new ETD[]{
			new ETD(true, "{tag=mwconfig, ensure=1.0.0-1}", "system::packages/ssb-core-features-fuse-config", "{ensure=absent, require=Package[ssb-fuse-config-ensemble], tag=mwconfig}", "/np/np-prp1-przo/ssm.yaml")
		};
                ETD[] tran10 = new ETD[]{
			new ETD(true, "1.0.0-1", "system::packages/ssb-core-features-fuse-application/ensure", HieraData.ABSENT, "/np/np-prp1-przo/ssm.yaml")
		};
		
        String csv = "gen-bin-jboss-fuse,1.0.0-1,groupid,gen-bin-jboss-fuse,1.0.0,war,\n"
			+ "gen-ins-jboss-fuse,1.0.0-1,groupid,gen-ins-jboss-fuse,1.0.0,war,\n"
			+ "ssb-core-features-fuse-application,1.0.0-1,groupid,ssb-core-features-fuse-application,1.0.0,war,\n"
			+ "ssb-core-features-fuse-config,1.0.0-1,groupid,ssb-core-features-fuse-config,1.0.0,war,\n"
			+ "ssb-core-features-lib-nexus,1.0.0-1,groupid,ssb-core-features-lib-nexus,1.0.0,war,\n"
			+ "ssb-db-schema,1.0.0-1,groupid,ssb-db-schema,1.0.0,war,\n"
			+ "ssb-fuse-config-ensemble,1.0.0-1,groupid,ssb-fuse-config-ensemble,1.0.0,war,\n"
			+ "ssb-fuse-config-managed-node,1.0.0-1,groupid,ssb-fuse-config-managed-node,1.0.0,war,\n"
			+ "ssb-ldap-schema,1.0.0-1,groupid,ssb-ldap-schema,1.0.0,war,\n"
			+ "ssb-rpm-nexus-baseline-config,1.0.0-1,groupid,ssb-rpm-nexus-baseline-config,1.0.0,war,\n";

		ChainDeploymentVerification.verify(dep, csv, 10, new int[]{2,2,1,1,2,1,1,1,1,1}, tran1, tran2, tran3, tran4, tran5, tran6, tran7, tran8, tran9, tran10);
	}
        
	@Test
	public void ebsad8418UndeployNoDeploy() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD_8418_undeploy_no_deploy");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] tran0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-config-ssb-brp/ensure", "1.0.87-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-config-ssb-iabs/ensure", "1.0.90-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-policy-ssb-brp/ensure", "1.0.111-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-policy-ssb-iabs/ensure", "1.0.103-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
		
		ETD[] tran1 = new ETD[] {new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-core/ensure", "1.0.128-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
		ETD[] tran2 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-installer/ensure", "1.0.135-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};

		ETD[] tran3 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-common/ensure", "1.0.137-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		
		ETD[] tran4 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "{tag=appdeploy, ensure=1.0.137-1}", "system::packages/forumsentry-common", "{ensure=absent, tag=mwdeploy}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		
		ChainDeploymentVerification.verify(dep, "forumsentry-common,1.0.137-1,groupid,forumsentry-common,1.0.137,war,\n", 5, new int[]{4,1,1,1,1}, tran0, tran1, tran2, tran3, tran4);
	}
	
	@Test
	public void ebsad10588_UndeployNeverDeployed() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD_10588_undeploy_never_deployed");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] tran0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-config-ssb-brp/ensure", "1.0.87-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-config-ssb-iabs/ensure", "1.0.90-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-policy-ssb-brp/ensure", "1.0.111-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-policy-ssb-iabs/ensure", "1.0.103-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		ETD[] tran1 = new ETD[]{
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-installer/ensure", "1.0.135-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};

		ETD[] tran2 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-common/ensure", "1.0.137-1", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		
		ETD[] tran3 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "{tag=appdeploy, ensure=1.0.137-1}", "system::packages/forumsentry-common", "{ensure=absent, tag=mwdeploy}", "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		
		ChainDeploymentVerification.verify(dep, "forumsentry-common,1.0.137-1,groupid,forumsentry-common,1.0.137,war,\n", 4, new int[]{4,1,1,1}, tran0, tran1, tran2, tran3);
	}
	
	/**
	 * See EBSAD-7691 for description of issue. Basically some packages are being uninstalled and reinstalled on
	 * a separate branch of the dependency tree when they don't need to be.
	 * @throws Exception 
	 */
	@Test
	public void ebsad7691MultiChangeDeployments() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD_7691_multichain_deployments");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t0 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-config-ssb-iabs/ensure", "1.0.95-1", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, HieraData.ABSENT, "system::packages/forumsentry-policy-ssb-iabs/ensure", "1.0.108-1", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
		};
		
		ETD[] t1 = new ETD[]{
			// true, requestedValue, requestedPath, existingValue, filePath
			new ETD(true, "{tag=appdeploy, ensure=1.0.140-1}", "system::packages/forumsentry-common", "{ensure=1.0.140-1, tag=mwdeploy}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-common], tag=appdeploy, ensure=1.0.140-1}", "system::packages/forumsentry-installer", "{ensure=1.0.140-1, require=Package[forumsentry-common], tag=mwconfig}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-installer], tag=appdeploy, ensure=1.0.131-1}", "system::packages/forumsentry-core", "{ensure=1.0.131-1, require=Package[forumsentry-installer], tag=mwconfig}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "1.0.109-1", "system::packages/forumsentry-policy-ssb-iabs/ensure", HieraData.ABSENT, "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-brp], tag=appdeploy, ensure=1.0.90-1}", "system::packages/forumsentry-config-ssb-brp", "{ensure=1.0.90-1, require=Package[forumsentry-policy-ssb-brp], tag=appconfig}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-iabs], tag=appdeploy, ensure=1.0.95-1}", "system::packages/forumsentry-config-ssb-iabs", "{ensure=absent, require=Package[forumsentry-policy-ssb-iabs], tag=appconfig}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true, "{require=Package[forumsentry-policy-ssb-pkiss], tag=appdeploy, ensure=1.0.87-1}", "system::packages/forumsentry-config-ssb-pkiss", "{ensure=1.0.87-1, require=Package[forumsentry-policy-ssb-pkiss], tag=appconfig}", "/st/st-cit1-cdp2/soatzm01.st-cit1-cdp2.ipt.local.yaml"),
	    };
		
		String csv = "forumsentry-config-ssb-iabs,1.0.95-1,groupid,forumsentry-config-ssb-iabs,1.0.95,war,\nforumsentry-policy-ssb-iabs,1.0.109-1,groupid,forumsentry-policy-ssb-iabs,1.0.109,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 2, new int[]{2,7}, t0, t1);
	}
	
	@Test
	public void ebsad8425MultiTransitionUndeploy() throws FileNotFoundException, IOException, Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-8425-multi-transition-undeploy");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		ETD[] t0 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/forumsentry-config-ssb-brp/ensure","1.0.87-1","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true,HieraData.ABSENT,"system::packages/forumsentry-config-ssb-iabs/ensure","1.0.90-1","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true,HieraData.ABSENT,"system::packages/forumsentry-policy-ssb-brp/ensure","1.0.111-1","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
			new ETD(true,HieraData.ABSENT,"system::packages/forumsentry-policy-ssb-iabs/ensure","1.0.103-1","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		ETD[] t1 = new ETD[] {new ETD(true,HieraData.ABSENT,"system::packages/forumsentry-core/ensure","1.0.128-1","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
		ETD[] t2 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/forumsentry-installer/ensure","1.0.135-1","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		ETD[] t3 = new ETD[]{
			new ETD(true,"{tag=appdeploy, ensure=1.0.137-1}","system::packages/forumsentry-common","{ensure=1.0.137-1, tag=mwdeploy}","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
		};
		ChainDeploymentVerification.verify(dep, null, 4, new int[]{4, 1, 1, 1}, t0, t1, t2, t3);
	}

	@Test
	public void daveMultiInsteadOfOne() throws Exception {
		//Dave found that there were multiple transitions instead of one one done of his transitions
		//had not showed up.  
		ApplicationVersion appVersion = setUpData("dave_multi_insteadOfone");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		ETD[] s = new ETD[]{ new ETD(true,"1.0.0-1","system::packages/one/ensure","0.0.9-1","/st/test-environment/mc1.yaml"),
 				 			 new ETD(true,"{ensure=1.0.0-1}","system::packages/four","{ensure=0.0.9-1, require=Package[three]}","/st/test-environment/mc2.yaml"),
                             new ETD(true,"1.0.0-1","system::packages/two/ensure","0.0.9-1","/st/test-environment/mc1.yaml")
				 };
		ChainDeploymentVerification.verify(dep, "four,1.0.0-1,groupid,four,1.0.0,war,\none,1.0.0-1,groupid,one,1.0.0,war,\nthree,1.0.0-1,groupid,three,1.0.0,war,\ntwo,1.0.0-1,groupid,two,1.0.0,war,\n", 2, new int[]{3,1}, s);
	}
	
	private String createEnvironmentName() {
		String prefix = ConfigurationFactory.getConfiguration("deployment.config.hieraOrganisationPrefix").equals("st") ?
				OrgEnvUtil.ST_ORG_PREFIX
				: OrgEnvUtil.NP_ORG_PREFIX;
		return prefix.toUpperCase() + ConfigurationFactory.getConfiguration("environment").toUpperCase().replaceAll("-", "_");
	}
	
	@Test
	public void subhashUpgradeConf() throws Exception {
		//Subhash found a null pointer in the report when there were no changes (i.e. transitions)  
		//If this runs to completion then the null pointer remains fixed.
		ApplicationVersion appVersion = setUpData("UpgradeConf");
		
		Exception ex = null;
		Deployer d = new Deployer();
		try {
			d.deploy(appVersion, createEnvironmentName(), null);
		} catch (Exception e) {
			ex = e;
		}
		assertEquals(null, "No changes can be made, deployment halted!", ex.getMessage());
	}
	
	/**
	 * Where YAML is out of sync and we are not doing a version change, we should FIX the YAML.
	 * @throws Exception 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Test
	public void ebsad11817YamlSync() throws FileNotFoundException, IOException, Exception{
		ApplicationVersion appVersion = setUpData("EBSAD-11817-yaml-sync");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t0 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/oer_stg_14.0/ensure","1.17-1","/st/st-cit1-cdp2/dbstzm01.yaml")
		};
		
		ETD[] t1 = new ETD[]{
			new ETD(true,"{tag=bananarama, ensure=1.29-1}","system::packages/oer_stg_conf","{ensure=1.29-1, tag=appdeploy}","/st/st-cit1-cdp2/dbstzm01.yaml"),
			new ETD(true,"1.18-1","system::packages/oer_stg_14.0/ensure",HieraData.ABSENT,"/st/st-cit1-cdp2/dbstzm01.yaml"),
		};
		
		String csv = "oer_stg_14.0,1.18-1,groupid,oer_stg_14.0,1.18,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 2, new int[]{1,2}, t0, t1);
	}
	
	/**
	 * Subhash was getting some additional deployment steps.  This was due to a change I made to merge down deployment 
	 * transitions if possible - the change introduced some regressions which have been rectified and checked here
	 * If this runs to completion then the null pointer remains fixed.
	 * @throws Exception
	 */
	@Test
	public void subhashUpgradeStg14() throws Exception {
		ApplicationVersion appVersion = setUpData("Upgrade_Stg_14");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] s = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/oer_stg_14.0/ensure","1.17-1","/st/st-cit1-cdp2/dbstzm01.yaml")
		};
		
		String csv = "oer_stg_14.0,1.18-1,groupid,oer_stg_14.0,1.18,war,\noer_stg_conf,1.29-1,groupid,oer_stg_conf,1.29,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 3, new int[]{1,1,2}, s);
	}
	
	
	/**
	 * Checks that upgrading/downgrading/undeploying a top level component will perform a no-change undeploy/redeploy
	 * of its dependents, if the chain behaviour is anything other than 'isolated'.
	 * 
	 * See EBSAD-9294 for further details.
	 * 
	 * @throws Exception
	 */
	@Test
	public void ebsad9294DowngradeTopLevelAndDependents() throws Exception {
		ApplicationVersion appVersion = setUpData("downgradeTopLevelAndDependents");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		// Uninstall depns on 'scheduler' host
		ETD[] trans1 = new ETD[]{
				new ETD(true,HieraData.ABSENT,"system::packages/cdp_activeeon_scripts/ensure","1.0.23-1","/st/st-cit1-cdp2/schtzm01.st-cit1-cdp2.ipt.local.yaml"),
				new ETD(true,HieraData.ABSENT,"system::packages/cdp_activeeon_workflows_rpm/ensure","1.0.9-1","/st/st-cit1-cdp2/schtzm01.st-cit1-cdp2.ipt.local.yaml"),
				new ETD(true,HieraData.ABSENT,"system::packages/gen_bin_proactive_scheduler/ensure","1.0.8-1","/st/st-cit1-cdp2/schtzm01.st-cit1-cdp2.ipt.local.yaml")
		};
		
		// Uninstall depns on 'node' host
		ETD[] trans2 = new ETD[]{
				new ETD(true,HieraData.ABSENT,"system::packages/gen_bin_proactive_node/ensure","1.0.59-1","/st/st-cit1-cdp2/etltzm01.st-cit1-cdp2.ipt.local.yaml")
		};
		
		// Uninstall request top level component
		ETD[] trans3 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/cdp_activeeon_conf/ensure","1.0.8-1","/st/st-cit1-cdp2/etltzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true,HieraData.ABSENT,"system::packages/cdp_activeeon_conf/ensure","1.0.8-1","/st/st-cit1-cdp2/schtzm01.st-cit1-cdp2.ipt.local.yaml")
		};
		
		// Re-install all components (no need to transition, puppet is able to interpret dependencies during deployment)
		ETD[] trans4 = new ETD[] {
			new ETD(true,"1.0.9-1","system::packages/cdp_activeeon_conf/ensure",HieraData.ABSENT,"/st/st-cit1-cdp2/etltzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true,"1.0.9-1","system::packages/cdp_activeeon_conf/ensure",HieraData.ABSENT,"/st/st-cit1-cdp2/schtzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true,"1.0.59-1","system::packages/gen_bin_proactive_node/ensure",HieraData.ABSENT,"/st/st-cit1-cdp2/etltzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true,"1.0.23-1","system::packages/cdp_activeeon_scripts/ensure",HieraData.ABSENT,"/st/st-cit1-cdp2/schtzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true,"1.0.9-1","system::packages/cdp_activeeon_workflows_rpm/ensure",HieraData.ABSENT,"/st/st-cit1-cdp2/schtzm01.st-cit1-cdp2.ipt.local.yaml"),
			new ETD(true,"1.0.8-1","system::packages/gen_bin_proactive_scheduler/ensure",HieraData.ABSENT,"/st/st-cit1-cdp2/schtzm01.st-cit1-cdp2.ipt.local.yaml")
		};
		
		String csv = "cdp_activeeon_conf,1.0.9-1,groupid,cdp_activeeon_conf,1.0.9,war,"
				+ "\ncdp_activeeon_scripts,1.0.23-1,groupid,cdp_activeeon_scripts,1.0.23,war,"
				+ "\ncdp_activeeon_workflows_rpm,1.0.9-1,groupid,cdp_activeeon_workflows_rpm,1.0.9,war,"
				+ "\ngen_bin_proactive_node,1.0.59-1,groupid,gen_bin_proactive_node,1.0.59,war,"
				+ "\ngen_bin_proactive_scheduler,1.0.8-1,groupid,gen_bin_proactive_scheduler,1.0.8,war,"
				+ "\n";
		ChainDeploymentVerification.verify(dep, csv, 4, new int[]{3, 1, 2, 6}, trans1, trans2, trans3, trans4);
	}
	
	
	@Test
	public void invalidDeploymentTest() throws Exception {
		ApplicationVersion appVersion = setUpData("Invalid_Deployment_Test");
		
		Exception ex = null;
		Deployer d = new Deployer();
		try {
			d.deploy(appVersion, OrgEnvUtil.ST_ORG_PREFIX.toUpperCase() + ConfigurationFactory.getConfiguration("environment").toUpperCase().replaceAll("-", "_"), null);
			fail("Exception expected");
		} catch (Exception e) {
			ex = e;
		}
		assertEquals("Some failures have been detected and these need to be addressed before the deployment can continue.", ex.getMessage());
	}

	
	/**
	 * This test and the accompanying resources replicate EBSAD-10020.
	 * 
	 * @see {@code test/resouces/scenarios/undeployAll_EBSAD_10020}
	 * @throws Exception
	 */
	@Test
	public void ebsad10020UndeployAll() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-10020_UndeployAll");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] rmaComponentRemovals1 = new ETD[] {
			new ETD(true,"absent","system::packages/ssb-core-features-lib-nexus/ensure", "2.1.41-release_2.1.41_1","/st/st-dev1-ebs1/rma.yaml"),
		};
		ETD[] rmaComponentRemovals2 = new ETD[] {
				new ETD(true,"absent","system::packages/ssb-rpm-nexus-baseline-config/ensure", "2.0.3-1","/st/st-dev1-ebs1/rma.yaml")
			};
		ETD[] ldpComponentRemovals = new ETD[] {
			new ETD(true,"absent","system::packages/ssb-ldap-schema/ensure", "1.143-1", "/st/st-dev1-ebs1/ldp.yaml")
		};
		ETD[] dbComponentRemovals = new ETD[] {
			new ETD(true,"absent","system::packages/ssb-db-schema/ensure", "1.376.293-1", "/st/st-dev1-ebs1/dbs.yaml")
		};
		
		ChainDeploymentVerification.verify(dep, null, 4, new int[]{1, 1, 1, 1}, rmaComponentRemovals1, rmaComponentRemovals2, ldpComponentRemovals, dbComponentRemovals);
	}
	

	/**
	 * Sets up data based on a scenario defined in the src/test/resources/scenarios folder
	 * @param scenarioFolder
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws Exception
	 */
	private ApplicationVersion setUpData(String scenarioFolder) throws IOException, FileNotFoundException, Exception {
		/* Load the external scenario properties */
		
		TestHelper.mergeProperties(SCENARIOS_BASE_FOLDER+scenarioFolder+"/scenario.properties");
		/* Override those that are relative paths */
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER, SCENARIOS_BASE_FOLDER+scenarioFolder+"/hiera");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER, "target/scenarios/"+scenarioFolder);
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR, SCENARIOS_BASE_FOLDER+scenarioFolder+"/DeploymentDescriptor.xml");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_USE_UNIQUE_NAME, "false");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARE_ONLY, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FILENAME, scenarioFolder + ".html");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_YUM_TEST_FOLDER, SCENARIOS_BASE_FOLDER+scenarioFolder + "/yum/yum.txt");
		
		/* Load the external scenarios */
		ApplicationVersion appVersion = setUpDataForTest();
		return appVersion;
	}

	private ApplicationVersion setUpDataForTest() throws Exception {
		return TestHelper.getApplicationVersion(getEntityManager());
	}

	/**
	 * Complete uninstall of all SSB components over 4 transitions
	 * @throws Exception
	 */
	@Test
	public void ebsad9727SsbCompleteUndeploy() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-9727-SSB-Undeploy");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"absent","system::packages/ssb-core-features-fuse-application/ensure","2.1.119-1","/st/st-dev1-ebs1/ssb.yaml"),
	 			 new ETD(true,"absent","system::packages/ssb-core-features-fuse-config/ensure","2.1.119-1","/st/st-dev1-ebs1/ssb.yaml"),
		};
		ETD[] t2 = new ETD[] { new ETD(true,"absent","system::packages/ssb-rpm-fuse-config/ensure","2.0.294-1","/st/st-dev1-ebs1/ssb.yaml")};
		ETD[] t3 = new ETD[] { new ETD(true,"absent","system::packages/ssb-core-features-lib-nexus/ensure","2.1.41-release_2.1.41_1","/st/st-dev1-ebs1/rma.yaml")};
		ETD[] t4 = new ETD[] { new ETD(true,"absent","system::packages/ssb-rpm-nexus-baseline-config/ensure","2.0.3-1","/st/st-dev1-ebs1/rma.yaml")};
		ETD[] t5 = new ETD[] { new ETD(true,"absent","system::packages/ssb-ldap-schema/ensure","1.143-1","/st/st-dev1-ebs1/ldp.yaml")};
		ETD[] t6 = new ETD[] { new ETD(true,"absent","system::packages/ssb-db-schema/ensure","1.376.293-1","/st/st-dev1-ebs1/dbs.yaml")};
		
		ChainDeploymentVerification.verify(dep, null, 6, new int[]{2,1,1,1,1,1}, t1, t2, t3, t4, t5, t6);
	}
	
	/**
	 * Complete install of all SSB components over 4 transitions (components already in YAML as 'absent')
	 * @throws Exception
	 */
	@Test
	public void ebsad9727SsbCompleteDeploy() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-9727-SSB-Deploy");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"1.376.293-1","system::packages/ssb-db-schema/ensure","absent","/st/st-dev1-ebs1/dbs.yaml")};
		ETD[] t2 = new ETD[]{ new ETD(true,"{require=Package[ssb_cfg_openldap], tag=mwconfig, ensure=1.143-1}","system::packages/ssb-ldap-schema","{ensure=absent, require=Package[ssb_cfg_openldap], tag=appdeploy}","/st/st-dev1-ebs1/ldp.yaml")};
		ETD[] t3 = new ETD[]{
				new ETD(true,"{require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], ensure=2.0.3-1}","system::packages/ssb-rpm-nexus-baseline-config","{ensure=absent, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], tag=mwconfig}","/st/st-dev1-ebs1/rma.yaml"),
				new ETD(true,"{tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Mount[/var/sonatype-work]], ensure=2.1.41-release_2.1.41_1}","system::packages/ssb-core-features-lib-nexus","{ensure=absent, tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Package[gen_bin_nexus], Mount[/var/sonatype-work]]}","/st/st-dev1-ebs1/rma.yaml")	 			 
		};
		ETD[] t4 = new ETD[]{
				new ETD(true,"{require=[Package[gen-ins-jboss-fuse], File[/opt/fuse]], ensure=2.0.294-1}","system::packages/ssb-rpm-fuse-config","{ensure=absent, require=Package[gen-ins-jboss-fuse], tag=mwconfig}","/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true,"{require=Package[ssb-rpm-fuse-config], tag=mwconfig, ensure=2.1.119-1}","system::packages/ssb-core-features-fuse-config","{ensure=absent, require=Package[ssb-rpm-fuse-config], tag=mwdeploy}","/st/st-dev1-ebs1/ssb.yaml"),
	 			new ETD(true,"{require=Package[ssb-core-features-fuse-config], tag=mwconfig, ensure=2.1.119-1}","system::packages/ssb-core-features-fuse-application","{ensure=absent, require=Package[ssb-core-features-fuse-config], tag=mwdeploy}","/st/st-dev1-ebs1/ssb.yaml")
        };	
		
		String csv = "ssb-core-features-fuse-application,2.1.119-1,groupid,ssb-core-features-fuse-application,2.1.119,war,"
		+ "\nssb-core-features-fuse-config,2.1.119-1,groupid,ssb-core-features-fuse-config,2.1.119,war,"
		+ "\nssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,groupid,ssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,war,"
		+ "\nssb-db-schema,1.376.293-1,groupid,ssb-db-schema,1.376.293,war,"
		+ "\nssb-ldap-schema,1.143-1,groupid,ssb-ldap-schema,1.143,war,"
		+ "\nssb-rpm-fuse-config,2.0.294-1,groupid,ssb-rpm-fuse-config,2.0.294,war,"
		+ "\nssb-rpm-nexus-baseline-config,2.0.3-1,groupid,ssb-rpm-nexus-baseline-config,2.0.3,war,"
		+ "\n";
				
		ChainDeploymentVerification.verify(dep, csv, 4, new int[]{1,1,2,3}, t1, t2, t3, t4);
	}
        
    /**
	 * Complete install of all SSB components over 4 transitions (components already in YAML as 'absent')
	 * @throws Exception
	 */
	@Test
	public void ebsad12005_restarts() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12005-restarts");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] ta = new ETD[]{ new ETD(true,"step","thepath/for/start",null,"/st/st-dev1-ebs1/dbs.yaml")};
		ETD[] t1 = new ETD[]{ new ETD(true,"1.376.293-1","system::packages/ssb-db-schema/ensure","absent","/st/st-dev1-ebs1/dbs.yaml")};
		ETD[] t2 = new ETD[]{ new ETD(true,"{require=Package[ssb_cfg_openldap], tag=mwconfig, ensure=1.143-1}","system::packages/ssb-ldap-schema","{ensure=absent, require=Package[ssb_cfg_openldap], tag=appdeploy}","/st/st-dev1-ebs1/ldp.yaml")};
		ETD[] t3 = new ETD[]{
				new ETD(true,"{require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], ensure=2.0.3-1}","system::packages/ssb-rpm-nexus-baseline-config","{ensure=absent, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], tag=mwconfig}","/st/st-dev1-ebs1/rma.yaml"),
				new ETD(true,"{tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Mount[/var/sonatype-work]], ensure=2.1.41-release_2.1.41_1}","system::packages/ssb-core-features-lib-nexus","{ensure=absent, tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Package[gen_bin_nexus], Mount[/var/sonatype-work]]}","/st/st-dev1-ebs1/rma.yaml")	 			 
		};
		ETD[] t4 = new ETD[]{
				new ETD(true,"{require=[Package[gen-ins-jboss-fuse], File[/opt/fuse]], ensure=2.0.294-1}","system::packages/ssb-rpm-fuse-config","{ensure=absent, require=Package[gen-ins-jboss-fuse], tag=mwconfig}","/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true,"{require=Package[ssb-rpm-fuse-config], tag=mwconfig, ensure=2.1.119-1}","system::packages/ssb-core-features-fuse-config","{ensure=absent, require=Package[ssb-rpm-fuse-config], tag=mwdeploy}","/st/st-dev1-ebs1/ssb.yaml"),
	 			new ETD(true,"{require=Package[ssb-core-features-fuse-config], tag=mwconfig, ensure=2.1.119-1}","system::packages/ssb-core-features-fuse-application","{ensure=absent, require=Package[ssb-core-features-fuse-config], tag=mwdeploy}","/st/st-dev1-ebs1/ssb.yaml")
                };
                ETD[] tb = new ETD[]{ new ETD(true,"trep","thepath/for/end",null,"/st/st-dev1-ebs1/dbs.yaml"),
                                      new ETD(true,"trep2","thepath/for/end2",null,"/st/st-dev1-ebs1/dbs.yaml")};
		
		String csv = "ssb-core-features-fuse-application,2.1.119-1,groupid,ssb-core-features-fuse-application,2.1.119,war,"
		+ "\nssb-core-features-fuse-config,2.1.119-1,groupid,ssb-core-features-fuse-config,2.1.119,war,"
		+ "\nssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,groupid,ssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,war,"
		+ "\nssb-db-schema,1.376.293-1,groupid,ssb-db-schema,1.376.293,war,"
		+ "\nssb-ldap-schema,1.143-1,groupid,ssb-ldap-schema,1.143,war,"
		+ "\nssb-rpm-fuse-config,2.0.294-1,groupid,ssb-rpm-fuse-config,2.0.294,war,"
		+ "\nssb-rpm-nexus-baseline-config,2.0.3-1,groupid,ssb-rpm-nexus-baseline-config,2.0.3,war,"
		+ "\n";
				
		ChainDeploymentVerification.verify(dep, csv, 6, new int[]{1,1,1,2,3,2}, ta, t1, t2, t3, t4, tb);
	}

	@Test
	public void ebsad12005_BeforeAndAfterStep() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12005-simpleBeforeAndAfter");

		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);

		ETD[] t0 = new ETD[] { new ETD(true, "stepOneValue", "stepOneInjectPath", null, "/st/st-dev1-ebs1/dbs.yaml") };

		ETD[] t1 = new ETD[] { new ETD(true, "beforeValue", "componentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/dbs.yaml"),
				new ETD(true, "beforeValue", "componentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/dzf.yaml")};
		
		ETD[] t2 = new ETD[] { new ETD(true, "1.376.293-1", "system::packages/ssb-db-schema/ensure", "absent", "/st/st-dev1-ebs1/dbs.yaml") };
		
		ETD[] t3 = new ETD[] { new ETD(true, "afterValue", "componentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/dbs.yaml"),
				new ETD(true, "afterValue", "componentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/dzf.yaml")};

		ETD[] t4 = new ETD[] { new ETD(true, "beforeValue", "hintLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/ldp.yaml") };
		
		ETD[] t5 = new ETD[] { new ETD(true, "{require=Package[ssb_cfg_openldap], tag=mwconfig, ensure=1.143-1}", "system::packages/ssb-ldap-schema", "{ensure=absent, require=Package[ssb_cfg_openldap], tag=appdeploy}", "/st/st-dev1-ebs1/ldp.yaml") };
		
		ETD[] t6 = new ETD[] { new ETD(true, "afterValue", "hintLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/ldp.yaml") };

		ETD[] t7 = new ETD[] { new ETD(true, "{require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], ensure=2.0.3-1}", "system::packages/ssb-rpm-nexus-baseline-config", "{ensure=absent, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], tag=mwconfig}", "/st/st-dev1-ebs1/rma.yaml"),
				new ETD(true, "{tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Mount[/var/sonatype-work]], ensure=2.1.41-release_2.1.41_1}", "system::packages/ssb-core-features-lib-nexus", "{ensure=absent, tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Package[gen_bin_nexus], Mount[/var/sonatype-work]]}", "/st/st-dev1-ebs1/rma.yaml") };
		
		ETD[] t8 = new ETD[] { new ETD(true, "{require=[Package[gen-ins-jboss-fuse], File[/opt/fuse]], ensure=2.0.294-1}", "system::packages/ssb-rpm-fuse-config", "{ensure=absent, require=Package[gen-ins-jboss-fuse], tag=mwconfig}", "/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true, "{require=Package[ssb-rpm-fuse-config], tag=mwconfig, ensure=2.1.119-1}", "system::packages/ssb-core-features-fuse-config", "{ensure=absent, require=Package[ssb-rpm-fuse-config], tag=mwdeploy}", "/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true, "{require=Package[ssb-core-features-fuse-config], tag=mwconfig, ensure=2.1.119-1}", "system::packages/ssb-core-features-fuse-application", "{ensure=absent, require=Package[ssb-core-features-fuse-config], tag=mwdeploy}", "/st/st-dev1-ebs1/ssb.yaml") };
		
		ETD[] t9 = new ETD[] { new ETD(true, "stepThreeAValue", "stepThreeAInjectPath", null, "/st/st-dev1-ebs1/dbs.yaml"), 
				new ETD(true, "stepThreeBValue", "stepThreeBInjectPath", null, "/st/st-dev1-ebs1/dbs.yaml") };

		String csv = "ssb-core-features-fuse-application,2.1.119-1,groupid,ssb-core-features-fuse-application,2.1.119,war," + "\nssb-core-features-fuse-config,2.1.119-1,groupid,ssb-core-features-fuse-config,2.1.119,war," + "\nssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,groupid,ssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,war," + "\nssb-db-schema,1.376.293-1,groupid,ssb-db-schema,1.376.293,war," + "\nssb-ldap-schema,1.143-1,groupid,ssb-ldap-schema,1.143,war,"
				+ "\nssb-rpm-fuse-config,2.0.294-1,groupid,ssb-rpm-fuse-config,2.0.294,war," + "\nssb-rpm-nexus-baseline-config,2.0.3-1,groupid,ssb-rpm-nexus-baseline-config,2.0.3,war," + "\n";

		ChainDeploymentVerification.verify(dep, csv, 10, new int[] { 1, 2, 1, 2, 1, 1, 1, 2, 3, 2 }, t0, t1, t2, t3, t4, t5, t6, t7, t8, t9);
	}
	
	/**
	 * Test for complex set of commands at both plan, component and hint level.
	 * Also covers case where commands are skipped because ssb-ldap-schema is not being deployed.
	 * @throws Exception
	 */
	@Test
	public void ebsad12005_Commands() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12005-commands");
		
		String zone = "IPT_ST_DEV1_EBS1";

		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t0 = new ETD[] { new ETD(true, "start commands", buildCommandHost("dbs", zone), null, null) };
		
		ETD[] t1 = new ETD[] { new ETD(true, "componentLevelBeforeCommand", buildCommandHost("dbs", zone), null, null) };
		
		ETD[] t2 = new ETD[] { new ETD(true, "1.376.2193-1", "system::packages/ssb-db-schema/ensure", "absent", "/st/st-dev1-ebs1/dbs.yaml") };
		
		ETD[] t3 = new ETD[] { new ETD(true, "componentLevelAfterCommand", buildCommandHost("dbs", zone), null, null) };
		
		ETD[] t4 = new ETD[] { new ETD(true, "{require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], ensure=2.0.3-1}", "system::packages/ssb-rpm-nexus-baseline-config", "{ensure=absent, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], tag=mwconfig}", "/st/st-dev1-ebs1/rma.yaml"),
				new ETD(true, "{tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Mount[/var/sonatype-work]], ensure=2.1.41-release_2.1.41_1}", "system::packages/ssb-core-features-lib-nexus", "{ensure=absent, tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Package[gen_bin_nexus], Mount[/var/sonatype-work]]}", "/st/st-dev1-ebs1/rma.yaml") };
		
		ETD[] t5 = new ETD[] { new ETD(true, "hintLevelBeforeDeployCommand1", buildCommandHost("dbs", zone), null, null),
				new ETD(true, "hintLevelBeforeDeployCommand2", buildCommandHost("dbs1", zone), null, null),
				new ETD(true, "hintLevelBeforeDeployCommand3", buildCommandHost("dbs2", zone), null, null) };
		
		ETD[] t6 = new ETD[] { new ETD(true, "{require=[Package[gen-ins-jboss-fuse], File[/opt/fuse]], ensure=2.0.294-1}", "system::packages/ssb-rpm-fuse-config", "{ensure=absent, require=Package[gen-ins-jboss-fuse], tag=mwconfig}", "/st/st-dev1-ebs1/ssb.yaml") };
		
		ETD[] t7 = new ETD[] { new ETD(true, "hintLevelAfterDeployCommand", buildCommandHost("dbs2", zone), null, null) };
		
		ETD[] t8 = new ETD[] { new ETD(true, "{require=Package[ssb-rpm-fuse-config], tag=mwconfig, ensure=2.1.119-1}", "system::packages/ssb-core-features-fuse-config", "{ensure=absent, require=Package[ssb-rpm-fuse-config], tag=mwdeploy}", "/st/st-dev1-ebs1/ssb.yaml"),
			new ETD(true, "{require=Package[ssb-core-features-fuse-config], tag=mwconfig, ensure=2.1.119-1}", "system::packages/ssb-core-features-fuse-application", "{ensure=absent, require=Package[ssb-core-features-fuse-config], tag=mwdeploy}", "/st/st-dev1-ebs1/ssb.yaml") };
		
		ETD[] t9 = new ETD[] { new ETD(true, "end commands", buildCommandHost("dbs", zone), null, null) };
		
		String csv = "ssb-core-features-fuse-application,2.1.119-1,groupid,ssb-core-features-fuse-application,2.1.119,war," + "\nssb-core-features-fuse-config,2.1.119-1,groupid,ssb-core-features-fuse-config,2.1.119,war," + "\nssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,groupid,ssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,war," + "\nssb-db-schema,1.376.2193-1,groupid,ssb-db-schema,1.376.2193,war,"
				+ "\nssb-rpm-fuse-config,2.0.294-1,groupid,ssb-rpm-fuse-config,2.0.294,war," + "\nssb-rpm-nexus-baseline-config,2.0.3-1,groupid,ssb-rpm-nexus-baseline-config,2.0.3,war," + "\n";

		ChainDeploymentVerification.verify(dep, csv, 10, new int[] { 1, 1, 1, 1, 2, 3, 1, 1, 2, 1 }, t0, t1, t2, t3, t4, t5, t6, t7, t8, t9);
	}

	private String buildCommandHost(String hostOrRole, String zone) {
		return Arrays.asList(new ResolvedHost(hostOrRole, zone)).toString();
	}

	/**
	 * Path in YAML doesn't exist we are expecting the deployment to fail with an exception
	 * @throws Exception
	 */
	@Test(expected=Exception.class)
	public void ebsad12005_fail() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12005-fail");

		Deployer d = new Deployer();
		d.deploy(appVersion, createEnvironmentName(), null);		
	}
	
	/**
	 * Path in YAML doesn't exist we are expecting the deployment to fail with an exception
	 * @throws Exception
	 */
	@Test(expected=Exception.class)
	public void ebsad12005_fail2() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12005-fail2");

		Deployer d = new Deployer();
		d.deploy(appVersion, createEnvironmentName(), null);		
	}
	
	/**
	 * Path in YAML doesn't exist we are expecting the deployment to fail with an exception
	 * @throws Exception
	 */
	@Test(expected=Exception.class)
	public void ebsad12005_fail3() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12005-fail3");

		Deployer d = new Deployer();
		d.deploy(appVersion, createEnvironmentName(), null);		
	}
	
	/**
	 * Complex scenario with multiple before and after steps (transitions) at component and component hint level.
	 * Also checks that steps at the undeploy hint level are not actioned (as that component is being deployed, not undeployed)
	 * @throws Exception
	 */
	@Test
	public void ebsad12005_multiStep() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12005-multiStep");

		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);

		ETD[] t0 = new ETD[] { new ETD(true, "stepOneValue", "stepOneInjectPath", null, "/st/st-dev1-ebs1/dbs.yaml") };

		ETD[] t1 = new ETD[] { new ETD(true, "beforeValue", "componentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/dbs.yaml"),
				new ETD(true, "beforeValue", "secondComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/dbs.yaml")};
		
		ETD[] t2 = new ETD[] { new ETD(true, "beforeValue", "thirdComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/dbs.yaml"),
				new ETD(true, "beforeValue", "forthComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/dbs.yaml")};
		
		ETD[] t3 = new ETD[] { new ETD(true, "1.376.293-1", "system::packages/ssb-db-schema/ensure", "absent", "/st/st-dev1-ebs1/dbs.yaml") };
		
		ETD[] t4 = new ETD[] { new ETD(true, "afterValue", "componentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/dbs.yaml"),
				new ETD(true, "afterValue", "secondComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/dbs.yaml")};
		
		ETD[] t5 = new ETD[] { new ETD(true, "afterValue", "thirdComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/dbs.yaml"),
				new ETD(true, "afterValue", "forthComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/dbs.yaml")};

		ETD[] t6 = new ETD[] { new ETD(true, "beforeValue", "hintLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/ldp.yaml") };
		ETD[] t7 = new ETD[] { new ETD(true, "{require=Package[ssb_cfg_openldap], tag=mwconfig, ensure=1.143-1}", "system::packages/ssb-ldap-schema", "{ensure=absent, require=Package[ssb_cfg_openldap], tag=appdeploy}", "/st/st-dev1-ebs1/ldp.yaml") };
		ETD[] t8 = new ETD[] { new ETD(true, "afterValue", "hintLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/ldp.yaml") };
		
		ETD[] t9 = new ETD[] { new ETD(true, "beforeValue", "componentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/rma.yaml"),
				new ETD(true, "beforeValue", "secondComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/rma.yaml")};
		
		ETD[] t10 = new ETD[] { new ETD(true, "beforeValue", "thirdComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/rma.yaml"),
				new ETD(true, "beforeValue", "forthComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/rma.yaml")};
				
		ETD[] t11 = new ETD[] { new ETD(true, "{require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], ensure=2.0.3-1}", "system::packages/ssb-rpm-nexus-baseline-config", "{ensure=absent, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], tag=mwconfig}", "/st/st-dev1-ebs1/rma.yaml")};
		
		ETD[] t12 = new ETD[] { new ETD(true, "afterValue", "componentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/rma.yaml"),
				new ETD(true, "afterValue", "secondComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/rma.yaml")};
		
		ETD[] t13 = new ETD[] { new ETD(true, "afterValue", "thirdComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/rma.yaml"),
				new ETD(true, "afterValue", "forthComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/rma.yaml")};
		
		ETD[] t14 = new ETD[] { new ETD(true, "{tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Mount[/var/sonatype-work]], ensure=2.1.41-release_2.1.41_1}", "system::packages/ssb-core-features-lib-nexus", "{ensure=absent, tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Package[gen_bin_nexus], Mount[/var/sonatype-work]]}", "/st/st-dev1-ebs1/rma.yaml") };
		ETD[] t15 = new ETD[] { new ETD(true, "{require=[Package[gen-ins-jboss-fuse], File[/opt/fuse]], ensure=2.0.294-1}", "system::packages/ssb-rpm-fuse-config", "{ensure=absent, require=Package[gen-ins-jboss-fuse], tag=mwconfig}", "/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true, "{require=Package[ssb-rpm-fuse-config], tag=mwconfig, ensure=2.1.119-1}", "system::packages/ssb-core-features-fuse-config", "{ensure=absent, require=Package[ssb-rpm-fuse-config], tag=mwdeploy}", "/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true, "{require=Package[ssb-core-features-fuse-config], tag=mwconfig, ensure=2.1.119-1}", "system::packages/ssb-core-features-fuse-application", "{ensure=absent, require=Package[ssb-core-features-fuse-config], tag=mwdeploy}", "/st/st-dev1-ebs1/ssb.yaml") };
		ETD[] t16 = new ETD[] { new ETD(true, "stepThreeAValue", "stepThreeAInjectPath", null, "/st/st-dev1-ebs1/dbs.yaml"), new ETD(true, "stepThreeBValue", "stepThreeBInjectPath", null, "/st/st-dev1-ebs1/dbs.yaml") };

		String csv = "ssb-core-features-fuse-application,2.1.119-1,groupid,ssb-core-features-fuse-application,2.1.119,war," + "\nssb-core-features-fuse-config,2.1.119-1,groupid,ssb-core-features-fuse-config,2.1.119,war," + "\nssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,groupid,ssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,war," + "\nssb-db-schema,1.376.293-1,groupid,ssb-db-schema,1.376.293,war," + "\nssb-ldap-schema,1.143-1,groupid,ssb-ldap-schema,1.143,war,"
				+ "\nssb-rpm-fuse-config,2.0.294-1,groupid,ssb-rpm-fuse-config,2.0.294,war," + "\nssb-rpm-nexus-baseline-config,2.0.3-1,groupid,ssb-rpm-nexus-baseline-config,2.0.3,war," + "\n";

		ChainDeploymentVerification.verify(dep, csv, 17, new int[] { 1, 2, 2, 1, 2, 2, 1, 1, 1, 2, 2, 1, 2, 2, 1, 3, 2}, t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16);
	}	
	
	/**
	 * Complex scenario with multiple before and after steps (transitions) at component and component hint level.
	 * Also checks that steps at the deploy hint level are not actioned (as that component is being undeployed, not deployed)
	 * @throws Exception
	 */
	@Test
	public void ebsad12005_multiStep_undeploy() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12005-multiStep-undeploy");

		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);

		ETD[] t0 = new ETD[] { new ETD(true, "stepOneValue", "stepOneInjectPath", null, "/st/st-dev1-ebs1/dbs.yaml") };
		
		ETD[] t1 = new ETD[] { new ETD(true, "absent", "system::packages/ssb-core-features-fuse-application/ensure", "2.1.119-1", "/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true, "absent", "system::packages/ssb-core-features-fuse-config/ensure", "2.1.119-1", "/st/st-dev1-ebs1/ssb.yaml")
		};
		
		ETD[] t2 = new ETD[] { new ETD(true, "beforeValue", "componentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true, "beforeValue", "secondComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/ssb.yaml")};
		
		ETD[] t3 = new ETD[] { new ETD(true, "beforeValue", "thirdComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true, "beforeValue", "forthComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/ssb.yaml")				
		};
		
		ETD[] t4 = new ETD[] { new ETD(true, "absent", "system::packages/ssb-rpm-fuse-config/ensure", "2.0.294-1", "/st/st-dev1-ebs1/ssb.yaml")				
		};
		
		ETD[] t5 = new ETD[] { new ETD(true, "afterValue", "componentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true, "afterValue", "secondComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/ssb.yaml")				
		};
		
		ETD[] t6 = new ETD[] { new ETD(true, "afterValue", "thirdComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true, "afterValue", "forthComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/ssb.yaml")				
		};
		
		ETD[] t7 = new ETD[] { new ETD(true, "absent", "system::packages/ssb-core-features-lib-nexus/ensure", "2.1.41-release_2.1.41_1", "/st/st-dev1-ebs1/rma.yaml")};
		ETD[] t8 = new ETD[] {	new ETD(true, "absent", "system::packages/ssb-rpm-nexus-baseline-config/ensure", "2.0.3-1", "/st/st-dev1-ebs1/rma.yaml")};
		
		ETD[] t9 = new ETD[] { new ETD(true, "absent", "system::packages/ssb-ldap-schema/ensure", "1.143-1", "/st/st-dev1-ebs1/ldp.yaml")				
		};
		
		ETD[] t10 = new ETD[] { new ETD(true, "beforeValue", "componentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/dbs.yaml"),
				new ETD(true, "beforeValue", "secondComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/dbs.yaml")
		};
		
		ETD[] t11 = new ETD[] { new ETD(true, "beforeValue", "thirdComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/dbs.yaml"),
				new ETD(true, "beforeValue", "forthComponentLevelStartupAndShutdown", null, "/st/st-dev1-ebs1/dbs.yaml")};
		
		ETD[] t12 = new ETD[] { new ETD(true, "absent", "system::packages/ssb-db-schema/ensure", "1.376.293-1", "/st/st-dev1-ebs1/dbs.yaml") };
		
		ETD[] t13 = new ETD[] { new ETD(true, "afterValue", "componentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/dbs.yaml"),
				new ETD(true, "afterValue", "secondComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/dbs.yaml")
		};
		
		ETD[] t14 = new ETD[] { new ETD(true, "afterValue", "thirdComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/dbs.yaml"),
				new ETD(true, "afterValue", "forthComponentLevelStartupAndShutdown", "beforeValue", "/st/st-dev1-ebs1/dbs.yaml")};
		
		ETD[] t15 = new ETD[] { new ETD(true, "stepThreeAValue", "stepThreeAInjectPath", null, "/st/st-dev1-ebs1/dbs.yaml"), 
				new ETD(true, "stepThreeBValue", "stepThreeBInjectPath", null, "/st/st-dev1-ebs1/dbs.yaml") 
		};		
		
		ChainDeploymentVerification.verify(dep, null, 16, new int[] { 1, 2, 2, 2, 1, 2, 2, 1, 1, 1, 2, 2, 1, 2, 2, 2}, t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15);
	}	
	
	/**
	 * Test case for a complete and real-world deployment of CDP DOS into SST1-CDP1
	 * @throws Exception
	 */
	@Test
	public void ebsad12005_CDPSOA() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12005-CDPSOA");

		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t0 = new ETD[] { new ETD(true, "1.0.193-1", "system::packages/EnvironmentConfiguration-Services/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.193-1", "system::packages/EnvironmentConfiguration-Services/ensure", "absent", "/st/st-sst1-cdp1/soatzm02.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.15-1", "system::packages/cdp-soa-audit-logging-config-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.15-1", "system::packages/cdp-soa-audit-logging-config-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm02.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.12-1", "system::packages/cdp-soa-security-config-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.46-1", "system::packages/cdp-soa-classpath-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml")
		};
		
		ETD[] t1 = new ETD[] { new ETD(true, "stopped", "system::services/weblogic-cdp/ensure", "running", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml") };
		
		ETD[] t2 = new ETD[] { new ETD(true, "running", "system::services/weblogic-cdp/ensure", "stopped", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, true, "system::services/weblogic-cdp/enable", null, "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml")};
		
		ETD[] t3 = new ETD[] { new ETD(true, "1.0.46-1", "system::packages/cdp-soa-classpath-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm02.st-sst1-cdp1.ipt.local.yaml") };
		
		ETD[] t4 = new ETD[] { new ETD(true, "stopped", "system::services/weblogic-cdp/ensure", "running", "/st/st-sst1-cdp1/soatzm02.st-sst1-cdp1.ipt.local.yaml") };
		
		ETD[] t5 = new ETD[] { new ETD(true, "running", "system::services/weblogic-cdp/ensure", "stopped", "/st/st-sst1-cdp1/soatzm02.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, true, "system::services/weblogic-cdp/enable", null, "/st/st-sst1-cdp1/soatzm02.st-sst1-cdp1.ipt.local.yaml")};
		
		ETD[] t6 = new ETD[] { new ETD(true, "1.0.168-1", "system::packages/SOAMETADATA-Services/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.161-1", "system::packages/addressmanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.34-1", "system::packages/biometricmanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.51-1", "system::packages/etlbatchmanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.57-1", "system::packages/imagemanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.119-1", "system::packages/partymanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.83-1", "system::packages/processmanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.59-1", "system::packages/referencedatamanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.67-1", "system::packages/searchmanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.144-1", "system::packages/servicedeliverymanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.103-1", "system::packages/systemmanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.106-1", "system::packages/eventmanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.87-1", "system::packages/documentmanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"),
				new ETD(true, "1.0.113-1", "system::packages/utilitymanagement-rpm/ensure", "absent", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml")};
		
		String csv = "EnvironmentConfiguration-Services,1.0.193-1,groupid,EnvironmentConfiguration-Services,1.0.193,war,";
		csv += "\nSOAMETADATA-Services,1.0.168-1,groupid,SOAMETADATA-Services,1.0.168,war,";
		csv += "\naddressmanagement-rpm,1.0.161-1,groupid,addressmanagement-rpm,1.0.161,war,";
		csv += "\nbiometricmanagement-rpm,1.0.34-1,groupid,biometricmanagement-rpm,1.0.34,war,";
		csv += "\ncdp-soa-audit-logging-config-rpm,1.0.15-1,groupid,cdp-soa-audit-logging-config-rpm,1.0.15,war,";
		csv += "\ncdp-soa-classpath-rpm,1.0.46-1,groupid,cdp-soa-classpath-rpm,1.0.46,war,";
		csv += "\ncdp-soa-security-config-rpm,1.0.12-1,groupid,cdp-soa-security-config-rpm,1.0.12,war,";
		csv += "\ndocumentmanagement-rpm,1.0.87-1,groupid,documentmanagement-rpm,1.0.87,war,";
		csv += "\netlbatchmanagement-rpm,1.0.51-1,groupid,etlbatchmanagement-rpm,1.0.51,war,";
		csv += "\neventmanagement-rpm,1.0.106-1,groupid,eventmanagement-rpm,1.0.106,war,";
		csv += "\nimagemanagement-rpm,1.0.57-1,groupid,imagemanagement-rpm,1.0.57,war,";
		csv += "\npartymanagement-rpm,1.0.119-1,groupid,partymanagement-rpm,1.0.119,war,";
		csv += "\nprocessmanagement-rpm,1.0.83-1,groupid,processmanagement-rpm,1.0.83,war,";
		csv += "\nreferencedatamanagement-rpm,1.0.59-1,groupid,referencedatamanagement-rpm,1.0.59,war,";
		csv += "\nsearchmanagement-rpm,1.0.67-1,groupid,searchmanagement-rpm,1.0.67,war,";
		csv += "\nservicedeliverymanagement-rpm,1.0.144-1,groupid,servicedeliverymanagement-rpm,1.0.144,war,";
		csv += "\nsystemmanagement-rpm,1.0.103-1,groupid,systemmanagement-rpm,1.0.103,war,";
		csv += "\nutilitymanagement-rpm,1.0.113-1,groupid,utilitymanagement-rpm,1.0.113,war,";
		csv += "\n";
		
		ChainDeploymentVerification.verify(dep, csv, 7, new int[] { 6, 1, 2, 1, 1, 2, 14 }, t0, t1, t2, t3, t4, t5, t6);
	}
	
	
	/**
	 * Test case for a complete and real-world deployment of a CDP upgrade of components in  SST1-CDP1
	 */
	@Test
	public void ebsad12492CdpUpgrade() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12492-cdp_upgrade");

		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t0 = new ETD[] { new ETD(true, "absent", "system::packages/oer_live_db_21.0/ensure", "1.72-1", "/st/st-sst1-cdp1/dbstzm01.st-sst1-cdp1.ipt.local.yaml") };
		
		ETD[] t1 = new ETD[] { new ETD(true, "stopped", "system::services/weblogic-cdp/ensure", "running", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"), 
							   new ETD(true, "stopped", "system::services/weblogic-cdp/ensure", "running", "/st/st-sst1-cdp1/soatzm02.st-sst1-cdp1.ipt.local.yaml")};
		
		ETD[] t2 = new ETD[] { new ETD(true, "1.75-1", "system::packages/oer_live_db_21.0/ensure", "absent", "/st/st-sst1-cdp1/dbstzm01.st-sst1-cdp1.ipt.local.yaml") };

		ETD[] t3 = new ETD[] { new ETD(true, "running", "system::services/weblogic-cdp/ensure", "stopped", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"), 
				   			   new ETD(true, "running", "system::services/weblogic-cdp/ensure", "stopped", "/st/st-sst1-cdp1/soatzm02.st-sst1-cdp1.ipt.local.yaml"),
				   			   new ETD(true, true, "system::services/weblogic-cdp/enable", "true", "/st/st-sst1-cdp1/soatzm01.st-sst1-cdp1.ipt.local.yaml"), 
				   			   new ETD(true, true, "system::services/weblogic-cdp/enable", "true", "/st/st-sst1-cdp1/soatzm02.st-sst1-cdp1.ipt.local.yaml")};

		
		String csv = "oer_live_db_21.0,1.75-1,groupid,oer_live_db_21.0,1.75,war,\n";
		
		ChainDeploymentVerification.verify(dep, csv, 4, new int[] { 1, 2, 1, 4 }, t0, t1, t2, t3);
	}
	
	
	/**
	 * Complete install of all SSB components where the deployment descriptor uses schemes to determine the target hosts for
	 * each component in a particular environment.
	 */
	@Test
	public void ebsad9583SsbCompleteDeployToHostsInDeploymentDescriptorSchemes() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD_9583-deployment-descriptor-schemes");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"1.376.293-1","system::packages/ssb-db-schema/ensure","absent","/st/st-dev1-ebs1/db1.yaml"),
							  new ETD(true,"1.376.293-1","system::packages/ssb-db-schema/ensure","absent","/st/st-dev1-ebs1/db2.yaml")};
		ETD[] t2 = new ETD[]{ new ETD(true,"{require=Package[ssb_cfg_openldap], tag=mwconfig, ensure=1.143-1}","system::packages/ssb-ldap-schema","{ensure=absent, require=Package[ssb_cfg_openldap], tag=appdeploy}","/st/st-dev1-ebs1/ldp.yaml")};
		ETD[] t3 = new ETD[]{ new ETD(true,"{require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], ensure=2.0.3-1}","system::packages/ssb-rpm-nexus-baseline-config","{ensure=absent, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], tag=mwconfig}","/st/st-dev1-ebs1/rma.yaml"),
				new ETD(true,"{tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Mount[/var/sonatype-work]], ensure=2.1.41-release_2.1.41_1}","system::packages/ssb-core-features-lib-nexus","{ensure=absent, tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Package[gen_bin_nexus], Mount[/var/sonatype-work]]}","/st/st-dev1-ebs1/rma.yaml")	 			 
		};
		ETD[] t4 = new ETD[]{ new ETD(true,"{require=[Package[gen-ins-jboss-fuse], File[/opt/fuse]], ensure=2.0.294-1}","system::packages/ssb-rpm-fuse-config","{ensure=absent, require=Package[gen-ins-jboss-fuse], tag=mwconfig}","/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true,"{require=Package[ssb-rpm-fuse-config], tag=mwconfig, ensure=2.1.119-1}","system::packages/ssb-core-features-fuse-config","{ensure=absent, require=Package[ssb-rpm-fuse-config], tag=mwdeploy}","/st/st-dev1-ebs1/ssb.yaml"),
	 			new ETD(true,"{require=Package[ssb-core-features-fuse-config], tag=mwconfig, ensure=2.1.119-1}","system::packages/ssb-core-features-fuse-application","{ensure=absent, require=Package[ssb-core-features-fuse-config], tag=mwdeploy}","/st/st-dev1-ebs1/ssb.yaml")
        };	
		
		String csv = "ssb-core-features-fuse-application,2.1.119-1,groupid,ssb-core-features-fuse-application,2.1.119,war,"
		+ "\nssb-core-features-fuse-config,2.1.119-1,groupid,ssb-core-features-fuse-config,2.1.119,war,"
		+ "\nssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,groupid,ssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,war,"
		+ "\nssb-db-schema,1.376.293-1,groupid,ssb-db-schema,1.376.293,war,"
		+ "\nssb-ldap-schema,1.143-1,groupid,ssb-ldap-schema,1.143,war,"
		+ "\nssb-rpm-fuse-config,2.0.294-1,groupid,ssb-rpm-fuse-config,2.0.294,war,"
		+ "\nssb-rpm-nexus-baseline-config,2.0.3-1,groupid,ssb-rpm-nexus-baseline-config,2.0.3,war,"
		+ "\n";
				
		ChainDeploymentVerification.verify(dep, csv, 4, new int[]{2,1,2,3}, t1, t2, t3, t4);
	}
	
	
	/**
	 * Complete install of all SSB components over 4 transitions (components not in YAML already)
	 * @throws Exception
	 */
	@Test
	public void ebsad9727SsbCompleteFreshDeploy() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-9727-SSB-Fresh-Deploy");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"{ensure=1.376.293-1, require=[Package[jdk], Service[postgresql-9.2]], tag=mwdeploy}","system::packages/ssb-db-schema",null,"/st/st-dev1-ebs1/dbs.yaml")};
		ETD[] t2 = new ETD[]{ new ETD(true,"{ensure=1.143-1, require=Package[ssb_cfg_openldap], tag=mwconfig}","system::packages/ssb-ldap-schema",null,"/st/st-dev1-ebs1/ldp.yaml")};
		ETD[] t3 = new ETD[]{ new ETD(true,"{ensure=2.0.3-1, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]]}","system::packages/ssb-rpm-nexus-baseline-config",null,"/st/st-dev1-ebs1/rma.yaml"),
				new ETD(true,"{ensure=2.1.41-release_2.1.41_1, require=[Package[ssb-rpm-nexus-baseline-config], Mount[/var/sonatype-work]], tag=appdeploy}","system::packages/ssb-core-features-lib-nexus",null,"/st/st-dev1-ebs1/rma.yaml")	 			 
		};
		ETD[] t4 = new ETD[]{ new ETD(true,"{ensure=2.0.294-1, require=[Package[gen-ins-jboss-fuse], File[/opt/fuse]]}","system::packages/ssb-rpm-fuse-config",null,"/st/st-dev1-ebs1/ssb.yaml"),
				new ETD(true,"{ensure=2.1.119-1, require=Package[ssb-rpm-fuse-config], tag=mwconfig}","system::packages/ssb-core-features-fuse-config",null,"/st/st-dev1-ebs1/ssb.yaml"),
	 			new ETD(true,"{ensure=2.1.119-1, require=Package[ssb-core-features-fuse-config], tag=mwconfig}","system::packages/ssb-core-features-fuse-application",null,"/st/st-dev1-ebs1/ssb.yaml")
        };	
		
		String csv = "ssb-core-features-fuse-application,2.1.119-1,groupid,ssb-core-features-fuse-application,2.1.119,war,"
		+ "\nssb-core-features-fuse-config,2.1.119-1,groupid,ssb-core-features-fuse-config,2.1.119,war,"
		+ "\nssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,groupid,ssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,war,"
		+ "\nssb-db-schema,1.376.293-1,groupid,ssb-db-schema,1.376.293,war,"
		+ "\nssb-ldap-schema,1.143-1,groupid,ssb-ldap-schema,1.143,war,"
		+ "\nssb-rpm-fuse-config,2.0.294-1,groupid,ssb-rpm-fuse-config,2.0.294,war,"
		+ "\nssb-rpm-nexus-baseline-config,2.0.3-1,groupid,ssb-rpm-nexus-baseline-config,2.0.3,war,"
		+ "\n";
				
		ChainDeploymentVerification.verify(dep, csv, 4, new int[]{1,1,2,3}, t1, t2, t3, t4);
	}
	
	
	/**
	 * Complete uninstall of all SSB components over 4 transitions
	 * @throws Exception
	 */
	@Test
	public void ebsad11507SsbRpmFuseConfig() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11507");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"absent","system::packages/ssb-core-features-lib-nexus/ensure","2.1.385-1","/st/st-sst1-ssb1/rma.yaml"),
	 			              new ETD(true,"absent","system::packages/ssb-core-features-fuse-application/ensure","2.1.385-1","/st/st-sst1-ssb1/ssb.yaml"),
                              new ETD(true,"absent","system::packages/ssb-core-features-fuse-config/ensure","2.1.385-1","/st/st-sst1-ssb1/ssb.yaml"),
		};
		
		ETD[] t2 = new ETD[]{ new ETD(true,"absent","system::packages/ssb-ldap-schema/ensure","1.160-1","/st/st-sst1-ssb1/ldp.yaml")};
		
		ETD[] t3 = new ETD[]{ new ETD(true,"absent","system::packages/ssb-db-schema/ensure","1.376.336-1","/st/st-sst1-ssb1/dbs.yaml")};
		
		ETD[] t4 = new ETD[]{ new ETD(true,"1.376.336-1","system::packages/ssb-db-schema/ensure","absent","/st/st-sst1-ssb1/dbs.yaml")};
		
		ETD[] t5 = new ETD[]{ new ETD(true,"{require=Package[ssb_cfg_openldap], tag=mwconfig, ensure=1.147-1}","system::packages/ssb-ldap-schema","{ensure=absent, require=Package[ssb_cfg_openldap], tag=appdeploy}","/st/st-sst1-ssb1/ldp.yaml"),
							  new ETD(true,"2.1.349-1", "system::packages/ssb-core-features-fuse-config/ensure","absent","/st/st-sst1-ssb1/ssb.yaml")};
		
		ETD[] t6 = new ETD[]{ new ETD(true,"{require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], ensure=2.0.11-1}","system::packages/ssb-rpm-nexus-baseline-config","{ensure=absent, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], tag=mwconfig}","/st/st-sst1-ssb1/rma.yaml"),
							  new ETD(true,"2.1.349-1","system::packages/ssb-core-features-fuse-application/ensure","absent","/st/st-sst1-ssb1/ssb.yaml"),
							  new ETD(true,"{tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Mount[/var/sonatype-work]], ensure=2.1.349-1}","system::packages/ssb-core-features-lib-nexus","{ensure=absent, tag=appdeploy, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]]}","/st/st-sst1-ssb1/rma.yaml")};
		
		String csv = "ssb-core-features-fuse-application,2.1.349-1,groupid,ssb-core-features-fuse-application,2.1.349,war,"
				+ "\nssb-core-features-fuse-config,2.1.349-1,groupid,ssb-core-features-fuse-config,2.1.349,war,"
				+ "\nssb-core-features-lib-nexus,2.1.349-1,groupid,ssb-core-features-lib-nexus,2.1.349,war,"
				+ "\nssb-db-schema,1.376.336-1,groupid,ssb-db-schema,1.376.336,war,"
				+ "\nssb-ldap-schema,1.147-1,groupid,ssb-ldap-schema,1.147,war,"
				+ "\nssb-rpm-nexus-baseline-config,2.0.11-1,groupid,ssb-rpm-nexus-baseline-config,2.0.11,war,"
				+ "\n";
		
		ChainDeploymentVerification.verify(dep, csv, 6, new int[]{3,1,1,1,2,3}, t1, t2, t3, t4, t5, t6);
	}
	
	
	/**
	 * Tests uninstalling from only those nodes that fall within the schemes scope for a given environment.
	 */
	@Test
	public void ebsad11654ScopeSpecificUndeploy() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11654-scoped_undeploy");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), "Magical Trevor");
		
		ETD[] t1 = new ETD[]{ new ETD(true,"absent","system::packages/ssb-core-features-fuse-application/ensure","2.1.119-1","/st/st-dev1-ebs1/ssb1.yaml"),
							  new ETD(true,"absent","system::packages/ssb-core-features-fuse-config/ensure","2.1.119-1","/st/st-dev1-ebs1/ssb1.yaml")};
		
		ETD[] t2 = new ETD[]{new ETD(true,"absent","system::packages/ssb-rpm-fuse-config/ensure","2.0.294-1","/st/st-dev1-ebs1/ssb1.yaml")};
		
		ETD[] t3 = new ETD[]{ new ETD(true,"absent","system::packages/ssb-core-features-lib-nexus/ensure","2.1.41-release_2.1.41_1","/st/st-dev1-ebs1/rma.yaml")};
		
		ETD[] t4 = new ETD[]{new ETD(true,"absent","system::packages/ssb-rpm-nexus-baseline-config/ensure","2.0.3-1","/st/st-dev1-ebs1/rma.yaml")};
		ETD[] t5 = new ETD[]{ new ETD(true,"absent","system::packages/ssb-ldap-schema/ensure","1.143-1","/st/st-dev1-ebs1/ldp.yaml")};

		// This transition shouldn't occur as dbs1 is not in scope for the chosen scheme
		// ETD[] t4 = new ETD[]{ new ETD(true,"absent","system::packages/ssb-db-schema/ensure","1.376.293-1","/st/st-dev1-ebs1/dbs1.yaml")};
		
		ChainDeploymentVerification.verify(dep, null, 5, new int[]{2,1,1,1,1}, t1, t2, t3, t4, t5);
	}
	
	
	/**
	 * Tests deploying to only those nodes that fall within the schemes scope for a given environment.
	 * Also tests undeploy of component in scope, but not in deployment descriptor - this triggers
	 * whole chain redeploy behaviour.
	 */
	@Test
	public void ebsad11654ScopeSpecificDeploy() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11654-scoped_deploy");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), "Magical Trevor");
		
		ETD[] t0 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/ssb-core-features-fuse-application/ensure","2.1.119-1","/st/st-dev1-ebs1/ssb1.yaml"),
			new ETD(true,HieraData.ABSENT,"system::packages/ssb-core-features-fuse-config/ensure","2.1.119-1","/st/st-dev1-ebs1/ssb1.yaml"),
		};
		ETD[] t1 = new ETD[] {new ETD(true,HieraData.ABSENT,"system::packages/ssb-rpm-fuse-config/ensure","2.0.294-1","/st/st-dev1-ebs1/ssb1.yaml")};
		ETD[] t2 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/ssb-core-features-lib-nexus/ensure","2.1.41-release_2.1.41_1","/st/st-dev1-ebs1/rma.yaml")
		};
		ETD[] t3 = new ETD[] {
				new ETD(true,HieraData.ABSENT,"system::packages/ssb-rpm-nexus-baseline-config/ensure","2.0.3-1","/st/st-dev1-ebs1/rma.yaml")
		};
		ETD[] t4 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/ssb-ldap-schema/ensure","1.143-1","/st/st-dev1-ebs1/ldp.yaml"),
		};
		ETD[] t5 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/ssb-db-schema/ensure","1.376.293-1","/st/st-dev1-ebs1/ldp.yaml"),
		};
		ETD[] t6 = new ETD[]{
			new ETD(true,"{ensure=1.376.298-1, require=[Package[jdk], Service[postgresql-9.2]], tag=mwdeploy}","system::packages/ssb-db-schema",null,"/st/st-dev1-ebs1/ssb1.yaml"),
		};
		ETD[] t7 = new ETD[]{
			new ETD(true,"{require=Package[ssb_cfg_openldap], tag=mwconfig, ensure=1.143-1}","system::packages/ssb-ldap-schema","{ensure=absent, require=Package[ssb_cfg_openldap], tag=appdeploy}","/st/st-dev1-ebs1/ldp.yaml"),
		};
		ETD[] t8 = new ETD[]{
			new ETD(true,"{require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], ensure=2.0.8-1}","system::packages/ssb-rpm-nexus-baseline-config","{ensure=absent, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], tag=mwconfig}","/st/st-dev1-ebs1/rma.yaml"),
			new ETD(true,"{tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Mount[/var/sonatype-work]], ensure=2.1.41-release_2.1.41_1}","system::packages/ssb-core-features-lib-nexus","{ensure=absent, tag=appdeploy, require=[Package[ssb-rpm-nexus-baseline-config], Package[gen_bin_nexus], Mount[/var/sonatype-work]]}","/st/st-dev1-ebs1/rma.yaml"),
		};
		ETD[] t9 = new ETD[]{
			new ETD(true,"{require=[Package[gen-ins-jboss-fuse], File[/opt/fuse]], ensure=2.0.294-1}","system::packages/ssb-rpm-fuse-config","{ensure=absent, require=Package[gen-ins-jboss-fuse], tag=mwconfig}","/st/st-dev1-ebs1/ssb1.yaml"),
			new ETD(true,"{require=Package[ssb-rpm-fuse-config], tag=mwconfig, ensure=2.1.119-1}","system::packages/ssb-core-features-fuse-config","{ensure=absent, require=Package[ssb-rpm-fuse-config], tag=mwdeploy}","/st/st-dev1-ebs1/ssb1.yaml"),
			new ETD(true,"{tag=mwconfig, ensure=2.1.119-1}","system::packages/ssb-core-features-fuse-application","{ensure=absent, require=Package[ssb-core-features-fuse-config], tag=mwdeploy}","/st/st-dev1-ebs1/ssb1.yaml"),
		};
		
		String csv = "ssb-core-features-fuse-application,2.1.119-1,groupid,ssb-core-features-fuse-application,2.1.119-1,war,\n"
				+ "ssb-core-features-fuse-config,2.1.119-1,groupid,ssb-core-features-fuse-config,2.1.119-1,war,\n"
				+ "ssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,groupid,ssb-core-features-lib-nexus,2.1.41-release_2.1.41_1,war,\n"
				+ "ssb-db-schema,1.376.298-1,groupid,ssb-db-schema,1.376.298-1,war,\n"
				+ "ssb-ldap-schema,1.143-1,groupid,ssb-ldap-schema,1.143-1,war,\n"
				+ "ssb-rpm-fuse-config,2.0.294-1,groupid,ssb-rpm-fuse-config,2.0.294-1,war,\n"
				+ "ssb-rpm-nexus-baseline-config,2.0.8-1,groupid,ssb-rpm-nexus-baseline-config,2.0.8-1,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 10, new int[]{2, 1, 1, 1, 1, 1, 1, 1, 2, 3}, t0, t1, t2, t3, t4, t5, t6, t7, t8, t9);
	}
	
	/**
	 * Tests deploying to only those nodes that fall within the schemes scope for a given environment.
	 */
	@Test
	public void ebsad11654DeployOutOfScope() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11654-scheme-deploy-oos");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, "HO_IPT_NP_PRP2_DAZO", null);
		
		ETD[] t0 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/lr_sis_8.0/ensure","1.37-1","/np/np-prp2-dazo/dbsem51.np-prp2-dazo.ipt.ho.local.yaml"),
		};
		ETD[] t1 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/lr_sis_3.0/ensure","1.10-1","/np/np-prp2-dazo/dbsem51.np-prp2-dazo.ipt.ho.local.yaml"),	
		};
		ETD[] t2 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/lr_sis_2.0/ensure","1.6-1","/np/np-prp2-dazo/dbsem51.np-prp2-dazo.ipt.ho.local.yaml"),	
		};
		ETD[] t3 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/lr_sis_conf/ensure","1.46-1","/np/np-prp2-dazo/dbsem51.np-prp2-dazo.ipt.ho.local.yaml"),	
		};
		ETD[] t4 = new ETD[]{
			new ETD(true,"{tag=appdeploy, ensure=1.46-1}","system::packages/lr_sis_conf","{ensure=absent, require=Package[cdp_cfg_db_SISR], tag=appdeploy}","/np/np-prp2-dazo/dbsem51.np-prp2-dazo.ipt.ho.local.yaml"),
			new ETD(true,"1.8-1","system::packages/lr_sis_2.0/ensure",HieraData.ABSENT,"/np/np-prp2-dazo/dbsem51.np-prp2-dazo.ipt.ho.local.yaml"),
			new ETD(true,"1.12-1","system::packages/lr_sis_3.0/ensure",HieraData.ABSENT,"/np/np-prp2-dazo/dbsem51.np-prp2-dazo.ipt.ho.local.yaml"),
			new ETD(true,"1.61-1","system::packages/lr_sis_8.0/ensure",HieraData.ABSENT,"/np/np-prp2-dazo/dbsem51.np-prp2-dazo.ipt.ho.local.yaml"),
		};
		String csv = "lr_sis_2.0,1.8-1,groupid,lr_sis_2.0,1.8-1,war,\n"
				+ "lr_sis_3.0,1.12-1,groupid,lr_sis_3.0,1.12-1,war,\n"
				+ "lr_sis_8.0,1.61-1,groupid,lr_sis_8.0,1.61-1,war,\n"
				+ "lr_sis_conf,1.46-1,groupid,lr_sis_conf,1.46-1,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 5, new int[]{1,1,1,1,4}, t0, t1, t2, t3, t4);
	}
	
	
	/**
	 * Tests deploying to only those nodes that fall within the schemes scope for a given environment.
	 */
	@Test
	public void ebsad12000DeployOutOfScope() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-12000-Undeploy_out_of_scope");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, "HO_IPT_NP_PRP1_DAZO", null);
		
		ETD[] t0 = new ETD[]{
			new ETD(true,HieraData.ABSENT,"system::packages/lr_crs_conf/ensure","1.9-1","/np/np-prp1-dazo/dbsem31.np-prp1-dazo.ipt.ho.local.yaml"),
		};
		ChainDeploymentVerification.verify(dep, null, 1, new int[]{1}, t0);
	}
	
	
	/**
	 * Complete uninstall of all OER components where the DD defines dbstzm01 but they all also exist in dbstzm03
	 * @throws Exception
	 */
	@Test
	public void ebsad10564undeployAbsentIssue() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-10564-Undeploy-Absent-Issue");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"absent","system::packages/oer_stg_crs_sw/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
							  new ETD(true,"absent","system::packages/oer_stg_crs_sw/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t2 = new ETD[]{ new ETD(true,"absent","system::packages/oer_stg_core_sw/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				              new ETD(true,"absent","system::packages/oer_stg_core_sw/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t3 = new ETD[]{ new ETD(true,"absent","system::packages/oer_stg_crs_20.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				              new ETD(true,"absent","system::packages/oer_stg_crs_20.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t4 = new ETD[]{ new ETD(true,"absent","system::packages/oer_stg_crs_19.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				              new ETD(true,"absent","system::packages/oer_stg_crs_19.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t5 = new ETD[]{ new ETD(true,"absent","system::packages/oer_stg_crs_18.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				              new ETD(true,"absent","system::packages/oer_stg_crs_18.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t6 = new ETD[]{ new ETD(true,"absent","system::packages/oer_stg_crs_17.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				              new ETD(true,"absent","system::packages/oer_stg_crs_17.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t7 = new ETD[]{ new ETD(true,"absent","system::packages/oer_stg_crs_16.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				              new ETD(true,"absent","system::packages/oer_stg_crs_16.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t8 = new ETD[]{ new ETD(true,"absent","system::packages/oer_stg_crs_base/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				              new ETD(true,"absent","system::packages/oer_stg_crs_base/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t9 = new ETD[]{ new ETD(true,"absent","system::packages/oer_live_sw/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				              new ETD(true,"absent","system::packages/oer_live_sw/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t10 = new ETD[]{ new ETD(true,"absent","system::packages/oer_live_db_20.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				               new ETD(true,"absent","system::packages/oer_live_db_20.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t11 = new ETD[]{ new ETD(true,"absent","system::packages/oer_live_db_19.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				               new ETD(true,"absent","system::packages/oer_live_db_19.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t12 = new ETD[]{ new ETD(true,"absent","system::packages/oer_live_db_18.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				               new ETD(true,"absent","system::packages/oer_live_db_18.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t13 = new ETD[]{ new ETD(true,"absent","system::packages/oer_live_db_17.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				               new ETD(true,"absent","system::packages/oer_live_db_17.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t14 = new ETD[]{ new ETD(true,"absent","system::packages/oer_live_db_16.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				               new ETD(true,"absent","system::packages/oer_live_db_16.0/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t15 = new ETD[]{ new ETD(true,"absent","system::packages/oer_live_sys_obj/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				               new ETD(true,"absent","system::packages/oer_live_sys_obj/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ETD[] t16 = new ETD[]{ new ETD(true,"absent","system::packages/oer_live_cancel_batches/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				               new ETD(true,"absent","system::packages/oer_live_cancel_batches/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml"),
				               new ETD(true,"absent","system::packages/oer_live_db_conf/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				               new ETD(true,"absent","system::packages/oer_live_db_conf/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml"),
				               new ETD(true,"absent","system::packages/oer_stg_crs_conf/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm01.yaml"),
				               new ETD(true,"absent","system::packages/oer_stg_crs_conf/ensure","1.0.0-1","/st/st-dev1-ebs1/dbstzm03.yaml")};
		
		ChainDeploymentVerification.verify(dep, null, 16, new int[]{2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,6}, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16);
	}
	
	
	/**
	 * Checks that the correct exception is thrown when a component's host can't be resolved.
	 */
	@Test
	public void ebsad11654NoCorrespondingSchemeForEnvironment() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11654-no_corresponding_scheme");
		try {
			Deployer d = new Deployer();
			d.deploy(appVersion, "HO_IPT_NP_PRP1_ESZO", null);
			fail();
		} catch (Exception e) {
			assertEquals("Incorrect exception thrown", "No changes can be made, deployment halted!", e.getMessage());
		}
	}
	
	
	/**
	 * Checks that the code can cope with a scheme name being supplied that doesn't match any of the schemes
	 * defined in the deployment descriptor.
	 */
	@Test
	public void ebsad11654SchemesWithNoScope() throws Exception {
		try {
			ApplicationVersion appVersion = setUpData("EBSAD-11654-schemes_with_no_scope");
			Deployer d = new Deployer();
			d.deploy(appVersion, createEnvironmentName(), "lucifer");
			fail();
		} catch (Exception e) {
			assertEquals("Incorrect exception thrown", "No changes can be made, deployment halted!", e.getMessage());
		}
	}
	
	/**
	 * Should not throw a plan -1 exception
	 */
	@Test
	public void ebsad11942PlanException() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11942-plan-1");
		Deployer d = new Deployer();
		d.deploy(appVersion, createEnvironmentName(), null);
	}

	/**
	 * Shouldn't throw a deploy plan -1 error.
	 */
	@Test
	public void ebsad11934DeployPlan1() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11934-deploy-plan-1");
		Deployer d = new Deployer();
		d.deploy(appVersion, createEnvironmentName(), null);
	}
	
	/**
	 * Test whole-chain-multi-transition behaviour by installing TAL components into an environment where they all exist as absent
	 * @throws Exception
	 */
	@Test
	public void testWholeChainMultiTransition() throws Exception {
		ApplicationVersion appVersion = setUpData("WholeChainMultiTransition");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"1.0.0-1","system::packages/cdp-talend-context-dbschema-rpm/ensure","absent","/st/st-sst1-cdp1/dbstzm01.st-sst1-cdp1.ipt.local.yaml")};
		
		ETD[] t2 = new ETD[]{ new ETD(true,"1.0.0-1","system::packages/cdp-talend-tac-config-rpm/ensure","absent","/st/st-sst1-cdp1/etc.yaml")};
		
		ETD[] t3 = new ETD[]{ new ETD(true,"1.0.0-1","system::packages/cdp-talend-jobserver-config-rpm/ensure","absent","/st/st-sst1-cdp1/etl.yaml")};
		
		ETD[] t4 = new ETD[]{ new ETD(true,"1.0.0-1","system::packages/cdp-tal-crsaddr-rpm/ensure","absent","/st/st-sst1-cdp1/etc.yaml"),
				new ETD(true,"1.0.0-1","system::packages/cdp-tal-crsdoc-rpm/ensure","absent","/st/st-sst1-cdp1/etc.yaml"),
				new ETD(true,"1.0.0-1","system::packages/cdp-tal-crsevt-rpm/ensure","absent","/st/st-sst1-cdp1/etc.yaml"),
				new ETD(true,"1.0.0-1","system::packages/cdp-tal-crspty-rpm/ensure","absent","/st/st-sst1-cdp1/etc.yaml"),
				new ETD(true,"1.0.0-1","system::packages/cdp-tal-crssd-rpm/ensure","absent","/st/st-sst1-cdp1/etc.yaml"),
				new ETD(true,"1.0.0-1","system::packages/cdp-tal-crsxref-rpm/ensure","absent","/st/st-sst1-cdp1/etc.yaml"),
				new ETD(true,"1.0.0-1","system::packages/cdp-tal-iptmap-rpm/ensure","absent","/st/st-sst1-cdp1/etc.yaml"),
				new ETD(true,"1.0.0-1","system::packages/cdp-tal-iptref-rpm/ensure","absent","/st/st-sst1-cdp1/etc.yaml"),
				new ETD(true,"1.0.0-1","system::packages/cdp-tal-scheduling-rpm/ensure","absent","/st/st-sst1-cdp1/etc.yaml")};		
		
		String csv = "cdp-tal-crsaddr-rpm,1.0.0-1,groupid,cdp-tal-crsaddr-rpm,1.0.0,war,\n";
			csv += "cdp-tal-crsdoc-rpm,1.0.0-1,groupid,cdp-tal-crsdoc-rpm,1.0.0,war,\n";
			csv += "cdp-tal-crsevt-rpm,1.0.0-1,groupid,cdp-tal-crsevt-rpm,1.0.0,war,\n";
			csv += "cdp-tal-crspty-rpm,1.0.0-1,groupid,cdp-tal-crspty-rpm,1.0.0,war,\n";
			csv += "cdp-tal-crssd-rpm,1.0.0-1,groupid,cdp-tal-crssd-rpm,1.0.0,war,\n";
			csv += "cdp-tal-crsxref-rpm,1.0.0-1,groupid,cdp-tal-crsxref-rpm,1.0.0,war,\n";
			csv += "cdp-tal-iptmap-rpm,1.0.0-1,groupid,cdp-tal-iptmap-rpm,1.0.0,war,\n";
			csv += "cdp-tal-iptref-rpm,1.0.0-1,groupid,cdp-tal-iptref-rpm,1.0.0,war,\n";
			csv += "cdp-tal-scheduling-rpm,1.0.0-1,groupid,cdp-tal-scheduling-rpm,1.0.0,war,\n";
			csv += "cdp-talend-context-dbschema-rpm,1.0.0-1,groupid,cdp-talend-context-dbschema-rpm,1.0.0,war,\n";
			csv += "cdp-talend-jobserver-config-rpm,1.0.0-1,groupid,cdp-talend-jobserver-config-rpm,1.0.0,war,\n";
			csv += "cdp-talend-tac-config-rpm,1.0.0-1,groupid,cdp-talend-tac-config-rpm,1.0.0,war,\n";
		
			ChainDeploymentVerification.verify(dep, csv, 4, new int[]{1,1,1,9}, t1, t2, t3, t4);
	}
	
	/**
	 * Fail if a component is duplicated in the DD.
	 * @throws Exception
	 */
	@Test
	public void ebsad10630DuplicatedComponent() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-10630-duplicated-component");
		try {
			Deployer d = new Deployer();
			d.deploy(appVersion, createEnvironmentName(), null);
		} catch (Exception e) {
			// doesn't seem to be a way to check the FAIL on the component
			assertEquals("Exception expected", "No changes can be made, deployment halted!", e.getMessage());
		}
	}
	
	/**
	 * Tests upgrading of a single chain which has a number of sibling chains, ie.
	 * 
	 * <pre>
	 * Grandaddy
	 *   |__Daddy
	 *      |__Child 1
	 *      	|__Grandchild 1
	 *      |__Child 2
	 *      	|__Grandchild 2
	 *      |__Child 3
	 *      	|__Grandchild 3
	 *      |__Child 4                <-- This one
	 *      	|__Grandchild 4       <-- and this one as part of the same 2-part chain, say
	 *      |__Child 5
	 *      	|__Grandchild 5
	 * </pre>
	 * This test checks that a. Both components of the chain are actioned and b. that the actions appear
	 * as their own transitions as defined in the deployment descriptor.
	 */
	@Test
	public void ebsad11307UpgradeOneOfANumberOfSmallChains() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-11307");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"absent","system::packages/forumsentry-config-ssb-pkiss/ensure","1.0.102-1","/st/st-cit1-sec1/scg.yaml")};
		
		ETD[] t2 = new ETD[]{ new ETD(true,"absent","system::packages/forumsentry-policy-ssb-pkiss/ensure", "1.0.126-1", "/st/st-cit1-sec1/scg.yaml")};
		
		ETD[] t3 = new ETD[]{ new ETD(true,"1.0.127-1","system::packages/forumsentry-policy-ssb-pkiss/ensure","absent","/st/st-cit1-sec1/scg.yaml"),
							  new ETD(true,"1.0.102-1","system::packages/forumsentry-config-ssb-pkiss/ensure","absent","/st/st-cit1-sec1/scg.yaml")};
		
		ChainDeploymentVerification.verify(dep, "forumsentry-config-ssb-pkiss,1.0.102-1,groupid,forumsentry-config-ssb-pkiss,1.0.102,war,\n"
				+ "forumsentry-policy-ssb-pkiss,1.0.127-1,groupid,forumsentry-policy-ssb-pkiss,1.0.127,war,\n", 3, new int[]{1, 1, 2}, t1, t2, t3);
	}
	
	@Test
	public void ebsad14629InsertYamlBlock() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-14629-insert-yaml-block");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"{tag=appdeploy, ensure=1.0.135-1}","system::packages/forumsentry-common","{ensure=absent, tag=mwdeploy}","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
		
		ETD[] t2 = new ETD[]{ new ETD(true,"xyz","system::packages/ipt-appointment-services-rpm/change", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
				   new ETD(true,"[someSubChange, nextSubChange]","system::packages/ipt-appointment-services-rpm/someOtherChange", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
		
				
		ChainDeploymentVerification.verify(dep, 
				"forumsentry-common,1.0.135-1,groupid,forumsentry-common,1.0.135,war,\n"
				, 2, new int[]{1, 2}, t1, t2);
	}
	
	@Test
	public void ebsad15239TestYamlTypes() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-15239-test-yaml-types");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"{tag=appdeploy, ensure=1.0.135-1}","system::packages/forumsentry-common","{ensure=absent, tag=mwdeploy}","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
		
		ETD[] t2 = new ETD[]{ 
				new ETD(true,false,"system::packages/my-test-package/aBoolean", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
				new ETD(true,"false","system::packages/my-test-package/aBooleanAsText", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
				new ETD(true,"[firstItem, secondItem]","system::packages/my-test-package/aList", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
				new ETD(true,5,"system::packages/my-test-package/aNumber", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
				new ETD(true,"5","system::packages/my-test-package/aNumberAsText", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
				new ETD(true,"xyz","system::packages/my-test-package/someText", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")
			};
	
			
		ChainDeploymentVerification.verify(dep, 
			"forumsentry-common,1.0.135-1,groupid,forumsentry-common,1.0.135,war,\n"
			, 2, new int[]{t1.length, t2.length}, t1, t2);
	}
	
	@Test
	public void ebsad15239TestYamlTypesSingleLine() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-15239-test-yaml-types-single-line");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"{tag=appdeploy, ensure=1.0.135-1}","system::packages/forumsentry-common","{ensure=absent, tag=mwdeploy}","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
		
		ETD[] t2 = new ETD[]{ 
				new ETD(true,"xyz","system::packages/my-test-package/someText", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
				new ETD(true,false,"system::packages/my-test-package/aBoolean", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
				new ETD(true,"false","system::packages/my-test-package/aBooleanAsText", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
				new ETD(true,5,"system::packages/my-test-package/aNumber", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml"),
				new ETD(true,"5","system::packages/my-test-package/aNumberAsText", null, "/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")				
			};
	
			
		ChainDeploymentVerification.verify(dep, 
			"forumsentry-common,1.0.135-1,groupid,forumsentry-common,1.0.135,war,\n"
			, 2, new int[]{t1.length, t2.length}, t1, t2);
	}
	
	@Test
	public void ebsad19506AlternateEnvironmentState() throws Exception {
		String scenarioFolder ="EBSAD-19506-alternate-environment-state";
		ApplicationVersion appVersion = setUpData(scenarioFolder);
		//ConfigurationFactory.getProperties().put(Configuration.ALTERNATIVE_ENVIRONMENT_STATE, TestEnvironmentStateManager.class.getCanonicalName());
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"{tag=appdeploy, ensure=1.0.135-1}","system::packages/forumsentry-common","{ensure=absent, tag=mwdeploy}","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
			
		ChainDeploymentVerification.verify(dep, 
			"forumsentry-common,1.0.135-1,groupid,forumsentry-common,1.0.135,war,\n"
			, 1, new int[]{t1.length}, t1);
		
		StringBuffer dump = new StringBuffer();
		
		Method dumpDeploymentActions = ApplicationReport.class.getDeclaredMethod("dumpDeploymentActions", Deployment.class, StringBuffer.class);
		dumpDeploymentActions.setAccessible(true);
		dumpDeploymentActions.invoke(new ApplicationReport(), dep, dump);
		
		String expectedHtml = 
				"<table id=\"applicationversionreport\">"
				+ "<thead>"
				+ "<tr>" 
				+ "<th>Name</th>" 
				+ "<th>Minimum plan</th>"
				+ "<th>Target version</th>"
				+ "<th>Existing version</th>"
				+ "<th>Deployed Version</th>"
				+ "<th>Max depth</th>"
				+ "<th>Action(s)</th>"
				+ "</tr>"
				+ "</thead>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td valign=\"top\">forumsentry-common [APP]</td>"
				+ "<td valign=\"top\">1</td>"
				+ "<td valign=\"top\">1.0.135&nbsp;&nbsp;&nbsp; [ 1.0.135-1 ]</td>"
				+ "<td valign=\"top\">absent</td>"
				+ "<td valign=\"top\">1.0.110-1 (soatzm01.st-cit1-app2)</td>"
				+ "<td valign=\"top\">0</td>"
				+ "<td valign=\"top\"><ul><li>DEPLOY [Prepared] (soatzm01)</li></ul></td>"
				+ "</tr>"
				+ "</tbody>"
				+ "</table>";
		Assert.assertEquals(expectedHtml, dump.toString());
	}
	
	@Test
	public void ebsad19506AlternateEnvironmentStateMultipleDeployedVersions() throws Exception {
		String scenarioFolder ="EBSAD-19506-alternate-environment-state-multiple-deployed-versions";
		ApplicationVersion appVersion = setUpData(scenarioFolder);
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"{tag=appdeploy, ensure=1.0.135-1}","system::packages/forumsentry-common","{ensure=absent, tag=mwdeploy}","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
			
		ChainDeploymentVerification.verify(dep, 
			"forumsentry-common,1.0.135-1,groupid,forumsentry-common,1.0.135,war,\n"
			, 1, new int[]{t1.length}, t1);
		
		StringBuffer dump = new StringBuffer();
		
		Method dumpDeploymentActions = ApplicationReport.class.getDeclaredMethod("dumpDeploymentActions", Deployment.class, StringBuffer.class);
		dumpDeploymentActions.setAccessible(true);
		dumpDeploymentActions.invoke(new ApplicationReport(), dep, dump);
		
		String expectedHtml = 
				"<table id=\"applicationversionreport\">"
				+ "<thead>"
				+ "<tr>" 
				+ "<th>Name</th>" 
				+ "<th>Minimum plan</th>"
				+ "<th>Target version</th>"
				+ "<th>Existing version</th>"
				+ "<th>Deployed Version</th>"
				+ "<th>Max depth</th>"
				+ "<th>Action(s)</th>"
				+ "</tr>"
				+ "</thead>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td valign=\"top\">forumsentry-common [APP]</td>"
				+ "<td valign=\"top\">1</td>"
				+ "<td valign=\"top\">1.0.135&nbsp;&nbsp;&nbsp; [ 1.0.135-1 ]</td>"
				+ "<td valign=\"top\">absent</td>"
				+ "<td valign=\"top\">1.0.110-1 (soatzm01.st-cit1-app2)<br />1.0.220-1 (tsttzm02.st-cit1-app2)</td>"
				+ "<td valign=\"top\">0</td>"
				+ "<td valign=\"top\"><ul><li>DEPLOY [Prepared] (soatzm01)</li></ul></td>"
				+ "</tr>"
				+ "</tbody>"
				+ "</table>";
		Assert.assertEquals(expectedHtml, dump.toString());
	}
	
	@Test
	public void ebsad19506AlternateEnvironmentStateMultipleDeployedSameVersion() throws Exception {
		String scenarioFolder ="EBSAD-19506-alternate-environment-state-multiple-deployed-same-version";
		ApplicationVersion appVersion = setUpData(scenarioFolder);
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"{tag=appdeploy, ensure=1.0.135-1}","system::packages/forumsentry-common","{ensure=absent, tag=mwdeploy}","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
			
		ChainDeploymentVerification.verify(dep, 
			"forumsentry-common,1.0.135-1,groupid,forumsentry-common,1.0.135,war,\n"
			, 1, new int[]{t1.length}, t1);
		
		StringBuffer dump = new StringBuffer();
		
		Method dumpDeploymentActions = ApplicationReport.class.getDeclaredMethod("dumpDeploymentActions", Deployment.class, StringBuffer.class);
		dumpDeploymentActions.setAccessible(true);
		dumpDeploymentActions.invoke(new ApplicationReport(), dep, dump);
		
		String expectedHtml = 
				"<table id=\"applicationversionreport\">"
				+ "<thead>"
				+ "<tr>" 
				+ "<th>Name</th>" 
				+ "<th>Minimum plan</th>"
				+ "<th>Target version</th>"
				+ "<th>Existing version</th>"
				+ "<th>Deployed Version</th>"
				+ "<th>Max depth</th>"
				+ "<th>Action(s)</th>"
				+ "</tr>"
				+ "</thead>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td valign=\"top\">forumsentry-common [APP]</td>"
				+ "<td valign=\"top\">1</td>"
				+ "<td valign=\"top\">1.0.135&nbsp;&nbsp;&nbsp; [ 1.0.135-1 ]</td>"
				+ "<td valign=\"top\">absent</td>"
				+ "<td valign=\"top\">1.0.110-1 (soatzm01.st-cit1-app2)<br />1.0.110-1 (tsttzm02.st-cit1-app2)</td>"
				+ "<td valign=\"top\">0</td>"
				+ "<td valign=\"top\"><ul><li>DEPLOY [Prepared] (soatzm01)</li></ul></td>"
				+ "</tr>"
				+ "</tbody>"
				+ "</table>";
		Assert.assertEquals(expectedHtml, dump.toString());
	}
	
	@Test
	public void ebsad19506AlternateEnvironmentStateNotDeployed() throws Exception {
		String scenarioFolder ="EBSAD-19506-alternate-environment-state-not-deployed";
		ApplicationVersion appVersion = setUpData(scenarioFolder);
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t1 = new ETD[]{ new ETD(true,"{tag=appdeploy, ensure=1.0.135-1}","system::packages/forumsentry-common","{ensure=absent, tag=mwdeploy}","/st/st-cit1-app2/soatzm01.st-cit1-app2.ipt.local.yaml")};
			
		ChainDeploymentVerification.verify(dep, 
			"forumsentry-common,1.0.135-1,groupid,forumsentry-common,1.0.135,war,\n"
			, 1, new int[]{t1.length}, t1);
		
		StringBuffer dump = new StringBuffer();
		
		Method dumpDeploymentActions = ApplicationReport.class.getDeclaredMethod("dumpDeploymentActions", Deployment.class, StringBuffer.class);
		dumpDeploymentActions.setAccessible(true);
		dumpDeploymentActions.invoke(new ApplicationReport(), dep, dump);
		
		String expectedHtml = 
				"<table id=\"applicationversionreport\">"
				+ "<thead>"
				+ "<tr>" 
				+ "<th>Name</th>" 
				+ "<th>Minimum plan</th>"
				+ "<th>Target version</th>"
				+ "<th>Existing version</th>"
				+ "<th>Deployed Version</th>"
				+ "<th>Max depth</th>"
				+ "<th>Action(s)</th>"
				+ "</tr>"
				+ "</thead>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td valign=\"top\">forumsentry-common [APP]</td>"
				+ "<td valign=\"top\">1</td>"
				+ "<td valign=\"top\">1.0.135&nbsp;&nbsp;&nbsp; [ 1.0.135-1 ]</td>"
				+ "<td valign=\"top\">absent</td>"
				+ "<td valign=\"top\"></td>"
				+ "<td valign=\"top\">0</td>"
				+ "<td valign=\"top\"><ul><li>DEPLOY [Prepared] (soatzm01)</li></ul></td>"
				+ "</tr>"
				+ "</tbody>"
				+ "</table>";
		Assert.assertEquals(expectedHtml, dump.toString());
	}
	
	@Test
	public void ebsad19506AlternateEnvironmentStateSSBTest() throws Exception {
		String scenarioFolder ="EBSAD-19506-alternate-environment-state-ssb-test";
		ApplicationVersion appVersion = setUpData(scenarioFolder);
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), null);
		
		ETD[] t0 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, HieraData.ABSENT, "system::packages/iabs-simulator-rpm/ensure", "0.0.759-1", "/st/st-sst1-ssb1/tst.yaml"),
				new ETD(true, HieraData.ABSENT, "system::packages/ocf-simulator-rpm/ensure", "0.0.287-1", "/st/st-sst1-ssb1/tst.yaml"),
				new ETD(true, HieraData.ABSENT, "system::packages/pkiss-simulator-rpm/ensure", "0.0.820-1", "/st/st-sst1-ssb1/tst.yaml"),
				new ETD(true, HieraData.ABSENT, "system::packages/ssb-simulator-rpm/ensure", "0.0.805-1", "/st/st-sst1-ssb1/tst.yaml"),
			};
			ETD[] t1 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, HieraData.ABSENT, "system::packages/ssb-common-config-rpm/ensure", "1.0.1117-1", "/st/st-sst1-ssb1/tst.yaml"),
				new ETD(true, HieraData.ABSENT, "system::packages/ssb-sim-master-data-ta-rpm/ensure", "1.0.48-1", "/st/st-sst1-ssb1/tst.yaml"),
			};
			ETD[] t2 = new ETD[]{
				// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(true, "{require=[Package[ssb_cfg_tomcat], File[/etc/profile.d/tomcat_home.sh]], tag=appdeploy, aBoolean=false, aNumber=6.3, aBooleanAsText=false, aNumberAsText=7.1, ensure=1.0.852-1}", "system::packages/ssb-common-config-rpm", "{ensure=absent, require=[Package[ssb_cfg_tomcat], File[/etc/profile.d/tomcat_home.sh]], tag=appdeploy}", "/st/st-sst1-ssb1/tst.yaml"),
			};
			ETD[] t3 = new ETD[]{
					// true, requestedValue, requestedPath, existingValue, filePath
					new ETD(true, "xyz", "system::packages/ssb-common-config-rpm/change", null, "/st/st-sst1-ssb1/tst.yaml"),
					new ETD(true, "[listItem1, listItem2]", "system::packages/ssb-common-config-rpm/someOtherChange", null, "/st/st-sst1-ssb1/tst.yaml"),
				};
			ETD[] t4 = new ETD[]{
					// true, requestedValue, requestedPath, existingValue, filePath
					new ETD(true, "0.0.637-1", "system::packages/iabs-simulator-rpm/ensure", HieraData.ABSENT, "/st/st-sst1-ssb1/tst.yaml"),
					new ETD(true, "0.0.156-1", "system::packages/ocf-simulator-rpm/ensure", HieraData.ABSENT, "/st/st-sst1-ssb1/tst.yaml"),
					new ETD(true, "0.0.681-1", "system::packages/pkiss-simulator-rpm/ensure", HieraData.ABSENT, "/st/st-sst1-ssb1/tst.yaml"),
					new ETD(true, "0.0.672-1", "system::packages/ssb-simulator-rpm/ensure", HieraData.ABSENT, "/st/st-sst1-ssb1/tst.yaml"),
				};
			
		ChainDeploymentVerification.verify(dep, 
			"iabs-simulator-rpm,0.0.637-1,groupid,iabs-simulator-rpm,0.0.637-1,war,\n" +
			"ocf-simulator-rpm,0.0.156-1,groupid,ocf-simulator-rpm,0.0.156-1,war,\n" +
			"pkiss-simulator-rpm,0.0.681-1,groupid,pkiss-simulator-rpm,0.0.681-1,war,\n" +
			"ssb-common-config-rpm,1.0.852-1,groupid,ssb-common-config-rpm,1.0.852-1,war,\n" +
			"ssb-simulator-rpm,0.0.672-1,groupid,ssb-simulator-rpm,0.0.672-1,war,\n"
			, 5, new int[]{t0.length, t1.length, t2.length, t3.length, t4.length}, t0, t1, t2, t3, t4);
		
		StringBuffer dump = new StringBuffer();
		
		Method dumpDeploymentActions = ApplicationReport.class.getDeclaredMethod("dumpDeploymentActions", Deployment.class, StringBuffer.class);
		dumpDeploymentActions.setAccessible(true);
		dumpDeploymentActions.invoke(new ApplicationReport(), dep, dump);
		
		String expectedHtml = 
				"<table id=\"applicationversionreport\">"
				+ "<thead>"
				+ "<tr>"
				+ "<th>Name</th>"
				+ "<th>Minimum plan</th>"
				+ "<th>Target version</th>"
				+ "<th>Existing version</th>"
				+ "<th>Deployed Version</th>"
				+ "<th>Max depth</th>"
				+ "<th>Action(s)</th>"
				+ "</tr>"
				+ "</thead>"
			    + "<tbody>"
			    + "<tr>"
			    + "<td valign=\"top\">iabs-simulator-rpm [APP]</td>"
			    + "<td valign=\"top\">1</td>"
			    + "<td valign=\"top\">0.0.637-1&nbsp;&nbsp;&nbsp; [ 0.0.637-1 ]</td>"
			    + "<td valign=\"top\">0.0.759-1</td>"
			    + "<td valign=\"top\">0.0.759-1 (tsttzm01.st-sst1-ssb1)</td>"
			    + "<td valign=\"top\">1</td>"
			    + "<td valign=\"top\">"
			    + "<ul><li>DOWNGRADE (tst)<ul><li>UNDEPLOY [Prepared] (tst)</li><li>DEPLOY [Prepared] (tst)</li></ul></li></ul>"
			    + "</td>"
			    + "</tr>"
			    + "<tr>"
			    + "<td valign=\"top\">ocf-simulator-rpm [APP]</td>"
			    + "<td valign=\"top\">1</td>"
			    + "<td valign=\"top\">0.0.156-1&nbsp;&nbsp;&nbsp; [ 0.0.156-1 ]</td>"
			    + "<td valign=\"top\">0.0.287-1</td>"
			    + "<td valign=\"top\">0.0.287-1 (tsttzm01.st-sst1-ssb1)</td>"
			    + "<td valign=\"top\">1</td>"
			    + "<td valign=\"top\">"
			    + "<ul><li>DOWNGRADE (tst)<ul><li>UNDEPLOY [Prepared] (tst)</li><li>DEPLOY [Prepared] (tst)</li></ul></li></ul>"
			    + "</td>"
			    + "</tr>"
			    + "<tr><td valign=\"top\">pkiss-simulator-rpm [APP]</td>"
			    + "<td valign=\"top\">1</td>"
			    + "<td valign=\"top\">0.0.681-1&nbsp;&nbsp;&nbsp; [ 0.0.681-1 ]</td>"
			    + "<td valign=\"top\">0.0.820-1</td>"
			    + "<td valign=\"top\">0.0.820-1 (tsttzm01.st-sst1-ssb1)</td>"
			    + "<td valign=\"top\">1</td>"
			    + "<td valign=\"top\">"
			    + "<ul><li>DOWNGRADE (tst)<ul><li>UNDEPLOY [Prepared] (tst)</li><li>DEPLOY [Prepared] (tst)</li></ul></li></ul>"
			    + "</td>"
			    + "</tr>"
			    + "<tr>"
			    + "<td valign=\"top\">ssb-common-config-rpm [APP]</td>"
			    + "<td valign=\"top\">1</td>"
			    + "<td valign=\"top\">1.0.852-1&nbsp;&nbsp;&nbsp; [ 1.0.852-1 ]</td>"
			    + "<td valign=\"top\">1.0.1117-1</td>"
			    + "<td valign=\"top\">1.0.1117-1 (tsttzm01.st-sst1-ssb1)</td>"
			    + "<td valign=\"top\">0</td>"
			    + "<td valign=\"top\">"
			    + "<ul><li>DOWNGRADE (tst)<ul><li>UNDEPLOY [Prepared] (tst)</li><li>DEPLOY [Prepared] (tst)</li></ul></li></ul>"
			    + "</td>"
			    + "</tr>"
			    + "<tr>"
			    + "<td valign=\"top\">ssb-sim-master-data-ta-rpm [APP]</td>"
			    + "<td valign=\"top\">1</td>"
			    + "<td valign=\"top\">&nbsp;</td>"
			    + "<td valign=\"top\">1.0.48-1</td>"
			    + "<td valign=\"top\">1.0.48-1 (tsttzm01.st-sst1-ssb1)</td>"
			    + "<td valign=\"top\">0</td>"
			    + "<td valign=\"top\">"
			    + "<ul><li>UNDEPLOY [Prepared][A relevant ComponentVersion does not exist for component 'ssb-sim-master-data-ta-rpm', however a version '1.0.48-1' of the component is deployed in the environment.  This will result in an UNDEPLOY.] (tst)</li></ul>"
			    + "</td>"
			    + "</tr>"
			    + "<tr>"
			    + "<td valign=\"top\">ssb-simulator-rpm [APP]</td>"
			    + "<td valign=\"top\">1</td>"
			    + "<td valign=\"top\">0.0.672-1&nbsp;&nbsp;&nbsp; [ 0.0.672-1 ]</td>"
			    + "<td valign=\"top\">0.0.805-1</td>"
			    + "<td valign=\"top\">0.0.805-1 (tsttzm01.st-sst1-ssb1)</td>"
			    + "<td valign=\"top\">1</td>"
			    + "<td valign=\"top\">"
			    + "<ul><li>DOWNGRADE (tst)<ul><li>UNDEPLOY [Prepared] (tst)</li><li>DEPLOY [Prepared] (tst)</li></ul></li></ul>"
			    + "</td>"
			    + "</tr>"
			    + "</tbody>"
			    + "</table>";

		Assert.assertEquals(expectedHtml, dump.toString());
	}
	
	/**
	 * Based on the SSB deployment descriptor, this scenario starts with empty YAML files and deploys 
	 * components (and injects some additional YAML) across two mock zones: st-env-zone1/2
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	@Test
	public void testEBSAD19532Environments() throws FileNotFoundException, IOException, Exception {
		String scenarioFolder = "EBSAD-19532-environments";
		ApplicationVersion appVersion = setUpData(scenarioFolder);
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion,
				createEnvironmentName(), true, null);

		ETD[][] ts = {
				new ETD[] {
						// true, requestedValue, requestedPath, existingValue,
						// filePath
						new ETD(true, "1.7.1-3.el6_4.1",
								"system::packages/git/ensure", null,
								"/st/st-env-zone1/ssm.yaml"),
						new ETD(true, "mwdeploy", "system::packages/git/tag",
								null, "/st/st-env-zone1/ssm.yaml"), },
				new ETD[] {
						// true, requestedValue, requestedPath, existingValue,
						// filePath
						new ETD(true, "2.0.0-12.el6",
								"system::packages/opencv/ensure", null,
								"/st/st-env-zone2/ssb.yaml"),
						new ETD(true, "mwdeploy",
								"system::packages/opencv/tag", null,
								"/st/st-env-zone2/ssb.yaml"), },
				new ETD[] {
						// true, requestedValue, requestedPath, existingValue,
						// filePath
						new ETD(
								true,
								"0.10.29-2.el6",
								"system::packages/gstreamer-plugins-base/ensure",
								null, "/st/st-env-zone1/ssb.yaml"),
						new ETD(true, "mwdeploy",
								"system::packages/gstreamer-plugins-base/tag",
								null, "/st/st-env-zone1/ssb.yaml"),
						new ETD(
								true,
								"0.10.29-2.el6",
								"system::packages/gstreamer-plugins-base/ensure",
								null, "/st/st-env-zone2/ssb.yaml"),
						new ETD(true, "mwdeploy",
								"system::packages/gstreamer-plugins-base/tag",
								null, "/st/st-env-zone2/ssb.yaml"), },
				new ETD[] {
						// true, requestedValue, requestedPath, existingValue,
						// filePath
						new ETD(
								true,
								"{ensure=1.0.0-1, require=[Package[jdk], Postgresql::Db[ho_ssb]], tag=mwdeploy}",
								"system::packages/ssb-db-schema", null,
								"/st/st-env-zone1/dbstzm01.yaml"),
						new ETD(
								true,
								"{ensure=1.0.0-1, require=[Package[jdk], Postgresql::Db[ho_ssb]], tag=mwdeploy}",
								"system::packages/ssb-db-schema", null,
								"/st/st-env-zone1/dbstzm02.yaml"),
						new ETD(
								true,
								"{ensure=1.0.1-1, require=Package[ssb_cfg_openldap], tag=mwconfig}",
								"system::packages/ssb-ldap-schema", null,
								"/st/st-env-zone2/ldp.yaml"),
						new ETD(
								true,
								"{ensure=1.0.2-1, require=[Package[gen_bin_nexus], Mount[/var/sonatype-work]], tag=mwconfig}",
								"system::packages/ssb-rpm-nexus-baseline-config",
								null, "/st/st-env-zone1/rma.yaml"),
						new ETD(
								true,
								"{ensure=1.0.3-1, require=[Package[ssb-rpm-nexus-baseline-config], Mount[/var/sonatype-work]], tag=appdeploy}",
								"system::packages/ssb-core-features-lib-nexus",
								null, "/st/st-env-zone2/rma.yaml") },
				new ETD[] {
						// true, requestedValue, requestedPath, existingValue,
						// filePath
						new ETD(
								true,
								"{ensure=1.0.4-1, tag=mwconfig}",
								"system::packages/ssb-core-features-fuse-config",
								null, "/st/st-env-zone1/ssm.yaml"),
						new ETD(
								true,
								"{ensure=1.0.4-1, tag=mwconfig}",
								"system::packages/ssb-core-features-fuse-config",
								null, "/st/st-env-zone2/rma.yaml"), },
				new ETD[] {
				// true, requestedValue, requestedPath, existingValue, filePath
				new ETD(
						true,
						"{ensure=1.0.5-1, require=Package[ssb-core-features-fuse-config], tag=mwconfig}",
						"system::packages/ssb-core-features-fuse-application",
						null, "/st/st-env-zone2/ssb.yaml"), },
				new ETD[] {
						// true, requestedValue, requestedPath, existingValue,
						// filePath
						new ETD(true, true,
								"system::services/fuse-service/hasstatus",
								null, "/st/st-env-zone1/ssb.yaml"),
						new ETD(true, true,
								"system::services/fuse-service/hasstatus",
								null, "/st/st-env-zone2/ssb.yaml"),
						new ETD(true, false,
								"system::services/fuse-service/enable", null,
								"/st/st-env-zone1/ssb.yaml"),
						new ETD(true, false,
								"system::services/fuse-service/enable", null,
								"/st/st-env-zone2/ssb.yaml"),
						new ETD(true, "stopped",
								"system::services/fuse-service/ensure", null,
								"/st/st-env-zone1/ssb.yaml"),
						new ETD(true, "stopped",
								"system::services/fuse-service/ensure", null,
								"/st/st-env-zone2/ssb.yaml"), },
				new ETD[] {
						// true, requestedValue, requestedPath, existingValue,
						// filePath
						new ETD(true, true,
								"system::services/fuse-service/enable",
								"false", "/st/st-env-zone2/ssb.yaml"),
						new ETD(true, "running",
								"system::services/fuse-service/ensure",
								"stopped", "/st/st-env-zone2/ssb.yaml"), } };

		ChainDeploymentVerification
				.verify(dep,
						"ssb-core-features-fuse-application,1.0.5-1,groupid,ssb-core-features-fuse-application,1.0.5,war,\n"
								+ "ssb-core-features-fuse-config,1.0.4-1,groupid,ssb-core-features-fuse-config,1.0.4,war,\n"
								+ "ssb-core-features-lib-nexus,1.0.3-1,groupid,ssb-core-features-lib-nexus,1.0.3,war,\n"
								+ "ssb-db-schema,1.0.0-1,groupid,ssb-db-schema,1.0.0,war,\n"
								+ "ssb-ldap-schema,1.0.1-1,groupid,ssb-ldap-schema,1.0.1,war,\n"
								+ "ssb-rpm-nexus-baseline-config,1.0.2-1,groupid,ssb-rpm-nexus-baseline-config,1.0.2,war,\n",
						ts.length, new int[] { ts[0].length, ts[1].length,
								ts[2].length, ts[3].length, ts[4].length,
								ts[5].length, ts[6].length, ts[7].length },
						ts[0], ts[1], ts[2], ts[3], ts[4], ts[5], ts[6], ts[7]);
	}

	
	/**
	 * Tests uninstalling from only those nodes that fall within the schemes scope for a given environment.
	 */
	@Test
	public void ebsad22428SchemeSelectionViaVariantForDDWithEnvironments() throws Exception {
		ApplicationVersion appVersion = setUpData("EBSAD-22428-variants-within-environments");
		
		Deployer d = new Deployer();
		ApplicationDeployment dep = d.deploy(appVersion, createEnvironmentName(), "ds");
		
		ETD[] t1 = new ETD[]{ new ETD(true,"absent","system::packages/oer_live_sys_obj/ensure","1.12-1","/st/st-sst1-cdp1/ext/dbstzm01.st-sst1-cdp1.ipt.local.yaml")};
		
		ETD[] t2 = new ETD[]{ new ETD(true,"absent","system::packages/oer_live_sys_cfg/ensure","1.16-1","/st/st-sst1-cdp1/ext/dbstzm01.st-sst1-cdp1.ipt.local.yaml")};
		// There should be no mention of undeploying from dbstzm03 as this is out of scope for the chosen variant.
		
		ChainDeploymentVerification.verify(dep, null, 2, new int[]{1,1}, t1, t2);
	}
}
