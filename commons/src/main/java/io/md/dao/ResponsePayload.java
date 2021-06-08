package io.md.dao;

import java.util.Arrays;
import java.util.List;

import io.md.constants.Constants;

public interface ResponsePayload extends Payload {

	String getStatusCode();

	default List<String> getPayloadFields() {
		return Arrays.asList(String.format("%s:%s", Constants.STATUS_PATH, getStatusCode()));
	}

}
