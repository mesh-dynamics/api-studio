package io.cube.agent;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cube.agent.EncryptionConfig.JSONPathMeta;
import io.cube.agent.samplers.AdaptiveSampler;
import io.cube.agent.samplers.Attributes;
import io.cube.agent.samplers.BoundarySampler;
import io.cube.agent.samplers.CountingSampler;
import io.cube.agent.samplers.Sampler;
import io.cube.agent.samplers.SamplerConfig;
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

	public static PrintWriter nullPrintWriter(Logger logger) {
		return new PrintWriter(
		new OutputStream() {
			private volatile boolean closed;
			private Logger LOGGER = logger;

			private void ensureOpen() throws IOException {
				if (closed) {
					throw new IOException("Stream closed");
				}
			}

			public void discardLog() {
				LOGGER.info("nullOutputStream: Discarding the write content");
			}

			@Override
			public void write(int b) throws IOException {
				ensureOpen();
				discardLog();
			}

			@Override
			public void write(byte b[], int off, int len) throws IOException {
				if (off < 0 || len < 0 || (off + len) > b.length) {
					throw new IndexOutOfBoundsException(" buffer with length [" + b.length + "], "
						+ "with range from ["
						+ off + "], length [" + len + "]");
				}

				ensureOpen();
				discardLog();
			}

			@Override
			public void close() {
				closed = true;
			}
		});
	}

	public static Sampler initSampler(Optional<SamplerConfig> samplerConfig) {
		if (!samplerConfig.isPresent()) {
			LOGGER.debug("Invalid config file, not sampling!");
			return Sampler.NEVER_SAMPLE;
		}
		SamplerConfig config = samplerConfig.get();
		if (!validateSamplerConfig(config)) {
			return Sampler.NEVER_SAMPLE;
		}
		return createSampler(samplerConfig.get());

	}

	public static boolean validateSamplerConfig(SamplerConfig config) {
		String type = config.getType();
		Optional<Integer> accuracy = config.getAccuracy();
		Optional<Float> rate = config.getRate();
		Optional<String> fieldCategory = config.getFieldCategory();
		Optional<List<Attributes>> attributes = config.getAttributes();

		if (type == null) {
			LOGGER.debug("Sampler Type missing!");
			return false;
		}

		if ((type.equalsIgnoreCase(SimpleSampler.TYPE)
			|| type.equalsIgnoreCase(CountingSampler.TYPE))
			&& (!rate.isPresent() || !accuracy.isPresent())) {
			LOGGER.debug("Need sampling rate/accuracy for Simple/Counting Samplers!");
			return false;
		}

		if (type.equalsIgnoreCase(BoundarySampler.TYPE)
			&& (!rate.isPresent() || !accuracy.isPresent()
			|| !fieldCategory.isPresent() || !attributes.isPresent())) {
			LOGGER.debug("Need sampling rate/accuracy/ "
				+ "fieldCategory/attributes for Boundary Sampler!");
			return false;
		}

		if (type.equalsIgnoreCase(AdaptiveSampler.TYPE)
			&& (!fieldCategory.isPresent() || !attributes.isPresent())) {
			LOGGER.debug("Need field category/attributes "
				+ "for Adaptive Sampler!");
			return false;
		}

		return true;
	}

	public static Sampler createSampler(SamplerConfig samplerConfig) {
		String type = samplerConfig.getType();
		Optional<Integer> accuracy = samplerConfig.getAccuracy();
		Optional<Float> rate = samplerConfig.getRate();
		Optional<String> fieldCategory = samplerConfig.getFieldCategory();
		Optional<List<Attributes>> attributes = samplerConfig.getAttributes();

		if (SimpleSampler.TYPE.equalsIgnoreCase(type)) {
			return SimpleSampler.create(rate.get(), accuracy.get());
		}

		if (CountingSampler.TYPE.equalsIgnoreCase(type)) {
			return CountingSampler.create(rate.get(), accuracy.get());
		}

		//This sampler takes only a list of fields on which the sampling is to be done.
		//Specific values are not looked at.
		if (BoundarySampler.TYPE.equalsIgnoreCase(type)) {
			List<String> samplingParams = new ArrayList<>();
			for (Attributes attr : attributes.get()) {
				if (attr.getField() == null || attr.getField().trim().isEmpty()) {
					LOGGER.debug("Invalid input, using default sampler ");
					samplingParams.clear();
					return Sampler.NEVER_SAMPLE;
				}
				samplingParams.add(attr.getField());
			}
			return BoundarySampler
				.create(rate.get(), accuracy.get(), fieldCategory.get(), samplingParams);
		}

		//This sampler takes fields, values and specific rates. However currently
		//only one field and multiple values are supported. Not multiple fields.
		//`other` is a special value that can be used to specify rate for every other
		//value that are possible for the specified field. Special value should be the
		//last in the rule hierarchy.
		if (AdaptiveSampler.TYPE.equalsIgnoreCase(type)) {
			//ordered map to allow special value `other` at the end.
			Map<Pair<String, String>, Float> samplingParams = new LinkedHashMap<>();
			for (Attributes attr : attributes.get()) {
				if (attr.getField() == null || attr.getField().trim().isEmpty()
					|| !attr.getValue().isPresent() || !attr.getRate().isPresent()) {
					LOGGER.debug("Invalid input, using default sampler ");
					samplingParams.clear();
					return Sampler.NEVER_SAMPLE;
				}

				samplingParams.put(new ImmutablePair<>(attr.getField(), attr.getValue().get()),
					attr.getRate().get());
			}
			return AdaptiveSampler.create(fieldCategory.get(), samplingParams);
		}

		LOGGER.error("Invalid sampling strategy, using default sampler , type : ".concat(type));
		return Sampler.NEVER_SAMPLE;
	}

}

