package io.md.tracer.handlers;

import io.md.dao.MDTraceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultTraceHandler implements  MDTraceHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTraceHandler.class);
    private static DefaultTraceHandler singleton;

    private final Map<Tracer , MDTraceHandler> tracehandlers;
    private DefaultTraceHandler(Map<Tracer , MDTraceHandler> handlers){
        this.tracehandlers = handlers;
    }

    public synchronized static DefaultTraceHandler getInstance(Map<Tracer , MDTraceHandler> tracehandlers){
        if(singleton==null){
            singleton = new DefaultTraceHandler(tracehandlers);
        }
        return singleton;
    }

    @Override
    public Optional<MDTraceInfo> getTraceInfo(MultivaluedMap<String, String> headers, String app) {

        // Otherwise default priority order of trace Handlers.
        for(Map.Entry<Tracer, MDTraceHandler> entry : tracehandlers.entrySet()){

            Optional<MDTraceInfo> traceInfo = entry.getValue().getTraceInfo(headers ,app);
            if(traceInfo.isPresent()) {
                LOGGER.debug("traceInfo generated "+traceInfo.get().toString() + " by tracer :"+entry.getKey().toString());
                return traceInfo;
            }
        }

        LOGGER.warn("No traceInfo generated for app "+app + " giving empty. Request Headers : "+headers.keySet().stream().collect(Collectors.joining(",")));
        return Optional.of(new MDTraceInfo());
    }

    @Override
    public Tracer getTracer() {
        return Tracer.MeshD;
    }
}
