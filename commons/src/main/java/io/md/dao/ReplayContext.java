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

package io.md.dao;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ReplayContext {
    public Optional<String> reqTraceId;
    public Optional<Instant> reqStartTs;
    public Optional<Instant> reqEndTs;
    public Optional<String> currentCollection;
    public Optional<String> reqSpanId;

    public final Map<String , Instant> mockReqsMatchInfo = new HashMap<>();

    public ReplayContext(){
        reqTraceId = Optional.empty();
        reqStartTs = Optional.empty();
        reqEndTs = Optional.empty();
        currentCollection = Optional.empty();
        reqSpanId = Optional.empty();
    }

    public ReplayContext(Event request , Instant reqEndTs , Optional<ReplayContext> currentCtx){
        this.reqTraceId = Optional.of(request.getTraceId());
        this.reqStartTs = Optional.of(request.timestamp);
        this.reqEndTs = Optional.ofNullable(reqEndTs);
        this.currentCollection = currentCtx.flatMap(ctx->ctx.currentCollection);
        this.reqSpanId = Optional.of(request.getSpanId());
    }

    @JsonIgnore
    public void setCurrentCollection(String currentRecording) {
        this.currentCollection = Optional.of(currentRecording);
    }


    @JsonIgnore
    public void setMockResultToReplayContext(Event event){
        String id = getEventUniqueMockReqId(event);
        mockReqsMatchInfo.put(id , event.timestamp);
    }

    @JsonIgnore
    public Optional<Instant> getLastMockEventTs(Event event){
        String id = getEventUniqueMockReqId(event);
        return Optional.ofNullable(mockReqsMatchInfo.get(id));
    }

    @JsonIgnore
    public static String getEventUniqueMockReqId(Event event){
        return String.format("%s-%s-%s-%s" , event.service , event.apiPath , event.getTraceId() , event.payloadKey);
    }

    /*
    @JsonIgnore
    public boolean isTracePropagationEnabled(){
       return !reqStartTs.isPresent() && !reqEndTs.isPresent();
    }
    */
}
