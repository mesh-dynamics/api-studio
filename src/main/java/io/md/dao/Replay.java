package io.md.dao;

import io.md.constants.ReplayStatus;
import io.md.core.ReplayTypeEnum;

import java.net.URLClassLoader;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Replay {

	public String endpoint;
	public String customerId;
	public String app;
	public String instanceId;
	public String collection;
	public String userId;
	public List<String> reqIds;
	public String templateVersion;
	public String replayId;
	public boolean async;
	public ReplayStatus status;
	public Optional<String> service;
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
	public Optional<RRTransformer> xfmer;
	public transient Optional<URLClassLoader> generatedClassLoader;
	public Optional<String> dynamicInjectionConfigVersion;

	public Replay(String endpoint, String customerId, String app, String instanceId,
		String collection, String userId, List<String> reqIds,
		String replayId, boolean async, String templateVersion, ReplayStatus status,
		List<String> paths, boolean excludePaths, int reqcnt, int reqsent, int reqfailed,
		Instant creationTimestamp,
		Optional<Double> sampleRate, List<String> intermediateServices,
		Optional<String> generatedClassJarPath, Optional<URLClassLoader> classLoader,
		Optional<String> service, ReplayTypeEnum replayType, Optional<String> xfms,
		Optional<RRTransformer> xfmer, List<String> mockServices,
		Optional<String> testConfigName, Optional<String> goldenName, Optional<String> recordingId,
		boolean archived, Optional<String> dynamicInjectionConfigVersion) {
		this.endpoint = endpoint;
		this.customerId = customerId;
		this.app = app;
		this.instanceId = instanceId;
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
		this.xfmer = xfmer;
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
	}

	//for deserialization
	public Replay() {
		endpoint = "";
		customerId = "";
		app = "";
		instanceId = "";
		collection = "";
		userId = "";
		replayId = "";
		async = false;
		sampleRate = Optional.empty();
		creationTimeStamp = Instant.now();
		reqIds = Collections.emptyList();
		paths = Collections.emptyList();
		service = Optional.empty();
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
		xfmer = Optional.empty();
		dynamicInjectionConfigVersion = Optional.empty();
	}

}