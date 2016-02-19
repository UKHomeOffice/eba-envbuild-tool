package com.ipt.ebsa.buildtools.release.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * This entity is a handle to the set of applications to which components can belong.  Every component needs to belong to at least one application.
 * @author scowx
 *
 */
@Entity
@NamedQueries({
@NamedQuery(
	    name="findApplicationByShortName",
	    query="select x from Application x where x.shortName = :shortName"
	),
@NamedQuery(name="list",
   query="select x from Application x order by x.name")
})
public class Application implements ReleaseEntity {
	@Id
	@GeneratedValue
	private Long id;

	private String shortName;
	
	private String name;
	
	private String role;

	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append(String.format("id='%s', ",id)); 
		b.append(String.format("name='%s', ",name));
		b.append(String.format("shortName='%s', ",shortName));
		b.append(String.format("role='%s'",role));
		return b.toString();
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getRole() {
		return role;
	}
	
	public void setRole(String role) {
		this.role = role;
	}	
}
