package com.ipt.ebsa.ssh.testhelper;

import java.util.Stack;

public class CommandRegister {
	
	Stack<String> commandStack = new Stack<String>();
	
	public CommandRegister() {
		
	}
	
	public void reset() {
		commandStack.clear();
	}
	
	public String getLastCommand() {
		return commandStack.pop().toString();
	}
	
	public void insertCommand(String obj) {
		commandStack.push((String) obj);
	}

}
