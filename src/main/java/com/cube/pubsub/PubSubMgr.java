package com.cube.pubsub;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.cache.Constants.PubSubContext;
import io.md.utils.CubeObjectMapperProvider;

import com.cube.ws.Config.JedisConnResourceProvider;

public class PubSubMgr {

	Logger LOGGER = LogManager.getLogger(PubSubMgr.class);
	private final JedisConnResourceProvider jedisPool;
	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	public PubSubMgr(JedisConnResourceProvider jedisPool){
		this.jedisPool = jedisPool;
	}

	public Long publish(PubSubContext context , Map data){

		try {
			String strMsg =  jsonMapper.writeValueAsString(new ChannelMsg(context , data));
			LOGGER.info("Publishing Msg "+strMsg);
			return jedisPool.getResource().publish(PubSubChannel.MD_PUBSUB_CHANNEL_NAME, strMsg);
		} catch (JsonProcessingException e) {
			LOGGER.error("Publish Msg Json Serialization error "+e.getMessage() , e);
			return -1L;
		}
	}


}
