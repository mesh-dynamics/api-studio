package com.cube.ws;

public final class Constants {
	/*
	 * Defining Constants here since these constants will be used across services to 
	 * set and read custom header constants.
	 * We can then include this Constants class in all web services and keep them consistent.
	 */
	
	// TODO: make sure this doesn't collide; perhaps add some other signature GUID
	public final static String CUBE_REPLAYID_HDRNAME = "__cube_replayid__";   
	
	private Constants() {}
}
