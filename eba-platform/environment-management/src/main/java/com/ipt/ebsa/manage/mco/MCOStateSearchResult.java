package com.ipt.ebsa.manage.mco;

import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.yaml.YamlUtil;

public class MCOStateSearchResult extends StateSearchResult {
	public static final String VERSION_KEY = "version";
	public static final String PACKAGE_KEY = "package";

	@Override
	public StateSearchResult copyOf() {
		StateSearchResult copy = new MCOStateSearchResult();
		copy.setSource(this.getSource() == null ? null : this.getSource().copyOf());
		if (this.getComponentState() != null) {
			copy.setComponentState(YamlUtil.deepCopyOfYaml(this.getComponentState()));
		}
		return copy;
	}
	
	@Override
	public String getComponentVersion() {
		return getComponentState().get(VERSION_KEY).toString();
	}

}
