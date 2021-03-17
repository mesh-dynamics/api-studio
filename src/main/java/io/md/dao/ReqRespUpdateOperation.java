package io.md.dao;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReqRespUpdateOperation {
	@JsonProperty("op")
	public OperationType operationType;
	@JsonProperty("path")
	public final String jsonpath;
	@JsonProperty("value")
	public Object value;
	@JsonProperty("eventType")
	public Type eventType;
	@JsonProperty("method")
	public Optional<String> method;

	public enum Type {
		Request,
		Response;
	}

	public enum OperationType {

		ADD,
		REPLACE,
		REMOVE,
	}

	@JsonCreator
	public ReqRespUpdateOperation(@JsonProperty("op") OperationType operationType,
		@JsonProperty("path") String jsonpath) {
		this.operationType = operationType;
		this.jsonpath = jsonpath;
		this.value = null;
		this.method = Optional.empty();
	}

	@Override
	public String toString() {
		return "ReqRespUpdateOperation{" +
			"operationType=" + operationType +
			", jsonpath='" + jsonpath + '\'' +
			", value=" + value +
			" , method=" + method +
			'}';
	}

	public String key() {
		return jsonpath.concat("::").concat(eventType.name());
	}
}
