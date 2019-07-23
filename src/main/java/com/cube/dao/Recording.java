/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.time.Instant;
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
	 * @param customerid
	 * @param app
	 * @param instanceid
	 * @param collection
	 * @param status
	 */
	Recording(String customerid, String app, String instanceid, String collection, RecordingStatus status
        , Optional<Instant> updateTimestamp) {
		super();
		this.customerid = customerid;
		this.app = app;
		this.instanceid = instanceid;
		this.collection = collection;
		this.status = status;
		this.updateTimestamp = updateTimestamp;
	}

	// for json deserialization
	public Recording() {
	    super();
	    this.customerid = "";
	    this.app = "";
	    this.instanceid = "";
	    this.collection = "";
    }

	@JsonProperty("cust")
	public final String customerid;
    @JsonProperty("app")
	public final String app;
    @JsonProperty("instance")
	public final String instanceid;
    @JsonProperty("collec")
    public final String collection; // unique within a (customerid, app)
    @JsonProperty("status")
    public RecordingStatus status;
    @JsonProperty("timestmp")
	public Optional<Instant> updateTimestamp;

	public static Optional<Recording> startRecording(String customerid, String app, String instanceid, 
			String collection, ReqRespStore rrstore) {
		Recording recording = new Recording(customerid, app, instanceid, collection, RecordingStatus.Running
            , Optional.of(Instant.now()));
		if (rrstore.saveRecording(recording))
			return Optional.of(recording);
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
