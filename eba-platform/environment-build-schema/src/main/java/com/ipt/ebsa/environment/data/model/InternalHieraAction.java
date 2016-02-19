package com.ipt.ebsa.environment.data.model;

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import com.ipt.ebsa.environment.v1.build.XMLScope;
import com.ipt.ebsa.environment.v1.build.XMLUpdateBehaviour;
import com.ipt.ebsa.environment.v1.build.XMLZone;


/**
 * Adds/updates hiera data inside the target environment
 *
 * @author James Shepherd
 */
public class InternalHieraAction extends Action {

	private InternalHieraActionPlaceHolder placeHolder;

	public InternalHieraAction(InternalHieraActionPlaceHolder placeHolder) {
		super(placeHolder.getId());
		this.placeHolder = placeHolder;
	}

	public String getHieraRepoUrl() {
		return placeHolder.xmlData.getHierarepourl();
	}
	
	public String getRoutesRepoUrl() {
		return placeHolder.xmlData.getRoutesrepourl();
	}
	
	public String getRoutesPath() {
		return placeHolder.xmlData.getRoutespath();
	}
	
	public XMLUpdateBehaviour getUpdateBehaviour() {
		return placeHolder.xmlData.getUpdatebehaviour();
	}
	
	public Set<String> getZones() {
		TreeSet<String> output = new TreeSet<>();
		for (Object child : placeHolder.xmlData.getZoneOrScope()) {
			if (child instanceof XMLZone) {
				output.add(((XMLZone) child).getZone());
			}
		}
		
		if (StringUtils.isNotBlank(placeHolder.xmlData.getZones())) {
			for (String zone : placeHolder.xmlData.getZones().split(",")) {
				output.add(zone.trim());
			}
		}
		
		return output;
	}
	
	public Set<String> getScope() {
		TreeSet<String> output = new TreeSet<>();
		for (Object child : placeHolder.xmlData.getZoneOrScope()) {
			if (child instanceof XMLScope) {
				output.add(((XMLScope) child).getYamlpath());
			}
		}
		
		return output;
	}
}
