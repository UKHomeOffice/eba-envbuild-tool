package com.ipt.ebsa.manage.mco;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.ResolvedHost;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.Deployment;

public class TestEnvironmentStateManager extends MCOEnvironmentStateManager {
	private static Logger LOG = LogManager.getLogger(TestEnvironmentStateManager.class);
	private String yumOutputFilePath;
	
	@Override
	public boolean load(Deployment dep, Organisation org, Set<String> zones, Map<String, Collection<ResolvedHost>> scope) {
		this.zones = zones;
		this.yumOutputFilePath = Configuration.getYumTestFolder();
		
		byte[] fileContents = null;
		try {
			LOG.debug("Loading data from file: " + yumOutputFilePath);
			fileContents = Files.readAllBytes(Paths.get(yumOutputFilePath));
		} catch (IOException e) {
			throw new RuntimeException(String.format("Couldn't load Yum data from file [%s]", yumOutputFilePath), e);
		}
		
		String mcoOutput = new String(fileContents);
		Map<String, String> jsonOutput = MCOUtils.parseJson(mcoOutput);
		this.zoneState = parseMCOYumList(jsonOutput);
		return true;
	}
}
