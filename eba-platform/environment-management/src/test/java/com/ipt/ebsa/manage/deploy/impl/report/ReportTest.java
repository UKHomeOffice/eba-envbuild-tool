package com.ipt.ebsa.manage.deploy.impl.report;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.manage.deploy.Deployment;

import static org.mockito.Mockito.*;

/**
 * Test some report HTML dumping
 * @author Dan McCarthy
 *
 */
public class ReportTest {

	/**
	 * Uses cheeky reflection to call the private method Report.dumpFailFiles() and assert the response for non-null input
	 * @throws Exception
	 */
	@Test
	public void dumpFailFilesTest() throws Exception {
		Map<String, String> failFiles = new TreeMap<String, String>();
		failFiles.put("etctzm01.st-sit1-cor1.ipt.local", "");
		failFiles.put("doctzm01.st-sit1-cor1.ipt.local", "Failed RPM File exists");
		failFiles.put("mzfmzm01.st-sit1-cor1.ipt.local", "");
		
		Deployment deployment = mock(Deployment.class);
		when(deployment.getFailFilesByFQDN()).thenReturn(failFiles);
		
		StringBuffer dump = new StringBuffer();
		
		Method dumpFailFiles = ApplicationReport.class.getDeclaredMethod("dumpFailFiles", Deployment.class, StringBuffer.class);
		dumpFailFiles.setAccessible(true);
		dumpFailFiles.invoke(new ApplicationReport(), deployment, dump);
		
		String htmlDump = 
				"<table id=\"failfiles\">"
				+ "<thead>"
				+ "<tr>"
				+ "<th>Host</th>"
				+ "<th>Status</th>"
				+ "</tr>"
				+ "</thead>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td valign=\"top\">doctzm01.st-sit1-cor1.ipt.local</td>"
				+ "<td style=\"color: red\" valign=\"top\">Failed RPM File exists</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td valign=\"top\">etctzm01.st-sit1-cor1.ipt.local</td>"
				+ "<td style=\"color: black\" valign=\"top\">None found</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td valign=\"top\">mzfmzm01.st-sit1-cor1.ipt.local</td>"
				+ "<td style=\"color: black\" valign=\"top\">None found</td>"
				+ "</tr>"
				+ "</tbody>"
				+ "</table>";
		Assert.assertEquals(htmlDump, dump.toString());
	}
	
	/**
	 * Uses cheeky reflection to call the private method Report.dumpFailFiles() and assert the response for empty input
	 * @throws Exception
	 */
	@Test
	public void dumpFailFilesTestEmpty() throws Exception {
		Deployment deployment = mock(Deployment.class);
		when(deployment.getFailFilesByFQDN()).thenReturn(new TreeMap<String, String>());
		
		StringBuffer dump = new StringBuffer();
		
		Method dumpFailFiles = ApplicationReport.class.getDeclaredMethod("dumpFailFiles", Deployment.class, StringBuffer.class);
		dumpFailFiles.setAccessible(true);
		dumpFailFiles.invoke(new ApplicationReport(), deployment, dump);
		
		String htmlDump = 
				"<table id=\"failfiles\">"
				+ "<thead>"
				+ "<tr>"
				+ "<th>Host</th>"
				+ "<th>Status</th>"
				+ "</tr>"
				+ "</thead>"
				+ "<tbody>"
				+ "<tr>"
				+ "<td style=\"color: red\" colspan=\"2\" valign=\"top\">Unable to report RPM fail files - check the logs for details</td>"
				+ "</tr>"
				+ "</tbody>"
				+ "</table>";
		Assert.assertEquals(htmlDump, dump.toString());
	}
	
}
