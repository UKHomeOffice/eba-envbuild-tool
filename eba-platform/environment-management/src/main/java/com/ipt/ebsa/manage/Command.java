package com.ipt.ebsa.manage;

public interface Command {
    public void execute() throws Exception;
	
	public void cleanUp();
}
