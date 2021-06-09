package com.cube.pubsub;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.cache.MDCacheMgr;
import io.md.utils.CubeObjectMapperProvider;
import redis.clients.jedis.JedisPubSub;

import com.cube.ws.Config;

public class PubSubChannel extends JedisPubSub {
	Logger LOGGER = LogManager.getLogger(PubSubChannel.class);
	public static final String MD_PUBSUB_CHANNEL_NAME = "MeshDPubSubChannel";
	private final Config cfg;

	private PubSubChannel(Config cfg){
		this.cfg = cfg;
	}

	private static PubSubChannel singleton;
	private ObjectMapper mapper = CubeObjectMapperProvider.getInstance();

	public static PubSubChannel getSingleton(Config cfg){
		if(singleton!=null) return singleton;
		synchronized (PubSubChannel.class){
			if(singleton==null){
				singleton = new PubSubChannel(cfg);
			}
		}
		return singleton;
	}

	@Override
	public void onMessage(String channel, String message) {
		LOGGER.info("Msg Received: "+message + " channel:"+channel);
		if(!channel.equals(MD_PUBSUB_CHANNEL_NAME)){
			LOGGER.error("received msg for channel "+channel + " code bug.");
			return;
		}
		ChannelMsg msg = null;
		try{
			msg = mapper.readValue(message.getBytes() , ChannelMsg.class);
			if(msg.context == null){
				throw new Exception("Channel Msg fields missing");
			}
		}catch (Exception e){
			LOGGER.error("Msg parsing error. not a Channel Msg " , e);
			return;
		}
		try{
			Object res =  handle(msg);
			LOGGER.info("Handle pubsub result "+res);
		}catch (Exception e){
			LOGGER.error("handle msg error "+e.getMessage() , e);
		}
	}

	private Object handle(ChannelMsg msg) throws Exception{
		switch (msg.context){
			case IN_MEM_CACHE:
				return MDCacheMgr.handlePubSub(msg.data);
			default:
				throw new Exception("Channel Message received for Unknown Context "+ msg.context);
		}

	}

}
