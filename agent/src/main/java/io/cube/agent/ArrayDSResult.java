/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cube.agent;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import io.md.services.DSResult;

/*
 * Created by IntelliJ IDEA.
 * Date: 23/06/20
 */
public class ArrayDSResult<T> implements DSResult<T> {

    private List<T> objects;

    private final long numResults;

    private final long numFound;

    public ArrayDSResult(List<T> objects, long numResults, long numFound) {
        this.objects = objects;
        this.numResults = numResults;
        this.numFound = numFound;
    }

    // for jackson
    public ArrayDSResult() {
        this.objects = Collections.emptyList();
        this.numResults = 0;
        this.numFound = 0;
    }


    @Override
    public Stream<T> getObjects() {
        return objects.stream();
    }

    @Override
    public long getNumResults() {
        return numResults;
    }

    @Override
    public long getNumFound() {
        return numFound;
    }
}
