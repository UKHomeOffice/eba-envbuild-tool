package com.ipt.ebsa.agnostic.client.controller;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.ipt.ebsa.agnostic.client.config.Config;
import com.ipt.ebsa.agnostic.client.controller.component.ComponentType;
import com.ipt.ebsa.agnostic.client.exception.InvalidConfigurationException;
import com.ipt.ebsa.agnostic.client.manager.CloudManagerEnum;
import com.ipt.ebsa.agnostic.client.manager.CloudManagerFactory;
import com.ipt.ebsa.agnostic.client.manager.ICloudManager;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.EnvironmentOverrideException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.util.ReflectionUtils;
import com.ipt.ebsa.agnostic.client.util.XPathHandler;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdCommand;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdDetail;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType.CmdOverrides.CmdOverride;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdErrorStrategy;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute.CmdEnvironmentContainer;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdVirtualApplication;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLApplicationNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLDataCenterType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType.XMLEnvironmentContainerDefinition;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentDefinitionType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.ipt.ebsa.sdkclient.acloudconfig.ACloudConfigurationLoader;
import com.jcabi.aspects.Loggable;
import com.vmware.vcloud.sdk.VCloudException;

/**
 * This is a controller interface which provides a facade for the CLI (and
 * possibly other interfaces in the future)
 * 
 *
 */
@Typed(Controller.class)
@Loggable(prepend = true)
public class Controller {

	private Logger logger = LogManager.getLogger(Controller.class);

	@Inject
	private CloudManagerFactory cloudManagerFactory;

	@Inject
	@Config
	private String definition;
	
	@Inject
	@Config
	private String networkLayout;

	@Inject
	@Config
	private String environments;

	@Inject
	@Config
	private String executionplan;
	
	private CmdExecute instructions;
	
	private File processedDefinitionFile = null;
	static final String	DEFN_OVERRIDE_FILENAME = "overriddenDefinition.xml";
	
	public Controller() {
		
	}
	
	public Controller(CloudManagerFactory factory, String def, String plan) {
		cloudManagerFactory = factory;
		definition = def;
		executionplan = plan;
	}

	public void sandpit() {
	}

	public void execute() throws SAXException, IOException,
			StrategyFailureException, VCloudException,
			InvalidStrategyException, UnresolvedDependencyException,
			ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, InvalidConfigurationException, EnvironmentOverrideException {
		logger.debug("Started execute");
		CmdExecute job = getInstructions();
		// Load the configuration to be managed
		XMLGeographicContainerType geographic = getConfigurationWithOverride(job);
		//override
		executeEnvironmentContainerInstructions(geographic, job);

		logger.debug("Completed execute");
	}

	/**
	 * Runs VApp instructions and calls down to nested instructions
	 * 
	 * @param applications
	 * @throws SAXException
	 * @throws IOException
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws UnresolvedDependencyException
	 * @throws ConnectionException
	 * @throws InvalidConfigurationException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	private void executeEnvironmentContainerInstructions(
			XMLGeographicContainerType geographic, CmdExecute job) throws SAXException,
			IOException, StrategyFailureException, VCloudException,
			InvalidStrategyException, UnresolvedDependencyException,
			ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, InvalidConfigurationException {
		logger.debug("Started executeEnvironmentContainerInstructions");
		if (job.getEnvironmentContainer() != null) {
			CmdEnvironmentContainer envContainerCmd = job
					.getEnvironmentContainer();
			ICloudManager cloudManager = cloudManagerFactory
					.getCloudManager(CloudManagerEnum
							.valueOf(geographic.getEnvironmentContainer()
									.getProvider().toString()));
			executeEnvironmentContainer(job, geographic, envContainerCmd,
					cloudManager);
		} else {
			// No Environment element
			throw new RuntimeException(
					"No Environment element specified within the execution plan");
		}
		logger.debug("Finished executeEnvironmentContainerInstructions");
	}

	

	private void executeEnvironmentContainer(CmdExecute job,
			XMLGeographicContainerType geographic,
			CmdEnvironmentContainer envContainerCmd, ICloudManager cloudManager)
			throws SAXException, IOException, StrategyFailureException,
			VCloudException, InvalidStrategyException,
			UnresolvedDependencyException, ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, InvalidConfigurationException {
		logger.debug("Started executeEnvironmentContainer");
		XMLEnvironmentContainerType environmentContainer = geographic
				.getEnvironmentContainer();
		CmdCommand command = envContainerCmd.getCommand();
		//Setup object to be used in the execution of the instructions
		HashMap<Class<?>, Object> objects = new HashMap<Class<?>, Object>();
		objects.put(envContainerCmd.getStrategy().getClass(),
				envContainerCmd.getStrategy());
		objects.put(environmentContainer.getClass(),
				environmentContainer);
		objects.put(geographic.getClass(),
				geographic);

		//Environment
		//Only working with 1 environment for now
		CmdEnvironmentType envCmd = envContainerCmd.getEnvironment();
		for(XMLEnvironmentType environment:environmentContainer.getEnvironment()) {
			
			objects.put(environment.getClass(), environment);
			objects.put(envCmd.getStrategy().getClass(), envCmd.getStrategy());
			
			executeCommandWithStrategy(envCmd.getCommand(),ComponentType.Environment,cloudManager,envContainerCmd,objects);
			
			XMLEnvironmentContainerDefinition environmenContainertDefinition = getCurrentEnvironmentContainerDefinition(environmentContainer.getEnvironmentContainerDefinition());
			//Edge gateways
			executeGatewayInstructions(environmentContainer, environmenContainertDefinition.getGateway(), cloudManager, environment);
			//Org networks
			executeOrganisationNetworkInstructions(environmentContainer, environmenContainertDefinition.getNetwork(), cloudManager, geographic, environment);
			
			XMLEnvironmentDefinitionType environmentDefinition = getCurrentEnvironmentDefinition(environment);
			executeEnvironmentInstructions( environmentContainer, environmentDefinition, cloudManager, geographic, environment);
		}
		
		logger.debug("Finished executeEnvironmentContainer");
	}
	
	private void executeEnvironmentInstructions(
			XMLEnvironmentContainerType environment, 
			XMLEnvironmentDefinitionType environmentDefinition, 
			ICloudManager cloudManager,
			XMLGeographicContainerType geographic, 
			XMLEnvironmentType environmentType) throws SAXException, IOException, StrategyFailureException, VCloudException, InvalidStrategyException, UnresolvedDependencyException, ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		logger.debug("Started executeEnvironmentInstructions");
		CmdExecute job = getInstructions();
		if (job.getEnvironmentContainer().getEnvironment() != null) {
			executeVirtualMachineContainerInstructions(environmentDefinition.getVirtualMachineContainer(), environment, cloudManager, geographic, environmentType);
		} else {
			// No Environment element
			throw new RuntimeException(
					"No EnvironmentContainer element specified within the execution plan");
		}
		logger.debug("Finished executeEnvironmentInstructions");
	}
	
	
	private void executeGatewayInstructions(
			XMLEnvironmentContainerType environment, List<XMLGatewayType> gateways, ICloudManager cloudManager, XMLEnvironmentType environmentType) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, SAXException, IOException, ConnectionException {
		logger.debug("Started executeGatewayInstructions");
		CmdExecute job = getInstructions();
		if (job.getEnvironmentContainer().getEdgeGateway() != null) {
			List<CmdDetail> cmdDetail = job.getEnvironmentContainer()
					.getEdgeGateway();
			for (CmdDetail instance : cmdDetail) {
				executeGateways(instance, gateways, environment, cloudManager, environmentType);
			}
		} else {
			// No Environment element
			throw new RuntimeException(
					"No OrganisationNetwork element specified within the execution plan");
		}
		logger.debug("Finished executeGatewayInstructions");
	}

	private void executeGateways(CmdDetail gatewayCmd,
			List<XMLGatewayType> gateways,
			XMLEnvironmentContainerType environment, ICloudManager cloudManager, XMLEnvironmentType environmentType) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ConnectionException {
		logger.debug("Started executeGateways");
		CmdCommand command = gatewayCmd.getCommand();
		String componentName = "EdgeGateway";
		//Setup object to be used in the execution of the instructions
		
		List<XMLGatewayType> gatewayResults = selectPatternMatchedComponents(gateways, gatewayCmd, "getName");
		for(XMLGatewayType gateway : gatewayResults) {
			HashMap<Class<?>, Object> objects = new HashMap<Class<?>, Object>();
			objects.put(gatewayCmd.getStrategy().getClass(), gatewayCmd.getStrategy());
			objects.put(environment.getClass(), environment);
			objects.put(environmentType.getClass(), environmentType);
			objects.put(gateway.getClass(), gateway);
			executeCommandWithStrategy(command,ComponentType.EdgeGateway,cloudManager,gatewayCmd,objects);
		}
		logger.debug("Finished executeGateways");
	}
	
	public <T> List<T> selectPatternMatchedComponents(List<T> anArray, CmdDetail command, String methodName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		logger.debug("Started selectPatternMatchedComponents");
		List<T> selectedItems = new ArrayList<T>();
		Pattern in = command.getIncludes() == null ? null : Pattern
				.compile(command.getIncludes());
		Pattern ex = command.getExcludes() == null ? null : Pattern
				.compile(command.getExcludes());
		for (T myType : anArray) {
			
			Object[] args = null;
			String matchString = (String) myType.getClass().getMethod(methodName).invoke(myType, args);
			
			if (ex == null || !ex.matcher(matchString).matches()) {
				if (in == null || in.matcher(matchString).matches()) {
					selectedItems.add(myType);
				}
			}
		}
		logger.debug("Finished selectPatternMatchedComponents");
		return selectedItems;
	}
	
	private XMLEnvironmentContainerDefinition getCurrentEnvironmentContainerDefinition(List<XMLEnvironmentContainerDefinition> environmentDefinitions) throws InvalidConfigurationException {
		// The XML exported from the database will contain the single EnvironmentContainerDefinition that is to be actioned (selected in the environment-build UI)
		String errorMessage;
		if (environmentDefinitions.size() == 1) {
			return environmentDefinitions.get(0);
		} else if (environmentDefinitions.size() > 0) {
			errorMessage = "Multiple Environment Container Definitions are present";
		} else {
			errorMessage = "No Environment Container Definition is present";
		}
		logger.error(errorMessage);
		throw new InvalidConfigurationException(errorMessage);
	}
	
	private XMLEnvironmentDefinitionType getCurrentEnvironmentDefinition(XMLEnvironmentType environment) throws InvalidConfigurationException {
		// The XML exported from the database will contain the single EnvironmentDefinition that is to be actioned (selected in the environment-build UI)
		String errorMessage;
		List<XMLEnvironmentDefinitionType> environmentDefinitions = environment.getEnvironmentDefinition();
		if (environmentDefinitions.size() == 1) {
			return environmentDefinitions.get(0);
		} else if (environmentDefinitions.size() > 0) {
			errorMessage = "Multiple Environment Definitions are present";
		} else {
			errorMessage = "No Environment Definition is present";
		}
		logger.error(errorMessage);
		throw new InvalidConfigurationException(errorMessage);
	}
	
	protected void executeCommandWithStrategy(CmdCommand command, ComponentType componentName, ICloudManager cloudManager, CmdDetail envContainerCmd, HashMap<Class<?>, Object> objects) throws ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		logger.debug("Started executeCommandWithStrategy");
		if (command != null) {
			Method cloudManagerMethod = ReflectionUtils.locateActionMethod(command.toString(),
					componentName.toString(), cloudManager);
			if (cloudManagerMethod != null) {
				
				ReflectionUtils.executeActionMethod(cloudManagerMethod, cloudManager, objects);
			} else {
				warnNotApplicable(command, componentName.toString());
			}
		}
		logger.debug("Finished executeCommandWithStrategy");
	}

	/**
	 * Runs organisation network instructions and calls down to nested
	 * instructions
	 * 
	 * @param organisationNetworks
	 * @throws SAXException
	 * @throws IOException
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws UnresolvedDependencyException
	 * @throws ConnectionException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	protected void executeOrganisationNetworkInstructions(XMLEnvironmentContainerType environment, 
			List<XMLOrganisationalNetworkType> organisationNetworks,
			ICloudManager cloudManager, XMLGeographicContainerType geographic, XMLEnvironmentType environmentType) throws SAXException, IOException,
			StrategyFailureException, VCloudException,
			InvalidStrategyException, UnresolvedDependencyException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ConnectionException {
		logger.debug("Started executeOrganisationNetworkInstructions");
		CmdExecute job = getInstructions();
		if (job.getEnvironmentContainer().getOrganisationNetwork() != null) {
			List<CmdDetail> cmdDetail = job.getEnvironmentContainer()
					.getOrganisationNetwork();
			for (CmdDetail instance : cmdDetail) {
				executeOrganisationNetworks(environment, instance, organisationNetworks,
						cloudManager, geographic, environmentType);
			}
		} else {
			// No Environment element
			throw new RuntimeException(
					"No OrganisationNetwork element specified within the execution plan");
		}
		logger.debug("Finished executeOrganisationNetworkInstructions");
	}

	protected void executeOrganisationNetworks(XMLEnvironmentContainerType environment, CmdDetail cmdDetail,
			List<XMLOrganisationalNetworkType> organisationNetworks,
			ICloudManager cloudManager, XMLGeographicContainerType geographic, XMLEnvironmentType environmentType) throws StrategyFailureException,
			UnresolvedDependencyException, VCloudException,
			InvalidStrategyException, ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		logger.debug("Started executeOrganisationNetworks");
		CmdCommand command = cmdDetail.getCommand();
		String componentName = "OrganisationNetwork";
		//Setup object to be used in the execution of the instructions
		
		List<XMLOrganisationalNetworkType> orgNetworkResults = selectPatternMatchedComponents(organisationNetworks, cmdDetail, "getName");
		for(XMLOrganisationalNetworkType network : orgNetworkResults) {
			HashMap<Class<?>, Object> objects = new HashMap<Class<?>, Object>();
			objects.put(cmdDetail.getStrategy().getClass(),
					cmdDetail.getStrategy());
			objects.put(environment.getClass(),
					environment);
			objects.put(environmentType.getClass(), environmentType);
			objects.put(network.getClass(), network);
			objects.put(geographic.getClass(), geographic);
			executeCommandWithStrategy(command,ComponentType.OrganisationNetwork,cloudManager,cmdDetail,objects);
		}
		logger.debug("Finished executeOrganisationNetworks");
	}

	
	/**
	 * Execute instructions relating to application networks
	 * 
	 * @param appCmd
	 * @param app
	 * @throws StrategyFailureException
	 * @throws UnresolvedDependencyException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	private void executeAppNetworks(XMLEnvironmentContainerType environment, CmdVirtualApplication vmcCmd,
			XMLVirtualMachineContainerType vmc, ICloudManager cloudManager, XMLGeographicContainerType geographic, XMLEnvironmentType environmentType)
			throws StrategyFailureException, UnresolvedDependencyException,
			VCloudException, InvalidStrategyException, ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		logger.debug("Started executeAppNetworks");
		String componentName = "ApplicationNetwork";
		//Setup object to be used in the execution of the instructions
		
		List<XMLApplicationNetworkType> networks =  vmc.getNetwork();
		
		for (CmdDetail networkCmd : vmcCmd.getApplicationNetwork()) {
			CmdCommand command = networkCmd.getCommand();
			List<XMLApplicationNetworkType> networkResults = selectPatternMatchedComponents(networks, networkCmd, "getName");
			for(XMLApplicationNetworkType network : networkResults) {
				HashMap<Class<?>, Object> objects = new HashMap<Class<?>, Object>();
				objects.put(networkCmd.getStrategy().getClass(), networkCmd.getStrategy());
				objects.put(vmc.getClass(), vmc);
				objects.put(environment.getClass(), environment);
				objects.put(environmentType.getClass(), environmentType);
				objects.put(network.getClass(), network);
				objects.put(geographic.getClass(), geographic);
				executeCommandWithStrategy(command,ComponentType.ApplicationNetwork,cloudManager,vmcCmd,objects);
			}
		}
		
		logger.debug("Finished executeAppNetworks");
		
	}

	private void manageError(CmdDetail cmdDetail, Exception e) {
		logger.debug("Started manageError");
		logger.error(
				String.format(
						"Error caught, ",
						cmdDetail.getErrorStrategy() == null ? " no error strategy defined, defaulting to "
								+ CmdErrorStrategy.EXIT
								: "applying error strategy : "
										+ cmdDetail.getErrorStrategy()), e);
		CmdErrorStrategy s = cmdDetail.getErrorStrategy() == null ? CmdErrorStrategy.EXIT
				: cmdDetail.getErrorStrategy();
		switch (s) {
		case CONTINUE:
			logger.error(
					String.format("Strategy will result in error being ignored and processing will continue"),
					e);
			break;
		case CLEAN_AND_EXIT:
		case EXIT:
			throw new RuntimeException(
					String.format("Strategy will result in an exit.  Exception being wrapped and thrown."),
					e);
		}
		logger.debug("Finished manageError");
	}

	/**
	 * Execute instructions relating to virtual machines
	 * 
	 * @param appCmd
	 * @param app
	 * @throws StrategyFailureException
	 * @throws UnresolvedDependencyException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws IOException
	 * @throws ConnectionException
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	private void executeVMs(XMLEnvironmentContainerType environment, CmdExecute execute, CmdVirtualApplication vmcCmd,
			XMLVirtualMachineContainerType vmc, ICloudManager cloudManager, XMLGeographicContainerType geographic, XMLEnvironmentType environmentType)
			throws StrategyFailureException, UnresolvedDependencyException,
			VCloudException, InvalidStrategyException, IOException,
			ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		logger.debug("Started executeVMs");
		String componentName = "VirtualMachine";
		List<CmdDetail> vmCommands = vmcCmd.getVirtualMachine();
		for (CmdDetail vmCommand : vmCommands) {
			List<XMLVirtualMachineType> selectedVirtualMachines = selectPatternMatchedComponents(vmc.getVirtualMachine(), vmCommand, "getVmName");
			logger.debug("Found "+selectedVirtualMachines.size()+" vms to process");
			for(XMLVirtualMachineType vm : selectedVirtualMachines) {
				logger.debug("Processing filtered virtual machine "+ ReflectionToStringBuilder.reflectionToString(vm));
				HashMap<Class<?>, Object> objects = new HashMap<Class<?>, Object>();
				
				if(vm == null) {
					logger.debug(vm+" is null, this is not going to go well.");
				}
				
				if (vmCommand != null) {
					objects.put(vmCommand.getClass(), vmCommand);
				} else {
					logger.debug("vmCommand object is null");
				}
				
				if(vmCommand.getStrategy() != null) {
					objects.put(vmCommand.getStrategy().getClass(), vmCommand.getStrategy());
				} else {
					logger.debug(vm.getVmName()+"has no strategy set with command "+vmCommand.getCommand());
				}
				
				if(execute != null) {
					objects.put(execute.getClass(), execute);
				} else {
					logger.debug("execute object is null");
				}
				
				if(vmc != null) {
					objects.put(vmc.getClass(), vmc);
				} else {
					logger.debug("vmc object is null");
				}
				
				if (vm != null) {
					objects.put(vm.getClass(), vm);
				} else {
					logger.debug("vm object is null");
				}
				if (environment != null) {
					objects.put(environment.getClass(), environment);
				} else {
					logger.debug("environment object is null");
				}
				if (geographic != null) {
					objects.put(geographic.getClass(), geographic);
				} else {
					logger.debug("geographic object is null");
				}
				if (environmentType != null) {
					objects.put(environmentType.getClass(), environmentType);
				} else {
					logger.debug("environmentType object is null");
				}
				
				ReflectionUtils.executeCommandWithStrategy(vmCommand.getCommand(),componentName,cloudManager,objects);
			}
		}
		logger.debug("Finished executeVMs");
	}

	private void warnNotApplicable(CmdCommand command, String type) {
		logger.warn(String
				.format("Command %s is not supported for %s.  No action will be taken.",
						command, type));
	}

	/**
	 * Runs VApp instructions and calls down to nested instructions
	 * 
	 * @param applications
	 * @throws SAXException
	 * @throws IOException
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws UnresolvedDependencyException
	 * @throws ConnectionException
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	private void executeVirtualMachineContainerInstructions(
			List<XMLVirtualMachineContainerType> vmcs, 
			XMLEnvironmentContainerType environment, 
			ICloudManager cloudManager, XMLGeographicContainerType geographic, XMLEnvironmentType environmentType)
			throws SAXException, IOException, StrategyFailureException,
			VCloudException, InvalidStrategyException,
			UnresolvedDependencyException, ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		logger.debug("Started executeVirtualMachineContainerInstructions");
		CmdExecute job = getInstructions();
		if (job.getEnvironmentContainer() != null) {
			List<CmdVirtualApplication> appCommands = job
					.getEnvironmentContainer().getEnvironment().getVirtualMachineContainer();
			for (CmdVirtualApplication vmcCmd : appCommands) {
				executeVirtualMachineContainer(job, vmcs, vmcCmd, environment, cloudManager, geographic, environmentType);
			}
		} else {
			// No Environment element
			throw new RuntimeException(
					"No Environment element specified within the execution plan");
		}
		logger.debug("Finished executeVirtualMachineContainerInstructions");
	}

	/**
	 * Perform instructions on list of VApps which match the
	 * List<XMLVirtualMachineContainerType>
	 * 
	 * @param applications
	 * @param appCmd
	 * @throws StrategyFailureException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws UnresolvedDependencyException
	 * @throws IOException
	 * @throws ConnectionException
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	private void executeVirtualMachineContainer(CmdExecute execute,
			List<XMLVirtualMachineContainerType> vmcs,
			CmdVirtualApplication vmcCmd, 
			XMLEnvironmentContainerType environment, 
			ICloudManager cloudManager,
			XMLGeographicContainerType geographic, XMLEnvironmentType environmentType)
			throws StrategyFailureException, VCloudException,
			InvalidStrategyException, UnresolvedDependencyException,
			IOException, ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		logger.debug("Started executeVirtualMachineContainer");
		CmdCommand command = vmcCmd.getCommand();
		String componentName = "VirtualMachineContainer";
		//Setup object to be used in the execution of the instructions
		
		List<XMLVirtualMachineContainerType> vmcResults = selectPatternMatchedComponents(vmcs, vmcCmd, "getName");
		for(XMLVirtualMachineContainerType vmc : vmcResults) {
			HashMap<Class<?>, Object> objects = new HashMap<Class<?>, Object>();
			objects.put(vmcCmd.getStrategy().getClass(),
					vmcCmd.getStrategy());
			objects.put(environmentType.getClass(),
					environmentType);
			objects.put(vmc.getClass(), vmc);
			objects.put(geographic.getClass(),
					geographic);
			executeCommandWithStrategy(command,ComponentType.VirtualMachineContainer,cloudManager,vmcCmd,objects);
			
			executeAssignOrganisationNetworks(environment, vmcCmd, vmc, cloudManager, environmentType);
			executeAppNetworks(environment, vmcCmd, vmc, cloudManager, geographic, environmentType);
			executeVMs(environment, execute, vmcCmd, vmc, cloudManager, geographic, environmentType);
		}
		logger.debug("Finished executeVirtualMachineContainer");
	}
	
	/**
	 * Execute instructions relating to assigning org networks
	 * 
	 * @param appCmd
	 * @param app
	 * @throws StrategyFailureException
	 * @throws UnresolvedDependencyException
	 * @throws VCloudException
	 * @throws InvalidStrategyException
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	private void executeAssignOrganisationNetworks(XMLEnvironmentContainerType environment, CmdVirtualApplication vmcCmd,
			XMLVirtualMachineContainerType vmc, ICloudManager cloudManager, XMLEnvironmentType environmentType)
			throws StrategyFailureException, UnresolvedDependencyException,
			VCloudException, InvalidStrategyException, ConnectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		logger.debug("Started executeAssignOrganisationNetworks");
		CmdCommand command = vmcCmd.getCommand();
		String componentName = "AssignOrgNetwork";
		//Setup object to be used in the execution of the instructions
		
		List<XMLOrganisationalNetworkType> orgNetworks = new ArrayList<XMLOrganisationalNetworkType>();
		
		for(XMLVirtualMachineType vm : vmc.getVirtualMachine()) {
			for(XMLNICType nic : vm.getNIC()) {
				if(nic.getNetworkID() instanceof XMLOrganisationalNetworkType) {
					orgNetworks.add((XMLOrganisationalNetworkType)nic.getNetworkID());
				}
			}
		}
		
		for(XMLOrganisationalNetworkType network : orgNetworks) {
			HashMap<Class<?>, Object> objects = new HashMap<Class<?>, Object>();
			objects.put(vmcCmd.getStrategy().getClass(),
					vmcCmd.getStrategy());
			objects.put(vmc.getClass(),
					vmc);
			objects.put(environment.getClass(),
					environment);
			objects.put(environmentType.getClass(), environmentType);
			objects.put(network.getClass(), network);
			executeCommandWithStrategy(command,ComponentType.AssignOrgNetwork,cloudManager,vmcCmd,objects);
		}
		logger.debug("Finished executeAppNetworks");
		
	}

	/**
	 * Load the cloud config
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	public XMLGeographicContainerType loadConfiguration(File file)
			throws SAXException, IOException {
		logger.debug("Started loadConfiguration");
		logger.debug("Started loading the configuration '"
				+ file.getAbsolutePath() + "'");
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		XMLGeographicContainerType aCloud = loader.loadVC(file);
		logger.debug("Completed loading configuration.  Returning " + aCloud);
		logger.debug("Finished loadConfiguration");
		return aCloud;
	}

	/**
	 * Load the instructions
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	public CmdExecute loadInstructions(File file) throws SAXException,
			IOException {
		logger.debug("Started loadInstructions");
		logger.debug("Started loading the execution plan '"
				+ file.getAbsolutePath() + "'");
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		CmdExecute job = loader.loadJob(file);
		logger.debug("Completed loading the execution plan.  Returning " + job);
		logger.debug("Finished loadInstructions");
		return job;
	}

	/**
	 * Get definition object
	 * @param job
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	protected XMLGeographicContainerType getConfiguration() throws SAXException, IOException {
		return loadConfiguration(new File(definition));
	}
	
	protected XMLGeographicContainerType getConfigurationWithOverride(CmdExecute job) throws EnvironmentOverrideException, SAXException, IOException {
		boolean overridesPresent = false;
		XMLGeographicContainerType organisation = null;
		List<XMLEnvironmentType> environments = new  ArrayList<XMLEnvironmentType>();
		if(StringUtils.isNotBlank(definition)) {
			//  - Override any Xpath values that have been passed in in the job Command.
			if(job != null && job.getEnvironmentContainer() != null && job.getEnvironmentContainer().getEnvironment() != null && job.getEnvironmentContainer().getEnvironment().getOverrides() != null)
			{			
				List<CmdOverride> overrides = job.getEnvironmentContainer().getEnvironment().getOverrides().getOverride();
				if(overrides != null && overrides.size() > 0)
				{
					overridesPresent = true;
					processedDefinitionFile = new XPathHandler().applyEnvOverrides(definition, overrides, processedDefinitionFile);
				}		
			}		
			
			// Load the configuration to be managed - this may have been updated due to override values in the Command
			
			if(overridesPresent && processedDefinitionFile != null){
				logger.debug("Overrides have been detected - using the processed definition file");
				organisation = loadConfiguration(processedDefinitionFile);
			}	
			else {
				logger.debug("No overrides have been detected - using the original definition file");
				organisation = loadConfiguration(new File(definition));
			}
			
			if(processedDefinitionFile!= null && processedDefinitionFile.exists())
			{
				logger.debug("Deleting temporary overridden Environment Definition file.");
				FileUtils.deleteQuietly(processedDefinitionFile);
			}
		} else {
			
			File networkLayout = new File(getNetworkPath());
			if(!networkLayout.exists()) {
				throw new RuntimeException("The network layout xml was no found here "+getNetworkPath());
			}
			XMLGeographicContainerType orgNetworkLayout = loadConfiguration(networkLayout);
			
			//Comma delimited path separator
			String[] environmentFiles = getEnvironmentPath().split(",");
			
			for(int i = 0; i< environmentFiles.length; i++) {
				String currentFile = environmentFiles[i];
				if(!new File(currentFile).exists()) {
					throw new RuntimeException("Environment file "+currentFile+"specified does not exist");
				}
				
				if(job != null && job.getEnvironmentContainer() != null && job.getEnvironmentContainer().getEnvironment() != null && job.getEnvironmentContainer().getEnvironment().getOverrides() != null)
				{			
					List<CmdOverride> overrides = job.getEnvironmentContainer().getEnvironment().getOverrides().getOverride();
					if(overrides != null && overrides.size() > 0)
					{
						overridesPresent = true;
						processedDefinitionFile = new XPathHandler().applyEnvOverrides(currentFile, overrides, processedDefinitionFile);
					}		
				}		
				XMLGeographicContainerType environment = null;
				// Load the configuration to be managed - this may have been updated due to override values in the Command
				
				if(overridesPresent && processedDefinitionFile != null){
					logger.debug("Overrides have been detected - using the processed definition file");
					environment = loadConfiguration(processedDefinitionFile);
				}	
				else {
					logger.debug("No overrides have been detected - using the original definition file");
					environment = loadConfiguration(new File(currentFile));
				}
				
				if(processedDefinitionFile!= null && processedDefinitionFile.exists())
				{
					logger.debug("Deleting temporary overridden Environment Definition file.");
					FileUtils.deleteQuietly(processedDefinitionFile);
				}
				environments.addAll(environment.getEnvironmentContainer().getEnvironment());
			}
			
			organisation = amalgamateAndLinkEnvironmentDefinitions(orgNetworkLayout,environments);
		}
		return organisation;
	}
	
	/**
	 * Link together the org networks, environment, data centers and gateways with the VMC's, VM's and app networks. This will allow the disassociated
	 * layout and environment definitions to be processed by the tool.
	 * @param networks
	 * @param environments
	 * @return 
	 */
	private XMLGeographicContainerType amalgamateAndLinkEnvironmentDefinitions(XMLGeographicContainerType networks, List<XMLEnvironmentType> environments) {
		
		XMLEnvironmentContainerDefinition envDef = networks.getEnvironmentContainer().getEnvironmentContainerDefinition().get(0);
		Map<String,XMLOrganisationalNetworkType> orgNetworksByName = new HashMap<String,XMLOrganisationalNetworkType>();
		Map<String,XMLDataCenterType> dataCentersByName = new HashMap<String,XMLDataCenterType>();
		
		for(XMLOrganisationalNetworkType orgNet : envDef.getNetwork()) {
			orgNetworksByName.put(orgNet.getName(), orgNet);
		}
		
		for(XMLDataCenterType dc : envDef.getDataCenter()) {
			dataCentersByName.put(dc.getName(), dc);
		}
		//Make sure we only load networks from the network definition, so discard any environment information.
		networks.getEnvironmentContainer().getEnvironment().clear();
		
		for(XMLEnvironmentType environment :environments) {
			Map<String,XMLVirtualMachineContainerType> vmcs = new HashMap<String,XMLVirtualMachineContainerType>();
			//Set the environment definition to link the org networks to the environment
			environment.setEnvironmentContainerDefinitionId(envDef);
			//Loop over all the VMC's for this environment
			for(XMLVirtualMachineContainerType vmc : environment.getEnvironmentDefinition().get(0).getVirtualMachineContainer()) {
				if(vmcs.containsKey(vmc.getName())) {
					throw new RuntimeException("There are duplicate Virtual Machine Container elements in the files you have specified. "+vmc.getName()+" is a duplicate name.");
				}
				vmcs.put(vmc.getName(),vmc);
				vmc.setDataCenterId(dataCentersByName.get(vmc.getDataCenterName()));
				Map<String,XMLApplicationNetworkType> vmcAppNetworks = new HashMap<String,XMLApplicationNetworkType>();

				for(XMLApplicationNetworkType appnet: vmc.getNetwork()) {
					vmcAppNetworks.put(appnet.getName(), appnet);
					appnet.setDataCenterId(dataCentersByName.get(vmc.getDataCenterName()));
				}
				
				for(XMLVirtualMachineType vm :vmc.getVirtualMachine()) {
					for(XMLNICType nic: vm.getNIC()) {
						if(vmcAppNetworks.get(nic.getNetworkName()) != null) {
							nic.setNetworkID(vmcAppNetworks.get(nic.getNetworkName()));
						} else if(orgNetworksByName.get(nic.getNetworkName()) != null) {
							nic.setNetworkID(orgNetworksByName.get(nic.getNetworkName()));
						} else {
							throw new RuntimeException(String.format("Invalid network definition for vm %s in vmc %s in envrionment %s", vm.getVmName(),vmc.getName(), environment.getName()));
						}
						SubnetUtils utils = new SubnetUtils(nic.getNetworkID().getCIDR());
						for(XMLInterfaceType ipAddress:nic.getInterface()) {
							if(!utils.getInfo().isInRange(ipAddress.getStaticIpAddress())) {
								throw new RuntimeException(String.format("Nic %s for vm %s on network %s is out of the CIDR range %s using ip address %s. This needs to be corrected!", ipAddress.getInterfaceNumber(),vm.getVmName(), nic.getNetworkID().getName(), nic.getNetworkID().getCIDR(), ipAddress.getStaticIpAddress()));
							}
						}
					}
				}
			}
			networks.getEnvironmentContainer().getEnvironment().add(environment);
		}
		
		return networks;
	}
	
	/**
	 * Get instruction object
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	protected CmdExecute getInstructions() throws SAXException, IOException {
		if (instructions == null) {
			instructions = loadInstructions(new File(executionplan));
		}
		return instructions;
	}
	
	/**
	 * Load the cloud config
	 * 
	 *  - need to be able to test at this level, so have to set the definition value if null.
	 * 
	 * NB - Should never be used in live running outside of unit tests.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	public XMLGeographicContainerType loadConfigurationForTest(File file) throws SAXException, IOException {
		logger.debug("Started loading the definition '" + file.getAbsolutePath() + "'");
		
		//  - need to be able to test at this level, so have to set this. Should never occur in live running outside of unit tests.
		definition = file.getAbsolutePath();
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		XMLGeographicContainerType config = loader.loadVC(file);
		logger.debug("Completed loading definition.  Returning " + config);
		return config;
	}
	
	
	/**
	 * Load the cloud config
	 * 
	 *  - need to be able to test at this level, so have to set the definition value if null.
	 * 
	 * NB - Should never be used in live running outside of unit tests.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	public XMLGeographicContainerType loadEnvironmentContainerDefinitionForTest(File file) throws SAXException, IOException {
		logger.debug("Started loading the definition '" + file.getAbsolutePath() + "'");
		
		//  - need to be able to test at this level, so have to set this. Should never occur in live running outside of unit tests.
		definition = file.getAbsolutePath();
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		XMLGeographicContainerType config = loader.loadVC(file);
		logger.debug("Completed loading definition.  Returning " + config);
		return config;
	}
	
	public File writeConfigurationForTest(XMLGeographicContainerType geographic, String filename) throws SAXException, IOException {
		logger.debug("Started writing the definition");
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		File config = loader.write(geographic,filename);
		definition = config.getAbsolutePath();
		logger.debug("Completed loading definition.  Returning " + config);
		return config;
	}
	
	public File writeInstructionForTest(CmdExecute instruction, String filename) throws SAXException, IOException {
		logger.debug("Started writing the definition");
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		File config = loader.write(instruction,filename);
		definition = config.getAbsolutePath();
		logger.debug("Completed loading definition.  Returning " + config);
		return config;
	}

	/**
	 * Load the instructions for Unit Test cases.
	 * 
	 *  - need to be able to test at this level, so have to set the executionplan value if null.
	 * 
	 * NB - Should never be used in live running outside of unit tests.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	public CmdExecute loadInstructionsForTest(File file) throws SAXException, IOException {
		logger.debug("Started loading the execution plan '" + file.getAbsolutePath() + "'");
		instructions = null;
		//  - need to be able to test at this level, so have to set this. Should never occur in live running outside of unit tests.
		executionplan = file.getAbsolutePath();
		ACloudConfigurationLoader loader = new ACloudConfigurationLoader();
		CmdExecute job = loader.loadJob(file);
		logger.debug("Completed loading the execution plan.  Returning " + job);
		return job;
	}

	public String getNetworkPath() {
		return networkLayout;
	}

	protected void setNetworkPath(String networkPath) {
		this.networkLayout = networkPath;
	}

	public String getEnvironmentPath() {
		return environments;
	}

	protected void setEnvironmentPath(String environmentPath) {
		this.environments = environmentPath;
	}
}