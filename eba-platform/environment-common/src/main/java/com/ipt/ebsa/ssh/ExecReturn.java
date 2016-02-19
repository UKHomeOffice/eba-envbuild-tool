package com.ipt.ebsa.ssh;

/**
 * @author James Shepherd
 */
public class ExecReturn {
	private int returnCode = 1;
	private String stdOut = "";
	
	public ExecReturn(){}
	
	public ExecReturn(int theReturnCode){
		this.returnCode = theReturnCode;
	}
	
	public int getReturnCode() {
		return returnCode;
	}
	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
	public String getStdOut() {
		return stdOut;
	}
	public void setStdOut(String stdout) {
		this.stdOut = stdout;
	} 
}
