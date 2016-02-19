package com.ipt.ebsa.manage.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.manage.deploy.Deployment;
import com.ipt.ebsa.manage.environment.EnvironmentStateManager;

/**
 * Manages the creation of EnvironmentStateManager instances.
 * @author Ben Noble
 *
 */
public class EnvironmentStateManagerFactory {
	/**
	 * Get an instance of a class which implements EnvronmentStateManager
	 * @param typeName The name of the type to create
	 * @param deployment The deployment object
	 * @param org The organisation
	 * @param zones Zones which this ESM will manage
	 * @param scope Scope of the ESM
	 * @return
	 */
	public static EnvironmentStateManager getInstanceOfType(String typeName, Deployment deployment, Organisation org, Set<String> zones, Map<String, Collection<ResolvedHost>> scopes) {
		try {
			@SuppressWarnings("unchecked")
			Class<EnvironmentStateManager> type = (Class<EnvironmentStateManager>) Class.forName(typeName);
			return getInstanceOfType(type, deployment, org, zones, scopes);
		} catch (Exception e) {
			throw new RuntimeException(String.format("Unable to load EnvironmentStateManager of type [%s]", typeName), e);
		}
	}

	/**
	 * Get an instance of a class which implements EnvronmentStateManager
	 * @param t The type to create an instance of
	 * @param deployment The deployment object
	 * @param org The organisation
	 * @param zones Zones which this ESM will manage
	 * @param scope Scope of the ESM
	 * @return
	 */
	private static EnvironmentStateManager getInstanceOfType(Class<EnvironmentStateManager> t, Deployment deployment, Organisation org, Set<String> zones, Map<String, Collection<ResolvedHost>> scopes) throws InstantiationException, IllegalAccessException {
		EnvironmentStateManager esm = t.newInstance();
		esm.load(deployment, org, zones, scopes);
		return esm;
	}
}
