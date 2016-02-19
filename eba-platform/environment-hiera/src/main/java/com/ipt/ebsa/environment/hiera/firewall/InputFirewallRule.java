/**
 * 
 */
package com.ipt.ebsa.environment.hiera.firewall;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import com.ipt.ebsa.util.IPUtils;

/**
 * Represents a single firewall input rule.
 * @author James Shepherd
 */
public class InputFirewallRule implements Cloneable, Comparable<InputFirewallRule> {
	private static final Logger LOG = Logger.getLogger(InputFirewallRule.class);
	private static int nextNumber = 9999;
	
	private String host;
	private String domain;
	private String source;
	private String port;
	private String protocol;
	private String desc;
	private String version;
	private String comments;
	private String number;
	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	@Override
	public InputFirewallRule clone() {
		InputFirewallRule clone = new InputFirewallRule();
		clone.setComments(getComments());
		clone.setDesc(getDesc());
		clone.setDomain(getDomain());
		clone.setHost(getHost());
		clone.setNumber(getNumber());
		clone.setPort(getPort());
		clone.setProtocol(getProtocol());
		clone.setSource(getSource());
		clone.setVersion(getVersion());
		return clone;
	}
	
	public static List<InputFirewallRule> parse(InputFirewallDetails fwd) {
		LOG.debug(String.format("Parsing [%s]", fwd));
		
		// split up hosts
		List<String> hosts;
		if ("common".equalsIgnoreCase(fwd.getHosts())) {
			hosts = Arrays.asList(fwd.getHosts());
		} else {
			hosts = FirewallUtil.parseHostname(fwd.getHosts());
		}
		
		// split up sources
		ArrayList<String> sources = new ArrayList<>();
		if (StringUtils.isNotBlank(fwd.getSources())) {
			int nlIndex = fwd.getSources().indexOf("\n");
			if (nlIndex == -1) {
				parseSources(sources, fwd.getSources());
			} else {
				for (String source : fwd.getSources().trim().split("\\s*\\n\\s*")) {
					parseSources(sources, source);
				}
			}
		}
		
		// split up ports
		List<String> ports = Arrays.asList(fwd.getPorts().trim().split("\\s*,\\s*"));
		
		ArrayList<InputFirewallRule> output = new ArrayList<>();
		for (String host : hosts) {
			for (String port : ports) {
				if (StringUtils.isBlank(fwd.getSources())) {
					setupRule(fwd, output, host, port, null);
				} else {
					for (String source : sources) {
						setupRule(fwd, output, host, port, source);
					}
				}
			}
		}
		
		return output;
	}
	
	private static void setupRule(InputFirewallDetails fwd, ArrayList<InputFirewallRule> output, String host, String port, String source) {
		InputFirewallRule rule = new InputFirewallRule();
		rule.setHost(host);
		rule.setDomain(fwd.getZone());
		rule.setSource(source);
		rule.setPort(port);
		rule.setProtocol(StringUtils.isBlank(fwd.getProtocol()) ? null : fwd.getProtocol());
		rule.setDesc(fwd.getDesc());
		rule.setVersion(fwd.getVersion());
		rule.setComments(fwd.getComments());
		rule.setNumber(String.valueOf(++nextNumber));
		output.add(rule);
	}
	
	private static void parseSources(ArrayList<String> sources, String source) {
		if (source.matches(".*[A-Za-z].*")) {
			// assume is a hostname
			sources.addAll(FirewallUtil.parseHostname(source));
		} else if (IPUtils.isIPv4Cidr(source)) {
			sources.add(IPUtils.toFullIPv4Cidr(source));
		} else if (IPUtils.isIPv4Addresses(source)) {
			sources.addAll(IPUtils.toIPv4Addresses(source));
		}
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
		InputFirewallRule rhs = (InputFirewallRule) obj;
		return new EqualsBuilder().append(getHost(), rhs.getHost()).append(getDomain(), rhs.getDomain())
				.append(getSource(), rhs.getSource()).append(getPort(), rhs.getPort())
				.append(getProtocol(), rhs.getProtocol()).append(getDesc(), rhs.getDesc())
				.append(getVersion(), rhs.getVersion()).append(getComments(), rhs.getComments()).isEquals();
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("host", getHost())
				.append("domain", getDomain())
				.append("source", getSource())
				.append("port", getPort())
				.append("protocol", getProtocol())
				.append("description", getDesc())
				.append("version", getVersion())
				.append("comments", getComments()).toString();
	}
	@Override
	public int compareTo(InputFirewallRule o) {
		return new CompareToBuilder().append(getHost(), o.getHost()).append(getDomain(), o.getDomain())
				.append(getSource(), o.getSource()).append(getPort(), o.getPort())
				.append(getProtocol(), o.getProtocol()).append(getDesc(), o.getDesc()).toComparison();
	}
	
	/**
	 * This is to reset the numbers that are given to each parsed rule.
	 * This is a bit of a hack, as the numbers are not given in the spreadsheet.
	 * @param i
	 */
	public static void setNumbers(int i) {
		nextNumber = i;
	}
}
