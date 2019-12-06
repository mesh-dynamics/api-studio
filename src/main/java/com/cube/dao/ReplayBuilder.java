package com.cube.dao;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.cube.dao.Replay.ReplayStatus;
import com.cube.utils.Constants;
import com.cube.utils.ReplayTypeEnum;

public class ReplayBuilder {

	private String replayEndpoint;
	private String customerId;
	private String app;
	private String instanceId;
	private String collection;
	private String userId;
	private String templateSetVersion;
	//very specific to HTTP requests (the entire filtering can
	//be done just based on paths)
	private List<String> pathsToReplay;
	// particular reqIds to replay (if present the rest of the filtering
	// won't matter much)
	private List<String> reqIdsToReplay;
	private String replayId;
	private boolean async;
	private ReplayStatus replayStatus;
	private Optional<Double> sampleRate;
	List<String> intermediateServices;
	Optional<String> generatedClassJarPath;
	Optional<String> serviceToReplay;
	ReplayTypeEnum replayType;

	public ReplayBuilder (String endpoint, CubeMetaInfo metaInfo,
		String collection, String userId) {
		this.replayEndpoint = endpoint;
		this.customerId = metaInfo.customerId;
		this.app = metaInfo.app;
		this.instanceId = metaInfo.instance;
		this.collection = collection;
		this.userId = userId;
		this.templateSetVersion = Constants.DEFAULT_TEMPLATE_VER;
		this.pathsToReplay = Collections.EMPTY_LIST;
		this.reqIdsToReplay = Collections.EMPTY_LIST;
		this.replayId = Replay.getReplayIdFromCollection(collection);
		this.replayStatus = ReplayStatus.Init;
		async = false;
		sampleRate = Optional.empty();
		this.generatedClassJarPath = Optional.empty();
		this.serviceToReplay = Optional.empty();
		// Default replay type as HTTP
		this.replayType = ReplayTypeEnum.HTTP;
	}

	public Replay build() {
		/**
		 * public Replay(String endpoint, String customerId, String app, String instanceId,
		 * 		String collection, String userId
		 * 		, List<String> reqIds,
		 * 		String replayId, boolean async, String templateVersion, ReplayStatus status,
		 * 		List<String> paths, int reqcnt, int reqsent, int reqfailed, Instant creationTimestamp,
		 * 		Optional<Double> sampleRate, List<String> intermediateServices,
		 * 		Optional<String> generatedClassJarPath, Optional<String> service)
		 */
		return new Replay(replayEndpoint, customerId, app, instanceId, collection, userId,
			reqIdsToReplay, replayId, async, templateSetVersion, replayStatus, pathsToReplay,
			0 , 0 , 0, Instant.now(), sampleRate, intermediateServices,
			generatedClassJarPath, serviceToReplay, replayType);
	}

	public ReplayBuilder withPaths(List<String> paths) {
		this.pathsToReplay = paths;
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

	public ReplayBuilder withTemplateSetVersion(String templateVersion) {
		this.templateSetVersion = templateVersion;
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

	public ReplayBuilder withSampleRate(Optional<Double> sampleRate) {
		this.sampleRate = sampleRate;
		return this;
	}

	public ReplayBuilder withIntermediateServices(List<String> services) {
		this.intermediateServices  = services;
		return this;
	}

	public ReplayBuilder withGeneratedClassJar(Optional<String> jarPath) {
		this.generatedClassJarPath = jarPath;
		return this;
	}

	public ReplayBuilder withServiceToReplay(Optional<String> service) {
		this.serviceToReplay = service;
		return this;
	}

	public ReplayBuilder withReplayType(ReplayTypeEnum replayType) {
		this.replayType = replayType;
		return this;
	}

}
