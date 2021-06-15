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
import java.math.BigInteger;
import java.util.Optional;

import static io.md.utils.Utils.decodedValue;
import static io.md.utils.Utils.convertTraceId;
import static io.md.utils.Utils.high;


public abstract  class JaegerStyleTracerHandler implements MDTraceHandler {

    private static final Logger LOGGER = LogMgr.getLogger(JaegerStyleTracerHandler.class);

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
