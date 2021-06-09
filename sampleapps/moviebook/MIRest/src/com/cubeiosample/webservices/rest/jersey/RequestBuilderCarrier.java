package com.cubeiosample.webservices.rest.jersey;

import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.client.Invocation.Builder;

public class RequestBuilderCarrier implements io.opentracing.propagation.TextMap {
    private final Builder builder;

    RequestBuilderCarrier(Builder builder) {
        this.builder = builder;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("carrier is write-only");
    }

    @Override
    public void put(String key, String value) {
        builder.header(key, value);
    }
}