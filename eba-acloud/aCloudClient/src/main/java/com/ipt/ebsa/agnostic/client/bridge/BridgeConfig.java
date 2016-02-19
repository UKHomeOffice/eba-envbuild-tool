package com.ipt.ebsa.agnostic.client.bridge;

import java.util.Properties;

import com.ipt.ebsa.AgnosticClientCLI;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;

public class BridgeConfig {
	private XMLGeographicContainerType definitionXML;
	private CmdExecute instructionXML;
	private Properties properties;
	
	public void validate() {
		StringBuilder sb = new StringBuilder();

		
		if (null == getProperties().getProperty(AgnosticClientCLI.VDC)) {
			sb.append("VDC not set; ");
		}
		
		if (null == getProperties().getProperty(AgnosticClientCLI.URL)) {
			sb.append("URL not set; ");
		}
		
		if (null == getProperties().getProperty(AgnosticClientCLI.USER)) {
			sb.append("Username not set; ");
		}
		
		if (null == getProperties().getProperty(AgnosticClientCLI.PASSWORD)) {
			sb.append("Password not set; ");
		}
		
		if (null == getProperties().getProperty(AgnosticClientCLI.ORGANISATION)) {
			sb.append("Organisation not set; ");
		}
		
		if (null == getDefinitionXML()) {
			sb.append("Definition XML not set; ");
		}
		
		if (null == getInstructionXML()) {
			sb.append("Instruction XML not set; ");
		}
		
		if (sb.length() > 0) {
			throw new RuntimeException("Failed to validate: " + sb.toString());
		}
	}
	
	/**
	 * @return the definition
	 */
	public XMLGeographicContainerType getDefinitionXML() {
		return definitionXML;
	}
	/**
	 * @param definition the definition to set
	 */
	public void setDefinitionXML(XMLGeographicContainerType definition) {
		this.definitionXML = definition;
	}
	/**
	 * @return the instructionXML
	 */
	public CmdExecute getInstructionXML() {
		return instructionXML;
	}
	/**
	 * @param instructionXML the instructionXML to set
	 */
	public void setInstructionXML(CmdExecute instructionXML) {
		this.instructionXML = instructionXML;
	}

	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}