package com.cube.injection;

import java.time.Instant;
import java.util.ArrayList;
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
		final String apiPath;

		@JsonProperty("method")
		final HTTPMethodType method;

		@JsonProperty("name")
		final SourceMeta nameMeta;

		@JsonProperty("value")
		final SourceMeta valueMeta;

		@JsonProperty("reset")
		final boolean reset;

		private ExtractionMeta() {
			apiPath = "";
			method = HTTPMethodType.POST;
			nameMeta = new SourceMeta();
			valueMeta = new SourceMeta();
			reset = true;
		}

	}

	public class SourceMeta {

		@JsonProperty("source")
		final VariableSources source;

		@JsonProperty("jsonPath")
		final String jsonPath;

		@JsonProperty("suffix")
		final String suffix;

		private SourceMeta() {
			source = VariableSources.GoldenResponse;
			jsonPath = "";
			suffix = "";
		}

	}

	//TODO Define injection class
	public class InjectionMeta {

	}

	public enum HTTPMethodType {
		GET,
		POST
	}

	public enum VariableSources {
		GoldenRequest,
		GoldenResponse,
		TestSetRequest,
		TestSetResponse
	}

}