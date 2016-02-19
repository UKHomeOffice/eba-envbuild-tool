package com.ipt.ebsa.agnostic.client.aws.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Vpc;
import com.ipt.ebsa.agnostic.client.aws.datastructures.StartupShutdownInfo;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.logging.LogUtils;
import com.ipt.ebsa.agnostic.client.logging.LogUtils.LogAction;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.jcabi.aspects.Loggable;

/**
 * 
 *
 */
@Loggable(prepend=true)
public class AwsVmContainerModule extends AwsModule{
	
	private Logger logger = LogManager.getLogger(AwsVmContainerModule.class);

	@Inject
	AwsSecurityGroupModule securityGroupModule;
	
	@Inject
	AwsVmModule vmModule;
	
	@Inject
	private StrategyHandler strategyHandler;
	
	public String createVirtualMachineContainer(
			XMLEnvironmentType env,
			XMLVirtualMachineContainerType vmc) throws InterruptedException {
		Vpc vpc = getVpc(env);
		String groupId = securityGroupModule.createSecurityGroup(AwsNamingUtil.getVmcName(vmc), vpc.getVpcId(), vmc.getDescription(), vmc);
		ArrayList<Tag> tags = new ArrayList<Tag>();
		tags.add(new Tag("Name", AwsNamingUtil.getVmcName(vmc)));
		tags.add(new Tag("domain", vmc.getDomain()));
		createTags(tags, groupId);
		return groupId;
	}
	
	public void deleteVirtualMachineContainer(
			XMLEnvironmentType env,
			XMLVirtualMachineContainerType vmc) throws InterruptedException {
		securityGroupModule.deleteSecurityGroup(env, vmc, getVpc(env));
	}
	
	public void deleteVmc(SecurityGroup sg) {
		securityGroupModule.deleteSecurityGroup(sg);
	}
	
	public Collection<String> confirmVirtualMachineContainer(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc) throws InterruptedException, StrategyFailureException, InvalidStrategyException {
		Vpc vpc = getVpc(env);
		String vmcInstance = null;
		Collection<String> vmcSecurityGroup = new ArrayList<String>();
		if(vpc != null) {
			vmcSecurityGroup = securityGroupModule.getSecurityGroups(env, vmc, vpc);
			if(!vmcSecurityGroup.isEmpty()) {
				vmcInstance = StringUtils.EMPTY;
			}
			logger.debug(String.format("Located %s security groups for vmc %s", vmcSecurityGroup.size(), AwsNamingUtil.getVmcName(vmc)));
		}
		
		strategyHandler.resolveConfirmStrategy(strategy, vmcInstance, "Virtual Machine Container", AwsNamingUtil.getVmcName(vmc), " confirm");
		
		return vmcSecurityGroup;
	}

	public void mergeVirtualMachineContainer(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc) {
		logger.warn("NOT IMPLEMENTED mergeVirtualMachineContainer");//TODO write this method
	}

	public void overwriteVirtualMachineContainer(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc) {
		logger.warn("NOT IMPLEMENTED overwriteVirtualMachineContainer");//TODO write this method
		
	}
	
	private HashMap<String, Instance> getInstanceMap(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc) {
		Vpc vpc = getVpc(env);
		HashMap<String, Instance> instanceLookup = new HashMap<String, Instance>();
		List<Instance> vms = getAllVmcInstances(env,vmc,vpc);
		for(Instance iVm :vms) {
			String vmName = StringUtils.EMPTY;
			for(Tag tag : iVm.getTags()) {
				if(tag.getKey().contains("Name")) {
					vmName = tag.getValue();
					instanceLookup.put(vmName, iVm);
					break;
				}
			}
		}
		return instanceLookup;
	}
	
	private List<StartupShutdownInfo> sortVms(List<StartupShutdownInfo> sortedVms) {
		Collections.sort(sortedVms, new Comparator<StartupShutdownInfo>() {
			@Override
			public int compare(StartupShutdownInfo vm1, StartupShutdownInfo vm2) {
				return (vm1.getOrder() > vm2.getOrder()) ? 1 : -1;
			}

		});
		return sortedVms;
	}
	
	private HashMap<Integer,StartupShutdownInfo> getVmMap(XMLVirtualMachineContainerType vmc) {
		HashMap<Integer,StartupShutdownInfo> map = new HashMap<Integer,StartupShutdownInfo>();
		
		for(XMLVirtualMachineType vm : vmc.getVirtualMachine()) {
			StartupShutdownInfo info = map.get(vm.getVMOrder());
			
			if(info == null) {
				info = new StartupShutdownInfo();
				info.setOrder(vm.getVMOrder());
				info.setVmc(vmc);
				map.put(vm.getVMOrder(),info);
			}
			info.getVmList().add(vm);
		}
		
		return map;
	}

	public void stopVirtualMachineContainer(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc) {
		
		LogUtils.log(LogAction.STOPPING, "Virtual Machine Container " + AwsNamingUtil.getVmcName(vmc));
		HashMap<String, Instance> instanceLookup = getInstanceMap(env,vmc);
		HashMap<Integer,StartupShutdownInfo> map = getVmMap(vmc);
		List<StartupShutdownInfo> sortedVMs = sortVms(new ArrayList<StartupShutdownInfo>(map.values()));
		
		for (StartupShutdownInfo vm : sortedVMs) {
			Collection<String> ids = vm.getInstanceIds(instanceLookup);
			vmModule.stopVirtualMachine(ids);

			for (String id : ids) {
				try {
					vmModule.waitForInstanceStatus(id, InstanceStateName.Stopped, true);
				} catch (UnSafeOperationException e) {
					logger.debug("Encountered an exception while stopping vm " + id, e);
				}
			}

			waitForVmTimeout(vm.getVmList().get(0), vm.getVmList().get(0).getVMStopDelay());
		}
		LogUtils.log(LogAction.STOPPED, "Virtual Machine Container " + AwsNamingUtil.getVmcName(vmc));
	}

	public void startVirtualMachineContainer(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc) {

		LogUtils.log(LogAction.STARTING, "Virtual Machine Container " + AwsNamingUtil.getVmcName(vmc));
		HashMap<String, Instance> instanceLookup = getInstanceMap(env,vmc);
		HashMap<Integer,StartupShutdownInfo> map = getVmMap(vmc);
		List<StartupShutdownInfo> sortedVMs = sortVms(new ArrayList<StartupShutdownInfo>(map.values()));

		for (StartupShutdownInfo vm : sortedVMs) {
			Collection<String> ids = vm.getInstanceIds(instanceLookup);
			vmModule.startVirtualMachine(ids);

			for (String id : ids) {
				try {
					vmModule.waitForInstanceStatus(id, InstanceStateName.Running, true);
				} catch (UnSafeOperationException e) {
					logger.debug("Encountered an exception while starting vm " + id, e);
				}
			}

			waitForVmTimeout(vm.getVmList().get(0), vm.getVmList().get(0).getVMStopDelay());
		}
		LogUtils.log(LogAction.STARTED, "Virtual Machine Container " + AwsNamingUtil.getVmcName(vmc));
	}
	
	private void waitForVmTimeout(XMLVirtualMachineType vm, Integer delay) {
		try {
			Thread.sleep(delay * 1000);
		} catch (InterruptedException e) {
			logger.error(String.format("A thread InterruptedException was recieved while performing delay for vm &s ", vm.getVmName()), e);
		}
	}

}
