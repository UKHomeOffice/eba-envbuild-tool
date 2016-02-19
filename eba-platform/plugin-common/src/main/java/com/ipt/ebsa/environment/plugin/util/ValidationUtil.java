package com.ipt.ebsa.environment.plugin.util;


import hudson.util.FormValidation;

import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.ipt.ebsa.util.collection.ParamFactory;

/**
 * Validation utility class
 *
 */
public class ValidationUtil {
	
    /**
     * Pattern for capturing variables. Either $xyz, ${xyz} or ${a.b} but not $a.b, while ignoring "$$"
     * @see hudson.Util.java
     */
    private static final Pattern VARIABLE = Pattern.compile("\\$([A-Za-z0-9_]+|\\{[A-Za-z0-9_.]+\\}|\\$)");
	
	/**
	 * Returns a FormValidation.error if the field with the given name has a blank value, otherwise FormValidation.ok()
	 * @param name
	 * @param value
	 * @return
	 */
	public static FormValidation validateMandatory(String name, String value) {
		return StringUtils.isBlank(value) ? FormValidation.error(name + " is mandatory") : FormValidation.ok();
	}
	
	/**
	 * Returns a FormValidation.error if the value of the field with the given name does NOT match the regex, otherwise FormValidation.ok()
	 * @param name
	 * @param value
	 * @param regex
	 * @return
	 */
	public static FormValidation validateWithRegex(String name, String value, String regex) {
		return StringUtils.defaultString(value).matches(StringUtils.defaultString(regex)) ? FormValidation.ok() : FormValidation.error(name + " is not in the correct format");
	}
	
	/**
	 * Returns a FormValidation.error() listing all the params that have a blank value or were unresolved parameters, otherwise FormValidation.ok()
	 * @param params
	 * @return
	 */
	public static FormValidation validateMandatory(ParamFactory params) {
		FormValidation formValidation = FormValidation.ok();
		StringBuffer blank = new StringBuffer();
		StringBuffer unresolved = new StringBuffer();
		if (params != null) {
			for (Entry<String, Object> entry : params.parameters().entrySet()) {
				String value = ObjectUtils.toString(entry.getValue());
				if (StringUtils.isBlank(value)) {
					blank.append(entry.getKey());
					blank.append(", ");
				} else if (VARIABLE.matcher(value).matches()) {
					unresolved.append(entry.getKey());
					unresolved.append(", ");
				}
			}
			String error = "";
			if (blank.length() > 0) {
				blank.setLength(blank.length() - 2); // Remove the trailing comma and space
				error = "Missing mandatory value(s) for: " + blank.toString() + ". ";
			}
			if (unresolved.length() > 0) {
				unresolved.setLength(unresolved.length() - 2); // Remove the trailing comma and space
				error += "Unresolved mandatory value(s) for: " + unresolved.toString() + ".";
			}
			if (error.length() > 0) {
				formValidation = FormValidation.error(error);
			}
		}
		return formValidation;
	}

}
