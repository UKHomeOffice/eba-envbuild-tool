package com.ipt.ebsa.manage.deploy.rpmfailfile;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.manage.mco.MCOUtils;
import com.ipt.ebsa.manage.puppet.EMPuppetManager;
import com.ipt.ebsa.manage.puppet.SshManager;
import com.ipt.ebsa.ssh.ExecReturn;

/**
 * Manages the generation of an RPM fail file report as part of the deployment into a given zone
 * @author Dan McCarthy
 *
 */
public class RPMFailFileManager {
	
	private static final String MC_COMMAND = "failedfiles report -j";
	private static final Logger LOG = Logger.getLogger(RPMFailFileManager.class);
	
	/**
	 * Loops through application short names and generates an RPM fail file report for the deployment into the given zone.
	 * An MC operation is triggered and the resulting JSON parsed.
	 * Returns a map where the key is an FQDN and the value is either 'Failed RPM File exists' or an empty string.
	 * Returns an empty map if there was an error running the MC command or parsing the response.  
	 * @param deployment
	 * @param applicationShortNames collection
	 * @return
	 */
	public Map<String, String> reportFailFiles(Organisation org, Collection<String> zonesInScope) {
		Map<String, String> report = new TreeMap<>();
		
		for (String zone : zonesInScope) {
			report.putAll(reportFailFiles(org, zone));
		}

		return report;
	}
	
	/**
	 * Generates an RPM fail file report for the deployment into the given zone.
	 * An MC operation is triggered and the resulting JSON parsed.
	 * Returns a map where the key is an FQDN and the value is either 'Failed RPM File exists' or an empty string.
	 * Returns an empty map if there was an error running the MC command or parsing the response.  
	 * @param deployment
	 * @param applicationShortName
	 * @return
	 */
	public Map<String, String> reportFailFiles(Organisation org, String zoneName) {
		Map<String, String> report = new TreeMap<>();
		
		LOG.debug("Reporting RPM fail files for zone " + zoneName);
		EMPuppetManager puppet = new EMPuppetManager(new SshManager());
		ExecReturn result = doMCOperation(org, zoneName, puppet);		
		
		if (result != null && result.getReturnCode() == 0) {			
			LOG.debug("RPM fail file report returned with zero code");
			String json = result.getStdOut();
			report = MCOUtils.parseJson(json);
		} else {
			if (result == null) {
				LOG.error("Null exec return from RPM fail file command on Puppet for zone " + zoneName);
			} else {
				LOG.error("Non-zero exec return code (" + result.getReturnCode() + ") from RPM fail file command on Puppet for zone " + zoneName);
			}
		}

		return report;
	}
	
	/**
	 * Trigger MC fail file operation for the given deployment organisation and zone
	 * @param organisation
	 * @param zone
	 * @param puppet
	 * @return
	 */
	protected ExecReturn doMCOperation(Organisation organisation, String zoneName, EMPuppetManager puppet) {
		try {
			return puppet.doMCollectiveOperationWithOutput(organisation, zoneName, MC_COMMAND);
		} catch (Exception e) {
			LOG.error("Failed to invoke MC operation", e);
		}
		return null;
	}
}
