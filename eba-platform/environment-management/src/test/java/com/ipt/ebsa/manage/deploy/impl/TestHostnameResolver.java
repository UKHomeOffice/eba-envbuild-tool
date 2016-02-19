package com.ipt.ebsa.manage.deploy.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import com.ipt.ebsa.buildtools.release.entities.Application;
import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.deployment.descriptor.XMLDeploymentDescriptorType;
import com.ipt.ebsa.deployment.descriptor.XMLEnvironmentType;
import com.ipt.ebsa.deployment.descriptor.XMLEnvironmentsType;
import com.ipt.ebsa.deployment.descriptor.XMLSchemeType;
import com.ipt.ebsa.deployment.descriptor.XMLSchemesType;
import com.ipt.ebsa.deployment.descriptor.XMLTargetType;
import com.ipt.ebsa.deployment.descriptor.XMLZoneType;
import com.ipt.ebsa.manage.deploy.ApplicationDeployment;
import com.ipt.ebsa.manage.deploy.impl.HostnameResolver.UnresolvableHostOrRoleException;

/**
 * Tests for {@link HostnameResolver}.
 *  
 * @author David Manning
 */
public class TestHostnameResolver {

	private static final String APP_SHORT_NAME = "APP";

	/**
	 * Tests that when there's no scheme matching the environment, the hostnames are returned as literals.
	 */
	@Test
	public void noSchemeDefinedForEnvironment() throws UnresolvableHostOrRoleException {
		String zone = "IPT_ST_HOSTILE";
		
		ApplicationDeployment deployment = new ApplicationDeployment(createApplicationVersion());
		XMLDeploymentDescriptorType deploymentDescriptor = new XMLDeploymentDescriptorType();
		deployment.setDeploymentDescriptor(new DeploymentDescriptor(deploymentDescriptor, "APP"));
		
		HostnameResolver resolver = new HostnameResolver();
		assertEquals("Hostname not correctly resolved", Arrays.asList(
				new ResolvedHost("hostA", zone), 
				new ResolvedHost("hostB", zone)), 
				resolver.resolve(deploymentDescriptor, "hostA, hostB", zone, null, null));
	}
	
	
	/**
	 * Tests that if a scheme exists for the environment but there's no target in the scheme for a given hostname, that
	 * hostname is returned as a literal.
	 */
	@Test
	public void schemeDefinedButNoMatchingTarget() throws UnresolvableHostOrRoleException {
		String zone = "IPT_ST_HOSTILE";
		String schemeZone = "st-hostile";
		
		ApplicationDeployment deployment = new ApplicationDeployment(createApplicationVersion());
		XMLDeploymentDescriptorType deploymentDescriptor = new XMLDeploymentDescriptorType();
		
		// One scheme with no targets
		XMLSchemesType schemes = new XMLSchemesType();
		XMLSchemeType scheme1 = new XMLSchemeType();
		scheme1.setEnvironment(schemeZone);
		schemes.getScheme().add(scheme1);
		deploymentDescriptor.setSchemes(schemes);
		deployment.setDeploymentDescriptor(new DeploymentDescriptor(deploymentDescriptor, "APP"));
		
		HostnameResolver resolver = new HostnameResolver();
		assertEquals("Hostname not correctly resolved", Arrays.asList(new ResolvedHost("hostA", zone)), resolver.resolve(deploymentDescriptor, "hostA", zone, null, null));
	}
	
	
	/**
	 * Tests where there is a mixture of literal and looked up hostnames in the raw input.
	 */
	@Test
	public void mixOfLiteralAndLookedUpHostNames() throws UnresolvableHostOrRoleException {
		String zone = "IPT_ST_HOSTILE";
		String schemeZone = "st-hostile";
		
		ApplicationDeployment deployment = new ApplicationDeployment(createApplicationVersion());
		XMLDeploymentDescriptorType deploymentDescriptor = new XMLDeploymentDescriptorType();
		
		// One scheme with one target that matches one host, one that matches no hosts and one
		// host that has not matching target
		XMLSchemesType schemes = new XMLSchemesType();
		XMLSchemeType scheme1 = new XMLSchemeType();
		scheme1.setEnvironment(schemeZone);
		
		XMLTargetType matchingTarget = new XMLTargetType();
		matchingTarget.setName("raw host 1");
		matchingTarget.setHostnames("target host A");
		scheme1.getTarget().add(matchingTarget);
		
		XMLTargetType unusedTarget = new XMLTargetType();
		unusedTarget.setName("unrelated target");
		unusedTarget.setHostnames("target host b");
		scheme1.getTarget().add(unusedTarget);
		
		schemes.getScheme().add(scheme1);
		deploymentDescriptor.setSchemes(schemes);
		deployment.setDeploymentDescriptor(new DeploymentDescriptor(deploymentDescriptor, "APP"));
		
		HostnameResolver resolver = new HostnameResolver();
		assertEquals("Hostname not correctly resolved", Arrays.asList(
				new ResolvedHost("target host A", zone),
				new ResolvedHost("raw host 2", zone),
				new ResolvedHost("raw host 3", zone)), 
				resolver.resolve(deploymentDescriptor, "   raw host 1    ,raw host 2,   raw host 3", zone, null, null));
	}
	
	
	/**
	 * Tests where a raw hostname resolves to a target with multiple physical hosts.
	 */
	@Test
	public void hostnameResolvedToMultipleTargets() throws UnresolvableHostOrRoleException {
		String zone = "IPT_ST_HOSTILE";
		String schemeZone = "st-hostile";
		
		ApplicationDeployment deployment = new ApplicationDeployment(createApplicationVersion());
		XMLDeploymentDescriptorType deploymentDescriptor = new XMLDeploymentDescriptorType();
		
		// One scheme with one target that matches one host, one that matches no hosts and one
		// host that has not matching target
		XMLSchemesType schemes = new XMLSchemesType();
		XMLSchemeType scheme1 = new XMLSchemeType();
		scheme1.setEnvironment(schemeZone);
		
		XMLTargetType matchingTarget = new XMLTargetType();
		matchingTarget.setName("raw host 1");
		matchingTarget.setHostnames(" target host A    ,  target host B,target host C   ");
		scheme1.getTarget().add(matchingTarget);
		
		XMLTargetType unusedTarget = new XMLTargetType();
		unusedTarget.setName("unrelated target");
		unusedTarget.setHostnames("target host D");
		scheme1.getTarget().add(unusedTarget);
		
		schemes.getScheme().add(scheme1);
		deploymentDescriptor.setSchemes(schemes);
		deployment.setDeploymentDescriptor(new DeploymentDescriptor(deploymentDescriptor, "APP"));
		
		HostnameResolver resolver = new HostnameResolver();
		assertEquals("Hostname not correctly resolved", Arrays.asList(
				new ResolvedHost("target host A", zone), 
				new ResolvedHost("target host B", zone), 
				new ResolvedHost("target host C", zone), 
				new ResolvedHost("raw host 2", zone)), 
				resolver.resolve(deploymentDescriptor, "   raw host 1    ,raw host 2", zone, null, null));
	}
	
	
	/**
	 * Tests where a raw hostname resolves to a target with multiple physical hosts and where there is another
	 * non-matching environment.
	 */
	@Test
	public void hostnameMultipleSchemesPresent() throws UnresolvableHostOrRoleException {
		String zone = "IPT_ST_HOSTILE";
		String schemeZone = "st-hostile";
		
		XMLDeploymentDescriptorType deploymentDescriptor = new XMLDeploymentDescriptorType();
		
		// One scheme with one target that matches one host
		XMLSchemesType schemes = new XMLSchemesType();
		XMLSchemeType scheme1 = new XMLSchemeType();
		scheme1.setEnvironment("st-unmatching");
		
		// One scheme with one target that matches one host and one that doesn't
		XMLSchemeType scheme2 = new XMLSchemeType();
		scheme2.setEnvironment(schemeZone);
		
		XMLTargetType matchingTarget = new XMLTargetType();
		matchingTarget.setName("raw host 1");
		matchingTarget.setHostnames(" target host A    ,  target host B,target host C   ");
		scheme2.getTarget().add(matchingTarget);
		
		XMLTargetType unusedTarget = new XMLTargetType();
		unusedTarget.setName("unrelated target");
		unusedTarget.setHostnames("target host D");
		scheme2.getTarget().add(unusedTarget);
		
		schemes.getScheme().add(scheme1);
		schemes.getScheme().add(scheme2);
		deploymentDescriptor.setSchemes(schemes);
		
		HostnameResolver resolver = new HostnameResolver();
		assertEquals("Hostname not correctly resolved", Arrays.asList(
				new ResolvedHost("target host A", zone), 
				new ResolvedHost("target host B", zone), 
				new ResolvedHost("target host C", zone), 
				new ResolvedHost("raw host 2", zone)), 
				resolver.resolve(deploymentDescriptor, "   raw host 1    ,raw host 2", zone, null, null));
	}
	
	
	/**
	 * Tests where many schemes are present for a given environment and a scheme name has been specified for the deployment.
	 */
	@Test
	public void multipleSchemesPresentForSameEnvironment() {
		String zone = "IPT_ST_HOSTILE";
		String schemeZone = "st-hostile";
		
		XMLDeploymentDescriptorType deploymentDescriptor = new XMLDeploymentDescriptorType();
		
		XMLSchemesType schemes = new XMLSchemesType();
		XMLSchemeType scheme1 = new XMLSchemeType();
		scheme1.setEnvironment(schemeZone);
		scheme1.setName("Raymond");
		XMLTargetType scheme1Target = new XMLTargetType();
		scheme1Target.setName("raw host");
		scheme1Target.setHostnames(" target host A, target host B");
		scheme1.getTarget().add(scheme1Target);
		
		// This scheme should be used:
		XMLSchemeType scheme2 = new XMLSchemeType();
		scheme2.setEnvironment(schemeZone);
		scheme2.setName("Blanc");
		XMLTargetType scheme2Target = new XMLTargetType();
		scheme2Target.setName("raw host");
		scheme2Target.setHostnames("target host C, target host D");
		scheme2.getTarget().add(scheme2Target);
		
		schemes.getScheme().add(scheme1);
		schemes.getScheme().add(scheme2);
		deploymentDescriptor.setSchemes(schemes);
		
		HostnameResolver resolver = new HostnameResolver();
		try {
			assertEquals("Hostname not correctly resolved", Arrays.asList(
					new ResolvedHost("target host C", zone), 
					new ResolvedHost("target host D", zone))
					, resolver.resolve(deploymentDescriptor, "raw host", zone, "Blanc", null));
		} catch (UnresolvableHostOrRoleException e) {
		}
	}
	

	@Test
	public void hostnameResolvedForEnvironment() throws UnresolvableHostOrRoleException {
		XMLDeploymentDescriptorType deploymentDescriptor = new XMLDeploymentDescriptorType();
		
		// One scheme with one target that matches one host, one that matches no hosts and one
		// host that has not matching target
		XMLEnvironmentsType environments = new XMLEnvironmentsType();
		XMLEnvironmentType env1 = new XMLEnvironmentType();
		environments.getEnvironment().add(env1);
		env1.setName("np-prp1");
		XMLZoneType dazoZone = new XMLZoneType();
		dazoZone.setName("dazo");
		dazoZone.setReference("np-prp1-dazo");
		env1.getZone().add(dazoZone);
		
		XMLTargetType matchingTarget = new XMLTargetType();
		matchingTarget.setName("raw host 1");
		matchingTarget.setHostnames("target host A");
		dazoZone.getTarget().add(matchingTarget);
		
		XMLTargetType unusedTarget = new XMLTargetType();
		unusedTarget.setName("unrelated target");
		unusedTarget.setHostnames("target host b");
		dazoZone.getTarget().add(unusedTarget);
		
		deploymentDescriptor.setEnvironments(environments);
		
		HostnameResolver resolver = new HostnameResolver();
		assertEquals("Hostname not correctly resolved", Arrays.asList(new ResolvedHost("target host A", "HO_IPT_NP_PRP1_DAZO")), resolver.resolve(deploymentDescriptor, "dazo:raw host 1", null, null, "HO_IPT_NP_PRP1"));
	}
	
	/**
	 * Tests all the situations which could result in being unable to resolve host names.
	 */
	@Test
	public void hostnameResolutionImpossibleForEnvironment() throws UnresolvableHostOrRoleException {
		XMLDeploymentDescriptorType deploymentDescriptor = new XMLDeploymentDescriptorType();
		
		// One scheme with one target that matches one host, one that matches no hosts and one
		// host that has not matching target
		XMLEnvironmentsType environments = new XMLEnvironmentsType();
		deploymentDescriptor.setEnvironments(environments);
		XMLEnvironmentType env1 = new XMLEnvironmentType();
		environments.getEnvironment().add(env1);
		env1.setName("np-prp1");

		try {
			// try with no zones
			new HostnameResolver().resolve(deploymentDescriptor, "dazo:raw host 1", null, null, "HO_IPT_NP_PRP1");
			fail("Should have failed due to no zones being present");
		} catch (UnresolvableHostOrRoleException e ){
		}
		
		XMLZoneType dazoZone = new XMLZoneType();
		dazoZone.setName("dazo");
		dazoZone.setReference("np-prp1-dazo");
		env1.getZone().add(dazoZone);
		
		try {
			// try with matching zone but no targets
			new HostnameResolver().resolve(deploymentDescriptor, "dazo:raw host 1", null, null, "HO_IPT_NP_PRP1");
			fail("Should have failed due to no targets being present");
		} catch (UnresolvableHostOrRoleException e ){
		}
		
		XMLTargetType matchingTarget = new XMLTargetType();
		matchingTarget.setName("raw host 1");
		matchingTarget.setHostnames("target host A");
		dazoZone.getTarget().add(matchingTarget);
		
		
		try {
			// try with matching zone but duff host (doesn't include 'dazo' as a prefix)
			new HostnameResolver().resolve(deploymentDescriptor, "raw host 1", null, null, "HO_IPT_NP_PRP1");
			fail("Should have failed due to no targets being present");
		} catch (UnresolvableHostOrRoleException e ){
		}
		
		// and just to prove it does work if you use it properly:
		new HostnameResolver().resolve(deploymentDescriptor, "dazo:raw host 1", null, null, "HO_IPT_NP_PRP1");
	}
	
	
	private ApplicationVersion createApplicationVersion() {
		ApplicationVersion applicationVersion = new ApplicationVersion();
		Application application = new Application();
		application.setShortName(APP_SHORT_NAME);
		applicationVersion.setApplication(application);
		return applicationVersion;
	}
}

