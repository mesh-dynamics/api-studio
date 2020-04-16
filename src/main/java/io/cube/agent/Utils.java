package io.cube.agent;


import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cube.agent.EncryptionConfig.JSONPathMeta;
import io.cube.agent.samplers.Sampler;
import io.cube.agent.samplers.SimpleSampler;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.cryptography.EncryptionAlgorithmFactory;
import io.md.dao.Event;
import io.md.dao.Payload;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;

public class Utils {

	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

	static Optional<Payload> encryptFields(CommonConfig commonConfig, Event event) {

		return commonConfig.encryptionConfig.flatMap(encryptionConfig ->
			encryptionConfig.getServiceMeta(event.service).flatMap(services ->
				services.getApiPathMeta(event.apiPath).flatMap(apiPathMeta -> {
				Map<String, JSONPathMeta> jsonPathMetas = apiPathMeta.getJSONPathMap();
				Payload eventPayload = event.payload;
				for (String jsonPath : jsonPathMetas.keySet()) {
					JSONPathMeta algoDetails = jsonPathMetas.get(jsonPath);
					String algoName = algoDetails.getAlgorithm();
					Map<String, Object> metaDataMap = algoDetails.getMetaData();
					EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithmFactory
						.build(algoName, encryptionConfig.getPassPhrase(),
							metaDataMap);
					eventPayload.encryptField(jsonPath, encryptionAlgorithm);
				}
				return Optional.of(eventPayload);
			})));
	}

	public static Optional<Sampler> getConstSamplerIfValid(float samplingRate, int samplingAccuracy) {
		if (samplingAccuracy <= 0) {
			samplingAccuracy = SimpleSampler.DEFAULT_SAMPLING_ACCURACY;
		}

		if (samplingRate == 0) {
			return Optional.of(Sampler.NEVER_SAMPLE);
		}
		if (samplingRate == 1.0) {
			return Optional.of(Sampler.ALWAYS_SAMPLE);
		}
		if (samplingRate < 1.0f / samplingAccuracy || samplingRate > 1.0) {
			LOGGER.error("The sampling rate must be between 1/samplingAccuracy and 1.0");
			return Optional.of(Sampler.NEVER_SAMPLE);
		}
		return Optional.empty();
	}

	public static Span createPerformanceSpan(String operationName) {
		return CommonUtils.startClientSpan(operationName,
			! CommonConfig.getInstance().performanceTest);
	}

	public static Span createPerformanceSpan(String operationName, SpanContext parentContext) {
		return CommonUtils.startClientSpan(operationName, parentContext,
			! CommonConfig.getInstance().performanceTest);
	}


	public static Scope activatePerformanceSpan(Span span) {
		return CommonUtils.activateSpan(span ,
			! CommonConfig.getInstance().performanceTest);
	}
}

