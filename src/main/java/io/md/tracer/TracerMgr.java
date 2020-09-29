package io.md.tracer;

import io.md.dao.CustomerAppConfig;
import io.md.dao.MDTraceInfo;
import io.md.services.DataStore;
import io.md.tracer.handlers.*;
import io.md.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.util.*;

public class TracerMgr {

    private static final Logger LOGGER = LoggerFactory.getLogger(TracerMgr.class);
    private DataStore dStore;

    private static Map<Tracer , MDTraceHandler> tracehandlers;
    private static Map<String , Optional<Tracer>> appTracerConfig = new HashMap<>();

    static {
        // Using LinkedHashMap so that order (priority) is maintained
        tracehandlers = new LinkedHashMap<>();

        tracehandlers.put(Tracer.MeshD , new MeshDTraceHandler());
        tracehandlers.put(Tracer.Jaeger , new JaegerTraceHandler());
        tracehandlers.put(Tracer.Zipkin , new ZipkinTraceHandler());
        tracehandlers.put(Tracer.Datadog , new DatadogTraceHandler());
    }

    private Optional<Tracer> getTracer(String customer,  String app){
        Optional<Tracer> tracer =  appTracerConfig.get(app);
        if(tracer!=null) return tracer;

        //First time. Populate from Datastore
        synchronized (TracerMgr.class){
            if(tracer==null){
                tracer = getTracerFromAppConfig(customer , app);
            }
        }

        LOGGER.info("tracer for app "+ app + " :"+ tracer.orElse(null) );

        appTracerConfig.put(app , tracer);
        return tracer;
    }

    private Optional<Tracer> getTracerFromAppConfig(String customer, String app){
        Optional<CustomerAppConfig> config =  this.dStore.getAppConfiguration(customer ,app);

        return config.flatMap(appCfg->{
            String tracer = appCfg.tracer;
            LOGGER.info("Tracer config key for cust: "+ customer + " app :" + app +":" + tracer);
            return Utils.valueOf(Tracer.class , tracer);
        });
    }

    public TracerMgr(DataStore store){
        this.dStore = store;
    }

    public Optional<MDTraceInfo> getTraceInfo(MultivaluedMap<String, String> headers, String customerId, String app){

        //See if there is any tracer configured
        Optional<Tracer> tracer = getTracer(customerId, app);
        if(tracer.isPresent()){
            return tracehandlers.get(tracer.get()).getTraceInfo(headers, app);
        }

        // Otherwise default priority order of trace Handlers.
        for(Map.Entry<Tracer, MDTraceHandler> entry : tracehandlers.entrySet()){

            Optional<MDTraceInfo> traceInfo = entry.getValue().getTraceInfo(headers ,app);
            if(traceInfo.isPresent()) return traceInfo;
        }

        return Optional.of(new MDTraceInfo());
    }

}
