package com.ipt.ebsa.environment;

public interface EnvironmentUpdate {

	public boolean doUpdate(String environmentName, String baseCommitString);

	public boolean changeMade();

	public Object getRequestedValue();

	public void setRequestedValue(Object requestedValue);

	public Object getExistingValue();

	public void setExistingValue(Object existingValue);

	public MachineState getSource();

	public void setSource(MachineState newSource);
	
	public String getComponentName();

	public String getApplicationName();
	
	public String getZoneName();

}