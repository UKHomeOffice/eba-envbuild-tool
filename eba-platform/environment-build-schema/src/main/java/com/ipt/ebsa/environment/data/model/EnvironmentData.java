package com.ipt.ebsa.environment.data.model;

import java.util.Map;
import java.util.Set;

/**
 * Encapsulates a set of environment data and provides the means to query it.
 *
 * @author David Manning
 */
public interface EnvironmentData {

	/**
	 * @return builds (ultimately lists of actions) which may be used to create an environment.
	 */
	public Set<Build> getBuilds();
	
	public Build getBuildForEnvironmentAndBuildId(String environment, String id);

	public Build getBuildForEnvironmentContainerAndBuildId(String container, String id);
	
	/**
	 * @return Global parameters applicable globally, the world-over.
	 */
	public Map<String, String> getGlobalParameters();
	
	public Set<AvailableBuildOrContainer> getAvailableBuilds(String provider);
	
	public Set<AvailableBuildOrContainer> getAvailableContainers(String provider);
	
	public EnvironmentDetailsHolder getEnvironmentDetails();
	
	/**
	 * @return one XML document with all the XML that we have encountered
	 */
	public String getAllXmlAsXml();
	
	public enum Mode {
		BUILD, UPDATE;
		
		public static Mode fromString(String attr) {
			if ("build".equalsIgnoreCase(attr)) {
				return Mode.BUILD;
			} else if ("update".equalsIgnoreCase(attr)) {
				return Mode.UPDATE;
			} else {
				throw new IllegalArgumentException("Unknown build mode [" + attr + "]");
			}
		}
	}

	
	public static final class AvailableBuildOrContainer implements Comparable<AvailableBuildOrContainer> {
		private final String environmentorContainer;
		private BuildRef buildRef;
		
		public AvailableBuildOrContainer(String environmentOrContainer, BuildRef buildRef) {
			this.environmentorContainer = environmentOrContainer;
			this.buildRef = buildRef;
		}

		public String getEnvironmentOrContainer() {
			return environmentorContainer;
		}

		public String getDisplayName() {
			return buildRef.getDisplayName();
		}
		
		public String getBuildId() {
			return buildRef.getId();
		}
		
		public Map<String, String> getUserParameters() {
			return buildRef.getUiParams();
		}

		@Override
		public int compareTo(AvailableBuildOrContainer o) {
			if (o == null) {
				return 1;
			}
			int namecomparison = environmentorContainer.compareTo(o.environmentorContainer);
			return namecomparison != 0 ? namecomparison : buildRef.getDisplayName().compareTo(o.buildRef.getDisplayName());
		}
	}
}
