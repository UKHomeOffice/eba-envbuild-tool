package com.ipt.ebsa.environment.build.json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

import com.ipt.ebsa.environment.build.entities.EnvironmentContainerDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition;
import com.ipt.ebsa.environment.build.entities.EnvironmentDefinition.DefinitionType;
import com.ipt.ebsa.environment.build.manager.ReadManager;
import com.ipt.ebsa.environment.data.factory.EnvironmentDataFactory;
import com.ipt.ebsa.environment.data.model.EnvironmentData;
import com.ipt.ebsa.environment.data.model.EnvironmentData.AvailableBuildOrContainer;

/**
 * Parses a build plan from XML file(s) and turns it into a single JSON file ready
 * for consumption by the plugin.
 *
 * @author David Manning
 */
public class JsonFileWriter {
	
	private ReadManager readManager;
	
	private static final Logger LOG = Logger.getLogger(JsonFileWriter.class.getName());

	public JsonFileWriter(ReadManager readManager) {
		super();
		this.readManager = readManager;
	}

	public boolean writeToFile(String buildPlanPath, String buildDataPath, String provider) {
		EnvironmentDataFactory factory = new EnvironmentDataFactory();
		EnvironmentData data = factory.getEnvironmentDataInstance(new File(buildPlanPath));
		JsonObjectBuilder obj = Json.createObjectBuilder();
		obj.add("envs", getEnvironmentsJSON(data, provider));
		obj.add("conts", getEnvironmentContainersJSON(data, provider));
		obj.add("returnCode", 0);
		JsonWriter createWriter = null;
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(buildDataPath);
			createWriter = Json.createWriter(out);
			createWriter.writeObject(obj.build());
			LOG.log(Level.FINE, "JSON env/container file [" + buildDataPath + "] written");
			return true;
		} catch (FileNotFoundException e) {
			LOG.log(Level.SEVERE, "Unable to write combined build plan to file", e);
			return false;
		} finally {
			createWriter.close();
			try {
				out.close();
			} catch (IOException e) {
				LOG.warning("Unable to close file reader on [" + buildDataPath + "]. Probably not going to be able to delete this thing");
			}
		}
	}
	
	private JsonArrayBuilder getEnvironmentsJSON(EnvironmentData data, String provider) {
		JsonArrayBuilder builds = Json.createArrayBuilder();
		Map<String, List<EnvironmentDefinition>> environmentDefinitions = getEnvironmentVersions(data, provider);
		
		for (AvailableBuildOrContainer b : data.getAvailableBuilds(provider)) {
			JsonObjectBuilder obj = Json.createObjectBuilder();
			JsonArrayBuilder versions = Json.createArrayBuilder();
			for (EnvironmentDefinition envDef : environmentDefinitions.get(b.getEnvironmentOrContainer())) {
				versions.add(envDef.getVersion());
			}
			LOG.finer("Versions of environment [" + b.getEnvironmentOrContainer() + "] present: " + versions);
			obj.add("versions", versions);
			obj.add("environment", b.getEnvironmentOrContainer());
			obj.add("displayName", b.getDisplayName());
			obj.add("buildId", b.getBuildId());
			JsonArrayBuilder userParams = Json.createArrayBuilder();
			for (Entry<String, String> entry : b.getUserParameters().entrySet()) {
				JsonObjectBuilder param = Json.createObjectBuilder();
				param.add("id", entry.getKey());
				param.add("displayName", entry.getValue());
				userParams.add(param);
			}
			obj.add("userParams", userParams);
			builds.add(obj.build());
		}
		return builds;
	}

	private Map<String, List<EnvironmentDefinition>> getEnvironmentVersions(EnvironmentData data, String provider) {
		ArrayList<String> environments = new ArrayList<>();
		for (AvailableBuildOrContainer b : data.getAvailableBuilds(provider)) {
			environments.add(b.getEnvironmentOrContainer());
		}
		
		Map<String, List<EnvironmentDefinition>> environmentDefinitions = Collections.emptyMap();
		try {
			environmentDefinitions = readManager.getEnvironmentDefinitions(environments, DefinitionType.Physical, provider);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Unable to load versions for environments", e);
		}
		return environmentDefinitions;
	}
	
	private JsonArrayBuilder getEnvironmentContainersJSON(EnvironmentData data, String provider) {
		JsonArrayBuilder builds = Json.createArrayBuilder();
		
		Map<String, List<EnvironmentContainerDefinition>> environmentContainerDefinitions = getEnvironmentContainerDefinitions(data, provider);
		
		for (AvailableBuildOrContainer b : data.getAvailableContainers(provider)) {
			JsonObjectBuilder obj = Json.createObjectBuilder();
			JsonArrayBuilder versions = Json.createArrayBuilder();
			
			for (EnvironmentContainerDefinition envContDefn : environmentContainerDefinitions.get(b.getEnvironmentOrContainer())) {
				versions.add(envContDefn.getVersion());
			}
			obj.add("versions", versions);
			LOG.finer("Versions of container [" + b.getEnvironmentOrContainer() + "] present: " + versions);
			obj.add("container", b.getEnvironmentOrContainer());
			obj.add("displayName", b.getDisplayName());
			obj.add("buildId", b.getBuildId());
			JsonArrayBuilder userParams = Json.createArrayBuilder();
			for (Entry<String, String> entry : b.getUserParameters().entrySet()) {
				JsonObjectBuilder param = Json.createObjectBuilder();
				param.add("id", entry.getKey());
				param.add("displayName", entry.getValue());
				userParams.add(param);
			}
			obj.add("userParams", userParams);
			builds.add(obj.build());
		}
		return builds;
	}

	private Map<String, List<EnvironmentContainerDefinition>> getEnvironmentContainerDefinitions(EnvironmentData data, String provider) {
		ArrayList<String> environmentContainerNames = new ArrayList<>();
		for (AvailableBuildOrContainer b : data.getAvailableContainers(provider)) {
			environmentContainerNames.add(b.getEnvironmentOrContainer());
		}
		
		Map<String, List<EnvironmentContainerDefinition>> environmentContainerDefinitions = Collections.emptyMap();
		try {
			environmentContainerDefinitions = readManager.getEnvironmentContainerDefinitions(environmentContainerNames, provider);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Unable to load versions for environment containers", e);
		}
		return environmentContainerDefinitions;
	}
}
