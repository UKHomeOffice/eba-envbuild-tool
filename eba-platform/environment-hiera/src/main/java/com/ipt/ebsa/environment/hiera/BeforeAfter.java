package com.ipt.ebsa.environment.hiera;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

/**
 * Holds before and after yaml.
 * @author James Shepherd
 */
public class BeforeAfter implements Comparable<BeforeAfter> {
	private static final Logger LOG = Logger.getLogger(BeforeAfter.class);
	
	private String before = "";
	private String after = "";
	private String basename;
	private String domain;
	
	public String getBefore() {
		return before;
	}
	void setBefore(String before) {
		this.before = before;
	}
	public String getAfter() {
		return after;
	}
	void setAfter(String after) {
		this.after = after;
	}
	public String getBasename() {
		return basename;
	}
	void setBasename(String basename) {
		this.basename = basename;
	}
	public String getDomain() {
		return domain;
	}
	void setDomain(String domain) {
		this.domain = domain;
	}
	
	@Override
	public boolean equals(Object o) {
		if (null == o) {
			return false;
		}
		
		if (this == o) {
			return true;
		}
		
		if (o instanceof BeforeAfter) {
			BeforeAfter other = (BeforeAfter) o;
			return compareTo(other) == 0;
		}
		
		return false;
	}
	
	@Override
	public int compareTo(BeforeAfter other) {
		if (null == other) {
			return -1;
		}
		
		int zoneDiff = getDomain().compareTo(other.getDomain());
		if (zoneDiff != 0) {
			return zoneDiff;
		}
		
		int basenameDiff = getBasename().compareTo(other.getBasename());
		if (basenameDiff != 0) {
			return basenameDiff;
		}
		
		LOG.debug("equal");
		
		return 0;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).
			       append("domain", getDomain()).
			       append("basename", getBasename()).
			       toString();
	}
}