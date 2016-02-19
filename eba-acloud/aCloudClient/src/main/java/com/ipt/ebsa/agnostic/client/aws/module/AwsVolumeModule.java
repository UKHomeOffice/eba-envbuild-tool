package com.ipt.ebsa.agnostic.client.aws.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AttachVolumeResult;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.DetachVolumeResult;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDeviceSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceAttributeName;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMappingSpecification;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.ModifyVolumeAttributeRequest;
import com.amazonaws.services.ec2.model.RegisterImageRequest;
import com.amazonaws.services.ec2.model.RegisterImageResult;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.SnapshotState;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeState;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.Retryable;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.WaitCondition;
import com.ipt.ebsa.agnostic.client.controller.operation.DiskOperationHolder;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.logging.LogUtils;
import com.ipt.ebsa.agnostic.client.logging.LogUtils.LogAction;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.jcabi.aspects.Loggable;

/**
 * 
 *
 */
@Loggable(prepend = true)
public class AwsVolumeModule extends AwsModule {

	private Logger logger = LogManager.getLogger(AwsVolumeModule.class);

	public AwsVolumeModule() {
	}

	public Volume resizeVolume(String volumeId, String vmName, String deviceName, Integer newSize, String availabilityZone,
			DiskOperationHolder diskOperation, XMLVirtualMachineContainerType vapp) throws InterruptedException, UnSafeOperationException {
		// backup the volume
		Snapshot snap = createSnapshot(volumeId, vmName, deviceName, vapp);
		waitForSnapshotStatus(snap.getSnapshotId(), SnapshotState.Completed, true);
		diskOperation.setSnapshot(snap);
		// create expanded volume
		Volume expandedVolume = createSnapshotVolume(newSize, snap.getSnapshotId(), availabilityZone, vmName, vapp);
		waitForVolumeStatus(expandedVolume.getVolumeId(), VolumeState.Available, true);
		return expandedVolume;
	}

	public void deleteVolume(String volumeId) {
		final DeleteVolumeRequest deleteVolumeRequest = new DeleteVolumeRequest();
		deleteVolumeRequest.setVolumeId(volumeId);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().deleteVolume(deleteVolumeRequest);
				return null;
			}
		});

	}

	public void deleteSnapshot(String snapshotId) {
		final DeleteSnapshotRequest deleteSnapshotRequest = new DeleteSnapshotRequest();
		deleteSnapshotRequest.setSnapshotId(snapshotId);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().deleteSnapshot(deleteSnapshotRequest);
				return null;
			}
		});

	}

	public void detachVolumeFromInstance(String instanceId, String deviceName, String volumeId) {
		final DetachVolumeRequest detach = new DetachVolumeRequest();
		detach.setInstanceId(instanceId);
		detach.setDevice(deviceName);
		detach.setVolumeId(volumeId);
		DetachVolumeResult result = AwsRetryManager.run(new Retryable<DetachVolumeResult>() {
			@Override
			public DetachVolumeResult run() {
				return cv.getEC2Client().detachVolume(detach);
			}
		});
	}

	public void attachVolumeToInstance(String instanceId, String deviceName, String volumeId, String vmName) {
		LogUtils.log(LogAction.ATTACHING,
				String.format("Attaching volume %s to device path %s for vm %s with instance id %s", volumeId, deviceName, vmName, instanceId));
		final AttachVolumeRequest attach = new AttachVolumeRequest();
		attach.setInstanceId(instanceId);
		attach.setDevice(deviceName);
		attach.setVolumeId(volumeId);

		AttachVolumeResult result = AwsRetryManager.run(new Retryable<AttachVolumeResult>() {
			@Override
			public AttachVolumeResult run() {
				return cv.getEC2Client().attachVolume(attach);
			}
		});
		ArrayList<Tag> tags = new ArrayList<Tag>();
		tags.add(new Tag("Name", vmName + "Volume"));
		tags.add(new Tag("device", deviceName));
		createTags(tags, volumeId);
		LogUtils.log(LogAction.ATTACHED,
				String.format("Attached volume %s to device path %s for vm %s with instance id %s", volumeId, deviceName, vmName, instanceId));
	}

	public Volume createSnapshotVolume(Integer size, String snapshotId, String availabilityZone, String vmName, XMLVirtualMachineContainerType vapp) {
		final CreateVolumeRequest expandVolume = new CreateVolumeRequest();
		expandVolume.setSize(size);
		expandVolume.setSnapshotId(snapshotId);
		expandVolume.setAvailabilityZone(availabilityZone);

		CreateVolumeResult result = AwsRetryManager.run(new Retryable<CreateVolumeResult>() {
			@Override
			public CreateVolumeResult run() {
				return cv.getEC2Client().createVolume(expandVolume);
			}
		});
		createTags(vmName + "Volume", result.getVolume().getVolumeId(), vapp);
		return result.getVolume();
	}

	public Snapshot createSnapshot(String volumeId, String vm, String mapping, XMLVirtualMachineContainerType vapp) {
		final CreateSnapshotRequest snapshotRequest = new CreateSnapshotRequest();
		snapshotRequest.setDescription(String.format("Creating snapshot of mapping %s on vm %s", mapping, vm));
		snapshotRequest.setVolumeId(volumeId);

		CreateSnapshotResult result = AwsRetryManager.run(new Retryable<CreateSnapshotResult>() {
			@Override
			public CreateSnapshotResult run() {
				return cv.getEC2Client().createSnapshot(snapshotRequest);
			}
		});
		ArrayList<Tag> tags = new ArrayList<Tag>();
		tags.add(new Tag("device", mapping));
		createTags(vm + "Snapshot", volumeId, vapp, tags);
		return result.getSnapshot();
	}

	public void waitForVolumeStatus(final String volumeId, final VolumeState desiredState, boolean extended) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {

				VolumeState actualState = getVolumeStatus(volumeId);
				logger.debug(String.format("Desired Volume State %s, Actual Volume State %s", desiredState, actualState));
				return actualState == desiredState;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Volume: " + volumeId + " did not reach state: " + desiredState, 5000, extended);
	}

	public void waitForSnapshotStatus(final String snapshotId, final SnapshotState desiredState, boolean extended) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getSnapshotStatus(snapshotId) == desiredState;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Snapshot: " + snapshotId + " did not reach state: " + desiredState, 5000, extended);
	}

	public void modifyVolumeAttributeAutoEnableIO(String volumeId, boolean autoEnableIO) {
		final ModifyVolumeAttributeRequest request = new ModifyVolumeAttributeRequest();
		request.setVolumeId(volumeId);
		request.setAutoEnableIO(autoEnableIO);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getEC2Client().modifyVolumeAttribute(request);
				return null;
			}
		});

	}

	public void modifyDisksDeleteOnTerminate(List<InstanceBlockDeviceMapping> disks, String instanceId, boolean deleteOnTerminiate) {
		// aws ec2 modify-instance-attribute --instance-id i-123abc45
		// --block-device-mappings
		// "[{\"DeviceName\": \"/dev/sdf\",\"Ebs\":{\"DeleteOnTermination\":true}}]"

		final ModifyInstanceAttributeRequest request = new ModifyInstanceAttributeRequest();
		request.setInstanceId(instanceId);
		request.setAttribute(InstanceAttributeName.BlockDeviceMapping);

		for (InstanceBlockDeviceMapping disk : disks) {
			InstanceBlockDeviceMappingSpecification spec = new InstanceBlockDeviceMappingSpecification();
			spec.setDeviceName(disk.getDeviceName());
			EbsInstanceBlockDeviceSpecification ebs = new EbsInstanceBlockDeviceSpecification();
			ebs.setDeleteOnTermination(deleteOnTerminiate);
			ebs.setVolumeId(disk.getEbs().getVolumeId());
			spec.setEbs(ebs);
			request.getBlockDeviceMappings().add(spec);
		}

		if (disks.size() > 0) {
			AwsRetryManager.run(new Retryable<Void>() {
				@Override
				public Void run() {
					cv.getEC2Client().modifyInstanceAttribute(request);
					return null;
				}
			});

		}

	}

	private void createRebuildAmi(Instance vm, XMLVirtualMachineType virtualMachine, Collection<BlockDeviceMapping> blockDeviceMappings) {
		String imageName = "";
		imageName = "Rebuild" + virtualMachine.getVmName();
		String description = imageName;
		String imageId = createAMI(vm.getArchitecture(), vm.getKernelId(), vm.getRamdiskId(), blockDeviceMappings, imageName, vm.getRootDeviceName(),
				description);
	}

	public String createAMI(String architecture, String kernelId, String ramdiskId, Collection<BlockDeviceMapping> blockDeviceMappings,
			String imageName, String rootDeviceName, String description) {
		final RegisterImageRequest request = new RegisterImageRequest();
		request.setArchitecture(architecture);
		request.setKernelId(kernelId);
		request.setRamdiskId(ramdiskId);
		request.setBlockDeviceMappings(blockDeviceMappings);
		request.setName(imageName);
		request.setRootDeviceName(rootDeviceName);
		request.setDescription(description);

		RegisterImageResult result = AwsRetryManager.run(new Retryable<RegisterImageResult>() {
			@Override
			public RegisterImageResult run() {
				return cv.getEC2Client().registerImage(request);
			}
		});
		return result.getImageId();
		// ec2-register -s snap-12345 -a i386 -d "Description of AMI" -n
		// "name-of-image" -k aki-12345 -r ari-12345
	}

	public Volume getVolume(String volumeId) {
		Collection<String> volumeIds = new ArrayList<String>();
		volumeIds.add(volumeId);
		List<Volume> retVolumes = getVolumes(volumeIds);
		if (retVolumes.size() > 0) {
			return retVolumes.get(0);
		} else {
			return null;
		}
	}

	public List<Volume> getVolumes(Collection<String> volumeIds) {
		final DescribeVolumesRequest volumeRequest = new DescribeVolumesRequest();
		volumeRequest.setVolumeIds(volumeIds);

		DescribeVolumesResult result = AwsRetryManager.run(new Retryable<DescribeVolumesResult>() {
			@Override
			public DescribeVolumesResult run() {
				return cv.getEC2Client().describeVolumes(volumeRequest);
			}
		});
		return result.getVolumes();
	}
}
