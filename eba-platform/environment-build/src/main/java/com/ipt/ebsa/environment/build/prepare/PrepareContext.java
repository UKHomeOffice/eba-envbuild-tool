package com.ipt.ebsa.environment.build.prepare;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;

public class PrepareContext {
	
	XMLGeographicContainerType geographicContainer;

	public XMLGeographicContainerType getGeographicContainer() {
		return geographicContainer;
	}

	public void setGeographicContainer(XMLGeographicContainerType geographicContainer) {
		this.geographicContainer = geographicContainer;
	}
	

}
