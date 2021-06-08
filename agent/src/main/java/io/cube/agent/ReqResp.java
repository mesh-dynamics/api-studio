package io.cube.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class ReqResp {


	/**
	 * @param pathwparams
	 * @param meta
	 * @param hdrs
	 * @param body
	 */
	public ReqResp(String pathwparams, List<Entry<String, String>> meta,
		List<Entry<String, String>> hdrs, String body) {
		super();
		this.pathwparams = pathwparams;
		this.meta = meta;
		this.hdrs = hdrs;
		this.body = body;
	}

	/**
	 *
	 */
	private ReqResp() {
		super();
		this.pathwparams = "";
		this.meta = new ArrayList<Entry<String, String>>();
		this.hdrs = new ArrayList<Entry<String, String>>();
		this.body = "";
	}


	@JsonProperty("path")
	public final String pathwparams; // path with params
	@JsonDeserialize(as=ArrayList.class)
	public final List<Entry<String, String>> meta;
	@JsonDeserialize(as=ArrayList.class)
	public final List<Entry<String, String>> hdrs;
	public final String body;

}
