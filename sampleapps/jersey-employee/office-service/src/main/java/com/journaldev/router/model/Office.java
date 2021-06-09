package com.journaldev.router.model;

public class Office {

	private int officeId;
	private String country;
	private Address officeAddress;

	public Office(int officeId, String country, Address officeAddress) {
		this.officeId = officeId;
		this.country = country;
		this.officeAddress = officeAddress;
	}

	public int getOfficeId() {
		return officeId;
	}

	public void setOfficeId(int officeId) {
		this.officeId = officeId;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public Address getOfficeAddress() {
		return officeAddress;
	}

	public void setOfficeAddress(Address officeAddress) {
		this.officeAddress = officeAddress;
	}
}
