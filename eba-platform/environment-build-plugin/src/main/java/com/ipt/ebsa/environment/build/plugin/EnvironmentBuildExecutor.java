package com.ipt.ebsa.environment.build.plugin;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.ipt.ebsa.environment.build.plugin.globalconfig.GlobalConfig;
import com.ipt.ebsa.environment.build.plugin.globalconfig.GlobalConfig.EnvironmentBuildPluginGlobalConfigDescriptor;
import com.ipt.ebsa.environment.plugin.process.ProcessExecutor;
import com.ipt.ebsa.environment.plugin.process.ProcessOutput;

public class EnvironmentBuildExecutor {

	public static final String CONFIG_EQUALS = "-config=";
	public static final String COMMAND_EQUALS = "-command=";
	
	private String configFile;
	private String executable;
	private String command;
	private ProcessExecutor processExecutor = new ProcessExecutor();

	public EnvironmentBuildExecutor(String command) throws Exception {
		this.command = command;
		getGlobalConfig();
	}
	
	/**
	 * Returns the standard items which can be set in the global config.  The executable and the config.
	 * @return
	 * @throws Exception
	 */
	private void getGlobalConfig() throws Exception {
		EnvironmentBuildPluginGlobalConfigDescriptor gc = (EnvironmentBuildPluginGlobalConfigDescriptor) GlobalConfig.getEnvironmentBuildPluginConfigDescriptor();
		configFile = gc.getEnvironmentBuildConfigFile();
		if (configFile == null) {
			throw new Exception("Environment build configFile has not been set up properly as it is null.  Check the Jenkins global configuration for the EBSA Environment Build plugin to ensure that this value has been properly set."); 
		}
		executable = gc.getEnvironmentBuildExecutable();
		if (executable == null) {
			throw new Exception("Environment Build executable has not been set up properly as it is null.  Check the Jenkins global configuration for the EBSA Environment Build plugin to ensure that this value has been properly set."); 
		}
	}
	

	/**
	 * Returns a standard command for executing the EnvironmentManagement tool.  Other arguments can be added to the list afterwards
	 * @param applicationVersionId
	 * @param environment
	 * @param orgPrefix
	 * @param workFolder
	 * @return
	 * @throws Exception
	 */
	private List<String> getStandardExecutionCommand() {
		return Arrays.asList(new String[]{
			executable,
			CONFIG_EQUALS + configFile,
			COMMAND_EQUALS + command,
		});
	}

	/**
	 * This is for running an external program from a builder, possibly on a slave.
	 * @param build
	 * @param launcher
	 * @param listener
	 * @param extraArgs extra to {@link #getStandardExecutionCommand()}
	 * @return output
	 */
	public ProcessOutput runEnvironmentBuildStep(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, Collection<String> extraArgs) {
		ArrayList<String> allArgsList = new ArrayList<String>(getStandardExecutionCommand());
		allArgsList.addAll(extraArgs);
		return processExecutor.runProgram(build, launcher, listener, allArgsList.toArray(new String[allArgsList.size()]));
	}
	
	
	public ProcessOutput runCommand(Collection<String> extraArgs, String workDir) {
		ArrayList<String> allArgsList = new ArrayList<String>(getStandardExecutionCommand());
		allArgsList.addAll(extraArgs);
		return processExecutor.runProgram(allArgsList.toArray(new String[allArgsList.size()]), workDir);
	}
}
