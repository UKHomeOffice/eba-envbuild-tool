package com.ipt.ebsa.hiera.requires;


public class Require<T> implements Dependency {

	public T required;

	public Require(T required) {
		super();
		this.required = required;
	}

	public T getRequired() {
		return required;
	}

	public void setRequired(T required) {
		this.required = required;
	}
	
}
