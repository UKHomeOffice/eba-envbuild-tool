package com.ipt.ebsa.deployment.descriptor;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class ResolvedHost implements Comparable<ResolvedHost> {
	
	private final String hostOrRole;
	private final String zone;
	
	public ResolvedHost(String hostOrRole, String zone) {
		this.hostOrRole = hostOrRole;
		this.zone = zone;
	}

	public String getHostOrRole() {
		return hostOrRole;
	}

	public String getZone() {
		return zone;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != getClass()) {
			return false;
		}
		ResolvedHost rhs = (ResolvedHost) obj;
		return new EqualsBuilder()
		.append(this.hostOrRole, rhs.hostOrRole)
		.append(this.zone, rhs.zone)
		.isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(57, 49)
		.append(this.hostOrRole)
		.append(this.zone)
		.toHashCode();
	}
	
	@Override
	public String toString() {
		return new StringBuilder()
		.append(hostOrRole)
		.append(" [")
		.append(zone)
		.append("]")
		.toString();
	}

	@Override
	public int compareTo(ResolvedHost o) {
		return new CompareToBuilder()
		.append(this.hostOrRole, o.hostOrRole)
		.append(this.zone, o.zone)
		.toComparison();
	}
	
}
