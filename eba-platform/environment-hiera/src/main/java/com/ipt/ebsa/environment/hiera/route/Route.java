package com.ipt.ebsa.environment.hiera.route;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.ipt.ebsa.util.IPUtils;

/**
 * Domain object for a single routing rule.
 * @author James Shepherd
 */
public class Route {
	private static final Logger LOG = Logger.getLogger(Route.class);
	private static final Pattern VIA_PATTERN = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");
	
	private String cidr;
	private String via;
	private String iface;
	private String name;
	private boolean isDefaultRoute = false;
	
	public String getCidr() {
		return cidr;
	}
	
	public String getVia() {
		return via;
	}
	
	public void setCidr(String cidr) {
		this.cidr = cidr;
	}

	public void setVia(String via) {
		this.via = via;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isDefaultRoute() {
		return isDefaultRoute;
	}

	public void setDefaultRoute(boolean isDefaultRoute) {
		this.isDefaultRoute = isDefaultRoute;
	}

	public static Route parse(RouteDetails rd) {
		Route route = new Route();
		LOG.debug(String.format("Parsing [%s]", rd));
		
		if ("Default".equalsIgnoreCase(rd.getDest())) {
			route.setCidr("0.0.0.0/0");
			route.setDefaultRoute(true);
		} else {
		route.setCidr(IPUtils.toFullIPv4Cidr(rd.getDest()));
		}
		
		route.setIface(rd.getIface());
		if (null != rd.getDesc()) {
			route.setName(rd.getDesc().replaceAll("[^A-Za-z0-1]", "_"));
		}
		Matcher mvia = VIA_PATTERN.matcher(rd.getRoute());
		
		if (mvia.find()) {
			route.setVia(mvia.group(1));
			return route;
		} else {
			throw new RuntimeException(String.format("Failed to get via from [%s]", rd));
		}
	}
}