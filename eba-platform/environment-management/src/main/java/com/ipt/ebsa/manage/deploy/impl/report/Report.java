package com.ipt.ebsa.manage.deploy.impl.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.Deployment;

public abstract class Report {
	public abstract void generateReport(Deployment deployment) throws Exception;
	
	/**
	 * Figures out from the deployment configuration what the file name should be
	 * @param deploymentId
	 * @return
	 */
	protected File determineFile(String deploymentId) {
		String preparationSummaryReportFolder = Configuration.getPreparationSummaryReportFolder();
		if (preparationSummaryReportFolder == null) {
			preparationSummaryReportFolder = ".";
		}
		String preparationSummaryReportFileName = Configuration.getPreparationSummaryReportFileName();
		if (preparationSummaryReportFileName == null) {
			preparationSummaryReportFileName = "report.html";
		}
		
		File name = new File(preparationSummaryReportFolder, preparationSummaryReportFileName);
        if (Configuration.isUseUniqueName()) {
        	name = new File(name.getParent(), deploymentId + "_"+name.getName());
		}
		name.getParentFile().mkdirs();
		return name;
	}
	
	/**
	 * Writes all the data out to an output file.
	 * 
	 * @param file
	 * @param b
	 * @throws IOException
	 */
	protected void writeToFile(File file, StringBuffer b) throws IOException {
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(file);
			fileWriter.write(b.toString());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fileWriter != null) {
				fileWriter.close();
			}
		}
	}
}