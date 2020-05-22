package io.md.dao;

import java.net.URLClassLoader;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.constants.Constants;
import io.md.dao.Event.RunType;

/*
 * Created by IntelliJ IDEA.
 * Date: 21/05/20
 */
public class RecordOrReplay {

	@JsonIgnore
	public Optional<String> getCollection() {
		// Note that replayId is the collection for replay requests/responses
		// replay.collection refers to the original collection
		// return replay collection if non empty, else return recording collection
		Optional<String> ret = replay.map(replay -> replay.replayId);
		if (ret.isPresent()) {
			return ret;
		} else {
			return recording.map(recording -> recording.collection);
		}
	}

	@JsonIgnore
	public Optional<String> getRecordingCollection() {
		// return collection of recording corresponding to replay if non empty, else return recording collection
		Optional<String> ret = replay.map(replay -> replay.collection);
		if (ret.isPresent()) {
			return ret;
		} else {
			return recording.map(recording -> recording.collection);
		}
	}

	@JsonIgnore
	public Optional<String> getReplayId() {
		return replay.map(replay -> replay.replayId);
	}

	@JsonIgnore
	public Optional<String> getRecordingId() {
		return recording.map(recording -> recording.id);
	}

	@JsonIgnore
	public boolean isRecording() {
		return recording.isPresent();
	}

	@JsonIgnore
	public RunType getRunType(){
		return recording.isPresent() ? RunType.Record : RunType.Replay;
	}

	@JsonIgnore
	public String getTemplateVersion() {
		return replay.map(replay1 -> replay1.templateVersion)
			.orElseGet(() -> recording.map(recording1 -> recording1.templateVersion).orElse(
				Constants.DEFAULT_TEMPLATE_VER));
	}

	@JsonIgnore
	public Optional<URLClassLoader> getClassLoader() {
		// TODO add replay logic as well
		Optional<URLClassLoader> ret = replay.flatMap(replay1 -> replay1.generatedClassLoader);
		if (ret.isPresent()) {
			return ret;
		} else {
			return recording.flatMap(rec -> rec.generatedClassLoader);
		}
	}


	// for json de-serialization
	public RecordOrReplay() {
		super();
		replay = Optional.empty();
		recording = Optional.empty();
	}

	/**
	 *
	 */
	private RecordOrReplay(Optional<Recording> recording, Optional<Replay> replay) {
		super();
		this.replay = replay;
		this.recording = recording;
	}

	private RecordOrReplay(Recording recording) {
		this(Optional.of(recording), Optional.empty());
	}

	private RecordOrReplay(Replay replay) {
		this(Optional.empty(), Optional.of(replay));
	}

	public static RecordOrReplay createFromRecording(Recording recording) {
		RecordOrReplay rr = new RecordOrReplay(recording);
		return rr;
	}

	public static RecordOrReplay createFromReplay(Replay replay) {
		RecordOrReplay rr = new RecordOrReplay(replay);
		return rr;
	}

	@Override
	public String toString() {
		return getCollection()
			.orElse(getRecordingCollection().orElse("N/A"));
	}

	@JsonProperty("recording")
	public final Optional<Recording> recording;
	@JsonProperty("replay")
	public final Optional<Replay> replay;
}
