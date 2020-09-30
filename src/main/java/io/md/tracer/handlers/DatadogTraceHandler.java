package io.md.tracer.handlers;

import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Optional;

public class DatadogTraceHandler  implements MDTraceHandler {
    @Override
    public Optional<MDTraceInfo> getTraceInfo(MultivaluedMap<String, String> headers, String app) {

        Optional<String> trace = getHeader(headers , Constants.DATADOG_TRACE_FIELD);
        if(!trace.isPresent()) return Optional.empty();

        String traceField = trace.get();
        String spanField = getHeader(headers , Constants.DATADOG_TRACE_FIELD).orElse(null);
        String parentSpanField =  getDecodedHeaderValue(headers , Constants.DATADOG_BAGGAGE_PARENT_SPAN).orElse(null);

        return Optional.of(new MDTraceInfo(traceField , spanField , parentSpanField))  ;
    }

    @Override
    public Tracer getTracer() {
        return Tracer.Datadog;
    }
}
