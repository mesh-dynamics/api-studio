/**
 * Copyright Cube I O
 */
package io.md.dao;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * @author prasad
 */
public class Recording {

	private static final Logger LOGGER = LoggerFactory.getLogger(Recording.class);

	public enum RecordingStatus {
		Running,
		Completed,
		Error
	}

	public enum RecordingType {
		Golden,
		UserGolden,
		Capture,
		History
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
		Optional<String> generatedClassJarPath, Optional<URLClassLoader> generatedClassLoader, String label,
			RecordingType recordingType) {

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
		this.label = label;
		this.recordingType = recordingType;
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
		this.label="";
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
    @JsonProperty("label")
    public final String label;
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
	@JsonProperty("recordingType")
	public RecordingType recordingType;
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
				LOGGER.error("Unable to initialize URL Class Loader: " +
					jarPath);
			}
		});
	}

	public String getId() {
		return this.id;
	}


    public static class RecordingSaveFailureException extends Exception {

		public RecordingSaveFailureException(String message) {
			super(message);
		}

	}

}
