package com.ipt.ebsa.buildtools.release.manager;


public class GlobalConfig {

	private static ConnectionData sharedConnectionData = null;
	
	public static void setSharedConnectionData(ConnectionData connectionData) {
		sharedConnectionData = connectionData;
	}
	
	public static ConnectionData getConfig() {
		return sharedConnectionData;
	}

	public static final class ConnectionD  implements ConnectionData {

        private String driverClass;
        private String url;
        private String username;
        private String password;
        private String schema;
        private String autodll;
        private String dialect;
        
        
        
		public ConnectionD(String driverClass, String url, String username, String password, String schema, String autodll, String dialect) {
			super();
			this.driverClass = driverClass;
			this.url = url;
			this.username = username;
			this.password = password;
			this.schema = schema;
			this.autodll = autodll;
			this.dialect = dialect;
		}
		public String getDriverClass() {
			return driverClass;
		}
		public void setDriverClass(String driverClass) {
			this.driverClass = driverClass;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public String getSchema() {
			return schema;
		}
		public void setSchema(String schema) {
			this.schema = schema;
		}
		public String getAutodll() {
			return autodll;
		}
		public void setAutodll(String autodll) {
			this.autodll = autodll;
		}
		public String getDialect() {
			return dialect;
		}
		public void setDialect(String dialect) {
			this.dialect = dialect;
		}      
	}
}
