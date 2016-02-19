package com.ipt.ebsa.agnostic.client.aws;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.agnostic.client.aws.module.AwsVmContainerModule;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.jcabi.aspects.Loggable;

/**
 * 
 *
 */
@Loggable(prepend=true)
public class AwsVirtualMachineContainerTest extends AwsBaseTest {

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	public void createConfirmDeleteVmc() throws Exception {
		//BeanUtils
		XMLVirtualMachineContainerType clonedVmc = shallowClone(getBaseTestVirtualMachineContainer(false,null));

		Weld weld = new Weld();
		WeldContainer container = weld.initialize();
		AwsVmContainerModule securityGroupModule = container.instance().select(AwsVmContainerModule.class).get();

		try{
			securityGroupModule.deleteVirtualMachineContainer(environment, clonedVmc);
		} catch (Exception e) {
			//Dont care if we get an error, try to make sure test will be clean.
		}
		Collection<String> groupIds = securityGroupModule.confirmVirtualMachineContainer(CmdStrategy.DOESNOTEXIST, environment, clonedVmc);
		Assert.assertTrue(groupIds.isEmpty());
		String securityGroupId = securityGroupModule.createVirtualMachineContainer(environment, clonedVmc);
		Assert.assertTrue(StringUtils.isNotBlank(securityGroupId));
		groupIds = securityGroupModule.confirmVirtualMachineContainer(CmdStrategy.EXISTS, environment, clonedVmc);
		Assert.assertTrue(!groupIds.isEmpty());
		Assert.assertTrue(groupIds.contains(securityGroupId));
		Assert.assertTrue(groupIds.size() == 1);
		securityGroupModule.deleteVirtualMachineContainer(environment, clonedVmc);
		groupIds = securityGroupModule.confirmVirtualMachineContainer(CmdStrategy.DOESNOTEXIST, environment, clonedVmc);
		Assert.assertTrue(groupIds.isEmpty());
	}

}
