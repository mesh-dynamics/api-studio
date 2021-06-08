package io.md.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.md.core.Comparator.MatchType;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.core.WrapUnwrapContext;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.logger.LogMgr;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */
public interface DataObj {

	static final Logger LOGGER = LogMgr.getLogger(DataObj.class);

	@JsonIgnore
	boolean isLeaf();

	@JsonIgnore
	boolean isDataObjEmpty();

	@JsonIgnore
	DataObj getVal(String path);

	@JsonIgnore
	String getValAsString(String path) throws PathNotFoundException;

	@JsonIgnore
	String serializeDataObj() throws DataObjProcessingException;

	@JsonIgnore
	void collectKeyVals(Function<String, Boolean> filter, Map<String, String> vals);

	@JsonIgnore
	void transformSubTree(String path, Function<String, String>  transformFunction);

	@JsonIgnore
	MatchType compare(DataObj rhs, CompareTemplate template);

	@JsonIgnore
	boolean wrapAsString(String path, String mimetype, Optional<WrapUnwrapContext> wrapContext);

	@JsonIgnore
	boolean wrapAsEncoded(String path, String mimetype, Optional<WrapUnwrapContext> wrapContext);

	@JsonIgnore
	Optional<Object> encryptField(String path, EncryptionAlgorithm encrypter);

	@JsonIgnore
	Optional<String> decryptField(String path, EncryptionAlgorithm decrypter);

	@JsonIgnore
	void getPathRules(CompareTemplate template, Map<String, TemplateEntry> vals);

	@JsonIgnore
	DataObj applyTransform(DataObj rhs, List<ReqRespUpdateOperation> operationList);

	@JsonIgnore
	<T> Optional<T> getValAsObject(String path, Class<T> className);

	@JsonIgnore
	byte[] getValAsByteArray(String path) throws PathNotFoundException;

	@JsonIgnore
	boolean put(String path, DataObj value) throws PathNotFoundException;

	@JsonIgnore
	default int getChecksum(Optional<CompareTemplate> template){
		int checksum = 0;
		Map<String, String> keyValMap = new HashMap<>();
		List<String> keyVals = new ArrayList<>();
		collectKeyVals(path -> template.map(t->t.getRule(path).getCompareType()
			== CompareTemplate.ComparisonType.Equal).orElse(true) , keyValMap);

		for (Map.Entry<String, String> entry : keyValMap.entrySet()) {
			keyVals.add((entry.getKey() + "=" + entry.getValue()).toLowerCase());
		}
		LOGGER.info("Generating checksum from vals : ".concat(keyVals.toString()));

		if (!keyVals.isEmpty()) {
			checksum = Objects.hash(keyVals.get(0));
		}
		for (int i = 1 ; i < keyVals.size(); i++) {
			checksum ^= Objects.hash(keyVals.get(i));
		}
		LOGGER.info("checksum generated : ".concat(String.valueOf(checksum)));
		return checksum;
	}


	class PathNotFoundException extends Exception{

		public PathNotFoundException() {
			super();
		}

		public PathNotFoundException(String path) {
			super("Path doesn't exist :: " + path);
		}
	}

	class DataObjProcessingException extends Exception {
		public DataObjProcessingException(Throwable rootCause) {
			super(rootCause);
		}

		public DataObjProcessingException(String msg) {
			super(msg);
		}

		public DataObjProcessingException(String msg, Throwable e) {
			super(msg,e);
		}
	}

	class DataObjectCreationException extends Exception {
		public DataObjectCreationException(Throwable rootCause) {super(rootCause);}
		public DataObjectCreationException(String msg) {
			super(msg);
		}
	}

}