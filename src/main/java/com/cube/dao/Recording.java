/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import com.cube.utils.Constants;

/**
 * @author prasad
 */
public class Recording {

	private static final Logger LOGGER = LogManager.getLogger(Recording.class);

	public enum RecordingStatus {
		Running,
		Completed,
		Error
	}

	/**
	 * @param customerId
	 * @param app
	 * @param instanceId
	 * @param collection
	 * @param status
	 * @param templateVersion
	 */
	public Recording(String id, String customerId, String app, String instanceId, String collection,
		RecordingStatus status, Optional<Instant> updateTimestamp, String templateVersion,
		Optional<String> parentRecordingId, String rootRecordingId, String name
		, Optional<String> codeVersion, Optional<String> branch, List<String> tags
		, boolean archived, Optional<String> gitCommitId, Optional<String> collectionUpdOpSetId
		, Optional<String> templateUpdOpSetId, Optional<String> comment, String userId,
		Optional<String> generatedClassJarPath, Optional<URLClassLoader> generatedClassLoader) {

		super();
		this.customerId = customerId;
		this.app = app;
		this.instanceId = instanceId;
		this.collection = collection;
		this.status = status;
		this.updateTimestamp = updateTimestamp;
		this.templateVersion = templateVersion;
		this.id = id;
		this.parentRecordingId = parentRecordingId;
		this.rootRecordingId = rootRecordingId;
		this.name = name;
		this.codeVersion = codeVersion;
		this.branch = branch;
		this.tags = tags;
		this.archived = archived;
		this.gitCommitId = gitCommitId;
		this.collectionUpdOpSetId = collectionUpdOpSetId;
		this.templateUpdOpSetId = templateUpdOpSetId;
		this.comment = comment;
		this.userId = userId;
		this.generatedClassJarPath = generatedClassJarPath;
		this.generatedClassLoader = generatedClassLoader;
	}

	// for json deserialization
	public Recording() {
		super();
		this.id = "";
		this.customerId = "";
		this.app = "";
		this.instanceId = "";
		this.collection = "";
		this.templateVersion = "";
		this.parentRecordingId = Optional.empty();
		this.rootRecordingId = "";
		this.name = "";
		this.codeVersion = Optional.empty();
		this.branch = Optional.empty();
		this.tags = Collections.EMPTY_LIST;
		this.archived = false;
		this.gitCommitId = Optional.empty();
		this.collectionUpdOpSetId = Optional.empty();
		this.templateUpdOpSetId = Optional.empty();
		this.comment = Optional.empty();
		this.userId = "";
		this.generatedClassJarPath = Optional.empty();
	}

	@JsonProperty("id")
	public final String id;
	@JsonProperty("cust")
	public final String customerId;
	@JsonProperty("app")
	public final String app;
	@JsonProperty("instance")
	public final String instanceId;
	@JsonProperty("collec")
	public final String collection; // unique within a (customerid, app)
	@JsonProperty("status")
	public RecordingStatus status;
	@JsonProperty("timestmp")
	public Optional<Instant> updateTimestamp;
	@JsonProperty("templateVer")
	public final String templateVersion;
	@JsonProperty("rootRcrdngId")
	public final String rootRecordingId;
	@JsonProperty("prntRcrdngId")
	public final Optional<String> parentRecordingId;
	@JsonProperty("name")
	public final String name;
	@JsonProperty("codeVersion")
	public final Optional<String> codeVersion;
	@JsonProperty("branch")
	public final Optional<String> branch;
	@JsonProperty("tags")
	public final List<String> tags;
	@JsonProperty("archived")
	public boolean archived;
	@JsonProperty("gitCommitId")
	public final Optional<String> gitCommitId;
	@JsonProperty("collectionUpdOpSetId")
	public final Optional<String> collectionUpdOpSetId;
	@JsonProperty("templateUpdOpSetId")
	public final Optional<String> templateUpdOpSetId;
	@JsonProperty("comment")
	public final Optional<String> comment;
	@JsonProperty("userId")
	public final String userId;
	@JsonProperty("jarPath")
	public Optional<String> generatedClassJarPath;
	public transient Optional<URLClassLoader> generatedClassLoader;


	@JsonSetter
	public void setGeneratedClassJarPath(Optional<String> jarPathOpt) {
		this.generatedClassJarPath = jarPathOpt;
		generatedClassJarPath.ifPresent(jarPath -> {
			try {
				Path path = Paths.get(jarPath);
				this.generatedClassLoader = Optional.of(new URLClassLoader(
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

	public String getId() {
		return this.id;
	}


	public static Optional<Recording> startRecording(Recording recording, ReqRespStore rrstore) {
		if (rrstore.saveRecording(recording)) {
			return Optional.of(recording);
		}
		return Optional.empty();
	}

	public static Recording stopRecording(Recording recording, ReqRespStore rrstore) {
		if (recording.status == RecordingStatus.Running) {
			recording.status = RecordingStatus.Completed;
			recording.updateTimestamp = Optional.of(Instant.now());
			rrstore.saveRecording(recording);
		}
		return recording;
	}

	public Recording softDeleteRecording(ReqRespStore rrstore)
		throws RecordingSaveFailureException {
		this.archived = true;
		this.updateTimestamp = Optional.of(Instant.now());
		boolean success = rrstore.saveRecording(this);
		if (!success) {
			throw new RecordingSaveFailureException("Cannot delete recording");
		}
		return this;
	}

	public static class RecordingSaveFailureException extends Exception {

		public RecordingSaveFailureException(String message) {
			super(message);
		}

	}

	public static class RecordingWithSameNamePresent extends Exception {

		public RecordingWithSameNamePresent(String message) {
			super(message);
		}

	}
}
