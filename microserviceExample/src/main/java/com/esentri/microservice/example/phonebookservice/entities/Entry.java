package com.esentri.microservice.example.phonebookservice.entities;

/**
 * The class {@code Entry} represents a phone book entry entity.
 * @author dominikgaller
 *
 */
public class Entry {
	
	/** This entries id. */
	private long id;
	
	/** This entries name. */
	private String name;

	/** This entries number. */
	private String number;
	
	/**
	 * Constructor for an entry with id.
	 * @param id the entries id
	 * @param name the entries name
	 * @param number the entries number
	 */
	public Entry(long id, String name, String number) {
		this.setId(id);
		this.setName(name);
		this.setNumber(number);
	}
	
	/**
	 * Constructor for an entry without id.
	 * @param name the entries name
	 * @param number the entries number
	 */
	public Entry(String name, String number) {
		this.setName(name);
		this.setNumber(number);
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the number
	 */
	public String getNumber() {
		return number;
	}

	/**
	 * @param number the number to set
	 */
	public void setNumber(String number) {
		this.number = number;
	}
	
	
}
