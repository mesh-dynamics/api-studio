package io.cube.agent;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.google.gson.internal.$Gson$Preconditions;

import io.cube.agent.EncryptionConfig.APIPathMeta;
import io.cube.agent.EncryptionConfig.JSONPathMeta;
import io.md.constants.Constants;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.cryptography.EncryptionAlgorithmFactory;
import io.md.dao.DataObj;
import io.md.dao.Event;

public class Utils {

	static DataObj encryptFields(CommonConfig commonConfig, Event event) {

		DataObj payload = null;

		if(commonConfig.encryptionConfig.isPresent()) {
			EncryptionConfig encryptionConfig = commonConfig.encryptionConfig.get();

			if (encryptionConfig.services.containsKey((event.service))) {
				Map<String, APIPathMeta> apiPaths = encryptionConfig.services
					.get(event.service).apiPathMetaMap;
				if (apiPaths.containsKey(event.apiPath)) {
					Map<String, JSONPathMeta> jsonPathMetas = apiPaths
						.get(event.apiPath).JSONPathMap;
					for (String jsonPath : jsonPathMetas.keySet()) {
						JSONPathMeta algoDetails = jsonPathMetas.get(jsonPath);
						String algoName = algoDetails.algorithm;
						Map<String, Object> metaDataMap = (Map<String, Object>) algoDetails.metaData;
						JSONObject metaData = new JSONObject(metaDataMap);

						EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithmFactory
							.build(algoName, encryptionConfig.passPhrase, metaData);

						Map<String, Object> params = new HashMap<>();
						params.put(Constants.OBJECT_MAPPER, commonConfig.jsonMapper);
						payload = event.getPayload(params);
						payload.encryptField(jsonPath, encryptionAlgorithm);
					}
					;
				}
			}
		}
	return payload;
	}



}

