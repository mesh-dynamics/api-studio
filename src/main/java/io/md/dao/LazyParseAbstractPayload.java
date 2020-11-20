package io.md.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.md.core.WrapUnwrapContext;
import io.md.logger.LogMgr;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.core.Comparator.MatchType;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.utils.CubeObjectMapperProvider;

public abstract class LazyParseAbstractPayload implements Payload {

	private static final Logger LOGGER = LogMgr.getLogger(LazyParseAbstractPayload.class);

	@JsonIgnore
	public JsonDataObj dataObj;
	@JsonIgnore
	protected ObjectMapper mapper;

	public LazyParseAbstractPayload() {
		this.mapper = CubeObjectMapperProvider.getInstance();
	}

	public LazyParseAbstractPayload(JsonNode objRoot) {
		this.mapper = CubeObjectMapperProvider.getInstance();
		this.dataObj = new JsonDataObj(objRoot, mapper);
	}

	@Override
	@JsonIgnore
	abstract  public byte[] rawPayloadAsByteArray()
		throws NotImplementedException, RawPayloadEmptyException;

	@Override
	@JsonIgnore
	abstract  public String rawPayloadAsString()
		throws NotImplementedException, RawPayloadProcessingException, RawPayloadEmptyException;

	@Override
	@JsonIgnore
	public abstract  boolean isRawPayloadEmpty();

	@JsonIgnore
	public abstract  void postParse();

	@JsonIgnore
	// This is default implementation. Can be overriden by specific payload types
	public void parseIfRequired() {
		if (this.dataObj == null) {
			this.dataObj = new JsonDataObj(this, mapper);
			postParse();
		}
	}

	@JsonIgnore
	// This is default implementation. Can be overriden by specific payload types
	public void reParse() {
			// dataObj needs to be set to null before setting it to new JsonDataObj
			// otherwise the custom deserializer will prioritize the existing dataObj while deserializing
			this.dataObj = null;
			this.dataObj = new JsonDataObj(this, mapper);
			postParse();
	}

	@Override
	public boolean isLeaf() {
		parseIfRequired();
		return dataObj.isLeaf();
	}

	@Override
	public boolean isDataObjEmpty() {
		return dataObj == null || dataObj.isDataObjEmpty();
	}

	@Override
	public DataObj getVal(String path) {
		parseIfRequired();;
		return dataObj.getVal(path);
	}

	@Override
	public String getValAsString(String path) throws PathNotFoundException {
		parseIfRequired();
		return dataObj.getValAsString(path);
	}

	public byte[] getValAsByteArray(String path) throws PathNotFoundException {
		parseIfRequired();
		return dataObj.getValAsByteArray(path);
	}

	@Override
	public void collectKeyVals(Function<String, Boolean> filter, Map<String, String> vals) {
		parseIfRequired();
		dataObj.collectKeyVals(filter, vals);
	}

	@Override
	public MatchType compare(DataObj rhs, CompareTemplate template) {
		parseIfRequired();
		return dataObj.compare(rhs, template);
	}

	@Override
	public boolean wrapAsString(String path, String mimetype, Optional<WrapUnwrapContext> wrapContext) {
		parseIfRequired();
		return dataObj.wrapAsString(path, mimetype, wrapContext);
	}

	@Override
	public boolean wrapAsEncoded(String path, String mimetype, Optional<WrapUnwrapContext> wrapContext) {
		parseIfRequired();
		return dataObj.wrapAsEncoded(path, mimetype, wrapContext);
	}

	@Override
	public Optional<Object> encryptField(String path, EncryptionAlgorithm encrypter) {
		parseIfRequired();
		return dataObj.encryptField(path, encrypter);

	}

	@Override
	public  Optional<String> decryptField(String path, EncryptionAlgorithm decrypter) {
		parseIfRequired();
		return dataObj.decryptField(path, decrypter);
	}

	@Override
	public void getPathRules(CompareTemplate template, Map<String, TemplateEntry> vals) {
		// Using json pointer to handle proper escaping in case keys have special characters
		parseIfRequired();
		dataObj.getPathRules(template, vals);
	}

	@Override
	public String serializeDataObj() throws DataObjProcessingException {
		if (dataObj != null) {
			return dataObj.serializeDataObj();
		} else {
			throw new DataObjProcessingException("Data Object is null");
		}
	}

	@Override
	public <T> Optional<T> getValAsObject(String path, Class<T> className) {
		parseIfRequired();
		return dataObj.getValAsObject(path , className);
	}

	@Override
	public DataObj applyTransform(DataObj rhs, List<ReqRespUpdateOperation> operationList) {
		parseIfRequired();
		return dataObj.applyTransform(rhs, operationList);
	}

	@Override
	public void transformSubTree(String path, Function<String, String> transformFunction) {
		parseIfRequired();
		this.dataObj.transformSubTree(path, transformFunction);
	}


	@Override
	public Payload applyTransform(Payload rhs, List<ReqRespUpdateOperation> operationList) {
		if (rhs instanceof  LazyParseAbstractPayload) {
			parseIfRequired();
			this.dataObj = (JsonDataObj) dataObj.
				applyTransform(((LazyParseAbstractPayload)rhs).dataObj, operationList);
		}
		return this;
	}

	@Override
	public long size() {
		return 0;
	}

	@Override
	public void updatePayloadBody() throws PathNotFoundException {
		return;
	}

	@Override
	public  void replaceContent(List<String> pathsToKeep, String path, long maxSize) {
		 this.dataObj.replaceContent(this.dataObj.objRoot.at(path), pathsToKeep, path, 0, maxSize);
	}

	public boolean put(String path, DataObj value) throws PathNotFoundException {
		parseIfRequired();
		return dataObj.put(path, value);
	}

	@JsonIgnore
	public String getPayloadAsJsonString() {
		return getPayloadAsJsonString(false);
	}

	@JsonIgnore
	public ConvertEventPayloadResponse checkAndConvertResponseToString(boolean wrapForDisplay,
		List<String> pathsToKeep, long size, String path) {
		ConvertEventPayloadResponse response = new ConvertEventPayloadResponse();
		try {
			this.updatePayloadBody();
			if (this.size() > size) {
				this.replaceContent(pathsToKeep, path, size);
				response.setTruncated(true);
			}
		} catch (Exception e) {
			LOGGER.error("Error while updating payload body", e);
		}
		response.setResponse(this.getPayloadAsJsonString(wrapForDisplay));
		return response;
	}

	@JsonIgnore
	public String getPayloadAsJsonString(boolean wrapForDisplay) {
		if (!isRawPayloadEmpty()) {
			try {
				return this.rawPayloadAsString(wrapForDisplay);
			} catch (Exception e) {
				LOGGER.error("Error while "
					+ "converting payload to json string", e);
			}
		}
		return "";
	}
}

