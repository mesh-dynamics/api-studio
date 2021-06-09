package com.journaldev.model;

import java.util.ArrayList;
import java.util.List;

public class Departments {

	private List<Department> departmentList;

	public List<Department> getDepartmentList() {
		if (departmentList == null) {
			departmentList = new ArrayList<>();
		}
		return departmentList;
	}

	public void setDepartmentList(List<Department> departmentList) {
		this.departmentList = departmentList;
	}
}
