package com.ipt.ebsa.agnostic.client.bridge;

import java.util.Map.Entry;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import com.ipt.ebsa.agnostic.client.config.ConfigurationFactory;

/**
 * Bridge to allow the aCloudClient to be called from Java.
 */
public class AgnosticClientBridge {
	
	private Logger logger = LogManager.getLogger(AgnosticClientBridge.class);

	public void execute(BridgeConfig config) throws Exception {
		config.validate();
		Weld weld = null;
		try {
			for (Entry<Object, Object> entry : config.getProperties().entrySet()) {
				ConfigurationFactory.getProperties().setProperty((String) entry.getKey(), (String) entry.getValue());
			}
			/* Instantiate Weld */
			weld = new Weld();
			WeldContainer container = weld.initialize();
			DirectController controller = container.instance().select(DirectController.class).get();
			controller.setConfig(config);
			controller.execute();
		} catch (Exception e) {
			logger.error("Error executing AgnosticClientBridge", e);
			throw e;
		} finally {
			if (weld != null) {
				weld.shutdown();
			}
		}
	}
}
