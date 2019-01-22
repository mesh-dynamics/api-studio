package io.cube.utils;

import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.client.Invocation.Builder;

import org.apache.log4j.Logger;

public class RequestBuilderCarrier implements io.opentracing.propagation.TextMap {
  final static Logger LOGGER = Logger.getLogger(RequestBuilderCarrier.class);
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