/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.time.Instant;
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

    public static final String DEFAULT_TEMPLATE_VER = "DEFAULT";

	/**
     * @param customerId
     * @param app
     * @param instanceId
     * @param collection
     * @param status
     * @param templateVersionOpt
     */
	public Recording(String customerId, String app, String instanceId, String collection, RecordingStatus status
        , Optional<Instant> updateTimestamp, Optional<String> templateVersionOpt, Optional<String> parentRecordingId
        , Optional<String> rootRecordingId) {
		super();
		this.customerId = customerId;
		this.app = app;
		this.instanceId = instanceId;
		this.collection = collection;
		this.status = status;
		this.updateTimestamp = updateTimestamp;
        this.templateVersion = templateVersionOpt.or(() -> Optional.of(Recording.DEFAULT_TEMPLATE_VER));/*.orElse(DEFAULT_TEMPLATE_VER)*/;
        this.id = ReqRespStoreSolr.Types.Recording.toString().concat("-").concat(String.valueOf(Objects.hash(customerId, app,
            collection, templateVersion)));
        this.parentRecordingId = parentRecordingId;
        this.rootRecordingId = rootRecordingId;
    }

	// for json deserialization
	public Recording() {
	    super();
	    this.id = "";
	    this.customerId = "";
	    this.app = "";
	    this.instanceId = "";
	    this.collection = "";
	    this.templateVersion = Optional.empty();
	    this.parentRecordingId = Optional.empty();
	    this.rootRecordingId = Optional.empty();
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
	public final Optional<String> templateVersion;
    @JsonProperty("rootRcrdngId")
    public final Optional<String> rootRecordingId;
    @JsonProperty("prntRcrdngId")
    public final Optional<String> parentRecordingId;

    public String getId() {
        return this.id;
    }


	public static Optional<Recording> startRecording(String customerId, String app, String instanceId,
                                                     String collection, Optional<String> templateSetId, ReqRespStore rrstore) {
		Recording recording = new Recording(customerId, app, instanceId, collection, RecordingStatus.Running
            , Optional.of(Instant.now()), templateSetId, Optional.empty(), Optional.empty());
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
