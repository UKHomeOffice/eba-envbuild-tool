package com.ipt.ebsa.manage.environment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.manage.deploy.Deployment;
/**
 * The interface for a class which manages Environment State
 * @author Ben Noble
 *
 */
public interface EnvironmentStateManager {
	
	/**
	 * Load the environment state into the manager.
	 * @param deploy The Deployment object
	 * @param org The organisation
	 * @param zone The zone to load state for
	 * @param scope The scope of the state to manage (segregated by application)
	 * @return boolean indicating success
	 */
	public boolean load(Deployment deploy, Organisation org, Set<String> zone, Map<String, Collection<ResolvedHost>> scopes);
	
	/**
	 * Find a component of a given name in the zone.
	 * @param componentName
	 * @param zone
	 * @return List of results
	 */
	public List<StateSearchResult> findComponent(String componentName, String zone);
	
	/**
	 * Find a component of a given name in the zones.
	 * @param componentName
	 * @param zones
	 * @return List of results
	 */
	public List<StateSearchResult> findComponent(String componentName, Set<String> zones);
	
	/**
	 * Ignoring scope, does the manager know if this role or host exists?
	 * @param roleOrHost
	 * @param zone
	 * @return
	 */
	public boolean doesRoleOrHostExist(String roleOrHost, String zone);
	
	/**
	 * Get a machine state object for a particular role or host.
	 * @param zone
	 * @param roleOrHost
	 * @return
	 */
	public MachineState getEnvironmentState(String zone, String roleOrHost);
}
