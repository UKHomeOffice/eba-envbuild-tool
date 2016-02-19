package com.ipt.ebsa.environment.hiera;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.build.entities.Nic;
import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.hiera.firewall.FirewallManager;
import com.ipt.ebsa.environment.hiera.firewall.ForwardFirewallRule;
import com.ipt.ebsa.environment.hiera.firewall.InputFirewallRule;
import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.hiera.NodeMissingBehaviour;
import com.ipt.ebsa.template.TemplateManager;
import com.ipt.ebsa.util.FileUtil;
import com.ipt.ebsa.util.OrgEnvUtil;
import com.ipt.ebsa.yaml.YamlInjector;

/**
 * Overall manager for firewall rules
 * @author James Shepherd
 */
public class FirewallHieraManager extends BaseHieraManager {
	private static final String FIREWALL_CENTOS_HIERA_KEY = "ebsa::firewall::forward";
	private static final String FIREWALL_RULE_FORWARD_CENTOS_TEMPLATE = "firewallRule-forward-centos.yaml";
	private static final String FIREWALL_RULE_FORWARD_VYATTA_TEMPLATE = "firewallRule-forward-vyatta.yaml";
	private static final String FIREWALL_RULE_INPUT_TEMPLATE = "firewallRule-input.yaml";
	private static final Logger LOG = Logger.getLogger(FirewallHieraManager.class);
	
	private FirewallManager firewallManager;
	private ArrayList<ForwardFirewallRule> rules;
	private YamlInjector yamlInjector = new YamlInjector();
	private TreeSet<String> firewallKeys;
	private boolean isVyattaEnabled = true;
	
	/**
	 * 
	 * @param environment
	 * @param environmentVersion
	 * @param environmentProvider
	 * @param firewallManager
	 * @param hieraDirRoot
	 * @param templateManager
	 * @param hieraFileManager
	 * @param scope only paths with these prefixes will be touched, null means no restriction
	 * @param zones only these zones will be touched, null means all zones in the environment
	 * @param updateBehaviour
	 */
	public FirewallHieraManager(String environment, String environmentVersion, String environmentProvider, FirewallManager firewallManager, File hieraDirRoot,
									TemplateManager templateManager, HieraFileManager hieraFileManager,
									Set<String> scope, Set<String> zones, UpdateBehaviour updateBehaviour) {
		super(environment, environmentVersion, environmentProvider, hieraDirRoot, templateManager, hieraFileManager, scope, zones, updateBehaviour);
		this.firewallManager = firewallManager;
	}
	
	public void prepare() {
		reset();
		FileUtil.checkDirExistsOrCreate(hieraDirRoot);
		fetchTargetEnvironmentVMsAndZones();
		fetchForwardRules();
		
		for (ForwardFirewallRule rule : rules) {
			prepareForwardYamlChange(rule);
		}
		
		prepareInputYaml();
		
		addYamlDeletes();
	}

	private void prepareInputYaml() {
		InputFirewallRule.setNumbers(10000);
		Map<String, Set<InputFirewallRule>> ruleHistory = new TreeMap<>();
		for (VirtualMachine vm : targetEnvironmentVMs.values()) {
			String host = vm.getComputerName();
			String domain = OrgEnvUtil.getDomainForPuppet(vm.getVirtualmachinecontainer().getName());
			String zone = OrgEnvUtil.getEnvironmentName(vm.getVirtualmachinecontainer().getName());
			for (InputFirewallRule fwr : firewallManager.getInputFirewallRules(host, domain)) {
				if (isNewRule(ruleHistory, fwr)) {
					decodeSource(fwr);
					HieraMachineState hieraFile = findHieraFile(zone, host);
					templateManager.resetContext();
					templateManager.put("vm", vm);
					templateManager.put("rule", fwr);
					renderTemplate(hieraFile, FIREWALL_RULE_INPUT_TEMPLATE);
				}
			}
		}
	}

	private boolean isNewRule(Map<String, Set<InputFirewallRule>> ruleHistory, InputFirewallRule fwr) {
		InputFirewallRule clone = fwr.clone();
		String role = fwr.getHost().substring(0, 3);
		clone.setHost(role);
		Set<InputFirewallRule> history = ruleHistory.get(role);
		if (null == history) {
			history = new TreeSet<>();
			ruleHistory.put(role, history);
		}
		LOG.debug(String.format("Rule for role [%s]", clone));
		return history.add(clone);
	}

	private void decodeSource(InputFirewallRule fwr) {
		if (null != fwr.getSource() && fwr.getSource().matches(".*[^.0-9/].*")) {
			String fqdn = fwr.getSource() + "." + fwr.getDomain();
			VirtualMachine vm = targetEnvironmentVMs.get(fqdn);
			if (null == vm) {
				throw new RuntimeException(String.format("Can't find vm [%s] in target environment to decode hostname to IP address for input firewall rules", fqdn));
			}
			
			Nic appNic = null;
			for (Nic nic : vm.getNics()) {
				if (nic.getNetworkName().contains("APP")) {
					appNic = nic;
					break;
				}
			}

			if (null == appNic) {
				throw new RuntimeException(String.format("Can't find application nic for [%s].[%s]", vm.getComputerName(), OrgEnvUtil.getDomainForPuppet(vm.getVirtualmachinecontainer().getName())));
			}
			
			if (appNic.getInterfaces().isEmpty()) {
				throw new RuntimeException(String.format("Can't find application nic first interface for [%s].[%s]", vm.getComputerName(), vm.getVirtualmachinecontainer().getName()));
			}
			
			fwr.setSource(appNic.getInterfaces().get(0).getStaticIpAddress());
		}
		
	}

	private void fetchForwardRules() {
		rules.addAll(firewallManager.getAllForwardFirewallRules());
		TreeSet<String> bareZones = new TreeSet<>();
		for (String zone : zones) {
			bareZones.add(OrgEnvUtil.getDomainForPuppet(zone));
		}
		
		for (Iterator<ForwardFirewallRule> iterator = rules.iterator(); iterator.hasNext(); ) {
			ForwardFirewallRule rule = iterator.next();
			if (!bareZones.contains(rule.getZone())) {
				iterator.remove();
			}
		}
	}

	private void prepareForwardYamlChange(ForwardFirewallRule rule) {
		HieraMachineState hieraFile = findHieraFile(rule);
		templateManager.resetContext();
		templateManager.put("rule", rule);
		String template;
		
		if (rule.isVyatta()) {
			if (!isVyattaEnabled()) {
				LOG.debug("vyatta rules disabled");
				return;
			}
			
			if (!NumberUtils.isDigits(rule.getRuleNumber())) {
				throw new RuntimeException(String.format("Vyatta rule does not have 'rule number' or it is not just digits rule=[%s]", rule));
			}
			
			template = FIREWALL_RULE_FORWARD_VYATTA_TEMPLATE;
		} else {
			if (checkIfCentosRuleAlreadyExists(hieraFile, rule)) {
				return;
			} else {
				template = FIREWALL_RULE_FORWARD_CENTOS_TEMPLATE;
			}
		}
		
		renderTemplate(hieraFile, template);
	}

	protected void renderTemplate(HieraMachineState hieraFile, String template) {
		try {
			String yaml = templateManager.render(template);
			yaml = filterYamlByPath(yaml);
			if (StringUtils.isNotBlank(yaml)) {
				hieraUpdates.addAll(yamlInjector.updateYamlWithBlock(hieraFile, "", yaml, NodeMissingBehaviour.INSERT_ALL, "Bagpuss", hieraFile.getEnvironmentName()));
			}
		} catch (Exception e) {
			throw new RuntimeException(String.format("Failed to update yaml file [%s] from template [%s]",
					hieraFile.getFile().getAbsolutePath(),
					template), e);
		}
	}

	private boolean checkIfCentosRuleAlreadyExists(HieraMachineState hieraFile, ForwardFirewallRule rule) {
		Object object = hieraFile.getState().get(FIREWALL_CENTOS_HIERA_KEY);
		
		if (null != object) {
			if (object instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) object;
				for (Entry<String, Map<String, String>> me : map.entrySet()) {
					firewallKeys.add(makeKey(hieraFile, me.getKey()));
					Map<String, String> yamlRule = me.getValue();
					if (rule.getPort().equals(String.valueOf(yamlRule.get("port")).trim())
							&& rule.getDest().equals(yamlRule.get("destination").trim())
							&& rule.getSource().equals(yamlRule.get("source").trim())
							&& (null == yamlRule.get("proto") || rule.getProtocol().equals(yamlRule.get("proto").trim())
						)) {
						LOG.debug(String.format("Found existing rule for [%s]", rule));
						return true;
					}
				}
			} else {
				throw new RuntimeException(String.format("YAML for [%s] not a map in file [%s]",
						FIREWALL_CENTOS_HIERA_KEY, hieraFile.getFile().getAbsolutePath()));
			}
		}
		
		int i = 0;
		String key = String.format("%s_%s", environment, rule.getDesc().replaceAll("[^A-Za-z0-1]", "_"));
		String suggestedKey;
		do {
			suggestedKey = key + i++;
		} while (firewallKeys.contains(makeKey(hieraFile, suggestedKey)));
		key = suggestedKey;
		
		rule.setRuleName(key);
		firewallKeys.add(key);
		return false;
	}

	private String makeKey(HieraMachineState hieraFile, String key) {
		return hieraFile.getFile().getAbsolutePath() + key;
	}

	private HieraMachineState findHieraFile(ForwardFirewallRule rule) {
		return findHieraFile(rule.getZoneName(), rule.getHost());
	}

	protected HieraMachineState findHieraFile(String zoneName, String hostOrRole) {
		LOG.debug(String.format("finding hierafile for host or role [%s] zone [%s]", zoneName, hostOrRole));
		String org = zoneName.substring(0, 2);
		
		// EBSAD-19196 fw rules always go in role file
		String basename = hostOrRole.substring(0, 3)  + ".yaml";

		File zoneHieraDir = new File(new File(hieraDirRoot, org), zoneName);
		return findYamlFile(zoneHieraDir, hostOrRole, basename);
	}

	protected void reset() {
		super.reset();
		rules = new ArrayList<>();
		firewallKeys = new TreeSet<>();
	}

	public boolean isVyattaEnabled() {
		return isVyattaEnabled;
	}

	public void setVyattaEnabled(boolean isVyattaEnabled) {
		this.isVyattaEnabled = isVyattaEnabled;
	}
}
