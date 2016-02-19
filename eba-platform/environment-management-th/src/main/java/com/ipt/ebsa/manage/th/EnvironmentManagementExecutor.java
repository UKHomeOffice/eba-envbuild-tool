package com.ipt.ebsa.manage.th;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class contains most of the logic and all of the constans associated with executing EmvironmentManagementCLI processes.
 * @author scowx
 *
 */
public class EnvironmentManagementExecutor {

	public static final String GIT_LOCAL_CHECKOUT_DIR 											= "git.local.checkout.dir";
	public static final String GIT_REMOTE_CHECKOUT_ENVIRONMENT_DEPLOYMENT_DESCRIPTORS_REPO_URL	= "git.remote.checkout.environment.deployment.descriptors.repo.url";
	public static final String GIT_REMOTE_CHECKOUT_HIERA_REPO_URL 								= "git.remote.checkout.hiera.repo.url";
	public static final String DEPLOYMENT_CONFIG_WORK_FOLDER 									= "deployment.config.workFolder";
	public static final String DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_WRAP_IN_TEMPLATE 	= "deployment.config.preparationSummaryReportWrapInTemplate";
	public static final String DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FILENAME 			= "deployment.config.preparationSummaryReportFilename";
	public static final String DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER 				= "deployment.config.preparationSummaryReportFolder";
	public static final String DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_USE_UNIQUE_NAME 	= "deployment.config.preparationSummaryReport.useUniqueName";
	public static final String DEPLOYMENT_CONFIG_HIERA_ORGANISATION_PREFIX 						= "deployment.config.hieraOrganisationPrefix";
	public static final String DEPLOYMENT_CONFIG_DONTCHECKOUTHIERAFOLDER 						= "deployment.config.dontcheckouthierafolder";
	public static final String DEPLOYMENT_CONFIG_DONTCHECKOUTDDFOLDER 							= "deployment.config.dontcheckoutddfolder";
	public static final String DEPLOYMENT_CONFIG_PREPARE_ONLY 									= "deployment.config.prepareOnly";
	public static final String CONFIG_EQUALS 													= "-config=";
	public static final String APPLICATION_VERSION_ID_EQUALS 									= "-applicationVersionId=";
	public static final String ENVIRONMENT_NAME_EQUALS 											= "-environmentName=";
	public static final String COMMAND_CHECKOUT 												= "-command=checkout";
	public static final String COMMAND_DEPLOY 													= "-command=deploy";
	
	/** The prefix for the NP organisation. */
	private static final String NP_ORG_PREFIX = "ho_ipt_";
	/** The prefix for the ST organisation. */
	private static final String ST_ORG_PREFIX = "ipt_";
	
	/**
	 * Returns a standard command for executing the EnvironmentManagement tool.  Other arguments can be added to the list afterwards
	 * @param applicationVersionId
	 * @param environment
	 * @param orgPrefix
	 * @param workFolder
	 * @return
	 * @throws Exception
	 */
	public List<String> getStandardExecutionCommand(String executable, String configFile, String applicationVersionId, String environment) throws Exception {
		String environmentPlusOrg = getOrgAndEnvironmentName(environment);
		
		List<String> list = new ArrayList<String>();
		list.add(executable);
		list.add(EnvironmentManagementExecutor.COMMAND_DEPLOY);
		list.add(EnvironmentManagementExecutor.CONFIG_EQUALS + configFile);
		list.add(EnvironmentManagementExecutor.ENVIRONMENT_NAME_EQUALS + environmentPlusOrg);
		list.add(EnvironmentManagementExecutor.APPLICATION_VERSION_ID_EQUALS + applicationVersionId);
		list.add(EnvironmentManagementExecutor.DEPLOYMENT_CONFIG_PREPARE_ONLY + "=true");
		
		return list;
	}
	
	/**
	 * Generic method which executes an external process and provides the output as keys in a JSON string
	 * @param map
	 * @param args
	 */
	public int runProgram(Map<String,String> map, String[] args, String workFolder) {
		StringBuffer out = new StringBuffer();
		StringBuffer err = new StringBuffer();
		Process p = null;
		
		try {
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.directory(new File(workFolder));
			
			p = pb.start();
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), err);
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), out);
			errorGobbler.start();
			outputGobbler.start();
			int i = p.waitFor();
			
			return i;
			
		} catch (Throwable e) {
			e.printStackTrace();
			String s = getAsString(e);
			map.put("exc", s);
		    return 2;
		}
		finally {
			
			map.put("out", out.toString());
			map.put("err", err.toString());
		}
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

	/**
	 * The environment name from the plugin will come in as "st-something-something" or "np-something-something".
	 * 
	 * The environment names are actually "IPT_ST_SOMETHING_SOMETHING" or "HO_IPT_SOMETHING_SOMETHING".
	 * 
	 * This will fix that name.
	 * 
	 * @param environment
	 * @return the environment name and the organisation.
	 */
	private String getOrgAndEnvironmentName(String environment) {
		// First, make sure the environment name includes the organisation.
		if (environment.startsWith("st")) {
			environment = ST_ORG_PREFIX + environment;
		} else if (environment.startsWith("np")) {
			environment = NP_ORG_PREFIX + environment;
		} else {
			throw new RuntimeException("Could not determine which organisation this environment belongs to: " + environment);
		}
		return environment.toUpperCase().replaceAll("-", "_");
	}
}
