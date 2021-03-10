package io.md.tracer;

import static io.md.cache.Constants.APP;
import static io.md.cache.Constants.CUSTOMER_ID;

import io.md.cache.AbstractMDCache;
import io.md.cache.Constants;
import io.md.dao.CustomerAppConfig;
import io.md.dao.MDTraceInfo;
import io.md.logger.LogMgr;
import io.md.services.CustAppConfigCache;
import io.md.services.DataStore;
import io.md.tracer.handlers.*;
import io.md.utils.Utils;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import javax.ws.rs.core.MultivaluedMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class TracerMgr {

    private static final Logger LOGGER = LogMgr.getLogger(TracerMgr.class);
    private DataStore dStore;

    private static Map<Tracer , MDTraceHandler> tracehandlers;
    private static DefaultTraceHandler defaultTraceHandler;
    private static PassiveExpiringMap<String , MDTraceHandler> appTracerConfig = new PassiveExpiringMap<>(30 , TimeUnit.MINUTES);

    private static class MDCacheTracer extends AbstractMDCache {

        private static final MDCacheTracer singleton = new MDCacheTracer();
        private List<Pair<PassiveExpiringMap<String, ?>, String[]>> cacheAndKeys = new ArrayList<>(1);
        {
            cacheAndKeys.add(Pair.of(appTracerConfig, new String[]{CUSTOMER_ID, APP}));
        }
        private MDCacheTracer(){

        }
        @Override
        public String getName() {
            return Constants.TRACER;
        }

        @Override
        public List<Pair<PassiveExpiringMap<String, ?>, String[]>> getCacheAndKeys() {
            return cacheAndKeys;
        }
    }

    static {
        // Using LinkedHashMap so that order (priority) is maintained
        tracehandlers = new LinkedHashMap<>();

        tracehandlers.put(Tracer.MeshD , new MeshDTraceHandler());
        tracehandlers.put(Tracer.Jaeger , new JaegerTraceHandler());
        tracehandlers.put(Tracer.Zipkin , new ZipkinTraceHandler());
        tracehandlers.put(Tracer.Datadog , new DatadogTraceHandler());

        defaultTraceHandler = DefaultTraceHandler.getInstance(tracehandlers) ;
        Object obj =  MDCacheTracer.singleton;
    }

    private MDTraceHandler getTraceHandler(String customer, String app){
        final String custAppKey = customer.concat("-").concat(app);
        MDTraceHandler traceHandler =  appTracerConfig.get(custAppKey);
        if(traceHandler!=null) return traceHandler;

        //First time. Populate from Datastore
        synchronized (TracerMgr.class){
            if(traceHandler==null){
                //See if there is any tracer configured
                // Otherwise traceHandler based on default priority order of existing trace Handlers.
                Optional<Tracer> tracer =  getTracerFromAppConfig(customer , app);
                LOGGER.info("tracer  for app "+ custAppKey + " :"+ tracer);
                traceHandler = tracer.map(tracehandlers::get).orElse(defaultTraceHandler);
            }
        }

        LOGGER.info("trace Handler for app "+ custAppKey + " :"+ traceHandler.getTracer() + ":" + traceHandler.getClass().getSimpleName());

        appTracerConfig.put(custAppKey , traceHandler);
        return traceHandler;
    }

    private Optional<Tracer> getTracerFromAppConfig(String customer, String app){
        Optional<CustomerAppConfig> config = CustAppConfigCache.getInstance(dStore).getCustomerAppConfig(customer , app);

        return config.flatMap(appCfg->{
            LOGGER.info("Tracer config key for customer: "+ customer + " app :" + app +":" + appCfg.tracer);
            return appCfg.tracer.flatMap(tracer->Utils.valueOf(Tracer.class , tracer));
        });
    }

    public TracerMgr(DataStore store){
        this.dStore = store;
    }

    public Optional<MDTraceInfo> getTraceInfo(MultivaluedMap<String, String> headers, String customerId, String app){

        return getTraceHandler(customerId, app).getTraceInfo(headers, app);
    }

}
