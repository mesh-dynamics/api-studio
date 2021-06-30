package com.cube.launch;

public enum ExitCode {
	SERVER_PORT_UNAVAILABLE(15),
	REDIS_PORT_UNAVAILABLE(16);

	public int getValue() {
		return value;
	}

	private int value;
	ExitCode(int val){
		this.value = val;
	}


}
