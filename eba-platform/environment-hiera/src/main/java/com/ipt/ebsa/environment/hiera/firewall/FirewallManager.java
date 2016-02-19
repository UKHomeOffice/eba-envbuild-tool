package com.ipt.ebsa.environment.hiera.firewall;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;

import com.ipt.ebsa.environment.hiera.msoffice.MsOffice;

/**
 * Manages parsing the firewall spreadsheet.
 * @author James Shepherd
 */
public class FirewallManager {
	private static final Logger LOG = Logger.getLogger(FirewallManager.class);
	private static final String HEADING_FORWARD_TYPE = "type";
	private static final String HEADING_FORWARD_HOST = "host";
	private static final String HEADING_FORWARD_ZONE = "vapp";
	private static final String HEADING_FORWARD_SOURCE = "source";
	private static final String HEADING_FORWARD_DEST = "dest";
	private static final String HEADING_FORWARD_PORT = "port";
	private static final String HEADING_FORWARD_PROTOCOL = "protocol";
	private static final String HEADING_FORWARD_DESC = "description";
	private static final String HEADING_FORWARD_RELATED_ZONES = "related vapps";
	private static final String HEADING_FORWARD_COMMENTS = "comments";
	private static final String HEADING_FORWARD_RULE_NUMBER = "rule number (vyatta)";
	private static final String HEADING_FORWARD_VERSION = "version changed";
	
	private static final String HEADING_INPUT_HOST = "host";
	private static final String HEADING_INPUT_ZONE = "vapp";
	private static final String HEADING_INPUT_SOURCES = "source servers";
	private static final String HEADING_INPUT_PORTS = "ports";
	private static final String HEADING_INPUT_PROTOCOL = "protocol";
	private static final String HEADING_INPUT_DESC = "description";
	private static final String HEADING_INPUT_VERSION = "version changed";
	private static final String HEADING_INPUT_COMMENTS = "comments";
	
	private static final String COMMON_INPUT_RULE_KEY = "";
	
	private File xl;
	private List<ForwardFirewallDetails> forwardFirewallDetails;
	private MultiHashMap inputFirewallRulesMap;
	
	public FirewallManager(File firewallXls) {
		this.xl = firewallXls;
	}
	
	/**
	 * @param relatedZone something like "II_PJT1_DEV1"
	 * @return all forward firewall rules associated with the related zone
	 */
	public List<ForwardFirewallRule> getForwardFirewallRules(String relatedZone) {
		parseForwardFirewallRules();
		ArrayList<ForwardFirewallRule> output = new ArrayList<>();
		// match with a comma or end as delimiters
		Pattern p = Pattern.compile("(?:^|,)\\s*" + Pattern.quote(relatedZone)+ "\\s*(?:$|,)");
		for (ForwardFirewallDetails fwd : forwardFirewallDetails) {
			if ((null != fwd.getRelatedZones() && p.matcher(fwd.getRelatedZones()).find())) {
				output.addAll(ForwardFirewallRule.parse(fwd));
			}
		}
		return output;
	}

	/**
	 * @return all firewall rules
	 */
	public List<ForwardFirewallRule> getAllForwardFirewallRules() {
		parseForwardFirewallRules();
		ArrayList<ForwardFirewallRule> output = new ArrayList<>();
		for (ForwardFirewallDetails fwd : forwardFirewallDetails) {
			output.addAll(ForwardFirewallRule.parse(fwd));
		}
		return output;
	}
	
	/**
	 * 
	 * @param host
	 * @param domain puppet dotted domain
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<InputFirewallRule> getInputFirewallRules(String host, String domain) {
		LOG.debug(String.format("Finding input firewall rules for host [%s] domain [%s]", host, domain));
		parseInputFirewallRules();
		ArrayList<InputFirewallRule> output = new ArrayList<>();
		
		if (FirewallUtil.isHostnameVyatta(host)) {
			Collection<InputFirewallRule> rules = (Collection<InputFirewallRule>) inputFirewallRulesMap.getCollection(COMMON_INPUT_RULE_KEY);
			if (null != rules) {
				for (InputFirewallRule rule : rules) {
					InputFirewallRule clone = rule.clone();
					clone.setHost(host);
					clone.setDomain(domain);
					output.add(clone);
				}
			}
		}
		
		Collection<InputFirewallRule> rules = (Collection<InputFirewallRule>) inputFirewallRulesMap.getCollection(domain);
		
		if (null != rules) {
			for(InputFirewallRule rule : rules) {
				if (host.equalsIgnoreCase(rule.getHost())) {
					output.add(rule);
				}
			}
		}
		
		LOG.debug(String.format("Found [%s] rules", output.size()));
		return output;
	}
	
	private void parseForwardFirewallRules() {
		if (null == forwardFirewallDetails) {
			forwardFirewallDetails = new ArrayList<>();
			Workbook workbook = new MsOffice().getWorkbook(xl);
			MsOffice.parseAllSheets(workbook, new MsOffice.RowParser() {
				private final TreeSet<String> headings = new TreeSet<>(Arrays.asList(
						HEADING_FORWARD_TYPE,
						HEADING_FORWARD_HOST,
						HEADING_FORWARD_ZONE,
						HEADING_FORWARD_SOURCE,
						HEADING_FORWARD_DEST,
						HEADING_FORWARD_PORT,
						HEADING_FORWARD_PROTOCOL,
						HEADING_FORWARD_DESC,
						HEADING_FORWARD_RELATED_ZONES,
						HEADING_FORWARD_COMMENTS,
						HEADING_FORWARD_RULE_NUMBER,
						HEADING_FORWARD_VERSION
					));
				
				@Override
				public Set<String> getRowHeadings() {
					return headings;
				}

				@Override
				public void parse(Map<String, String> row) {
					String type = row.get(HEADING_FORWARD_TYPE);
					if ("Firewall".equals(type)) {
						ForwardFirewallDetails fwd = new ForwardFirewallDetails();
						fwd.setType(type);
						fwd.setHost(row.get(HEADING_FORWARD_HOST));
						fwd.setZone(row.get(HEADING_FORWARD_ZONE));
						fwd.setSource(row.get(HEADING_FORWARD_SOURCE));
						fwd.setDest(row.get(HEADING_FORWARD_DEST));
						fwd.setPort(row.get(HEADING_FORWARD_PORT));
						fwd.setProtocol(row.get(HEADING_FORWARD_PROTOCOL));
						fwd.setDesc(row.get(HEADING_FORWARD_DESC));
						fwd.setRelatedZones(row.get(HEADING_FORWARD_RELATED_ZONES));
						fwd.setComments(row.get(HEADING_FORWARD_COMMENTS));
						fwd.setRuleNumber(row.get(HEADING_FORWARD_RULE_NUMBER));
						fwd.setVersion(row.get(HEADING_FORWARD_VERSION));
						
						// FIXME PROPERLY - EBSAD-19196
						if ("ANY".equalsIgnoreCase(fwd.getSource())
								|| "VPN".equalsIgnoreCase(fwd.getSource())
								|| "ANY".equalsIgnoreCase(fwd.getDest())) {
							return;
						}
						
						forwardFirewallDetails.add(fwd);
					}
				}
			});
			
			try {
				workbook.close();
			} catch (IOException e) {
				throw new RuntimeException("Failed to close firewall spreadsheet");
			}
		}
		
	}
	
	private void parseInputFirewallRules() {
		if (null == inputFirewallRulesMap) {
			inputFirewallRulesMap = new MultiHashMap();
			Workbook workbook = new MsOffice().getWorkbook(xl);
			MsOffice.parseAllSheets(workbook, new MsOffice.RowParser() {
				private final TreeSet<String> headings = new TreeSet<>(Arrays.asList(
						HEADING_INPUT_HOST,
						HEADING_INPUT_ZONE,
						HEADING_INPUT_SOURCES,
						HEADING_INPUT_PORTS,
						HEADING_INPUT_PROTOCOL,
						HEADING_INPUT_DESC,
						HEADING_INPUT_VERSION,
						HEADING_INPUT_COMMENTS
					));
				
				@Override
				public Set<String> getRowHeadings() {
					return headings;
				}

				@Override
				public void parse(Map<String, String> row) {
					String host = row.get(HEADING_INPUT_HOST);
					if (StringUtils.isNotBlank(host)) {
						InputFirewallDetails details = new InputFirewallDetails();
						details.setHosts(row.get(HEADING_INPUT_HOST));
						details.setZone(row.get(HEADING_INPUT_ZONE));
						details.setSources(row.get(HEADING_INPUT_SOURCES));
						details.setPorts(row.get(HEADING_INPUT_PORTS));
						details.setProtocol(row.get(HEADING_INPUT_PROTOCOL));
						details.setDesc(row.get(HEADING_INPUT_DESC));
						details.setVersion(row.get(HEADING_INPUT_VERSION));
						details.setComments(row.get(HEADING_INPUT_COMMENTS));
			
						String key = details.getHosts().equalsIgnoreCase("common") ? COMMON_INPUT_RULE_KEY : details.getZone();
						inputFirewallRulesMap.putAll(key, InputFirewallRule.parse(details));
					}
				}
			});
			
			try {
				workbook.close();
			} catch (IOException e) {
				throw new RuntimeException("Failed to close firewall spreadsheet");
			}
		}
	}
}
