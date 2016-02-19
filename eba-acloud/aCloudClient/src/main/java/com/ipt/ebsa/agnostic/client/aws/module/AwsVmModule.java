package com.ipt.ebsa.agnostic.client.aws.module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AttachNetworkInterfaceRequest;
import com.amazonaws.services.ec2.model.AttachNetworkInterfaceResult;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest;
import com.amazonaws.services.ec2.model.GetConsoleOutputResult;
import com.amazonaws.services.ec2.model.IamInstanceProfile;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification;
import com.amazonaws.services.ec2.model.InstancePrivateIpAddress;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.NetworkInterfaceStatus;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeState;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.elasticloadbalancing.model.InvalidInstanceException;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.Role;
import com.ipt.ebsa.agnostic.client.aws.exception.SubnetUnavailableException;
import com.ipt.ebsa.agnostic.client.aws.exception.VpcUnavailableException;
import com.ipt.ebsa.agnostic.client.aws.extensions.IptNetworkInterface;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.Retryable;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.WaitCondition;
import com.ipt.ebsa.agnostic.client.aws.module.handler.UserDataHandler;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.client.config.Config;
import com.ipt.ebsa.agnostic.client.controller.operation.DiskOperationHolder;
import com.ipt.ebsa.agnostic.client.controller.operation.DiskOperationType;
import com.ipt.ebsa.agnostic.client.controller.operation.HardwareOperationType;
import com.ipt.ebsa.agnostic.client.controller.operation.HardwareProfileOperationHolder;
import com.ipt.ebsa.agnostic.client.controller.operation.NicOperationHolder;
import com.ipt.ebsa.agnostic.client.controller.operation.NicOperationtype;
import com.ipt.ebsa.agnostic.client.exception.FatalException;
import com.ipt.ebsa.agnostic.client.exception.ResourceInUseException;
import com.ipt.ebsa.agnostic.client.exception.ToManyResultsException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.logging.LogUtils;
import com.ipt.ebsa.agnostic.client.logging.LogUtils.LogAction;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.skyscape.manager.VMManager;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler;
import com.ipt.ebsa.agnostic.client.util.MandatoryCheck;
import com.ipt.ebsa.agnostic.client.util.StringTools;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLInterfaceType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLNICType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLOrganisationalNetworkType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLStorageType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.jcabi.aspects.Loggable;

/**
 * This module allows operations to be performed on virtual machines. It
 * contains the logic and choreography code to create, delete and update virtual
 * machines
 * 
 *
 */
@Loggable(prepend = true)
public class AwsVmModule extends AwsModule {

	private static final String NICS = "NICS";
	private static final String GATEWAY_NICS = "GATEWAY_NICS";

	private Logger logger = LogManager.getLogger(AwsVmModule.class);

	@Inject
	AwsSecurityGroupModule securityGroupModule;

	@Inject
	AwsGatewayModule gatewayManager;

	@Inject
	AwsVolumeModule volumeModule;

	@Inject
	private AwsNetworkModule networkManager;

	@Inject
	private AwsRoleModule roleManager;

	@Inject
	private StrategyHandler strategyHandler;

	@Inject
	@Config
	private String guestCustScriptDir;

	@Inject
	@Config
	private String toolingDomain;

	@Inject
	@Config
	private String bootstrapAdditionalTimeout;

	@Inject
	@Config
	String keyName = "ebsa-aws-client";

	public Instance createVirtualMachine(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc, XMLVirtualMachineType virtualMachine,
			XMLGeographicContainerType geographic) throws UnSafeOperationException, ResourceInUseException, InterruptedException,
			UnresolvedDependencyException, IOException, ToManyResultsException, SubnetUnavailableException, FatalException {

		Vpc vpc = getVpc(env);
		HashMap<String, ArrayList<IptNetworkInterface>> nicMap = createAllNics(env, vmc, virtualMachine);
		ArrayList<IptNetworkInterface> nics = nicMap.get(NICS);
		Collection<String> securityGroups = securityGroupModule.getSecurityGroups(env, vmc, vpc);
		RunInstancesResult instanceResult = createInstance(securityGroups, nics, virtualMachine, vmc, null, vpc);
		Instance resultInstance = instanceResult.getReservation().getInstances().get(0);

		ArrayList<IptNetworkInterface> gatewayNics = nicMap.get(GATEWAY_NICS);
		if (gatewayNics != null) {
			for (IptNetworkInterface gatewayNic : gatewayNics) {
				List<Address> ipAddress = gatewayManager.getUnallocatedGatewayElasticIpAddress(vpc);
				if (ipAddress != null && ipAddress.size() == 0) {
					AllocateAddressResult createdAddress = gatewayManager.createElasticIPAddress();
					gatewayManager.associateElasticIPAddress(resultInstance.getInstanceId(), createdAddress.getAllocationId(),
							createdAddress.getPublicIp(), gatewayNic);
				} else {
					gatewayManager.associateElasticIPAddress(resultInstance.getInstanceId(), ipAddress.get(0).getAllocationId(), ipAddress.get(0)
							.getPublicIp(), gatewayNic);
				}
			}
		}

		ArrayList<Tag> tags = new ArrayList<Tag>();
		tags.add(new Tag("InstanceType", virtualMachine.getHardwareProfile()));
		createTags(AwsNamingUtil.getVmName(virtualMachine, vmc), resultInstance.getInstanceId(), vmc, tags);

		return resultInstance;
	}

	public Instance confirmVirtualMachine(CmdStrategy strategy, XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLVirtualMachineType virtualMachine, XMLGeographicContainerType geographic) throws InterruptedException, StrategyFailureException,
			InvalidStrategyException, ToManyResultsException, VpcUnavailableException {
		Vpc vpc = getVpc(env);
		if (vpc == null) {
			throw new VpcUnavailableException(String.format("Vpc %s was not found when it was looked up.", AwsNamingUtil.getEnvironmentName(env)));
		}
		Instance vmInstance = getInstance(env, virtualMachine, vmc, vpc);
		strategyHandler.resolveConfirmStrategy(strategy, vmInstance, "Virtual Machine", virtualMachine.getVmName(), " confirm");
		return vmInstance;
	}

	private HashMap<String, ArrayList<IptNetworkInterface>> createAllNics(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc,
			XMLVirtualMachineType virtualMachine) throws ResourceInUseException, UnSafeOperationException, InterruptedException,
			SubnetUnavailableException {
		HashMap<String, ArrayList<IptNetworkInterface>> nicMap = new HashMap<String, ArrayList<IptNetworkInterface>>();

		ArrayList<IptNetworkInterface> nics = new ArrayList<IptNetworkInterface>();
		nicMap.put(NICS, nics);

		ArrayList<IptNetworkInterface> gatwayNics = new ArrayList<IptNetworkInterface>();
		for (XMLNICType nic : virtualMachine.getNIC()) {
			IptNetworkInterface createdNic = networkManager.createNic(nic, vmc, env, virtualMachine);
			nics.add(createdNic);
			if (nic.getNetworkID() instanceof XMLOrganisationalNetworkType) {
				if (((XMLOrganisationalNetworkType) nic.getNetworkID()).getGatewayId() != null) {
					gatwayNics.add(new IptNetworkInterface(createdNic));
				}
			}
		}

		Comparator<IptNetworkInterface> nicComparator = new Comparator<IptNetworkInterface>() {
			public int compare(IptNetworkInterface c1, IptNetworkInterface c2) {
				return c2.getDeviceIndex() - c1.getDeviceIndex();
			}
		};

		Collections.sort(nics, nicComparator);

		if (gatwayNics.size() > 0) {
			nicMap.put(GATEWAY_NICS, gatwayNics);
		}

		return nicMap;
	}

	public AttachNetworkInterfaceResult attachNetworkCardToInstance(String instanceId, NetworkInterface nic, int index) {
		final AttachNetworkInterfaceRequest attachNetworkInterfaceRequest = new AttachNetworkInterfaceRequest();
		attachNetworkInterfaceRequest.setDeviceIndex(index);
		attachNetworkInterfaceRequest.setNetworkInterfaceId(nic.getNetworkInterfaceId());
		attachNetworkInterfaceRequest.setInstanceId(instanceId);
		AttachNetworkInterfaceResult result = AwsRetryManager.run(new Retryable<AttachNetworkInterfaceResult>() {
			@Override
			public AttachNetworkInterfaceResult run() {
				return cv.getEC2Client().attachNetworkInterface(attachNetworkInterfaceRequest);
			}
		});
		return result;
	}

	private List<BlockDeviceMapping> createDeviceBlockMapping(XMLVirtualMachineType virtualMachine) {
		List<BlockDeviceMapping> blockDeviceMappings = new ArrayList<BlockDeviceMapping>();
		for (XMLStorageType disk : virtualMachine.getStorage()) {
			if (disk.getSize().intValue() > 0) {
				BlockDeviceMapping mapping = createBlockDeviceMapping(disk);
				blockDeviceMappings.add(mapping);
			}
		}
		return blockDeviceMappings;
	}

	private BlockDeviceMapping createBlockDeviceMapping(XMLStorageType disk) {
		BlockDeviceMapping mapping = new BlockDeviceMapping();
		mapping.setDeviceName(disk.getDeviceMount());
		EbsBlockDevice ebs = new EbsBlockDevice();
		ebs.setVolumeSize(disk.getSize().intValue());
		ebs.setDeleteOnTermination(true);
		// ebs.setVolumeType("gp2");
		mapping.withEbs(ebs);
		return mapping;
	}

	public String createEmptyTempInstance(XMLVirtualMachineType virtualMachine, XMLVirtualMachineContainerType vmc,
			Collection<BlockDeviceMapping> blockDeviceMappings, String subnetId, Collection<IptNetworkInterface> nics, Vpc vpc)
			throws InterruptedException, UnSafeOperationException, ToManyResultsException, UnresolvedDependencyException, IOException {
		String imageId = virtualMachine.getTemplateName();
		final RunInstancesRequest rir = new RunInstancesRequest(imageId, 1, 1);
		rir.setInstanceType(virtualMachine.getHardwareProfile());
		rir.setBlockDeviceMappings(blockDeviceMappings);
		rir.setKeyName(keyName);
		rir.setNetworkInterfaces(createNicSpecs(nics));

		IamInstanceProfileSpecification roleSpec = getIamInstanceProfileSpecification(nics, vmc);
		if (roleSpec != null) {
			rir.setIamInstanceProfile(roleSpec);
		}

		UserDataHandler userDataHandler = new UserDataHandler(guestCustScriptDir, toolingDomain, gatewayManager.getGatewayElasticIpAddress(vpc));
		String userData = userDataHandler.getUserDataMultiPartMime(true, vmc, virtualMachine, nics);
		rir.setUserData(userData);

		RunInstancesResult result = AwsRetryManager.run(new Retryable<RunInstancesResult>() {
			@Override
			public RunInstancesResult run() {
				return cv.getEC2Client().runInstances(rir);
			}
		});

		Instance tempCreated = result.getReservation().getInstances().get(0);
		waitForInstanceStatus(tempCreated.getInstanceId(), InstanceStateName.Running, false);
		ArrayList<Tag> tags = new ArrayList<Tag>();
		tags.add(new Tag("InstanceType", virtualMachine.getHardwareProfile()));
		createTags(AwsNamingUtil.getVmName(virtualMachine, vmc), result.getReservation().getInstances().get(0).getInstanceId(), vmc, tags);

		stopVirtualMachine(tempCreated.getInstanceId(), virtualMachine.getVmName());
		waitForInstanceStatus(tempCreated.getInstanceId(), InstanceStateName.Stopped, false);

		Instance upgradedStoppedInstance = getInstance(tempCreated.getInstanceId());
		for (InstanceBlockDeviceMapping disk : upgradedStoppedInstance.getBlockDeviceMappings()) {
			volumeModule.detachVolumeFromInstance(upgradedStoppedInstance.getInstanceId(), disk.getDeviceName(), disk.getEbs().getVolumeId());
			volumeModule.waitForVolumeStatus(disk.getEbs().getVolumeId(), VolumeState.Available, false);
			volumeModule.deleteVolume(disk.getEbs().getVolumeId());
		}

		return tempCreated.getInstanceId();
	}

	public Collection<InstanceNetworkInterfaceSpecification> createNicSpecs(Collection<IptNetworkInterface> nics) {
		Collection<InstanceNetworkInterfaceSpecification> specs = new ArrayList<InstanceNetworkInterfaceSpecification>();

		for (IptNetworkInterface nic : nics) {
			InstanceNetworkInterfaceSpecification spec = new InstanceNetworkInterfaceSpecification();
			spec.setNetworkInterfaceId(nic.getNetworkInterfaceId());
			spec.setDeviceIndex(nic.getDeviceIndex());
			specs.add(spec);
		}
		return specs;
	}

	public IamInstanceProfileSpecification getIamInstanceProfileSpecification(List<NicOperationHolder> nics, XMLVirtualMachineContainerType vmc)
			throws ToManyResultsException, InterruptedException, UnSafeOperationException {
		Collection<IptNetworkInterface> iptNics = new ArrayList<IptNetworkInterface>();
		for (NicOperationHolder nic : nics) {
			IptNetworkInterface nic2 = nic.getNicForRoleCheck();
			if (nic2 != null) {
				iptNics.add(nic2);
			}
		}
		if (iptNics.size() > 0) {
			return getIamInstanceProfileSpecification(iptNics, vmc);
		} else {
			return null;
		}
	}

	public IamInstanceProfileSpecification getIamInstanceProfileSpecification(Collection<IptNetworkInterface> nics, XMLVirtualMachineContainerType vmc)
			throws ToManyResultsException, InterruptedException, UnSafeOperationException {

		IamInstanceProfileSpecification roleSpec = new IamInstanceProfileSpecification();

		for (IptNetworkInterface nic : nics) {
			if (nic.getVips() != null && nic.getVips().size() > 0) {
				for (String vip : nic.getVips()) {
					String roleName = vip;
					String instanceProfileName = roleManager.createInstanceProfileNameFromRole(roleName);
					InstanceProfile existingProfile = roleManager.getInstanceProfile(roleName);
					if (existingProfile == null) {
						logger.debug("existingProfile == null");

						InstanceProfile profile = roleManager.getInstanceProfile(instanceProfileName);
						Role role = roleManager.getRoleFromList(roleName);

						if (role == null) {
							// Create role is null else assign value
							roleManager.createRolePolicyForHa(roleName);
							role = roleManager.getRole(roleName);
						}

						if (profile != null) {
							if (!profile.getRoles().contains(roleManager.getRole(roleName))) {
								roleManager.assignRoleToInstanceProfile(instanceProfileName, roleName);
							}
						} else {
							roleManager.waitForRoleCreated(roleName);
							Role check = roleManager.getRoleFromList(roleName);
							roleManager.createInstanceProfile(instanceProfileName);
							roleManager.waitForInstanceProfileCreated(instanceProfileName);
							roleManager.assignRoleToInstanceProfile(instanceProfileName, check.getRoleName());
							roleManager.waitForRoleAssignToInstanceProfile(roleName, instanceProfileName);
						}
					} else {
						logger.debug("existingProfile != null");
						
						boolean exists = false;
						for (Role r : existingProfile.getRoles()) {
							if (r.getRoleName().equals(roleName)) {
								exists = true;
							}
						}

						if (!exists) {
							logger.debug("exists != null");
							
							Role role = roleManager.getRoleFromList(roleName);

							if (role == null) {
								logger.debug("role != null");
								// Create role is null else assign value
								roleManager.createRolePolicyForHa(roleName);
								role = roleManager.getRole(roleName);
							}
							Role check = roleManager.getRoleFromList(roleName);
							roleManager.assignRoleToInstanceProfile(instanceProfileName, check.getRoleName());
							roleManager.waitForRoleAssignToInstanceProfile(roleName, instanceProfileName);
						}
					}
					roleManager.waitForRoleAssignToInstanceProfile(roleName, instanceProfileName);
					roleSpec.setName(instanceProfileName);
				}
				if (StringUtils.isBlank(roleSpec.getName())) {
					throw new RuntimeException("We have an invalid InstanceProfileName");
				}
			}
		}
		return roleSpec;
	}

	public RunInstancesResult createInstance(Collection<String> securityGroupIds, Collection<IptNetworkInterface> nics,
			final XMLVirtualMachineType virtualMachine, XMLVirtualMachineContainerType vmc, Collection<BlockDeviceMapping> blockDeviceMappings,
			Vpc vpc) throws InterruptedException, UnresolvedDependencyException, IOException, ToManyResultsException, UnSafeOperationException,
			FatalException {

		String imageId = virtualMachine.getTemplateName();
		int minInstanceCount = 1; // create 1 instance
		int maxInstanceCount = 1;
		final RunInstancesRequest rir = new RunInstancesRequest(imageId, minInstanceCount, maxInstanceCount);
		if (vmc.getDataCenterId() != null) {
			Placement zone = new Placement();
			zone.setAvailabilityZone(vmc.getDataCenterId().getName());
			rir.setPlacement(zone);
		}

		IamInstanceProfileSpecification roleSpec = getIamInstanceProfileSpecification(nics, vmc);
		if (roleSpec != null && (StringUtils.isNotBlank(roleSpec.getArn()) || StringUtils.isNotBlank(roleSpec.getName()))) {
			logger.debug("using iam instance profile " + roleSpec.getName());
			rir.setIamInstanceProfile(roleSpec);
		}
		rir.setNetworkInterfaces(createNicSpecs(nics));
		rir.setInstanceType(virtualMachine.getHardwareProfile());

		if (blockDeviceMappings == null) {
			blockDeviceMappings = createDeviceBlockMapping(virtualMachine);
		}

		UserDataHandler userDataHandler = new UserDataHandler(guestCustScriptDir, toolingDomain, gatewayManager.getGatewayElasticIpAddress(vpc));
		String userData = userDataHandler.getUserDataMultiPartMime(true, vmc, virtualMachine, nics);
		rir.setUserData(userData);
		if(blockDeviceMappings.size() > 0) {
			rir.setBlockDeviceMappings(blockDeviceMappings);
		}
		rir.setKeyName(keyName);

		RunInstancesResult result = AwsRetryManager.run2(new Retryable<RunInstancesResult>() {
			@Override
			public RunInstancesResult run() {
				LogUtils.log(LogAction.CREATING, "VM", virtualMachine, "computerName", "hardwareProfile");
				RunInstancesResult result = null;
				result = cv.getEC2Client().runInstances(rir);
				return result;
			}
		});
		LogUtils.log(LogAction.CREATED, "VM", result.getReservation().getInstances().get(0), "instanceId");

		createTags(AwsNamingUtil.getVmName(virtualMachine, vmc), result.getReservation().getInstances().get(0).getInstanceId(), vmc);
		return result;

	}

	private List<DiskOperationHolder> compareBlockDeviceMappings(List<InstanceBlockDeviceMapping> instanceList, List<BlockDeviceMapping> configList,
			String rootDeviceMapping, String rootDeviceName) throws UnSafeOperationException {
		ArrayList<DiskOperationHolder> operations = new ArrayList<DiskOperationHolder>();
		HashMap<String, InstanceBlockDeviceMapping> volumeBlockMap = new HashMap<String, InstanceBlockDeviceMapping>();
		Collection<String> volumeIds = new ArrayList<String>();
		for (InstanceBlockDeviceMapping volumeId : instanceList) {
			volumeIds.add(volumeId.getEbs().getVolumeId());
			volumeBlockMap.put(volumeId.getEbs().getVolumeId(), volumeId);
		}
		List<Volume> result = volumeModule.getVolumes(volumeIds);

		HashMap<String, Volume> volumeMap = new HashMap<String, Volume>();
		for (Volume volumeResult : result) {
			volumeMap.put(volumeResult.getVolumeId(), volumeResult);
		}

		String availabilityZone = null;
		for (BlockDeviceMapping configMapping : configList) {
			boolean foundConfigInstance = false;

			for (InstanceBlockDeviceMapping instanceMapping : instanceList) {
				if (instanceMapping.getDeviceName().equals(configMapping.getDeviceName())) {
					foundConfigInstance = true;
					// matching existing device
					Volume thisVolume = volumeMap.get(instanceMapping.getEbs().getVolumeId());
					availabilityZone = thisVolume.getAvailabilityZone();
					Integer intanceVolumeSize = thisVolume.getSize();
					if (!intanceVolumeSize.equals(configMapping.getEbs().getVolumeSize())) {
						if (intanceVolumeSize > configMapping.getEbs().getVolumeSize()) {
							// trying to change to a smaller size
							String errorMessage = String.format(
									"Current instance volume %s with size %s, new size is %s which is smaller and not supported",
									instanceMapping.getDeviceName(), configMapping.getEbs().getVolumeSize(), configMapping.getEbs().getVolumeSize());
							throw new UnSafeOperationException(errorMessage);
						}
						DiskOperationHolder operationInstance = new DiskOperationHolder(DiskOperationType.RESIZE, instanceMapping, configMapping,
								thisVolume, rootDeviceName, availabilityZone);
						operations.add(operationInstance);
						volumeMap.remove(thisVolume.getVolumeId());
						// Need to change size
					}
				}
			}

			if (!foundConfigInstance) {
				// Instance has volume that isnt in config
				DiskOperationHolder operationInstance = new DiskOperationHolder(DiskOperationType.CREATE, null, configMapping, null, rootDeviceName,
						availabilityZone);
				operations.add(operationInstance);
			}
		}

		if (configList.size() < instanceList.size()) {
			// We have deletes
			for (InstanceBlockDeviceMapping instanceMapping : instanceList) {
				boolean foundInstanceMapping = false;
				for (BlockDeviceMapping configMapping : configList) {
					if (instanceMapping.getDeviceName().equals(configMapping.getDeviceName())) {
						foundInstanceMapping = true;
					}
				}

				if (!foundInstanceMapping) {
					if (instanceMapping.getDeviceName().equals(rootDeviceMapping)) {
						throw new UnSafeOperationException("You cannot delete the root volume");
					}
					DiskOperationHolder operationInstance = new DiskOperationHolder(DiskOperationType.DELETE, instanceMapping, null,
							volumeMap.get(instanceMapping.getEbs().getVolumeId()), rootDeviceName, availabilityZone);
					operations.add(operationInstance);
					volumeMap.remove(instanceMapping.getEbs().getVolumeId());
				}
			}
		}
		Iterator<String> iter = volumeMap.keySet().iterator();
		while (iter.hasNext()) {
			String volumeId = iter.next();
			DiskOperationHolder operationInstance = new DiskOperationHolder(DiskOperationType.UNCHANGED, volumeBlockMap.get(volumeId), null,
					volumeMap.get(volumeId), rootDeviceName, availabilityZone);
			operations.add(operationInstance);
		}

		return operations;
	}

	public Instance updateVirtualMachine(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc, XMLVirtualMachineType virtualMachine,
			XMLGeographicContainerType geographic, Instance vm) throws UnSafeOperationException, InterruptedException, ResourceInUseException,
			ToManyResultsException, SubnetUnavailableException, UnresolvedDependencyException, IOException {
		Instance returnInstance = null;

		// Hard drives
		List<BlockDeviceMapping> blockDeviceMappings = createDeviceBlockMapping(virtualMachine);
		List<DiskOperationHolder> diskOperations = compareBlockDeviceMappings(vm.getBlockDeviceMappings(), blockDeviceMappings,
				vm.getRootDeviceName(), vm.getRootDeviceName());

		// Hardware type
		List<HardwareProfileOperationHolder> hardwareProfileOperations = compareHardwareTypes(vm, virtualMachine);

		// networking
		List<NicOperationHolder> nicOperations = compareNicMappings(vm, virtualMachine.getNIC(), vmc, virtualMachine);

		boolean stopRequired = false;
		boolean terminateRequired = false;
		boolean hardwareUpgrade = false;
		// If change needed, stop instance
		for (HardwareProfileOperationHolder hardwareOperation : hardwareProfileOperations) {
			if (!hardwareOperation.getOperation().equals(HardwareOperationType.UNCHANGED)) {
				hardwareUpgrade = true;
				terminateRequired = true;
				logger.debug("Hardware upgrade operation is required.");
			}
		}

		boolean diskUpgrade = false;
		// If change needed, stop instance
		for (DiskOperationHolder diskOperation : diskOperations) {
			if (!diskOperation.getOperation().equals(DiskOperationType.UNCHANGED)) {
				diskUpgrade = true;
				if (diskOperation.isVmStoppedOperation()) {
					stopRequired = true;
				}
				logger.debug("Disk upgrade operation requires stop.");
			}
		}

		boolean networkUpgrade = false;
		String primarySubnetId = "";
		// If change needed, stop instance
		for (NicOperationHolder nicOperation : nicOperations) {
			if (!nicOperation.getOperation().equals(NicOperationtype.UNCHANGED)) {
				networkUpgrade = true;
				if (nicOperation.isVmStoppedOperation()) {
					stopRequired = true;
					logger.debug("NIC upgrade operation requires stop.");
				}
				if (nicOperation.isVmTerminateOperation()) {
					terminateRequired = true;
					hardwareUpgrade = true;
					logger.debug("NIC upgrade operation requires terminate.");
				}
			}
			if (nicOperation.getConfigNic().getIndexNumber().intValue() == 0) {
				primarySubnetId = nicOperation.getInstanceNic().getSubnetId();
			}
		}
		boolean deleteVmRoleSpec = false;
		// Detect if current iam profile for the box and rebuild if its different.
		if(vm.getIamInstanceProfile() != null) {
			String iamInstanceProfileName = AwsNamingUtil.getIamInstanceProfileNameFromArn(vm.getIamInstanceProfile().getArn());
			IamInstanceProfileSpecification roleSpec = getIamInstanceProfileSpecification(nicOperations, vmc);
			if ((roleSpec == null && vm.getIamInstanceProfile() != null)
					|| (roleSpec != null && !roleSpec.getName().equals(iamInstanceProfileName))) {
				// No vip for this vm and vip configured rebuild
				terminateRequired = true;
				hardwareUpgrade = true;
				deleteVmRoleSpec = true;
				logger.debug(String.format("Current instance profile name is %s, new instance profile name is %s", iamInstanceProfileName, roleSpec == null ? "null" :roleSpec.getName()));
				logger.debug(String.format("VM role upgrade operation requires role to be deleted: %s",vm.getIamInstanceProfile().getArn()));
				logger.debug("VM role upgrade operation requires terminate.");
			}
		}

		if (stopRequired) {
			stopVirtualMachine(vm.getInstanceId(), virtualMachine.getVmName());
			waitForInstanceStatus(vm.getInstanceId(), InstanceStateName.Stopped, true);
		}

		if (hardwareUpgrade || terminateRequired) {
			setAllDeleteOnTerminiateReferences(vm, false);
			IamInstanceProfile role = vm.getIamInstanceProfile();
			terminateVirtualMachine(vm);
			waitForInstanceStatus(vm.getInstanceId(), InstanceStateName.Terminated, true);

			if (deleteVmRoleSpec) {
				roleManager.deleteInstanceProfileByArn(role.getArn());
			}

			Instance currentInstanceState = getInstance(vm.getInstanceId());
			InstanceStateName state = InstanceStateName.fromValue(currentInstanceState.getState().getName());

			networkUpgrade(nicOperations, state, env, vmc, currentInstanceState, virtualMachine);

			BlockDeviceMapping root = null;
			for (BlockDeviceMapping mapping : blockDeviceMappings) {
				mapping.getDeviceName().equals(vm.getRootDeviceName());
			}
			Collection<BlockDeviceMapping> rootOnly = new ArrayList<BlockDeviceMapping>();
			rootOnly.add(root);
			String tempInstanceId = createEmptyTempInstance(virtualMachine, vmc, rootOnly, primarySubnetId, getUpgradeNics(nicOperations),
					getVpc(env));

			diskUpgrade(diskOperations, state, currentInstanceState, virtualMachine, vmc);

			for (DiskOperationHolder diskOperation : diskOperations) {
				volumeModule.attachVolumeToInstance(tempInstanceId, diskOperation.getDeviceMount(), diskOperation.getReattachVolumeId(),
						virtualMachine.getVmName());
			}

			returnInstance = getInstance(tempInstanceId);

		} else {

			Instance currentInstanceState = getInstance(vm.getInstanceId());
			InstanceStateName state = InstanceStateName.fromValue(currentInstanceState.getState().getName());

			if (diskUpgrade) {
				diskUpgrade(diskOperations, state, currentInstanceState, virtualMachine, vmc);
			}

			if (networkUpgrade) {
				networkUpgrade(nicOperations, state, env, vmc, currentInstanceState, virtualMachine);
			}

			returnInstance = getInstance(vm.getInstanceId());
		}

		startVirtualMachine(returnInstance.getInstanceId(), virtualMachine);

		return returnInstance;
	}

	private ArrayList<IptNetworkInterface> getUpgradeNics(List<NicOperationHolder> nicOperations) {
		ArrayList<IptNetworkInterface> nics = new ArrayList<IptNetworkInterface>();
		for (NicOperationHolder nicOperation : nicOperations) {
			nics.add(nicOperation.getNicForVM());
		}

		Comparator<IptNetworkInterface> nicComparator = new Comparator<IptNetworkInterface>() {
			public int compare(IptNetworkInterface c1, IptNetworkInterface c2) {
				return c2.getDeviceIndex() - c1.getDeviceIndex();
			}
		};

		Collections.sort(nics, nicComparator);

		return nics;
	}

	public void setAllDeleteOnTerminiateReferences(Instance vm, boolean deleteOnTerminiate) {
		for (InstanceNetworkInterface nic : vm.getNetworkInterfaces()) {
			networkManager.modifyNicDeleteOnTerminate(nic, deleteOnTerminiate);
		}

		volumeModule.modifyDisksDeleteOnTerminate(vm.getBlockDeviceMappings(), vm.getInstanceId(), deleteOnTerminiate);
	}

	private void diskUpgrade(List<DiskOperationHolder> diskOperations, InstanceStateName instanceState, Instance vm,
			XMLVirtualMachineType virtualMachine, XMLVirtualMachineContainerType vapp) throws InterruptedException, UnSafeOperationException {
		for (DiskOperationHolder diskOperation : diskOperations) {
			logger.debug(String.format("%s %s %s %s", StringTools.lastDotValue(diskOperation.getOperation().getClass().getName()),
					diskOperation.getOperation(), StringTools.lastDotValue(instanceState.getClass().getName()), instanceState));
			switch (diskOperation.getOperation()) {
			case CREATE:
				logger.debug(String.format("%s %s %s %s", diskOperation.getOperation(), diskOperation.getOperation().getClass().getName(),
						instanceState.getClass().getName(), instanceState));
				Volume newVolume = volumeModule.createSnapshotVolume(diskOperation.getConfig().getEbs().getVolumeSize(), null,
						diskOperation.getAvailabilityZone(), virtualMachine.getVmName(), vapp);
				diskOperation.setNewVolume(newVolume);
				switch (instanceState) {
				case Stopping:
				case ShuttingDown:
				case Pending:
					throw new InvalidInstanceException("The instance was not ready to be altered, state was " + instanceState);
				case Running:
				case Stopped:
					volumeModule.waitForVolumeStatus(newVolume.getVolumeId(), VolumeState.Available, true);
					volumeModule.attachVolumeToInstance(vm.getInstanceId(), diskOperation.getDeviceMount(), newVolume.getVolumeId(),
							virtualMachine.getVmName());
					break;
				case Terminated:
					break;
				default:
					break;
				}
				break;
			case DELETE:
				switch (instanceState) {
				case Stopping:
				case ShuttingDown:
				case Pending:
					throw new InvalidInstanceException("The instance was not ready to be altered, state was " + instanceState);
				case Running:
				case Stopped:
					volumeModule.detachVolumeFromInstance(vm.getInstanceId(), diskOperation.getInstance().getDeviceName(), diskOperation.getVolume()
							.getVolumeId());
					volumeModule.waitForVolumeStatus(diskOperation.getVolume().getVolumeId(), VolumeState.Available, false);
					volumeModule.deleteVolume(diskOperation.getVolume().getVolumeId());
					break;
				case Terminated:
					volumeModule.deleteVolume(diskOperation.getVolume().getVolumeId());
					break;
				default:
					break;
				}

				break;
			case RESIZE:
				switch (instanceState) {
				case Stopping:
				case ShuttingDown:
				case Pending:
					throw new InvalidInstanceException("The instance was not ready to be altered, state was " + instanceState);
				case Stopped:
					volumeModule.detachVolumeFromInstance(vm.getInstanceId(), diskOperation.getInstance().getDeviceName(), diskOperation.getVolume()
							.getVolumeId());
					volumeModule.waitForVolumeStatus(diskOperation.getVolume().getVolumeId(), VolumeState.Available, false);
					Volume expandedVolume = volumeModule.resizeVolume(diskOperation.getVolume().getVolumeId(), virtualMachine.getVmName(),
							diskOperation.getInstance().getDeviceName(), diskOperation.getConfig().getEbs().getVolumeSize(), diskOperation
									.getVolume().getAvailabilityZone(), diskOperation, vapp);
					diskOperation.setNewVolume(expandedVolume);
					volumeModule.attachVolumeToInstance(vm.getInstanceId(), diskOperation.getInstance().getDeviceName(),
							expandedVolume.getVolumeId(), virtualMachine.getVmName());
					volumeModule.waitForVolumeStatus(expandedVolume.getVolumeId(), VolumeState.InUse, true);
					volumeModule.deleteSnapshot(diskOperation.getSnapshot().getSnapshotId());
					volumeModule.deleteVolume(diskOperation.getVolume().getVolumeId());
					break;
				case Terminated:
					volumeModule.waitForVolumeStatus(diskOperation.getVolume().getVolumeId(), VolumeState.Available, false);
					diskOperation.setNewVolume(volumeModule.resizeVolume(diskOperation.getVolume().getVolumeId(), virtualMachine.getVmName(),
							diskOperation.getInstance().getDeviceName(), diskOperation.getConfig().getEbs().getVolumeSize(), diskOperation
									.getVolume().getAvailabilityZone(), diskOperation, vapp));
					volumeModule.deleteSnapshot(diskOperation.getSnapshot().getSnapshotId());
					volumeModule.deleteVolume(diskOperation.getVolume().getVolumeId());
					break;
				default:
					// incompatible state, handle
					break;
				}
				break;
			case UNCHANGED:
				break;
			default:
				break;
			}
		}
	}

	private void networkUpgrade(List<NicOperationHolder> nicOperations, InstanceStateName instanceState, XMLEnvironmentType env,
			XMLVirtualMachineContainerType vmc, Instance vm, XMLVirtualMachineType virtualMachine) throws InterruptedException,
			ResourceInUseException, UnSafeOperationException, SubnetUnavailableException {
		for (NicOperationHolder nicOperation : nicOperations) {
			logger.debug(String.format("%s %s %s %s", StringTools.lastDotValue(nicOperation.getOperation().getClass().getName()),
					nicOperation.getOperation(), StringTools.lastDotValue(instanceState.getClass().getName()), instanceState));
			switch (nicOperation.getOperation()) {
			case DELETE:
				switch (instanceState) {
				case Pending:
				case ShuttingDown:
				case Stopping:
					throw new InvalidInstanceException("The instance was not ready to be altered, state was " + instanceState);
				case Running:
				case Stopped:
					networkManager.detachNetworkInterface(nicOperation.getInstanceNic().getAttachment().getAttachmentId());
					waitForNicStatus(nicOperation.getInstanceNic().getNetworkInterfaceId(), NetworkInterfaceStatus.Available, false);
					networkManager.deleteNetworkInterface(nicOperation.getInstanceNic().getNetworkInterfaceId());
					break;
				case Terminated:
					networkManager.deleteNetworkInterface(nicOperation.getInstanceNic().getNetworkInterfaceId());
					break;
				default:
					break;
				}
				break;
			case REPLACE:
				switch (instanceState) {
				case Pending:
				case ShuttingDown:
				case Stopping:
					throw new InvalidInstanceException("The instance was not ready to be altered, state was " + instanceState);
				case Running:
				case Stopped:
					networkManager.detachNetworkInterface(nicOperation.getInstanceNic().getAttachment().getAttachmentId());
					waitForNicStatus(nicOperation.getInstanceNic().getNetworkInterfaceId(), NetworkInterfaceStatus.Available, false);
					networkManager.deleteNetworkInterface(nicOperation.getInstanceNic().getNetworkInterfaceId());
					IptNetworkInterface createdNic = networkManager.createNic(nicOperation.getConfigNic(), vmc, env, virtualMachine);
					nicOperation.setCreatedNic(createdNic);
					networkManager.attachNicToInstance(createdNic, vm);
					break;
				case Terminated:
					waitForNicStatus(nicOperation.getInstanceNic().getNetworkInterfaceId(), NetworkInterfaceStatus.Available, false);
					networkManager.deleteNetworkInterface(nicOperation.getInstanceNic().getNetworkInterfaceId());
					nicOperation.setCreatedNic(networkManager.createNic(nicOperation.getConfigNic(), vmc, env, virtualMachine));
					break;
				default:
					break;
				}
				break;
			case CREATE:
				IptNetworkInterface createdNic = networkManager.createNic(nicOperation.getConfigNic(), vmc, env, virtualMachine);
				nicOperation.setCreatedNic(createdNic);
				switch (instanceState) {
				case Pending:
				case ShuttingDown:
				case Stopping:
					throw new InvalidInstanceException("The instance was not ready to be altered, state was " + instanceState);
				case Terminated:
					break;
				case Running:
				case Stopped:
					waitForNicStatus(createdNic.getNetworkInterfaceId(), NetworkInterfaceStatus.Available, false);
					networkManager.attachNicToInstance(createdNic, vm);
					break;
				default:
					break;
				}
				break;
			case MODIFY:
				InstanceNetworkInterface instanceNic = nicOperation.getInstanceNic();

				String primaryIp = instanceNic.getPrivateIpAddress();

				Collection<String> privateIpAddressesDelete = new ArrayList<String>();
				Collection<String> privateIpAddressesAdd = new ArrayList<String>();

				Set<String> configIps = new HashSet<String>();
				Set<String> instanceIps = new HashSet<String>();

				for (XMLInterfaceType configInterface : nicOperation.getConfigNic().getInterface()) {
					configIps.add(configInterface.getStaticIpAddress());
				}

				for (InstancePrivateIpAddress iIp : instanceNic.getPrivateIpAddresses()) {
					instanceIps.add(iIp.getPrivateIpAddress());
				}

				for (InstancePrivateIpAddress iIp : instanceNic.getPrivateIpAddresses()) {
					if (!configIps.contains(iIp.getPrivateIpAddress())) {
						privateIpAddressesDelete.add(iIp.getPrivateIpAddress());
					} 
				}

				for (XMLInterfaceType cIp : nicOperation.getConfigNic().getInterface()) {
					if (!instanceIps.contains(cIp.getStaticIpAddress())) {
						if (cIp.isIsVip()) {
							NetworkInterface nic = getNetworkInterfaceByPrivateIpAddress(cIp.getStaticIpAddress(), getVpc(env));
							if (nic == null) {
								logger.debug(String.format("Adding vip ip %s for vm %s", cIp.getStaticIpAddress(), virtualMachine.getComputerName()));
								privateIpAddressesAdd.add(cIp.getStaticIpAddress());
							} else {
								logger.debug("Not adding vip ip "+cIp.getStaticIpAddress()+" its allready allocated to interfaceId "+nic.getNetworkInterfaceId());
							}
						} else {
							privateIpAddressesAdd.add(cIp.getStaticIpAddress());
						}
					}
				}

				String name = AwsNamingUtil.getNicName(nicOperation.getConfigNic(), vmc);
				createTags(name, nicOperation.getInstanceNic().getNetworkInterfaceId(), vmc);

				if (privateIpAddressesDelete.size() > 0) {
					networkManager.deleteSecondaryIp(instanceNic.getNetworkInterfaceId(), privateIpAddressesDelete);
				}
				if (privateIpAddressesAdd.size() > 0) {
					networkManager.addSeccondaryIp(instanceNic.getNetworkInterfaceId(), privateIpAddressesAdd);
				}
			case UNCHANGED:
				switch (instanceState) {
				default:
					break;
				}
				break;
			default:
				break;
			}
		}
	}

	public void stopVirtualMachine(String instanceId, String vmName) {
		LogUtils.log(LogAction.STOPPING, "VM", "instanceId=" + instanceId + ", vmName=" + vmName);
		final StopInstancesRequest shutdownRequest = new StopInstancesRequest().withInstanceIds(instanceId);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().stopInstances(shutdownRequest);
				return null;
			}
		});

		LogUtils.log(LogAction.STOPPED, "VM", "instanceId=" + instanceId + ", vmName=" + vmName);
	}

	public void stopVirtualMachine(Collection<String> instanceIds) {
		LogUtils.log(LogAction.STOPPING, "VMs", "instanceIds=" + instanceIds);
		final StopInstancesRequest shutdownRequest = new StopInstancesRequest().withInstanceIds(instanceIds);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().stopInstances(shutdownRequest);
				return null;
			}
		});

		LogUtils.log(LogAction.STOPPED, "VMs", "instanceId=" + instanceIds);
	}

	public void startVirtualMachine(String instanceId, XMLVirtualMachineType virtualMachine) {
		final StartInstancesRequest startRequest = new StartInstancesRequest().withInstanceIds(instanceId);
		LogUtils.log(LogAction.STARTING, "VM", "instanceId=" + instanceId + ", vmName=" + virtualMachine+ ", instanceType=" + virtualMachine.getHardwareProfile());
		StartInstancesResult result = AwsRetryManager.run(new Retryable<StartInstancesResult>() {
			@Override
			public StartInstancesResult run() {
				return cv.getEC2Client().startInstances(startRequest);
			}
		});
		result.getStartingInstances();
		LogUtils.log(LogAction.STARTED, "VM", "instanceId=" + instanceId + ", vmName=" + virtualMachine);
	}

	public void startVirtualMachine(Collection<String> instanceIds) {
		final StartInstancesRequest startRequest = new StartInstancesRequest().withInstanceIds(instanceIds);
		LogUtils.log(LogAction.STARTING, "VMs", "instanceId=" + instanceIds);
		StartInstancesResult result = AwsRetryManager.run(new Retryable<StartInstancesResult>() {
			@Override
			public StartInstancesResult run() {
				return cv.getEC2Client().startInstances(startRequest);
			}
		});
		result.getStartingInstances();
		LogUtils.log(LogAction.STARTED, "VMs", "instanceId=" + instanceIds);
	}

	public void rebootVm(String instanceId, String message) {
		final RebootInstancesRequest rebootRequest = new RebootInstancesRequest().withInstanceIds(instanceId);
		LogUtils.log(LogAction.REBOOTING, "VM", "instanceId=" + instanceId + ", message=" + message);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().rebootInstances(rebootRequest);
				return null;
			}
		});

		LogUtils.log(LogAction.REBOOTED, "VM", "instanceId=" + instanceId + ", message=" + message);
	}

	private List<NicOperationHolder> compareNicMappings(Instance vm, List<XMLNICType> nics, XMLVirtualMachineContainerType vmc,
			XMLVirtualMachineType virtualMachine) {

		List<NicOperationHolder> nicOperations = new ArrayList<NicOperationHolder>();

		HashMap<Integer, InstanceNetworkInterface> instanceNicIndexMap = new HashMap<Integer, InstanceNetworkInterface>();
		HashMap<String, Integer> instanceNicIpMap = new HashMap<String, Integer>();
		for (InstanceNetworkInterface instanceNic : vm.getNetworkInterfaces()) {
			instanceNicIndexMap.put(instanceNic.getAttachment().getDeviceIndex(), instanceNic);
			instanceNicIpMap.put(instanceNic.getPrivateIpAddress(), instanceNic.getAttachment().getDeviceIndex());
		}

		HashMap<Integer, XMLNICType> configNicIndexMap = new HashMap<Integer, XMLNICType>();
		for (XMLNICType configNic : nics) {
			configNicIndexMap.put(configNic.getIndexNumber().intValue(), configNic);
		}
		if (instanceNicIndexMap.size() != configNicIndexMap.size()) {
			Iterator<Integer> iter = instanceNicIndexMap.keySet().iterator();
			while (iter.hasNext()) {
				Integer indexKey = iter.next();
				XMLNICType configNic = configNicIndexMap.get(indexKey);
				if (configNic == null) {
					NicOperationHolder deleteNic = new NicOperationHolder();
					deleteNic.setVm(virtualMachine);
					deleteNic.setOperation(NicOperationtype.DELETE);
					deleteNic.setInstanceNic(instanceNicIndexMap.get(indexKey));
					deleteNic.setVmc(vmc);
					nicOperations.add(deleteNic);
				}
			}
		}

		for (XMLNICType configNic : nics) {
			InstanceNetworkInterface instanceNic = instanceNicIndexMap.get(configNic.getIndexNumber().intValue());
			boolean nicOperationFound = false;
			if (instanceNic == null) {
				NicOperationHolder newNic = new NicOperationHolder();
				newNic.setVm(virtualMachine);
				newNic.setOperation(NicOperationtype.CREATE);
				newNic.setConfigNic(configNic);
				newNic.setVmc(vmc);
				nicOperations.add(newNic);
				nicOperationFound = true;
			} else if (instanceNic != null) {
				String ip = instanceNic.getPrivateIpAddress();
				Integer index = instanceNic.getAttachment().getDeviceIndex();

				if (!configNic.getInterface().get(0).getStaticIpAddress().equals(ip) && index == 0) {
					NicOperationHolder replaceNic = new NicOperationHolder();
					replaceNic.setVm(virtualMachine);
					replaceNic.setOperation(NicOperationtype.REPLACE);
					replaceNic.setConfigNic(configNic);
					replaceNic.setInstanceNic(instanceNic);
					replaceNic.setVmc(vmc);
					nicOperations.add(replaceNic);
					nicOperationFound = true;
				} else {

					if (instanceNic.getPrivateIpAddresses().size() != configNic.getInterface().size()) {
						// Upgrade secondary nics
						NicOperationHolder modifyNic = new NicOperationHolder();
						modifyNic.setVm(virtualMachine);
						modifyNic.setOperation(NicOperationtype.MODIFY);
						modifyNic.setConfigNic(configNic);
						modifyNic.setInstanceNic(instanceNic);
						modifyNic.setVmc(vmc);
						nicOperations.add(modifyNic);
						nicOperationFound = true;
					} else {

						// Look for different ip addresses in the secondary nics
						Set<String> configIps = new HashSet<String>();

						for (XMLInterfaceType configInterface : configNic.getInterface()) {
							configIps.add(configInterface.getStaticIpAddress());
						}

						for (InstancePrivateIpAddress liveip : instanceNic.getPrivateIpAddresses()) {
							if (!configIps.contains(liveip.getPrivateIpAddress())) {
								NicOperationHolder modifyNic = new NicOperationHolder();
								modifyNic.setVm(virtualMachine);
								modifyNic.setOperation(NicOperationtype.MODIFY);
								modifyNic.setConfigNic(configNic);
								modifyNic.setInstanceNic(instanceNic);
								modifyNic.setVmc(vmc);
								nicOperations.add(modifyNic);
								nicOperationFound = true;
							}
						}
					}
				}
			}
			if (!nicOperationFound) {

				boolean deletedNic = false;
				for (NicOperationHolder nicHolder : nicOperations) {
					if (nicHolder.getInstanceNic().getMacAddress().equals(instanceNic.getMacAddress())) {
						deletedNic = true;
					}
				}

				if (!deletedNic) {
					NicOperationHolder unchangedNic = new NicOperationHolder();
					unchangedNic.setVm(virtualMachine);
					unchangedNic.setOperation(NicOperationtype.UNCHANGED);
					unchangedNic.setConfigNic(configNic);
					unchangedNic.setInstanceNic(instanceNic);
					unchangedNic.setVmc(vmc);
					nicOperations.add(unchangedNic);
				}
			}
		}

		return nicOperations;
	}

	private List<HardwareProfileOperationHolder> compareHardwareTypes(Instance vm, XMLVirtualMachineType virtualMachine) {

		List<HardwareProfileOperationHolder> hardwareOperation = new ArrayList<HardwareProfileOperationHolder>();
		if (vm.getInstanceType().equals(virtualMachine.getHardwareProfile())) {
			hardwareOperation.add(new HardwareProfileOperationHolder(HardwareOperationType.UNCHANGED, virtualMachine.getHardwareProfile(), null));
		} else {
			hardwareOperation.add(new HardwareProfileOperationHolder(HardwareOperationType.UPGRADE, virtualMachine.getHardwareProfile(), vm));
		}

		return hardwareOperation;
	}

	public void deleteVirtualMachine(Instance vm) {
		setAllDeleteOnTerminiateReferences(vm, true);
		terminateVirtualMachine(vm);
	}

	public void deleteVirtualMachine(XMLEnvironmentType env, XMLVirtualMachineContainerType vmc, XMLVirtualMachineType virtualMachine)
			throws VpcUnavailableException, ToManyResultsException, UnSafeOperationException {
		Vpc vpc = getVpc(env);
		if (vpc == null) {
			throw new VpcUnavailableException(String.format("Vpc %s was not found when it was looked up.", AwsNamingUtil.getEnvironmentName(env)));
		}
		Instance vmInstance = getInstance(env, virtualMachine, vmc, vpc);
		if (vmInstance != null) {
			deleteVirtualMachine(vmInstance);
			waitForInstanceStatus(vmInstance.getInstanceId(), InstanceStateName.Terminated, false);
		}
	}

	public void terminateVirtualMachine(final Instance vm) {
		final TerminateInstancesRequest request = new TerminateInstancesRequest();
		request.withInstanceIds(vm.getInstanceId());
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				IamInstanceProfile role = vm.getIamInstanceProfile();
				cv.getEC2Client().terminateInstances(request);
				if (role != null) {
					try {
						List<Instance> usage = getInstanceProfileUsage(role.getArn());
						boolean included = false;
						for(Instance vmUsage : usage) {
							if(vmUsage.getInstanceId().equals(vm.getInstanceId())) {
								included = true;
							}
						}
						
						if(included && usage.size() <= 1) {
							roleManager.deleteInstanceProfileByArn(role.getArn());
						} else if(!included && usage.size() <= 0) {
							roleManager.deleteInstanceProfileByArn(role.getArn());
						}
						
					} catch (NoSuchEntityException e) {
						logger.debug("NoSuchEntityException "+e.getMessage() + " while deleting instance profile "+role.getArn());
					}
				}
				return null;
			}
		});

		LogUtils.log(LogAction.TERMINATED, "VM", vm, "instanceId");
	}

	public String getConsoleOutput(String instanceId) {
		final GetConsoleOutputRequest request = new GetConsoleOutputRequest();
		request.setInstanceId(instanceId);

		GetConsoleOutputResult result = null;
		try {
			result = AwsRetryManager.run(new Retryable<GetConsoleOutputResult>() {
				@Override
				public GetConsoleOutputResult run() {
					return cv.getEC2Client().getConsoleOutput(request);
				}
			});

			logger.debug("Console decoded output length [" + result.getDecodedOutput().length() + "]");
		} catch (NullPointerException npe) {
			logger.debug("Console output [null]");
			return StringUtils.EMPTY;
		}
		return result.getDecodedOutput();
	}

	public void waitForInstanceConsoleOutput(final String instanceId, final String searchString, boolean extended) throws UnSafeOperationException {
		MandatoryCheck.checkNotNull(MandatoryCheck.VM_RESOURCE_ID, instanceId);
		MandatoryCheck.checkNotNull(MandatoryCheck.VM_BOOSTRAP_STRING, searchString);
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				String retVal = getConsoleOutput(instanceId);
				logger.debug("Console output \n" + retVal);
				if (StringUtils.isBlank(retVal)) {
					return false;
				} else {
					if (retVal.contains(searchString)) {
						logger.debug("Matched console search string " + searchString);
						return true;
					}

					// do this as AWS only returns 1 snapshot from the console,
					// so there is no point waiting as we will never get any
					// more text so its pointless.
					logger.debug("Did not match console search string " + searchString);
					try {
						// This is a build in latency to allow an additional
						// delay in the builds when bootstrapping
						if (org.apache.commons.lang.StringUtils.isNumeric(bootstrapAdditionalTimeout)) {
							logger.debug("Performing boostrap delay");
							Thread.sleep(Integer.parseInt(bootstrapAdditionalTimeout) * 60 * 1000);
							logger.debug("Finished boostrap delay");
						}
					} catch (InterruptedException e) {
						logger.error("Interruption when waiting on boostrap delay");
					}
					return true;

				}
			}
		};

		AwsRetryManager.waitForBootstrap(waitCondition, 60000, 1);
	}

}
