package io.md.tracer.handlers;

import io.md.dao.MDTraceInfo;
import io.md.utils.Utils;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Optional;
import java.util.function.BiFunction;

public interface MDTraceHandler {

    default Optional<String> getHeader(MultivaluedMap<String, String>  headers , String headerKey) {
        String val = headers.getFirst(headerKey);
        return (val==null || val.isEmpty()) ?  Optional.empty() : Optional.of(val);
    }

    default Optional<String> getDecodedHeaderValue(MultivaluedMap<String, String>  headers , String headerKey) {
        return getHeader(headers , headerKey).map(Utils::decodedValue);
    }

    Optional<MDTraceInfo> getTraceInfo(MultivaluedMap<String, String> headers , String app);

    Tracer getTracer();

}


