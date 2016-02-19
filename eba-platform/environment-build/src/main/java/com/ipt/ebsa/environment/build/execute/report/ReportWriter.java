package com.ipt.ebsa.environment.build.execute.report;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.collect.Sets;
import com.ipt.ebsa.environment.build.execute.BuildNode;
import com.ipt.ebsa.environment.build.execute.action.ActionContext;
import com.ipt.ebsa.environment.build.execute.action.ActionPerformer;
import com.ipt.ebsa.environment.hiera.BeforeAfter;

/**
 * Stateful writer for building up an HTML report as the object graph is created.
 *
 * @author David Manning
 */
public class ReportWriter {
	
	private static final Logger LOG = Logger.getLogger(ReportWriter.class);

	private final BuildNode root;
	
	public ReportWriter(BuildNode root) {
		super();
		this.root = root;
	}
	
	public String toHTML() {
		try {
			String reportSubstituted = String.format(IOUtils.toString(getClass().getResourceAsStream("/report-template.html")), toJson());
			return updateReport(reportSubstituted);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load report template", e);
		}
	}
	
	/**
	 * Reads the content of report.js and adds it as a script to the head of the report HTML.
	 */
	private String updateReport(String report) {
		Document document = Jsoup.parse(report);
		try {
			addJs(document, "/report.js");
			addJs(document, "/jsdifflib/difflib.js");
			addJs(document, "/jsdifflib/diffview.js");
			addCss(document, "/jsdifflib/diffview.css");
			return document.toString();
		} catch (IOException e) {
			LOG.warn("Unable to update HTML with javascript and css. Chances are the report won't looks nice");
			return report;
		}
	}

	private void addJs(Document document, String resource) throws IOException {
		String jsRaw = IOUtils.toString(getClass().getResource(resource));
		Element script = document.head().prependElement("script");
		script.attr("type", "text/javascript");
		script.appendChild(new DataNode(jsRaw, ""));
	}
	
	private void addCss(Document document, String resource) throws IOException {
		String cssRaw = IOUtils.toString(getClass().getResource(resource));
		Element script = document.head().prependElement("style");
		script.appendChild(new DataNode(cssRaw, ""));
	}
	
	public String toJson() {
		HashMap<String, ?> config = new HashMap<String, Object>();
		JsonBuilderFactory factory = Json.createBuilderFactory(config);
		JsonValue build = generateJson(factory, root);
		
		return build.toString();
	}

	private JsonValue generateJson(JsonBuilderFactory factory, BuildNode root) {
		LOG.debug("Build JSON for build node [" + root.getNode().getId() + "]");
		JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
		for (BuildNode child : root.getChildren()) {
			arrayBuilder.add(generateJson(factory, child));
		}
		
		JsonObjectBuilder builder = factory.createObjectBuilder()
			.add("id", root.getNode().getId() == null ? "" : root.getNode().getId())
			.add("contextParameters", (root.getParent() == null ? root.getBuildContext().parameterMapEntrySet().toString().replace("[", "").replace("]", "") : complement(root.getBuildContext().parameterMapEntrySet(), root.getParent().getBuildContext().parameterMapEntrySet()).toString().replace("[", "").replace("]", "")))
			.add("fullParameters", root.getBuildContext().parameterMapEntrySet().toString().replace("[", "").replace("]", ""))
			.add("open", !root.getChildren().isEmpty())
			.add("data", arrayBuilder);
		
		if (root.getChildren().isEmpty() && root instanceof ActionPerformer) {
			ActionPerformer actionPerformer = (ActionPerformer) root;
			ActionContext ac = actionPerformer.getActionContext();
			builder.add("actioncontext", mapToJson(ac.getActionContextMap().entrySet()));
			builder.add("actiontype", actionPerformer.getActionDisplayName());
			
			List<String> tableHead = ac.getGuiTableHead();
			if (null != tableHead) {
				JsonObjectBuilder table = factory.createObjectBuilder();
				table.add("head", collectionToJson(tableHead));
				table.add("body", listOfListToJson(ac.getGuiTableBody()));
				builder.add("table", table);
			}
			
			Set<BeforeAfter> beforeAfters = ac.getBeforeAfter();
			if (null != beforeAfters) {
				JsonArrayBuilder list = factory.createArrayBuilder();
				for (BeforeAfter ba : beforeAfters) {
					JsonObjectBuilder object = factory.createObjectBuilder();
					object.add("before", ba.getBefore());
					object.add("after", ba.getAfter());
					object.add("domain", ba.getDomain());
					object.add("basename", ba.getBasename());
					list.add(object);
					LOG.debug(String.format("Added BeforeAfter for [%s] [%s]", ba.getDomain(), ba.getBasename()));
				}
				builder.add("beforeAfters", list);
			}
		}
		
		JsonObject build = builder.build();
		LOG.debug("JSON built");
		return build;
	}
	
	private JsonArrayBuilder listOfListToJson(List<List<String>> list) {
		JsonArrayBuilder output = Json.createArrayBuilder();
		for (List<String> i : list) {
			output.add(collectionToJson(i));
		}
		
		return output;
	}

	private JsonArrayBuilder collectionToJson(Collection<String> list) {
		JsonArrayBuilder output = Json.createArrayBuilder();
		for (String i : list) {
			output.add(i);
		}
		
		return output;
	}

	private JsonObjectBuilder mapToJson(Set<Entry<String, String>> entrySet) {
		JsonObjectBuilder output = Json.createObjectBuilder();
		
		for (Map.Entry<String, String> me : entrySet) {
			LOG.debug(String.format("Adding [%s]=[%s]", me.getKey(), me.getValue()));
			output.add(me.getKey(), me.getValue());
		}
		
		return output;
	}

	/**
	 * Wikipedia said so.
	 * 
	 * @return the elements in map b which don't appear in a.
	 */
	private Set<Map.Entry<String, String>> complement(Set<Map.Entry<String, String>> a, Set<Map.Entry<String, String>> b) {
		return Sets.difference(a, b);
	}
}
