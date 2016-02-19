package com.ipt.ebsa.environment.build.plugin;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import com.ipt.ebsa.environment.build.plugin.globalconfig.GlobalConfig;
import com.ipt.ebsa.environment.build.plugin.globalconfig.GlobalConfig.EnvironmentBuildPluginGlobalConfigDescriptor;
import com.ipt.ebsa.environment.plugin.process.ProcessOutput;

/**
 * Form bean-like/controller-like thingy for providing data to both the build parameters and the configuration
 * for an environment build.
 * 
 * @author James Shepherd
 */
public class EnvironmentBuildParameterDefinition extends SimpleParameterDefinition {
	
	private static final long serialVersionUID = 1L;
	private static final int XML_PARSE_ERROR_CODE = 100;
	private static final int CHECKOUT_ERROR_CODE = 200;
	private static final int PLAN_GENERATION_ERROR_CODE = 300;
	
	public static final String PARAMETER_DEF_NAME = EnvironmentBuildParameterDefinition.class.getName();
	
	public static final Logger LOG = Logger.getLogger(EnvironmentBuildParameterDefinition.class.getName());
	
	private String workDir;
	private String uniqueWorkDir;
	private File workspace;

	/**
	 * @param workDir Base directory in which build-specific sub-directories will be created
	 * during the running of the CLI (and then deleted).
	 */
	@DataBoundConstructor
	public EnvironmentBuildParameterDefinition(String workDir) {
		super(PARAMETER_DEF_NAME, "Provides the facility to define Environment Build parameters");
		this.workDir = workDir;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}
	
	/**
	 * @return Prediction of what the workspace will be, since we don't really know for certain during this phase of the
	 * plugin's lifecycle...
	 */
	@JavaScriptMethod
	public File getWorkspace() {
		if (workspace != null) {
			// Just in case someone's cleared down the workspace...
			if (!workspace.exists()) {
				workspace.mkdirs();
			}
			return workspace;
		}
		try {
			String url = Functions.getCurrentDescriptorByNameUrl();
			String build = "";
			build = URLDecoder.decode(url.substring(url.lastIndexOf('/') + 1).replace("+", "%2B"), "UTF-8").replace("%2B", "+");
			FilePath workspaceFile = Jenkins.getInstance().getWorkspaceFor(Jenkins.getInstance().getItem(build));
			workspace = new File(workspaceFile.toURI());
			if (!workspace.exists()) {
				workspace.mkdirs();
			}
			
			return workspace;
		} catch (Exception e) {
		}
		return null;
	}

	@JavaScriptMethod
	public JSONObject runPreparePhase(String environment, String container, String mode, String additionalParams, String version, String provider) {
		LOG.info(String.format("Running environment build prepare phase for environment [%s] and mode [%s] and provider [%s]", environment, mode, provider));
		
		try {
			// Stick the addition params in the workdir as there is no need to keep them after this build
			File additionalParamsOnDisk = new File(getUniqueWorkDir(), "additionalparams");
			// Stick the report in the workspace so it's visible from the Jenkins UI
			File reportPath = new File(getWorkspace(), generateReportName());
	
			try {
				FileUtils.write(additionalParamsOnDisk, additionalParams);
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Failed to write user params to disk", e);
			}
	
			try {
				List<String> args = Arrays.asList(new String[]{
						"-workdir=" + getUniqueWorkDir(),
						"-environment=" + environment,
						"-container=" + container,
						"-mode=" + mode,
						"-reportpath=" + reportPath.getAbsolutePath(),
						"-version=" + version,
						"-provider=" + provider,
						"-additionalparamspath=" + additionalParamsOnDisk.getAbsolutePath()
				});
				
				String command = "prepare";
				logProcessStart(args, command);
				ProcessOutput procOutput = new EnvironmentBuildExecutor(command).runCommand(args, getUniqueWorkDir());
				
				int returnCode = procOutput.getReturnCode();
				LOG.info("return code from environment build process: [" + returnCode + "]");
				if (returnCode == 0) {
					 procOutput.setAuxiliaryOutput(extractBodyContent(reportPath));
				} else {
					LOG.fine("Non zero return code from environment build process: [" + returnCode + "]");
					procOutput.setReturnCode(PLAN_GENERATION_ERROR_CODE);
				}
				
				return procOutput.toJSON();
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Exception while preparing report", e);
				JSONObject obj = new JSONObject();
				obj.put("returnCode", PLAN_GENERATION_ERROR_CODE);
				return obj;
			}
		} finally {
			teardownWorkingDir();
		}
	}

	private void logProcessStart(List<String> args, String command) {
		LOG.info(String.format("Performing action [%s] with arguments %s", command, args));
	}

	private String generateReportName() {
		String dt = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		return "report_prepare_" + dt + ".html";
	}

	private String extractBodyContent(File report) {
		try {
			Document html = Jsoup.parse(report, "UTF-8");
			return html.body().html().toString();
		} catch (IOException e) {
			return "Unable to parse generated report. Contact support";
		}
	}

	/**
	 * Performs a git clone of the environment plans directory, parses the data and returns
	 * the environments available for building.
	 * @param provider The cloud provider
	 */
	@JavaScriptMethod
	public JSONObject runCheckoutPhase(String provider) {
		File jsonDataPath = new File(getUniqueWorkDir(), "envandorgdata.json");
		FileInputStream is = null;
		try {
			// Trigger the Git clone and output the results to somewhere known in the work dir
			List<String> args = Arrays.asList(
				"-workdir=" + getUniqueWorkDir(),
				"-builddatapath=" + jsonDataPath.getAbsolutePath(),
				"-provider=" + provider
			);
	
			String command = "checkoutplans";
			logProcessStart(args, command);
			ProcessOutput procOutput = new EnvironmentBuildExecutor(command).runCommand(args, getUniqueWorkDir());
			
			int returnCode = procOutput.getReturnCode();
			if (returnCode != 0) {
				LOG.fine("Non zero return code from environment build plans checkout: [" + returnCode + "]");
			}
			
			String log = procOutput.getStdout();
			String err = procOutput.getStderr();
			if (returnCode != 0) {
				log = log + "\n\n=======ERR======\n\n" + err + "\n\n";
				LOG.fine("Unable to clone plans repo: " + log);
				procOutput.setReturnCode(CHECKOUT_ERROR_CODE);
				return procOutput.toJSON();
			}
			
			is = new FileInputStream(jsonDataPath);
			JsonSlurper slurper = new JsonSlurper();
			JSONObject obj = (JSONObject) slurper.parse(is);
			
			
			EnvironmentBuildPluginGlobalConfigDescriptor gc = (EnvironmentBuildPluginGlobalConfigDescriptor) GlobalConfig.getEnvironmentBuildPluginConfigDescriptor();
			if (gc.isLdapFilteringEnabled()) {
				User user = null;
				try {
					user = User.current();
					new EnvironmentFilterer().filterEnvironments(obj, user);
					if (user == null) {
						LOG.log(Level.WARNING, "Unable to get current user, maybe no one's logged in?");
					}
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Unable to get current user, maybe no one's logged in?", e);
				}
			}
			obj.put("returnCode", 0);
			
			FileUtils.deleteQuietly(jsonDataPath);
			
			return obj;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Exception while preparing report", e);
			JSONObject obj = new JSONObject();
			obj.put("returnCode", XML_PARSE_ERROR_CODE);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			obj.put("err", sw.toString());
		    return obj;
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Unable to remove temporary json file [" + jsonDataPath + "]");
			}
			teardownWorkingDir();
		}
	}

	private void teardownWorkingDir() {
		try {
			FileUtils.deleteDirectory(new File(getUniqueWorkDir()));
			uniqueWorkDir = null;
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to delete temporary work directory [" + getUniqueWorkDir() + "]");
		}
	}

	public static final class KeyValue {
		public final String key, value;
		
		public KeyValue(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}

	@Extension
	public static class DescriptorImpl extends ParameterDescriptor {

		@Override
		public String getDisplayName() {
			return "IPT Environment Build Parameters";
		}
	}

	@Override
	public EnvironmentBuildParameterValue createValue(StaplerRequest req, JSONObject jo) {
		EnvironmentBuildParameterValue value = req.bindJSON(EnvironmentBuildParameterValue.class, jo);
		// EBSAD-18152: Extract any user parameters from the request
		StringBuilder userParams = new StringBuilder();
		@SuppressWarnings("unchecked")
		Map<String, String[]> params = req.getParameterMap();
		for (Entry<String, String[]> param : params.entrySet()) {
			String name = param.getKey();
			if (name.startsWith("up_")) {
				userParams.append(name);
				userParams.append("=");
				userParams.append(param.getValue()[0]);
				userParams.append("&");
			}
		}
		int userParmsLength = userParams.length();
		if (userParmsLength > 0) {
			// Remove the trailing "&"
			userParams.setLength(userParmsLength - 1);
			value.setUserParameters(userParams.toString());
		}
		
		return value;
	}
	
	/** 
	 * Doubt this will ever be called?
	 * @see hudson.model.SimpleParameterDefinition#createValue(java.lang.String)
	 */
	@Override
	public ParameterValue createValue(String value) {
		return null;
	}
	
	private String getUniqueWorkDir() {
		if (uniqueWorkDir == null) {
			File uniqueWorkDirFile = new File(workDir, "work-" + UUID.randomUUID().toString());
			uniqueWorkDir = uniqueWorkDirFile.getAbsolutePath();
			if (!uniqueWorkDirFile.exists()) {
				uniqueWorkDirFile.mkdirs();
			}
		}
		return uniqueWorkDir;
	}

	public String getWorkDir() {
		return workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}
}