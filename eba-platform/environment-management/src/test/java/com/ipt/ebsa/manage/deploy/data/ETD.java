package com.ipt.ebsa.manage.deploy.data;

import java.io.File;

import org.apache.commons.lang.StringUtils;


/**
 * ETD stands for Expected Transition Data
 * @author scowx
 */
public class ETD {

	public boolean changeMade;
	public String requestedPath;
	public Object requestedValue;
	public String existingPath;
	public String existingValue;
	public String filePath;
	public int waitDuration;
	public boolean stop;
	public String stopMessage;
	
	/**
	 * 
	 * @param changeMade is there a change made in this component transition?
	 * @param requestedValue what version is requested to be installed?
	 * @param requestedPath what is the path in the YAML of the item to be installed (e.g. system::packages/name-of-component/ensure)
	 * @param existingValue what version is this component at?
	 * @param filePath the path to the Hiera file to look for
	 */
	public ETD(boolean changeMade, Object requestedValue, String requestedPath, String existingValue, String filePath) {
		this(changeMade, requestedValue, requestedPath, existingValue, null, filePath);
	}
	
	/**
	 * Useful for asserting YAML removals as the existing path (to be removed from) can be specified
	 * 
	 * @param changeMade is there a change made in this component transition?
	 * @param requestedValue what version is requested to be installed?
	 * @param requestedPath what is the path in the YAML of the item to be installed (e.g. system::packages/name-of-component/ensure)
	 * @param existingValue what version is this component at?
	 * @param existingPath what is the current path? 
	 * @param filePath the path to the Hiera file to look for
	 */
	public ETD(boolean changeMade, Object requestedValue, String requestedPath, String existingValue, String existingPath, String filePath) {
		super();
		this.changeMade = changeMade;
		this.requestedPath = requestedPath;
		this.requestedValue = requestedValue;
		this.existingValue = existingValue;
		this.existingPath = existingPath;
		// Replace "/" in the file path with the current OS file separator so comparisons can be made with file paths when the tests are run on Windows
		this.filePath = StringUtils.replace(filePath, "/", File.separator);
	}
	
	public ETD(int waitDuration) {
		super();
		this.waitDuration = waitDuration;
	}
	
	public ETD(boolean stop, String stopMessage) {
		super();
		this.stop = stop;
		this.stopMessage = stopMessage;
	}
	
}
