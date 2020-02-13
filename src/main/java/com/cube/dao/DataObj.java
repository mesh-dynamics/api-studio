/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

import com.cube.core.Comparator.MatchType;
import com.cube.core.CompareTemplate;
import com.cube.core.TemplateEntry;
import com.cube.cryptography.EncryptionAlgorithm;
import com.cube.golden.ReqRespUpdateOperation;

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

    DataObj applyTransform(DataObj rhs, List<ReqRespUpdateOperation> operationList);

    Event.RawPayload toRawPayload();

    boolean wrapAsString(String path, String mimetype);

    Optional<String> encryptField(String path, EncryptionAlgorithm encrypter);

    Optional<String> decryptField(String path, EncryptionAlgorithm decrypter);

    void getPathRules(CompareTemplate template, Map<String, TemplateEntry> vals);

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
