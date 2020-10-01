package io.md.tracer.handlers;

import io.md.dao.MDTraceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.math.BigInteger;
import java.util.Optional;

import static io.md.utils.Utils.decodedValue;
import static io.md.utils.Utils.convertTraceId;
import static io.md.utils.Utils.high;


public abstract  class JaegerStyleTracerHandler implements MDTraceHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaegerStyleTracerHandler.class);

    public abstract  String getTracekey(String app);
    public abstract String getParentSpankey(String app);

    @Override
    public Optional<MDTraceInfo> getTraceInfo(MultivaluedMap<String, String> headers, String app) {
        final String traceKey = getTracekey(app);
        final Optional<String> trace = getHeader(headers , traceKey);
        if(!trace.isPresent()) return Optional.empty();

        final String mdTrace = trace.get();
        String[] parts = decodedValue(mdTrace).split(":");

        String traceField = null, spanField = null;

        if (parts.length != 4) {
            LOGGER.warn("trace id should have 4 parts but found: " + parts.length);
        } else {
            String traceId = parts[0];
            if (traceId.length() <= 32 && traceId.length() >= 1) {
                traceField = convertTraceId(high(parts[0]), (new BigInteger(parts[0], 16)).longValue());
                spanField = Long.toHexString((new BigInteger(parts[1], 16)).longValue());
            } else {
                LOGGER.error("Trace id [" + traceId + "] length is not within 1 and 32");
            }
        }
        String parentSpanField =  getDecodedHeaderValue(headers , getParentSpankey(app)).orElse(null);

        return Optional.of(new MDTraceInfo(traceField , spanField , parentSpanField))  ;
    }
}
