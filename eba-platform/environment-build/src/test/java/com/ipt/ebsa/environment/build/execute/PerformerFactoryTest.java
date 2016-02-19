package com.ipt.ebsa.environment.build.execute;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLProviderType;
import com.ipt.ebsa.environment.build.execute.action.ActionPerformer;
import com.ipt.ebsa.environment.build.execute.action.ActionPerformerFactory;
import com.ipt.ebsa.environment.build.test.BaseTest;
import com.ipt.ebsa.environment.data.model.Build;
import com.ipt.ebsa.environment.data.model.EnvironmentData;
import com.ipt.ebsa.environment.data.model.InfraAction;
import com.ipt.ebsa.environment.data.model.ParameterisedNode;
import com.ipt.ebsa.environment.data.model.SshAction;

public class PerformerFactoryTest extends BaseTest {

	private Build build;
	private EnvironmentData data;
	
	/**
	 * Sets up a simple structure comprising:
	 * 				Root Node
	 * 			  /	          \
	 *       Child 1        Child 2
	 *      /       \          
	 *  Grand     Grand
	 *  Child 1   Child 2 
	 *  
	 *  chain #1: r -> c1 -> gc1
	 *  chain #2: r -> c1 -> gc2
	 *  chain #3: r -> c2
	 */
	@Before
	public void setUpBuildMock() {
		data = mock(EnvironmentData.class);
		build = mock(Build.class);
		when(data.getBuildForEnvironmentAndBuildId("Englebert", "BUILD")).thenReturn(build);

		HashMap<String, String> globalParams = new HashMap<String, String>();
		globalParams.put("g1_p1", "g1_p1");
		when(data.getGlobalParameters()).thenReturn(globalParams);

		ParameterisedNode gc1= node("gc1").withParams(p("gc1_p1"), p("gc1_p2"), p("gc1_p3")).ofType(SshAction.class).build();
		ParameterisedNode gc2 = node("gc2").withParams(p("gc2_p1"), p("gc2_p2"), p("gc2_p3")).ofType(SshAction.class).build();
		ParameterisedNode c1 = node("c1").withChildren(gc1, gc2).withParams(p("c1_p1"), p("c1_p2")).build();
		ParameterisedNode c2 = node("c2").withParams(p("c2_p1")).ofType(InfraAction.class).build();
		ParameterisedNode rootNode = node("root").withChildren(c1, c2).withParams(p("r1_p1")).build();
		when(build.getChildren()).thenReturn(Arrays.asList(rootNode));
	}
	
	/**
	 * Tests that the parameters are cascaded correct down through the sequence/action tree.
	 */
	@Test
	public void buildActionParameters() {
		PerformerFactory factory = new PerformerFactory(data, new ActionPerformerFactory());
		BuildPerformer buildPerformer = factory.getEnvironmentBuildPerformer(getWorkDirPath(), "Englebert", "Humperdink", "BUILD", XMLProviderType.SKYSCAPE.toString(), new HashMap<String, String>(), "/no/path");
		List<ActionPerformer> actionPerformers = buildPerformer.getActionPerformers();
		
		assertEquals("Number of actions", 3, actionPerformers.size());

		// Check params for each chain (depth first)
		assertParameters(actionPerformers.get(0), "c1_p1", "c1_p2", "g1_p1", "gc1_p1", "gc1_p2", "gc1_p3", "r1_p1");
		assertParameters(actionPerformers.get(1), "c1_p1", "c1_p2", "g1_p1", "gc2_p1", "gc2_p2", "gc2_p3", "r1_p1");
		assertParameters(actionPerformers.get(2), "c2_p1", "g1_p1", "r1_p1");
	}
	
	private void assertParameters(ActionPerformer actionPerformer, String... expected) {
		int i = 0;
		for (Entry<String, String> entry : actionPerformer.getBuildContext().parameterMapEntrySet()) {
			assertEquals("Parameters #" + i, expected[i], entry.getKey());
			i++;
		}
	}
	
	
	private static NodeBuilder node(String id) {
		NodeBuilder b = new NodeBuilder();
		b.id = id;
		return b; 
	}
	
	private static final class NodeBuilder {
		
		private String id;
		private List<? extends ParameterisedNode> children = new ArrayList<>();
		private List<Param> params = new ArrayList<>();
		private Class<? extends ParameterisedNode> type;

		public NodeBuilder withChildren(ParameterisedNode... children) {
			this.children = Arrays.asList(children);
			return this;
		}
		
		public NodeBuilder withParams(Param... params) {
			this.params = Arrays.asList(params);
			return this;
		}
		
		public NodeBuilder ofType(Class<? extends ParameterisedNode> type) {
			this.type = type;
			return this;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public ParameterisedNode build() {
			ParameterisedNode node;
			if (type == null) {
				node = mock(ParameterisedNode.class);
			} else {
				node = mock(type);
			}
			when(node.getId()).thenReturn(id);
			when(node.getChildren()).thenReturn((List)children);
			
			Map<String, String> paramsMap = new HashMap<String, String>();
			for (Param p : params) {
				paramsMap.put(p.key, p.value);
			}
			when(node.getParameters()).thenReturn(paramsMap);
			return node;
		}
	}
	
	private Param p(String key) {
		return new Param(key, key);
	}
	
	private static final class Param {
		private String key;
		private String value;

		public Param(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}
}
