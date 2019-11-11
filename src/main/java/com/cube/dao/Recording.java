/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author prasad
 *
 */
public class Recording {

    //private static final Logger LOGGER = LogManager.getLogger(Recording.class);

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
	public Recording(String customerId, String app, String instanceId, String collection, RecordingStatus status
        , Optional<Instant> updateTimestamp, String templateVersion, Optional<String> parentRecordingId
        , Optional<String> rootRecordingId, Optional<String> name, Optional<String> codeVersion, Optional<String> branch
        , List<String> tags) {
		super();
		this.customerId = customerId;
		this.app = app;
		this.instanceId = instanceId;
		this.collection = collection;
		this.status = status;
		this.updateTimestamp = updateTimestamp;
        this.templateVersion = templateVersion;
        this.id = ReqRespStoreSolr.Types.Recording.toString().concat("-").concat(String.valueOf(Objects.hash(customerId, app,
            collection, templateVersion)));
        this.parentRecordingId = parentRecordingId;
        this.rootRecordingId = rootRecordingId.orElse(this.id);
        this.name = name;
        this.codeVersion = codeVersion;
        this.branch = branch;
        this.tags = tags;
        this.archived = false;
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
        this.name = Optional.empty();
        this.codeVersion = Optional.empty();
        this.branch = Optional.empty();
        this.tags = Collections.EMPTY_LIST;
        this.archived = false;
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
    public final Optional<String> name;
    @JsonProperty("codeVersion")
    public final Optional<String> codeVersion;
    @JsonProperty("branch")
    public final Optional<String> branch;
    @JsonProperty("tags")
    public final List<String> tags;
    @JsonProperty("archived")
    public boolean archived;



    public String getId() {
        return this.id;
    }


	public static Optional<Recording> startRecording(String customerId, String app, String instanceId, String collection,
                                                     String templateVersion, ReqRespStore rrstore, Optional<String> name,
                                                     Optional<String> codeVersion, Optional<String> branch, List<String> tags) {
		Recording recording = new Recording(customerId, app, instanceId, collection, RecordingStatus.Running
            , Optional.of(Instant.now()), templateVersion, Optional.empty(), Optional.empty(), name, codeVersion, branch, tags);
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

}
