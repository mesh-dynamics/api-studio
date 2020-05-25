package com.cube.dao;

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

import io.cube.agent.UtilException;
import io.md.constants.ReplayStatus;
import io.md.core.ReplayTypeEnum;
import io.md.dao.Replay;

public class ReplayBuilder {

	private static final Logger LOGGER = LogManager.getLogger(ReplayBuilder.class);


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
	private boolean excludePaths;
	private String replayId;
	private boolean async;
	private ReplayStatus replayStatus;
	private Optional<Double> sampleRate;
	List<String> intermediateServices;
	Optional<String> generatedClassJarPath;
	Optional<String> serviceToReplay;
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
	public Instant analysisCompleteTimestamp;

	public ReplayBuilder(String endpoint, CubeMetaInfo metaInfo,
		String collection, String userId) {
		this.replayEndpoint = endpoint;
		this.customerId = metaInfo.customerId;
		this.app = metaInfo.app;
		this.instanceId = metaInfo.instance;
		this.collection = collection;
		this.userId = userId;
		this.templateSetVersion = DEFAULT_TEMPLATE_VER;
		this.pathsToReplay = Collections.EMPTY_LIST;
		this.excludePaths = false;
		this.reqIdsToReplay = Collections.EMPTY_LIST;
		this.replayId = ReplayUpdate.getReplayIdFromCollection(collection);
		this.replayStatus = ReplayStatus.Init;
		async = false;
		sampleRate = Optional.empty();
		this.generatedClassJarPath = Optional.empty();
		this.serviceToReplay = Optional.empty();
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
		/**
		 * the value is set to the EPOCH so we won't be able to fetch replay in timeline till its analyis is complete
		 * Once analysis is complete the value is set to the corresponding time
		 */
		this.analysisCompleteTimestamp = Instant.EPOCH;
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
		return new Replay(replayEndpoint, customerId, app, instanceId, collection, userId,
			reqIdsToReplay, replayId, async, templateSetVersion, replayStatus, pathsToReplay,
            excludePaths, reqCnt , reqSent , reqFailed, creationTimestamp, sampleRate, intermediateServices,
			generatedClassJarPath, classLoader, serviceToReplay, replayType, xfms, mockServices
	            , testConfigName, goldenName, recordingId, archived,dynamicInjectionConfigVersion,
				analysisCompleteTimestamp);
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

	public ReplayBuilder withServiceToReplay(String service) {
		this.serviceToReplay = Optional.of(service);
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

	public ReplayBuilder withAnalysisCompleteTimestamp(Instant analysisCompleteTimestamp) {
    	this.analysisCompleteTimestamp = analysisCompleteTimestamp;
    	return this;
	}

}
