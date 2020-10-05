package io.md.tracer.handlers;

import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Optional;

public class ZipkinTraceHandler implements MDTraceHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZipkinTraceHandler.class);

    @Override
    public Optional<MDTraceInfo> getTraceInfo(MultivaluedMap<String, String> headers, String app) {

        final Optional<String> trace = getHeader(headers , Constants.ZIPKIN_TRACE_FIELD);
        if(!trace.isPresent()) return Optional.empty();


        String traceField = trace.get();
        String spanField = getHeader(headers , Constants.ZIPKIN_SPAN_FIELD).orElse(null);
        String parentSpanField =  getDecodedHeaderValue(headers , Constants.ZIPKIN_BAGGAGE_PARENT_SPAN).orElse(null);

        return Optional.of(new MDTraceInfo(traceField , spanField , parentSpanField))  ;
    }

    @Override
    public Tracer getTracer() {
        return Tracer.Zipkin;
    }
}
