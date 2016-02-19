package com.ipt.ebsa.database.manager;

/**
 * Allows me to pass data into the ConnectionManager in a nice way
 * @author scowx
 *
 */
public interface ConnectionData {
	
	public String getDriverClass(); 
	public String getUrl();
	public String getUsername();
	public String getPassword();
	public String getSchema();
	public String getAutodll();
	public String getDialect();
      
}
