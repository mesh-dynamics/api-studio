/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.md.dao;

import java.net.URLClassLoader;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.constants.Constants;
import io.md.core.CollectionKey;
import io.md.dao.Event.RunType;
import io.md.dao.Recording.RecordingType;

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
		Optional<String> ret = replay.map(Replay::getCurrentRecording);
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

	@JsonIgnore
	public RecordingType getRecordingType() {
		return recording.map(r -> r.recordingType).orElse(RecordingType.Replay);
	}

	public Optional<String> getDynamicInjectionConfigVersion(){
		// check in replay first. otherwise in recording
		Optional<String> diCfgVer = replay.flatMap(r->r.dynamicInjectionConfigVersion);
		if(diCfgVer.isPresent()) return diCfgVer;
		return recording.flatMap(r->r.dynamicInjectionConfigVersion);
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

	@JsonIgnore
	public Optional<String> getRunId() {
		Optional<String> ret = replay.map(replay -> replay.runId);
		if (ret.isPresent()) {
			return ret;
		} else {
			return recording.map(recording -> recording.runId);
		}
	}

	@JsonIgnore
	public CollectionKey getCollectionKey(){
		return recording.map(record->new CollectionKey(record.customerId, record.app, record.instanceId)).orElseGet(
			()->replay.map(repl->new CollectionKey(repl.customerId , repl.app , repl.instanceId)).orElseGet (
				()->new CollectionKey("NA" , "NA" , "NA")
			)
		);
	}

	@JsonIgnore
	public Optional<String> getCustomerId(){
		return isRecording() ? recording.map(r->r.customerId) : replay.map(r->r.customerId);
	}




	@JsonProperty("recording")
	public final Optional<Recording> recording;
	@JsonProperty("replay")
	public final Optional<Replay> replay;
}
