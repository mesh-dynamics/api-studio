package io.cube.agent;


import java.net.URLEncoder;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
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

	/**
	 * Reference : https://golb.hplar.ch/2019/01/java-11-http-client.html
	 * @param data
	 * @return
	 */
	public static BodyPublisher ofFormData(Map<Object, Object> data) {
		var builder = new StringBuilder();
		for (Map.Entry<Object, Object> entry : data.entrySet()) {
			if (builder.length() > 0) {
				builder.append("&");
			}
			builder
				.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
			builder.append("=");
			builder
				.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
		}
		return BodyPublishers.ofString(builder.toString());
	}
}

