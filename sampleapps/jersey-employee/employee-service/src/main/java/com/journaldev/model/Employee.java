package com.journaldev.model;

public class Employee {
	private int employeeId;
	private String givenName;
	private String middleName;
	private String familyName;
	private String displayName;
	private String active;
	private String phone;
	private String email;
	private String employeeType;
	private Address primaryAddress;
	private int departmentId;
	private int officeId;

	public Employee(int employeeId, String givenName, String middleName, String familyName,
		String displayName, String active, String phone, String email, String employeeType,
		Address primaryAddress, int departmentId, int officeId) {
		this.employeeId = employeeId;
		this.givenName = givenName;
		this.middleName = middleName;
		this.familyName = familyName;
		this.displayName = displayName;
		this.active = active;
		this.phone = phone;
		this.email = email;
		this.employeeType = employeeType;
		this.primaryAddress = primaryAddress;
		this.departmentId = departmentId;
		this.officeId = officeId;
	}

	public int getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(int employeeId) {
		this.employeeId = employeeId;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getActive() {
		return active;
	}

	public void setActive(String active) {
		this.active = active;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmployeeType() {
		return employeeType;
	}

	public void setEmployeeType(String employeeType) {
		this.employeeType = employeeType;
	}

	public Address getPrimaryAddress() {
		return primaryAddress;
	}

	public void setPrimaryAddress(Address primaryAddress) {
		this.primaryAddress = primaryAddress;
	}

	public int getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(int departmentId) {
		this.departmentId = departmentId;
	}

	public int getOfficeId() {
		return officeId;
	}

	public void setOfficeId(int officeId) {
		this.officeId = officeId;
	}
}
