package com.ipt.ebsa.environment.build.execute.action;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.environment.build.Configuration;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainer;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.environment.data.model.InfraAction;

/**
 * Common Infra Action Performer. A subclass should be implemented for each cloud provider
 * @author Mark Kendall
 *
 */
public abstract class InfraActionPerformer extends ActionPerformer {
	
	private static final Logger LOG = Logger.getLogger(InfraActionPerformer.class);

	private InfraAction action;

	public InfraActionPerformer(InfraAction action) {
		this.action = action;
	}
	
	/**
	 * Writes the environment definition XML to the file identified by envDefnXmlPath
	 * @param xml
	 */
	protected void writeEnvDefnXml(String xml) {
		String envDefnXmlPath = getBuildContext().getEnvDefnXmlPath();
		if (envDefnXmlPath != null) {
			try {
				FileUtils.write(new File(envDefnXmlPath), xml);
			} catch (IOException e) {
				LOG.error("Unable to write environment definition file to path " + envDefnXmlPath, e);
			}
		}
	}
	
	/**
	 * Loads the Organisation and Provider specific properties
	 * @return
	 */
	protected Properties loadProperties() {
		String org = determineOrg();
		Organisation organisation = ConfigurationFactory.getOrganisations().get(org);
		Properties properties = Configuration.getProviderProperties(organisation, getBuildContext().getProvider());
		addCustomisationScriptProperty(org, properties);
		addBuildParamerters(properties);
		return properties;
	}
	
	private Properties addBuildParamerters(Properties properties) {
		
		if(properties == null) {
			LOG.error("Input properties object is null so instantiating a new object");
			properties = new Properties();
		}
		
		if (getBuildContext() != null) {
			Set<Map.Entry<String, String>> map = getBuildContext().parameterMapEntrySet();
			if (map != null && map.size() > 0) {
				Iterator<Entry<String, String>> i = map.iterator();
				while (i.hasNext()) {
					Entry<String, String> entry = i.next();
					LOG.info(String.format("Adding build parameter with key [%s] and value [%s] to the config properties", entry.getKey(),entry.getValue()));
					properties.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return properties;
	}
	
	/**
	 * Adds the guest customisation script directory to the properties
	 * @param org
	 * @param properties
	 */
	private void addCustomisationScriptProperty(String org, Properties properties) {
		String gitUrl = Configuration.getCustomisationScriptsGitUrl(org);
		if (gitUrl != null) {
			// This is where the guest customisation scripts were checked out to previously
			properties.setProperty("guestCustScriptDir", getBuildContext().getWorkDir() + File.separator + Configuration.getCustomisationScriptsCheckoutDir());
		} else {
			String scriptPath = Configuration.getCustomisationScriptPath(org);
			properties.setProperty("guestCustScriptDir", scriptPath);
		}
	}
	
	/**
	 * Returns the Organisation
	 * @return
	 */
	private String determineOrg() {
		String org = getBuildContext().getOrganisation();
		if (org == null) {
			try {
				EnvironmentContainer envContainer = new ReadManager().getEnvironmentContainerForEnvironmentName(
						getBuildContext().getEnvironment(), getBuildContext().getProvider());
				org = envContainer.getName();
			} catch (Exception e) {
				throw new RuntimeException("Failed to get Environment Container", e);
			}
		}
		return org;
	}
	
	@Override
	public InfraAction getAction() {
		return action;
	}

	@Override
	public String getActionDisplayName() {
		return "Infra action";
	}
}
