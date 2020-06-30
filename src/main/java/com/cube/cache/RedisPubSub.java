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
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import com.cube.dao.ReqRespStore;
import com.cube.utils.Constants;

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
		if (channel.endsWith("expired") && message.startsWith(Constants.REDIS_SHADOW_KEY_PREFIX)) {
			LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Received Key Expiry Message",
				"CHANNEL", channel, "MESSAGE" , message)));
			String actualKey = message.split(":")[1];
			try (Jedis jedis = jedisPool.getResource()) {
				String existingRecordOrReplay = jedis.get(actualKey);
				jedis.del(actualKey);
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
					} else {
						LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
							"No status key in redis, setting status to error"
							, Constants.REPLAY_ID_FIELD, replay.replayId)));
						replay.status = ReplayStatus.Error;
					}
					rrStore.saveReplay(replay);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			//ReqRespStore.deleteRecording()
		}
	}
}
