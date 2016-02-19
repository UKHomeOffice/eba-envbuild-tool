package com.ipt.ebsa.deployment.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class (along with it's nested classes) act as a wrapper for the XML generated classes
 * so that additional info such as application short name can be stored. When an instance of
 * this class is instantiated it will automatically create children instances for any plans,
 * components and before/after steps. All children carry a reference to the parent class.
 * Fields of these classes are immutable, including all collections.
 * 
 * @author Dan McCarthy
 *
 */
public class DeploymentDescriptor {
	
	private final XMLDeploymentDescriptorType xmlType;
	private final String applicationShortName;
	private final List<Plan> plans;
	private final List<Component> components;
	
	public DeploymentDescriptor(
			XMLDeploymentDescriptorType xmlType,
			String applicationShortName) {
		super();
		
		// XML and short name can't be null
		this.xmlType = checkNotNull(xmlType, "xmlType");
		this.applicationShortName = checkNotNull(applicationShortName, "applicationShortName");
		
		plans = initPlans();
		components = initComponents();
	}
	
	private static <T> T checkNotNull(T arg, String argName) {
		if (arg == null) {
			throw new IllegalArgumentException(String.format("Arg %s cannot be null", argName));
		}
		return arg;
	}
	
	private static void checkAllNotNull(Object[] args, String[] argNames) {
		boolean allNulls = true;
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null) {
				allNulls = false;
				break;
			}
		}
		if (allNulls) {
			throw new IllegalArgumentException(String.format("Args %s cannot all be null", Arrays.toString(argNames)));
		}
	}

	private List<Plan> initPlans() {
		List<Plan> tempPlans = new ArrayList<>();
		if (xmlType.getPlans() != null && 
				xmlType.getPlans().getPlan() != null) {
			for (XMLPlanType plan : xmlType.getPlans().getPlan()) {
				tempPlans.add(new Plan(plan, this, applicationShortName));
			}
		}
		return Collections.unmodifiableList(tempPlans);
	}
	
	private List<Component> initComponents() {
		List<Component> tempComponents = new ArrayList<>();
		if (xmlType.getComponents() != null &&
				xmlType.getComponents().getComponent() != null) {
			for (XMLComponentType component : xmlType.getComponents().getComponent()) {
				tempComponents.add(new Component(component, this, applicationShortName));
			}
		}
		return Collections.unmodifiableList(tempComponents);
	}

	public XMLDeploymentDescriptorType getXMLType() {
		return xmlType;
	}

	public String getApplicationShortName() {
		return applicationShortName;
	}
	
	public List<Plan> getPlans() {
		return plans;
	}

	public List<Component> getComponents() {
		return components;
	}
	
	public boolean containsEnvironments() {
		XMLEnvironmentsType environments = xmlType.environments;
		return environments != null && environments.environment != null;
	}

	public static class Plan {
		
		private final XMLPlanType xmlType;
		private final DeploymentDescriptor deploymentDescriptor;
		private final List<Step> steps;
		private final String applicationShortName;
		
		public Plan(XMLPlanType xmlType, DeploymentDescriptor deploymentDescriptor, String applicationShortName) {
			super();
			this.xmlType = checkNotNull(xmlType, "xmlType");
			this.deploymentDescriptor = checkNotNull(deploymentDescriptor, "deploymentDescriptor");
			this.applicationShortName = checkNotNull(applicationShortName, "applicationShortName");
			
			steps = initSteps();
		}
		
		private List<Step> initSteps() {
			List<Step> tempSteps = new ArrayList<>();
			if (xmlType.getStep() != null) {
				for (XMLStepItemType step : xmlType.getStep()) {
					tempSteps.add(new Step(step, this, applicationShortName));
				}
			}
			return Collections.unmodifiableList(tempSteps);
		}

		public XMLPlanType getXMLType() {
			return xmlType;
		}

		public DeploymentDescriptor getDeploymentDescriptor() {
			return deploymentDescriptor;
		}
		
		public List<Step> getSteps() {
			return steps;
		}

		public String getApplicationShortName() {
			return applicationShortName;
		}

	}
	
	public static class Component {
		
		private final XMLComponentType xmlType;
		private final DeploymentDescriptor deploymentDescriptor;
		private final String applicationShortName;
		private final BeforeAfterSteps steps;
		private final BeforeAfterSteps deploySteps;
		private final BeforeAfterSteps undeploySteps;
		private final BeforeAfterSteps downgradeSteps;
		private final BeforeAfterSteps upgradeSteps;
		
		public Component(XMLComponentType xmlType, 
				DeploymentDescriptor deploymentDescriptor, 
				String applicationShortName) {
			super();
			this.xmlType = checkNotNull(xmlType, "xmlType");
			this.deploymentDescriptor = checkNotNull(deploymentDescriptor, "deploymentDescriptor");
			this.applicationShortName = checkNotNull(applicationShortName, "applicationShortName");
			
			steps = initSteps();
			deploySteps = initDeploy();
			undeploySteps = initUndeploy();
			downgradeSteps = initDowngrade();
			upgradeSteps = initUpgrade();
		}
		
		private BeforeAfterSteps initSteps() {
			List<Step> beforeSteps = new ArrayList<>();
			if (xmlType.getBefore() != null && 
					xmlType.getBefore().getStep() != null) {
				for (XMLStepItemType step : xmlType.getBefore().getStep()) {
					beforeSteps.add(new Step(step, this, applicationShortName));
				}
			}
			List<Step> afterSteps = new ArrayList<>();
			if (xmlType.getAfter() != null && 
					xmlType.getAfter().getStep() != null) {
				for (XMLStepItemType step : xmlType.getAfter().getStep()) {
					afterSteps.add(new Step(step, this, applicationShortName));
				}
			}
			return new BeforeAfterSteps(Collections.unmodifiableList(beforeSteps), Collections.unmodifiableList(afterSteps));
		}
		
		private BeforeAfterSteps initDeploy() {
			List<Step> beforeDeploySteps = new ArrayList<>();
			if (xmlType.getHints() != null && 
					xmlType.getHints().getDeploy() != null &&
					xmlType.getHints().getDeploy().getBefore() != null &&
					xmlType.getHints().getDeploy().getBefore().getStep() != null) {
				for (XMLStepItemType step : xmlType.getHints().getDeploy().getBefore().getStep()) {
					beforeDeploySteps.add(new Step(step, this, applicationShortName));
				}
			}
			List<Step> afterDeploySteps = new ArrayList<>();
			if (xmlType.getHints() != null && 
					xmlType.getHints().getDeploy() != null &&
					xmlType.getHints().getDeploy().getAfter() != null &&
					xmlType.getHints().getDeploy().getAfter().getStep() != null) {
				for (XMLStepItemType step : xmlType.getHints().getDeploy().getAfter().getStep()) {
					afterDeploySteps.add(new Step(step, this, applicationShortName));
				}
			}
			return new BeforeAfterSteps(Collections.unmodifiableList(beforeDeploySteps), Collections.unmodifiableList(afterDeploySteps));
		}
		
		private BeforeAfterSteps initUndeploy() {
			List<Step> beforeUndeploySteps = new ArrayList<>();
			if (xmlType.getHints() != null && 
					xmlType.getHints().getUndeploy() != null &&
					xmlType.getHints().getUndeploy().getBefore() != null &&
					xmlType.getHints().getUndeploy().getBefore().getStep() != null) {
				for (XMLStepItemType step : xmlType.getHints().getUndeploy().getBefore().getStep()) {
					beforeUndeploySteps.add(new Step(step, this, applicationShortName));
				}
			}
			List<Step> afterUndeploySteps = new ArrayList<>();
			if (xmlType.getHints() != null && 
					xmlType.getHints().getUndeploy() != null &&
					xmlType.getHints().getUndeploy().getAfter() != null &&
					xmlType.getHints().getUndeploy().getAfter().getStep() != null) {
				for (XMLStepItemType step : xmlType.getHints().getUndeploy().getAfter().getStep()) {
					afterUndeploySteps.add(new Step(step, this, applicationShortName));
				}
			}
			return new BeforeAfterSteps(Collections.unmodifiableList(beforeUndeploySteps), Collections.unmodifiableList(afterUndeploySteps));
		}
		
		private BeforeAfterSteps initDowngrade() {
			List<Step> beforeDowngradeSteps = new ArrayList<>();
			if (xmlType.getHints() != null && 
					xmlType.getHints().getDowngrade() != null &&
					xmlType.getHints().getDowngrade().getBefore() != null &&
					xmlType.getHints().getDowngrade().getBefore().getStep() != null) {
				for (XMLStepItemType step : xmlType.getHints().getDowngrade().getBefore().getStep()) {
					beforeDowngradeSteps.add(new Step(step, this, applicationShortName));
				}
			}
			List<Step> afterDowngradeSteps = new ArrayList<>();
			if (xmlType.getHints() != null && 
					xmlType.getHints().getDowngrade() != null &&
					xmlType.getHints().getDowngrade().getAfter() != null &&
					xmlType.getHints().getDowngrade().getAfter().getStep() != null) {
				for (XMLStepItemType step : xmlType.getHints().getDowngrade().getAfter().getStep()) {
					afterDowngradeSteps.add(new Step(step, this, applicationShortName));
				}
			}
			return new BeforeAfterSteps(Collections.unmodifiableList(beforeDowngradeSteps), Collections.unmodifiableList(afterDowngradeSteps));
		}
		
		private BeforeAfterSteps initUpgrade() {
			List<Step> beforeUpgradeSteps = new ArrayList<>();
			if (xmlType.getHints() != null && 
					xmlType.getHints().getUpgrade() != null &&
					xmlType.getHints().getUpgrade().getBefore() != null &&
					xmlType.getHints().getUpgrade().getBefore().getStep() != null) {
				for (XMLStepItemType step : xmlType.getHints().getUpgrade().getBefore().getStep()) {
					beforeUpgradeSteps.add(new Step(step, this, applicationShortName));
				}
			}
			List<Step> afterUpgradeSteps = new ArrayList<>();
			if (xmlType.getHints() != null && 
					xmlType.getHints().getUpgrade() != null &&
					xmlType.getHints().getUpgrade().getAfter() != null &&
					xmlType.getHints().getUpgrade().getAfter().getStep() != null) {
				for (XMLStepItemType step : xmlType.getHints().getUpgrade().getAfter().getStep()) {
					afterUpgradeSteps.add(new Step(step, this, applicationShortName));
				}
			}
			return new BeforeAfterSteps(Collections.unmodifiableList(beforeUpgradeSteps), Collections.unmodifiableList(afterUpgradeSteps));
		}

		public XMLComponentType getXMLType() {
			return xmlType;
		}

		public DeploymentDescriptor getDeploymentDescriptor() {
			return deploymentDescriptor;
		}

		public String getApplicationShortName() {
			return applicationShortName;
		}

		public BeforeAfterSteps getSteps() {
			return steps;
		}

		public BeforeAfterSteps getDeploySteps() {
			return deploySteps;
		}

		public BeforeAfterSteps getUndeploySteps() {
			return undeploySteps;
		}

		public BeforeAfterSteps getDowngradeSteps() {
			return downgradeSteps;
		}

		public BeforeAfterSteps getUpgradeSteps() {
			return upgradeSteps;
		}
		
	}
	
	public static class BeforeAfterSteps {
		
		private final List<Step> before;
		private final List<Step> after;
		
		public BeforeAfterSteps(List<Step> before, List<Step> after) {
			super();
			this.before = checkNotNull(before, "before");
			this.after = checkNotNull(after, "after");
		}

		public List<Step> getBefore() {
			return before;
		}

		public List<Step> getAfter() {
			return after;
		}
		
	}
	
	public static class Step {
		
		private final XMLStepItemType xmlType;
		private final List<StepItem> items;
		private final Plan plan;
		private final Component component;
		private final String applicationShortName;
		
		public Step(XMLStepItemType xmlType, Plan plan, String applicationShortName) {
			this(xmlType, plan, null, applicationShortName);
		}
		
		public Step(XMLStepItemType xmlType, Component component, String applicationShortName) {
			this(xmlType, null, component, applicationShortName);
		}
		
		public Step(XMLStepItemType xmlType, Plan plan, Component component, String applicationShortName) {
			super();
			this.xmlType = checkNotNull(xmlType, "xmlType");
			
			// Both plan and component cannot be null be one should be
			checkAllNotNull(new Object[]{plan,  component}, new String[]{"plan", "component"});
			this.plan = plan;
			this.component = component;
			
			this.applicationShortName = checkNotNull(applicationShortName, "applicationShortName");
			
			items = initItems();
		}

		private List<StepItem> initItems() {
			List<StepItem> tempItems = new ArrayList<>();
			if (xmlType.getInjectOrPerformOrExecute() != null) {
				for (XMLStepCommandType command : xmlType.getInjectOrPerformOrExecute()) {
					tempItems.add(new StepItem(command, applicationShortName, this));
				}
			}
			return Collections.unmodifiableList(tempItems);
		}

		public XMLStepItemType getXMLType() {
			return xmlType;
		}
		
		/**
		 * Will return null if getComponent() != null
		 * @return
		 */
		public Plan getPlan() {
			return plan;
		}

		/**
		 * Will return null if getPlan() != null
		 * @return
		 */
		public Component getComponent() {
			return component;
		}

		public String getApplicationShortName() {
			return applicationShortName;
		}

		public List<StepItem> getItems() {
			return items;
		}
		
	}
	
	public static class StepItem {
		
		private final XMLStepCommandType xmlType;
		private final String applicationShortName;
		private final Step step;
		private Collection<ResolvedHost> hosts;
		
		public StepItem(XMLStepCommandType xmlType) {
			this(xmlType, "", null);
		}
		
		public StepItem(XMLStepCommandType xmlType, String applicationShortName, Step step) {
			this.xmlType = checkNotNull(xmlType, "xmlType");
			this.applicationShortName = checkNotNull(applicationShortName, "applicationShortName");
			this.step = step;
		}

		public Collection<ResolvedHost> getHosts() {
			return hosts;
		}

		public void setHosts(Collection<ResolvedHost> hosts) {
			this.hosts = hosts;
		}

		public XMLStepCommandType getXmlType() {
			return xmlType;
		}

		public String getApplicationShortName() {
			return applicationShortName;
		}

		public Step getStep() {
			return step;
		}
		
	}

}
