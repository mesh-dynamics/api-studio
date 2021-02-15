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

    public ReplayContext(Event request , Instant reqStartTs , Instant reqEndTs , Optional<ReplayContext> currentCtx){
        this.reqTraceId = Optional.of(request.getTraceId());
        this.reqStartTs = Optional.ofNullable(reqStartTs);
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
