/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.services;

import java.util.stream.Stream;

/*
 * Created by IntelliJ IDEA.
 * Date: 14/05/20
 */
public interface CubeDSResult<T> {

    /**
     * @return
     */
    public Stream<T> getObjects();

    public long getNumResults();

    public long getNumFound();
}
