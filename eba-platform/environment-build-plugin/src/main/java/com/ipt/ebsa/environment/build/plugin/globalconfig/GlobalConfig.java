package com.ipt.ebsa.environment.build.plugin.globalconfig;
import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Descriptor;
import hudson.model.Job;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import com.ipt.ebsa.database.manager.ConnectionData;


/**
 * Global config descriptor for Release plugins. Used as a singleton.
 * 
 */
public class GlobalConfig extends JobProperty<Job<?, ?>>  {
	
	@Override
    public EnvironmentBuildPluginGlobalConfigDescriptor getDescriptor() {
        return (EnvironmentBuildPluginGlobalConfigDescriptor)Jenkins.getInstance().getDescriptor(getClass());
    }

    public static Descriptor<?> getEnvironmentBuildPluginConfigDescriptor() {
        return (EnvironmentBuildPluginGlobalConfigDescriptor)Jenkins.getInstance().getDescriptor(GlobalConfig.class);
    }


    @Extension
    public static final class EnvironmentBuildPluginGlobalConfigDescriptor extends JobPropertyDescriptor implements ConnectionData {

        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String environmentBuildExecutable;
        private String environmentBuildConfigFile;
        private String driverClass;
        private String url;
        private String username;
        private String password;
        private String schema;
        private String autodll;
        private String dialect;
        private boolean ldapFilteringEnabled;
        
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor. This object is constructed when the app starts up
         * so this hack forces the config to be loaded and set globally.
         */
        public EnvironmentBuildPluginGlobalConfigDescriptor() {
            load();
            com.ipt.ebsa.database.manager.GlobalConfig.getInstance().setSharedConnectionData((com.ipt.ebsa.database.manager.ConnectionData)this);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "EBSA Environment Build Global Configuration";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            com.ipt.ebsa.database.manager.GlobalConfig.getInstance().setSharedConnectionData((com.ipt.ebsa.database.manager.ConnectionData)this);
            return super.configure(req,formData);
        }

		public String getEnvironmentBuildExecutable() {
			return environmentBuildExecutable;
		}

		public void setEnvironmentBuildExecutable(String environmentBuildExecutable) {
			this.environmentBuildExecutable = environmentBuildExecutable;
		}

		public String getEnvironmentBuildConfigFile() {
			return environmentBuildConfigFile;
		}

		public void setEnvironmentBuildConfigFile(String environmentBuildConfigFile) {
			this.environmentBuildConfigFile = environmentBuildConfigFile;
		}

		public String getDriverClass() {
			return driverClass;
		}

		public String getUrl() {
			return url;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

		public String getSchema() {
			return schema;
		}

		public String getAutodll() {
			return autodll;
		}

		public String getDialect() {
			return dialect;
		}

		public void setDriverClass(String driverClass) {
			this.driverClass = driverClass;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public void setAutodll(String autodll) {
			this.autodll = autodll;
		}

		public void setDialect(String dialect) {
			this.dialect = dialect;
		}

		public boolean isLdapFilteringEnabled() {
			return ldapFilteringEnabled;
		}

		public void setLdapFilteringEnabled(boolean ldapFilteringEnabled) {
			this.ldapFilteringEnabled = ldapFilteringEnabled;
		}
    }	
}

