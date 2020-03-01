package io.md.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.core.Comparator.MatchType;
import io.md.core.CompareTemplate;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.utils.CubeObjectMapperProvider;

public abstract class LazyParseAbstractPayload implements Payload {

	@JsonIgnore
	protected JsonDataObj dataObj;
	@JsonIgnore
	protected ObjectMapper mapper;

	public LazyParseAbstractPayload() {
		this.mapper = CubeObjectMapperProvider.getInstance();
	}

	@Override
	abstract  public byte[] rawPayloadAsByteArray()
		throws NotImplementedException, RawPayloadEmptyException;

	@Override
	abstract  public String rawPayloadAsString()
		throws NotImplementedException, RawPayloadProcessingException, RawPayloadEmptyException;

	@Override
	public abstract  boolean isRawPayloadEmpty();

	public abstract  void postParse();

	public void parseIfRequired() {
		if (this.dataObj == null) {
			this.dataObj = new JsonDataObj(this, mapper);
			postParse();
		}
	}

	@JsonIgnore
	abstract public void syncFromDataObj() throws PathNotFoundException, DataObjProcessingException;

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
	public void collectKeyVals(Function<String, Boolean> filter, Collection<String> vals) {
		parseIfRequired();
		dataObj.collectKeyVals(filter, vals);
	}

	@Override
	public MatchType compare(DataObj rhs, CompareTemplate template) {
		parseIfRequired();
		return dataObj.compare(rhs, template);
	}

	//TODO leaving it out from here
	//DataObj applyTransform(DataObj rhs, List<ReqRespUpdateOperation> operationList);

	@Override
	public boolean wrapAsString(String path, String mimetype) {
		parseIfRequired();
		return dataObj.wrapAsString(path, mimetype);
	}

	@Override
	public boolean wrapAsByteArray(String path, String mimetype) {
		parseIfRequired();
		return dataObj.wrapAsByteArray(path, mimetype);
	}

	@Override
	public Optional<String> encryptField(String path, EncryptionAlgorithm encrypter) {
		parseIfRequired();
		return dataObj.encryptField(path, encrypter);

	}

	@Override
	public  Optional<String> decryptField(String path, EncryptionAlgorithm decrypter) {
		parseIfRequired();
		return dataObj.decryptField(path, decrypter);
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
	public <T> T convertToType(Class<T> tClass) {
		parseIfRequired();
		return dataObj.convertToType(tClass);
	}
}
