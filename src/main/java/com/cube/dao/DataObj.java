/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.cube.core.Comparator.MatchType;
import com.cube.core.CompareTemplate;
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

    class PathNotFoundException extends Exception{

    }

    class DataObjCreationException extends Exception {
        public DataObjCreationException(String msg, Throwable e) {
            super (msg,e);
        }

        public DataObjCreationException(Throwable e) {
            super(e);
        }
    }
}
