/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.util.Collection;
import java.util.function.Function;

import com.cube.core.Comparator.MatchType;
import com.cube.core.CompareTemplate;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */
public interface DataObj {

    boolean isLeaf();

    boolean isEmpty();

    DataObj getVal(String path);

    //Optional<String> getValAsString(String path);

    String getValAsString(String path) throws PathNotFoundException;

    String serialize();

    void collectKeyVals(Function<String, Boolean> filter, Collection<String> vals);

    MatchType compare(DataObj rhs, CompareTemplate template);

    class PathNotFoundException extends Exception{

    }
}
