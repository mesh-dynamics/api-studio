package io.cube.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;

import com.google.gson.internal.$Gson$Preconditions;

import io.cube.agent.EncryptionConfig.APIPathMeta;
import io.cube.agent.EncryptionConfig.JSONPathMeta;
import io.cube.agent.EncryptionConfig.ServiceMeta;
import io.md.constants.Constants;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.cryptography.EncryptionAlgorithmFactory;
import io.md.dao.DataObj;
import io.md.dao.Event;

public class Utils {

	static Optional<DataObj> encryptFields(CommonConfig commonConfig, Event event) {

		Optional<DataObj> payload = commonConfig.encryptionConfig.map(encryptionConfig -> {
			ServiceMeta services = encryptionConfig.getServices().get((event.service));
			if (services != null) {
				Map<String, APIPathMeta> apiPaths = services.getApiPathMetaMap();
				APIPathMeta apiPathMeta = apiPaths.get(event.apiPath);
				if (apiPathMeta != null) {
					Map<String, JSONPathMeta> jsonPathMetas = apiPathMeta.getJSONPathMap();

					Map<String, Object> params = new HashMap<>();
					params.put(Constants.OBJECT_MAPPER, commonConfig.jsonMapper);
					DataObj eventPayload = event.getPayload(params);

					for (String jsonPath : jsonPathMetas.keySet()) {
						JSONPathMeta algoDetails = jsonPathMetas.get(jsonPath);
						String algoName = algoDetails.getAlgorithm();
						Map<String, Object> metaDataMap = algoDetails.getMetaData();

						EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithmFactory
							.build(algoName, encryptionConfig.getPassPhrase(), new JSONObject(metaDataMap)); //TODO Remove JSONObject conversion when changes are reflected in commons
						eventPayload.encryptField(jsonPath, encryptionAlgorithm);
					}
					return eventPayload;
				}
			}
			return null;
		});
		return payload;
	}



}

