package io.cube.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.cube.agent.EncryptionConfig.JSONPathMeta;
import io.md.constants.Constants;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.cryptography.EncryptionAlgorithmFactory;
import io.md.dao.DataObj;
import io.md.dao.Event;

public class Utils {

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
}

