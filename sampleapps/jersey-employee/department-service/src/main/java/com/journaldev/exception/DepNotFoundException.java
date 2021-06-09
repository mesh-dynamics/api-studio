package com.journaldev.exception;

public class DepNotFoundException extends Exception {

	private static final long serialVersionUID = 4351720088030656859L;
	private int errorId;

	public int getErrorId() {
		return errorId;
	}

	public DepNotFoundException(String msg, int errorId) {
		super(msg);
		this.errorId = errorId;
	}

	public DepNotFoundException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
