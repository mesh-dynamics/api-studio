package io.md.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.md.core.Comparator.MatchType;
import io.md.core.CompareTemplate;
import io.md.cryptography.EncryptionAlgorithm;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */
public interface DataObj {

	boolean isLeaf();

	boolean isEmpty();

	DataObj getVal(String path);

	String getValAsString(String path) throws PathNotFoundException;

	String serialize();

	void collectKeyVals(Function<String, Boolean> filter, Collection<String> vals);

	MatchType compare(DataObj rhs, CompareTemplate template);

	//TODO leaving it out from here
	//DataObj applyTransform(DataObj rhs, List<ReqRespUpdateOperation> operationList);

	Event.RawPayload toRawPayload();

	boolean wrapAsString(String path, String mimetype);

	Optional<String> encryptField(String path, EncryptionAlgorithm encrypter);

	Optional<String> decryptField(String path, EncryptionAlgorithm decrypter);

	class PathNotFoundException extends Exception{

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