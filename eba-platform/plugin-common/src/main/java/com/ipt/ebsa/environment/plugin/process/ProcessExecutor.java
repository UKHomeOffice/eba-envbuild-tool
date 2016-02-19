package com.ipt.ebsa.environment.plugin.process;

import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class contains most of the logic and all of the constants associated with executing EmvironmentManagementCLI processes.
 * @author scowx
 *
 */
public class ProcessExecutor {

	// inside jenkins use java.util logging
	private static final Logger LOG = Logger.getLogger(ProcessExecutor.class.getName());
	
	/**
	 * This is for running an external program from a builder, possibly on a slave.
	 * @param build
	 * @param launcher
	 * @param listener
	 * @param args
	 * @return
	 */
	public ProcessOutput runProgram(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, String[] args) {
		StringLogOutputStream stdout = new StringLogOutputStream(listener.getLogger());
		StringLogOutputStream stderr = new StringLogOutputStream(listener.getLogger());

		ArgumentListBuilder command = new ArgumentListBuilder();

		for (String arg : args) {
			command.add(arg);
		}

		ProcessOutput processOutput = new ProcessOutput();
		ProcStarter ps = launcher.new ProcStarter();

		try {
			ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener)).cmds(command).stdout(stdout).stderr(stderr);
			Proc proc = launcher.launch(ps);
			processOutput.setReturnCode(proc.join());
		} catch (IOException e) {
			e.printStackTrace(listener.getLogger());
		} catch (InterruptedException e) {
			e.printStackTrace(listener.getLogger());
		}

		stdout.flush();
		stderr.flush();
		processOutput.setStderr(stderr.getOutput());
		processOutput.setStdout(stdout.getOutput());
		return processOutput;
	}
	
	/**
	 * Do not use this for running a program on a slave, use {@link #runProgram(AbstractBuild, Launcher, BuildListener, String[]))}
	 * for that. This is for running a program on the master only, and not as part of a build step.
	 * @param args
	 * @param workFolder
	 * @param env environment variable to set
	 * @return
	 */
	public ProcessOutput runProgram(String[] args, String workFolder, Map<String, String> env) {
		StringBuffer out = new StringBuffer();
		StringBuffer err = new StringBuffer();
		Process p = null;
		ProcessOutput procOutput = new ProcessOutput();
		
		try {
			LOG.info("External Command: " + Arrays.asList(args).toString());
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.directory(new File(workFolder));
			
			for(Map.Entry<String, String> me : env.entrySet()) {
				LOG.info(String.format("Environment variable [%s] = [%s]", me.getKey(), me.getValue()));
				pb.environment().put(me.getKey(), me.getValue());
			}
			
			p = pb.start();
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), err, null);
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), out, null);
			errorGobbler.start();
			outputGobbler.start();
			int i = p.waitFor();
			
			procOutput.setReturnCode(i);
		} catch (Throwable e) {
			err.append("Exception occurred: " + e);
			err.append(getAsString(e));
		} finally {
			procOutput.setStdout(out.toString());
			procOutput.setStderr(err.toString());
		}
		
		return procOutput;
	}
	
	/**
	 * Do not use this for running a program on a slave, use {@link #runProgram(AbstractBuild, Launcher, BuildListener, String[]))}
	 * for that. This is for running a program on the master only, and not as part of a build step.
	 * @param args
	 * @param workFolder
	 * @return
	 */
	public ProcessOutput runProgram(String[] args, String workFolder) {
		return runProgram(args, workFolder, Collections.<String, String>emptyMap());
	}

	/**
	 * Pops an exception and trace into a string
	 * @param e
	 * @return
	 */
	private String getAsString(Throwable e) {
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		String s = errors.toString();
		return s;
	}
}
