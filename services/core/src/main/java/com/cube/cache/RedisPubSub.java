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

package com.cube.cache;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.constants.ReplayStatus;
import io.md.dao.RecordOrReplay;
import io.md.dao.Recording;
import io.md.dao.Recording.RecordingStatus;
import io.md.dao.Replay;
import io.md.utils.Utils;
import io.md.utils.Constants;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import com.cube.dao.ReqRespStore;

public class RedisPubSub extends JedisPubSub {

	private ReqRespStore rrStore;
	private ObjectMapper jsonMapper;
	private JedisPool jedisPool;

	Logger LOGGER = LogManager.getLogger(RedisPubSub.class);

	public RedisPubSub(ReqRespStore rrStore, ObjectMapper objectMapper, JedisPool jedisPool) {
		this.rrStore = rrStore;
		this.jsonMapper = objectMapper;
		this.jedisPool = jedisPool;
	}


	@Override
	public void onMessage(String channel, String message) {
		super.onMessage(channel, message);
		LOGGER.debug("CHANNEL :: " + channel + " MESSAGE :: " + message);
	}

	@Override
	public void onPMessage(String pattern, String channel, String message) {
		super.onPMessage(pattern, channel, message);
		if(channel==null || message==null){
		    LOGGER.error("null channel/message {}{}", channel , message);
		    return;
        }
		if (channel.endsWith("expired") && message.startsWith(Constants.REDIS_SHADOW_KEY_PREFIX)) {
			LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Received Key Expiry Message",
				"CHANNEL", channel, "MESSAGE" , message)));
			try (Jedis jedis = jedisPool.getResource()) {
			    String actualKey = message.replaceFirst("^" + Constants.REDIS_SHADOW_KEY_PREFIX, "");
                String existingRecordOrReplay = jedis.get(actualKey);
				if (existingRecordOrReplay != null  && !existingRecordOrReplay.equals("nil")) {
					RecordOrReplay recordOrReplay = jsonMapper.readValue(existingRecordOrReplay,
						RecordOrReplay.class);
					if (recordOrReplay.isRecording()) {
						Recording recording = recordOrReplay.recording.get();
						if (recording.status == RecordingStatus.Running) {
							recording.status = RecordingStatus.Completed;
							recording.updateTimestamp = Optional.of(Instant.now());
							LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE,
								"Marking Recording Completed in Solr", Constants.RECORDING_ID,
								recording.id)));
							rrStore.saveRecording(recording);
						}
					} else {
						Replay replay = recordOrReplay.replay.get();
						String statusKey = Constants.REDIS_STATUS_KEY_PREFIX + actualKey;
						String currentStatus = jedis.get(statusKey);
						if (currentStatus != null && !currentStatus.equals("nil")) {
							if (currentStatus.equals(ReplayStatus.Completed.toString())) {
								replay.status = ReplayStatus.Completed;
							} else if (currentStatus.equals(ReplayStatus.Error.toString())) {
								replay.status = ReplayStatus.Error;
							}
							jedis.del(statusKey);
							rrStore.saveReplay(replay);
						} else {
							LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
								"No status key in redis, probably deleted by someone else"
								, Constants.REPLAY_ID_FIELD, replay.replayId)));
						}
					}
					// delete this only after solr is updated above
					jedis.del(actualKey);
				}
            } catch (Throwable e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Quitting subscriber thread")) ,e);
			}
			//ReqRespStore.deleteRecording()
		}
	}
}
