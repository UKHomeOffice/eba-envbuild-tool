package com.ipt.ebsa.puppet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum PuppetError {
	LOCKED(101, "A puppet run is currently in progress and will need to finish before SS2 can continue.", ".*agent_catalog_run.lock exists.*", true),
	RPM_FAIL_FILE_EXISTS(102, "An RPM fail file exists on the environment being deployed to. This will need to be removed.", ".*Failed RPM File exists.*", false),
	UNKNOWN(199, "Puppet error. Please check Puppet logs.", ".*", false);

	private final int code;
	private final String description;
	private final String regex;
	private final boolean canRetry;

	private PuppetError(int code, String description, String regex, boolean canRetry) {
		this.code = code;
		this.description = description;
		this.regex = regex;
		this.canRetry = canRetry;
	}

	public String getDescription() {
		return description;
	}

	public int getCode() {
		return code;
	}
	
	public String getRegex() {
		return regex;
	}
	
	public boolean getCanRetry(){
		return canRetry;
	}

	@Override
	public String toString() {
		return code + ": " + description;
	}
	
	public static PuppetError getPuppetErrorForLog(String log){
		PuppetError[] errors = PuppetError.values();
		for (PuppetError pe : errors) {
			Pattern p = Pattern.compile(pe.getRegex(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
			Matcher m = p.matcher(log);
			if (m.matches()){
				return pe;
			}
		}
		return null;
	}
}