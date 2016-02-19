package com.ipt.ebsa.environment.data.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.cloud.command.v1.CmdCommand;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdDetail;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdErrorStrategy;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute.CmdEnvironmentContainer;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdVirtualApplication;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentDefinitionType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.environment.data.model.EnvironmentData;
import com.ipt.ebsa.environment.data.model.EnvironmentDataImpl;
import com.ipt.ebsa.environment.data.model.EnvironmentDetailsHolder;
import com.ipt.ebsa.environment.v1.build.XMLACloudCommand;
import com.ipt.ebsa.environment.v1.build.XMLActionType;
import com.ipt.ebsa.environment.v1.build.XMLBuildRefType;
import com.ipt.ebsa.environment.v1.build.XMLBuildType;
import com.ipt.ebsa.environment.v1.build.XMLBuildsType;
import com.ipt.ebsa.environment.v1.build.XMLEnvironmentContainerType;
import com.ipt.ebsa.environment.v1.build.XMLEnvironmentType;
import com.ipt.ebsa.environment.v1.build.XMLGlobalParametersType;
import com.ipt.ebsa.environment.v1.build.XMLInfrastructureProvisioningActionDefinitionType;
import com.ipt.ebsa.environment.v1.build.XMLParamType;
import com.ipt.ebsa.environment.v1.build.XMLSequenceChoiceType;
import com.ipt.ebsa.environment.v1.build.XMLSequenceType;

public class EnvironmentStartupShutdownDataFactory extends XMLBase {
	private static final Logger LOG = Logger.getLogger(EnvironmentStartupShutdownDataFactory.class);
	
	public EnvironmentData getEnvironmentDataInstance(Map<String, Object> susdYaml, XMLGeographicContainerType geographicContainer, String environmentName, String vmc, boolean start, ArrayList<String> order) {
		EnvironmentDataImpl environmentData = new EnvironmentDataImpl();
		
		EnvironmentDetailsHolder details = getEnvironmentDetails(geographicContainer, environmentName, vmc, order);
		environmentData.setEnvironmentDetails(details);
		XMLBuildsType root = createBaseBuild(details, start);
		readContentIntoStructure(environmentData, root);
		return environmentData;
	}
	
	private EnvironmentDetailsHolder getEnvironmentDetails(XMLGeographicContainerType geographicContainer, String environmentName, String vmcName, ArrayList<String> order) {
		
		EnvironmentDetailsHolder details = new EnvironmentDetailsHolder();
		details.setGeographicContainer(geographicContainer);
		details.setEnvironmentContainer(geographicContainer.getEnvironmentContainer());
		String environmentContainerName = geographicContainer.getEnvironmentContainer().getName();
		details.setEnvironmentContainerName(environmentContainerName);
		String environmentProvider = geographicContainer.getEnvironmentContainer().getProvider().name();
		details.setEnvironmentProvider(environmentProvider);
		
		for(com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType environment : geographicContainer.getEnvironmentContainer().getEnvironment()) {
			String envName = environment.getName();
			details.setEnvironment(environment);
			details.setEnvironmentGroupName(envName);
			details.setEnvironmentName(environmentName);
			String envVersion = "";
			for (XMLEnvironmentDefinitionType envDef :environment.getEnvironmentDefinition()) {
				details.setEnvironmentDefinition(envDef);
				envVersion = envDef.getVersion();
				details.setVersion(envVersion);
				for(XMLVirtualMachineContainerType vmc: envDef.getVirtualMachineContainer()) {
					if(vmc.getName().endsWith(vmcName)) {
						vmcName = vmc.getName();
						details.setVmcName(vmcName);
						details.setVmc(vmc);
						details.setDomain(vmc.getDomain());
						processVmcOrdering(vmc,order);
						break;
						//We have found all the information that we need to get the seed data from this domain.
					}
				}
			}
			if(details.getVmc() != null) {
				break;
			}
		}
		
		return details;

	}
	
	public final String getVmFQDN(XMLVirtualMachineType vm, XMLVirtualMachineContainerType vmc) {
		StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotBlank(vmc.getDomain()) && !vm.getVmName().contains(vmc.getDomain())) {
			sb.append(vm.getVmName());
			sb.append(".");
			sb.append(vmc.getDomain());
		} else {
			sb.append(vm.getVmName());
		}
		return sb.toString();
	}
	
	private void processVmcOrdering(XMLVirtualMachineContainerType vmc, ArrayList<String> order) {
		
		HashMap<String, XMLVirtualMachineType> allVms = new HashMap<String, XMLVirtualMachineType>();
		HashMap<String, XMLVirtualMachineType> allVmsFQDN = new HashMap<String, XMLVirtualMachineType>();
		for(XMLVirtualMachineType vm : vmc.getVirtualMachine()) {
			vm.setVMStartAction("powerOn");
			vm.setVMStartDelay(0);//TODO externalise
			vm.setVMStopAction("guestShutdown");
			vm.setVMStopDelay(60);//TODO externalise
			vm.setVMOrder(0);//all at the same time
			allVms.put(vm.getVmName(), vm);
			allVmsFQDN.put(getVmFQDN(vm,vmc), vm);
		}
		
		//Set the vm order to the specification. is not specified, ordered to start/stop after all the specified vm have been actioned.
		if(order != null) {
			int counter = 1;
			for(String vms : order) {
				String [] vmsForIndex =  vms.split(":");
				for(int i=0; i<vmsForIndex.length;i++) {
					//Find by vm name or fqdn
					XMLVirtualMachineType currentVm = allVmsFQDN.get(vmsForIndex[i]) != null? allVmsFQDN.get(vmsForIndex[i]):allVms.get(vmsForIndex[i]);
					if(currentVm != null) {
						currentVm.setVMOrder(counter);
					}
				}
				counter++;
			}
			
			for(XMLVirtualMachineType vm : vmc.getVirtualMachine()) {
				if(vm.getVMOrder() == 0) {
					vm.setVMOrder(counter);
				}
			}
		} 
	}
	
	private XMLBuildRefType createBuildRef(String buildId, String buildRefId, String displayName, EnvironmentDetailsHolder details) {
		XMLBuildRefType susd = new XMLBuildRefType();
		susd.setBuildid(buildId);
		susd.setDisplayname(displayName);
		susd.setId(buildRefId);
		
		XMLParamType vapp_name = new XMLParamType();
		vapp_name.setName("vapp_name");
		vapp_name.setValue(details.getVmcName());
		
		XMLParamType jumphosts = new XMLParamType();
		jumphosts.setName("jumphosts");
		jumphosts.setValue(ConfigurationFactory.getConfiguration(details.getEnvironmentContainerName()+"."+"jumphosts"));
		
		XMLParamType environment = new XMLParamType();
		environment.setName("environment");
		environment.setValue(details.getEnvironmentGroupName());
		
		XMLParamType domain = new XMLParamType();
		domain.setName("domain");
		domain.setValue(details.getDomain());
		
		XMLParamType expected_hosts = new XMLParamType();
		expected_hosts.setName("expected_hosts");
		expected_hosts.setValue(String.valueOf(details.getVmc().getVirtualMachine().size()));
		
		XMLParamType puppet_master = new XMLParamType();
		puppet_master.setName("puppet_master");
		puppet_master.setValue(ConfigurationFactory.getConfiguration(details.getEnvironmentContainerName()+"."+"puppet_master"));
		
		XMLParamType tooling_domain = new XMLParamType();
		tooling_domain.setName("tooling_domain");
		tooling_domain.setValue(ConfigurationFactory.getConfiguration(details.getEnvironmentContainerName()+"."+"tooling_domain"));
		
		XMLParamType mtzo_env = new XMLParamType();
		mtzo_env.setName("mtzo_env");
		mtzo_env.setValue(ConfigurationFactory.getConfiguration(details.getEnvironmentContainerName()+"."+"mtzo_env"));
		
		XMLParamType container_name = new XMLParamType();
		container_name.setName("container_name");
		container_name.setValue(details.getEnvironmentContainerName());
		
		XMLParamType env_name = new XMLParamType();
		env_name.setName(details.getEnvironmentName());
		
		susd.getParameterOrUserparameter().add(vapp_name);
		susd.getParameterOrUserparameter().add(jumphosts);
		susd.getParameterOrUserparameter().add(environment);
		susd.getParameterOrUserparameter().add(domain);
		susd.getParameterOrUserparameter().add(expected_hosts);
		susd.getParameterOrUserparameter().add(puppet_master);
		susd.getParameterOrUserparameter().add(tooling_domain);
		susd.getParameterOrUserparameter().add(mtzo_env);
		susd.getParameterOrUserparameter().add(container_name);
		susd.getParameterOrUserparameter().add(env_name);
	
		return susd;
	}
	
	private XMLBuildsType createBaseBuild(EnvironmentDetailsHolder details, boolean start) {
		String BUILD_ID="susd_build";
		String BUILD_REF_ID="susd_build_ref";
		String BUILD_SEQUENCE_ID="susd_sequence";
		String BUILD_ACTION_ID="susd_action";
		details.setBuildReferenceid(BUILD_REF_ID);
		
		//Builds - contains everything
		XMLBuildsType root = new XMLBuildsType();
		
		//Build
		XMLBuildType build = new XMLBuildType();
		build.setId(BUILD_ID);
		// - SequenceRef - links to sequence - only scope is this build
		XMLBuildType.XMLSequenceref sequenceRef = new XMLBuildType.XMLSequenceref();
		sequenceRef.setSequenceid(BUILD_SEQUENCE_ID);
		build.getSequenceref().add(sequenceRef);
		root.getBuild().add(build);
		
		//Sequence
		XMLSequenceType sequence = new XMLSequenceType();
		sequence.setId(BUILD_SEQUENCE_ID);
		// - Step
		XMLSequenceChoiceType.XMLStep step = new XMLSequenceChoiceType.XMLStep();
		step.setActionid(BUILD_ACTION_ID);
		sequence.getStepOrSequenceref().add(step);
		root.getSequence().add(sequence);
		
		//Global Params
		XMLGlobalParametersType global = new XMLGlobalParametersType();
		root.setGlobalparams(global);
		
		//Environment
		XMLEnvironmentType env = new XMLEnvironmentType();
		env.setName(details.getEnvironmentName());
		root.getEnvironment().add(env);
		// - buildRef
		env.getBuildref().add(createBuildRef(BUILD_ID,BUILD_REF_ID, BUILD_ID+"_"+env.getName(), details));
		
		//Actions
		XMLActionType action = new XMLActionType();
		action.setId(BUILD_ACTION_ID);
		root.getAction().add(action);
		// - Infra
		XMLInfrastructureProvisioningActionDefinitionType infraAction = new XMLInfrastructureProvisioningActionDefinitionType();
		action.getInfraOrCallOrSshcommand().add(infraAction);
		//    - aCloud
		XMLACloudCommand acloudAction = createAcloudInfraAction(details, start);

		infraAction.setACloud(acloudAction);
		action.getInfraOrCallOrSshcommand().add(infraAction);
		return root;
		
	}
	
	private XMLACloudCommand createAcloudInfraAction(EnvironmentDetailsHolder details, boolean start) {
		XMLACloudCommand acloudAction = new XMLACloudCommand();
		CmdCommand susdAction;
		
		if(start) {
			susdAction = CmdCommand.START; 
		} else {
			susdAction = CmdCommand.SHUTDOWN;
		}
		
		CmdEnvironmentContainer envContainer = new CmdEnvironmentContainer();
		envContainer.setCommand(CmdCommand.CONFIRM);
		envContainer.setIncludes(details.getEnvironmentContainerName());
		envContainer.setStrategy(CmdStrategy.EXISTS);
		envContainer.setErrorStrategy(CmdErrorStrategy.EXIT);
		
		CmdEnvironmentType environment = new CmdEnvironmentType();
		environment.setCommand(CmdCommand.CONFIRM);
		environment.setIncludes(details.getEnvironmentGroupName());
		environment.setStrategy(CmdStrategy.EXISTS);
		environment.setErrorStrategy(CmdErrorStrategy.EXIT);
		
		CmdVirtualApplication vmc = new CmdVirtualApplication();
		vmc.setCommand(susdAction);
		vmc.setIncludes(details.getVmcName());
		vmc.setStrategy(CmdStrategy.EXISTS);
		vmc.setErrorStrategy(CmdErrorStrategy.EXIT);
		environment.getVirtualMachineContainer().add(vmc);
		envContainer.setEnvironment(environment);
		acloudAction.setEnvironmentContainer(envContainer);
		
		return acloudAction;
	}
	
	private CmdDetail createVmDirective(String filter, boolean start) {
		CmdDetail vm = new CmdDetail();
		if(start) {
			vm.setCommand(CmdCommand.START);
		} else {
			vm.setCommand(CmdCommand.SHUTDOWN);
		}
		vm.setIncludes(filter);
		vm.setStrategy(CmdStrategy.EXISTS);
		vm.setErrorStrategy(CmdErrorStrategy.EXIT);
		
		return vm;
	}
	
	private void readContentIntoStructure(EnvironmentDataImpl environmentData, XMLBuildsType root) {
		// Read the builds
		for (XMLBuildType build : root.getBuild()) {
			environmentData.addBuild(build);
		}
		
		// Read the sequences
		for (XMLSequenceType sequence : root.getSequence()) {
			environmentData.addSequence(sequence);
		}
		
		// And global config
		if (root.getGlobalparams() != null) {
			for (XMLParamType xmlParamType : root.getGlobalparams().getParam()) {
				environmentData.addGlobalConfig(xmlParamType);
			}
		}
		
		for (XMLEnvironmentType xmlEnvironmentType : root.getEnvironment()) {
			environmentData.addEnvironment(xmlEnvironmentType);
		}
		
		for (XMLEnvironmentContainerType xmlEnvironmentType : root.getEnvironmentcontainer()) {
			environmentData.addEnvironmentContainer(xmlEnvironmentType);
		}
		
		for (XMLActionType xmlActionType : root.getAction()) {
			environmentData.addAction(xmlActionType);
		}
	}
}
