package com.ipt.ebsa.environment.hiera;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.environment.hiera.firewall.FirewallManager;
import com.ipt.ebsa.environment.hiera.test.BaseTest;
import com.ipt.ebsa.template.TemplateManager;

public class FirewallHieraManagerTest extends BaseTest {

	@Test
	public void testFirewall() throws IOException {
		FileUtils.copyDirectoryToDirectory(new File("src/test/resources/input_firewall"), workDir);
		File hieraRootDir = new File(workDir, "input_firewall");
		FirewallManager fwm = new FirewallManager(new File("src/test/resources/xl/IPTFirewallRulesPR.xlsx"));
		TemplateManager tm = new TemplateManager("src/test/resources/templates_firewall");
		FirewallHieraManager hieraManager = new FirewallHieraManager("HO_IPT_PR_CTL1", "0.26", XMLProviderType.SKYSCAPE.toString(), fwm, hieraRootDir, tm, new HieraFileManager(), null, null, null);
		hieraManager.prepare();
		hieraManager.execute();
		
		diff(new File("src/test/resources/output_firewall"), hieraRootDir);
	}
	
	@Test
	public void testFirewallCtl1Inbc() throws IOException {
		File hieraRootDir = new File(workDir, "firewall");
		FirewallManager fwm = new FirewallManager(new File("src/test/resources/xl/IPTFirewallRulesPR.xlsx"));
		TemplateManager tm = new TemplateManager("src/test/resources/templates_firewall");
		FirewallHieraManager hieraManager = new FirewallHieraManager("HO_IPT_PR_CTL1", "0.26", XMLProviderType.SKYSCAPE.toString(), fwm, hieraRootDir, tm, new HieraFileManager(), Sets.newSet("vyatta::firewall::rule"), Sets.newSet("HO_IPT_PR_CTL1_INBC"), null);
		hieraManager.prepare();
		hieraManager.execute();
		
		diff(new File("src/test/resources/output_firewall_ctl1_inbc"), hieraRootDir);
	}
}
