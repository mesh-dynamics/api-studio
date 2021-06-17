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

package io.cube.agent;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.md.dao.Event;
import io.md.dao.MockWithCollection;
import io.md.services.MockResponse;
import io.md.services.Mocker;

/*
 * Created by IntelliJ IDEA.
 * Date: 02/07/20
 */

/**
 * This class is for handling the case where there are multiple matches for an api. It will cache the timestamp of
 * the previous match and use in the next mock call
 */
public class NonIdempotentMocker implements Mocker {

    final private Mocker mocker;
    final private Map<Integer, Instant> fnMap = new ConcurrentHashMap<>();


    public NonIdempotentMocker(Mocker mocker) {
        this.mocker = mocker;
    }

    @Override
    public MockResponse mock(Event event, Optional<Instant> lowerBoundForMatching, Optional<MockWithCollection> mockWithCollections)
            throws MockerException {

        Optional<String> traceId = Optional.ofNullable(event.getTraceId());
        Optional<String> spanId = Optional.ofNullable(event.spanId);
        Optional<String> parentSpanId = Optional.ofNullable(event.parentSpanId);

        Integer key = traceId.orElse("").concat(spanId.orElse(""))
                .concat(parentSpanId.orElse(""))
                .concat(event.apiPath).hashCode();
        Optional<Instant> lowerBound = lowerBoundForMatching.isPresent() ? lowerBoundForMatching : Optional.ofNullable(fnMap.get(key));

        MockResponse mockResponse = mocker.mock(event, lowerBound,
                Optional.empty());

        if (mockResponse.numResults > 1) {
            // multiple match case
            // If multiple Solr docs are returned, we need to maintain the last timestamp
            // to be used in the next mock call.
            mockResponse.response.ifPresent(resp -> {
                fnMap.put(key, resp.timestamp);
            });
        } else {
            fnMap.remove(key);
        }
        return mockResponse;
    }
}
