/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */
public interface DataObj {

    boolean isLeaf();

    boolean isEmpty();

    DataObj getVal(String path);

    String getValAsString(String path);

    String serialize();
}
