package com.ipt.ebsa.manage.deploy.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.BeforeAfterSteps;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Component;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Plan;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.Step;
import com.ipt.ebsa.deployment.descriptor.DeploymentDescriptor.StepItem;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.deployment.descriptor.XMLDeploymentDescriptorType;
import com.ipt.ebsa.deployment.descriptor.XMLEnvironmentType;
import com.ipt.ebsa.deployment.descriptor.XMLExecuteType;
import com.ipt.ebsa.deployment.descriptor.XMLSchemeType;
import com.ipt.ebsa.deployment.descriptor.XMLStepCommandType;
import com.ipt.ebsa.deployment.descriptor.XMLTargetType;
import com.ipt.ebsa.deployment.descriptor.XMLZoneType;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.deploy.impl.ComponentDeploymentData.ComponentId;
import com.ipt.ebsa.util.OrgEnvUtil;

import static com.ipt.ebsa.util.OrgEnvUtil.*;

/**
 * @author David Manning
 */
public class HostnameResolver {

	private static final String	AFTER	= "after";

	private static final String	BEFORE	= "before";

	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");
	
	private static final Pattern ZONE_AND_HOST_PATTERN = Pattern.compile("([^:]*):([^:]*)");

	private static final Logger log = Logger.getLogger(HostnameResolver.class);
	
	private static String UNNAMED = "unnamed";

	/**
	 * Resolves the hostname by performing a looking against the deployment
	 * schemes, if any. The rules are:
	 * <ul>
	 * <li>if there is no scheme for the current zone then the values
	 * provided in the hostnames tag can be taken as literals</li>
	 * <li>if there is a scheme the target(s) matching the hostnames should be
	 * used, otherwise the hostnames tag can be taken literally</li>
	 * <li>When the hostnames tag contains a csv list then the list should be
	 * parsed and each item should be replaced appropriately, if one is a
	 * literal and the other is based on a scheme then thats ok.</li>
	 * </ul>
	 * 
	 * @param deploymentDescriptor
	 *            Deployment data from which to obtain the deployment schemes
	 * @param rawHostnames
	 *            The hostnames string to interpret.
	 * @param zoneName
	 *            Only used in scheme and literal modes
	 * @param schemeName
	 *            Only used in scheme mode, likely to actually be null
	 * @param environment
	 *            Only used in environment mode, null in scheme mode
	 * @return
	 */
	public Collection<ResolvedHost> resolve(XMLDeploymentDescriptorType deploymentDescriptor, String rawHostnames, String zoneName, 
			String schemeName, String environment) throws UnresolvableHostOrRoleException {
		if (rawHostnames == null) {
			throw new IllegalArgumentException("At least one hostname must be specified for a given component");
		}

		log.debug(String.format("Resolving hostname for environment [%s], zone [%s], scheme [%s] from raw hostnames [%s]", environment, zoneName, schemeName, rawHostnames));

		String[] hostArray = SPLIT_PATTERN.split(rawHostnames.trim());

		if (environment == null && deploymentDescriptor.getSchemes() != null && !deploymentDescriptor.getSchemes().getScheme().isEmpty()) {
			// Scheme (SS2) mode
			return resolveFromSchemes(deploymentDescriptor, zoneName, schemeName, hostArray);
		} else if (environment != null) {
			// Environment (SS3) mode
			return resolveFromEnvironments(deploymentDescriptor, environment, hostArray);
		} else if (zoneName != null) {
			log.debug("No schemes or environments present, fallback to literal interpretation");
			List<ResolvedHost> literals = new ArrayList<>();
			for (String host : hostArray) {
				literals.add(new ResolvedHost(host, zoneName));
			}
			return literals;
		} else {
			throw new UnresolvableHostOrRoleException(Arrays.asList(hostArray), "Unable to resolve zone for hosts");
		}
	}
	
	
	private Collection<ResolvedHost> resolveFromEnvironments(XMLDeploymentDescriptorType deploymentDescriptor,
			String environment, String[] hostArray)	throws UnresolvableHostOrRoleException {
		ArrayList<String> unresolvableHostOrRoles = new ArrayList<>();
		List<ResolvedHost> returnedHosts = new ArrayList<>();
		
		boolean envFound = false;
		for (XMLEnvironmentType env : deploymentDescriptor.getEnvironments().getEnvironment()) {
			if (environment.equals(getZoneOrEnvUCWithOrgPrefix(env.getName()))) {
				envFound = true;
				log.debug(String.format("Found data for environment [%s]", environment));
				
				for (String rawHost : hostArray) {
					// Determine the zone for this host
					Matcher zoneAndHostMatcher = ZONE_AND_HOST_PATTERN.matcher(rawHost);
					if (!zoneAndHostMatcher.find()) {
						// Host is defined using the old (no-zone) pattern
						log.warn("Host or role [" + rawHost + "] does not follow [zone]:[host/Role] pattern");
						unresolvableHostOrRoles.add(rawHost);
					} else {
						// Host is defined using the zone-aware pattern
						String zone = zoneAndHostMatcher.group(1);
						String hostOrRoleAlias = zoneAndHostMatcher.group(2);
						
						XMLZoneType zoneData = null;
						for (XMLZoneType xmlZoneType : env.getZone()) {
							if (xmlZoneType.getName().equals(zone)) {
								zoneData = xmlZoneType;
								break;
							}
						}
						if (zoneData == null) {
							log.warn(String.format("Unable to find zone matching [%s]", zone));
							unresolvableHostOrRoles.add(rawHost);
						} else {
							log.debug(String.format("Searching for target matching host alias [%s] in zone [%s]", rawHost, zone));
							
							String zoneWithOrg = getZoneOrEnvUCWithOrgPrefix(zoneData.getReference());
							
							boolean targetFound = false;
							for (XMLTargetType target : zoneData.getTarget()) {
								if (target.getName().equalsIgnoreCase(hostOrRoleAlias)) {
									targetFound = true;
									
									log.debug(String.format("Found target matching host alias [%s]", target.getName()));
									// if the targets match, add all the target hosts and
									// bomb out
									for (String host : SPLIT_PATTERN.split(target.getHostnames().trim())) {
										returnedHosts.add(new ResolvedHost(host, zoneWithOrg));
									}
									break;
								}
							}
							if (!targetFound) {
								log.warn(String.format("Unable to find target matching [%s] in [%s]", hostOrRoleAlias, zone));
								unresolvableHostOrRoles.add(rawHost);
							}
						}
					}
				}
				// Found the environment so break out of environment loop now
				break;
			}
		}
		if (!envFound) {
			// No env match
			throw new UnresolvableHostOrRoleException(Arrays.asList(hostArray), String.format("Unable to resolve environment in DD for %s", environment));
		}
		if (!unresolvableHostOrRoles.isEmpty()) {
			throw new UnresolvableHostOrRoleException(unresolvableHostOrRoles);
		}
		return returnedHosts;
	}



	/**
	 * Resolve hostnames using the older SS2 schemes
	 */
	private Collection<ResolvedHost> resolveFromSchemes(XMLDeploymentDescriptorType deploymentDescriptor, String zoneName,
												  String schemeName, String[] hostArray) {
		Collection<ResolvedHost> returnedHosts = new ArrayList<>();

		HOSTS: for (String host : hostArray) {
			for (XMLSchemeType scheme : deploymentDescriptor.getSchemes().getScheme()) {
				log.debug(String.format("Inspecting scheme [%s] for zone [%s]", StringUtils.defaultString(scheme.getName(), UNNAMED), scheme.getEnvironment()));
				
				String schemeZone = getZoneOrEnvUCWithOrgPrefix(scheme.getEnvironment());
				if (!zoneName.equals(schemeZone)) {
					// if the zones don't match carry on to the next one
					continue;
				}
				log.debug(String.format("Found scheme [%s] matching zone [%s]", StringUtils.defaultIfBlank(schemeName, UNNAMED), zoneName));

				if (schemeName != null && !schemeName.equals(scheme.getName())) {
					log.debug(String.format("Scheme [%s]'s name doesn't match, continuing...", schemeName));
					// if the zones match and a
					// scheme's been defined for the deployment and doesn't
					// match this scheme, carry on to the next one
					continue;
				}

				log.debug(String.format("Searching for target matching host alias [%s]", host));
				for (XMLTargetType target : scheme.getTarget()) {
					if (target.getName().equals(host)) {
						log.debug(String.format("Found target matching host alias [%s]", target.getName()));
						// if the targets match, add all the target hosts and
						// bomb out
						for (String targetHost : SPLIT_PATTERN.split(target.getHostnames().trim())) {
							returnedHosts.add(new ResolvedHost(targetHost, zoneName));
						}
						continue HOSTS;
					}
				}
			}
			// The zone matches but there are no targets that match, therefore fall back to
			// interpreting the host name(s) literally
			log.debug(String.format("Found zone match but no applicable targets"));
			returnedHosts.add(new ResolvedHost(host.trim(), zoneName));
		}

		return returnedHosts;
	}

	
	public static class UnresolvableHostOrRoleException extends Exception {

		private static final long serialVersionUID = 1L;
		private static final String DEF_MSG_PREFIX = "Unable to resolve hosts and/or roles";
		
		private final List<String> hostsOrRoles;
		private final String messagePrefix;
		
		public UnresolvableHostOrRoleException(List<String> hostsOrRoles) {
			this(hostsOrRoles, DEF_MSG_PREFIX);
		}
		
		public UnresolvableHostOrRoleException(List<String> hostsOrRoles, String messagePrefix) {
			super();
			this.hostsOrRoles = hostsOrRoles;
			this.messagePrefix = messagePrefix;
		}
		
		@Override
		public String getMessage() {
			return new StringBuilder(messagePrefix).append(": ").append(hostsOrRoles).toString();
		}
	}



	/**
	 * This function runs through the host names defined in the "hostnames"
	 * attribute of the components and resolves them based on the schemes or environment data present.
	 */
	private boolean resolveComponentHostNames(Deployment deployment) {
		boolean greatSuccess = true;
		for (Entry<ComponentId, ComponentDeploymentData> component : deployment.getComponents().entrySet()) {
			log.debug(String.format("Resolving hostnames for %s", component.getKey()));
			
			ComponentDeploymentData componentData = component.getValue();
			String applicationShortName = componentData.getApplicationShortName();
			Component ddDef = componentData.getDeploymentDescriptorDef();
			
			/* Stage 1 - Check the hostnames attribute directly */
			String rawHostsOrRoles = ddDef.getXMLType().getHostnames();

			Collection<ResolvedHost> resolvedHostsOrRoles = null;
			try {
				resolvedHostsOrRoles = resolve(componentData.getDeploymentDescriptorParent().getXMLType(), rawHostsOrRoles, deployment.getDefaultZoneName(), deployment.getSchemeName(applicationShortName), deployment.getEnvironmentName());
				for (ResolvedHost resolvedHostOrRole : resolvedHostsOrRoles) {
					boolean roleOrHostExists = deployment.getEnvironmentStateManager().doesRoleOrHostExist(resolvedHostOrRole.getHostOrRole(), resolvedHostOrRole.getZone());
					if (!roleOrHostExists) {
						String text = String.format("No matching hiera file for host or role [%s]", resolvedHostOrRole);
						boolean hieraShouldBeCreated = Configuration.getHieraShouldBeCreated();
						if (!hieraShouldBeCreated) {
							componentData.getChangeSets().add(DifferenceManager.getFailDeploymentActionGroup(componentData, ChangeType.FAIL, text));
							componentData.getExceptions().add(new RuntimeException(text));
							greatSuccess = false;
						}
						log.debug(text);
					}
				}
			} catch (UnresolvableHostOrRoleException e) {
				throw new RuntimeException("Failed to resolve host or role", e);
			}

			componentData.setHosts(resolvedHostsOrRoles);
			
			/* Stage 2 check component level action hostnames */
			if (!resolveComponentActions(deployment, ddDef, componentData)) {
				greatSuccess = false;
			}
			
			/* Stage 3 check hint level action hostnames */
			if (!resolveDeploymentHintActions(deployment, ddDef, componentData)) {
				greatSuccess = false;
			}
		}
		return greatSuccess;
	}

	

	/**
	 * Resolves all of the hostnames used in the descriptor against the schemes
	 * and Environment State
	 * 
	 * @return whether or not all hostnames were resolved successfully.
	 */
	public boolean doHostnameResolution(Deployment deployment, DeploymentDescriptor deploymentDescriptor) {

		/*
		 * Decode hostnames from schemes. Error early if any of the components
		 * resolve to non-existent hiera files
		 */
		boolean componentHostsValid = resolveComponentHostNames(deployment);
		if (!componentHostsValid) {
			log.error("Some components couldn't be resolved to valid hosts or roles");
		}
		/*
		 * Decode hostnames from schemes. Error early if any of the actions not
		 * related to components fail to resolve to non-existent hiera files
		 */
		boolean actionHostsValid = resolvePlanActionHostNames(deployment, deploymentDescriptor);
		if (!actionHostsValid) {
			log.error("Some actions couldn't be resolved to valid hosts or roles");
		}
		return actionHostsValid && componentHostsValid;
	}

	/**
	 * This function runs through the host names defined in the "hostnames"
	 * attribute of the actions in the plan and resolves them based on the
	 * schemes
	 * 
	 * @param deployment
	 * @return
	 */
	private boolean resolvePlanActionHostNames(Deployment deployment, DeploymentDescriptor deploymentDescriptor) {
		boolean greatSuccess = true;
		
		if (deploymentDescriptor != null && deploymentDescriptor.getPlans() != null) {
			final List<Plan> plans = deploymentDescriptor.getPlans();
			int planCount = 1;
			for (Plan plan : plans) {
				if (!doSteps(deployment, planCount, "plan", plan.getSteps(), null)) {
					greatSuccess = false;
				}
				planCount++;
			}
		}
			
		return greatSuccess;
	}

	/**
	 * This method resolves hostnames in component level actions
	 * @param deployment
	 * @param greatSuccess
	 * @param ddDef
	 * @param componentData 
	 * @return
	 */
	private boolean resolveComponentActions(Deployment deployment, Component ddDef, ComponentDeploymentData componentData) {
		boolean greatSuccess = true;
		if (ddDef.getSteps() != null) {
			if (ddDef.getSteps().getBefore() != null) {
				if (!doSteps(deployment, 0, BEFORE, ddDef.getSteps().getBefore(), componentData)) {
					greatSuccess = false;
				}
			}
			if (ddDef.getSteps().getAfter() != null) {
				if (!doSteps(deployment, 0, AFTER, ddDef.getSteps().getAfter(), componentData)) {
					greatSuccess = false;
				}
			}
		}
		return greatSuccess;
	}

	/**
	 * This rather verbose method checks host name resolution on actions related to hints
	 * @param deployment
	 * @param greatSuccess
	 * @param ddDef
	 * @param componentData 
	 * @return
	 */
	private boolean resolveDeploymentHintActions(Deployment deployment, Component ddDef, ComponentDeploymentData componentData) {
		boolean greatSuccess = true;
		BeforeAfterSteps deploy = ddDef.getDeploySteps();
		BeforeAfterSteps undeploy = ddDef.getUndeploySteps();
		BeforeAfterSteps upgrade = ddDef.getUpgradeSteps();
		BeforeAfterSteps downgrade = ddDef.getDowngradeSteps();

		/* Deploy */
		if (deploy != null) {
			if (deploy.getBefore() != null) {
				if (!doSteps(deployment, 0, BEFORE, deploy.getBefore(), componentData)) {
					greatSuccess = false;
				}
			}
			if (deploy.getAfter() != null) {
				if (!doSteps(deployment, 0, AFTER, deploy.getAfter(), componentData)) {
					greatSuccess = false;
				}
			}
		}

		/* Undeploy */
		if (undeploy != null) {
			if (undeploy.getBefore() != null) {
				if (!doSteps(deployment, 0, BEFORE, undeploy.getBefore(), componentData)) {
					greatSuccess = false;
				}
			}
			if (undeploy.getAfter() != null) {
				if (!doSteps(deployment, 0, AFTER, undeploy.getAfter(), componentData)) {
					greatSuccess = false;
				}
			}
		}

		/* Upgrade */
		if (upgrade != null) {
			if (upgrade.getBefore() != null) {
				if (!doSteps(deployment, 0, BEFORE, upgrade.getBefore(), componentData)) {
					greatSuccess = false;
				}
			}
			if (upgrade.getAfter() != null) {
				if (!doSteps(deployment, 0, AFTER, upgrade.getAfter(), componentData)) {
					greatSuccess = false;
				}
			}
		}

		/* Downgrade */
		if (downgrade != null) {
			if (downgrade.getBefore() != null) {
				if (!doSteps(deployment, 0, BEFORE, downgrade.getBefore(), componentData)) {
					greatSuccess = false;
				}
			}
			if (downgrade.getAfter() != null) {
				if (!doSteps(deployment, 0, AFTER, downgrade.getAfter(), componentData)) {
					greatSuccess = false;
				}
			}
		}
		return greatSuccess;
	}

	/**
	 * Resolve the hosts for a number of steps
	 * @param deployment
	 * @param referenceCount
	 * @param referenceName
	 * @param steps
	 * @param componentData, could be null
	 * @return
	 */
	private boolean doSteps(Deployment deployment, int referenceCount, String referenceName, final List<Step> steps, ComponentDeploymentData componentData) {
		boolean b = true;
		if (steps != null) {
			int stepCount = 1;
			for (Step step : steps) {
				if (!doStep(deployment, referenceCount, referenceName, stepCount, step, componentData)) {
					b = false;
				}
			}
		}
		return b;
	}

	/**
	 * Resolve hostnames for an individual step, return false if not successful 
	 * @param deployment
	 * @param referenceCount
	 * @param referenceName
	 * @param stepCount
	 * @param step
	 * @param componentData, could be null
	 * @return
	 */
	private boolean doStep(Deployment deployment, int referenceCount, String referenceName, int stepCount, Step step, ComponentDeploymentData componentData) {
		boolean greatSuccess = true;
		List<StepItem> items = step.getItems();
		for (StepItem item : items) {
			XMLStepCommandType commandXml = item.getXmlType();
			String rawHostsOrRoles = commandXml.getHostnames();
			if (rawHostsOrRoles != null) {
				XMLDeploymentDescriptorType ddXml;
				String applicationShortName;
				if (componentData != null) {
					// This is a component-level step
					ddXml = componentData.getDeploymentDescriptorParent().getXMLType();
					applicationShortName = componentData.getApplicationShortName();
				} else {
					// This is an application-level step
					DeploymentDescriptor dd = step.getPlan().getDeploymentDescriptor();
					applicationShortName = dd.getApplicationShortName();
					ddXml = dd.getXMLType();
				}
				
				Collection<ResolvedHost> resolvedHostsOrRoles;
				try {
					resolvedHostsOrRoles = resolve(ddXml, rawHostsOrRoles, deployment.getDefaultZoneName(), deployment.getSchemeName(applicationShortName), deployment.getEnvironmentName());
					item.setHosts(resolvedHostsOrRoles);
					
					/**
					 * Command execution may be on roles or hosts which are not present in the hierarchy 
					 * visible to this tool but may still be valid.  A static check does not work for executions
					 * therefore we exclude them from the check below.
					 */
					if (!(commandXml instanceof XMLExecuteType)) {
						for (ResolvedHost resolvedHostOrRole : resolvedHostsOrRoles) {
							boolean roleOrHostExists = deployment.getEnvironmentStateManager().doesRoleOrHostExist(resolvedHostOrRole.getHostOrRole(), resolvedHostOrRole.getZone());
							boolean createHiera = Configuration.getHieraShouldBeCreated();
							if (!roleOrHostExists && !createHiera) {
								log.error(String.format("No matching hiera file for host or role [%s] which is declared in step %s of %s %s.", resolvedHostOrRole, stepCount, referenceName, referenceCount));
								greatSuccess = false;
							}
						}
					}
				} catch (UnresolvableHostOrRoleException e) {
					throw new RuntimeException("Failed to resolve host or role", e);
				}
			}
		}
		stepCount++;
		return greatSuccess;
	}

	
	/**
	 * Try to determine the scope for this deployment by determining the scheme that is in effect.
	 * 
	 * @param dd The deployment descriptor
	 * @param zone The zone name, e.g. 'HO_IPT_NP_PRP2_DAZO' or 'IPT_ST_SIT1_COR1'
	 * @param schemeName (Optional) The configured scheme name from UI, likely to be null
	 * @return the scope from the scheme, or null if no scoping is in place (i.e. the scope is the whole zone)
	 */
	public static Collection<ResolvedHost> getSchemeScope(DeploymentDescriptor dd, String zone, String schemeName) {
		if (dd.getXMLType().getSchemes() == null || dd.getXMLType().getSchemes().getScheme().isEmpty()) {
			log.debug("No schemes present to get scope from");
			return null;
		}
		
		if (schemeName == null || StringUtils.isBlank(schemeName)) {
			log.debug("Scheme name not specified, determining scheme and scope from zone match only");
		} else {
			log.debug(String.format("Scheme name specified as [%s], determining scheme and scope by zone and name match", schemeName));
		}
		
		XMLSchemeType matchedScheme = null;
		for (XMLSchemeType scheme : dd.getXMLType().getSchemes().getScheme()) {
			if (schemeName != null && !schemeName.equals(scheme.getName())) {
				// We've been given a scheme name, but it doesn't match this scheme
				continue;
			}
			if (!zone.equals(getZoneOrEnvUCWithOrgPrefix(scheme.getEnvironment()))) {
				// This scheme's name matched the provided name (or wasn't specified), but the zone doesn't
				// scheme.getEnvironment() actually returns a zone :) 
				continue;
			}
			matchedScheme = scheme;
			break;
		}
		
		if (matchedScheme == null) {
			log.debug("No scheme configured for zone [" + zone + "]" + (schemeName == null ? "" : " using scheme name [" + schemeName + "]"));
			return null;
		}
		
		if (matchedScheme.getScope() == null) {
			log.debug(String.format("Scheme scope undefined for scheme [%s]", schemeName));
			return null;
		}
		log.debug(String.format("Scheme scope for [%s] found: [%s]", schemeName, matchedScheme.getScope()));
		
		try {
			return new HostnameResolver().resolve(dd.getXMLType(), matchedScheme.getScope(), zone, schemeName, null);
		} catch (UnresolvableHostOrRoleException e) {
			log.warn("Unable to identify scheme due to hostname resolution error", e);
			return null;
		}
	}

	/**
	 * Try to determine the scope for this deployment by determining the zone that is in effect.
	 * 
	 * @param dd The deployment descriptor
	 * @param environment The environment name, e.g. 'HO_IPT_NP_PRP2' or 'IPT_ST_SIT1'
	 * @param zoneName (Optional) The configured zone name from UI, likely to be null
	 * @param variant (Optional) The variant of the environment to search for.
	 * @return the scope from the zone, or null if no scoping is in place (i.e. the scope is the whole environment)
	 */
	public static Collection<ResolvedHost> getEnvironmentScopes(DeploymentDescriptor dd, String environment, String zoneName, String variant) {
		List<XMLZoneType> matchedZones = new ArrayList<>();
		for (XMLEnvironmentType env : dd.getXMLType().getEnvironments().getEnvironment()) {
			if (!environment.equals(getZoneOrEnvUCWithOrgPrefix(env.getName()))) {
				// Environments don't match
				continue;
			}
			if (environment.equals(getZoneOrEnvUCWithOrgPrefix(env.getName())) && variant != null && !variant.equals(env.getVariant())) {
				// We found a fantastic environment, just the variant label doesn't match what came in from the front-end
				continue;
			}
			for (XMLZoneType zone : env.getZone()) {
				if (zoneName != null && !zoneName.equals(getZoneOrEnvUCWithOrgPrefix(zone.getReference()))) {
					// We've been given a zone name, but it doesn't match this zone
					continue;
				} 
				matchedZones.add(zone);
			}
		}
		
		if (matchedZones.isEmpty()) {
			log.debug("No zone configured for environment [" + environment + "]" + (zoneName == null ? "" : " using zone name [" + zoneName + "]"));
			return null;
		}
		
		List<ResolvedHost> hostsInScope = new ArrayList<ResolvedHost>();
		ZONE: for (XMLZoneType matchedZone : matchedZones) {
			if (matchedZone.getScope() == null) {
				log.debug(String.format("Zone scope undefined for zone [%s]. All target hosts/roles are in scope", zoneName));
				for (XMLTargetType target : matchedZone.getTarget()) {
					for (String roleOrHost : target.getHostnames().trim().split(SPLIT_PATTERN.pattern())) {
						hostsInScope.add(new ResolvedHost(roleOrHost, getZoneOrEnvUCWithOrgPrefix(matchedZone.getReference())));
					}
				}
				continue ZONE;
			}
			log.debug(String.format("Zone scope for [%s] found: [%s]", zoneName, matchedZone.getScope()));
			
			List<String> zoneScope = Arrays.asList(matchedZone.getScope().trim().split(SPLIT_PATTERN.pattern()));
			for (XMLTargetType target : matchedZone.getTarget()) {
				if (zoneScope.contains(target.getName())) {
					for (String roleOrHost : target.getHostnames().trim().split(SPLIT_PATTERN.pattern())) {
						hostsInScope.add(new ResolvedHost(roleOrHost, getZoneOrEnvUCWithOrgPrefix(matchedZone.getReference())));
					}
				}
			}
			if (hostsInScope.isEmpty()) {
				throw new IllegalArgumentException(String.format("Scope [%s] doesn't match any target in this zone", matchedZone.getScope()));
			}
			
		}
		return hostsInScope;
	}	
	
	/**
	 * Currently assumes that hostnames are defined in the DD in the form <hostOrRole>.<zone>
	 * e.g. dbs.st-sit1-cor1 or soatzm01.np-prp1-dazo
	 * @param hostnames
	 * @return
	 */
	public static List<ResolvedHost> buildPhaseResolvedHosts(String hostnames) {
		List<ResolvedHost> hosts = new ArrayList<>();
		if (hostnames != null) {
			for (String host : hostnames.trim().split("\\s*,\\s*")) {
				if (host.contains(".")) {
					String hostOrRole = host.substring(0, host.indexOf("."));
					String zone = host.substring(host.indexOf(".") + 1);
					String zoneUC = OrgEnvUtil.getZoneOrEnvUCWithOrgPrefix(zone);
					hosts.add(new ResolvedHost(hostOrRole, zoneUC));
				} else {
					throw new IllegalArgumentException(String.format("Host '%s' from deployment descriptor is invalid. It must be of the form <host-or-role>.<zone>", host));
				}
			}
		}
		return hosts;
	}
}
