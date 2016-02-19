package com.ipt.ebsa.environment.build.execute.action;

import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.bridge.AgnosticClientBridge;
import com.ipt.ebsa.agnostic.client.bridge.BridgeConfig;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.environment.build.Configuration;
import com.ipt.ebsa.environment.build.prepare.PrepareContext;
import com.ipt.ebsa.environment.data.factory.XMLHelper;
import com.ipt.ebsa.environment.data.model.InfraAction;
import com.ipt.ebsa.environment.metadata.export.agnostic.EnvironmentBuildMetadataAgnosticExport;
import com.ipt.ebsa.environment.v1.build.XMLACloudCommand;
import com.ipt.ebsa.ssh.ExecReturn;

/**
 * Infra Action performer for Skyscape
 * @author Mark Kendall
 *
 */
public class SkyscapeInfraActionPerformer extends InfraActionPerformer {
	
	private static final Logger LOG = Logger.getLogger(SkyscapeInfraActionPerformer.class);

	private XMLGeographicContainerType environmentDefnXML;
	
	public SkyscapeInfraActionPerformer(InfraAction action) {
		super(action);
	}
	
	@Override
	protected void doPrepare() {
		EnvironmentBuildMetadataAgnosticExport md = new EnvironmentBuildMetadataAgnosticExport();
		String environment = getBuildContext().getEnvironment();
		String version = getBuildContext().getVersion();
		String provider = getBuildContext().getProvider();
		LOG.info("Beginning extract of environment metadata in vCloud format from the database");
		try {
			if (environment == null) { 
				environmentDefnXML = md.extractEnvironmentContainer(getBuildContext().getOrganisation(), version, provider);
			} else {
				environmentDefnXML = md.extractEnvironment(environment, version, provider);
			}
		} catch (Exception e) {
			LOG.error("Enable to extract environment definition", e);
			throw new RuntimeException(e);
		}
		
		String xml = new XMLHelper().marshallConfigurationXML(environmentDefnXML);
		writeEnvDefnXml(xml);
	}
	
	@Override
	protected void doPrepare(PrepareContext context) {
		
		if(context.getGeographicContainer() != null) {
			environmentDefnXML = context.getGeographicContainer();
		} else {
			doPrepare();
		}
	}
	
	@Override
	protected ExecReturn doExecute() {
		CmdExecute instructions = getActionContext().getInstructionXML();
		if (instructions == null) {
			throw new IllegalStateException("No vCloud instructions in build plan for action: " + getAction().getId());
		}
		Properties properties = loadProperties();
		
		if (Configuration.isInfraActionEnabled()) {
			AgnosticClientBridge aCloudClient = new AgnosticClientBridge();
			BridgeConfig config = new BridgeConfig();
			config.setDefinitionXML(environmentDefnXML);
			config.setInstructionXML(instructions);
			config.setProperties(properties);
			
			try {
				aCloudClient.execute(config);
			} catch (Exception e) {
				throw new RuntimeException("Failed to run aCloudClient", e);
			}
		}
		return new ExecReturn(0);
	}

	@Override
	public InfraActionContext getActionContext() {
		return new InfraActionContext();
	}
	
	public class InfraActionContext extends ActionContext {

		private String instructionXML;
		private CmdExecute instructionJAXB;
		
		private synchronized void processInstructionXML() {
			if (null == instructionXML) {
				XMLACloudCommand aCloudCommand = getAction().getDefinition().getACloud();
				if (aCloudCommand != null) {
					XMLHelper helper = new XMLHelper();
					String xml = helper.marshallInstructionXML(aCloudCommand);
					instructionXML = getBuildContext().substituteParams(xml);
					instructionJAXB = helper.unmarshallACloudInstructionXML(instructionXML);
				}
			}
		}

		private String getInstructionXMLString() {
			processInstructionXML();
			return instructionXML;
		}
		
		public CmdExecute getInstructionXML() {
			processInstructionXML();
			return instructionJAXB;
		}
		
		@Override
		public Map<String, String> getActionContextMap() {
			TreeMap<String, String> map = new TreeMap<>();
			map.put("instructionXML", StringUtils.defaultString(getInstructionXMLString()));
			return map;
		}
	}

	@Override
	public String getActionDisplayName() {
		return "Skyscape Infra action";
	}
}
