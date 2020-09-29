package io.md.tracer.handlers;

import io.md.dao.MDTraceInfo;
import io.md.utils.Utils;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Optional;
import java.util.function.BiFunction;

public interface MDTraceHandler {

    BiFunction<MultivaluedMap<String, String>  , String ,  Optional<String>> getHeader = (headers , headerKey) -> {
        String val = headers.getFirst(headerKey);
        return (val==null || val.isEmpty()) ?  Optional.empty() : Optional.of(val);
    };

    BiFunction<MultivaluedMap<String, String>  , String ,  Optional<String>> getDecodedHeaderValue = (headers , headerKey) -> {

        return getHeader.apply(headers , headerKey).map(Utils::decodedValue);
    };

    Optional<MDTraceInfo> getTraceInfo(MultivaluedMap<String, String> headers , String app);

    Tracer getTracer();
}


