package io.md.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.md.core.Comparator.MatchType;
import io.md.core.CompareTemplate;
import io.md.cryptography.EncryptionAlgorithm;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */
public interface DataObj {

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
	void collectKeyVals(Function<String, Boolean> filter, Collection<String> vals);

	@JsonIgnore
	MatchType compare(DataObj rhs, CompareTemplate template);

	//TODO leaving it out from here
	//DataObj applyTransform(DataObj rhs, List<ReqRespUpdateOperation> operationList);

	@JsonIgnore
	boolean wrapAsString(String path, String mimetype);

	@JsonIgnore
	Optional<String> encryptField(String path, EncryptionAlgorithm encrypter);

	@JsonIgnore
	Optional<String> decryptField(String path, EncryptionAlgorithm decrypter);

	@JsonIgnore
	<T> T getValAsObject(String path, Class<T> className) throws PathNotFoundException
		, DataObjProcessingException;

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


	class DataObjCreationException extends RuntimeException {
		public DataObjCreationException(String msg, Throwable e) {
			super (msg,e);
		}

		public DataObjCreationException(Throwable e) {
			super(e);
		}
	}
}