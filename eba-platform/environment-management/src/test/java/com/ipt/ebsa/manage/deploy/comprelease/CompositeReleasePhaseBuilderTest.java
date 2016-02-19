package com.ipt.ebsa.manage.deploy.comprelease;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;
import com.ipt.ebsa.buildtools.release.entities.Application;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.buildtools.release.entities.ReleaseVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.manage.Configuration;

public class CompositeReleasePhaseBuilderTest {
	
	@Test
	public void buildPhasesReleaseAndDDAppsMatch() throws Exception {
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
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_MAX_WAIT_SECS);
		
		ReleaseVersion releaseVersion = buildReleaseFromAppShortNames("CDPLR_CID", "CDPLR_SIS", "APP", "SSB", "CDPSYSOBJ", 
				"CDPLR_CRS", "CDPOER", "SSBSIM", "DOC", "CDPSOA", "CDPTAL", "CDPTAL_DATA", "CDPTAL_XRFDATA", 
				"CDPETL_SERVICEMGMT", "CDPACT");
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(releaseVersion, "", "SITS.xml");
		
		CompositeReleasePhaseBuilder builder = new CompositeReleasePhaseBuilder();
		Set<CompositeReleasePhase> phases = builder.buildPhases(deployment);
		assertEquals(5, phases.size());
		Iterator<CompositeReleasePhase> iterator = phases.iterator();
		assertEquals("[CDPLR_CID, CDPLR_SIS, APP, SSB, CDPSYSOBJ, CDPLR_CRS]", iterator.next().getApplicationShortNames().toString());
		assertEquals("[CDPOER, SSBSIM]", iterator.next().getApplicationShortNames().toString());
		assertEquals("[DOC]", iterator.next().getApplicationShortNames().toString());
		assertEquals("[CDPSOA, CDPTAL, CDPTAL_DATA, CDPTAL_XRFDATA, CDPETL_SERVICEMGMT]", iterator.next().getApplicationShortNames().toString());
		assertEquals("[CDPACT]", iterator.next().getApplicationShortNames().toString());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void buildPhasesAppsNotInRelease() throws Exception {
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
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_MAX_WAIT_SECS);
		
		ReleaseVersion releaseVersion = buildReleaseFromAppShortNames("CDPLR_CID", "CDPLR_SIS", "APP", "SSB", "CDPSYSOBJ", "CDPLR_CRS", 
				
				// All apps from phase 2 missing
				/*"CDPOER", "SSBSIM",*/ 
				
				"DOC", "CDPSOA", "CDPTAL", 
				
				// App from phase 4 missing
				/*"CDPTAL_DATA",*/ 
				
				"CDPTAL_XRFDATA", "CDPETL_SERVICEMGMT", "CDPACT");
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(releaseVersion, "", "SITS.xml");
		
		CompositeReleasePhaseBuilder builder = new CompositeReleasePhaseBuilder();
		builder.buildPhases(deployment);
	}
	
	@Test
	public void buildPhasesAppNotInDD() throws Exception {
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
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_MAX_WAIT_SECS);
		
		ReleaseVersion releaseVersion = buildReleaseFromAppShortNames("CDPLR_CID", "CDPLR_SIS", "APP", "SSB", "CDPSYSOBJ", 
				"CDPLR_CRS", "CDPOER", 
				
				// This is a fake application :-)
				"BLAH_BLAH_NOT_IN_DD", 
				
				"SSBSIM", "DOC", "CDPSOA", "CDPTAL", "CDPTAL_DATA", "CDPTAL_XRFDATA", "CDPETL_SERVICEMGMT", "CDPACT");
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(releaseVersion, "", "SITS.xml");
		
		CompositeReleasePhaseBuilder builder = new CompositeReleasePhaseBuilder();
		Set<CompositeReleasePhase> phases = builder.buildPhases(deployment);
		assertEquals(5, phases.size());
		Iterator<CompositeReleasePhase> iterator = phases.iterator();
		assertEquals("[CDPLR_CID, CDPLR_SIS, APP, SSB, CDPSYSOBJ, CDPLR_CRS]", iterator.next().getApplicationShortNames().toString());
		assertEquals("[CDPOER, SSBSIM]", iterator.next().getApplicationShortNames().toString());
		assertEquals("[DOC]", iterator.next().getApplicationShortNames().toString());
		assertEquals("[CDPSOA, CDPTAL, CDPTAL_DATA, CDPTAL_XRFDATA, CDPETL_SERVICEMGMT]", iterator.next().getApplicationShortNames().toString());
		assertEquals("[CDPACT]", iterator.next().getApplicationShortNames().toString());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void buildPhasesDupAppInDD() throws Exception {
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
		ConfigurationFactory.getProperties().remove(Configuration.DEPLOYMENT_MAX_WAIT_SECS);
		
		ReleaseVersion releaseVersion = buildReleaseFromAppShortNames("CDPLR_CID", "CDPLR_SIS", "APP", "SSB", "CDPSYSOBJ", 
				"CDPLR_CRS", "CDPOER", "SSBSIM", "DOC", "CDPSOA", "CDPTAL", "CDPTAL_DATA", "CDPTAL_XRFDATA", "CDPETL_SERVICEMGMT", "CDPACT");
		CompositeReleaseDeployment deployment = new CompositeReleaseDeployment(releaseVersion, "", "SITS Duplicate.xml");
		
		CompositeReleasePhaseBuilder builder = new CompositeReleasePhaseBuilder();
		builder.buildPhases(deployment);
	}
	
	public static ReleaseVersion buildReleaseFromAppShortNames(String... applicationShortNames) {
		ReleaseVersion releaseVersion = new ReleaseVersion();
		List<ApplicationVersion> applicationVersions = new ArrayList<>();
		for (String applicationShortName : applicationShortNames) {
			ApplicationVersion applicationVersion = new ApplicationVersion();
			Application application = new Application();
			application.setShortName(applicationShortName);
			applicationVersion.setApplication(application);
			applicationVersions.add(applicationVersion);
		}
		releaseVersion.setApplicationVersions(applicationVersions);
		return releaseVersion;
	}
	
}
