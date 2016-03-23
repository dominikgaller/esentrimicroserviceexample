package com.esentri.microservice.example.authservice.entities;

import java.io.Serializable;

/**
 * The class @code User covers the user object.
 * 
 * @author dominikgaller
 *
 */
public class User implements Serializable {
	
	/** VersionUID. */
	private static final long serialVersionUID = 1L;
	
	/** Users id. */
	private long id;
	
	/** Users name. */
	private String name;
	
	/** Users password. */
	private String password;
	
	/** 
	 * Constructor for a new user.
	 * 
	 * @param id users id
	 * @param name users name
	 * @param password users password
	 */
	public User(long id, String name, String password) {
		this.setId(id);
		this.setName(name);
		this.setPassword(password);
	}
	
	/**
	 * Constructor for a new user
	 * 
	 * @param name users name.
	 * @param password users password.
	 */
	public User(String name, String password) {
		this.setName(name);
		this.setPassword(password);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
