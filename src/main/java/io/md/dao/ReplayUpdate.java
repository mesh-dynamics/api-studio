/*
 *
 *    Copyright Cube I O
 *
 */
package io.md.dao;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.md.dao.Event.EventType;
import io.md.services.DSResult;
import io.md.services.DataStore;

import io.md.core.BatchingIterator;
import io.md.utils.Constants;

/**
 * @author prasad
 *
 */
public class ReplayUpdate {

    private static final Logger LOGGER = LogManager.getLogger(ReplayUpdate.class);

	@JsonSetter
	public void setGeneratedClassJarPath(Replay replay, Optional<String> jarPathOpt){
		replay.generatedClassJarPath = jarPathOpt;
		replay.generatedClassJarPath.ifPresent(jarPath -> {
			try {
				Path path = Paths.get(jarPath);
				replay.generatedClassLoader = Optional.of(new URLClassLoader(
					new URL[]{path.toUri().toURL()},
					this.getClass().getClassLoader()
				));
			} catch (Exception e) {
				LOGGER.error(new
					ObjectMessage(Map.of(Constants.MESSAGE, "Unable to initialize URL Class Loader",
					Constants.JAR_PATH_FIELD, jarPath)));
			}
		});
	}


	static final String uuidPatternStr = "\\b[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-\\b[0-9a-fA-F]{12}\\b";
	static final String replayIdPatternStr = "^(.*)-" + uuidPatternStr + "$";
	private static final Pattern replayIdPattern = Pattern.compile(replayIdPatternStr);

	public static String getReplayIdFromCollection(String collection) {
		return String.format("%s-%s", collection, UUID.randomUUID().toString());
	}

	@JsonIgnore
	public static Pair<Stream<List<Event>>, Long> getRequestBatchesUsingEvents(int batchSize, DataStore dataStore,
                                                                               Replay replay) {
        DSResult<Event> requests = getEventResult(dataStore, replay , false, replay.reqIds);
        return Pair.of(BatchingIterator.batchedStreamOf(requests.getObjects(), batchSize), requests.getNumFound());
    }

	@JsonIgnore
	public static Stream<Event> getResponseEvents(DataStore dataStore, Replay replay, List<String> reqIds) {
		DSResult<Event> response = getEventResult(dataStore, replay , true, reqIds);
		return response.getObjects();
	}

	private static DSResult<Event> getEventResult(DataStore dataStore, Replay replay, boolean requireResponse, List<String> reqIds) {
		EventType eventType = EventType.mapType(EventType.fromReplayType(replay.replayType), requireResponse);
		EventQuery eventQuery = new EventQuery.Builder(replay.customerId, replay.app, eventType)
			/*.withRunType(Event.RunType.Record)*/.withReqIds(reqIds).withPaths(replay.paths)
            .withExcludePaths(replay.excludePaths)
			.withCollection(replay.getCurrentRecording())
			.withServices(replay.service)
			.withoutScoreOrder()
			.withTimestampAsc(true)
			.build();
		return dataStore.getEvents(eventQuery);
	}

    public static Replay softDeleteReplay(DataStore dataStore, Replay replay) throws ReplaySaveFailureException {
			replay.archived = true;
			boolean success = dataStore.saveReplay(replay);
			if (!success) {
				throw new ReplaySaveFailureException("Cannot archive Replay for replayId=" + replay.replayId);
			}
			return replay;
		}
		public static class ReplaySaveFailureException extends Exception {
			public ReplaySaveFailureException(String message) {
				super(message);
			}
		}
}
