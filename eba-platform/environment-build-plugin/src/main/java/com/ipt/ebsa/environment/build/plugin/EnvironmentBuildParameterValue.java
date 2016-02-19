package com.ipt.ebsa.environment.build.plugin;

import hudson.model.ParameterValue;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

/**
 * Mechanism for making data available for the 'plugin' class {@link EnvironmentBuildPlugin}.
 * 
 * @author James Shepherd
 */
public class EnvironmentBuildParameterValue extends ParameterValue {

    private static final long serialVersionUID = 1L;

    public static final String ENVIRONMENT_BUILD_PARAMETER = EnvironmentBuildParameterValue.class.getName();

    @Exported(visibility=4)
	private String workDir;

    @Exported(visibility=4)
	private String environment;
    
    @Exported(visibility=4)
    private String mode;
    
    @Exported(visibility=4)
    private String version;

    @Exported(visibility=4)
	private String container;
    
    @Exported(visibility=4)
	private String provider;
    
    private String userParameters;
    
	/**
	 * Accepts the xmlDir from the HTML form parameter.
	 */
	@DataBoundConstructor
    public EnvironmentBuildParameterValue(String workDir, String environment, String container, String version, String provider) {
    	super(EnvironmentBuildParameterValue.ENVIRONMENT_BUILD_PARAMETER, "Environment build details which have been selected by the user.");
		this.workDir = workDir;
		String[] split;
		if (container.matches(".*\\|.*")) {
			split = container.split("\\|");
			this.container = split[0];
		} else if (environment.matches(".*\\|.*")) {
			split = environment.split("\\|");
			this.environment = split[0];
		} else {
			throw new IllegalArgumentException("No environment or container parameter of the form [name|mode]");
		}
		this.mode = split[1];
		this.version = version;
		this.provider = provider;
    }

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((workDir == null) ? 0 : workDir.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		EnvironmentBuildParameterValue other = (EnvironmentBuildParameterValue) obj;
		if (workDir == null) {
			if (other.workDir != null)
				return false;
		}
		return true;
	}

	@Override
	public String getShortDescription() {
        return "Contains the selected command";
    }

	public String getEnvironment() {
		return environment;
	}
	
	public String getMode() {
		return mode;
	}

	public String getVersion() {
		return version;
	}

	public String getContainer() {
		return container;
	}
	
	public String getProvider() {
		return provider;
	}

	public String getWorkDir() {
		return workDir;
	}

	public String getUserParameters() {
		return userParameters;
	}

	public void setUserParameters(String userParameters) {
		this.userParameters = userParameters;
	}
}
