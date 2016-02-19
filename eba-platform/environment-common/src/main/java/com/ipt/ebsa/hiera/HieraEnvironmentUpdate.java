package com.ipt.ebsa.hiera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.EnvironmentUpdate;
import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.yaml.YamlInjector;
import com.ipt.ebsa.yaml.YamlUtil;

public class HieraEnvironmentUpdate implements EnvironmentUpdate {
	private static final Logger LOG = Logger.getLogger(HieraEnvironmentUpdate.class);
	private Object existingValue;
	private String existingPath;
	private String requestedPath;
	private Object requestedValue;
	private String pathElementsAdded = "";
	private String pathElementsRemoved = "";
    private MachineState source;
	private NodeMissingBehaviour nodeMissingBehaviour;
	
	// Additional fields required for SS3 to identify the context of this update
	private String componentName; // Could be null if this update is not related to a component
	private final String applicationName;
	private final String zoneName;
	
	public HieraEnvironmentUpdate(String applicationName, String zoneName) {
		super();
		this.applicationName = applicationName;
		this.zoneName = zoneName;
	}

	@Override
	public String toString() {
		return "EnvironmentUpdateResult [roleOrFqdn=" + source.getRoleOrFQDN() + ", existingValue=" + existingValue + ", existingPath=" + existingPath + ", requestedPath=" + requestedPath + ", requestedValue=" + requestedValue
				+ ", pathElementsAdded=" + pathElementsAdded + ", pathElementsRemoved=" + pathElementsRemoved + "]";
	}
	
	/* (non-Javadoc)
	 * @see com.ipt.ebsa.environment.EnvironmentUpdate#changeMade()
	 */
	@Override
	public boolean changeMade() {
		if (getPathElementsAdded() != null ) {
			return true;
		}
		if (getPathElementsRemoved() != null) {
			return true;
		}
		if (existingValue == null && requestedValue == null) {
			return false;
		}
		else { 
			if (existingValue == null) {
				return true;	
			}
			else {
				return existingValue.equals(requestedValue);
			}
		}
	}
	
	
	public String getExistingPath() {
		return existingPath;
	}
	public void setExistingPath(String existingPath) {
		this.existingPath = existingPath;
	}
	public String getRequestedPath() {
		return requestedPath;
	}
	public void setRequestedPath(String requestedPath) {
		this.requestedPath = requestedPath;
	}
	public String getPathElementsAdded() {
		return pathElementsAdded;
	}
	public void setPathElementsAdded(String pathElementsAdded) {
		this.pathElementsAdded = pathElementsAdded;
	}
	/* (non-Javadoc)
	 * @see com.ipt.ebsa.environment.EnvironmentUpdate#getRequestedValue()
	 */
	@Override
	public Object getRequestedValue() {
		return requestedValue;
	}
	/* (non-Javadoc)
	 * @see com.ipt.ebsa.environment.EnvironmentUpdate#setRequestedValue(java.lang.Object)
	 */
	@Override
	public void setRequestedValue(Object requestedValue) {
		this.requestedValue = requestedValue;
	}
	/* (non-Javadoc)
	 * @see com.ipt.ebsa.environment.EnvironmentUpdate#getExistingValue()
	 */
	@Override
	public Object getExistingValue() {
		return existingValue;
	}
	/* (non-Javadoc)
	 * @see com.ipt.ebsa.environment.EnvironmentUpdate#setExistingValue(java.lang.Object)
	 */
	@Override
	public void setExistingValue(Object existingValue) {
		this.existingValue = existingValue;
	}

	public String getPathElementsRemoved() {
		return pathElementsRemoved;
	}

	public void setPathElementsRemoved(String pathElementsRemoved) {
		this.pathElementsRemoved = pathElementsRemoved;
	}

	/* (non-Javadoc)
	 * @see com.ipt.ebsa.environment.EnvironmentUpdate#getSource()
	 */
	@Override
	public MachineState getSource() {
		return source;
	}

	/* (non-Javadoc)
	 * @see com.ipt.ebsa.environment.EnvironmentUpdate#setSource(com.ipt.ebsa.environment.MachineState)
	 */
	@Override
	public void setSource(MachineState newSource) {
		this.source = newSource;
	}

	public NodeMissingBehaviour getNodeMissingBehaviour() {
		return nodeMissingBehaviour;
	}

	public void setNodeMissingBehaviour(NodeMissingBehaviour behaviour) {
		this.nodeMissingBehaviour = behaviour;
	}

	@Override
	public String getComponentName() {
		return componentName;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	@Override
	public String getApplicationName() {
		return applicationName;
	}
	
	@Override
	public boolean doUpdate(String environmentName, String baseCommitString) {
		try {
			this.doAllUpdatesForFile(environmentName, baseCommitString);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public String getZoneName() {
		return zoneName;
	}

	/**
	 * Does a YAML update for a single YAML file 
	 * @param environmentName
	 * @param baseCommitString
	 * @param update
	 * @throws FileNotFoundException
	 * @throws Exception
	 * @throws IOException
	 */
	private void doAllUpdatesForFile(String environmentName, String baseCommitString) throws FileNotFoundException, Exception, IOException {
		HieraMachineState source = (HieraMachineState)this.getSource();
		String hieraFile = source.getFile().getName();
		Map<String, Object> yaml = YamlUtil.readYaml(source.getFile());
		new YamlInjector().apply(yaml, this);

		FileWriter writer = null;
		try {
			File f = source.getFile();
			if (!f.exists()) {
				LOG.info(String.format("Creating hiera file on disk: %s", hieraFile));
				if (f.getParentFile().mkdirs()) {
					LOG.info(String.format("Created directories for hiera file on disk: %s", f.getParentFile().getAbsolutePath()));
				}
				if (f.createNewFile()) {
					LOG.info(String.format("Created hiera file on disk: %s", f.getAbsolutePath()));	
				}
			}
			writer = new FileWriter(source.getFile());
			YamlUtil.write(yaml, writer);
			LOG.info(String.format("Written '%s' in YAML file %s for %s", this.getRequestedPath(), hieraFile, baseCommitString));
		} catch (IOException e) {
			throw new RuntimeException(String.format("Unable to write YAML files to physical disk, fatal for %s", baseCommitString), e);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
}
