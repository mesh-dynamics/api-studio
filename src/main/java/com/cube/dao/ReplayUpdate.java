/*
 *
 *    Copyright Cube I O
 *
 */
package com.cube.dao;

import com.cube.core.BatchingIterator;
import com.cube.utils.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.EventQuery;
import io.md.dao.Replay;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
	public static Pair<Stream<List<Event>>, Long> getRequestBatchesUsingEvents(int batchSize, ReqRespStore rrstore, Replay replay) {
        Result<Event> requests = getEventResult(rrstore, replay);
        return Pair.of(BatchingIterator.batchedStreamOf(requests.getObjects(), batchSize), requests.numFound);
    }

	private static Result<Event> getEventResult(ReqRespStore rrstore, Replay replay) {
		EventQuery eventQuery = new EventQuery.Builder(replay.customerId, replay.app, EventType.fromReplayType(replay.replayType))
			.withRunType(Event.RunType.Record).withReqIds(replay.reqIds).withPaths(replay.paths)
            .withExcludePaths(replay.excludePaths)
			.withCollection(replay.collection)
			.withServices(replay.service.map(List::of).orElse(Collections.emptyList())).withSortOrderAsc(true).build();
		return rrstore.getEvents(eventQuery);
	}

    public static Replay softDeleteReplay(ReqRespStore rrstore, Replay replay) throws ReplaySaveFailureException {
			replay.archived = true;
			boolean success = rrstore.saveReplay(replay);
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
