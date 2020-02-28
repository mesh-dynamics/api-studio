package io.md.dao;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

public interface RawPayload {

	@JsonIgnore
	public byte[] rawPayloadAsByteArray() throws NotImplementedException, RawPayloadEmptyException;

	@JsonIgnore
	public String rawPayloadAsString() throws NotImplementedException, RawPayloadEmptyException, RawPayloadProcessingException;

	@JsonIgnore
	public boolean isRawPayloadEmpty();

	static class RawPayloadProcessingException extends Exception {
		public RawPayloadProcessingException(Throwable e) {
			super(e);
		}

		public RawPayloadProcessingException(String msg) {
			super(msg);
		}

		public RawPayloadProcessingException(String msg, Throwable e) {
			super(msg, e);
		}
	}

	static class RawPayloadEmptyException extends Exception {
		public RawPayloadEmptyException(Throwable e) {
			super(e);
		}

		public RawPayloadEmptyException(String msg) {
			super(msg);
		}

		public RawPayloadEmptyException(String msg, Throwable e) {
			super(msg, e);
		}
	}

}
