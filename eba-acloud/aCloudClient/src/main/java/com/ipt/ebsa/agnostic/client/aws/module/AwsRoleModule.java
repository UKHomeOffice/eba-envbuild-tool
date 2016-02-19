package com.ipt.ebsa.agnostic.client.aws.module;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.services.identitymanagement.model.AddRoleToInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.AttachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileResult;
import com.amazonaws.services.identitymanagement.model.CreatePolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreatePolicyResult;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleResult;
import com.amazonaws.services.identitymanagement.model.DeleteInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.DeletePolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.amazonaws.services.identitymanagement.model.DetachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetPolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetPolicyResult;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesResult;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesForRoleRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesForRoleResult;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.ListPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListPoliciesResult;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.Policy;
import com.amazonaws.services.identitymanagement.model.RemoveRoleFromInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.Retryable;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.WaitCondition;
import com.ipt.ebsa.agnostic.client.aws.util.AwsNamingUtil;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.ipt.ebsa.agnostic.client.logging.LogUtils;
import com.ipt.ebsa.agnostic.client.logging.LogUtils.LogAction;
import com.jcabi.aspects.Loggable;

/**
 * 
 *
 */
@Loggable(prepend = true)
public class AwsRoleModule extends AwsModule {

	private Logger logger = LogManager.getLogger(AwsVmModule.class);

	public Role createRole(String roleName, String assumeRolePolicyDocument) {
		final CreateRoleRequest request = new CreateRoleRequest();
		request.setRoleName(roleName);
		request.setAssumeRolePolicyDocument(assumeRolePolicyDocument);

		CreateRoleResult result = AwsRetryManager.run(new Retryable<CreateRoleResult>() {
			@Override
			public CreateRoleResult run() {
				return cv.getIAMClient().createRole(request);
			}
		});
		return result.getRole();
	}

	public void deleteRole(String roleName) {
		final DeleteRoleRequest request = new DeleteRoleRequest();
		request.setRoleName(roleName);
		Role r = getRoleFromList(roleName);

		if (r != null) {
			AwsRetryManager.run(new Retryable<Void>() {
				@Override
				public Void run() {
					cv.getIAMClient().deleteRole(request);
					return null;
				}
			});

		}
	}

	public void deletePolicy(String policyName) {
		final DeletePolicyRequest request = new DeletePolicyRequest();
		Policy p = getPolicyFromList(policyName);
		if (p != null) {
			request.setPolicyArn(p.getArn());

			AwsRetryManager.run(new Retryable<Void>() {
				@Override
				public Void run() {
					cv.getIAMClient().deletePolicy(request);
					return null;
				}
			});
		}
	}

	public Role getRole(String roleName) {
		final GetRoleRequest getRoleRequest = new GetRoleRequest();
		getRoleRequest.setRoleName(roleName);
		logger.debug("Looking up role:" + roleName);
		GetRoleResult result = null;
		Role retVal = null;
		try {
			result = AwsRetryManager.run(new Retryable<GetRoleResult>() {
				@Override
				public GetRoleResult run() {
					return cv.getIAMClient().getRole(getRoleRequest);
				}
			});
			retVal = result.getRole();
		} catch (NoSuchEntityException nsee) {
			retVal = null;
		}
		return retVal;
	}

	public Policy getPolicy(String policyName) {
		final GetPolicyRequest request = new GetPolicyRequest();
		request.setPolicyArn(getPolicyArn(policyName, ""));

		Policy retVal = null;
		try {
			GetPolicyResult result = AwsRetryManager.run(new Retryable<GetPolicyResult>() {
				@Override
				public GetPolicyResult run() {
					return cv.getIAMClient().getPolicy(request);
				}
			});
			retVal = result.getPolicy();
		} catch (NoSuchEntityException nsee) {
			retVal = null;
		}
		return retVal;
	}

	public Policy createPolicy(String policyName, String description, String policyDocument) {
		final CreatePolicyRequest request = new CreatePolicyRequest();
		request.setDescription(description);
		request.setPolicyName(policyName);
		request.setPolicyDocument(policyDocument);
		CreatePolicyResult result = AwsRetryManager.run(new Retryable<CreatePolicyResult>() {
			@Override
			public CreatePolicyResult run() {
				return cv.getIAMClient().createPolicy(request);
			}
		});
		return result.getPolicy();
	}

	public String createHaAssumeRolePolicyDocument() {
		StringBuilder haRole = new StringBuilder();
		haRole.append("{");
		haRole.append("	 \"Version\" : \"2012-10-17\",");
		haRole.append("	 \"Statement\": [");
		haRole.append("	 {");
		haRole.append("	 \"Effect\": \"Allow\",");
		haRole.append("	 \"Principal\": {");
		haRole.append("	 \"Service\": [ \"ec2.amazonaws.com\" ]");
		haRole.append("	 },");
		haRole.append("	 \"Action\": [ \"sts:AssumeRole\" ]");
		haRole.append("	 } ]");
		haRole.append("	}");
		return haRole.toString();
	}

	public String createHaPolicyDocument() {
		StringBuilder haRole = new StringBuilder();
		haRole.append("{");
		haRole.append("	 \"Version\" : \"2012-10-17\",");
		haRole.append("	 \"Statement\": [");
		haRole.append("	 {");
		haRole.append("	 \"Action\": [");
		haRole.append("	 \"ec2:AssignPrivateIpAddresses\",");
		haRole.append("	 \"ec2:DescribeInstances\"");
		haRole.append("	 ],");
		haRole.append("	 \"Effect\": \"Allow\",");
		haRole.append("	 \"Resource\": \"*\"");
		haRole.append("	 }");
		haRole.append("	 ]");
		haRole.append("	}");
		return haRole.toString();
	}

	public void attachPolicyToRole(String roleName, String policyArn) {
		final AttachRolePolicyRequest request = new AttachRolePolicyRequest();
		request.setRoleName(roleName);
		request.setPolicyArn(policyArn);

		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getIAMClient().attachRolePolicy(request);
				return null;
			}
		});

	}

	public void detachPolicyFromRole(String roleName, final String policyName) {
		final DetachRolePolicyRequest request = new DetachRolePolicyRequest();
		request.setRoleName(roleName);

		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				try {
					Policy p = getPolicyFromList(policyName);
					if (p != null) {
						request.setPolicyArn(p.getArn());
						cv.getIAMClient().detachRolePolicy(request);
					}
				} catch (com.amazonaws.services.identitymanagement.model.NoSuchEntityException e) {
					return null;
					// no policy to detach so we can move on
				}
				return null;
			}
		});

	}

	public String createRolePolicyForHa(String roleName) throws UnSafeOperationException {
		String policyName = createPolicyNameFromRole(roleName);
		Role role = getRoleFromList(roleName);
		Policy policy = getPolicyFromList(policyName);
		if (role != null && policy != null) {
			AttachedPolicy aPolicy = getAttachedPolicyFromRole(roleName, policyName);
			if (aPolicy != null) {
				return policy.getArn();
			} else {
				attachPolicyToRole(roleName, policy.getArn());
			}
		}

		Role r = createRole(roleName, createHaAssumeRolePolicyDocument());
		waitForRoleCreated(roleName);
		Policy p = createPolicy(policyName, null, createHaPolicyDocument());
		waitForPolicyCreated(policyName);
		attachPolicyToRole(r.getRoleName(), p.getArn());
		return p.getArn();
	}

	/**
	 * The exception handling is wrong and needs refactoring to be precise.
	 * Needed something quick and dirty to get round an issue.
	 * 
	 * @param roleName
	 */
	public void deleteRolePolicyForHa(final String roleName) throws UnSafeOperationException {
		final String policyName = AwsNamingUtil.getPolicyName(roleName);

		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				detachPolicyFromRole(roleName, policyName);
				try {
					waitForPolicyDetachedFromRole(roleName, policyName);
				} catch (UnSafeOperationException e) {
					throw new RuntimeException();
				}
				return null;
			}
		});

		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				deletePolicy(policyName);
				try {
					waitForPolicyDeleted(policyName);
				} catch (UnSafeOperationException e) {
					throw new RuntimeException();
				}
				return null;
			}
		});

		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				removeRoleFromInstanceProfile(createInstanceProfileNameFromRole(roleName), roleName, false);
				return null;

			}
		});

		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				deleteRole(roleName);
				try {
					waitForRoleDeleted(roleName);
				} catch (UnSafeOperationException e) {
					throw new RuntimeException();
				}
				return null;
			}
		});
	}

	public String getPolicyArn(String roleName) {
		String policyName = createPolicyNameFromRole(roleName);
		return getPolicyArn(policyName, "");
	}

	public String getPolicyArn(String policyName, String region) {
		String partition = "aws";
		String service = "iam";
		String account = cv.getAccount();
		String resource = "policy";
		StringBuilder sb = new StringBuilder();
		sb.append("arn");
		sb.append(":");
		sb.append(partition);
		sb.append(":");
		sb.append(service);
		sb.append(":");
		sb.append(region);
		sb.append(":");
		sb.append(account);
		sb.append(":");
		sb.append(resource);
		sb.append("/");
		sb.append(policyName);

		return sb.toString();
	}

	public String createPolicyNameFromRole(String roleName) {
		return roleName + "_Policy";
	}

	public String createInstanceProfileNameFromRole(String roleName) {
		return roleName + "_IP";
	}

	public InstanceProfile createInstanceProfile(String instanceProfileName) {
		final CreateInstanceProfileRequest request = new CreateInstanceProfileRequest();
		request.setInstanceProfileName(instanceProfileName);
		LogUtils.log(LogAction.CREATING, "InstanceProfile", instanceProfileName);
		CreateInstanceProfileResult result = AwsRetryManager.run(new Retryable<CreateInstanceProfileResult>() {
			@Override
			public CreateInstanceProfileResult run() {
				return cv.getIAMClient().createInstanceProfile(request);
			}
		});
		LogUtils.log(LogAction.CREATED, "InstanceProfile", instanceProfileName);
		return result.getInstanceProfile();
	}

	public void deleteInstanceProfileByArn(final String instanceProfileArn) {
		String iamInstanceProfileName = AwsNamingUtil.getIamInstanceProfileNameFromArn(instanceProfileArn);
		deleteInstanceProfile(iamInstanceProfileName);
	}

	public void deleteInstanceProfile(final String instanceProfileName) {

		final DeleteInstanceProfileRequest request = new DeleteInstanceProfileRequest();
		request.setInstanceProfileName(instanceProfileName);
		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				InstanceProfile ip = getInstanceProfileFromList(instanceProfileName);
				if (ip != null) {
					for (Role r : ip.getRoles()) {
						removeRoleFromInstanceProfile(instanceProfileName, r.getRoleName(), true);
						try {
							waitForRoleDetachedInstanceProfile(r.getRoleName(), instanceProfileName);
						} catch (UnSafeOperationException e) {
							throw new RuntimeException();
						}
					}
					cv.getIAMClient().deleteInstanceProfile(request);
				}
				return null;
			}
		});
	}

	public void assignRoleToInstanceProfile(String instanceProfileName, String roleName) {
		final AddRoleToInstanceProfileRequest request = new AddRoleToInstanceProfileRequest();
		request.setInstanceProfileName(instanceProfileName);
		request.setRoleName(roleName);

		AwsRetryManager.run(new Retryable<Void>() {
			@Override
			public Void run() {
				cv.getIAMClient().addRoleToInstanceProfile(request);
				return null;
			}
		});

	}

	public void removeRoleFromInstanceProfile(final String instanceProfileName, final String roleName, final boolean deleteRoleAndPolicy) {
		final RemoveRoleFromInstanceProfileRequest request = new RemoveRoleFromInstanceProfileRequest();
		request.setInstanceProfileName(instanceProfileName);
		request.setRoleName(roleName);
		InstanceProfile ip = getInstanceProfileFromList(instanceProfileName);

		if (ip != null) {

			AwsRetryManager.run(new Retryable<Void>() {
				@Override
				public Void run() {
					Role r = getRoleFromList(roleName);
					if (r != null) {
						AwsRetryManager.run(new Retryable<Void>() {
							@Override
							public Void run() {
								cv.getIAMClient().removeRoleFromInstanceProfile(request);
								return null;
							}
						});
						try {
							waitForRoleDetachedInstanceProfile(roleName, instanceProfileName);
							if (deleteRoleAndPolicy) {
								deleteRolePolicyForHa(roleName);
							}
						} catch (UnSafeOperationException e) {
							throw new RuntimeException();
						}
					}
					return null;
				}
			});
		}

	}

	public InstanceProfile getInstanceProfile(String roleName) {
		String instanceProfileName = createInstanceProfileNameFromRole(roleName);
		return getInstanceProfileFromList(instanceProfileName);
	}

	// public InstanceProfile getInstanceProfile(String instanceProfileName,
	// boolean profile) {
	// GetInstanceProfileRequest request = new GetInstanceProfileRequest();
	// request.setInstanceProfileName(instanceProfileName);
	// GetInstanceProfileResult result = null;
	// InstanceProfile profileResult = null;
	// try {
	// result = cv.getIAMClient().getInstanceProfile(request);
	// profileResult = result.getInstanceProfile();
	// } catch(NoSuchEntityException e) {
	// if(e.getStatusCode() != Response.Status.NOT_FOUND.getStatusCode()) {
	// throw e;
	// }
	// }
	// return profileResult;
	// }

	public InstanceProfile getInstanceProfileFromList(String instanceProfileName) {
		final ListInstanceProfilesRequest request = new ListInstanceProfilesRequest();
		request.setPathPrefix("/");
		ListInstanceProfilesResult results = AwsRetryManager.run(new Retryable<ListInstanceProfilesResult>() {
			@Override
			public ListInstanceProfilesResult run() {
				return cv.getIAMClient().listInstanceProfiles(request);
			}
		});

		for (InstanceProfile p : results.getInstanceProfiles()) {
			if (p.getInstanceProfileName().equals(instanceProfileName)) {
				return p;
			}
		}
		return null;
	}

	public Role getRoleFromList(String roleName) {
		final ListRolesRequest request = new ListRolesRequest();
		request.setPathPrefix("/");

		ListRolesResult results = AwsRetryManager.run(new Retryable<ListRolesResult>() {
			@Override
			public ListRolesResult run() {
				return cv.getIAMClient().listRoles(request);
			}
		});
		for (Role p : results.getRoles()) {
			if (p.getRoleName().equals(roleName)) {
				return p;
			}
		}
		return null;
	}

	public InstanceProfile getRoleAttachedToInstanceProfileFromList(String roleName, String instanceProfileName) {
		final ListInstanceProfilesForRoleRequest request = new ListInstanceProfilesForRoleRequest();
		request.setRoleName(roleName);

		ListInstanceProfilesForRoleResult results = AwsRetryManager.run(new Retryable<ListInstanceProfilesForRoleResult>() {
			@Override
			public ListInstanceProfilesForRoleResult run() {
				return cv.getIAMClient().listInstanceProfilesForRole(request);
			}
		});
		if (results.getInstanceProfiles().size() > 0) {
			for (InstanceProfile p : results.getInstanceProfiles()) {
				if (p.getInstanceProfileName().equals(instanceProfileName)) {
					return p;
				}
			}
		}
		return null;
	}

	public Policy getPolicyFromList(String policyName) {
		final ListPoliciesRequest request = new ListPoliciesRequest();
		request.setPathPrefix("/");

		ListPoliciesResult results = AwsRetryManager.run(new Retryable<ListPoliciesResult>() {
			@Override
			public ListPoliciesResult run() {
				return cv.getIAMClient().listPolicies(request);
			}
		});
		for (Policy p : results.getPolicies()) {
			if (p.getPolicyName().equals(policyName)) {
				return p;
			}
		}
		return null;
	}

	public AttachedPolicy getAttachedPolicyFromRole(final String roleName, final String policyName) {
		final ListAttachedRolePoliciesRequest request = new ListAttachedRolePoliciesRequest();
		request.setPathPrefix("/");
		request.setRoleName(roleName);

		Role r = getRoleFromList(roleName);
		if (r != null) {
			ListAttachedRolePoliciesResult results = AwsRetryManager.run(new Retryable<ListAttachedRolePoliciesResult>() {
				@Override
				public ListAttachedRolePoliciesResult run() {
					return cv.getIAMClient().listAttachedRolePolicies(request);
				}
			});
			for (AttachedPolicy p : results.getAttachedPolicies()) {
				if (p.getPolicyName().equals(policyName)) {
					return p;
				}
			}
		}
		return null;
	}

	public void waitForPolicyDetachedFromRole(final String roleName, final String policyName) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getAttachedPolicyFromRole(roleName, policyName) == null;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Detached Policy: " + policyName + " From Role: " + roleName + " was not propigated in time: ", 10000,
				true);
	}

	public void waitForRoleDetachedInstanceProfile(final String roleName, final String instanceProfileName) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getRoleAttachedToInstanceProfileFromList(roleName, instanceProfileName) == null;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Detached Role: " + roleName + " From Instance Profile: " + instanceProfileName
				+ " was not propigated in time: ", 10000, true);
	}

	public void waitForRoleAssignToInstanceProfile(final String roleName, final String instanceProfileName) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getRoleAttachedToInstanceProfileFromList(roleName, instanceProfileName) != null;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Detached Role: " + roleName + " From Instance Profile: " + instanceProfileName
				+ " was not propigated in time: ", 10000, true);
	}

	public void waitForRoleCreated(final String roleName) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getRoleFromList(roleName) != null;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Role: " + roleName + " was not propigated in time: ", 10000, true);
	}

	public void waitForPolicyCreated(final String policyName) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getPolicyFromList(policyName) != null;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Policy: " + policyName + " creation was not propigated in time: ", 10000, true);
	}

	public void waitForPolicyDeleted(final String policyName) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getPolicyFromList(policyName) == null;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Policy: " + policyName + " deletion was not propigated in time: ", 10000, true);
	}

	public void waitForInstanceProfileDeleted(final String instanceProfileName) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getInstanceProfileFromList(instanceProfileName) == null;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Instance Profile: " + instanceProfileName + " deletion was not propigated in time: ", 10000, true);
	}

	public void waitForInstanceProfileCreated(final String instanceProfileName) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getInstanceProfileFromList(instanceProfileName) != null;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Instance Profile: " + instanceProfileName + " creation was not propigated in time: ", 10000, true);
	}

	public void waitForRoleDeleted(final String roleName) throws UnSafeOperationException {
		WaitCondition waitCondition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return getRoleFromList(roleName) == null;
			}
		};
		AwsRetryManager.waitFor(waitCondition, "Role: " + roleName + " deletion was not propigated in time: ", 10000, true);
	}
}
