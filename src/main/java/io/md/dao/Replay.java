package io.md.dao;

import java.net.URLClassLoader;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.md.constants.ReplayStatus;
import io.md.core.CollectionKey;
import io.md.core.ReplayTypeEnum;
import io.md.core.Validate;

public class Replay {

	public String endpoint;
	public String customerId;
	public String app;
	public String instanceId;
	public List<String> collection;
	public String userId;
	public List<String> reqIds;
	public String templateVersion;
	public String replayId;
	public boolean async;
	public ReplayStatus status;
	public List<String> service;
	public List<String> paths;
	public List<String> intermediateServices;
	public int reqcnt;
	public int reqsent;
	public int reqfailed;
	public Optional<Double> sampleRate;
	public Instant creationTimeStamp;
	public Optional<String> generatedClassJarPath;
	public ReplayTypeEnum replayType;
	public List<String> mockServices;
	public Optional<String> testConfigName;
	public Optional<String> goldenName;
	public Optional<String> recordingId;
	public boolean archived;
	public boolean excludePaths;
	public Optional<String> xfms;
	public transient Optional<URLClassLoader> generatedClassLoader;
	public Optional<String> dynamicInjectionConfigVersion;
	public Optional<String> staticInjectionMap;
	public Instant analysisCompleteTimestamp;
	public String runId;
	public boolean tracePropagation = true;
	public boolean storeToDatastore = false;
	public Optional<ReplayContext> replayContext = Optional.empty();
	@JsonIgnore
	public final CollectionKey collectionKey;

	public Replay(String endpoint, String customerId, String app, String instanceId,
		List<String> collection, String userId, List<String> reqIds,
		String replayId, boolean async, String templateVersion, ReplayStatus status,
		List<String> paths, boolean excludePaths, int reqcnt, int reqsent, int reqfailed,
		Instant creationTimestamp,
		Optional<Double> sampleRate, List<String> intermediateServices,
		Optional<String> generatedClassJarPath, Optional<URLClassLoader> classLoader,
		List<String> service, ReplayTypeEnum replayType, Optional<String> xfms,
		List<String> mockServices,
		Optional<String> testConfigName, Optional<String> goldenName, Optional<String> recordingId,
		boolean archived, Optional<String> dynamicInjectionConfigVersion,
		Instant analysisCompleteTimestamp, Optional<String> staticInjectionMap, String runId,
		boolean tracePropagation , boolean storeToDatastore ) {
		this.endpoint = endpoint;
		this.customerId = customerId;
		this.app = app;
		this.instanceId = instanceId;
		if(collection==null || collection.isEmpty()) throw new IllegalArgumentException("colllection can not be null or empty "+collection==null ? null : "empty");
		this.collection = collection;
		this.userId = userId;
		this.reqIds = reqIds;
		this.replayId = replayId;
		this.async = async;
		this.templateVersion = templateVersion;
		this.status = status;
		this.paths = paths;
		this.excludePaths = excludePaths;
		this.reqcnt = reqcnt;
		this.reqsent = reqsent;
		this.reqfailed = reqfailed;
		this.creationTimeStamp = creationTimestamp;
		this.xfms = xfms;
		this.sampleRate = sampleRate;
		this.intermediateServices = intermediateServices;
		this.generatedClassJarPath = generatedClassJarPath;
		this.service = service;
		this.replayType = replayType;
		this.generatedClassLoader = classLoader;
		this.mockServices = mockServices;
		this.testConfigName = testConfigName;
		this.goldenName = goldenName;
		this.recordingId = recordingId;
		this.archived = archived;
		this.dynamicInjectionConfigVersion = dynamicInjectionConfigVersion;
		this.analysisCompleteTimestamp = analysisCompleteTimestamp;
		this.staticInjectionMap = staticInjectionMap;
		this.runId = runId != null ? runId : this.replayId;
		this.tracePropagation = tracePropagation;
		this.storeToDatastore = storeToDatastore;
		this.collectionKey = new CollectionKey(customerId, app, instanceId);
	}

	//for deserialization
	public Replay() {
		endpoint = "";
		customerId = "";
		app = "";
		instanceId = "";
		collection = Collections.emptyList();
		userId = "";
		replayId = "";
		async = false;
		sampleRate = Optional.empty();
		creationTimeStamp = Instant.now();
		reqIds = Collections.emptyList();
		paths = Collections.emptyList();
		service = Collections.emptyList();
		intermediateServices = Collections.emptyList();
		templateVersion = "";
		generatedClassJarPath = Optional.empty();
		replayType = ReplayTypeEnum.HTTP;
		mockServices = Collections.emptyList();
		testConfigName = Optional.empty();
		goldenName = Optional.empty();
		recordingId = Optional.empty();
		archived = false;
		excludePaths = false;
		xfms = Optional.empty();
		dynamicInjectionConfigVersion = Optional.empty();
		analysisCompleteTimestamp = null;
		staticInjectionMap = null;
		runId = "";
		this.collectionKey = new CollectionKey(customerId, app, instanceId);
	}

	@JsonIgnore
	public String getCurrentRecording(){
		return replayContext.flatMap(ctx->ctx.currentCollection).orElse(collection.get(0));
	}

}