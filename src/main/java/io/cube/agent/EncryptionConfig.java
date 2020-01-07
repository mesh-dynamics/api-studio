package io.cube.agent;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EncryptionConfig {
	@JsonProperty("services")
	Map<String, ServiceMeta> services;

	@JsonProperty("passPhrase")
	String passPhrase;

	static public class JSONPathMeta {
		String algorithm;

		public String getAlgorithm() {
			return algorithm;
		}

		public void setAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}

		public Map<String, Object> getMetaData() {
			return metaData;
		}

		public void setMetaData(Map<String, Object> metaData) {
			this.metaData = metaData;
		}

		Map<String, Object> metaData;
	}

	static public class APIPathMeta {
		public Map<String, JSONPathMeta> JSONPathMap;
	}

	static public class ServiceMeta {
		public Map<String, APIPathMeta> apiPathMetaMap;
	}

}