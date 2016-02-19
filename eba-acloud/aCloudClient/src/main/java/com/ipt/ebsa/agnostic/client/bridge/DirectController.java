package com.ipt.ebsa.agnostic.client.bridge;

import java.io.IOException;

import javax.inject.Singleton;

import org.xml.sax.SAXException;

import com.ipt.ebsa.agnostic.client.controller.Controller;
import com.ipt.ebsa.agnostic.client.skyscape.exception.EnvironmentOverrideException;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;

@Singleton
public class DirectController extends Controller {

	private BridgeConfig config;

	public BridgeConfig getConfig() {
		return config;
	}

	public void setConfig(BridgeConfig config) {
		this.config = config;
	}

	@Override
	protected XMLGeographicContainerType getConfiguration() throws SAXException, IOException {
		return getConfig().getDefinitionXML();
	}
	
	@Override
	protected XMLGeographicContainerType getConfigurationWithOverride(CmdExecute job) throws EnvironmentOverrideException, SAXException, IOException {
		return getConfig().getDefinitionXML();
	}

	@Override
	protected CmdExecute getInstructions() throws SAXException, IOException {
		return getConfig().getInstructionXML();
	}
}
