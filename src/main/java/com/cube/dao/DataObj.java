/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */
public interface DataObj {

    boolean isLeaf();

    boolean isEmpty();

    DataObj getVal(String path);

    Optional<String> getValAsString(String path);

    String serialize();

    void collectKeyVals(Function<String, Boolean> filter, Collection<String> vals);
}
