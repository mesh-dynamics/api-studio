package com.journaldev.router.model;

import java.util.ArrayList;
import java.util.List;

public class Offices {

	private List<Office> officeList;

	public List<Office> getOfficeList() {
		if (officeList == null) {
			officeList = new ArrayList<>();
		}
		return officeList;
	}

	public void setOfficeList(List<Office> officeList) {
		this.officeList = officeList;
	}
}
