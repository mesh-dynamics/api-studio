/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cube.pubsub;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.cache.Constants.PubSubContext;
import io.md.utils.CubeObjectMapperProvider;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import com.cube.ws.Config.JedisConnResourceProvider;

public class PubSubMgr {

	Logger LOGGER = LogManager.getLogger(PubSubMgr.class);
	private final JedisConnResourceProvider jedisPool;
	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	public PubSubMgr(JedisConnResourceProvider jedisPool){

		this.jedisPool = jedisPool;
	}

	public Long publish(PubSubContext context , Map data){

		try(Jedis jedis = jedisPool.getResource()) {
			String strMsg =  jsonMapper.writeValueAsString(new ChannelMsg(context , data));
			LOGGER.info("Publishing Msg "+strMsg);
			return jedis.publish(PubSubChannel.MD_PUBSUB_CHANNEL_NAME, strMsg);
		} catch (JsonProcessingException e) {
			LOGGER.error("Publish Msg Json Serialization error "+e.getMessage() , e);
			return -1L;
		}
	}


}
