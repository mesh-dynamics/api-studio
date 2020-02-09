package io.cube.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.cube.agent.EncryptionConfig.JSONPathMeta;
import io.cube.agent.samplers.Sampler;
import io.md.constants.Constants;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.cryptography.EncryptionAlgorithmFactory;
import io.md.dao.DataObj;
import io.md.dao.Event;

public class Utils {

	private static final Logger LOGGER = LogManager.getLogger(Utils.class);

	static Optional<DataObj> encryptFields(CommonConfig commonConfig, Event event) {

		Optional<DataObj> payload = commonConfig.encryptionConfig.flatMap(encryptionConfig -> {
			return encryptionConfig.getServiceMeta(event.service).flatMap(services -> {
				return services.getApiPathMeta(event.apiPath).flatMap(apiPathMeta -> {
					Map<String, JSONPathMeta> jsonPathMetas = apiPathMeta.getJSONPathMap();

					Map<String, Object> params = new HashMap<>();
					params.put(Constants.OBJECT_MAPPER, commonConfig.jsonMapper);
					DataObj eventPayload = event.getPayload(params);

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
				});
			});
		});
		return payload;
	}

	public static Optional<Sampler> getSampler(float samplingRate, int samplingAccuracy) {
		if (samplingRate == 0) {
			return Optional.of(Sampler.NEVER_SAMPLE);
		}
		if (samplingRate == 1.0) {
			return Optional.of(Sampler.ALWAYS_SAMPLE);
		}
		if (samplingRate < 1.0f / samplingAccuracy || samplingRate > 1.0) {
			LOGGER.error("The sampling rate must be between 1/samplingAccuracy and 1.0");
			return Optional.of(Sampler.ALWAYS_SAMPLE);
		}
		return Optional.empty();
	}
}

