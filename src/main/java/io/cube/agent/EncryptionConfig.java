package io.cube.agent;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EncryptionConfig {
	@JsonProperty("services")
	private final Map<String, ServiceMeta> services;

	@JsonProperty("passPhrase")
	private final String passPhrase;

	// Default constructor for Jackson
	private EncryptionConfig() {
		services = null;
		passPhrase = null;
	}

	public EncryptionConfig(
		Map<String, ServiceMeta> services, String passPhrase) {
		this.services = services;
		this.passPhrase = passPhrase;
	}

	public Optional<ServiceMeta> getServiceMeta(String service) {
		return Optional.ofNullable(services.get(service));
	}

	public String getPassPhrase() {
		return passPhrase;
	}

	static public class JSONPathMeta {
		@JsonProperty("algorithm")
		private final String algorithm;

		@JsonProperty("metaData")
		private final Map<String, Object> metaData;

		public JSONPathMeta(String algorithm,
			Map<String, Object> metaData) {
			this.algorithm = algorithm;
			this.metaData = metaData;
		}

		public String getAlgorithm() {
			return algorithm;
		}

		public Map<String, Object> getMetaData() {
			return metaData;
		}

		// Default constructor for Jackson
		private JSONPathMeta() {
			algorithm = null;
			metaData = null;
		}


	}

	static public class APIPathMeta {

		public Map<String, JSONPathMeta> getJSONPathMap() {
			return JSONPathMap;
		}

		@JsonProperty("JSONPathMap")
		private final Map<String, JSONPathMeta> JSONPathMap;

		public APIPathMeta(
			Map<String, JSONPathMeta> jsonPathMap) {
			JSONPathMap = jsonPathMap;
		}

		// Default constructor for Jackson
		private APIPathMeta() {
			JSONPathMap = null;
		}
	}

	static public class ServiceMeta {

		public ServiceMeta(
			Map<String, APIPathMeta> apiPathMetaMap) {
			this.apiPathMetaMap = apiPathMetaMap;
		}

		// Default constructor for Jackson
		private ServiceMeta() {
			apiPathMetaMap = null;
		}

		public Optional<APIPathMeta> getApiPathMeta(String apiPath) {
			return Optional.ofNullable(apiPathMetaMap.get(apiPath));
		}

		@JsonProperty("apiPathMetaMap")
		private final Map<String, APIPathMeta> apiPathMetaMap;
	}

}