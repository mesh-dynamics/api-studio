package com.cube.injection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DynamicInjectionConfig {

	@JsonProperty("version")
	public final String version;

	@JsonProperty("customer")
	public final String customer;

	@JsonProperty("app")
	public final String app;

	@JsonProperty("timestamp")
	public final Instant timestamp;

	@JsonProperty("extraction")
	public final List<ExtractionMeta> extractionMetas;

	@JsonProperty("injection")
	public final List<InjectionMeta> injectionMetas;

	// Default constructor for Jackson
	private DynamicInjectionConfig() {
		version = "";
		customer = "";
		app = "";
		timestamp = Instant.now();
		extractionMetas = new ArrayList<>();
		injectionMetas = new ArrayList<>();
	}

	public DynamicInjectionConfig(String version, String customer, String app, Optional<Instant> timestamp,
		List<ExtractionMeta> extractionMetas, List<InjectionMeta> injectionMetas) {
		this.version = version;
		this.customer = customer;
		this.app = app;
		this.timestamp = timestamp.orElse(Instant.now());
		this.extractionMetas = extractionMetas;
		this.injectionMetas = injectionMetas;
	}

	public class ExtractionMeta {

		@JsonProperty("apiPath")
		public final String apiPath;

		@JsonProperty("method")
		public final HTTPMethodType method;

		@JsonProperty("name")
		public final String name;

		@JsonProperty("value")
		public final String value;

		@JsonProperty("reset")
		public final boolean reset;

		private ExtractionMeta() {
			apiPath = "";
			method = HTTPMethodType.POST;
			name = "";
			value = "";
			reset = true;
		}

	}

	public class InjectionMeta {

		@JsonProperty("apiPath")
		public final List<String> apiPaths;

		@JsonProperty("jsonPath")
		public final String jsonPath;

		@JsonProperty("injectAllPaths")
		public final boolean injectAllPaths;

		@JsonProperty("value")
		public final String value;

		public InjectionMeta() {
			this.apiPaths = Collections.EMPTY_LIST;
			this.jsonPath = "";
			this.injectAllPaths = false;
			this.value = "";
		}
	}

	public enum HTTPMethodType {
		GET,
		POST
	}


}