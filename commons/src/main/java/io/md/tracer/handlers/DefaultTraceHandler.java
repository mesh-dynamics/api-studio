/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.md.tracer.handlers;

import io.md.dao.MDTraceInfo;
import io.md.logger.LogMgr;
import org.slf4j.Logger;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultTraceHandler implements  MDTraceHandler {
    private static final Logger LOGGER = LogMgr.getLogger(DefaultTraceHandler.class);
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
