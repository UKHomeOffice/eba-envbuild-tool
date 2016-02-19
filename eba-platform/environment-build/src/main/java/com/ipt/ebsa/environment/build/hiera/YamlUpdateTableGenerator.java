package com.ipt.ebsa.environment.build.hiera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.ipt.ebsa.hiera.HieraEnvironmentUpdate;
import com.ipt.ebsa.hiera.HieraMachineState;

/**
 * This was originally used to format yaml updates into a table for the GUI.
 * It is now dead code, but might be useful in the future.
 * @author James Shepherd
 */
public class YamlUpdateTableGenerator {

	public static List<String> getTableHead() {
		return Arrays.asList("Existing path", "Existing value", "Requested path", "Requested value", "Paths added", "Paths removed", "Hiera file");
	}

	public static List<List<String>> getTableBody(List<HieraEnvironmentUpdate> yamlUpdates) {
		ArrayList<List<String>> body = new ArrayList<List<String>>(yamlUpdates.size());

		for (HieraEnvironmentUpdate yamlUpdate : yamlUpdates) {
			body.add(Arrays.asList(
				clean(StringUtils.trimToEmpty(yamlUpdate.getExistingPath())),
				clean(yamlUpdate.getExistingValue()),
				clean(yamlUpdate.getRequestedPath()),
				clean(yamlUpdate.getRequestedValue()),
				clean(yamlUpdate.getPathElementsAdded()),
				clean(yamlUpdate.getPathElementsRemoved()),
				clean(((HieraMachineState) yamlUpdate.getSource()).getFile().getPath())));
		}

		return body;
	}
	
	private static String clean(Object o) {
		if (null == o) {
			return "";
		}
		
		return StringUtils.trimToEmpty(o.toString());
	}
}
