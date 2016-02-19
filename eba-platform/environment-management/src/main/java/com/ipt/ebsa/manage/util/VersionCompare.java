package com.ipt.ebsa.manage.util;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

/**
 * Compares two version strings. 
 * 
 * Algorithm inspired by https://twiki.cern.ch/twiki/bin/view/Main/RPMAndDebVersioning
 * 
 * Breaks strings into portions defined by dots and a single dash (the last one) e.g. (xxx.xxx.xxx.xxx.xxx-xxx)
 *
 * Compares strings in xx.xx.xx.xx-xx format where the number of x.x's can be anything and the contents of 
 * each xx group is compared numerically if they are numbers and alphanumerically if they contain non numeric characters.  
 * 
 * if all the n.n.n's are the same then it compares the release versions (the bit after the last dash) if there are any.
*  The release versions are compared the same way as the other strings
 * 
 */
public class VersionCompare implements Comparator<String> {
	
	/**
	 * Class which parses out details from the version string so that it can be compared
	 * @author scowx
	 *
	 */
	static class Version {
		
		String releaseValue;
		String[] vals; 
		String originalString;
		String stringWithoutRelease;
				
		public Version(String originalString) {
			if (StringUtils.isBlank(originalString)) {
				throw new IllegalArgumentException("Version string is blank.");
			}
			this.originalString = originalString;
			calculateRelease();
			calculateOtherValues();			
		}
		
		/**
		 * turn the n.n.n.n into an array of numbers.  It does not matter how long it is.
		 */
		private void calculateOtherValues() {
			vals = stringWithoutRelease.split("\\.");
		}

		/**
		 * Chops the "-nn" off the end of the version.
		 */
		private void calculateRelease() {
			int indexOfRelease = originalString.lastIndexOf("-");
			if (indexOfRelease > 0){
				releaseValue = originalString.substring(indexOfRelease+1);
				if (StringUtils.isBlank(releaseValue)) {
					throw new RuntimeException("Invalid version, release is blank in '"+originalString+"'");
				}
				stringWithoutRelease = originalString.substring(0, indexOfRelease);
			}
			else if (indexOfRelease == 0) {
				throw new RuntimeException("Invalid version, release is the only thing in '"+originalString+"'");
			}
			else {
				stringWithoutRelease = originalString;
			}
		}
		
	}
	
	@Override
	public int compare(String o1, String o2) {
		
		/* Deal with nulls */
		if (o1 == null && o2 == null) {
			return 0;
		}
		else if (o1 == null) {
			return -1; 
		}
		else if (o2 == null) {
			return 1;
		}
		
		/* Convert to something more intelligent */
		Version v1 = new Version(o1);
		Version v2 = new Version(o2);
		
		/* Looks like these are version numbers that we can compare properly */
		int i = 0;
	    // set index to first non-equal ordinal or length of shortest version string
	    
		while (i < v1.vals.length && i < v2.vals.length && v1.vals[i].equals(v2.vals[i])) 
	    {
	      i++;
	    }
	    // compare first non-equal value
	    if (i < v1.vals.length && i < v2.vals.length) 
	    {
	    	//This will do a string comparison if one of them is a string and a numeric calculation of both of them are numbers
	    	return cleverCompare(v1.vals[i], v2.vals[i]);
	        
	    }
	    // the strings are equal or one string is a substring of the other
	    // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
	    else
	    {
	        int val = Integer.signum(v1.vals.length - v2.vals.length);
	        if (val == 0 ) {
	        	if (v1.releaseValue == null && v2.releaseValue == null) {
	        		return 0;
	        	}
	        	else if (v1.releaseValue != null ) {
	        		if (v2.releaseValue != null) {
	        			//The release values can be as complex as actual version numbers so we use the same tool al over again
	        			return new VersionCompare().compare(v1.releaseValue, v2.releaseValue);
	        		}
	        		else {
	        			return 1;
	        		}
	        	}
	        	else if (v2.releaseValue != null) {
	        		return -1;
	        	}
	        }
	        
	        return val;
	    }
	}

	/**
	 * This will do a string comparison if one of them is a string and a numeric calculation of both of them are numbers
	 * @param val1
	 * @param val2
	 * @return
	 */
	private int cleverCompare(String val1, String val2) {
		int diff;
		//String s = "";
		if (StringUtils.isNumeric(val1) && StringUtils.isNumeric(val2)) {
			diff = getInt(val1).compareTo(getInt(val2));
			//s += String.format("Comparing '%s' and '%s' as ints", val1, val2);
		}
		else {
			diff = val1.compareTo(val2);	
			//s += String.format("Comparing '%s' and '%s' as strings", val1, val2);
		}
		//s+= " result=";
		//s+=(diff + " converting to '"+Integer.signum(diff)+"'");
		//System.out.println(s);
		return Integer.signum(diff);
	}

	private Integer getInt(String val1) {
		return new Integer(Integer.parseInt(val1));
	}

	
}

