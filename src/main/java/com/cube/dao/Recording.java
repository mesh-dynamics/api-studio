/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.util.Optional;

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
        , Optional<Long> updateTimestamp) {
		super();
		this.customerid = customerid;
		this.app = app;
		this.instanceid = instanceid;
		this.collection = collection;
		this.status = status;
		this.updateTimestamp = updateTimestamp;
	}
	
	public final String customerid;
	public final String app;
	public final String instanceid;
	public final String collection; // unique within a (customerid, app)
	public RecordingStatus status;
	public Optional<Long> updateTimestamp;

	public static Optional<Recording> startRecording(String customerid, String app, String instanceid, 
			String collection, ReqRespStore rrstore) {
		Recording recording = new Recording(customerid, app, instanceid, collection, RecordingStatus.Running
            , Optional.of(System.currentTimeMillis()));
		if (rrstore.saveRecording(recording))
			return Optional.of(recording);
		return Optional.empty();
	}

	public static Recording stopRecording(Recording recording, ReqRespStore rrstore) {
		if (recording.status == RecordingStatus.Running) {
			recording.status = RecordingStatus.Completed;
			recording.updateTimestamp = Optional.of(System.currentTimeMillis());
			rrstore.saveRecording(recording);
		}
		return recording;
	}

}
