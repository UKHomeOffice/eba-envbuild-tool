package com.ipt.ebsa.environment.hiera.route;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;

import com.ipt.ebsa.environment.build.entities.VirtualMachine;
import com.ipt.ebsa.environment.hiera.msoffice.MsOffice;
import com.ipt.ebsa.util.OrgEnvUtil;

/**
 * Parses spreadsheet into routing rules.
 * @author James Shepherd
 */
public class RouteManager {
	
	private static final Logger LOG = Logger.getLogger(RouteManager.class);
	private static final Pattern ZONE_PATTERN = Pattern.compile("^\\s*[A-Z0-9_]+\\s*$");
	private static final String HEADING_ZONE = "vapp";
	private static final String HEADING_COMPARTMENT = "compartment or host prefix";
	private static final String HEADING_DEST = "dest";
	private static final String HEADING_ROUTE = "route";
	private static final String HEADING_INTERFACE = "if";
	private static final String HEADING_DESC = "desc";
	private static final String HEADING_RELATED_ZONES = "related vapps";
	private static final String HEADING_VERSION = "version";
	
	private Workbook workbook;
	private File xl;
	private Map<String, List<RouteDetails>> routeDetails;
	private Set<String> relatedZones;
	
	/**
	 * @param xl spreadsheet with routing rules in.
	 * @param relatedZones only output routes that are related to the given zone, if null then output all
	 */
	public RouteManager(File xl, Set<String> relatedZones) {
		this.xl = xl;
		if (null != relatedZones) {
			this.relatedZones = zonesToBareZones(relatedZones);
		}
	}

	private Set<String> zonesToBareZones(Set<String> zones) {
		TreeSet<String> output = new TreeSet<>();
		
		for (String zone : zones) {
			output.add(OrgEnvUtil.getBareEnvironment(zone));
		}
		
		return output;
	}

	public Map<String, List<Route>> getRoutes(VirtualMachine vm) {
		ArrayList<Route> outputHost = new ArrayList<>();
		ArrayList<Route> outputCompartment = new ArrayList<>();
		parseRoutes();
		String bareEnvironment = OrgEnvUtil.getBareEnvironment(vm.getVirtualmachinecontainer().getName());
		List<RouteDetails> vmRouteDetails = routeDetails.get(bareEnvironment);
		
		if (null == vmRouteDetails) {
			throw new RuntimeException(String.format("Failed to find routes for [%s] in file [%s]", bareEnvironment, xl.getAbsolutePath()));
		}
		
		String computerName = vm.getComputerName();
		LOG.info(String.format("Finding routes for vm [%s] from [%s] for this environment [%s]", computerName, vmRouteDetails.size(), bareEnvironment));
		
		for (RouteDetails rd : vmRouteDetails) {
			if (null != relatedZones) {
				boolean found = false;
				for (String zone : rd.getRelatedZones().split(",")) {
					if (relatedZones.contains(zone.trim())) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					continue;
				}
			}
			
			/*
			 * if we find that the compartment is actually the host prefix of
			 * the machine we are dealing with, then we need to take those routes.
			 */
			if (rd.getCompartment().length() > 1 && computerName.startsWith(rd.getCompartment())) {
				outputHost.add(Route.parse(rd));
			}
			
			/*
			 * we look for the compartment that this vm is in.
			 * The compartment is in the name e.g. docAm is in A.
			 */
			String vmCompartment = computerName.substring(3, 4).toUpperCase();
			if (rd.getCompartment().length() == 1 && vmCompartment.equals(rd.getCompartment().toUpperCase())) {
				outputCompartment.add(Route.parse(rd));
			}
		}
		
		List<Route> routes;
		if (!outputHost.isEmpty()) {
			routes = outputHost;
		} else {
			if (outputCompartment.isEmpty()) {
				LOG.warn(String.format("Failed to find routes for VM [%s] in file [%s]", vm.getComputerName(), xl.getAbsolutePath()));
			}
			routes = outputCompartment;
		}
		
		TreeMap<String, List<Route>> output = new TreeMap<>();
		for (Route route : routes){
			List<Route> ifRoutes;
			if (output.containsKey(route.getIface())) {
				ifRoutes = output.get(route.getIface());
			} else {
				ifRoutes = new ArrayList<>();
				output.put(route.getIface(), ifRoutes);
			}
			
			ifRoutes.add(route);
		}
		
		return output;
	}
	
	private void parseRoutes() {
		if (null == routeDetails) {
			openWorkbook();
			routeDetails = new TreeMap<>();
			MsOffice.parseAllSheets(workbook, new MsOffice.RowParser() {
				private final Set<String> headings = new TreeSet<>(Arrays.asList(
						HEADING_ZONE,
						HEADING_COMPARTMENT,
						HEADING_DEST,
						HEADING_ROUTE,
						HEADING_INTERFACE,
						HEADING_DESC,
						HEADING_RELATED_ZONES,
						HEADING_VERSION
					));
				
				@Override
				public Set<String> getRowHeadings() {
					return headings;
				}

				@Override
				public void parse(Map<String, String> row) {
					String zone = row.get(HEADING_ZONE);
					if (null != row && null != zone) {
						if (ZONE_PATTERN.matcher(zone).matches()) {
							// we are on a row with a route
							if (null == routeDetails.get(zone)) {
								routeDetails.put(zone, new ArrayList<RouteDetails>());
							}
							List<RouteDetails> zoneRouteDetails = routeDetails.get(zone);
							RouteDetails rd = new RouteDetails();
							rd.setZone(zone);
							rd.setCompartment(row.get(HEADING_COMPARTMENT));
							rd.setDest(row.get(HEADING_DEST));
							rd.setRoute(row.get(HEADING_ROUTE));
							rd.setIface(row.get(HEADING_INTERFACE));
							rd.setDesc(row.get(HEADING_DESC));
							rd.setRelatedZones(row.get(HEADING_RELATED_ZONES));
							rd.setVersion(row.get(HEADING_VERSION));
							zoneRouteDetails.add(rd);
						} else {
							LOG.debug("Cell doesn't match pattern");
						}
					} else {
						LOG.debug("Row or cell null");
					}
				}
			});
		}
		
		try {
			workbook.close();
		} catch (IOException e) {
			throw new RuntimeException("Failed to close routing spreadsheet");
		}
	}
	
	private void openWorkbook() {
		workbook = new MsOffice().getWorkbook(xl);
	}
}
