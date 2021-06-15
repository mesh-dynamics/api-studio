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
import io.md.utils.CommonUtils;
import io.md.utils.Utils;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Optional;
import java.util.function.BiFunction;

public interface MDTraceHandler {

    default Optional<String> getHeader(MultivaluedMap<String, String>  headers , String headerKey) {
        /*
        tracer headers are fixed. If there is possibility that we need to do the case insensitive lookup
        String val = CommonUtils.findFirstCaseInsensitiveMatch(headers , headerKey);
        */
        String val = headers.getFirst(headerKey);

        return (val==null || val.isEmpty()) ?  Optional.empty() : Optional.of(val);
    }

    default Optional<String> getDecodedHeaderValue(MultivaluedMap<String, String>  headers , String headerKey) {
        return getHeader(headers , headerKey).map(Utils::decodedValue);
    }

    Optional<MDTraceInfo> getTraceInfo(MultivaluedMap<String, String> headers , String app);

    Tracer getTracer();

}


