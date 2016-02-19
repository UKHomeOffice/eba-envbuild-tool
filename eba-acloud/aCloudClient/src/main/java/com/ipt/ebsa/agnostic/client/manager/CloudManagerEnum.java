package com.ipt.ebsa.agnostic.client.manager;


/**
 * 
 *
 */
public enum CloudManagerEnum {
	SKYSCAPE, AWS, MICROSOFTAZURE, GOOGLECOMPUTE, IBMSOFTLAYER;
	
	public static CloudManagerEnum findByString(String lowercase){
	    for(CloudManagerEnum v : values()){
	        if( v.toString().equals(lowercase.toUpperCase())){
	            return v;
	        }
	    }
	    return null;
	}
}
