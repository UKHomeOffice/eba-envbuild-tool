package com.ipt.ebsa.template;

import java.io.File;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

/**
 * To use
 * <ol>
 * <li>instantiate by passing in the path to the root of the dirs containing
 * the templates
 * <li>use {@link #put(String, Object)} to set template parameters
 * <li>call {@link #render(String)} with the name of the template to render the template
 * </ol>
 * 
 * If you want use the same context to render another template, then just call
 * {@link #render(String)} again. If not, then call {@link #resetContext()} to clear
 * out the context.
 *  
 * @author James Shepherd
 */
public class TemplateManager {

	private static final Logger LOG = Logger.getLogger(TemplateManager.class);

	private VelocityEngine velocityEngine = new VelocityEngine();
	private VelocityContext velocityContext;

	/**
	 * @param templateDir
	 */
	public TemplateManager(File templateDir) {
		this(templateDir.getAbsolutePath());
	}
	
	/**
	 * @param pathToTemplateDir
	 */
	public TemplateManager(String pathToTemplateDir) {
		// setup logging
		velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.Log4JLogChute");
		velocityEngine.setProperty("runtime.log.logsystem.log4j.logger", LOG.getName());
		
		// get templates from the configured dir
		LOG.info(String.format("Using templates from [%s]", pathToTemplateDir));
		velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, pathToTemplateDir);
		// check for new templates, useful during dev, and I don't think will have an impact in prod
		velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "false");
		velocityEngine.setProperty("file.resource.loader.modificationCheckInterval", "2"); // seconds
		
		// strict mode - for when placeholders are not set
		velocityEngine.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, "true");
		
		velocityEngine.init();
		
		resetContext();
	}
	
	public void resetContext() {
		velocityContext = new VelocityContext();
	}
	
	public void put(String key, Object value) {
		velocityContext.put(key, value);
	}
	
	public boolean templateExists(String relativePathToTemplate) {
		boolean output = velocityEngine.resourceExists(relativePathToTemplate);
		LOG.debug(String.format("template [%s] exists [%s]", relativePathToTemplate, output));
		return output;
	}
	
	/**
	 * @param relativePathToTemplate
	 * @return rendered template, or null if template not found
	 */
	public String render(String relativePathToTemplate) {
		StringWriter sw = new StringWriter();
		LOG.debug(String.format("Loading template [%s]", relativePathToTemplate));
		Template t = velocityEngine.getTemplate(relativePathToTemplate);
		LOG.debug(String.format("Rendering template [%s]", relativePathToTemplate));
		t.merge(velocityContext, sw);
		LOG.debug(String.format("Template [%s] rendered", relativePathToTemplate));
		return sw.toString();
	}
}
