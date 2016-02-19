package com.ipt.ebsa.hiera;

import com.ipt.ebsa.environment.StateSearchResult;
import com.ipt.ebsa.yaml.YamlUtil;

public class HieraStateSearchResult extends StateSearchResult {

	@Override
	public StateSearchResult copyOf() {
		StateSearchResult copy = new HieraStateSearchResult();
		copy.setSource(this.getSource() == null ? null : this.getSource().copyOf());
		if (this.getComponentState() != null) {
			//TODO: Implement ComponentState as its own interface of which, one implementation is Hiera data
			copy.setComponentState(YamlUtil.deepCopyOfYaml(this.getComponentState()));
		}
		return copy;
	}
	
	@Override
	public String getComponentVersion() {
		return getComponentState().get(HieraData.ENSURE).toString();
	}

}
