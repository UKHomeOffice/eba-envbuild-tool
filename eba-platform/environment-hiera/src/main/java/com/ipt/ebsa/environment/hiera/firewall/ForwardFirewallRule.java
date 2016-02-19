package com.ipt.ebsa.environment.hiera.firewall;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import com.ipt.ebsa.util.IPUtils;

/**
 * Domain object holding what will be a single rule in the appliance
 * @author James Shepherd
 */
public class ForwardFirewallRule {
	private static final Logger LOG = Logger.getLogger(ForwardFirewallRule.class);
	
	private String port;
	private String source;
	private String dest;
	private String desc;
	private String host;
	private String zone;
	private String protocol;
	private String ruleName;
	private String ruleNumber;
	
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getDest() {
		return dest;
	}
	public void setDest(String dest) {
		this.dest = dest;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getZone() {
		return zone;
	}
	public void setZone(String zone) {
		this.zone = zone;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getRuleName() {
		return ruleName;
	}
	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}
	public String getRuleNumber() {
		return ruleNumber;
	}
	public void setRuleNumber(String ruleNumber) {
		this.ruleNumber = ruleNumber;
	}
	
	public String getZoneName() {
		return getZone().substring(0, getZone().indexOf("."));
	}
	
	private void validate() {
		if (
			null == getZone()
			|| null == getDest()
			|| null == getPort()
			|| null == getProtocol()
			|| (isVyatta() && null == getRuleNumber())
		) {
			throw new RuntimeException(String.format("Firewall rule is not valid [%s]", this));
		}
	}
	
	public boolean isVyatta() {
		return FirewallUtil.isHostnameVyatta(getHost());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (null == obj) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		ForwardFirewallRule rhs = (ForwardFirewallRule) obj;
		return new EqualsBuilder().append(getDesc(), rhs.getDesc()).append(getDest(), rhs.getDest())
				.append(getHost(), rhs.getHost()).append(getPort(), rhs.getPort()).append(getProtocol(), rhs.getProtocol())
				.append(getRuleName(), rhs.getRuleName()).append(getRuleNumber(), rhs.getRuleNumber()).append(getSource(), rhs.getSource())
				.append(getZone(), rhs.getZone()).isEquals();
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("desc", getDesc()).append("dest", getDest()).append("host", getHost()).append("port", getPort())
				.append("protocol", getProtocol()).append("rule name", getRuleName()).append("rule number", getRuleNumber()).append("source", getSource())
				.append("zone", getZone()).toString();
	}
	
	public static List<ForwardFirewallRule> parse(ForwardFirewallDetails fwd) {
		LOG.debug(String.format("Parsing [%s]", fwd));
		ArrayList<ForwardFirewallRule> output = new ArrayList<>();
		List<String> hosts = FirewallUtil.parseHostname(fwd.getHost());
		String rawSource = fwd.getSource();
		String rawDest = fwd.getDest();
		List<String> sources;
		List<String> dests;
		List<String> ruleNumbers = new ArrayList<>();
		
		if (IPUtils.isIPv4Cidr(rawSource)) {
			sources = Arrays.asList(IPUtils.toFullIPv4Cidr(rawSource));
		} else if (IPUtils.isIPv4Addresses(rawSource)) {
			sources = IPUtils.toIPv4Addresses(rawSource);
		} else if (rawSource.equalsIgnoreCase("all")) {
			sources = Arrays.asList("0.0.0.0/0");
		} else if (rawSource.contains("psn")){
			// I imagine for security the psn ips are not in the spreadsheet.
			// not sure what to do about that
			// FIXME EBSAD-19196
			sources = Arrays.asList(rawSource);
		} else {
			throw new RuntimeException(String.format("Failed to parse source of firewall rule [%s]", fwd));
		}
		
		if (IPUtils.isIPv4Cidr(rawDest)) {
			dests = Arrays.asList(IPUtils.toFullIPv4Cidr(rawDest));
		} else if (IPUtils.isIPv4Addresses(rawDest)) {
			dests = IPUtils.toIPv4Addresses(rawDest);
		} else if (rawDest.equalsIgnoreCase("all")) {
			dests = Arrays.asList("0.0.0.0/0");
		} else if (rawDest.contains("psn")) {
			// I imagine for security the psn ips are not in the spreadsheet.
			// not sure what to do about that
			// FIXME EBSAD-19196
			dests = Arrays.asList(rawDest);
		} else {
			throw new RuntimeException(String.format("Failed to parse dest of firewall rule [%s]", fwd));
		}
		
		if (fwd.isVyatta()) {
			if (null == fwd.getRuleNumber()) {
				throw new RuntimeException(String.format("Vyatta rule has no rule number [%s]", fwd));
			}
			
			if (NumberUtils.isDigits(fwd.getRuleNumber())) {
				ruleNumbers.add(fwd.getRuleNumber());
			} else {
				for(String rn : fwd.getRuleNumber().split("[,/]")) {
					ruleNumbers.add(rn.trim());
				}
			}
			
			if (dests.size() * sources.size() > ruleNumbers.size()) {
				throw new RuntimeException(String.format("Not enough rule numbers for number of sources and destinations [%s]", fwd));
			}
		}
		
		String ports = fwd.getPort();
		if (ports.contains(",") && fwd.isVyatta()) {
			// need to format port as vyatta hiera expects
			ports = ports.replaceAll("[^0-9,]", "");
			ports = ports.replace(",", "%");
		}

		for (String host : hosts) {
			int ruleNumberIndex = 0;
			for (String source : sources) {
				for (String dest : dests) {
					ForwardFirewallRule fwr = new ForwardFirewallRule();
					fwr.setPort(ports);
					fwr.setDesc(fwd.getDesc());
					fwr.setZone(fwd.getZone());
					fwr.setProtocol(fwd.getProtocol());
					fwr.setSource(source);
					fwr.setHost(host);
					fwr.setDest(dest);
					if (fwd.isVyatta()) {
						fwr.setRuleNumber(ruleNumbers.get(ruleNumberIndex++));
					}
					fwr.validate();
					output.add(fwr);
				}
			}
		}
		return output;
	}
}