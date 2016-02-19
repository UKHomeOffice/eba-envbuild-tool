package com.ipt.ebsa.agnostic.client.aws.manager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.ipt.ebsa.agnostic.client.aws.exception.PeeringChoreographyException;
import com.ipt.ebsa.agnostic.client.aws.exception.SubnetUnavailableException;
import com.ipt.ebsa.agnostic.client.aws.exception.VpcUnavailableException;
import com.ipt.ebsa.agnostic.client.aws.module.AwsEnvironmentModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsGatewayModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsNetworkModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsVmContainerModule;
import com.ipt.ebsa.agnostic.client.aws.module.AwsVmModule;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.client.config.Config;
import com.ipt.ebsa.agnostic.client.exception.FatalException;
import com.ipt.ebsa.agnostic.client.exception.ResourceInUseException;
import com.ipt.ebsa.agnostic.client.exception.RetryableException;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.logging.LogUtils;
import com.ipt.ebsa.agnostic.client.logging.LogUtils.LogAction;
import com.ipt.ebsa.agnostic.client.manager.CloudManager;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler.Action;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLApplicationNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGatewayType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.jcabi.aspects.Loggable;
import com.microsoft.windowsazure.exception.ServiceException;
import com.vmware.vcloud.sdk.VCloudException;

/**
 * 
 *
 */
@Loggable(prepend = true)
public class AwsCloudManager extends CloudManager implements IAwsManager {

	private Logger logger = LogManager.getLogger(AwsCloudManager.class);

	@Inject
	private AwsVmContainerModule vmContainerManager;

	@Inject
	private AwsNetworkModule networkManager;

	@Inject
	private AwsGatewayModule gatewayManager;

	@Inject
	private AwsVmModule vmManager;

	@Inject
	private AwsEnvironmentModule envManager;

	@Inject
	private StrategyHandler strategyHandler;

	@Inject
	@Config
	private String searchBootstrapString;

	@Override
	public void createEnvironment(CmdStrategy strategy, XMLEnvironmentType env, XMLGeographicContainerType geographic)
			throws InterruptedException, StrategyFailureException, InvalidStrategyException, ToManyResultsException, UnSafeOperationException {
		LogUtils.log(LogAction.CREATING, "Environment", env, "name");
		Vpc existingVpc = envManager.getVpc(env);
		Action action = strategyHandler.resolveCreateStrategy(strategy, existingVpc, "VPC", AwsNamingUtil.getEnvironmentName(env), "getting vpc");
		switch (action) {
		case UPDATE:
			logger.error("Updating an Environment object is not supported.");
			break;
		case DESTROY_THEN_CREATE:
			envManager.deleteVpc(env, "");
		case CREATE:
			envManager.createVpc(env);
			break;
		case DELETE:
			envManager.deleteVpc(env, "");
			break;
		case DO_NOTHING:
			break;
		default:
			break;
		}
	}

	@Override
	public void createEdgeGateway(CmdStrategy strategy, XMLEnvironmentType env, XMLGatewayType gatewayConfig) throws InterruptedException,
			ToManyResultsException, StrategyFailureException, InvalidStrategyException {
		InternetGateway existing = gatewayManager.getEdgeGateway(env, gatewayConfig);
		Action action = strategyHandler.resolveCreateStrategy(strategy, existing, "Interget Gateway", AwsNamingUtil.getGatewayName(gatewayConfig), "getting geteway");
		switch (action) {
		case CREATE:
			LogUtils.log(LogAction.CREATING, "Gateway", gatewayConfig, "name");
			InternetGateway iGate = gatewayManager.createGateway(env, gatewayConfig);
			RouteTable rTable = gatewayManager.updateRouteTable(env, gatewayConfig, iGate, "0.0.0.0/0");
			break;
		case UPDATE:
			gatewayManager.updateGateway(env, gatewayConfig);
			break;
		case DESTROY_THEN_CREATE:
		case DELETE:
		case DO_NOTHING:
		default:
			break;
		}
	}

	@Override
	public void createOrganisationNetwork(CmdStrategy strategy, XMLEnvironmentType env, XMLOrganisationalNetworkType orgNetworkConfig,
			XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException, InvalidStrategyException,
			PeeringChoreographyException, ToManyResultsException, UnSafeOperationException {
		Subnet existing = networkManager.getSubnet(env, orgNetworkConfig);
		Action action = strategyHandler.resolveCreateStrategy(strategy, existing, "Organisation Network", AwsNamingUtil.getNetworkName(orgNetworkConfig),
				"getting org network");
		switch (action) {
		case CREATE:
			LogUtils.log(LogAction.CREATING, "OrganisationNetwork", orgNetworkConfig, "name", "CIDR");
			networkManager.createOrganisationSubnet(env, orgNetworkConfig);
		case UPDATE:
		case DESTROY_THEN_CREATE:
		case DELETE:
		case DO_NOTHING:
		default:
			break;
		}
	}

	@Override
	public void createVirtualMachineContainer(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException, InvalidStrategyException {
		LogUtils.log(LogAction.CREATING, "VirtualMachineContainer", vmc, "name", "domain", "description");

		Vpc vpc = vmContainerManager.getVpc(env);
		Boolean vmcExists = Boolean.FALSE;
		List<SecurityGroup> vmcs = vmContainerManager.getVmcs(vpc);
		for (SecurityGroup existingVmc : vmcs) {
			if (existingVmc.getGroupName().equals(AwsNamingUtil.getVmcName(vmc))) {
				vmcExists = Boolean.TRUE;
				break;
			}
		}

		Action action = strategyHandler.resolveCreateStrategy(strategy, vmcExists, "Vmc", AwsNamingUtil.getVmcName(vmc), "creating Vmc");

		switch (action) {
		case CREATE:
			vmContainerManager.createVirtualMachineContainer(env, vmc);
			break;
		case UPDATE:
		case DESTROY_THEN_CREATE:
		case DELETE:
		case DO_NOTHING:
		default:
			break;
		}
	}

	@Override
	public void createApplicationNetwork(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLApplicationNetworkType appNetworkType, XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException,
			InvalidStrategyException, PeeringChoreographyException, ToManyResultsException, UnSafeOperationException {
		Subnet existing = networkManager.getSubnet(env, appNetworkType);
		Action action = strategyHandler.resolveCreateStrategy(strategy, existing, "Application Network", AwsNamingUtil.getNetworkName(appNetworkType),
				"getting app network");
		switch (action) {
		case CREATE:
			LogUtils.log(LogAction.CREATING, "ApplicationNetwork", appNetworkType, "name", "CIDR", "description");
			networkManager.createApplicationSubnet(env, appNetworkType, vmc);
		case UPDATE:
		case DESTROY_THEN_CREATE:
		case DELETE:
		case DO_NOTHING:
		default:
			break;
		}
	}

	@Override
	public void createVirtualMachine(CmdExecute execute, CmdStrategy strategy, XMLEnvironmentType env,
			XMLVirtualMachineContainerType vmc, XMLVirtualMachineType virtualMachine, XMLGeographicContainerType geographic)
			throws InterruptedException, ToManyResultsException, StrategyFailureException, InvalidStrategyException, UnSafeOperationException,
			ResourceInUseException, UnresolvedDependencyException, IOException, SubnetUnavailableException, FatalException {
		LogUtils.log(LogAction.CREATING, "VirtualMachine", "vApp=" + AwsNamingUtil.getVmcName(vmc), virtualMachine, "computerName", "description");

		Vpc vpc = vmManager.getVpc(env);
		Instance vm = vmManager.getInstance(env, virtualMachine, vmc, vpc);

		Action theAction = strategyHandler.resolveCreateStrategy(strategy, vm, "VM", virtualMachine.getVmName(), "merging vm");

		switch (theAction) {
		case CREATE:
			vmManager.createVirtualMachine(env, vmc, virtualMachine, geographic);
			break;
		case CREATE_WAIT:
			Instance created = vmManager.createVirtualMachine(env, vmc, virtualMachine, geographic);
			vmManager.waitForInstanceStatus(created.getInstanceId(), InstanceStateName.Running, true);
			vmManager.waitForInstanceConsoleOutput(created.getInstanceId(), searchBootstrapString, true);
			break;
		case DELETE:
			vmManager.deleteVirtualMachine(vm);
			break;
		case DESTROY_THEN_CREATE:
			vmManager.deleteVirtualMachine(vm);
			vmManager.waitForInstanceStatus(vm.getInstanceId(), InstanceStateName.Terminated, true);
			vmManager.createVirtualMachine(env, vmc, virtualMachine, geographic);
			break;
		case UPDATE:
			vmManager.updateVirtualMachine(env, vmc, virtualMachine, geographic, vm);
			break;
		case UPDATE_WAIT:
			Instance updated = vmManager.updateVirtualMachine(env, vmc, virtualMachine, geographic, vm);
			vmManager.waitForInstanceStatus(updated.getInstanceId(), InstanceStateName.Running, true);
			vmManager.waitForInstanceConsoleOutput(updated.getInstanceId(), searchBootstrapString, true);
			break;
		case DO_NOTHING:
			break;
		default:
			break;
		}

	}

	@Override
	public void createAssignOrgNetwork(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLOrganisationalNetworkType orgNetworkType) throws StrategyFailureException, UnresolvedDependencyException, VCloudException,
			InvalidStrategyException, ConnectionException {
		logger.warn("AssignOrgNetwork operation not possible for this provider");
	}

	public void confirmAssignOrgNetwork(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLOrganisationalNetworkType orgNetworkType) throws StrategyFailureException, UnresolvedDependencyException, VCloudException,
			InvalidStrategyException, ConnectionException {
		logger.warn("ConfirmAssignOrgNetwork operation not possible for this provider");
	}

	public void deleteAssignOrgNetwork(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLOrganisationalNetworkType orgNetworkType) throws StrategyFailureException, UnresolvedDependencyException, VCloudException,
			InvalidStrategyException, ConnectionException {
		logger.warn("DeleteAssignOrgNetwork operation not possible for this provider");
	}

	@Override
	public void sandbox() {
		logger.warn("sandbox method not implemented");
	}

	public void confirmEnvironment(CmdStrategy strategy, XMLEnvironmentType env) throws InterruptedException, StrategyFailureException,
			InvalidStrategyException, ToManyResultsException, VpcUnavailableException {
		envManager.confirmVpc(strategy, env);
	}

	@Override
	public void confirmVirtualMachineContainer(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException, InvalidStrategyException {
		vmContainerManager.confirmVirtualMachineContainer(strategy, env, vmc);
	}

	public void confirmApplicationNetwork(CmdStrategy strategy, XMLEnvironmentType env, XMLApplicationNetworkType network)
			throws InterruptedException, StrategyFailureException, InvalidStrategyException, VpcUnavailableException {
		networkManager.confirmSubnet(strategy, env, network);
	}

	public void confirmOrganisationNetwork(CmdStrategy strategy, XMLEnvironmentType env, XMLOrganisationalNetworkType network)
			throws InterruptedException, StrategyFailureException, InvalidStrategyException, VpcUnavailableException {
		networkManager.confirmSubnet(strategy, env, network);
	}

	public void confirmVirtualMachine(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLVirtualMachineType virtualMachine, XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException,
			InvalidStrategyException, ToManyResultsException, VpcUnavailableException {
		vmManager.confirmVirtualMachine(strategy, env, vmc, virtualMachine, geographic);
	}

	public void confirmEdgeGateway(CmdStrategy strategy, XMLEnvironmentType env, XMLGatewayType gatewayConfig) throws ToManyResultsException,
			StrategyFailureException, InvalidStrategyException {
		gatewayManager.confirmEdgeGateway(strategy, env, gatewayConfig);
	}

	public void deleteVirtualMachine(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc, XMLVirtualMachineType virtualMachine)
			throws VpcUnavailableException, ToManyResultsException, UnSafeOperationException {
		vmManager.deleteVirtualMachine(env, vmc, virtualMachine);
	}

	public void deleteApplicationNetwork(XMLEnvironmentType env, XMLApplicationNetworkType network) throws VpcUnavailableException,
			ToManyResultsException {
		networkManager.deleteSubnet(env, network);
	}

	public void deleteOrganisationNetwork(XMLEnvironmentType env, XMLOrganisationalNetworkType network) throws VpcUnavailableException,
			ToManyResultsException {
		networkManager.deleteSubnet(env, network);
	}

	public void deleteVirtualMachineContainer(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc) throws InterruptedException {
		vmContainerManager.deleteVirtualMachineContainer(env, vmc);
	}

	public void deleteEdgeGateway(XMLEnvironmentType env, XMLGatewayType gatewayConfig) throws ToManyResultsException {
		gatewayManager.deleteGateway(env, gatewayConfig);
	}

	public void deleteEnvironment(XMLEnvironmentType env) throws ToManyResultsException, InterruptedException, UnSafeOperationException {
		envManager.deleteVpc(env, "");
	}

	@Override
	public void createEnvironmentContainer(CmdStrategy strategy, XMLEnvironmentContainerType env, XMLGeographicContainerType geographic)
			throws InterruptedException, ExecutionException, ServiceException, IOException, ParserConfigurationException, SAXException,
			TransformerException, URISyntaxException, NullPointerException, RetryableException, StrategyFailureException, InvalidStrategyException,
			FatalException {

	}

	public void stopVirtualMachineContainer(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc) {
		vmContainerManager.stopVirtualMachineContainer(env, vmc);
	}

	public void startVirtualMachineContainer(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc) {
		vmContainerManager.startVirtualMachineContainer(env, vmc);
	}

}
