package com.ipt.ebsa.agnostic.client.manager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.ipt.ebsa.agnostic.client.aws.exception.PeeringChoreographyException;
import com.ipt.ebsa.agnostic.client.aws.exception.VpcUnavailableException;
import com.ipt.ebsa.agnostic.client.exception.CreateFailException;
import com.ipt.ebsa.agnostic.client.exception.FatalException;
import com.ipt.ebsa.agnostic.client.exception.RetryableException;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.ConnectionException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
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
import com.microsoft.windowsazure.exception.ServiceException;
import com.vmware.vcloud.sdk.VCloudException;

/**
 * 
 *
 */
public interface ICloudManager {

	public void createEnvironmentContainer(CmdStrategy strategy, XMLEnvironmentContainerType env, XMLGeographicContainerType geographic)
			throws InterruptedException, ExecutionException, ServiceException, IOException, ParserConfigurationException, SAXException,
			TransformerException, URISyntaxException, NullPointerException, RetryableException, StrategyFailureException, InvalidStrategyException,
			FatalException;

	public void createEdgeGateway(CmdStrategy strategy, XMLEnvironmentType env, XMLGatewayType gatewayConfig) throws InterruptedException,
			ToManyResultsException, StrategyFailureException, InvalidStrategyException;

	public void createOrganisationNetwork(CmdStrategy strategy, XMLEnvironmentType env, XMLOrganisationalNetworkType orgNetworkConfig,
			XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException, UnresolvedDependencyException,
			VCloudException, InvalidStrategyException, ConnectionException, NullPointerException, IOException, ServiceException,
			ParserConfigurationException, SAXException, CreateFailException, TransformerException, ExecutionException, RetryableException, PeeringChoreographyException, ToManyResultsException, UnSafeOperationException;

	public void createVirtualMachineContainer(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException, VCloudException, InvalidStrategyException,
			ConnectionException, IOException, ServiceException, ParserConfigurationException, SAXException, RetryableException, ExecutionException;

	public void createApplicationNetwork(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLApplicationNetworkType appNetworkType, XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException,
			UnresolvedDependencyException, VCloudException, InvalidStrategyException, ConnectionException, NullPointerException, CreateFailException,
			IOException, ServiceException, ParserConfigurationException, SAXException, TransformerException, ExecutionException, RetryableException, PeeringChoreographyException, ToManyResultsException, UnSafeOperationException;

	public void createVirtualMachine(CmdExecute execute, CmdStrategy strategy, XMLEnvironmentType environment,
			XMLVirtualMachineContainerType vmc, XMLVirtualMachineType virtualMachine, XMLGeographicContainerType geographic)
			throws InterruptedException, StrategyFailureException, UnresolvedDependencyException, VCloudException, InvalidStrategyException,
			IOException, ConnectionException, Exception, Throwable;

	public void createAssignOrgNetwork(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLOrganisationalNetworkType orgNetworkType) throws StrategyFailureException, UnresolvedDependencyException, VCloudException,
			InvalidStrategyException, ConnectionException;

	public void sandbox();

	public void confirmVirtualMachineContainer(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException, InvalidStrategyException, VCloudException,
			ConnectionException;

	public void createEnvironment(CmdStrategy strategy, XMLEnvironmentType env, XMLGeographicContainerType geographic)
			throws InterruptedException, StrategyFailureException, InvalidStrategyException, ToManyResultsException, UnSafeOperationException, ExecutionException, ServiceException, IOException, ParserConfigurationException, SAXException, TransformerException, URISyntaxException, NullPointerException, RetryableException, FatalException;

	public void confirmOrganisationNetwork(CmdStrategy strategy, XMLEnvironmentType env,
			XMLOrganisationalNetworkType orgNetworkType) throws StrategyFailureException, UnresolvedDependencyException, VCloudException,
			InvalidStrategyException, ConnectionException, InterruptedException, VpcUnavailableException;
}
