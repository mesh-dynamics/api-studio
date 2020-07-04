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
