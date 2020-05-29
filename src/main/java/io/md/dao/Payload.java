package io.md.dao;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME,
	property = "type")
@JsonSubTypes({
	@Type(value = HTTPRequestPayload.class),
	@Type(value = HTTPResponsePayload.class),
	@Type(value = JsonPayload.class),
	@Type(value = JsonByteArrayPayload.class),
	@Type(value = FnReqRespPayload.class)
})
public interface Payload extends DataObj, RawPayload {

	Payload applyTransform(Payload rhs, List<ReqRespUpdateOperation> operationList);
	long size();
	void updatePayloadBody() throws PathNotFoundException;
	void replaceContent(List<String> pathsToKeep, String path);

}
