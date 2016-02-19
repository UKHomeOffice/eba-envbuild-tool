package com.ipt.ebsa.environment.build.execute;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.build.execute.action.ActionPerformer;
import com.ipt.ebsa.environment.build.execute.action.ActionPerformerFactory;
import com.ipt.ebsa.environment.build.git.GitMultiplexer;
import com.ipt.ebsa.environment.data.model.Build;
import com.ipt.ebsa.environment.data.model.EnvironmentData;
import com.ipt.ebsa.environment.data.model.ParameterisedNode;
import com.ipt.ebsa.environment.hiera.HieraFileManager;
import com.ipt.ebsa.util.FileUtil;

public class PerformerFactory {

	private static final Logger LOG = Logger.getLogger(PerformerFactory.class);
	
	EnvironmentData environmentData;
	private List<ActionPerformer> actionPerformers;

	private ActionPerformerFactory actionPerformerFactory;

	public PerformerFactory(EnvironmentData environmentData, ActionPerformerFactory actionPerformerFactory) {
		this.environmentData = environmentData;
		this.actionPerformerFactory = actionPerformerFactory;
	}
	
	public BuildPerformer getEnvironmentBuildPerformer(String workDir, String environment, String version, String buildRefId, String provider, Map<String, String> userParams, String envDefnXmlPath) {
		Build build = environmentData.getBuildForEnvironmentAndBuildId(environment, buildRefId);
		BuildContext globalBuildContext = new BuildContext();
		globalBuildContext.setEnvironment(environment);
		return getBuildPerformer(workDir, globalBuildContext, build, version, provider, buildRefId, userParams, envDefnXmlPath);
	}
	
	public BuildPerformer getContainerBuildPerformer(String workDir, String container, String version, String buildRefId, String provider, Map<String, String> userParams, String envDefnXmlPath) {
		Build build = environmentData.getBuildForEnvironmentContainerAndBuildId(container, buildRefId);
		BuildContext globalBuildContext = new BuildContext();
		globalBuildContext.setOrganisation(container);
		return getBuildPerformer(workDir, globalBuildContext, build, version, provider, buildRefId, userParams, envDefnXmlPath);
	}

	private BuildPerformer getBuildPerformer(String workDir, BuildContext globalBuildContext, Build build, String version, String provider, String buildRefId, 
			Map<String, String> userParams,	String envDefnXmlPath) {
		File workDirFile = new File(workDir);
		FileUtil.checkDirExistsOrCreate(workDirFile);
		globalBuildContext.setWorkDir(workDirFile);
		globalBuildContext.setGitMultiplexer(new GitMultiplexer());
		globalBuildContext.setHieraFileManager(new HieraFileManager());
		
		// clear out actionPerformers
		actionPerformers = new ArrayList<>();
		
		// global stuff
		BuildNode rootNode = generateRootNode(globalBuildContext, build, version, provider, userParams, envDefnXmlPath);

		return new BuildPerformer(rootNode, actionPerformers);
	}

	public BuildNode generateRootNode(String environment, String provider, String buildRefId, Map<String, String> userParams) {
		// clear out actionPerformers
		actionPerformers = new ArrayList<>();
		
		Build build = environmentData.getBuildForEnvironmentAndBuildId(environment, buildRefId);
		BuildContext globalBuildContext = new BuildContext();
		globalBuildContext.setEnvironment(environment);
		return generateRootNode(globalBuildContext, build, null, provider, userParams, null);
	}
	
	private BuildNode generateRootNode(BuildContext globalBuildContext, Build build, String version, String provider, Map<String, String> userParams,
			String envDefnXmlPath) {
		BuildNode globalNode = new BuildNode(); // Stick the global stuff in a BuildNode so it can be made visible as the 'parent' to the root node.
		
		globalBuildContext.setVersion(version);
		globalBuildContext.setProvider(provider);
		globalBuildContext.setEnvDefnXmlPath(envDefnXmlPath);
		globalNode.setBuildContext(globalBuildContext);
		Map<String, String> globalParameters = environmentData.getGlobalParameters();
		globalBuildContext.parameterMapPutAll(globalParameters);
		
		// root build stuff
		BuildNode rootNode = new BuildNode();
		
		globalNode.getChildren().add(rootNode);
		rootNode.setNode(build);
		setUpChildBuildContext(globalBuildContext, rootNode, build);
		
		List<KeyPair> missingParamValues = checkAndUpdateWithUserParams(build.getUserParameters(), rootNode.getBuildContext(), userParams);
		
		if (!missingParamValues.isEmpty()) {
			throw new RuntimeException("Missing user parameters, check your XML: " + missingParamValues);
		}
		
		// rest of the tree
		constructNodeChildren(rootNode);
		return rootNode;
	}
	
	


	private List<KeyPair> checkAndUpdateWithUserParams(HashMap<String, String> expectedUserParams, BuildContext buildContext,
			Map<String, String> actualParams) {
		List<KeyPair> missingParams = new ArrayList<>();
		for (Entry<String, String> expected : expectedUserParams.entrySet()) {
			if (!actualParams.containsKey(expected.getKey())) {
				missingParams.add(KeyPair.fromEntry(expected));
				continue;
			}
			
			buildContext.parameterMapPut(expected.getKey(), actualParams.get(expected.getKey()));
		}
		
		return missingParams;
	}
	
	private static final class KeyPair {
		public String key, value;
		public KeyPair(String key, String value) {
			this.key = key;
			this.value = value;
		}
		
		public static KeyPair fromEntry(Map.Entry<String, String> entry) {
			return new KeyPair(entry.getKey(), entry.getValue());
		}
		
		@Override
		public String toString() {
			return "[" + key + ": " + value + "]";
		}
	}

	private void setUpChildBuildContext(BuildContext parentBuildContext, BuildNode buildNode, ParameterisedNode nodeBeingBuildFrom) {
		LOG.debug("Setting up context for node: " + buildNode.getNode().getId());
		BuildContext buildContext = parentBuildContext.clone();
		buildContext.parameterMapPutAll(nodeBeingBuildFrom.getParameters());
		buildNode.setBuildContext(buildContext);
	}

	private void constructNodeChildren(BuildNode node) {
		for (ParameterisedNode child : node.getNode().getChildren()) {
			BuildNode childNode = createNode(node, child);
			node.getChildren().add(childNode);
			constructNodeChildren(childNode);
		}
	}

	private BuildNode createNode(BuildNode parentBuildNode, ParameterisedNode thisNode) {
		BuildNode thisBuildNode;
		
		if (thisNode.getChildren().size() > 0) {
			thisBuildNode = new BuildNode();
		} else {
			// baked into the xsd is that a leaf node must be an Action
			thisBuildNode = actionPerformerFactory.build(thisNode, parentBuildNode.getBuildContext());
			actionPerformers.add((ActionPerformer) thisBuildNode);
		}
		thisBuildNode.setNode(thisNode);
		thisBuildNode.setParent(parentBuildNode);
		setUpChildBuildContext(parentBuildNode.getBuildContext(), thisBuildNode, thisNode);
		return thisBuildNode;
	}
}
