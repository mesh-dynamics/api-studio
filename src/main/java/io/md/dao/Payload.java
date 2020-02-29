package io.md.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME,
	property = "type")
@JsonSubTypes({
	@Type(value = HTTPRequestPayload.class),
	@Type(value = HTTPResponsePayload.class),
	@Type(value = StringPayload.class),
	@Type(value = StringAsByteArrayPayload.class),
})
public interface Payload extends DataObj, RawPayload {

	@JsonIgnore
	public void syncFromDataObj() throws PathNotFoundException, DataObjProcessingException;

}
