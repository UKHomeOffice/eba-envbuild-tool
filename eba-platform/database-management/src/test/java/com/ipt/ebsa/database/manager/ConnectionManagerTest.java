package com.ipt.ebsa.database.manager;

import org.junit.Test;

public class ConnectionManagerTest extends DBTest {
	
	public static ConnectionData d = new ConnectionData() {
		public String getUsername() {
			return "DB_MANAGEMENT";
		}
		public String getUrl() {
			return "jdbc:h2:./target/h2db";
		}
		public String getSchema() {
			return "DB_MANAGEMENT";
		}
		public String getPassword() {
			return "DB_MANAGEMENT";
		}
		public String getDriverClass() {
			return "org.h2.Driver";
		}
		public String getAutodll() {
			return "validate";
		}
		public String getDialect() {
			return "org.hibernate.dialect.H2Dialect";
		}
	};
	
	@Test
	public void testConnectionManager() throws Exception  {
		ConnectionManager c = null;
		try {
    		c = new ConnectionManager();
	    	c.initialiseConnection(System.out, d, "ebsa-database-management-persistence-unit");
	    } catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (c != null) {
				c.closeConnection(System.out);
			}
		}
	}

}
