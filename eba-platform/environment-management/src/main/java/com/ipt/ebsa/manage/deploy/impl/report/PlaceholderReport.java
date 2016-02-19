package com.ipt.ebsa.manage.deploy.impl.report;

import java.io.File;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.manage.deploy.Deployment;

public class PlaceholderReport extends Report {
	private Logger log = LogManager.getLogger(PlaceholderReport.class);

	@Override
	public void generateReport(Deployment deployment) throws Exception {
		log.debug("Writing out deployment report");
		File file = determineFile(deployment.getId());
		
		String content = IOUtils.toString(getClass().getResourceAsStream("/report-placeholder.html"));
		StringBuffer b = new StringBuffer(content);
		
		writeToFile(file, b);
		log.debug("Finished writing out deployment report to '"+file.getAbsolutePath()+"'");
	}

}
