package com.ipt.ebsa.config;

/**
 * Organisation specific details
 * @since EBSAD-8025
 *
 */
public class Organisation {

	// Name e.g st, np, pr
    public String shortName;
    
	public Organisation(String shortName) {
		super();
		this.shortName = shortName;
	}

	public String getShortName() {
		return shortName;
	}
	
	@Override
	public boolean equals(Object o) {
		if (null != o) {
			if (o instanceof Organisation) {
				Organisation organisation = (Organisation) o;
				return organisation.getShortName().equals(this.getShortName()); 
			}
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return "Organisation [shortName=" + shortName + "]";
	}

}
