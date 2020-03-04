package io.md.dao;

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
	}

	@Override
	public String toString() {
		return "ReqRespUpdateOperation{" +
			"operationType=" + operationType +
			", jsonpath='" + jsonpath + '\'' +
			", value=" + value +
			'}';
	}

	public String key() {
		return jsonpath.concat("::").concat(eventType.name());
	}
}
