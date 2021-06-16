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

import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Optional;

public class DatadogTraceHandler  implements MDTraceHandler {
    @Override
    public Optional<MDTraceInfo> getTraceInfo(MultivaluedMap<String, String> headers, String app) {

        final Optional<String> trace = getHeader(headers , Constants.DATADOG_TRACE_FIELD);
        if(!trace.isPresent()) return Optional.empty();

        String traceField = trace.get();
        String parentSpanField =  getDecodedHeaderValue(headers , Constants.DATADOG_BAGGAGE_PARENT_SPAN).orElse(null);
        String spanField = getHeader(headers , Constants.DATADOG_SPAN_FIELD).orElse(null);

        return Optional.of(new MDTraceInfo(traceField , spanField , parentSpanField))  ;
    }

    @Override
    public Tracer getTracer() {
        return Tracer.Datadog;
    }
}
