package com.ipt.ebsa.manage.deploy.database;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;

import com.ipt.ebsa.buildtools.release.manager.ConnectionData;
import com.ipt.ebsa.buildtools.release.manager.ConnectionManager;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.test.TestHelper;

/**
 * Super class for nice tests
 * @author scowx
 *
 */
public abstract class DBTest {
	private EntityManager entityManager;

	private ConnectionManager connectionMnager = new ConnectionManager();

	public EntityManager getEntityManager() {
		return entityManager;
	}

	@Before
	public void connect() throws Exception {
		try {
			TestHelper.setupTestConfig();
			connectionMnager.initialiseConnection(System.out, getConnectioData());
			entityManager = connectionMnager.getManager();

			DBTestUtil.clearDownDB(entityManager);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@After
	public void close() {
		connectionMnager.closeConnection(System.out);
	}

	public static ConnectionData getConnectioData() {
		return new ConnectionData() {
			public String getUsername() {
				return Configuration.getJdbcUsername();
			}
			public String getUrl() {
				return Configuration.getJdbcUrl();
			}
			public String getSchema() {
				return Configuration.getJdbcSchema();
			}
			public String getPassword() {
				return Configuration.getJdbcPassword();
			}
			public String getDriverClass() {
				return Configuration.getJdbcDriver();
			}
			public String getAutodll() {
				return Configuration.getJdbcHbm2ddlAuto();
			}
			public String getDialect() {
				return Configuration.getJdbcDialect();
			}
	    };
	}
}
