package com.journaldev.model;

public class Address {
	private String line1;
	private String city;
	private String postalcode;

	public Address(String line1, String city, String postalcode) {
		this.line1 = line1;
		this.city = city;
		this.postalcode = postalcode;
	}

	public String getLine1() {
		return line1;
	}

	public void setLine1(String line1) {
		this.line1 = line1;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getPostalcode() {
		return postalcode;
	}

	public void setPostalcode(String postalcode) {
		this.postalcode = postalcode;
	}
}
