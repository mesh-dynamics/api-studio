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

import static io.md.constants.Constants.DEFAULT_TEMPLATE_VER;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.md.utils.UtilException;
import io.md.constants.ReplayStatus;
import io.md.core.ReplayTypeEnum;

public class ReplayBuilder {

	private static final Logger LOGGER = LogManager.getLogger(ReplayBuilder.class);


	private String replayEndpoint;
	private String customerId;
	private String app;
	private String instanceId;
	private List<String> collection;
	private String userId;
	//very specific to HTTP requests (the entire filtering can
	//be done just based on paths)
	private List<String> pathsToReplay;
	// particular reqIds to replay (if present the rest of the filtering
	// won't matter much)
	private List<String> reqIdsToReplay;
	private boolean excludePaths;
	private String replayId;
	private boolean async;
	private ReplayStatus replayStatus;
	private Optional<Double> sampleRate;
	List<String> intermediateServices;
	Optional<String> generatedClassJarPath;
	List<String> serviceToReplay;
	Optional<URLClassLoader> classLoader;
	ReplayTypeEnum replayType;
	int reqCnt;
	int reqSent;
	int reqFailed;
	private Instant creationTimestamp;
	public Optional<String> xfms;
    public List<String> mockServices;
	public Optional<String> testConfigName;
	public Optional<String> goldenName;
	public Optional<String> recordingId;
	public boolean archived;
	public Optional<String> dynamicInjectionConfigVersion;
	public Optional<String> staticInjectionMap;
	public Instant analysisCompleteTimestamp;
	public String runId;
	public boolean tracePropagation = true;
	public Optional<ReplayContext> replayContext = Optional.empty();
	public boolean storeToDatastore = false;
	public String templateSetName;
	public String templateSetLabel;


	public ReplayBuilder(String endpoint, String customerId, String app, String instanceId,
                         String collection, String userId) {
		this(endpoint , customerId , app , instanceId , List.of(collection) , userId);
	}
	public ReplayBuilder(String endpoint, String customerId, String app, String instanceId,
		List<String> collection, String userId) {
		this.replayEndpoint = endpoint;
		this.customerId = customerId;
		this.app = app;
		this.instanceId = instanceId;
		if(collection.isEmpty()) throw new IllegalArgumentException("Collection is Empty");
		this.collection = collection ;
		this.userId = userId;
		this.pathsToReplay = Collections.EMPTY_LIST;
		this.excludePaths = false;
		this.reqIdsToReplay = Collections.EMPTY_LIST;
		this.replayId = ReplayUpdate.getReplayIdFromCollection(collection.get(0));
		this.replayStatus = ReplayStatus.Init;
		async = false;
		sampleRate = Optional.empty();
		this.intermediateServices = Collections.emptyList();
		this.generatedClassJarPath = Optional.empty();
		this.serviceToReplay = Collections.emptyList();
		// Default replay type as HTTP
		this.replayType = ReplayTypeEnum.HTTP;
		reqCnt = 0;
		reqFailed = 0;
		reqSent = 0;
		this.creationTimestamp = Instant.now();
		this.xfms = Optional.empty();
		this.mockServices = Collections.emptyList();
		this.testConfigName = Optional.empty();
		this.goldenName = Optional.empty();
		this.recordingId = Optional.empty();
		this.archived = false;
		this.dynamicInjectionConfigVersion = Optional.empty();
		this.staticInjectionMap = Optional.empty();
		/**
		 * the value is set to the EPOCH so we won't be able to fetch replay in timeline till its analyis is complete
		 * Once analysis is complete the value is set to the corresponding time
		 */
		this.analysisCompleteTimestamp = Instant.EPOCH;
		this.runId = null;
	}


	private void populateClassLoader() throws Exception {
		generatedClassJarPath.ifPresent(UtilException.rethrowConsumer(jarPath -> {
			Path path = Paths.get(jarPath);
			this.classLoader = Optional.of(new URLClassLoader(
				new URL[]{path.toUri().toURL()},
				this.getClass().getClassLoader()
			));
		}));
	}

	public Replay build() {
		Replay replay = new Replay(replayEndpoint, customerId, app, instanceId, collection, userId,
				reqIdsToReplay, replayId, async, replayStatus, pathsToReplay,
				excludePaths, reqCnt , reqSent , reqFailed, creationTimestamp, sampleRate, intermediateServices,
				generatedClassJarPath, classLoader, serviceToReplay, replayType, xfms, mockServices
				, testConfigName, goldenName, recordingId, archived,dynamicInjectionConfigVersion,
				analysisCompleteTimestamp, staticInjectionMap, runId, tracePropagation , storeToDatastore,
			templateSetName, templateSetLabel);
		return replay;
	}

	public ReplayBuilder withPaths(List<String> paths) {
		this.pathsToReplay = paths;
		return this;
	}

	public ReplayBuilder withExcludePaths(Boolean excludePaths) {
		this.excludePaths = excludePaths;
		return this;
	}


	public ReplayBuilder withReqIds(List<String> reqIds) {
		this.reqIdsToReplay = reqIds;
		return this;
	}

	public ReplayBuilder withReplayId(String replayId) {
		this.replayId = replayId;
		return this;
	}

	public ReplayBuilder withAsync(boolean async) {
		this.async = async;
		return this;
	}

	public ReplayBuilder withReplayStatus(ReplayStatus replayStatus) {
		this.replayStatus = replayStatus;
		return this;
	}

	public ReplayBuilder withSampleRate(Double sampleRate) {
		this.sampleRate = Optional.of(sampleRate);
		return this;
	}

	public ReplayBuilder withIntermediateServices(List<String> services) {
		this.intermediateServices = services;
		return this;
	}

	public ReplayBuilder withGeneratedClassJar(String jarPath) throws Exception {
		this.generatedClassJarPath = Optional.of(jarPath);
		populateClassLoader();
		return this;
	}

	public ReplayBuilder withServiceToReplay(List<String> service) {
		this.serviceToReplay = service;
		return this;
	}

	public ReplayBuilder withReplayType(ReplayTypeEnum replayType) {
		this.replayType = replayType;
		return this;
	}

	public ReplayBuilder withReqCounts(int reqCnt, int reqSent, int reqFailed) {
		this.reqCnt = reqCnt;
		this.reqSent = reqSent;
		this.reqFailed = reqFailed;
		return this;
	}

	public ReplayBuilder withCreationTimestamp(Instant creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
		return this;
	}

	/**
	 * @param xfms - the json string form of the transformation - multivalued map of {key : [{src,
	 *             tgt}+]} in a string representation
	 * @return
	 */
	public ReplayBuilder withXfms(String xfms) {
	    this.xfms = Optional.of(xfms);
		return this;
	}

	public ReplayBuilder withMockServices(List<String> mockServices) {
		this.mockServices = mockServices;
		return this;
	}

	public ReplayBuilder withTestConfigName(String testConfigName) {
		this.testConfigName = Optional.of(testConfigName);
		return this;
	}

	public ReplayBuilder withGoldenName(String goldenName) {
		this.goldenName = Optional.of(goldenName);
		return this;
	}

	public ReplayBuilder withRecordingId(String recordingId) {
		this.recordingId = Optional.of(recordingId);
		return this;
	}

	public ReplayBuilder withArchived(boolean archived) {
		this.archived = archived;
		return this;
	}

	public ReplayBuilder withDynamicInjectionConfigVersion(String injectionConfigVersion) {
		this.dynamicInjectionConfigVersion = Optional.of(injectionConfigVersion);
		return this;
	}

	public ReplayBuilder withStaticInjectionMap(String staticInjectionMap) {
		this.staticInjectionMap = Optional.of(staticInjectionMap);
		return this;
	}

	public ReplayBuilder withAnalysisCompleteTimestamp(Instant analysisCompleteTimestamp) {
    	this.analysisCompleteTimestamp = analysisCompleteTimestamp;
    	return this;
	}

	public ReplayBuilder withRunId(String runId) {
		this.runId = runId;
		return this;
	}

	public ReplayBuilder withTracePropagation(boolean tracePropagation){
		this.tracePropagation = tracePropagation;
		return this;
	}

	public ReplayBuilder withStoreToDatastore(boolean store){
		this.storeToDatastore = store;
		return this;
	}
	//storeToDatastore

	public ReplayBuilder withTemplateSetName(String name) {
		this.templateSetName = name;
		return this;
	}

	public ReplayBuilder withTemplateSetLabel(String label) {
		this.templateSetLabel  = label;
		return this;
	}

	public String getReplayId() {
		return this.replayId;
	}

}
