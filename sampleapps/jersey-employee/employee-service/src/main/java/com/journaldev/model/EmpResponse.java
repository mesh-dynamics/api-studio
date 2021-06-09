package com.journaldev.model;

import javax.xml.bind.annotation.XmlRootElement;

//@XmlRootElement(name = "empResponse")
public class EmpResponse {
	private int id;
	private String name;
	private String deptName;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDeptName() { return deptName; }

	public void setDeptName (String deptName) { this.deptName = deptName; }

}
