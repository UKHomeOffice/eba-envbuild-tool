package com.ipt.ebsa.environment.build.manage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.environment.build.execute.BuildPerformer;
import com.ipt.ebsa.environment.build.execute.PerformerFactory;
import com.ipt.ebsa.environment.build.execute.action.ActionPerformer;
import com.ipt.ebsa.environment.build.execute.action.ActionPerformerFactory;
import com.ipt.ebsa.environment.build.execute.report.ReportWriter;
import com.ipt.ebsa.environment.build.prepare.PrepareContext;
import com.ipt.ebsa.environment.data.factory.EnvironmentDataFactory;
import com.ipt.ebsa.environment.data.factory.EnvironmentStartupShutdownDataFactory;
import com.ipt.ebsa.environment.data.model.EnvironmentData;
import com.ipt.ebsa.environment.data.model.EnvironmentDetailsHolder;
import com.ipt.ebsa.environment.metadata.export.agnostic.EnvironmentBuildMetadataAgnosticExport;
import com.ipt.ebsa.ssh.ExecReturn;
import com.ipt.ebsa.yaml.YamlUtil;

/**
 * Need to call {@link #prepareForEnvironment(String, String, String, Map, String)} before calling {@link #execute()}
 * @author James Shepherd
 */
public class BuildManager {

	private static final Logger LOG = Logger.getLogger(BuildManager.class);
	public static final String DEFAULT_REPORT_PATH = "report.html";
	
	private PerformerFactory performerFactory;
	private PrintWriter reportPrintWriter = null;
	private BuildPerformer buildPerformer = null;
	private EnvironmentData environmentData;
	
	private BuildManager(PerformerFactory performerFactory, EnvironmentData environmentData) {
		this.performerFactory = performerFactory;
		this.environmentData = environmentData;
	}
	
	public void prepareForEnvironment(String workDir, String environment, String version, String buildRefId, String provider, Map<String, String> rawAdditionalParams, String envDefnXmlPath) {
		buildPerformer = performerFactory.getEnvironmentBuildPerformer(workDir, environment, version, buildRefId, provider, rawAdditionalParams, envDefnXmlPath);
		
		for (ActionPerformer ap : buildPerformer.getActionPerformers()) {
			ap.prepare();
		}
		
		if(useReportWriter()) {
			ReportWriter reportWriter = new ReportWriter(buildPerformer.getBuild());
			PrintWriter reportPrintWriter = getReportPrintWriter();
			reportPrintWriter.println(reportWriter.toHTML());
		}
	}
	
	public void prepareForEnvironment(String workDir, String environment, String version, String buildRefId, String provider, Map<String, String> rawAdditionalParams, String envDefnXmlPath, PrepareContext context) {
		buildPerformer = performerFactory.getEnvironmentBuildPerformer(workDir, environment, version, buildRefId, provider, rawAdditionalParams, envDefnXmlPath);
		
		for (ActionPerformer ap : buildPerformer.getActionPerformers()) {
			ap.prepare(context);
		}
		
		if(useReportWriter()) {
			ReportWriter reportWriter = new ReportWriter(buildPerformer.getBuild());
			PrintWriter reportPrintWriter = getReportPrintWriter();
			reportPrintWriter.println(reportWriter.toHTML());
		}
	}
	
	public void prepareForStartupShutdown(String workDir, String environment, String version, String buildRefId, String provider, Map<String, String> rawAdditionalParams, String envDefnXmlPath) {
		buildPerformer = performerFactory.getEnvironmentBuildPerformer(workDir, environment, version, buildRefId, provider, rawAdditionalParams, envDefnXmlPath);
		
		for (ActionPerformer ap : buildPerformer.getActionPerformers()) {
			ap.prepare();
		}
		
		if(useReportWriter()) {
			ReportWriter reportWriter = new ReportWriter(buildPerformer.getBuild());
			PrintWriter reportPrintWriter = getReportPrintWriter();
			reportPrintWriter.println(reportWriter.toHTML());
		}
	}
	
	public void prepareForContainer(String workDir, String container, String version, String buildRefId, String provider, Map<String, String> rawAdditionalParams, String envDefnXmlPath) {
		buildPerformer = performerFactory.getContainerBuildPerformer(workDir, container, version, buildRefId, provider, rawAdditionalParams, envDefnXmlPath);
		
		for (ActionPerformer ap : buildPerformer.getActionPerformers()) {
			ap.prepare();
		}
		
		if(useReportWriter()) {
			ReportWriter reportWriter = new ReportWriter(buildPerformer.getBuild());
			PrintWriter reportPrintWriter = getReportPrintWriter();
			reportPrintWriter.println(reportWriter.toHTML());
		}
	}

	public void execute() {
		if (null == buildPerformer) {
			throw new RuntimeException("Need to call prepare before calling execute, or I have no BuildPerformer");
		}
		
		for (ActionPerformer actionPerformer : buildPerformer.getActionPerformers()) {
			ExecReturn execute = actionPerformer.execute();
			if (0 != execute.getReturnCode()) {
				String msg = String.format("Action [%s][%s] failed, terminating build",
						actionPerformer.getActionDisplayName(), actionPerformer.getAction().getId());
				LOG.warn(msg);
				throw new RuntimeException(msg);
			}
		}
	}
	
	public void cleanUp() {
		if (null != reportPrintWriter) {
			reportPrintWriter.close();
		}
		
		if (null != buildPerformer) {
			for (ActionPerformer actionPerformer : buildPerformer.getActionPerformers()) {
				try {
					actionPerformer.cleanUp();
				} catch (Exception e) {
					LOG.warn("Error running cleanUp", e);
				}
			}
		}
	}
	
	public PrintWriter getReportPrintWriter() {
		if (null == reportPrintWriter && useReportWriter) {
			try {
				reportPrintWriter = new PrintWriter(new FileWriter(new File(DEFAULT_REPORT_PATH)));
			} catch (IOException e) {
				throw new RuntimeException("Failed to open default report path: " + DEFAULT_REPORT_PATH, e);
			}
		}
		return reportPrintWriter;
	}
	
	boolean useReportWriter = false;
	
	public boolean useReportWriter() {
		return useReportWriter;
	}
	public void setReportPrintWriter(PrintWriter reportPrintWriter) {
		useReportWriter = true;
		this.reportPrintWriter = reportPrintWriter;
	}

	/**
	 * Use all the default implementations to construct.
	 * @param buildPlanDirectory
	 * @return fully configured BuildManager
	 */
	public static BuildManager getDefaultInstance(File buildPlanDirectory) {
		EnvironmentDataFactory environmentDataFactory = new EnvironmentDataFactory();
		EnvironmentData environmentData = environmentDataFactory.getEnvironmentDataInstance(buildPlanDirectory);
		PerformerFactory performerFactory = new PerformerFactory(environmentData, new ActionPerformerFactory());
		return new BuildManager(performerFactory, environmentData);
	}
	
	@SuppressWarnings("unchecked")
	public static BuildManager getStartupShutdownInstance(File buildPlanDirectory, File susdYamlFile, String environmentName, String vmc, boolean start, String reportPath) {
		EnvironmentStartupShutdownDataFactory environmentDataFactory = new EnvironmentStartupShutdownDataFactory();
		
		if (susdYamlFile == null) {
			throw new IllegalArgumentException("File parameter cannot be null.");
		}
		
		if (!susdYamlFile.exists()) {
			throw new IllegalStateException("Unable to load file '" + susdYamlFile.getAbsolutePath() + "' as it does not exist.");
		}
		
		LOG.info("Loading from directory [" + susdYamlFile.getAbsolutePath() + "]");
		
		Map<String, Object> susdYaml = null;
		try {
			susdYaml = YamlUtil.readYaml(susdYamlFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
			//Should not be possible as the files existence is previously checked
		}
		
		Object startOrder = susdYaml.get("startuporder");
		Object shutdownOrder = susdYaml.get("shutdownorder");
		
		ArrayList<String> order = null;
		if(start) {
			
			if(startOrder == null && shutdownOrder != null) {
				order = (ArrayList<String>)shutdownOrder;
				Collections.reverse(order);
			} else {
				order = (ArrayList<String>)startOrder;
			}
		} else {
			if(shutdownOrder == null && startOrder != null) {
				order = (ArrayList<String>)startOrder;
				Collections.reverse(order);
			} else {
				order = (ArrayList<String>)shutdownOrder;
			}
		}
		
		String provider = "";
		
		String configFileName = susdYamlFile.getName();
		if(configFileName.startsWith("np")) {
			provider = "SKYSCAPE";
		} else if(configFileName.startsWith("npa")) {
			provider = "AWS";
		} else {
			provider = "SKYSCAPE";
		}
		
		XMLGeographicContainerType geographicContainer = null;
		EnvironmentBuildMetadataAgnosticExport md = new EnvironmentBuildMetadataAgnosticExport();
		try {
			geographicContainer = md.extractEnvironmentsDeployed(environmentName, provider);
		} catch (Exception e) {
			LOG.error("Unable to get deployed environment container",e);
			throw new RuntimeException(e);
		}
		
		EnvironmentData environmentData = environmentDataFactory.getEnvironmentDataInstance(susdYaml, geographicContainer, environmentName, vmc, start, order);
		PerformerFactory performerFactory = new PerformerFactory(environmentData, new ActionPerformerFactory());
		BuildManager buildManager = new BuildManager(performerFactory, environmentData);
		
		File reportHtmlFile = null;
		if(StringUtils.isNotBlank(reportPath)) {
			reportHtmlFile = new File(reportPath);
			try {
				reportHtmlFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(!reportHtmlFile.isFile()) {
			reportHtmlFile = new File(buildPlanDirectory,System.currentTimeMillis()+"_report.html");
		}
		
		try {
			PrintWriter reportPrintWriter = new PrintWriter(new FileWriter(reportHtmlFile));
			buildManager.setReportPrintWriter(reportPrintWriter);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		LOG.info(String.format("Report will be written to [%s]", reportHtmlFile));
		
		EnvironmentDetailsHolder details = environmentData.getEnvironmentDetails();
		PrepareContext context = new PrepareContext();
		context.setGeographicContainer(environmentData.getEnvironmentDetails().getGeographicContainer());
		buildManager.prepareForEnvironment(buildPlanDirectory.getAbsolutePath(), details.getEnvironmentName(), details.getVersion(), details.getBuildReferenceid(), provider, new HashMap<String, String>(), "", context);
		return buildManager;
	}

	public EnvironmentData getEnvironmentData() {
		return environmentData;
	}
}
