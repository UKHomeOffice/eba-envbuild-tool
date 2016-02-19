package com.ipt.ebsa.util;

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * @author James Shepherd
 */
public class OrgEnvUtil {
	private static final Logger LOG = Logger.getLogger(OrgEnvUtil.class);

	/** The prefix for the NP organisation. */
	public static final String NP_ORG_PREFIX = "ho_ipt_";
	/** The prefix for the ST organisation. */
	public static final String ST_ORG_PREFIX = "ipt_";
	/** The prefix for the NPA organisation. */
	public static final String NPA_ORG_PREFIX = "ho_ipt_";
	
	/** The prefix for the NP without organisation. */
	public static final String PR_PREFIX = "pr_";
	/** The prefix for the NP without organisation. */
	public static final String NP_PREFIX = "np_";
	/** The prefix for the ST without organisation. */
	public static final String ST_PREFIX = "st_";
	/** The prefix for the NPA without organisation. */
	public static final String NPA_PREFIX = "npa_";
	
	public static String getOrganisationName(String environment) {
		String env = getEnvironmentName(environment);
		int index = env.indexOf("-");
		if (index != -1) {
			return env.substring(0, index);	
		} else {
			return env;
		}
	}

	public static String getEnvironmentName(String environment) {
		String domain = getDomainForPuppet(environment);
		int index = domain.indexOf(".");
		if (index != -1) {
			return domain.substring(0, index);	
		} else {
			return domain;
		}
	}
	
	public static String getEnvironmentNameForServer(String fqdn) {
		String envName = fqdn;
		String[] parts = fqdn.split("\\.");
		if (parts.length > 1) {
			envName = parts[1];
		} else {
			LOG.warn("Unable to parse environment for server " + fqdn);
		}
		return envName;
	}
	
	public static boolean zoneIsInEnvironment(String zoneName, String environment) {
		zoneName = zoneName.toLowerCase().replaceAll("-", "_");
		environment = environment.toLowerCase().replaceAll("-", "_").replaceAll(NP_ORG_PREFIX, "").replaceAll(ST_ORG_PREFIX, "");
		LOG.debug(String.format("Does %s contain %s?", zoneName, environment));
		return zoneName.contains(environment);
	}

	/**
	 * Given an environment name, this will check if it starts with ipt (ignoring case)
	 * which in turn will confirm if it is or is not in the ST organisation.
	 * @param environmentName
	 * @return
	 */
	public static boolean isST(String environmentName) {
		String lowercaseEnv = environmentName.toLowerCase().replaceAll("-", "_");
		return lowercaseEnv.startsWith(ST_ORG_PREFIX);
	}

	/**
	 * Given an environment name, this will check if it starts with ho_ipt (ignoring case)
	 * which in turn will confirm if it is or is not in the NP organisation.
	 * @param environmentName
	 * @return
	 */
	public static boolean isNP(String environmentName) {
		String lowercaseEnv = environmentName.toLowerCase().replaceAll("-", "_");
		return lowercaseEnv.startsWith(NP_ORG_PREFIX);
	}
	
	public static boolean isSTNoOrg(String zoneOrEnv) {
		String lowercase = zoneOrEnv.toLowerCase().replaceAll("-", "_");
		return lowercase.startsWith(ST_PREFIX);
	}

	public static boolean isNPNoOrg(String zoneOrEnv) {
		String lowercase = zoneOrEnv.toLowerCase().replaceAll("-", "_");
		return lowercase.startsWith(NP_PREFIX);
	}
	
	public static boolean isPRNoOrg(String zoneOrEnv) {
		String lowercase = zoneOrEnv.toLowerCase().replaceAll("-", "_");
		return lowercase.startsWith(PR_PREFIX);
	}
	
	public static boolean isNPANoOrg(String zoneOrEnv) {
		String lowercase = zoneOrEnv.toLowerCase().replaceAll("-", "_");
		return lowercase.startsWith(NPA_PREFIX);
	}

	/**
	 * Returns a set of domains (with insertion order maintained) that are derived from the provided set of zones.
	 * @param zones
	 * @return
	 */
	public static Set<String> getDomainsForPuppet(Set<String> zones) {
		// Maintain insertion order
		Set<String> domains = new LinkedHashSet<>(zones.size());
		for (String zone : zones) {
			domains.add(getDomainForPuppet(zone));
		}
		return domains;
	}
	
	public static String getDomainForPuppet(String zone) {
		StringBuilder sb = null;
		String lowercaseEnv = zone.toLowerCase();
		if (OrgEnvUtil.isNP(zone)) {
			sb = new StringBuilder(lowercaseEnv.replace(NP_ORG_PREFIX, "").replaceAll("_", "-"));
			sb.append(".ipt.ho.local");
		} else if (OrgEnvUtil.isST(zone)) {
			sb = new StringBuilder(lowercaseEnv.replace(ST_ORG_PREFIX, "").replaceAll("_", "-"));
			sb.append(".ipt.local");
		} else {
			throw new RuntimeException("Zone name ["+zone+"] is not recognised for conversion to puppet environment string, fatal");
		}
	
		return sb.toString();
	}
	
	/**
	 * Converts zone/environment name to format that is lower case with no organisation prefix
	 * @param zone, e.g. HO_IPT_NP_PRP1_DAZO, or HO_IPT_NP_PRP1
	 * @return zone, e.g. np-prp1-dazo, or np-prp1
	 */
	public static String getZoneOrEnvLCNoOrgPrefix(String zoneOrEnv) {
		if (zoneOrEnv == null) {
			throw new IllegalArgumentException("Zone/environment name cannot be null for conversion to lower case no org, fatal");
		}
		
		StringBuilder sb = null;
		String lowercaseEnv = zoneOrEnv.toLowerCase();
		if (OrgEnvUtil.isNP(zoneOrEnv)) {
			sb = new StringBuilder(lowercaseEnv.replace(NP_ORG_PREFIX, "").replaceAll("_", "-"));
		} else if (OrgEnvUtil.isST(zoneOrEnv)) {
			sb = new StringBuilder(lowercaseEnv.replace(ST_ORG_PREFIX, "").replaceAll("_", "-"));
		} else {
			throw new IllegalArgumentException("Zone/environment name ["+zoneOrEnv+"] is not recognised for conversion to lower case no org, fatal");
		}
	
		return sb.toString();
	}
	
	/**
	 * Converts zone/environment name to format that is upper case and includes organisation prefix
	 * @param zone, e.g. np-prp1-dazo, or np-prp1
	 * @return zone, e.g. HO_IPT_NP_PRP1_DAZO, or HO_IPT_NP_PRP1
	 */
	public static String getZoneOrEnvUCWithOrgPrefix(String zoneOrEnv) {
		if (zoneOrEnv == null) {
			throw new IllegalArgumentException("Zone/environment name cannot be null for conversion to upper case with org, fatal");
		}
		
		StringBuilder sb = null;
		if (isNPNoOrg(zoneOrEnv)) {
			sb = new StringBuilder(NP_ORG_PREFIX);
		} else if (isSTNoOrg(zoneOrEnv)) {
			sb = new StringBuilder(ST_ORG_PREFIX);
		} else if (isPRNoOrg(zoneOrEnv)) {
			sb = new StringBuilder(NP_ORG_PREFIX);
		} else if (isNPANoOrg(zoneOrEnv)) {
			sb = new StringBuilder(NP_ORG_PREFIX);
		} else {
			throw new IllegalArgumentException("Zone/environment name ["+zoneOrEnv+"] is not recognised for conversion to upper case with org, fatal");
		}
	
		return sb.append(zoneOrEnv).toString().toUpperCase().replaceAll("-", "_");
	}

	/**
	 * @param environment e.g. HO_IPT_NP_II_PJT3_DEV1
	 * @return II_PJT3_DEV1
	 */
	public static String getBareEnvironment(String environment) {
		String output;
		if (isNP(environment)) {
			output = environment.substring(10);
		} else if (isST(environment)) {
			output = environment.substring(7);
		} else {
			throw new RuntimeException(String.format("Failed to convert environment [%s] to bare environment", environment));
		}
		
		LOG.debug(String.format("in [%s] out [%s]", environment, output));
		return output;
	}
}
