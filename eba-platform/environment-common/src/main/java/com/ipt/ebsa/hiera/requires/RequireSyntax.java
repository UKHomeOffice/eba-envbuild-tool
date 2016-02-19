package com.ipt.ebsa.hiera.requires;

/**
 * Syntax found in the Require area
 * 
 * @author scowx
 * 
 */
public enum RequireSyntax {
	Service("Service"), Exec("Exec"), File("File"), Class("Class"), Mount("Mount"), Package("Package"), Postgres("Postgresql::Db");

	String value = null;

	private RequireSyntax(String s) {
		value = s;
	}
	
	/**
	 * Returns the enum based n the string sent in
	 * @param s
	 * @return
	 */
	public static RequireSyntax fromSyntax(String s) {
		if (Service.value.equals(s)) {
			return Service;
		}
		else if (Exec.value.equals(s)) {
			return Exec;
		}
		else if (File.value.equals(s)) {
			return File;
		}
		else if (Class.value.equals(s)) {
			return Class;
		}
		else if (Mount.value.equals(s)) {
			return Mount;
		}
		else if (Package.value.equals(s)) {
			return Package;
		}
		else if (Postgres.value.equals(s)) {
			return Postgres;
		}
		else {
			throw new IllegalArgumentException("Unrecognised syntax '"+s+"'");
		}
	}

}
