package com.cube.pubsub;

import java.util.Collections;
import java.util.Map;

import org.json.JSONObject;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.md.cache.Constants.PubSubContext;

public class ChannelMsg {

	public PubSubContext  context;

	@JsonInclude(Include.NON_ABSENT)
	public Map data = Collections.EMPTY_MAP;
	public ChannelMsg(){}

	public ChannelMsg(PubSubContext ctx , Map obj ){
		this.context = ctx;
		this.data = obj;
	}

}
