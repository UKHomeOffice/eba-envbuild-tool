package com.ipt.ebsa.environment.hiera;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.environment.hiera.route.RouteManager;
import com.ipt.ebsa.environment.hiera.test.BaseTest;
import com.ipt.ebsa.template.TemplateManager;

public class InternalHieraManagerTest extends BaseTest {

	@Test
	public void testCreatePjt() throws IOException {
		File targetEnvironmentHieraDir = new File(workDir, "target_hiera");
		TemplateManager templateManager = new TemplateManager("src/test/resources/templates_internal");
		RouteManager rm = new RouteManager(new File("src/test/resources/xl/IPTRoutesNP.xls"), null);
		BaseHieraManager hm = new InternalHieraManager(targetEnvironmentHieraDir, "HO_IPT_NP_II_PJT3_DEV1", "0.2", XMLProviderType.SKYSCAPE.toString(), templateManager, rm, new HieraFileManager(), null, null, null);
		hm.prepare();
		hm.execute();
		diff(new File("src/test/resources/output_internal"), targetEnvironmentHieraDir);
	}
	
	@Test
	public void testCreateCtzoOnly() throws IOException {
		File targetEnvironmentHieraDir = new File(workDir, "target_hiera");
		TemplateManager templateManager = new TemplateManager("src/test/resources/templates_internal");
		RouteManager rm = new RouteManager(new File("src/test/resources/xl/IPTRoutesPR.xls"), null);
		BaseHieraManager hm = new InternalHieraManager(targetEnvironmentHieraDir, "HO_IPT_PR_CTL1", "0.26", XMLProviderType.SKYSCAPE.toString(), templateManager, rm, new HieraFileManager(), null, Sets.newSet("HO_IPT_PR_CTL1_CTZO"), null);
		hm.prepare();
		hm.execute();
		diff(new File("src/test/resources/output_internal_ctl1_ctzo"), targetEnvironmentHieraDir);
	}
	
	@Test
	public void testCreateInbcOnly() throws IOException {
		File targetEnvironmentHieraDir = new File(workDir, "target_hiera");
		TemplateManager templateManager = new TemplateManager("src/test/resources/templates_internal");
		RouteManager rm = new RouteManager(new File("src/test/resources/xl/IPTRoutesPR.xls"), null);
		BaseHieraManager hm = new InternalHieraManager(targetEnvironmentHieraDir, "HO_IPT_PR_CTL1", "0.26", XMLProviderType.SKYSCAPE.toString(), templateManager, rm, new HieraFileManager(), null, Sets.newSet("HO_IPT_PR_CTL1_INBC"), null);
		hm.prepare();
		hm.execute();
		diff(new File("src/test/resources/output_internal_ctl1_inbc"), targetEnvironmentHieraDir);
	}
	
	@Test
	public void testCreateCtzoRelatedScope() throws IOException {
		File targetEnvironmentHieraDir = new File(workDir, "target_hiera");
		TemplateManager templateManager = new TemplateManager("src/test/resources/templates_internal");
		RouteManager rm = new RouteManager(new File("src/test/resources/xl/IPTRoutesPR.xls"), Sets.newSet("HO_IPT_PR_CTL1_CTZO"));
		BaseHieraManager hm = new InternalHieraManager(targetEnvironmentHieraDir, "HO_IPT_PR_CTL1", "0.26", XMLProviderType.SKYSCAPE.toString(), templateManager, rm, new HieraFileManager(), Sets.newSet("system::network::interfaces"), Sets.newSet("HO_IPT_PR_CTL1_INBC"), null);
		hm.prepare();
		hm.execute();
		diff(new File("src/test/resources/output_internal_ctl1_ctzo_filtered"), targetEnvironmentHieraDir);
	}
	
	@Test
	public void testUpdateBehaviour() throws IOException {
		File inputDir = new File("src/test/resources/input_internal_ctl1_ctzo_deleted");
		File outputDir = new File("src/test/resources/output_internal_ctl1_ctzo_deleted"); 
		
		FileUtils.copyDirectoryToDirectory(inputDir, workDir);
		File targetEnvironmentHieraDir = new File(workDir, "input_internal_ctl1_ctzo_deleted");
		TemplateManager templateManager = new TemplateManager("src/test/resources/templates_internal");
		RouteManager rm = new RouteManager(new File("src/test/resources/xl/IPTRoutesPR.xls"), null);
		BaseHieraManager hm = new InternalHieraManager(targetEnvironmentHieraDir, "HO_IPT_PR_CTL1", "0.26", XMLProviderType.SKYSCAPE.toString(), templateManager, rm, new HieraFileManager(), Sets.newSet("ebsa::dnsrecords", "system::network::interfaces"), Sets.newSet("HO_IPT_PR_CTL1_CTZO"), UpdateBehaviour.OVERWRITE_ALL);
		hm.prepare();
		Set<BeforeAfter> beforeAfters = hm.getBeforeAfter();
		hm.execute();
		diff(outputDir, targetEnvironmentHieraDir);
		
		inputDir = new File(new File(inputDir, "pr"), "pr-ctl1-ctzo");
		outputDir = new File(new File(outputDir, "pr"), "pr-ctl1-ctzo");
		
		assertEquals(15, beforeAfters.size());
		
		for (BeforeAfter beforeAfter : beforeAfters) {
			String basename = beforeAfter.getBasename();
			assertEquals(getFileContents(new File(inputDir, basename)), beforeAfter.getBefore().replace("\r", ""));
			assertEquals(getFileContents(new File(outputDir, basename)), beforeAfter.getAfter().replace("\r", ""));
		}
	}
	
	private String getFileContents(File file) throws IOException {
		if (!file.exists()) {
			return "";
		}
		return FileUtils.readFileToString(file).replace("\r", "");
	}
}
