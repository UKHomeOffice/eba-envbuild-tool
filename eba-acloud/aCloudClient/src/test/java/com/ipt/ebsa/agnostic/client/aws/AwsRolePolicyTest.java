package com.ipt.ebsa.agnostic.client.aws;

import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.services.identitymanagement.model.Policy;
import com.amazonaws.services.identitymanagement.model.Role;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.jcabi.aspects.Loggable;

@Loggable(prepend = true)
public class AwsRolePolicyTest extends AwsBaseTest {
	
	
	@Test
	public void createDeleteRole() throws UnSafeOperationException, InterruptedException {
		String roleName = "001_testRole";
		String policyName = "001_testRole_Policy";
		
		roleManager.deleteRolePolicyForHa(roleName);	
		//Thread.sleep(10000);
		roleManager.createRolePolicyForHa(roleName);
		//Thread.sleep(10000);
		Role testRole = roleManager.getRole(roleName);
		roleManager.waitForRoleCreated(roleName);
		Assert.assertEquals(roleName, testRole.getRoleName());
		//Thread.sleep(10000);
		Policy p = roleManager.getPolicy(policyName);
		roleManager.waitForPolicyCreated(policyName);
		Assert.assertNotNull(policyName, p);
		Assert.assertEquals(policyName, p.getPolicyName());
		
		int count = 12;
		Integer policyCount = null;
		do {
			p = roleManager.getPolicy(policyName);
			policyCount = p.getAttachmentCount();
			if(count == 0) {
				break;
			}
			count--;
			Thread.sleep(10000);
		} while(p.getAttachmentCount() == 0);
		
		Assert.assertEquals(1, policyCount.intValue());
		roleManager.deleteRolePolicyForHa(roleName);
	}
	
	/**
	 * Lots of waiting as aws takes a while to propagate changes in IAM, but instantly returns references.
	 * So it can take a while for the changes to actually take effect, hence the waiting.
	 * 
	 * @throws InterruptedException
	 * @throws UnSafeOperationException
	 */
	@Test
	public void createDeleteInstanceProfile() throws InterruptedException, UnSafeOperationException {
		String roleName = "001_UNITTEST_ROLE";
		String instanceProfileName = "001_UNITTEST_ROLE.test.domain.local_IP";
		roleManager.removeRoleFromInstanceProfile(instanceProfileName, roleName, true);
		roleManager.deleteRolePolicyForHa(roleName);
		roleManager.deleteInstanceProfile(instanceProfileName);
		roleManager.removeRoleFromInstanceProfile(instanceProfileName, roleName, true);
		roleManager.deleteRolePolicyForHa(roleName);
		roleManager.deleteInstanceProfile(instanceProfileName);
		roleManager.createInstanceProfile(instanceProfileName);
		roleManager.createRolePolicyForHa(roleName);
		roleManager.assignRoleToInstanceProfile(instanceProfileName, roleName);
		roleManager.removeRoleFromInstanceProfile(instanceProfileName, roleName, true);
		roleManager.deleteInstanceProfile(instanceProfileName);
	}

}
