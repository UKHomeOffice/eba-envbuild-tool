package com.ipt.ebsa.environment.build.plugin;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.ParametersAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.ipt.ebsa.environment.build.entities.EnvironmentBuild;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerBuild;
import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition.DefinitionType;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.environment.build.manager.UpdateManager;
import com.ipt.ebsa.environment.plugin.process.ProcessOutput;
import com.ipt.ebsa.environment.plugin.util.ValidationUtil;
import com.ipt.ebsa.util.collection.ParamFactory;

/**
 * Carries out an automated environment or container build.
 */
public class EnvironmentBuildPlugin extends Builder {
	
	public static final Logger LOG = Logger.getLogger(EnvironmentBuildPlugin.class.getName());
	
    @DataBoundConstructor
    public EnvironmentBuildPlugin() {
		super();
	}

	/**
     * This is the method which provides the main functionality for this class
     */
	@Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		boolean output = false;
		
	    /* Extract the values out of parameters specified by the user. */
		EnvironmentBuildParameterValue jobParameters = null;
		List<ParametersAction> parameters = build.getActions(ParametersAction.class);
        for (ParametersAction parametersAction : parameters) {
			ParameterValue value = parametersAction.getParameter(EnvironmentBuildParameterValue.ENVIRONMENT_BUILD_PARAMETER);
			if (value instanceof EnvironmentBuildParameterValue) {
				jobParameters = (EnvironmentBuildParameterValue) value;
			}
		}
        File uniqueWorkDir = null;
        try {
			if (jobParameters == null) {
				throw new Exception("You have not provided any parameters for this job.  You need to select a command to be used as a parameter for this job");
			}
			String command = "run";
			
			listener.getLogger().println("Command: " + command);

			String environment = jobParameters.getEnvironment();
			boolean isEnvironmentBuild = StringUtils.isNotBlank(environment);
			String container = jobParameters.getContainer();
			
			ParamFactory mandatoryParams = ParamFactory.with("build", jobParameters.getMode()).and("version", jobParameters.getVersion()).and("provider", jobParameters.getProvider());
			FormValidation formValidation = ValidationUtil.validateMandatory(mandatoryParams);
			if (FormValidation.ok() == formValidation && !isEnvironmentBuild && StringUtils.isBlank(container)) {
				formValidation = FormValidation.error("Must specify either environment or container");
			}
			if (FormValidation.ok() != formValidation) {
				// Invalid parameters
				throw formValidation;
			}
			
			String envOrContainer = isEnvironmentBuild ? environment : container;
			
			Object buildRecord = recordBuildStart(envOrContainer, isEnvironmentBuild, jobParameters, build);
			
			File reportFile = new File(new File(build.getWorkspace().toURI()), generateReportName(build));
			
			uniqueWorkDir = new File(jobParameters.getWorkDir(), "work-" + UUID.randomUUID().toString());
			if (!uniqueWorkDir.exists()) {
				uniqueWorkDir.mkdirs();
			}
			
			File additionalParamsOnDisk = new File(uniqueWorkDir, "additionalparams");
			try {
				FileUtils.write(additionalParamsOnDisk, jobParameters.getUserParameters());
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Failed to write user params to disk", e);
			}

			List<String> args = Arrays.asList(new String[]{
					"-workdir=" + uniqueWorkDir.getAbsolutePath(),
					"-environment=" + StringUtils.defaultString(jobParameters.getEnvironment()),
					"-container=" + StringUtils.defaultString(jobParameters.getContainer()),
					"-mode=" + jobParameters.getMode(),
					"-reportpath=" + reportFile.getAbsolutePath(),
					"-version=" + jobParameters.getVersion(),
					"-provider=" + jobParameters.getProvider(),
					"-additionalparamspath=" + additionalParamsOnDisk.getAbsolutePath(),
			});
			
			ProcessOutput procOutput = new EnvironmentBuildExecutor(command).runEnvironmentBuildStep(build, launcher, listener, args);
			
			int returnCode = procOutput.getReturnCode();
			if (returnCode != 0) {
				listener.getLogger().println("Non zero return code from environment build process.");
			} else {
				output = true;
			}
			
			String log = procOutput.getStdout();
			String err = procOutput.getStderr();
			if (StringUtils.isNotEmpty(err)) {
				log = log + "\n\n=======ERR======\n\n" + err + "\n\n";
			}
			
			String report = null;
			if (reportFile.canRead()) {
				report = FileUtils.readFileToString(reportFile);
			} else {
				listener.getLogger().printf("Unable to read report from file [%s]", reportFile.getAbsolutePath());
				output = false;
			}
			
			recordBuildEnd(buildRecord, isEnvironmentBuild, output, log, report);
			
		} catch (Exception e) {
			listener.getLogger().println("Exception while building. " + e.getClass().getName() + " " + e.getMessage());
		    e.printStackTrace(listener.getLogger());
		    output = false;
		} finally {
			if (uniqueWorkDir != null) {
				try {
					FileUtils.deleteDirectory(uniqueWorkDir);
				} catch (IOException e) {
					listener.getLogger().println("Unable to remove temporary working directory [" + uniqueWorkDir.getAbsolutePath() + "]");
				}
			}
		}
        
        return output;
    }
	
	private String generateReportName(AbstractBuild<?, ?> build) {
		String dt = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		return "report_run_" + dt + "_" + build.getNumber() + ".html";
	}

	/**
	 * Records the start of an environment definition build or an environment container definition build in the database 
	 * @param envOrContainer
	 * @param isEnvironmentBuild
	 * @param jobParameters
	 * @param build
	 * @return
	 * @throws Exception
	 */
	private Object recordBuildStart(String envOrContainer, boolean isEnvironmentBuild, EnvironmentBuildParameterValue jobParameters, AbstractBuild<?, ?> build) throws Exception {
		// TODO Improve this, perhaps by creating a common superclass which EnvironmentBuild and EnvironmentContainerBuild inherit from
		if (isEnvironmentBuild) {
			EnvironmentDefinition environmentdefinition = new ReadManager().getEnvironmentDefinition(envOrContainer, jobParameters.getVersion(), DefinitionType.Physical, jobParameters.getProvider());
			
			EnvironmentBuild environmentBuild = new EnvironmentBuild();
			environmentBuild.setDateStarted(new Date());
			environmentBuild.setEnvironmentDefinition(environmentdefinition);
			environmentBuild.setJenkinsBuildId(build.getId());
			environmentBuild.setJenkinsBuildNumber(build.getNumber());
			environmentBuild.setJenkinsJobName(build.getProject().getName());
			
			UpdateManager updateManager = new UpdateManager();
			updateManager.saveEnvironmentBuild(environmentBuild);
			return environmentBuild;
		} else {
			EnvironmentContainerDefinition environmentcontainerdefinition = new ReadManager().getEnvironmentContainerDefinition(envOrContainer, jobParameters.getVersion(), jobParameters.getProvider());
			
			EnvironmentContainerBuild environmentContainerBuild = new EnvironmentContainerBuild();
			environmentContainerBuild.setDateStarted(new Date());
			environmentContainerBuild.setEnvironmentContainerDefinition(environmentcontainerdefinition);
			environmentContainerBuild.setJenkinsBuildId(build.getId());
			environmentContainerBuild.setJenkinsBuildNumber(build.getNumber());
			environmentContainerBuild.setJenkinsJobName(build.getProject().getName());
			
			UpdateManager updateManager = new UpdateManager();
			updateManager.saveEnvironmentContainerBuild(environmentContainerBuild);
			return environmentContainerBuild;
		}
	}
	
	private void recordBuildEnd(Object buildRecord, boolean isEnvironmentBuild, boolean succeeded, String log, String report) throws Exception {
		if (isEnvironmentBuild) {
			EnvironmentBuild environmentBuild = (EnvironmentBuild) buildRecord;
			environmentBuild.setSucceeded(succeeded);
			environmentBuild.setLog(log);
			environmentBuild.setReport(report);
			environmentBuild.setDateCompleted(new Date());
			new UpdateManager().updateEnvironmentBuild(environmentBuild);
		} else {
			EnvironmentContainerBuild environmentContainerBuild = (EnvironmentContainerBuild) buildRecord;
			environmentContainerBuild.setSucceeded(succeeded);
			environmentContainerBuild.setLog(log);
			environmentContainerBuild.setReport(report);
			environmentContainerBuild.setDateCompleted(new Date());
			
			new UpdateManager().updateEnvironmentContainerBuild(environmentContainerBuild);
		}
	}
	
	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link EBSAReleasePlugin}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    	    	
    	/**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }
   
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Environment Build Tool";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return super.configure(req,formData);
        }
    }
}

