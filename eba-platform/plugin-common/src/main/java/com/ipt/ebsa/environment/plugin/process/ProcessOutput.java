package com.ipt.ebsa.environment.plugin.process;

import net.sf.json.JSONObject;

/**
 * For complex return type from an external process
 * @author Jedward
 */
public class ProcessOutput {
	private String stdout = "";
	private String stderr = "";
	/**
	 * Contains other output from the process.
	 */
	private String auxiliaryOutput = "";
	private int returnCode = -1;
	
	public ProcessOutput() {
	}

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public String getStdout() {
		return stdout;
	}

	public void setStdout(String stdout) {
		this.stdout = stdout;
	}

	public String getStderr() {
		return stderr;
	}

	public void setStderr(String stderr) {
		this.stderr = stderr;
	}

	public String getAuxiliaryOutput() {
		return auxiliaryOutput;
	}

	public void setAuxiliaryOutput(String auxiliaryOutput) {
		this.auxiliaryOutput = auxiliaryOutput;
	}

	/**
	 * Corresponding javascript on the client side:
	 * var report = json['preparationSummaryReport'];
	 * var rc = json['returnCode'];
	 * var out = json['out'];
	 * var err = json['err'];
	 * @return
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("preparationSummaryReport", getAuxiliaryOutput());
		json.put("out", getStdout());
		json.put("err", getStderr());
		json.put("returnCode", getReturnCode());
		return json;
	}
}
