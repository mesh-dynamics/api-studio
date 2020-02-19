package io.md.dao;

import io.md.constants.ReplayStatus;
import io.md.constants.ReplayTypeEnum;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Replay {
	public  String endpoint;
	public  String customerId;
	public  String app;
	public  String instanceId;
	public  String collection;
	public  String userId;
	public  List<String> reqIds;
	public  String templateVersion;
	public  String replayId;
	public  boolean async;
	public ReplayStatus status;
	public  Optional<String> service;
	public  List<String> paths;
	public  List<String> intermediateServices;
	public int reqcnt;
	public int reqsent;
	public int reqfailed;
	public  Optional<Double> sampleRate;
	public  Instant creationTimeStamp;
	public Optional<String> generatedClassJarPath;
	public  ReplayTypeEnum replayType;
	
	public Replay(String endpoint, String customerId, String app, String instanceId,
					String collection, String userId, List<String> reqIds,
					String replayId, boolean async, String templateVersion, ReplayStatus status,
					List<String> paths, int reqcnt, int reqsent, int reqfailed, Instant creationTimestamp,
					Optional<Double> sampleRate, List<String> intermediateServices,
					Optional<String> generatedClassJarPath, Optional<String> service,
					ReplayTypeEnum replayType) {
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
	    this.reqcnt = reqcnt;
	    this.reqsent = reqsent;
	    this.reqfailed = reqfailed;
	    this.creationTimeStamp = creationTimestamp;
	    this.sampleRate = sampleRate;
	    this.intermediateServices = intermediateServices;
	    this.generatedClassJarPath = generatedClassJarPath;
	    this.service = service;
	    this.replayType = replayType;
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
	}

}